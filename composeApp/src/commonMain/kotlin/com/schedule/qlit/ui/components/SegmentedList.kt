package com.schedule.qlit.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.schedule.qlit.ui.design.QDivider
import com.schedule.qlit.ui.design.QList
import com.schedule.qlit.ui.design.QRadioRow
import com.schedule.qlit.ui.design.QSwitchRow
import com.schedule.qlit.ui.design.qRowClickable
import com.schedule.qlit.ui.theme.Q
import com.schedule.qlit.ui.theme.QType
import top.yukonga.miuix.kmp.theme.LocalContentColor
import androidx.compose.material3.LocalContentColor as MaterialLocalContentColor
import androidx.compose.material3.LocalTextStyle as MaterialLocalTextStyle

/** White grouped list (see [QList]). */
@Composable
fun SegmentedColumn(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    QList(modifier = modifier, content = content)
}

/** Slot-based list row; headline renders as row title, supporting text in ink2. */
@Composable
fun SegmentedListItem(
    modifier: Modifier = Modifier,
    headlineContent: @Composable () -> Unit,
    supportingContent: (@Composable () -> Unit)? = null,
    leadingContent: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.qRowClickable(onClick = onClick) else Modifier)
            .heightIn(min = 56.dp)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        leadingContent?.let { leading ->
            CompositionLocalProvider(LocalContentColor provides Q.colors.ink2) { leading() }
        }
        Column(modifier = Modifier.weight(1f)) {
            ProvideRowText(QType.rowTitle) {
                CompositionLocalProvider(LocalContentColor provides Q.colors.ink, MaterialLocalContentColor provides Q.colors.ink) { headlineContent() }
            }
            supportingContent?.let { supporting ->
                ProvideRowText(QType.sub) {
                    CompositionLocalProvider(LocalContentColor provides Q.colors.ink2, MaterialLocalContentColor provides Q.colors.ink2) { supporting() }
                }
            }
        }
        trailingContent?.let { trailing ->
            CompositionLocalProvider(LocalContentColor provides Q.colors.ink2) { trailing() }
        }
    }
}

@Composable
private fun ProvideRowText(style: TextStyle, content: @Composable () -> Unit) {
    CompositionLocalProvider(MaterialLocalTextStyle provides style, content = content)
}

@Composable
fun SegmentedRadioItem(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    summary: String? = null,
) {
    QRadioRow(title = title, selected = selected, onClick = onClick, modifier = modifier, subtitle = summary)
}

@Composable
fun SegmentedSwitchItem(
    icon: ImageVector? = null,
    title: String,
    summary: String? = null,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
) {
    QSwitchRow(
        title = title,
        checked = checked,
        onCheckedChange = onCheckedChange,
        subtitle = summary,
        icon = icon,
        enabled = enabled,
    )
}

@Composable
fun SegmentedDivider(modifier: Modifier = Modifier) {
    QDivider(modifier = modifier, startInset = 14.dp)
}
