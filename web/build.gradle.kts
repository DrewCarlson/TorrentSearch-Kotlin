plugins {
    kotlin("multiplatform") version KOTLIN_VERSION
    id("org.jetbrains.compose") version JBCOMPOSE_VERSION
}

kotlin {
    js(IR) {
        browser()
        binaries.executable()
    }

    sourceSets {
        val jsMain by getting {
            dependencies {
                implementation(project(":"))
                implementation(kotlin("stdlib-common"))
                implementation(compose.web.core)
                implementation(compose.runtime)
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$COROUTINES_VERSION")
                implementation("io.ktor:ktor-client-json:$KTOR_VERSION")
                implementation("io.ktor:ktor-client-js:$KTOR_VERSION")
                implementation("io.ktor:ktor-client-content-negotiation:$KTOR_VERSION")
                implementation("io.ktor:ktor-serialization-kotlinx-json:$KTOR_VERSION")
            }
        }
    }
}
