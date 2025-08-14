plugins {
    alias(libs.plugins.multiplatform)
    alias(libs.plugins.serialization)
    alias(libs.plugins.dokka)
    //alias(libs.plugins.kotlinter)
    alias(libs.plugins.mavenPublish)
}

buildscript {
    dependencies {
        classpath(libs.atomicfu.plugin)
    }
}

allprojects {
    repositories {
        mavenCentral()
        google()
    }
}

version = System.getenv("GITHUB_REF")?.substringAfter("refs/tags/v", version.toString()) ?: version

kotlin {
    @OptIn(org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation::class)
    abiValidation {
        enabled = true
    }
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
    macosX64()
    macosArm64()
    mingwX64()
    linuxX64()
    linuxArm64()

    iosX64()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        all {
            explicitApi()
            languageSettings.optIn("kotlin.RequiresOptIn")
            languageSettings.optIn("kotlinx.coroutines.ExperimentalCoroutinesApi")
        }

        val commonMain by getting {
            dependencies {
                implementation(kotlin("stdlib-common"))
                api(libs.coroutines.core)
                implementation(libs.atomicfu)
                api(libs.ktor.client.core)
                implementation(libs.ktor.client.contentNegotiation)
                implementation(libs.ktor.serialization)
                implementation(libs.ktsoup.core)
                implementation(libs.ktsoup.ktor)
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
                implementation(libs.ktor.client.logging)
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

        val mingwX64Test by getting
        val macosX64Test by getting
        val macosArm64Test by getting
        val linuxX64Test by getting
        configure(listOf(mingwX64Test, macosX64Test, macosArm64Test, linuxX64Test)) {
            dependencies {
                implementation(libs.ktor.client.curl)
            }
        }

        iosTest {
            dependencies {
                implementation(libs.ktor.client.darwin)
            }
        }
    }
}

dokka {
    moduleName = "TorrentSearch"
}
