import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.serialization)
}

compose.resources {
    // rootProject.name 带空格会渗进生成包名，固定为模块命名空间
    packageOfResClass = "com.schedule.qlit.composeapp.generated.resources"
}

kotlin {
    android {
        namespace = "com.schedule.qlit"
        compileSdk = 37
        minSdk = 31

        // miuix 0.9.4 的构件按 JVM 21 编译,内联函数要求消费方同级别目标
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }

        // res/ 资源与手写 IUserService.java(替代 aidl)需要显式开启
        androidResources.enable = true
        withJava()

        withHostTest { }
        withDeviceTest {
            instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            // 共享地基:协程 + 序列化 + 网络 + Compose 运行时(@Immutable 等注解)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)
            implementation(libs.ktor.client.core)
            implementation(libs.compose.runtime)

            // 共享 UI:CMP 组件 + miuix(KMP 根坐标按平台解析变体)+ lifecycle-viewmodel
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodel.compose)
            implementation(libs.androidx.lifecycle.runtime.compose)
            implementation(libs.miuix.ui)
            implementation(libs.miuix.icons)
            implementation(libs.miuix.preference)
            implementation(libs.miuix.blur)
            implementation(libs.miuix.nav)
            implementation(libs.androidx.navigationevent.compose)
        }
        androidMain.dependencies {
            implementation(libs.androidx.core.ktx)
            implementation(libs.androidx.lifecycle.runtime.ktx)
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.androidx.activity.compose)
            implementation(project.dependencies.platform(libs.androidx.compose.bom))
            implementation(libs.androidx.compose.ui)
            implementation(libs.androidx.compose.ui.graphics)
            implementation(libs.androidx.compose.ui.tooling.preview)
            implementation(libs.material)
            implementation(libs.androidx.navigationevent.compose)
            implementation(libs.gson)
            implementation(libs.androidx.security.crypto)
            implementation(libs.okhttp)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.shizuku.api)
            implementation(libs.shizuku.provider)
            implementation(libs.hyper.notification.focus.api)
            implementation(libs.androidx.glance.appwidget)
            implementation(libs.androidx.glance.material3)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
        named("androidHostTest") {
            dependencies {
                implementation(libs.junit)
            }
        }
        named("androidDeviceTest") {
            dependencies {
                implementation(libs.androidx.junit)
                implementation(libs.androidx.espresso.core)
                implementation(project.dependencies.platform(libs.androidx.compose.bom))
                implementation(libs.androidx.compose.ui.test.junit4)
            }
        }
    }
}
