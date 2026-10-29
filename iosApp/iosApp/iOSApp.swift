import ComposeApp
import SwiftUI

@main
struct iOSApp: App {

    init() {
        // Kotlin commonMain 的 WidgetKit/ActivityKit 回调入口
        NativeWidgetBridgeHolder.shared.impl = WidgetBridgeImpl()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
