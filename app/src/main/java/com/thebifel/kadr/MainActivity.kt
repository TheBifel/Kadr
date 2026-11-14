package com.thebifel.kadr

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

private val Accent: Color @Composable get()=MaterialTheme.colorScheme.primary
private val Bg: Color @Composable get()=MaterialTheme.colorScheme.background
private val Border: Color @Composable get()=MaterialTheme.colorScheme.outline
private val Muted: Color @Composable get()=MaterialTheme.colorScheme.onSurfaceVariant
private val Ink: Color @Composable get()=MaterialTheme.colorScheme.onSurface
@Composable private fun S(id: Int,vararg args: Any)=stringResource(id,*args)

@Composable fun KadrTheme(content: @Composable ()->Unit) {
    val dark=isSystemInDarkTheme()
    val palette=if(dark)darkColorScheme(
        primary=Color(0xFFD2F680),onPrimary=Color(0xFF203014),background=Color(0xFF101410),surface=Color(0xFF191F17),
        surfaceVariant=Color(0xFF1C2419),onSurface=Color(0xFFEDF1E5),onBackground=Color(0xFFEDF1E5),
        onSurfaceVariant=Color(0xFF9DAA91),outline=Color(0xFF36412F),primaryContainer=Color(0xFF242F1B),onPrimaryContainer=Color(0xFFB8CBA3),
        errorContainer=Color(0xFF34291D),onErrorContainer=Color(0xFFE4BA91)
    ) else lightColorScheme(
        primary=Color(0xFF4C6B24),onPrimary=Color.White,background=Color(0xFFF4F5EE),surface=Color(0xFFEEF1E5),
        surfaceVariant=Color(0xFFE4E9DB),onSurface=Color(0xFF202A1A),onBackground=Color(0xFF202A1A),
        onSurfaceVariant=Color(0xFF657157),outline=Color(0xFFC7D1BB),primaryContainer=Color(0xFFE1EDCE),onPrimaryContainer=Color(0xFF344B1E),
        errorContainer=Color(0xFFFFE9D6),onErrorContainer=Color(0xFF77471F)
    )
    val view=LocalView.current
    SideEffect {
        (view.context as? android.app.Activity)?.window?.let { window ->
            WindowCompat.getInsetsController(window,view).apply { isAppearanceLightStatusBars=!dark;isAppearanceLightNavigationBars=!dark }
        }
    }
    MaterialTheme(colorScheme=palette,content=content)
}

open class LocalizedActivity: ComponentActivity() {
    override fun attachBaseContext(newBase: Context) { super.attachBaseContext(LocaleSupport.localized(newBase)) }
}

data class DraftPreview(val html: String,val dark: Boolean,val bitmap: Bitmap)
class StudioModel: ViewModel() {
    val drafts=mutableMapOf<String,MutableState<Project>>()
    val dirty=mutableMapOf<String,MutableState<Boolean>>()
    val previews=mutableMapOf<String,MutableState<DraftPreview?>>()
}

class MainActivity: LocalizedActivity() {
    private lateinit var repo: Repository
    private val model by lazy { ViewModelProvider(this)[StudioModel::class.java] }
    private var incomingProject by mutableStateOf<String?>(null)
    private var imported by mutableStateOf<String?>(null)
    private var exportHtml=""
    private val openFile=registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if(uri!=null)runCatching {
            contentResolver.openInputStream(uri)!!.use { stream ->
                val out=java.io.ByteArrayOutputStream();val buffer=ByteArray(8192)
                while(true){val n=stream.read(buffer);if(n<0)break;require(out.size()+n<=500_000){getString(R.string.file_too_large)};out.write(buffer,0,n)}
                out.toString("UTF-8")
            }
        }.onSuccess { imported=it }.onFailure { message(it.message ?: getString(R.string.file_failed)) }
    }
    private val saveFile=registerForActivityResult(ActivityResultContracts.CreateDocument("text/html")) { uri ->
        if(uri!=null)runCatching { contentResolver.openOutputStream(uri)!!.bufferedWriter().use { it.write(exportHtml) } }
            .onSuccess { message(getString(R.string.html_saved)) }.onFailure { message(getString(R.string.write_failed)) }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState);repo=Repository(this);incomingProject=intent.getStringExtra("project")
        repo.initialize()
        Schedule.reconcile(this,false)
        repo.all().firstOrNull()?.let { first ->
            val frame=repo.result(first.id)
            if(frame.success==0L || first.autoTheme && frame.dark!=first.isDark(this))Schedule.now(this,first,replace=true)
        }
        setContent { KadrTheme { Studio() } }
    }
    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent);setIntent(intent);incomingProject=intent.getStringExtra("project") }
    private fun message(text: String) { Toast.makeText(this,text,Toast.LENGTH_SHORT).show() }
    private fun copy(text: String,ai: Boolean) {
        getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText(if(ai)"Kadr AI instructions" else "Kadr SDK contract",text))
        message(getString(if(ai)R.string.ai_copied else R.string.contract_copied))
    }
    private fun pin(project: Project) {
        val manager=AppWidgetManager.getInstance(this)
        if(!manager.isRequestPinAppWidgetSupported){message(getString(R.string.pin_fallback));return}
        val intent=Intent(this,PinReceiver::class.java).putExtra("project",project.id).setData(android.net.Uri.parse("kadr://pin/${project.id}"))
        val callback=PendingIntent.getBroadcast(this,project.id.hashCode(),intent,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE)
        manager.requestPinAppWidget(ComponentName(this,KadrWidget::class.java),null,callback)
    }

    @Composable private fun Studio() {
        var projects by remember { mutableStateOf(repo.all()) }
        LaunchedEffect(Unit) {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                repo.changes().collect { projects=repo.all() }
            }
        }
        fun createProject(): Project = Project(name=getString(R.string.new_name),html=Templates.page(this@MainActivity,"blank")).also {
            repo.save(it);projects=repo.all();Schedule.now(this@MainActivity,it)
        }
        if(projects.isEmpty()) {
            Surface(Modifier.fillMaxSize(),color=Bg) {
                Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(24.dp),verticalArrangement=Arrangement.spacedBy(20.dp)) {
                    Text("▦ Kadr",color=Accent,fontSize=27.sp,fontWeight=FontWeight.Bold)
                    Spacer(Modifier.weight(1f))
                    Text(S(R.string.no_widgets),fontSize=26.sp)
                    Button(onClick={createProject()},modifier=Modifier.fillMaxWidth()){Text(S(R.string.new_widget))}
                    Spacer(Modifier.weight(1f))
                }
            }
            return
        }
        var selected by rememberSaveable { mutableStateOf(intent.getStringExtra("project")?.takeIf { repo.get(it)!=null } ?: projects.first().id) }
        var draft by remember(selected) { model.drafts.getOrPut(selected) { mutableStateOf(repo.get(selected) ?: projects.first()) } }
        var dirty by remember(selected) { model.dirty.getOrPut(selected) { mutableStateOf(false) } }
        var preview by remember(selected) { model.previews.getOrPut(selected) { mutableStateOf(null) } }
        var frame by remember(selected) { mutableStateOf(repo.result(selected)) }
        var previewBusy by remember(selected) { mutableStateOf(false) }
        var previewError by remember(selected) { mutableStateOf("") }
        var tab by rememberSaveable { mutableIntStateOf(0) }
        var dialog by remember { mutableStateOf("") }
        var deleteTarget by remember { mutableStateOf<Project?>(null) }
        var menu by remember { mutableStateOf(false) }
        val scope=rememberCoroutineScope()
        val scroll=rememberScrollState()
        val systemDark=isSystemInDarkTheme()
        val effectiveDark=if(draft.autoTheme)systemDark else draft.dark
        val waiting=frame.pending>0L
        val storedImage=remember(frame.file) { repo.bitmap(selected)?.asImageBitmap() }
        val showDraft=preview!=null && preview?.html==draft.html && preview?.dark==effectiveDark && (dirty || waiting)
        val image=if(showDraft)preview?.bitmap?.asImageBitmap() else storedImage
        fun edit(next: Project){draft=next;dirty=true;previewError=""}
        fun previewDraft(next: Project) {
            edit(next)
            if(previewBusy)return
            val id=next.id;val html=next.html;val dark=next.isDark(this)
            previewBusy=true;previewError=""
            scope.launch {
                try {
                    val dimensions=WidgetUi.dimensions(this@MainActivity,id)
                    val bitmap=HtmlRenderer(this@MainActivity).render(next,dimensions.first,dimensions.second)
                    if(selected==id && repo.get(id)!=null && draft.html==html)preview=DraftPreview(html,dark,bitmap) else bitmap.recycle()
                }catch(e: CancellationException){throw e}
                catch(e: Exception){previewError=e.message ?: getString(R.string.render_failed)}
                finally{previewBusy=false}
            }
        }
        fun save() {
            val current=repo.get(draft.id)
            draft=draft.copy(revision=(current?.revision ?: 0)+1)
            repo.save(draft);dirty=false;previewError=""
            Schedule.configure(this,draft);Schedule.now(this,draft,replace=true)
        }
        LaunchedEffect(selected) {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                repo.changes().collect {
                    frame=repo.result(selected)
                    if(!dirty)repo.get(selected)?.takeIf { it.revision!=draft.revision }?.let { draft=it }
                }
            }
        }
        LaunchedEffect(incomingProject) { incomingProject?.let { if(it!=selected && repo.get(it)!=null){selected=it;tab=0} } }
        LaunchedEffect(imported) { imported?.let { edit(draft.copy(html=it));tab=0;imported=null;message(getString(R.string.html_imported)) } }
        LaunchedEffect(systemDark,draft.autoTheme) { if(dirty && draft.autoTheme)previewDraft(draft) }
        Surface(Modifier.fillMaxSize(),color=Bg) {
            Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
                Row(Modifier.fillMaxWidth().padding(horizontal=20.dp,vertical=9.dp),verticalAlignment=Alignment.CenterVertically) {
                    Text("▦ "+S(R.string.app_name),color=Accent,fontSize=27.sp,fontWeight=FontWeight.Bold,letterSpacing=(-1).sp)
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick={dialog="projects"}){Text(S(R.string.my_widgets)+" ▾",fontSize=12.sp)}
                    Box {
                        IconButton(onClick={menu=true}){Text("⋮",fontSize=26.sp,color=Muted)}
                        DropdownMenu(expanded=menu,onDismissRequest={menu=false}) {
                            DropdownMenuItem(text={Text(S(R.string.import_html))},onClick={menu=false;openFile.launch(arrayOf("text/html","text/plain","application/xhtml+xml"))})
                            DropdownMenuItem(text={Text(S(R.string.export_html))},onClick={menu=false;exportHtml=draft.html;saveFile.launch("kadr.html")})
                            DropdownMenuItem(text={Text(S(R.string.contract_title))},onClick={menu=false;dialog="contract"})
                            DropdownMenuItem(text={Text(S(R.string.copy_ai))},onClick={menu=false;copy(ContractText.aiInstructions(this@MainActivity),true)})
                        }
                    }
                }
                HorizontalDivider(color=Border)
                Column(Modifier.weight(1f).verticalScroll(scroll).padding(20.dp),verticalArrangement=Arrangement.spacedBy(18.dp)) {
                    Column(verticalArrangement=Arrangement.spacedBy(7.dp)) {
                        Text(S(R.string.programmable_widget),fontSize=10.sp,color=Muted,letterSpacing=2.sp)
                        Text(draft.name,fontSize=29.sp,fontWeight=FontWeight.SemiBold,letterSpacing=(-.8).sp)
                        Row(verticalAlignment=Alignment.CenterVertically) {
                            Text("HTML  ·  CSS  ·  JavaScript",color=Accent,fontSize=11.sp,fontFamily=FontFamily.Monospace)
                            Spacer(Modifier.weight(1f));Text(S(if(dirty)R.string.unsaved else R.string.saved),color=Muted,fontSize=10.sp)
                        }
                    }
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.surfaceVariant).border(1.dp,Border,RoundedCornerShape(20.dp)).padding(20.dp),verticalArrangement=Arrangement.spacedBy(17.dp)) {
                        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                            Text(S(if(showDraft)R.string.draft_preview else R.string.preview_title),fontSize=9.sp,color=Muted,letterSpacing=1.3.sp)
                            Spacer(Modifier.weight(1f));Text("4 × 2",fontSize=10.sp,color=Accent,fontFamily=FontFamily.Monospace)
                        }
                        val cardBg=Color(if(effectiveDark)0xFF222B1B else 0xFFF2F4E9)
                        val cardMuted=Color(if(effectiveDark)0xFFB8C9A4 else 0xFF69765B)
                        val ratio=if(image!=null)image.width.toFloat()/image.height else 2f
                        Box(Modifier.fillMaxWidth().aspectRatio(ratio).clip(RoundedCornerShape(draft.radius.dp)).background(cardBg)) {
                            if(image!=null)Image(image,S(R.string.frame_content),Modifier.matchParentSize(),contentScale=ContentScale.FillBounds)
                            else Box(Modifier.matchParentSize(),contentAlignment=Alignment.Center){Text(S(if(previewBusy)R.string.building_preview else R.string.first_frame),fontSize=13.sp,color=cardMuted)}
                            Row(Modifier.align(Alignment.BottomEnd).width(Policy.controlsWidth.dp).height(Policy.controlsHeight.dp).padding(end=8.dp,bottom=8.dp),verticalAlignment=Alignment.CenterVertically) {
                                Text(if(showDraft)S(R.string.draft_label) else WidgetUi.overlayStamp(frame),modifier=Modifier.weight(1f),color=cardMuted,fontSize=10.sp,maxLines=1,textAlign=TextAlign.End)
                                IconButton(onClick={if(dirty)previewDraft(draft) else Schedule.now(this@MainActivity,draft)},enabled=!previewBusy,modifier=Modifier.size(48.dp).semantics { contentDescription=getString(R.string.update_frame) }){Text("↻",fontSize=26.sp,color=Color(0xFF7E9C4D))}
                            }
                        }
                        Text(S(R.string.static_caption),fontSize=10.sp,color=Muted)
                    }
                    when {
                        previewBusy -> Text(S(R.string.building_preview),color=Accent,fontSize=11.sp)
                        previewError.isNotEmpty() -> Notice(S(R.string.preview_failed,previewError),true)
                        waiting -> Text(S(R.string.queued),color=Accent,fontSize=11.sp)
                        frame.error.isNotEmpty() -> Notice(S(R.string.previous_kept,frame.error),true)
                        frame.success>0 -> Text("● "+S(R.string.frame_ready),fontSize=11.sp,color=Muted)
                    }
                    TabRow(selectedTabIndex=tab,containerColor=Bg,contentColor=Accent,divider={HorizontalDivider(color=Border)}) {
                        listOf(R.string.content,R.string.appearance,R.string.updates).forEachIndexed { i,id ->
                            Tab(selected=tab==i,onClick={tab=i},text={Text(S(id),fontSize=12.sp)},selectedContentColor=Accent,unselectedContentColor=Muted)
                        }
                    }
                    when(tab) {
                        0 -> {
                            Text(S(R.string.your_page),fontSize=17.sp,fontWeight=FontWeight.Medium)
                            Text(S(R.string.content_hint),fontSize=12.sp,color=Muted,lineHeight=19.sp)
                            OutlinedTextField(value=draft.name,onValueChange={if(it.length<=80)edit(draft.copy(name=it))},label={Text(S(R.string.widget_name))},modifier=Modifier.fillMaxWidth(),singleLine=true)
                            OutlinedButton(onClick={copy(ContractText.aiInstructions(this@MainActivity),true)},modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(10.dp)){Text(S(R.string.copy_ai),fontSize=12.sp)}
                            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                                Text("index.html",fontSize=12.sp,color=Accent,fontFamily=FontFamily.Monospace);Spacer(Modifier.weight(1f))
                                TextButton(onClick={dialog="presets"}){Text(S(R.string.choose_template),fontSize=11.sp)}
                            }
                            OutlinedTextField(value=draft.html,onValueChange={if(it.length<=500_000)edit(draft.copy(html=it))},modifier=Modifier.fillMaxWidth().height(320.dp).semantics { contentDescription="HTML CSS JavaScript" },textStyle=LocalTextStyle.current.copy(fontFamily=FontFamily.Monospace,fontSize=11.sp,lineHeight=18.sp,color=Ink),keyboardOptions=KeyboardOptions(capitalization=KeyboardCapitalization.None,autoCorrectEnabled=false,keyboardType=KeyboardType.Ascii),shape=RoundedCornerShape(10.dp))
                            Notice(S(R.string.vue_hint))
                        }
                        1 -> {
                            Text(S(R.string.appearance_title),fontSize=17.sp,fontWeight=FontWeight.Medium)
                            Text(S(R.string.appearance_hint),fontSize=12.sp,color=Muted,lineHeight=19.sp)
                            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                                FilterChip(selected=draft.autoTheme,onClick={previewDraft(draft.copy(autoTheme=true))},label={Text(S(R.string.auto_theme))})
                                FilterChip(selected=!draft.autoTheme&&!draft.dark,onClick={previewDraft(draft.copy(autoTheme=false,dark=false))},label={Text(S(R.string.light_theme))})
                                FilterChip(selected=!draft.autoTheme&&draft.dark,onClick={previewDraft(draft.copy(autoTheme=false,dark=true))},label={Text(S(R.string.dark_theme))})
                            }
                            Text(S(R.string.corners),fontSize=12.sp,color=Muted)
                            Row(horizontalArrangement=Arrangement.spacedBy(10.dp)){listOf(0,16,24).forEach { r -> FilterChip(selected=draft.radius==r,onClick={edit(draft.copy(radius=r))},label={Text("$r dp")}) }}
                            Notice(S(R.string.footer_hint))
                        }
                        2 -> {
                            Text(S(R.string.updates_title),fontSize=17.sp,fontWeight=FontWeight.Medium)
                            Text(S(R.string.preferred_interval),fontSize=12.sp,color=Muted)
                            Column {
                                listOf(listOf(0,15,30,60),listOf(180,360,1440)).forEach { row ->
                                    Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)){row.forEach { minutes -> FilterChip(selected=draft.interval==minutes,onClick={edit(draft.copy(interval=minutes))},label={Text(intervalText(this@MainActivity,minutes),fontSize=11.sp)}) }}
                                }
                            }
                            ToggleRow(S(R.string.wifi_only),S(R.string.wifi_hint),draft.wifi){edit(draft.copy(wifi=it))}
                            ToggleRow(S(R.string.battery_saver),S(R.string.battery_hint),draft.battery){edit(draft.copy(battery=it))}
                            Notice(S(R.string.interval_hint))
                        }
                    }
                    Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(onClick={previewDraft(draft)},enabled=!previewBusy,shape=RoundedCornerShape(10.dp),modifier=Modifier.height(50.dp)){Text(S(R.string.preview_action),fontSize=12.sp)}
                        Button(onClick={save()},modifier=Modifier.weight(1f).heightIn(min=50.dp),shape=RoundedCornerShape(10.dp)){Text(S(R.string.save_build),fontSize=12.sp,fontWeight=FontWeight.SemiBold)}
                    }
                    OutlinedButton(onClick={if(dirty)save();pin(draft)},modifier=Modifier.fillMaxWidth().height(48.dp),shape=RoundedCornerShape(10.dp)){Text(S(R.string.add_home),fontSize=12.sp)}
                    Text(S(R.string.app_footer,BuildConfig.VERSION_NAME),color=Muted,fontFamily=FontFamily.Monospace,fontSize=9.sp,modifier=Modifier.padding(vertical=6.dp))
                }
            }
        }
        when(dialog) {
            "projects" -> AlertDialog(onDismissRequest={dialog=""},title={Text(S(R.string.my_widgets))},text={
                Column(Modifier.fillMaxWidth().heightIn(max=380.dp).verticalScroll(rememberScrollState())) {
                    projects.forEach { p ->
                        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                            TextButton(onClick={selected=p.id;tab=0;dialog=""},modifier=Modifier.weight(1f)) {
                                Text((if(p.id==selected)"● " else "")+p.name,modifier=Modifier.fillMaxWidth(),maxLines=2,overflow=TextOverflow.Ellipsis)
                            }
                            IconButton(onClick={deleteTarget=p;dialog="delete"}) {
                                Icon(painterResource(R.drawable.ic_delete),S(R.string.delete_widget_name,p.name),tint=Muted)
                            }
                        }
                    }
                }
            },confirmButton={TextButton(onClick={val next=createProject();selected=next.id;tab=0;dialog=""}){Text(S(R.string.new_widget))}},dismissButton={TextButton(onClick={dialog=""}){Text(S(R.string.close))}})
            "delete" -> deleteTarget?.let { target ->
                val bound=repo.bindings().containsValue(target.id)
                AlertDialog(onDismissRequest={deleteTarget=null;dialog="projects"},title={Text(S(R.string.delete_widget_title))},
                    text={Text(S(if(bound)R.string.delete_bound_message else R.string.delete_widget_message,target.name))},
                    confirmButton={TextButton(onClick={
                        Schedule.delete(this@MainActivity,target.id)
                        model.drafts.remove(target.id);model.dirty.remove(target.id);model.previews.remove(target.id)
                        projects=repo.all()
                        if(selected==target.id){selected=projects.firstOrNull()?.id ?: "";tab=0}
                        deleteTarget=null;dialog="projects"
                    }){Text(S(R.string.delete_action),color=MaterialTheme.colorScheme.error)}},
                    dismissButton={TextButton(onClick={deleteTarget=null;dialog="projects"}){Text(S(R.string.cancel))}})
            }
            "presets" -> AlertDialog(onDismissRequest={dialog=""},title={Text(S(R.string.preset_title))},text={Column {
                Text(S(R.string.preset_hint),fontSize=12.sp,color=Muted)
                listOf(R.string.preset_power to "power",R.string.preset_weather to "weather",R.string.preset_blank to "blank").forEach { (name,kind) ->
                    TextButton(onClick={val next=draft.copy(name=getString(name),html=Templates.page(this@MainActivity,kind));dialog="";previewDraft(next)}){Text(S(name))}
                }
            }},confirmButton={TextButton(onClick={dialog=""}){Text(S(R.string.close))}})
            "contract" -> AlertDialog(onDismissRequest={dialog=""},title={Text(S(R.string.contract_title))},text={Column(Modifier.heightIn(max=380.dp).verticalScroll(rememberScrollState())){Text(ContractText.contract(this@MainActivity),fontSize=12.sp,lineHeight=19.sp)}},confirmButton={TextButton(onClick={copy(ContractText.contract(this@MainActivity),false)}){Text(S(R.string.copy_contract))}},dismissButton={TextButton(onClick={dialog=""}){Text(S(R.string.close))}})

        }
    }
}

fun intervalText(context: Context,value: Int): String {
    val c=LocaleSupport.localized(context)
    return when {value==0->c.getString(R.string.manual_interval);value==1440->c.getString(R.string.daily_interval);value>=60->c.getString(R.string.hours_interval,value/60);else->c.getString(R.string.minutes_interval,value)}
}
@Composable private fun Notice(text: String,warning: Boolean=false) {
    val colors=MaterialTheme.colorScheme
    Text(text,fontSize=11.sp,lineHeight=18.sp,color=if(warning)colors.onErrorContainer else colors.onPrimaryContainer,modifier=Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(if(warning)colors.errorContainer else colors.primaryContainer).border(1.dp,Border,RoundedCornerShape(10.dp)).padding(14.dp))
}
@Composable private fun ToggleRow(title: String,description: String,checked: Boolean,onChange: (Boolean)->Unit) {
    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(5.dp)){Text(title,fontSize=13.sp);Text(description,fontSize=11.sp,color=Muted,lineHeight=17.sp)}
        Switch(checked=checked,onCheckedChange=onChange)
    }
}
class ConfigureActivity: LocalizedActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val id=intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,AppWidgetManager.INVALID_APPWIDGET_ID)
        setResult(RESULT_CANCELED)
        if(id==AppWidgetManager.INVALID_APPWIDGET_ID){finish();return}
        val repo=Repository(this);repo.ensure()
        setContent { KadrTheme { Surface(Modifier.fillMaxSize(),color=Bg) {
            Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).verticalScroll(rememberScrollState()).padding(24.dp),verticalArrangement=Arrangement.spacedBy(20.dp)) {
                Text("▦ Kadr",fontSize=31.sp,color=Accent,fontWeight=FontWeight.Bold)
                Spacer(Modifier.height(10.dp));Text(S(R.string.pick_page),fontSize=26.sp,fontWeight=FontWeight.SemiBold)
                Text(S(R.string.pick_page_hint),color=Muted,fontSize=13.sp,lineHeight=21.sp)
                repo.all().forEach { project ->
                    OutlinedButton(onClick={val p=repo.cloneForBinding(project);repo.bind(id,p.id);Schedule.configure(this@ConfigureActivity,p);WidgetUi.update(this@ConfigureActivity,id);Schedule.now(this@ConfigureActivity,p);setResult(RESULT_OK,Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,id));finish()},modifier=Modifier.fillMaxWidth().heightIn(min=64.dp),shape=RoundedCornerShape(12.dp)) {
                        Column(Modifier.fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(5.dp)){Text(project.name,color=Ink,fontSize=15.sp);Text("HTML + CSS + JavaScript · ${intervalText(this@ConfigureActivity,project.interval)}",color=Muted,fontSize=11.sp)}
                    }
                }
            }
        } } }
    }
}
