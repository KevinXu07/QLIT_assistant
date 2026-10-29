import ActivityKit
import ComposeApp
import Foundation
import WidgetKit

/// Kotlin 侧 NativeWidgetBridge 协议的 Swift 实现：把 commonMain 的
/// refreshWidgets / postBreakLiveNotification 调用转成 WidgetKit/ActivityKit。
final class WidgetBridgeImpl: NSObject, NativeWidgetBridge {

    func reloadWidgetTimelines() {
        WidgetCenter.shared.reloadAllTimelines()
    }

    func liveActivitiesEnabled() -> Bool {
        ActivityAuthorizationInfo().areActivitiesEnabled
    }

    func updateCourseLiveActivity(
        courseName: String,
        classroom: String,
        minutesUntil: Int32,
        progressPercent: Int32
    ) {
        let state = CourseActivityAttributes.ContentState(
            courseName: courseName,
            classroom: classroom,
            minutesUntil: Int(minutesUntil),
            progressPercent: Int(progressPercent)
        )
        Task {
            if let activity = Activity<CourseActivityAttributes>.activities.first {
                await activity.update(ActivityContent(state: state, staleDate: nil))
            } else {
                _ = try? Activity<CourseActivityAttributes>.request(
                    attributes: CourseActivityAttributes(),
                    content: ActivityContent(state: state, staleDate: nil),
                    pushType: nil
                )
            }
        }
    }

    func endCourseLiveActivity() {
        Task {
            for activity in Activity<CourseActivityAttributes>.activities {
                await activity.end(dismissalPolicy: .immediate)
            }
        }
    }
}
