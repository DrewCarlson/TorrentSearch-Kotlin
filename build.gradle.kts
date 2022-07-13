import org.jetbrains.kotlin.gradle.targets.js.yarn.yarn

@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    alias(libs.plugins.multiplatform)
    alias(libs.plugins.serialization)
    alias(libs.plugins.binaryCompat)
    alias(libs.plugins.dokka)
    alias(libs.plugins.spotless)
}

allprojects {
    yarn.lockFileDirectory = file("gradle/kotlin-js-store")
    repositories {
        mavenCentral()
    }
}

apply(from = "gradle/publishing.gradle.kts")

kotlin {
    jvm()
    js(IR) {
        browser {
            testTask {
                useMocha {
                    timeout = "10000"
                }
            }
        }
        nodejs {
            testTask {
                useMocha {
                    timeout = "10000"
                }
            }
        }
    }
    macosX64("macos")
    mingwX64("win64")
    linuxX64()

    ios()

    sourceSets {
        all {
            explicitApi()
            languageSettings.optIn("kotlin.RequiresOptIn")
            languageSettings.optIn("kotlinx.coroutines.ExperimentalCoroutinesApi")
        }

        val commonMain by getting {
            dependencies {
                implementation(kotlin("stdlib-common"))
                implementation(libs.coroutines.core)
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.contentNegotiation)
                implementation(libs.ktor.serialization)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(libs.coroutines.test)
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
            }
        }
        val jvmMain by getting {
            dependencies {
                implementation(kotlin("stdlib-jdk8"))
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(kotlin("test-junit"))
                implementation(libs.ktor.client.okhttp)
            }
        }
        val jsMain by getting
        val jsTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(kotlin("test-js"))
                implementation(libs.ktor.client.js)
            }
        }

        val nativeCommonMain by creating {
            dependsOn(commonMain)
        }
        val nativeCommonTest by creating {
            dependsOn(commonTest)
        }
        val desktopCommonMain by creating {
            dependsOn(nativeCommonMain)
        }
        val desktopCommonTest by creating {
            dependsOn(nativeCommonTest)
            dependencies {
                implementation(libs.ktor.client.curl)
            }
        }

        val win64Main by getting
        val macosMain by getting
        val linuxX64Main by getting
        configure(listOf(win64Main, macosMain, linuxX64Main)) {
            dependsOn(desktopCommonMain)
        }

        val win64Test by getting
        val macosTest by getting
        val linuxX64Test by getting
        configure(listOf(win64Test, macosTest, linuxX64Test)) {
            dependsOn(desktopCommonTest)
        }

        val iosMain by getting {
            dependsOn(nativeCommonMain)
        }
        val iosTest by getting {
            dependsOn(nativeCommonTest)
            dependencies {
                implementation(libs.ktor.client.darwin)
            }
        }
    }
}

tasks.dokkaHtml {
    moduleName.set("TorrentSearch")
}
