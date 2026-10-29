import SwiftUI
import WidgetKit

/// 与 composeApp iOS 侧 widgetCache() 写入的 App Group suite 对齐。
private let widgetSuite = "group.cn.kevinxu.qlit.free"

struct ToolsStatusEntry: TimelineEntry {
    let date: Date
    let electricity: String?
    let chaoxingLabel: String
    let taobaoLabel: String
}

struct ToolsStatusProvider: TimelineProvider {

    func placeholder(in context: Context) -> ToolsStatusEntry { snapshot() }

    func getSnapshot(in context: Context, completion: @escaping (ToolsStatusEntry) -> Void) {
        completion(snapshot())
    }

    func getTimeline(in context: Context, completion: @escaping (Timeline<ToolsStatusEntry>) -> Void) {
        // 数据变更由 app 侧 reloadAllTimelines 驱动；保底每日一次系统刷新。
        let entry = snapshot()
        let next = Calendar.current.date(byAdding: .day, value: 1, to: entry.date)!
        completion(Timeline(entries: [entry], policy: .after(next)))
    }

    private func snapshot() -> ToolsStatusEntry {
        let d = UserDefaults(suiteName: widgetSuite)
        let elec = d?.string(forKey: "elec_value").flatMap { $0.isEmpty ? nil : $0 }

        let chaoxing: String
        if d?.object(forKey: "cx_state") == nil {
            chaoxing = "未登录"
        } else {
            switch d?.integer(forKey: "cx_state") ?? -1 {
            case 0:
                let pending = (d?.integer(forKey: "cx_work") ?? 0) + (d?.integer(forKey: "cx_exam") ?? 0)
                chaoxing = pending > 0 ? "\(pending) 待办" : "无待办"
            case 1: chaoxing = "需重新登录"
            default: chaoxing = "未登录"
            }
        }

        let taobao: String
        if d?.object(forKey: "tb_logged_in") == nil || d?.bool(forKey: "tb_logged_in") == false {
            taobao = "未登录"
        } else {
            let pending = d?.integer(forKey: "tb_pending") ?? -1
            taobao = pending > 0 ? "\(pending) 待取" : "无待取"
        }

        return ToolsStatusEntry(
            date: Date(),
            electricity: elec,
            chaoxingLabel: chaoxing,
            taobaoLabel: taobao
        )
    }
}

struct ToolsStatusWidget: Widget {
    let kind = "ToolsStatusWidget"

    var body: some WidgetConfiguration {
        StaticConfiguration(kind: kind, provider: ToolsStatusProvider()) { entry in
            ToolsStatusView(entry: entry)
                .containerBackground(.fill.tertiary, for: .widget)
        }
        .configurationDisplayName("工具状态")
        .description("电费余额、超星待办与取件数一览。")
        .supportedFamilies([.systemSmall, .systemMedium])
    }
}

private struct ToolsStatusView: View {
    let entry: ToolsStatusEntry

    var body: some View {
        HStack(spacing: 8) {
            tile(icon: "bolt.fill", title: "电费", value: entry.electricity ?? "--")
            tile(icon: "checklist", title: "超星", value: entry.chaoxingLabel)
            tile(icon: "shippingbox.fill", title: "取件", value: entry.taobaoLabel)
        }
        .padding(4)
    }

    private func tile(icon: String, title: String, value: String) -> some View {
        VStack(spacing: 6) {
            Image(systemName: icon)
                .font(.title3)
                .foregroundStyle(.secondary)
            Text(title)
                .font(.caption2)
                .foregroundStyle(.secondary)
            Text(value)
                .font(.subheadline.weight(.semibold))
                .lineLimit(1)
                .minimumScaleFactor(0.6)
        }
        .frame(maxWidth: .infinity)
    }
}
