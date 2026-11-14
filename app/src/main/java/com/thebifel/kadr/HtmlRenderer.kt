package com.thebifel.kadr

import android.annotation.SuppressLint
import android.content.Context
import android.app.Presentation
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.graphics.PixelFormat
import android.view.WindowManager
import android.view.ViewGroup
import android.graphics.drawable.ColorDrawable
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class HtmlRenderer(private val context: Context, private val resourceLoader: ((Uri)->WebResourceResponse?)? = null) {
    @SuppressLint("SetJavaScriptEnabled")
    suspend fun render(project: Project, widthDp: Int=320, heightDp: Int=160, timeoutMs: Long=15_000): Bitmap = withContext(Dispatchers.Main) {
        suspendCancellableCoroutine { continuation ->
            val handler=Handler(Looper.getMainLooper())
            val localized=LocaleSupport.localized(context)
            val dark=project.isDark(context)
            val (width,height)=Policy.renderSize(widthDp,heightDp)
            val web=WebView(context)
            val sink=ImageReader.newInstance(width,height,PixelFormat.RGBA_8888,2)
            sink.setOnImageAvailableListener({ reader -> runCatching { reader.acquireLatestImage()?.close() } },handler)
            val displayManager=context.getSystemService(DisplayManager::class.java)
            val display=displayManager.createVirtualDisplay("Kadr render",width,height,context.resources.displayMetrics.densityDpi,sink.surface,DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY or DisplayManager.VIRTUAL_DISPLAY_FLAG_PRESENTATION)
            requireNotNull(display) { localized.getString(R.string.render_display_failed) }
            val presentation=Presentation(context,display.display,android.R.style.Theme_Material_NoActionBar_Fullscreen)
            presentation.window?.addFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
            presentation.setContentView(web,ViewGroup.LayoutParams(width,height))
            presentation.show()
            presentation.window?.setLayout(width,height)
            presentation.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            presentation.window?.decorView?.setPadding(0,0,0,0)
            fun close() {
                web.stopLoading();presentation.dismiss();web.removeJavascriptInterface("__KadrSignal");web.destroy();display.release();sink.setOnImageAvailableListener(null,null);sink.close()
            }
            var ended=false
            var readyRequested=false
            val timeout=Runnable {
                if(!ended) { ended=true; close();continuation.resumeWithException(IllegalStateException(localized.getString(R.string.render_timeout,timeoutMs/1000))) }
            }
            fun finish(error: String?=null) {
                if(ended)return
                ended=true;handler.removeCallbacks(timeout)
                try {
                    if(error!=null) continuation.resumeWithException(IllegalStateException(error.take(180)))
                    else {
                        check(web.width==width && web.height==height) { localized.getString(R.string.render_size_failed) }
                        val bitmap=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888)
                        val canvas=Canvas(bitmap);canvas.drawColor(Color.parseColor(if(dark)"#222B1B" else "#F2F4E9"));web.draw(canvas)
                        continuation.resume(bitmap)
                    }
                } catch(e: Throwable) { if(continuation.isActive)continuation.resumeWithException(e) }
                finally { close() }
            }
            web.setBackgroundColor(Color.TRANSPARENT)
            web.setLayerType(View.LAYER_TYPE_SOFTWARE,null)
            web.settings.apply {
                javaScriptEnabled=true
                domStorageEnabled=false
                allowFileAccess=false
                allowContentAccess=false
                javaScriptCanOpenWindowsAutomatically=false
                setSupportMultipleWindows(false)
                mixedContentMode=android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW
                defaultTextEncodingName="UTF-8"
                cacheMode=android.webkit.WebSettings.LOAD_NO_CACHE
                setSupportZoom(false)
                textZoom=100
            }
            web.isVerticalScrollBarEnabled=false;web.isHorizontalScrollBarEnabled=false
            web.setInitialScale((width.toFloat()/widthDp.coerceAtLeast(140)*100).toInt())
            web.webViewClient=object: WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean = true
                override fun shouldInterceptRequest(view: WebView,request: WebResourceRequest): WebResourceResponse? {
                    val uri=request.url
                    resourceLoader?.invoke(uri)?.let { return it }
                    if(uri.host=="kadr.local") {
                        return if(uri.scheme=="https" && uri.path=="/vue.global.prod.js") WebResourceResponse("application/javascript","UTF-8",context.assets.open("vue.global.prod.js"))
                        else WebResourceResponse("text/plain","UTF-8",404,"Not found",emptyMap(),ByteArrayInputStream(ByteArray(0)))
                    }
                    if(uri.scheme !in listOf("https","data","blob"))return WebResourceResponse("text/plain","UTF-8",403,"HTTPS only",emptyMap(),ByteArrayInputStream(ByteArray(0)))
                    return null
                }
                override fun onReceivedError(view: WebView, request: WebResourceRequest, error: android.webkit.WebResourceError) {
                    if(request.isForMainFrame)handler.post { finish(localized.getString(R.string.page_load_failed)) }
                }
            }
            web.addJavascriptInterface(object {
                @JavascriptInterface fun ready() { handler.post {
                    if(!ended && !readyRequested) {
                        readyRequested=true
                        web.postVisualStateCallback(1,object: WebView.VisualStateCallback() {
                            override fun onComplete(requestId: Long) { if(!ended)finish() }
                        })
                        web.invalidate()
                    }
                } }
                @JavascriptInterface fun fail(message: String) { handler.post { finish(message) } }
            },"__KadrSignal")
            val bg=if(dark)"#222b1b" else "#f2f4e9"
            val fg=if(dark)"#edf4dd" else "#29351e"
            val muted=if(dark)"#b8c9a4" else "#69765b"
            val accent=if(dark)"#c8ed7e" else "#a6c566"
            val inject="""
                <meta name="viewport" content="width=$widthDp, initial-scale=1, maximum-scale=1">
                <style>:root{--kadr-bg:$bg;--kadr-fg:$fg;--kadr-muted:$muted;--kadr-accent:$accent;--kadr-controls-width:${Policy.controlsWidth}px;--kadr-controls-height:${Policy.controlsHeight}px;color-scheme:${if(dark)"dark" else "light"}}html,body{margin:0;overflow:hidden;background:var(--kadr-bg);color:var(--kadr-fg)}*{animation:none!important;transition:none!important;box-sizing:border-box}::-webkit-scrollbar{display:none}</style>
                <script>
                window.Kadr=Object.freeze({locale:'${LocaleSupport.language(context)}',theme:'${if(dark)"dark" else "light"}',width:$widthDp,height:$heightDp,controls:Object.freeze({width:${Policy.controlsWidth},height:${Policy.controlsHeight}}),
                    ready:async function(){try{await document.fonts.ready;await Promise.all(Array.from(document.images).map(i=>i.decode()));__KadrSignal.ready();}catch(e){__KadrSignal.fail(${org.json.JSONObject.quote(localized.getString(R.string.render_resource_failed))}+e.message);}},
                    fail:function(message){__KadrSignal.fail(String(message));}});
                window.addEventListener('error',e=>Kadr.fail(e.message||${org.json.JSONObject.quote(localized.getString(R.string.js_failed))}));
                window.addEventListener('unhandledrejection',e=>Kadr.fail(e.reason?.message||String(e.reason)));
                </script>
            """.trimIndent()
            val source=if(Regex("<head(?:\\s[^>]*)?>",RegexOption.IGNORE_CASE).containsMatchIn(project.html))project.html.replaceFirst(Regex("<head(?:\\s[^>]*)?>",RegexOption.IGNORE_CASE).find(project.html)!!.value,Regex("<head(?:\\s[^>]*)?>",RegexOption.IGNORE_CASE).find(project.html)!!.value+inject)
            else "<!doctype html><html><head>$inject</head><body>${project.html}</body></html>"
            web.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(height,View.MeasureSpec.EXACTLY));web.layout(0,0,width,height)
            handler.postDelayed(timeout,timeoutMs)
            continuation.invokeOnCancellation { handler.post { if(!ended){ended=true;handler.removeCallbacks(timeout);close()} } }
            web.loadDataWithBaseURL("https://kadr.local/",source,"text/html","UTF-8",null)
        }
    }
}
