package com.thebifel.kadr.presentation.editor

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thebifel.kadr.R
import com.thebifel.kadr.data.rendering.asBitmap
import com.thebifel.kadr.platform.widget.WidgetPresentation
import com.thebifel.kadr.presentation.theme.*

@Composable
internal fun WidgetPreview(
    state: EditorUiState,
    onEvent: (EditorEvent) -> Unit,
    onCopyError: (String) -> Unit,
) {
    val image = state.image?.asBitmap()?.asImageBitmap()
    val draft = checkNotNull(state.draft)
    val busy = state.previewBusy
    val error =
        state.operationError
            .ifEmpty { state.previewError }
            .ifEmpty { if (!state.showDraft) state.frame.error else "" }
    var details by remember(state.selectedId, error) { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(S(R.string.widget_preview), color = Muted, fontSize = 11.sp)
            Text("4 × 2", color = Muted, fontSize = 11.sp)
        }
        val cardBg = Color(if (state.effectiveDark) 0xFF222B1B else 0xFFF2F4E9)
        val cardMuted = Color(if (state.effectiveDark) 0xFFB8C9A4 else 0xFF69765B)
        val widgetShape = RoundedCornerShape(draft.radius.dp)
        Box(Modifier.fillMaxWidth()) {
            Box(
                Modifier.fillMaxWidth()
                    .aspectRatio(if (image != null) image.width.toFloat() / image.height else 2f)
                    .clip(widgetShape)
                    .background(cardBg)
                    .border(1.dp, Border.copy(alpha = .45f), widgetShape)
            ) {
                if (image != null)
                    Image(
                        image,
                        S(R.string.frame_content),
                        Modifier.matchParentSize(),
                        contentScale = ContentScale.FillBounds,
                    )
                Row(
                    Modifier.align(Alignment.BottomEnd)
                        .width(144.dp)
                        .height(56.dp)
                        .padding(end = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        WidgetPresentation.stamp(state.previewTime),
                        Modifier.weight(1f),
                        color = cardMuted,
                        fontSize = 10.sp,
                        maxLines = 1,
                        textAlign = TextAlign.End,
                    )
                    IconButton(
                        onClick = { onEvent(EditorEvent.Preview) },
                        enabled = !busy && !state.actionBusy,
                        modifier = Modifier.size(48.dp),
                    ) {
                        if (busy)
                            CircularProgressIndicator(
                                Modifier.size(20.dp),
                                color = cardMuted,
                                strokeWidth = 2.dp,
                            )
                        else
                            EditorIcon(
                                R.drawable.ic_refresh,
                                S(R.string.refresh_preview),
                                Modifier.size(20.dp),
                                Color(0xFF7E9C4D),
                            )
                    }
                }
            }
        }
        if (state.previewStale || error.isNotEmpty())
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End,
            ) {
                if (state.previewStale)
                    Text(S(R.string.preview_stale), color = Muted, fontSize = 11.sp)
                if (error.isNotEmpty()) {
                    IconButton(onClick = { onCopyError(error) }) {
                        EditorIcon(
                            R.drawable.ic_copy,
                            S(R.string.copy_error),
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                    IconButton(onClick = { details = !details }) {
                        EditorIcon(
                            R.drawable.ic_warning,
                            S(R.string.error_details),
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        if (details && error.isNotEmpty())
            Text(
                error,
                color = MaterialTheme.colorScheme.error,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                modifier = Modifier.fillMaxWidth().padding(8.dp),
            )
    }
}
