package com.thebifel.kadr.presentation.editor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thebifel.kadr.R
import com.thebifel.kadr.domain.model.*
import com.thebifel.kadr.presentation.theme.*
import kotlin.math.roundToInt

@Composable
internal fun EditorCode(
    draft: Project,
    expanded: Boolean,
    onToggle: () -> Unit,
    onEvent: (EditorEvent) -> Unit,
    platform: EditorPlatformActions,
) {
    Text(S(R.string.widget_code), fontSize = 13.sp)
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf(
                Triple(
                    if (expanded) R.string.collapse_code else R.string.edit_code,
                    R.drawable.ic_code,
                    onToggle,
                ),
                Triple(R.string.import_action, R.drawable.ic_import, platform.importHtml),
                Triple(
                    R.string.export_action,
                    R.drawable.ic_export,
                    { platform.exportHtml(draft.html) },
                ),
            )
            .forEach { (label, icon, action) ->
                OutlinedButton(
                    onClick = action,
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(9.dp),
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                ) {
                    EditorIcon(icon, null, Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(S(label), fontSize = 11.sp, maxLines = 1)
                }
            }
    }
    if (expanded)
        OutlinedTextField(
            value = draft.html,
            onValueChange = {
                if (it.length <= WidgetPolicy.maxHtmlLength)
                    onEvent(EditorEvent.Edit(draft.copy(html = it)))
            },
            modifier =
                Modifier.fillMaxWidth().height(320.dp).semantics {
                    contentDescription = "HTML CSS JavaScript"
                },
            textStyle =
                LocalTextStyle.current.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    lineHeight = 19.sp,
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
}

@Composable
internal fun EditorSettings(draft: Project, onEvent: (EditorEvent) -> Unit) {
    val context = LocalContext.current
    fun edit(next: Project) = onEvent(EditorEvent.Edit(next))
    OutlinedTextField(
        value = draft.name,
        onValueChange = {
            if (it.length <= WidgetPolicy.maxNameLength) edit(draft.copy(name = it))
        },
        label = { Text(S(R.string.widget_name)) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
    )
    Text(S(R.string.appearance), fontSize = 12.sp, color = Muted)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(R.string.auto_theme, R.string.light_theme, R.string.dark_theme).forEachIndexed {
            index,
            label ->
            FilterChip(
                selected =
                    when (index) {
                        0 -> draft.autoTheme
                        1 -> !draft.autoTheme && !draft.dark
                        else -> !draft.autoTheme && draft.dark
                    },
                onClick = { edit(draft.copy(autoTheme = index == 0, dark = index == 2)) },
                label = { Text(S(label), fontSize = 12.sp) },
            )
        }
    }
    val cornerLabel = S(R.string.corners)
    val cornerValue = context.getString(R.string.corner_radius_value, draft.radius)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(cornerLabel, fontSize = 12.sp, color = Muted)
        Text(cornerValue, fontSize = 12.sp, color = Ink)
    }
    Slider(
        value = draft.radius.toFloat(),
        onValueChange = { value ->
            val radius = value.roundToInt().coerceIn(0, WidgetPolicy.maxCornerRadiusDp)
            if (radius != draft.radius) edit(draft.copy(radius = radius))
        },
        valueRange = 0f..WidgetPolicy.maxCornerRadiusDp.toFloat(),
        steps = WidgetPolicy.maxCornerRadiusDp - 1,
        modifier =
            Modifier.fillMaxWidth().semantics {
                contentDescription = cornerLabel
                stateDescription = cornerValue
            },
    )
    Text(S(R.string.updates), fontSize = 12.sp, color = Muted)
    Column {
        WidgetPolicy.intervals.chunked(4).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { interval ->
                    FilterChip(
                        selected = draft.interval == interval,
                        onClick = { edit(draft.copy(interval = interval)) },
                        label = { Text(intervalText(context, interval), fontSize = 11.sp) },
                    )
                }
            }
        }
    }
    CompactToggle(S(R.string.wifi_only), draft.wifi) { edit(draft.copy(wifi = it)) }
    CompactToggle(S(R.string.battery_saver), draft.battery) { edit(draft.copy(battery = it)) }
}
