# TorrentSearch Kotlin

![Maven Central](https://img.shields.io/maven-central/v/org.drewcarlson/torrentsearch-jvm?label=maven&color=blue)
![](https://github.com/DrewCarlson/TorrentSearch-Kotlin/workflows/Jvm/badge.svg)
![](https://github.com/DrewCarlson/TorrentSearch-Kotlin/workflows/Js/badge.svg)
![](https://github.com/DrewCarlson/TorrentSearch-Kotlin/workflows/Native/badge.svg)

Torrent Provider API client written in Kotlin, based on [torrent-search-api](https://github.com/JimmyLaurent/torrent-search-api).

## About

TorrentSearch-Kotlin is written in common Kotlin to support multiplatform development.
[Kotlinx.serialization](https://github.com/Kotlin/kotlinx.serialization) provides json (de)serialization and [Ktor](https://ktor.io) the HTTP API.

Supported providers can be found in [`src/commonMain/kotlin/providers`](src/commonMain/kotlin/providers).

## Usage

```kotlin
val torrentSearch = TorrentSearch()

println(torrentSearch.search("big buck bunny"))
// TorrentDescription(provider=Libre, title=Big Buck Bunny, magnetUrl=magnet:?xt=urn:btih:...
```

## Download

![Maven Central](https://img.shields.io/maven-central/v/org.drewcarlson/torrentsearch-jvm?label=maven&color=blue)
![Sonatype Nexus (Snapshots)](https://img.shields.io/nexus/s/org.drewcarlson/torrentsearch-jvm?server=https%3A%2F%2Fs01.oss.sonatype.org)

![](https://img.shields.io/static/v1?label=&message=Platforms&color=grey)
![](https://img.shields.io/static/v1?label=&message=Js&color=blue)
![](https://img.shields.io/static/v1?label=&message=Jvm&color=blue)
![](https://img.shields.io/static/v1?label=&message=Linux&color=blue)
![](https://img.shields.io/static/v1?label=&message=macOS&color=blue)
![](https://img.shields.io/static/v1?label=&message=Windows&color=blue)
![](https://img.shields.io/static/v1?label=&message=iOS&color=blue)
![](https://img.shields.io/static/v1?label=&message=tvOS&color=blue)
![](https://img.shields.io/static/v1?label=&message=watchOS&color=blue)

```kotlin
repositories {
  mavenCentral()
  // Or snapshots
  maven(url = "https://s01.oss.sonatype.org/content/repositories/snapshots/")
}

dependencies {
  implementation("org.drewcarlson:torrentsearch:$torrentsearch_version")

  // For Jvm only:
  implementation("org.drewcarlson:torrentsearch-jvm:$torrentsearch_version")
}
```


Note: it is required to specify a Ktor client engine implementation.
([Documentation](https://ktor.io/clients/http-client/multiplatform.html))

```kotlin
dependencies {
  // Jvm/Android
  implementation("io.ktor:ktor-client-okhttp:$ktor_version")
  implementation("io.ktor:ktor-client-android:$ktor_version")
  // iOS
  implementation("io.ktor:ktor-client-ios:$ktor_version")
  // macOS/Windows/Linux
  implementation("io.ktor:ktor-client-curl:$ktor_version")
  // Javascript/NodeJS
  implementation("io.ktor:ktor-client-js:$ktor_version")
}
``` 

## License
```
Copyright (c) 2020 Andrew Carlson

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```
