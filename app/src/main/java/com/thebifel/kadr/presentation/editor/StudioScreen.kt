package com.thebifel.kadr.presentation.editor

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thebifel.kadr.R
import com.thebifel.kadr.domain.model.Project
import com.thebifel.kadr.presentation.theme.*
import kotlinx.coroutines.launch

@Composable
fun StudioScreen(
    state: EditorUiState,
    onEvent: (EditorEvent) -> Unit,
    platform: EditorPlatformActions,
) {
    val drawer = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var drawerPage by rememberSaveable { mutableStateOf("menu") }
    var dialog by remember { mutableStateOf("") }
    var deleteTarget by remember { mutableStateOf<Project?>(null) }
    var codeOpen by rememberSaveable(state.selectedId) { mutableStateOf(false) }
    var settingsOpen by rememberSaveable(state.selectedId) { mutableStateOf(false) }
    val draft = state.draft
    fun closeDrawer() {
        scope.launch {
            drawer.close()
            drawerPage = "menu"
        }
    }
    LaunchedEffect(state.importGeneration) { if (state.importGeneration > 0) codeOpen = true }
    BackHandler(drawer.isOpen) { closeDrawer() }
    Surface(color = Bg, modifier = Modifier.fillMaxSize()) {
        ModalNavigationDrawer(
            drawerState = drawer,
            // Closed drawers open only from the edge, never while selecting or editing code.
            gesturesEnabled = drawer.isOpen,
            drawerContent = {
                EditorDrawer(drawerPage, { drawerPage = it }, ::closeDrawer, onEvent, platform)
            },
        ) {
            Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).imePadding()) {
                Column(Modifier.fillMaxSize()) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(onClick = { scope.launch { drawer.open() } }) {
                            EditorIcon(R.drawable.ic_menu, S(R.string.tools_menu))
                        }
                        Text(
                            draft?.name ?: S(R.string.app_name),
                            fontSize = 19.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                        )
                        IconButton(onClick = { dialog = "projects" }) {
                            EditorIcon(R.drawable.ic_grid, S(R.string.my_widgets))
                        }
                    }
                    HorizontalDivider(color = Border.copy(alpha = .55f))
                    if (draft == null) {
                        Box(
                            Modifier.weight(1f).fillMaxWidth(),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (state.loading) CircularProgressIndicator()
                            else Text(S(R.string.no_widgets), color = Muted)
                        }
                    } else {
                        key(state.selectedId) {
                            Column(
                                Modifier.weight(1f)
                                    .verticalScroll(rememberScrollState())
                                    .padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(20.dp),
                            ) {
                                WidgetPreview(state, onEvent, platform.copyError)
                                HorizontalDivider(color = Border.copy(alpha = .5f))
                                EditorCode(
                                    draft,
                                    codeOpen,
                                    { codeOpen = !codeOpen },
                                    onEvent,
                                    platform,
                                )
                                HorizontalDivider(color = Border.copy(alpha = .5f))
                                TextButton(
                                    onClick = { settingsOpen = !settingsOpen },
                                    contentPadding = PaddingValues(0.dp),
                                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                                ) {
                                    Text(
                                        S(R.string.widget_settings),
                                        modifier = Modifier.weight(1f),
                                        color = Ink,
                                    )
                                    EditorIcon(
                                        if (settingsOpen) R.drawable.ic_up else R.drawable.ic_down,
                                        null,
                                    )
                                }
                                if (settingsOpen) EditorSettings(draft, onEvent)
                            }
                        }
                    }
                    HorizontalDivider(color = Border.copy(alpha = .55f))
                    val add = draft != null && !state.dirty && draft.id !in state.boundProjectIds
                    Button(
                        onClick = {
                            onEvent(
                                when {
                                    draft == null -> EditorEvent.Create
                                    add -> EditorEvent.Pin
                                    else -> EditorEvent.Save
                                }
                            )
                        },
                        enabled =
                            !state.loading &&
                                !state.actionBusy &&
                                (draft == null || state.dirty || add),
                        shape = RoundedCornerShape(12.dp),
                        modifier =
                            Modifier.fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 12.dp)
                                .heightIn(min = 48.dp),
                    ) {
                        if (state.actionBusy)
                            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        else
                            Text(
                                S(
                                    when {
                                        draft == null -> R.string.new_widget
                                        add -> R.string.add_home
                                        else -> R.string.save_action
                                    }
                                )
                            )
                    }
                }
                Box(
                    Modifier.align(Alignment.CenterStart)
                        .height(200.dp)
                        .width(16.dp)
                        .systemGestureExclusion()
                        .pointerInput(drawer) {
                            var distance = 0f
                            detectHorizontalDragGestures(
                                onDragStart = { distance = 0f },
                                onHorizontalDrag = { change, amount ->
                                    change.consume()
                                    distance += amount
                                },
                                onDragEnd = {
                                    if (distance > 40.dp.toPx()) scope.launch { drawer.open() }
                                },
                            )
                        }
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
        { dialog = it },
        { deleteTarget = it },
    )
}
