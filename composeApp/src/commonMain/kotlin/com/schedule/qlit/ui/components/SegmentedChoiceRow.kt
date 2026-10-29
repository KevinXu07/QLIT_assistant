package com.schedule.qlit.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.schedule.qlit.ui.design.QSegmented

/** Compact single-choice control. */
@Composable
fun SegmentedChoiceRow(
    options: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (options.isEmpty()) return
    QSegmented(
        options = options,
        selectedIndex = selectedIndex.coerceIn(0, options.lastIndex),
        onSelect = onSelected,
        modifier = modifier,
    )
}
