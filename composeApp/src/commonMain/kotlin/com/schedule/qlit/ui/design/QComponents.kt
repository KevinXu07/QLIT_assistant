package com.schedule.qlit.ui.design

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.schedule.qlit.ui.theme.Q
import com.schedule.qlit.ui.theme.QType
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.theme.LocalContentColor

// ── Text ─────────────────────────────────────────────────────────────

@Composable
fun QText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = QType.body,
    color: Color = Color.Unspecified,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Ellipsis,
    textAlign: TextAlign? = null,
    softWrap: Boolean = true,
) {
    val resolved = if (color != Color.Unspecified) color else style.color.takeIf { it != Color.Unspecified } ?: LocalContentColor.current
    BasicText(
        text = text,
        modifier = modifier,
        style = style.merge(color = resolved, textAlign = textAlign ?: TextAlign.Unspecified),
        overflow = overflow,
        softWrap = softWrap,
        maxLines = maxLines,
    )
}

@Composable
fun QText(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    style: TextStyle = QType.body,
    color: Color = Color.Unspecified,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Ellipsis,
    textAlign: TextAlign? = null,
) {
    val resolved = if (color != Color.Unspecified) color else LocalContentColor.current
    BasicText(
        text = text,
        modifier = modifier,
        style = style.merge(color = resolved, textAlign = textAlign ?: TextAlign.Unspecified),
        overflow = overflow,
        maxLines = maxLines,
    )
}

// ── Interaction ──────────────────────────────────────────────────────

/** Click with a short spring "sink" instead of a ripple. */
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.qClickable(
    enabled: Boolean = true,
    pressScale: Float = 0.97f,
    role: Role = Role.Button,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit,
): Modifier = composed {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) pressScale else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 700f),
        label = "qPress",
    )
    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .combinedClickable(
            interactionSource = interaction,
            indication = null,
            enabled = enabled,
            role = role,
            onLongClick = onLongClick,
            onClick = onClick,
        )
}

/** Click that tints the row background while pressed (list rows). */
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.qRowClickable(
    enabled: Boolean = true,
    role: Role = Role.Button,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit,
): Modifier = composed {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val highlight by animateColorAsState(
        targetValue = if (pressed && enabled) Q.colors.line else Color.Transparent,
        label = "qRowPress",
    )
    this
        .background(highlight)
        .combinedClickable(
            interactionSource = interaction,
            indication = null,
            enabled = enabled,
            role = role,
            onLongClick = onLongClick,
            onClick = onClick,
        )
}

// ── Surfaces ─────────────────────────────────────────────────────────

val QCardShape = RoundedCornerShape(24.dp)
val QListShape = RoundedCornerShape(22.dp)

@Composable
fun QCard(
    modifier: Modifier = Modifier,
    color: Color = Q.colors.surface,
    shape: Shape = QCardShape,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .then(if (onClick != null || onLongClick != null) Modifier.qClickable(onLongClick = onLongClick, onClick = onClick ?: {}) else Modifier)
            .clip(shape)
            .background(color)
            .padding(contentPadding),
        content = content,
    )
}

/** White grouped list block; put [QRow]s and [QDivider]s inside. */
@Composable
fun QList(
    modifier: Modifier = Modifier,
    color: Color = Q.colors.surface,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(QListShape)
            .background(color),
        content = content,
    )
}

/** A titled list section, 16dp from the screen edges. */
@Composable
fun QGroup(
    title: String?,
    modifier: Modifier = Modifier,
    footer: String? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        if (title != null || trailing != null) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                QText(
                    text = title.orEmpty(),
                    modifier = Modifier.weight(1f),
                    style = QType.label,
                    color = Q.colors.ink2,
                )
                trailing?.invoke(this)
            }
        }
        QList(content = content)
        if (footer != null) {
            QText(
                text = footer,
                modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = 8.dp),
                style = QType.caption,
                color = Q.colors.ink3,
            )
        }
    }
}

@Composable
fun QDivider(modifier: Modifier = Modifier, startInset: Dp = 64.dp, endInset: Dp = 0.dp) {
    val color = Q.colors.line
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = startInset, end = endInset)
            .height(1.dp)
            .background(color),
    )
}

// ── Icons ────────────────────────────────────────────────────────────

/** Rounded tile with a hue-tinted background and glyph (tools, settings). */
@Composable
fun QTintIcon(
    icon: ImageVector,
    hue: Float,
    modifier: Modifier = Modifier,
    size: Dp = 38.dp,
    iconSize: Dp = 21.dp,
    shape: Shape = RoundedCornerShape(11.dp),
    badge: Int? = null,
) {
    val tint = Q.colors.hue(hue)
    Box(modifier = modifier.size(size)) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(shape)
                .background(tint.iconContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(iconSize), tint = tint.icon)
        }
        if (badge != null && badge > 0) {
            QBadge(
                count = badge,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 5.dp, y = (-3).dp),
            )
        }
    }
}

@Composable
fun QBadge(count: Int, modifier: Modifier = Modifier, ringColor: Color = Q.colors.surface) {
    Box(
        modifier = modifier
            .heightIn(min = 19.dp)
            .widthIn(min = 19.dp)
            .border(2.5.dp, ringColor, CircleShape)
            .padding(1.dp)
            .clip(CircleShape)
            .background(Q.colors.seal)
            .padding(horizontal = 5.dp),
        contentAlignment = Alignment.Center,
    ) {
        QText(
            text = if (count > 99) "99+" else count.toString(),
            style = QType.num(12.5.sp),
            color = Q.colors.onSeal,
        )
    }
}

@Composable
fun QIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = Q.colors.ink2,
    background: Color = Color.Transparent,
    size: Dp = 40.dp,
    iconSize: Dp = 22.dp,
    enabled: Boolean = true,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(background)
            .qClickable(enabled = enabled, pressScale = 0.9f, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(iconSize),
            tint = if (enabled) tint else tint.copy(alpha = 0.38f),
        )
    }
}

@Composable
fun QBackButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    QIconButton(
        icon = QIcons.Back,
        contentDescription = "返回",
        onClick = onClick,
        modifier = modifier,
        tint = Q.colors.ink,
        background = Q.colors.surface2,
    )
}

// ── Rows ─────────────────────────────────────────────────────────────

@Composable
fun QRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    hue: Float? = null,
    leading: (@Composable () -> Unit)? = null,
    value: String? = null,
    valueHot: Boolean = false,
    trailing: (@Composable RowScope.() -> Unit)? = null,
    chevron: Boolean = false,
    enabled: Boolean = true,
    titleColor: Color = Color.Unspecified,
    subtitleMaxLines: Int = 2,
    onLongClick: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.qRowClickable(enabled = enabled, onLongClick = onLongClick, onClick = onClick) else Modifier)
            .heightIn(min = 56.dp)
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .graphicsLayer { alpha = if (enabled) 1f else 0.45f },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        when {
            leading != null -> leading()
            icon != null && hue != null -> QTintIcon(icon = icon, hue = hue)
            icon != null -> Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp), tint = Q.colors.ink2)
        }
        Column(modifier = Modifier.weight(1f)) {
            QText(text = title, style = QType.rowTitle, color = titleColor)
            if (!subtitle.isNullOrBlank()) {
                QText(
                    text = subtitle,
                    style = QType.sub,
                    color = Q.colors.ink2,
                    maxLines = subtitleMaxLines,
                )
            }
        }
        if (!value.isNullOrBlank()) {
            QText(
                text = value,
                style = if (valueHot) QType.label.copy(fontSize = 14.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold) else QType.label.copy(fontSize = 14.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Normal),
                color = if (valueHot) Q.colors.seal else Q.colors.ink2,
                maxLines = 1,
                modifier = Modifier.widthIn(max = 160.dp),
            )
        }
        trailing?.invoke(this)
        if (chevron) {
            Icon(QIcons.ChevronRight, contentDescription = null, modifier = Modifier.size(18.dp), tint = Q.colors.ink3)
        }
    }
}

@Composable
fun QSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    hue: Float? = null,
    enabled: Boolean = true,
) {
    QRow(
        title = title,
        modifier = modifier,
        subtitle = subtitle,
        icon = icon,
        hue = hue,
        enabled = enabled,
        onClick = { onCheckedChange(!checked) },
        trailing = {
            Switch(checked = checked, onCheckedChange = null, enabled = enabled)
        },
    )
}

@Composable
fun QRadioRow(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
) {
    QRow(
        title = title,
        modifier = modifier,
        subtitle = subtitle,
        onClick = onClick,
        trailing = {
            Box(Modifier.size(22.dp), contentAlignment = Alignment.Center) {
                if (selected) {
                    Icon(QIcons.Check, contentDescription = "已选择", modifier = Modifier.size(22.dp), tint = Q.colors.ink)
                }
            }
        },
    )
}

/** Key/value line used inside detail sheets and cards. */
@Composable
fun QInfoLine(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    numeric: Boolean = false,
    valueColor: Color = Color.Unspecified,
    labelWidth: Dp = 64.dp,
) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        QText(label, modifier = Modifier.width(labelWidth), style = QType.body, color = Q.colors.ink3)
        QText(
            value,
            modifier = Modifier.weight(1f),
            style = if (numeric) QType.num(17.sp) else QType.body,
            color = valueColor,
        )
    }
}

// ── Buttons ──────────────────────────────────────────────────────────

enum class QButtonKind { Primary, Seal, Tonal, Plain }

@Composable
fun QButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    kind: QButtonKind = QButtonKind.Tonal,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    loading: Boolean = false,
    height: Dp = 50.dp,
) {
    val colors = Q.colors
    val (bg, fg) = when (kind) {
        QButtonKind.Primary -> colors.ink to colors.surface
        QButtonKind.Seal -> colors.seal to colors.onSeal
        QButtonKind.Tonal -> colors.surface2 to colors.ink
        QButtonKind.Plain -> Color.Transparent to colors.ink2
    }
    val active = enabled && !loading
    Row(
        modifier = modifier
            .heightIn(min = height)
            .qClickable(enabled = active, onClick = onClick)
            .clip(RoundedCornerShape(16.dp))
            .background(if (enabled) bg else bg.copy(alpha = bg.alpha * 0.45f))
            .padding(horizontal = 18.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (loading) {
            InfiniteProgressIndicator(modifier = Modifier.size(18.dp), color = fg)
            Spacer(Modifier.width(8.dp))
        } else if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(19.dp), tint = if (enabled) fg else fg.copy(alpha = 0.5f))
            Spacer(Modifier.width(8.dp))
        }
        QText(
            text = text,
            style = QType.bodyStrong.copy(fontSize = 15.sp),
            color = if (enabled) fg else fg.copy(alpha = 0.5f),
            maxLines = 1,
        )
    }
}

/** Small pill, e.g. "周一" / "本周不上" on top of a course sheet. */
@Composable
fun QChip(
    text: String,
    modifier: Modifier = Modifier,
    hue: Float? = null,
    container: Color? = null,
    content: Color? = null,
) {
    val tint = hue?.let { Q.colors.hue(it) }
    QText(
        text = text,
        modifier = modifier
            .clip(CircleShape)
            .background(container ?: tint?.container ?: Q.colors.surface2)
            .padding(horizontal = 10.dp, vertical = 3.dp),
        style = QType.caption.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold),
        color = content ?: tint?.content ?: Q.colors.ink2,
        maxLines = 1,
    )
}

/** Segmented control (pill track with a raised selected segment). */
@Composable
fun QSegmented(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(Q.colors.surface2)
            .padding(3.dp),
    ) {
        options.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            Box(
                modifier = Modifier
                    .weight(1f)
                    .then(if (selected) Modifier.shadow(1.dp, CircleShape) else Modifier)
                    .clip(CircleShape)
                    .background(if (selected) Q.colors.surface else Color.Transparent)
                    .qClickable(role = Role.Tab, pressScale = 0.96f) { onSelect(index) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                QText(
                    text = label,
                    style = QType.label.copy(fontWeight = if (selected) androidx.compose.ui.text.font.FontWeight.SemiBold else androidx.compose.ui.text.font.FontWeight.Normal),
                    color = if (selected) Q.colors.ink else Q.colors.ink2,
                    maxLines = 1,
                )
            }
        }
    }
}

// ── States ───────────────────────────────────────────────────────────

/** Dashed placeholder used for empty days / empty lists. */
@Composable
fun QEmpty(
    text: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    content: (@Composable ColumnScope.() -> Unit)? = null,
) {
    val lineColor = Q.colors.ink3.copy(alpha = 0.35f)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                drawRoundRect(
                    color = lineColor,
                    cornerRadius = CornerRadius(20.dp.toPx()),
                    style = Stroke(width = 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 5.dp.toPx()))),
                )
            }
            .padding(horizontal = 20.dp, vertical = 26.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        QText(text, style = QType.body.copy(fontSize = 14.sp), color = Q.colors.ink3, textAlign = TextAlign.Center)
        if (subtitle != null) {
            QText(subtitle, style = QType.caption, color = Q.colors.ink3, textAlign = TextAlign.Center)
        }
        if (content != null) {
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
fun QLoading(modifier: Modifier = Modifier, text: String? = null) {
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        InfiniteProgressIndicator(color = Q.colors.ink2)
        if (text != null) QText(text, style = QType.sub, color = Q.colors.ink3)
    }
}

/** Thin progress track; seal for "right now", ink otherwise. */
@Composable
fun QProgressBar(
    progress: Float?,
    modifier: Modifier = Modifier,
    color: Color = Q.colors.ink,
    trackColor: Color = Q.colors.line,
    height: Dp = 6.dp,
) {
    if (progress == null) {
        top.yukonga.miuix.kmp.basic.LinearProgressIndicator(
            modifier = modifier,
            progress = null,
            colors = top.yukonga.miuix.kmp.basic.ProgressIndicatorDefaults.progressIndicatorColors(
                foregroundColor = color,
                backgroundColor = trackColor,
            ),
        )
        return
    }
    val animated by animateFloatAsState(progress.coerceIn(0f, 1f), label = "qProgress")
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(CircleShape)
            .background(trackColor),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(animated)
                .height(height)
                .clip(CircleShape)
                .background(color),
        )
    }
}

/** Banner for warnings / errors inside a page (seal tinted, never loud fill). */
@Composable
fun QNotice(
    text: String,
    modifier: Modifier = Modifier,
    title: String? = null,
    attention: Boolean = true,
    icon: ImageVector = if (attention) QIcons.Warning else QIcons.Info,
    action: (@Composable RowScope.() -> Unit)? = null,
) {
    val fg = if (attention) Q.colors.seal else Q.colors.ink2
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(if (attention) Q.colors.sealSoft else Q.colors.surface2)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = fg)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            if (title != null) QText(title, style = QType.bodyStrong.copy(fontSize = 15.sp), color = if (attention) fg else Q.colors.ink)
            QText(text, style = QType.sub, color = if (attention) fg.copy(alpha = 0.86f) else Q.colors.ink2)
        }
        action?.invoke(this)
    }
}

/** Big-number statistic: "38.6" + "度" label. */
@Composable
fun QStat(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    unit: String? = null,
    valueColor: Color = Color.Unspecified,
    size: androidx.compose.ui.unit.TextUnit = 32.sp,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            QText(value, style = QType.num(size), color = valueColor, maxLines = 1)
            if (unit != null) {
                QText(unit, modifier = Modifier.padding(bottom = 3.dp), style = QType.sub, color = Q.colors.ink2)
            }
        }
        QText(label, style = QType.caption, color = Q.colors.ink3)
    }
}

/** Simple centered box helper used by tiles. */
@Composable
internal fun QCenter(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(modifier = modifier, contentAlignment = Alignment.Center, content = content)
}
