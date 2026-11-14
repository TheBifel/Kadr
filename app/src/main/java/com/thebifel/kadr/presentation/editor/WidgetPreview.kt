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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thebifel.kadr.R
import com.thebifel.kadr.data.rendering.asBitmap
import com.thebifel.kadr.domain.model.*
import com.thebifel.kadr.platform.widget.WidgetPresentation
import com.thebifel.kadr.presentation.theme.*

@Composable
internal fun WidgetPreview(state: EditorUiState, onEvent: (EditorEvent) -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val image = state.image?.asBitmap()?.asImageBitmap()
    val frame = state.frame
    val draft = checkNotNull(state.draft)
    val effectiveDark = state.effectiveDark
    val showDraft = state.showDraft
    val previewBusy = state.previewBusy
    val dirty = state.dirty
    Column(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, Border, RoundedCornerShape(20.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(17.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                S(if (showDraft) R.string.draft_preview else R.string.preview_title),
                fontSize = 9.sp,
                color = Muted,
                letterSpacing = 1.3.sp,
            )
            Spacer(Modifier.weight(1f))
            Text("4 × 2", fontSize = 10.sp, color = Accent, fontFamily = FontFamily.Monospace)
        }
        val cardBg = Color(if (effectiveDark) 0xFF222B1B else 0xFFF2F4E9)
        val cardMuted = Color(if (effectiveDark) 0xFFB8C9A4 else 0xFF69765B)
        val ratio = if (image != null) image.width.toFloat() / image.height else 2f
        Box(
            Modifier.fillMaxWidth()
                .aspectRatio(ratio)
                .clip(RoundedCornerShape(draft.radius.dp))
                .background(cardBg)
        ) {
            if (image != null)
                Image(
                    image,
                    S(R.string.frame_content),
                    Modifier.matchParentSize(),
                    contentScale = ContentScale.FillBounds,
                )
            else
                Box(Modifier.matchParentSize(), contentAlignment = Alignment.Center) {
                    Text(
                        S(if (previewBusy) R.string.building_preview else R.string.first_frame),
                        fontSize = 13.sp,
                        color = cardMuted,
                    )
                }
            Row(
                Modifier.align(Alignment.BottomEnd)
                    .width(WidgetPolicy.controlsWidth.dp)
                    .height(WidgetPolicy.controlsHeight.dp)
                    .padding(end = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    if (showDraft) S(R.string.draft_label)
                    else WidgetPresentation.overlayStamp(frame),
                    modifier = Modifier.weight(1f),
                    color = cardMuted,
                    fontSize = 10.sp,
                    maxLines = 1,
                    textAlign = TextAlign.End,
                )
                IconButton(
                    onClick = {
                        if (dirty) onEvent(EditorEvent.Preview) else onEvent(EditorEvent.Refresh)
                    },
                    enabled = !previewBusy,
                    modifier =
                        Modifier.size(48.dp).semantics {
                            contentDescription = context.getString(R.string.update_frame)
                        },
                ) {
                    Text("↻", fontSize = 26.sp, color = Color(0xFF7E9C4D))
                }
            }
        }
        Text(S(R.string.static_caption), fontSize = 10.sp, color = Muted)
    }
}
