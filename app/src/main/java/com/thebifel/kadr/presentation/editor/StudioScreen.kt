package com.thebifel.kadr.presentation.editor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thebifel.kadr.BuildConfig
import com.thebifel.kadr.R
import com.thebifel.kadr.domain.model.*
import com.thebifel.kadr.presentation.theme.*

@Composable
fun StudioScreen(
    state: EditorUiState,
    onEvent: (EditorEvent) -> Unit,
    platform: EditorPlatformActions,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val draft = state.draft
    if (draft == null) {
        Surface(Modifier.fillMaxSize(), color = Bg) {
            Column(
                Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                Text("▦ Kadr", color = Accent, fontSize = 27.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                if (state.loading) CircularProgressIndicator()
                else Text(S(R.string.no_widgets), fontSize = 26.sp)
                Button(
                    onClick = { onEvent(EditorEvent.Create) },
                    enabled = !state.loading,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(S(R.string.new_widget))
                }
                Spacer(Modifier.weight(1f))
            }
        }
        return
    }
    val selected = state.selectedId
    val frame = state.frame
    val dirty = state.dirty
    val previewBusy = state.previewBusy
    val previewError = state.previewError
    val waiting = frame.pending > 0
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var dialog by remember { mutableStateOf("") }
    var deleteTarget by remember { mutableStateOf<Project?>(null) }
    var menu by remember { mutableStateOf(false) }
    val scroll = rememberScrollState()
    LaunchedEffect(selected) { tab = 0 }
    Surface(Modifier.fillMaxSize(), color = Bg) {
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "▦ " + S(R.string.app_name),
                    color = Accent,
                    fontSize = 27.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-1).sp,
                )
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { dialog = "projects" }) {
                    Text(S(R.string.my_widgets) + " ▾", fontSize = 12.sp)
                }
                Box {
                    IconButton(onClick = { menu = true }) {
                        Text("⋮", fontSize = 26.sp, color = Muted)
                    }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(
                            text = { Text(S(R.string.import_html)) },
                            onClick = {
                                menu = false
                                platform.importHtml()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(S(R.string.export_html)) },
                            onClick = {
                                menu = false
                                platform.exportHtml(draft.html)
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(S(R.string.contract_title)) },
                            onClick = {
                                menu = false
                                dialog = "contract"
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(S(R.string.copy_ai)) },
                            onClick = {
                                menu = false
                                platform.copyAi()
                            },
                        )
                    }
                }
            }
            HorizontalDivider(color = Border)
            Column(
                Modifier.weight(1f).verticalScroll(scroll).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text(
                        S(R.string.programmable_widget),
                        fontSize = 10.sp,
                        color = Muted,
                        letterSpacing = 2.sp,
                    )
                    Text(
                        draft.name,
                        fontSize = 29.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-.8).sp,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "HTML  ·  CSS  ·  JavaScript",
                            color = Accent,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                        )
                        Spacer(Modifier.weight(1f))
                        Text(
                            S(if (dirty) R.string.unsaved else R.string.saved),
                            color = Muted,
                            fontSize = 10.sp,
                        )
                    }
                }
                WidgetPreview(state, onEvent)
                when {
                    previewBusy ->
                        Text(S(R.string.building_preview), color = Accent, fontSize = 11.sp)
                    previewError.isNotEmpty() ->
                        Notice(S(R.string.preview_failed, previewError), true)
                    waiting -> Text(S(R.string.queued), color = Accent, fontSize = 11.sp)
                    frame.error.isNotEmpty() -> Notice(S(R.string.previous_kept, frame.error), true)
                    frame.success > 0 ->
                        Text("● " + S(R.string.frame_ready), fontSize = 11.sp, color = Muted)
                }
                TabRow(
                    selectedTabIndex = tab,
                    containerColor = Bg,
                    contentColor = Accent,
                    divider = { HorizontalDivider(color = Border) },
                ) {
                    listOf(R.string.content, R.string.appearance, R.string.updates)
                        .forEachIndexed { i, id ->
                            Tab(
                                selected = tab == i,
                                onClick = { tab = i },
                                text = { Text(S(id), fontSize = 12.sp) },
                                selectedContentColor = Accent,
                                unselectedContentColor = Muted,
                            )
                        }
                }
                EditorSettings(tab, draft, onEvent, platform, onPresets = { dialog = "presets" })
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = { onEvent(EditorEvent.Preview) },
                        enabled = !previewBusy,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.height(50.dp),
                    ) {
                        Text(S(R.string.preview_action), fontSize = 12.sp)
                    }
                    Button(
                        onClick = { onEvent(EditorEvent.Save) },
                        modifier = Modifier.weight(1f).heightIn(min = 50.dp),
                        shape = RoundedCornerShape(10.dp),
                    ) {
                        Text(
                            S(R.string.save_build),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
                OutlinedButton(
                    onClick = { onEvent(EditorEvent.Pin) },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Text(S(R.string.add_home), fontSize = 12.sp)
                }
                Text(
                    S(R.string.app_footer, BuildConfig.VERSION_NAME),
                    color = Muted,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                    modifier = Modifier.padding(vertical = 6.dp),
                )
            }
        }
    }
    EditorDialogs(
        dialog,
        deleteTarget,
        state,
        onEvent,
        platform,
        onDialog = { dialog = it },
        onDeleteTarget = { deleteTarget = it },
    )
}
