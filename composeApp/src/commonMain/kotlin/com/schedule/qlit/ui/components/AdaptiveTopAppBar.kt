package com.schedule.qlit.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.schedule.qlit.ui.design.QIconButton
import com.schedule.qlit.ui.design.QText
import com.schedule.qlit.ui.design.QTopBar
import com.schedule.qlit.ui.design.qClickable
import com.schedule.qlit.ui.theme.Q
import com.schedule.qlit.ui.theme.QType
import androidx.compose.material3.LocalContentColor as MaterialLocalContentColor
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator as MiuixInfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.RadioButton as MiuixRadioButton
import top.yukonga.miuix.kmp.basic.ScrollBehavior as MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.SnackbarHost as MiuixSnackbarHost
import top.yukonga.miuix.kmp.basic.SnackbarHostState as MiuixSnackbarHostState
import top.yukonga.miuix.kmp.basic.Switch as MiuixSwitch
import top.yukonga.miuix.kmp.basic.TextField as MiuixTextField
import top.yukonga.miuix.kmp.basic.TextFieldDefaults as MiuixTextFieldDefaults
import top.yukonga.miuix.kmp.preference.OverlayDropdownPreference as MiuixOverlayDropdownPreference
import top.yukonga.miuix.kmp.theme.LocalContentColor

/** Page header in the redesign style (large title that folds into the bar). */
@Composable
fun AdaptiveTopAppBar(
    miuixTitle: String,
    @Suppress("UNUSED_PARAMETER") title: @Composable () -> Unit = {},
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    miuixColor: Color? = null,
    @Suppress("UNUSED_PARAMETER") scrollBehavior: Any? = null,
    miuixScrollBehavior: MiuixScrollBehavior? = null,
    subtitle: String? = null,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
) {
    QTopBar(
        title = miuixTitle,
        modifier = modifier,
        subtitle = subtitle,
        scrollBehavior = miuixScrollBehavior,
        navigationIcon = navigationIcon,
        actions = actions,
        color = miuixColor ?: Q.colors.bg,
        largeTitle = !compact,
    )
}

@Composable
fun AdaptiveIconButton(
    onClick: () -> Unit,
    materialIcon: ImageVector,
    modifier: Modifier = Modifier,
    miuixIcon: ImageVector = materialIcon,
    contentDescription: String? = null,
    enabled: Boolean = true,
) {
    QIconButton(
        icon = miuixIcon,
        contentDescription = contentDescription,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        tint = Q.colors.ink,
    )
}

@Composable
fun AdaptiveTextButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    QText(
        text = text,
        modifier = Modifier
            .qClickable(enabled = enabled, onClick = onClick)
            .clip(CircleShape)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        style = QType.label.copy(fontSize = 15.sp),
        color = if (enabled) Q.colors.ink else Q.colors.ink3,
        maxLines = 1,
    )
}

/** Button with a content slot; `primary` is ink, otherwise paper-tonal. */
@Composable
fun AdaptiveButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    primary: Boolean = false,
    content: @Composable RowScope.() -> Unit,
) {
    val bg = if (primary) Q.colors.ink else Q.colors.surface2
    val fg = if (primary) Q.colors.surface else Q.colors.ink
    CompositionLocalProvider(
        LocalContentColor provides if (enabled) fg else fg.copy(alpha = 0.5f),
        MaterialLocalContentColor provides if (enabled) fg else fg.copy(alpha = 0.5f),
    ) {
        Row(
            modifier = modifier
                .heightIn(min = 48.dp)
                .qClickable(enabled = enabled, onClick = onClick)
                .clip(RoundedCornerShape(16.dp))
                .background(if (enabled) bg else bg.copy(alpha = bg.alpha * 0.45f))
                .padding(horizontal = 18.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }
}

@Composable
fun AdaptiveFloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit,
    text: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalContentColor provides Q.colors.surface,
        MaterialLocalContentColor provides Q.colors.surface,
    ) {
        Row(
            modifier = modifier
                .qClickable(onClick = onClick)
                .shadow(10.dp, CircleShape, ambientColor = Color.Black.copy(alpha = 0.2f), spotColor = Color.Black.copy(alpha = 0.2f))
                .clip(CircleShape)
                .background(Q.colors.ink)
                .heightIn(min = 52.dp)
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            icon()
            Spacer(modifier = Modifier.width(8.dp))
            text()
        }
    }
}

/** Paper-filled text field. The label doubles as placeholder; [supportingText] renders below. */
@Composable
fun AdaptiveTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    readOnly: Boolean = false,
    trailingIcon: (@Composable () -> Unit)? = null,
    @Suppress("UNUSED_PARAMETER") placeholder: (@Composable () -> Unit)? = null,
    supportingText: (@Composable () -> Unit)? = null,
    insideMargin: DpSize = DpSize(16.dp, 16.dp),
) {
    val field: @Composable (Modifier) -> Unit = { fieldModifier ->
        MiuixTextField(
            value = value,
            onValueChange = onValueChange,
            label = label,
            useLabelAsPlaceholder = true,
            modifier = fieldModifier,
            readOnly = readOnly,
            singleLine = singleLine,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            visualTransformation = visualTransformation,
            trailingIcon = trailingIcon,
            insideMargin = insideMargin,
            colors = MiuixTextFieldDefaults.textFieldColors(
                backgroundColor = Q.colors.surface2,
                labelColor = Q.colors.ink3,
                borderColor = Q.colors.ink,
            ),
            cornerRadius = 16.dp,
        )
    }
    if (supportingText != null) {
        Column(modifier = modifier) {
            field(Modifier.fillMaxWidth())
            Box(Modifier.padding(start = 8.dp, top = 6.dp)) {
                CompositionLocalProvider(LocalContentColor provides Q.colors.ink3) { supportingText() }
            }
        }
    } else {
        field(modifier)
    }
}

@Composable
fun AdaptiveDropdownField(
    label: String,
    options: List<String>,
    selectedIndex: Int,
    onSelectedIndexChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    insideMargin: PaddingValues = PaddingValues(16.dp),
) {
    if (options.isEmpty()) return
    MiuixOverlayDropdownPreference(
        modifier = modifier,
        title = label,
        items = options,
        selectedIndex = selectedIndex.coerceIn(options.indices),
        onSelectedIndexChange = onSelectedIndexChange,
        insideMargin = insideMargin,
    )
}

@Composable
fun AdaptiveRadioButton(
    selected: Boolean,
    onClick: () -> Unit,
) {
    MiuixRadioButton(
        selected = selected,
        onClick = onClick,
        modifier = Modifier.size(48.dp),
    )
}

@Composable
fun AdaptiveProgressIndicator(
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    @Suppress("UNUSED_PARAMETER") strokeWidth: androidx.compose.ui.unit.Dp = 3.dp,
) {
    MiuixInfiniteProgressIndicator(
        modifier = modifier,
        color = if (color == Color.Unspecified) Q.colors.ink2 else color,
    )
}

@Composable
fun AdaptiveSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
) {
    MiuixSwitch(
        checked = checked,
        enabled = enabled,
        onCheckedChange = onCheckedChange,
    )
}

@Stable
class AdaptiveSnackbarHostState internal constructor(
    internal val miuixState: MiuixSnackbarHostState,
) {
    suspend fun showSnackbar(message: String) {
        miuixState.showSnackbar(message)
    }
}

@Composable
fun rememberAdaptiveSnackbarHostState(): AdaptiveSnackbarHostState =
    remember { AdaptiveSnackbarHostState(MiuixSnackbarHostState()) }

@Composable
fun AdaptiveSnackbarHost(
    state: AdaptiveSnackbarHostState,
) {
    MiuixSnackbarHost(state = state.miuixState)
}
