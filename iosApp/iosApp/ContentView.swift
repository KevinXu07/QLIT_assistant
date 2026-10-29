import UIKit
import SwiftUI
import ComposeApp

/// Compose Multiplatform 宿主：整棵 UI 树由 commonMain 的 QlitApp 提供，
/// Swift 侧只负责窗口承载与（后续）深链/小组件回调转发。
struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

/// 原生 UITabBarController：叠在 Compose 画布上方，由系统提供 iOS 26
/// 液态玻璃材质、液态选中动画与按压物理反馈；双向同步走 NativeTabBridge。
struct NativeTabBarHost: UIViewControllerRepresentable {
    @Binding var selection: Int

    func makeUIViewController(context: Context) -> UIViewController {
        PlatformGlassBar_iosKt.NativeTabBarViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {
        guard let tabBarController = uiViewController as? UITabBarController,
              tabBarController.selectedIndex != selection else { return }
        tabBarController.selectedIndex = selection
    }
}

struct ContentView: View {
    @State private var selectedTab = Int(NativeTabBridge.shared.currentIndex())

    var body: some View {
        ComposeView()
            .ignoresSafeArea()
            .overlay(alignment: .bottom) {
                NativeTabBarHost(selection: $selectedTab)
                    .frame(height: 108)
                    .ignoresSafeArea(edges: .bottom)
            }
            .onAppear {
                // Compose → SwiftUI：深链/内页驱动 tab 变化时回写选中态
                NativeTabBridge.shared.listener = { index in
                    selectedTab = Int(index.int32Value)
                }
            }
    }
}
