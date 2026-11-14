package com.thebifel.kadr.presentation.editor

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thebifel.kadr.R
import com.thebifel.kadr.domain.model.*
import com.thebifel.kadr.presentation.theme.*

@Composable
internal fun EditorSettings(
    tab: Int,
    draft: Project,
    onEvent: (EditorEvent) -> Unit,
    platform: EditorPlatformActions,
    onPresets: () -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    fun edit(next: Project) = onEvent(EditorEvent.Edit(next))
    when (tab) {
        0 -> {
            Text(S(R.string.your_page), fontSize = 17.sp, fontWeight = FontWeight.Medium)
            Text(S(R.string.content_hint), fontSize = 12.sp, color = Muted, lineHeight = 19.sp)
            OutlinedTextField(
                value = draft.name,
                onValueChange = { if (it.length <= 80) edit(draft.copy(name = it)) },
                label = { Text(S(R.string.widget_name)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            OutlinedButton(
                onClick = { platform.copyAi() },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
            ) {
                Text(S(R.string.copy_ai), fontSize = 12.sp)
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "index.html",
                    fontSize = 12.sp,
                    color = Accent,
                    fontFamily = FontFamily.Monospace,
                )
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { onPresets() }) {
                    Text(S(R.string.choose_template), fontSize = 11.sp)
                }
            }
            OutlinedTextField(
                value = draft.html,
                onValueChange = { if (it.length <= 500_000) edit(draft.copy(html = it)) },
                modifier =
                    Modifier.fillMaxWidth().height(320.dp).semantics {
                        contentDescription = "HTML CSS JavaScript"
                    },
                textStyle =
                    LocalTextStyle.current.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        lineHeight = 18.sp,
                        color = Ink,
                    ),
                keyboardOptions =
                    KeyboardOptions(
                        capitalization = KeyboardCapitalization.None,
                        autoCorrectEnabled = false,
                        keyboardType = KeyboardType.Ascii,
                    ),
                shape = RoundedCornerShape(10.dp),
            )
            Notice(S(R.string.vue_hint))
        }
        1 -> {
            Text(S(R.string.appearance_title), fontSize = 17.sp, fontWeight = FontWeight.Medium)
            Text(S(R.string.appearance_hint), fontSize = 12.sp, color = Muted, lineHeight = 19.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = draft.autoTheme,
                    onClick = {
                        edit(draft.copy(autoTheme = true))
                        onEvent(EditorEvent.Preview)
                    },
                    label = { Text(S(R.string.auto_theme)) },
                )
                FilterChip(
                    selected = !draft.autoTheme && !draft.dark,
                    onClick = {
                        edit(draft.copy(autoTheme = false, dark = false))
                        onEvent(EditorEvent.Preview)
                    },
                    label = { Text(S(R.string.light_theme)) },
                )
                FilterChip(
                    selected = !draft.autoTheme && draft.dark,
                    onClick = {
                        edit(draft.copy(autoTheme = false, dark = true))
                        onEvent(EditorEvent.Preview)
                    },
                    label = { Text(S(R.string.dark_theme)) },
                )
            }
            Text(S(R.string.corners), fontSize = 12.sp, color = Muted)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf(0, 16, 24).forEach { r ->
                    FilterChip(
                        selected = draft.radius == r,
                        onClick = { edit(draft.copy(radius = r)) },
                        label = { Text("$r dp") },
                    )
                }
            }
            Notice(S(R.string.footer_hint))
        }
        2 -> {
            Text(S(R.string.updates_title), fontSize = 17.sp, fontWeight = FontWeight.Medium)
            Text(S(R.string.preferred_interval), fontSize = 12.sp, color = Muted)
            Column {
                listOf(listOf(0, 15, 30, 60), listOf(180, 360, 1440)).forEach { row ->
                    Row(
                        Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        row.forEach { minutes ->
                            FilterChip(
                                selected = draft.interval == minutes,
                                onClick = { edit(draft.copy(interval = minutes)) },
                                label = { Text(intervalText(context, minutes), fontSize = 11.sp) },
                            )
                        }
                    }
                }
            }
            ToggleRow(S(R.string.wifi_only), S(R.string.wifi_hint), draft.wifi) {
                edit(draft.copy(wifi = it))
            }
            ToggleRow(S(R.string.battery_saver), S(R.string.battery_hint), draft.battery) {
                edit(draft.copy(battery = it))
            }
            Notice(S(R.string.interval_hint))
        }
    }
}
