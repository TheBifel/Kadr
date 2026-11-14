package com.thebifel.kadr

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.thebifel.kadr.di.appContainer
import com.thebifel.kadr.domain.model.Project
import com.thebifel.kadr.platform.LocalizedActivity
import com.thebifel.kadr.presentation.editor.intervalText
import com.thebifel.kadr.presentation.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ConfigureActivity : LocalizedActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val id =
            intent.getIntExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID,
            )
        setResult(RESULT_CANCELED)
        if (id == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }
        var projects by mutableStateOf(emptyList<Project>())
        var binding by mutableStateOf(false)
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                appContainer.repository.ensure(appContainer.content::starter)
            }
            appContainer.repository.changes().collect {
                projects = withContext(Dispatchers.IO) { appContainer.repository.all() }
            }
        }
        setContent {
            KadrTheme {
                Surface(Modifier.fillMaxSize(), color = Bg) {
                    Column(
                        Modifier.fillMaxSize()
                            .windowInsetsPadding(WindowInsets.safeDrawing)
                            .verticalScroll(rememberScrollState())
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                    ) {
                        Text("▦ Kadr", fontSize = 31.sp, color = Accent)
                        Spacer(Modifier.height(10.dp))
                        Text(S(R.string.pick_page), fontSize = 26.sp)
                        Text(
                            S(R.string.pick_page_hint),
                            color = Muted,
                            fontSize = 13.sp,
                            lineHeight = 21.sp,
                        )
                        projects.forEach { project ->
                            OutlinedButton(
                                onClick = {
                                    binding = true
                                    lifecycleScope.launch {
                                        val bound =
                                            withContext(Dispatchers.IO) {
                                                appContainer.bindWidget(id, project.id)
                                            }
                                        if (bound != null) {
                                            setResult(
                                                RESULT_OK,
                                                Intent()
                                                    .putExtra(
                                                        AppWidgetManager.EXTRA_APPWIDGET_ID,
                                                        id,
                                                    ),
                                            )
                                            finish()
                                        } else binding = false
                                    }
                                },
                                enabled = !binding,
                                modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp),
                                shape = RoundedCornerShape(12.dp),
                            ) {
                                Column(
                                    Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(5.dp),
                                ) {
                                    Text(project.name, color = Ink, fontSize = 15.sp)
                                    Text(
                                        "HTML + CSS + JavaScript · ${intervalText(this@ConfigureActivity, project.interval)}",
                                        color = Muted,
                                        fontSize = 11.sp,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
