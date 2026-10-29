package com.schedule.qlit.widget

import android.content.Context
import android.content.Intent
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.material3.ColorProviders
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.schedule.qlit.MainActivity
import com.schedule.qlit.R

/** 刷新按钮：直接触发当前实例重走 provideGlance（含网络拉取）。 */
class WidgetRefreshAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        QlitWidget().update(context, glanceId)
    }
}

class QlitWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = runCatching { QlitWidgetData.load(context) }.getOrNull()
        data?.let { QlitWidgetRefresher.scheduleNextBoundary(context, it.nextBoundaryAt) }
        provideContent {
            GlanceTheme(
                colors = ColorProviders(
                    light = dynamicLightColorScheme(context),
                    dark = dynamicDarkColorScheme(context),
                )
            ) {
                if (data != null) {
                    WidgetContent(context, data)
                } else {
                    FallbackContent(context)
                }
            }
        }
    }

    private fun openApp(context: Context, navigateTo: String, tool: String? = null): Intent =
        Intent(context, MainActivity::class.java).apply {
            putExtra("navigate_to", navigateTo)
            tool?.let { putExtra("tool_destination", it) }
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

    @Composable
    private fun WidgetContent(context: Context, data: QlitWidgetData.WidgetData) {
        val compact = LocalSize.current.height < 108.dp
        Column(
            modifier = GlanceModifier.fillMaxSize()
                .appWidgetBackground()
                .background(GlanceTheme.colors.surface)
                .cornerRadius(android.R.dimen.system_app_widget_background_radius)
                .clickable(actionStartActivity(openApp(context, "today")))
                .padding(
                    start = 16.dp,
                    end = 14.dp,
                    top = if (compact) 8.dp else 12.dp,
                    bottom = 10.dp,
                ),
        ) {
            if (!compact) {
                HeaderRow(data.dateLine)
                Spacer(GlanceModifier.height(8.dp))
            }
            Row(modifier = GlanceModifier.fillMaxSize()) {
                CourseColumn(
                    data = data,
                    compact = compact,
                    modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                )
                Spacer(GlanceModifier.width(14.dp))
                StatsColumn(
                    context = context,
                    stats = data.stats,
                    modifier = GlanceModifier.width(112.dp).fillMaxHeight(),
                )
            }
        }
    }

    @Composable
    private fun HeaderRow(dateLine: String) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = dateLine,
                maxLines = 1,
                modifier = GlanceModifier.defaultWeight(),
                style = TextStyle(
                    color = GlanceTheme.colors.onSurfaceVariant,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                ),
            )
            Image(
                provider = ImageProvider(R.drawable.ic_widget_refresh),
                contentDescription = "刷新",
                colorFilter = ColorFilter.tint(GlanceTheme.colors.onSurfaceVariant),
                modifier = GlanceModifier
                    .size(18.dp)
                    .clickable(actionRunCallback<WidgetRefreshAction>()),
            )
        }
    }

    @Composable
    private fun CourseColumn(
        data: QlitWidgetData.WidgetData,
        compact: Boolean,
        modifier: GlanceModifier,
    ) {
        val maxRows = if (compact) 2 else 3
        Column(modifier = modifier) {
            Row(modifier = GlanceModifier.fillMaxWidth()) {
                Text(
                    text = "今日课程",
                    maxLines = 1,
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurfaceVariant,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                )
                Spacer(GlanceModifier.defaultWeight())
                if (data.hasSchedule && data.totalCourses > 0) {
                    Text(
                        text = "共${data.totalCourses}节",
                        maxLines = 1,
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurfaceVariant,
                            fontSize = 11.sp,
                        ),
                    )
                }
            }
            Spacer(GlanceModifier.height(6.dp))
            when {
                !data.hasSchedule -> Text(
                    text = "未导入课表",
                    maxLines = 1,
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurfaceVariant,
                        fontSize = 12.sp,
                    ),
                )
                data.courses.isEmpty() -> Text(
                    text = "今日无课",
                    maxLines = 1,
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurfaceVariant,
                        fontSize = 12.sp,
                    ),
                )
                else -> {
                    data.courses.take(maxRows).forEachIndexed { index, course ->
                        CourseRow(index, course)
                    }
                    val overflow = data.courses.size - maxRows
                    if (overflow > 0) {
                        Text(
                            text = "还有 $overflow 节",
                            maxLines = 1,
                            style = TextStyle(
                                color = GlanceTheme.colors.onSurfaceVariant,
                                fontSize = 10.sp,
                            ),
                            modifier = GlanceModifier.padding(top = 2.dp, start = 9.dp),
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun CourseRow(index: Int, course: QlitWidgetData.CourseRow) {
        val barColors = listOf(
            GlanceTheme.colors.primary,
            GlanceTheme.colors.secondary,
            GlanceTheme.colors.tertiary,
        )
        val (barColor, nameColor, timeColor) = when (course.state) {
            QlitWidgetData.CourseState.DONE -> Triple(
                GlanceTheme.colors.surfaceVariant,
                GlanceTheme.colors.onSurfaceVariant,
                GlanceTheme.colors.onSurfaceVariant,
            )
            QlitWidgetData.CourseState.ONGOING -> Triple(
                GlanceTheme.colors.primary,
                GlanceTheme.colors.primary,
                GlanceTheme.colors.primary,
            )
            QlitWidgetData.CourseState.UPCOMING -> Triple(
                barColors[index % barColors.size],
                GlanceTheme.colors.onSurface,
                GlanceTheme.colors.onSurfaceVariant,
            )
        }
        Row(
            modifier = GlanceModifier.fillMaxWidth().padding(vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = GlanceModifier
                    .width(3.dp)
                    .height(13.dp)
                    .cornerRadius(2.dp)
                    .background(barColor),
            ) {}
            Spacer(GlanceModifier.width(6.dp))
            Text(
                text = course.time,
                maxLines = 1,
                style = TextStyle(color = timeColor, fontSize = 11.sp),
            )
            Spacer(GlanceModifier.width(6.dp))
            Text(
                text = course.name,
                maxLines = 1,
                modifier = GlanceModifier.defaultWeight(),
                style = TextStyle(
                    color = nameColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                ),
            )
            if (course.room.isNotBlank()) {
                Text(
                    text = "·${course.room}",
                    maxLines = 1,
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurfaceVariant,
                        fontSize = 10.sp,
                    ),
                )
            }
        }
    }

    @Composable
    private fun StatsColumn(
        context: Context,
        stats: List<QlitWidgetData.StatRow>,
        modifier: GlanceModifier,
    ) {
        Column(modifier = modifier) {
            stats.forEachIndexed { index, stat ->
                Row(
                    modifier = GlanceModifier
                        .defaultWeight()
                        .fillMaxWidth()
                        .clickable(
                            actionStartActivity(
                                openApp(context, "tools", stat.toolDestination)
                            )
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Image(
                        provider = ImageProvider(stat.iconRes),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(GlanceTheme.colors.primary),
                        modifier = GlanceModifier.size(15.dp),
                    )
                    Spacer(GlanceModifier.width(7.dp))
                    Text(
                        text = stat.label,
                        maxLines = 1,
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurfaceVariant,
                            fontSize = 11.sp,
                        ),
                    )
                    Spacer(GlanceModifier.defaultWeight())
                    Text(
                        text = stat.value,
                        maxLines = 1,
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurface,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                }
                if (index < stats.lastIndex) {
                    Box(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(GlanceTheme.colors.surfaceVariant),
                    ) {}
                }
            }
        }
    }

    @Composable
    private fun FallbackContent(context: Context) {
        Box(
            modifier = GlanceModifier.fillMaxSize()
                .appWidgetBackground()
                .background(GlanceTheme.colors.surface)
                .cornerRadius(android.R.dimen.system_app_widget_background_radius)
                .clickable(actionStartActivity(openApp(context, "today")))
                .padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "点按打开 QLIT小助手",
                maxLines = 1,
                style = TextStyle(
                    color = GlanceTheme.colors.onSurfaceVariant,
                    fontSize = 12.sp,
                ),
            )
        }
    }
}
