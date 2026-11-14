package com.thebifel.kadr.presentation.editor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thebifel.kadr.R
import com.thebifel.kadr.domain.model.*
import com.thebifel.kadr.presentation.theme.*

@Composable
internal fun EditorDialogs(
    dialog: String,
    deleteTarget: Project?,
    state: EditorUiState,
    onEvent: (EditorEvent) -> Unit,
    platform: EditorPlatformActions,
    onDialog: (String) -> Unit,
    onDeleteTarget: (Project?) -> Unit,
) {
    val projects = state.projects
    val selected = state.selectedId
    when (dialog) {
        "projects" ->
            AlertDialog(
                onDismissRequest = { onDialog("") },
                title = { Text(S(R.string.my_widgets)) },
                text = {
                    Column(
                        Modifier.fillMaxWidth()
                            .heightIn(max = 380.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        projects.forEach { p ->
                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                TextButton(
                                    onClick = {
                                        onEvent(EditorEvent.Select(p.id))
                                        onDialog("")
                                    },
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Text(
                                        (if (p.id == selected) "● " else "") + p.name,
                                        modifier = Modifier.fillMaxWidth(),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        onDeleteTarget(p)
                                        onDialog("delete")
                                    }
                                ) {
                                    Icon(
                                        painterResource(R.drawable.ic_delete),
                                        S(R.string.delete_widget_name, p.name),
                                        tint = Muted,
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            onEvent(EditorEvent.Create)
                            onDialog("")
                        }
                    ) {
                        Text(S(R.string.new_widget))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { onDialog("") }) { Text(S(R.string.close)) }
                },
            )
        "delete" ->
            deleteTarget?.let { target ->
                val bound = target.id in state.boundProjectIds
                AlertDialog(
                    onDismissRequest = {
                        onDeleteTarget(null)
                        onDialog("projects")
                    },
                    title = { Text(S(R.string.delete_widget_title)) },
                    text = {
                        Text(
                            S(
                                if (bound) R.string.delete_bound_message
                                else R.string.delete_widget_message,
                                target.name,
                            )
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                onEvent(EditorEvent.Delete(target.id))
                                onDeleteTarget(null)
                                onDialog("projects")
                            }
                        ) {
                            Text(S(R.string.delete_action), color = MaterialTheme.colorScheme.error)
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = {
                                onDeleteTarget(null)
                                onDialog("projects")
                            }
                        ) {
                            Text(S(R.string.cancel))
                        }
                    },
                )
            }
        "presets" ->
            AlertDialog(
                onDismissRequest = { onDialog("") },
                title = { Text(S(R.string.preset_title)) },
                text = {
                    Column {
                        Text(S(R.string.preset_hint), fontSize = 12.sp, color = Muted)
                        listOf(
                                R.string.preset_power to "power",
                                R.string.preset_weather to "weather",
                                R.string.preset_blank to "blank",
                            )
                            .forEach { (name, kind) ->
                                TextButton(
                                    onClick = {
                                        onDialog("")
                                        onEvent(EditorEvent.Preset(kind))
                                    }
                                ) {
                                    Text(S(name))
                                }
                            }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { onDialog("") }) { Text(S(R.string.close)) }
                },
            )
        "contract" ->
            AlertDialog(
                onDismissRequest = { onDialog("") },
                title = { Text(S(R.string.contract_title)) },
                text = {
                    Column(Modifier.heightIn(max = 380.dp).verticalScroll(rememberScrollState())) {
                        Text(platform.contractText, fontSize = 12.sp, lineHeight = 19.sp)
                    }
                },
                confirmButton = {
                    TextButton(onClick = { platform.copyContract() }) {
                        Text(S(R.string.copy_contract))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { onDialog("") }) { Text(S(R.string.close)) }
                },
            )
    }
}
