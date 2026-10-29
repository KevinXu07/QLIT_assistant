import ActivityKit
import SwiftUI
import WidgetKit

/// 课间实况：锁屏横幅 + 灵动岛，由 app 侧 WidgetBridgeImpl 驱动。
struct CourseLiveActivity: Widget {
    var body: some WidgetConfiguration {
        ActivityConfiguration(for: CourseActivityAttributes.self) { context in
            LockScreenLiveActivityView(state: context.state)
        } dynamicIsland: { context in
            DynamicIsland {
                DynamicIslandExpandedRegion(.leading) {
                    Label(context.state.courseName, systemImage: "book.fill")
                        .font(.caption)
                }
                DynamicIslandExpandedRegion(.trailing) {
                    Text("\(context.state.minutesUntil) 分钟")
                        .font(.caption.monospacedDigit())
                }
                DynamicIslandExpandedRegion(.bottom) {
                    ProgressView(value: Double(context.state.progressPercent), total: 100)
                }
            } compactLeading: {
                Image(systemName: "book.fill")
            } compactTrailing: {
                Text("\(context.state.minutesUntil)m")
                    .font(.caption2.monospacedDigit())
            } minimal: {
                Image(systemName: "book.fill")
            }
        }
    }
}

private struct LockScreenLiveActivityView: View {
    let state: CourseActivityAttributes.ContentState

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Text(state.courseName)
                    .font(.headline)
                Spacer()
                Text("上课还有 \(state.minutesUntil) 分钟")
                    .font(.subheadline.monospacedDigit())
                    .foregroundStyle(.secondary)
            }
            if !state.classroom.isEmpty {
                Text(state.classroom)
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
            ProgressView(value: Double(state.progressPercent), total: 100)
        }
        .padding()
        .activityBackgroundTint(Color.black.opacity(0.75))
    }
}
