import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm")
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.compose.compiler)
}
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}
kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
    }
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(project(":appShare"))
}

compose.desktop {
    application {
        mainClass = "MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb, TargetFormat.Rpm, TargetFormat.Exe)
            packageName = "tFileTransfer"
        }
        nativeDistributions {
            macOS {
                iconFile.set(project.file("desktop-launcher-icons/launcher_macos.icns"))
            }
            windows {
                iconFile.set(project.file("desktop-launcher-icons/launcher_windows.ico"))
            }
            linux {
                iconFile.set(project.file("desktop-launcher-icons/launcher_linux.png"))
            }
        }
    }
}
