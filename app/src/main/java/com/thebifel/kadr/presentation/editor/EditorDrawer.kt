package com.thebifel.kadr.presentation.editor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thebifel.kadr.R
import com.thebifel.kadr.presentation.theme.*

@Composable
internal fun EditorDrawer(
    page: String,
    onPage: (String) -> Unit,
    onClose: () -> Unit,
    onEvent: (EditorEvent) -> Unit,
    platform: EditorPlatformActions,
) {
    ModalDrawerSheet(modifier = Modifier.fillMaxWidth(.86f)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
            if (page != "menu")
                IconButton(onClick = { onPage("menu") }) {
                    EditorIcon(R.drawable.ic_back, S(R.string.back_to_menu))
                }
            Text(
                S(
                    when (page) {
                        "presets" -> R.string.templates
                        "ai" -> R.string.ai_instructions
                        "contract" -> R.string.code_reference
                        else -> R.string.app_name
                    }
                ),
                modifier = Modifier.weight(1f).padding(vertical = 12.dp),
                fontSize = 18.sp,
            )
            IconButton(onClick = onClose) { EditorIcon(R.drawable.ic_close, S(R.string.close)) }
        }
        HorizontalDivider(color = Border.copy(alpha = .5f))
        key(page) {
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                when (page) {
                    "menu" -> {
                        listOf(
                                Triple("presets", R.string.templates, R.drawable.ic_grid),
                                Triple("ai", R.string.ai_instructions, R.drawable.ic_code),
                                Triple("contract", R.string.code_reference, R.drawable.ic_code),
                            )
                            .forEach { (key, label, icon) ->
                                NavigationDrawerItem(
                                    label = { Text(S(label), fontSize = 14.sp) },
                                    icon = { EditorIcon(icon, null) },
                                    selected = false,
                                    onClick = { onPage(key) },
                                )
                            }
                    }
                    "presets" ->
                        listOf(
                                R.string.preset_power to "power",
                                R.string.preset_weather to "weather",
                                R.string.preset_blank to "blank",
                            )
                            .forEach { (label, preset) ->
                                TextButton(
                                    onClick = {
                                        onEvent(EditorEvent.Preset(preset))
                                        onClose()
                                    },
                                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                                ) {
                                    Text(S(label))
                                }
                            }
                    else ->
                        SelectionContainer {
                            Text(
                                if (page == "ai") platform.aiText else platform.contractText,
                                fontSize = 12.sp,
                                lineHeight = 19.sp,
                            )
                        }
                }
            }
        }
        if (page == "ai" || page == "contract")
            Button(
                onClick = { if (page == "ai") platform.copyAi() else platform.copyContract() },
                modifier = Modifier.fillMaxWidth().padding(20.dp).heightIn(min = 48.dp),
            ) {
                Text(S(if (page == "ai") R.string.copy_ai else R.string.copy_contract))
            }
    }
}
