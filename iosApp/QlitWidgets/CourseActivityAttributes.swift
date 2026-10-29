import ActivityKit

/// 课间实况 Live Activity 的数据契约：app 与 Widget 扩展共用。
/// 动态字段全在 ContentState（每次 postBreakLiveNotification 都重发）。
public struct CourseActivityAttributes: ActivityAttributes {
    public struct ContentState: Codable, Hashable {
        public var courseName: String
        public var classroom: String
        public var minutesUntil: Int
        public var progressPercent: Int
    }
}
