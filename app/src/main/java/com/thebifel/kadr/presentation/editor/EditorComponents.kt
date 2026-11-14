package com.thebifel.kadr.presentation.editor

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thebifel.kadr.R
import com.thebifel.kadr.domain.model.*
import com.thebifel.kadr.platform.LocaleSupport
import com.thebifel.kadr.presentation.theme.*

fun intervalText(context: Context, value: Int): String {
    val c = LocaleSupport.localized(context)
    return when {
        value == 0 -> c.getString(R.string.manual_interval)
        value == 1440 -> c.getString(R.string.daily_interval)
        value >= 60 -> c.getString(R.string.hours_interval, value / 60)
        else -> c.getString(R.string.minutes_interval, value)
    }
}

@Composable
internal fun Notice(text: String, warning: Boolean = false) {
    val colors = MaterialTheme.colorScheme
    Text(
        text,
        fontSize = 11.sp,
        lineHeight = 18.sp,
        color = if (warning) colors.onErrorContainer else colors.onPrimaryContainer,
        modifier =
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(if (warning) colors.errorContainer else colors.primaryContainer)
                .border(1.dp, Border, RoundedCornerShape(10.dp))
                .padding(14.dp),
    )
}

@Composable
internal fun ToggleRow(
    title: String,
    description: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(title, fontSize = 13.sp)
            Text(description, fontSize = 11.sp, color = Muted, lineHeight = 17.sp)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
