package torrentsearch.web

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import io.ktor.client.HttpClient
import io.ktor.client.request.HttpRequestPipeline
import io.ktor.client.request.url
import io.ktor.http.encodeURLQueryComponent
import io.ktor.http.path
import kotlinx.browser.window
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectIndexed
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.isActive
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.attributes.placeholder
import org.jetbrains.compose.web.css.DisplayStyle
import org.jetbrains.compose.web.css.FlexDirection
import org.jetbrains.compose.web.css.JustifyContent
import org.jetbrains.compose.web.css.display
import org.jetbrains.compose.web.css.em
import org.jetbrains.compose.web.css.flexDirection
import org.jetbrains.compose.web.css.flexGrow
import org.jetbrains.compose.web.css.flexShrink
import org.jetbrains.compose.web.css.gap
import org.jetbrains.compose.web.css.height
import org.jetbrains.compose.web.css.justifyContent
import org.jetbrains.compose.web.css.margin
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.css.width
import org.jetbrains.compose.web.dom.A
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Input
import org.jetbrains.compose.web.dom.Option
import org.jetbrains.compose.web.dom.Select
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.renderComposable
import org.w3c.dom.url.URLSearchParams
import torrentsearch.TorrentSearch
import torrentsearch.models.Category
import torrentsearch.models.SearchResult
import torrentsearch.models.TorrentDescription

fun main() {
    val httpClient = HttpClient {
        install("cors-proxy") {
            requestPipeline.intercept(HttpRequestPipeline.State) {
                val originalUrl = context.url.buildString()
                context.url(scheme = "https", host = "thingproxy.freeboard.io") {
                    path("fetch", originalUrl)
                    parameters.clear()
                }
                proceed()
            }
        }
    }
    val torrentSearch = TorrentSearch(
        httpClient = httpClient
    )
    renderComposable("root") {
        var searchQuery by remember {
            val queryParams = URLSearchParams(window.location.search)
            mutableStateOf(queryParams.get("q"))
        }
        var searchCategory: Category? by remember { mutableStateOf(null) }
        val searchResult by produceState<SearchResult?>(null, searchQuery, searchCategory) {
            if (searchQuery.isNullOrBlank()) {
                value = null
                return@produceState
            }
            delay(300)
            value = torrentSearch.search {
                content = searchQuery
                category = searchCategory
            }
        }
        val torrents by produceState(emptyList<TorrentDescription>(), searchResult) {
            value = emptyList()
            searchResult?.torrents()?.onEach {
                value = (value + it).sortedByDescending(TorrentDescription::seeds)
            }?.collect()
        }
        val completedState by produceState<String?>(null, searchResult) {
            val result = searchResult
            if (result == null) {
                value = null
            } else {
                value = "Loading 0 / ${result.providerCount()}"
                result.providerResults()
                    .collectIndexed { index, _ ->
                        value = "Loading ${index + 1} / ${result.providerCount()}"
                    }
                value = "Completes ${result.providerCount()}"
            }
        }
        val loading by produceState<String?>(null, searchResult) {
            val result = searchResult
            if (result?.isCompleted() == false) {
                value = "."
                while (isActive && !result.isCompleted()) {
                    if (value?.length == 4) {
                        value = "."
                    } else {
                        value += '.'
                    }
                    delay(250)
                }
            }
            value = null
        }

        Div({
            style {
                width(100.percent)
                height(100.percent)
                display(DisplayStyle.Flex)
                flexDirection(FlexDirection.Column)
            }
        }) {
            Div({
                style {
                    width(100.percent)
                    flexShrink(0)
                    gap(1.em)
                    display(DisplayStyle.Flex)
                }
            }) {
                Input(InputType.Text) {
                    placeholder("Search ...")
                    onInput {
                        searchQuery = it.value
                        if (searchQuery.isNullOrBlank()) {
                            window.history.pushState(null, window.document.title, "")
                        } else {
                            val url = "?q=${searchQuery?.encodeURLQueryComponent()}"
                            window.history.pushState(null, window.document.title, url)
                        }
                    }
                }
                Select({
                    onChange {
                        searchCategory = it.value?.takeIf(String::isNotBlank)?.run(Category::valueOf)
                    }
                }) {
                    val categories = remember { Category.values().toList() - Category.MOVIES - Category.TV }
                    Option("(category)") { Text("(none)") }
                    Option("MOVIES") { Text("Movies") }
                    Option("TV") { Text("Tv") }
                    (categories - Category.MOVIES).forEach { category ->
                        Option(category.name) {
                            val name = remember { category.name.lowercase().replaceFirstChar { it.uppercaseChar() } }
                            Text(name)
                        }
                    }
                }
                Div({
                    style {
                        display(DisplayStyle.Flex)
                        gap(.5.em)
                    }
                }) {
                    Div { Text("Status:") }
                    Div { Text(completedState ?: "(idle)") }
                    Div { Text(loading.orEmpty()) }
                }
            }
            Div({
                style {
                    width(100.percent)
                    height(100.percent)
                }
            }) {
                Div({
                    style {
                        width(100.percent)
                        flexGrow(1)
                        display(DisplayStyle.Flex)
                        flexDirection(FlexDirection.Column)
                    }
                }) {
                    torrents.forEach { torrent ->
                        TorrentItem(torrent)
                    }
                }
            }
        }
    }
}

@Composable
fun TorrentItem(torrent: TorrentDescription) {
    Div({
        style {
            width(100.percent)
            display(DisplayStyle.Flex)
            gap(1.5.em)
            margin(6.px)
            justifyContent(JustifyContent.SpaceBetween)
        }
    }) {
        Div({ style { width(100.px) } }) {
            Text(torrent.provider)
        }
        Div({ style { width(370.px) } }) {
            Text(torrent.hash)
        }
        Div({ style { property("margin-right", "auto") } }) {
            Text(torrent.title)
        }
        Div { Text("Seeds (${torrent.seeds})") }
        Div { Text("Peers (${torrent.peers})") }
        Div {
            A(torrent.magnetUrl) {
                Text("Download")
            }
        }
    }
}
