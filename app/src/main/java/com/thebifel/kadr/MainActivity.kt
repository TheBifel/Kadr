package com.thebifel.kadr

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.thebifel.kadr.di.appContainer
import com.thebifel.kadr.platform.HtmlDocuments
import com.thebifel.kadr.platform.LocalizedActivity
import com.thebifel.kadr.platform.widget.WidgetPinRequester
import com.thebifel.kadr.presentation.editor.*
import com.thebifel.kadr.presentation.theme.KadrTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Stable Android entry point; editing state and project actions live in EditorViewModel. */
class MainActivity : LocalizedActivity() {
    private val services
        get() = appContainer

    private var incomingProject by mutableStateOf<String?>(null)
    private var exportHtml = ""
    private val documents by lazy { HtmlDocuments(contentResolver) }
    private val model by lazy {
        ViewModelProvider(
            this,
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    with(services) {
                        EditorViewModel(
                            repository,
                            content,
                            renderer,
                            environment,
                            widgets,
                            scheduler,
                            saveProject,
                            deleteProject,
                            requestRefresh,
                        )
                            as T
                    }
            },
        )[EditorViewModel::class.java]
    }

    private val openFile =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null)
                lifecycleScope.launch {
                    runCatching { withContext(Dispatchers.IO) { documents.read(uri) } }
                        .onSuccess { model.onEvent(EditorEvent.ImportHtml(it)) }
                        .onFailure {
                            model.onEvent(
                                EditorEvent.ReportError(
                                    it.message ?: getString(R.string.file_failed)
                                )
                            )
                        }
                }
        }
    private val saveFile =
        registerForActivityResult(ActivityResultContracts.CreateDocument("text/html")) { uri ->
            if (uri != null)
                lifecycleScope.launch {
                    runCatching { withContext(Dispatchers.IO) { documents.write(uri, exportHtml) } }
                        .onFailure {
                            model.onEvent(EditorEvent.ReportError(getString(R.string.write_failed)))
                        }
                }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        incomingProject = intent.getStringExtra("project")
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                services.repository.initialize(services.content::starter)
                services.reconcile(refresh = false)
                services.repository.all().firstOrNull()?.let { project ->
                    val frame = services.repository.result(project.id)
                    if (
                        frame.success == 0L ||
                            project.autoTheme &&
                                frame.dark != services.environment.current(project).dark
                    ) {
                        services.requestRefresh(project.id, replace = true)
                    }
                }
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                model.effects.collect { effect ->
                    when (effect) {
                        is EditorEffect.Message ->
                            model.onEvent(EditorEvent.ReportError(effect.text))
                        is EditorEffect.Pin ->
                            if (!WidgetPinRequester(this@MainActivity).request(effect.project))
                                model.onEvent(
                                    EditorEvent.ReportError(getString(R.string.pin_fallback))
                                )
                    }
                }
            }
        }
        setContent {
            KadrTheme {
                val state by model.state.collectAsStateWithLifecycle()
                val systemDark = isSystemInDarkTheme()
                LaunchedEffect(incomingProject) {
                    incomingProject?.let { model.onEvent(EditorEvent.Select(it)) }
                }
                LaunchedEffect(systemDark) { model.onEvent(EditorEvent.EnvironmentChanged) }
                StudioScreen(
                    state,
                    model::onEvent,
                    EditorPlatformActions(
                        importHtml = {
                            openFile.launch(
                                arrayOf("text/html", "text/plain", "application/xhtml+xml")
                            )
                        },
                        exportHtml = {
                            exportHtml = it
                            saveFile.launch("kadr.html")
                        },
                        copyAi = {
                            copy(services.content.aiInstructions(), "Kadr AI instructions")
                        },
                        copyContract = { copy(services.content.contract(), "Kadr SDK contract") },
                        copyError = { copy(it, "Kadr error") },
                        contractText = services.content.contract(),
                        aiText = services.content.aiInstructions(),
                    ),
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        incomingProject = intent.getStringExtra("project")
    }

    private fun copy(text: String, label: String) {
        getSystemService(ClipboardManager::class.java)
            .setPrimaryClip(ClipData.newPlainText(label, text))
    }
}
