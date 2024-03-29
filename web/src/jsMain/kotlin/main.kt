package torrentsearch.web

import androidx.compose.runtime.*
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.browser.window
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.renderComposable
import org.w3c.dom.url.URLSearchParams
import torrentsearch.TorrentSearch
import torrentsearch.models.Category
import torrentsearch.models.ProviderResult
import torrentsearch.models.SearchResult
import torrentsearch.models.TorrentDescription
import torrentsearch.web.components.CategorySelect
import torrentsearch.web.components.QueryStatus
import torrentsearch.web.components.SearchQueryInput
import torrentsearch.web.components.TorrentItem

fun main() {
    val httpClient = HttpClient {
        install("cors-proxy") {
            requestPipeline.intercept(HttpRequestPipeline.State) {
                val originalUrl = context.url.buildString()
                context.url {
                    path("")
                    parameters.clear()
                    //takeFrom("https://corsproxy.io/?${originalUrl.encodeURLParameter()}")
                    takeFrom("https://thingproxy.freeboard.io/fetch/${originalUrl.encodeURLParameter()}")
                }
                proceed()
            }
        }
    }
    val torrentSearch = TorrentSearch(httpClient = httpClient).apply {
        providers().forEach { enableProvider(it.name) }
    }
    val startParams = URLSearchParams(window.location.search)
    val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    renderComposable("root") {
        var searchQuery by remember { mutableStateOf(startParams.get("q")?.decodeURLQueryComponent()) }
        var searchImdbQuery by remember { mutableStateOf(startParams.get("imdb")) }
        var searchTmdbQuery by remember { mutableStateOf(startParams.get("tmdb")) }
        var searchPageQuery by remember { mutableStateOf(startParams.get("page")) }
        var searchCategory: Category? by remember {
            mutableStateOf(startParams.get("c")?.takeIf(String::isNotBlank)?.run(Category::valueOf))
        }
        val resolvedTorrents = remember {
            mutableStateMapOf<String, TorrentDescription>()
        }
        val searchResult by produceState<SearchResult?>(
            null,
            searchQuery,
            searchImdbQuery,
            searchTmdbQuery,
            searchCategory,
            searchPageQuery,
        ) {
            val queries = listOfNotNull(searchQuery, searchImdbQuery, searchTmdbQuery)
            if (queries.isEmpty() || queries.all(String::isBlank)) {
                value = null
                resolvedTorrents.clear()
                return@produceState
            }

            delay(500)
            resolvedTorrents.clear()
            value = torrentSearch.search {
                content = searchQuery
                imdbId = searchImdbQuery
                tmdbId = searchTmdbQuery?.toIntOrNull()
                category = searchCategory
                page = searchPageQuery?.toIntOrNull() ?: 1
            }
        }
        val torrents by produceState(emptyList<TorrentDescription>(), searchResult) {
            value = emptyList()
            searchResult?.torrents()?.onEach {
                value = (value + it).sortedByDescending(TorrentDescription::seeds)
            }?.collect()
        }
        val providerResults by produceState(emptyList<ProviderResult>(), searchResult) {
            value = emptyList()
            searchResult?.providerResults()?.onEach {
                value = (value + it).sortedBy(ProviderResult::providerName)
            }?.collect()
        }

        val onResolve = remember {
            { torrent: TorrentDescription ->
                scope.launch {
                    val resultSet = torrentSearch.resolve(listOf(torrent))
                    val resolved = resultSet.resolved.firstOrNull()
                    if (resolved != null) {
                        resolvedTorrents[resolved.title] = resolved
                    }
                }
                Unit
            }
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
                SearchQueryInput("Search ...", searchQuery) { newQuery ->
                    searchQuery = newQuery
                    searchImdbQuery = null
                    searchTmdbQuery = null
                    updateQueryParam("q", newQuery, "imdb", "tmdb")
                }
                SearchQueryInput("imdb id", searchImdbQuery) { newQuery ->
                    searchQuery = null
                    searchImdbQuery = newQuery
                    searchTmdbQuery = null
                    updateQueryParam("imdb", newQuery, "q", "tmdb")
                }
                SearchQueryInput("tmdb id", searchTmdbQuery) { newQuery ->
                    searchQuery = null
                    searchImdbQuery = null
                    searchTmdbQuery = newQuery
                    updateQueryParam("tmdb", newQuery, "q", "imdb")
                }
                SearchQueryInput("page", searchPageQuery) { newQuery ->
                    searchPageQuery = newQuery
                    updateQueryParam("page", newQuery)
                }
                CategorySelect(searchCategory) { selectedCategory ->
                    searchCategory = selectedCategory
                    updateQueryParam("c", selectedCategory?.name)
                }
            }

            Div({
                style {
                    width(100.percent)
                    flexShrink(0)
                    display(DisplayStyle.Flex)
                    justifyContent(JustifyContent.SpaceBetween)
                    marginTop(.5.em)
                    marginBottom(.5.em)
                }
            }) {
                QueryStatus(searchResult)
                torrentSearch.enabledProviders().forEach { provider ->
                    val result = providerResults.find { it.providerName == provider.name }
                    val providerStatus by derivedStateOf {
                        when (result) {
                            is ProviderResult.Success -> result.torrents.size.toString()
                            is ProviderResult.Error -> result::class.simpleName
                            else -> ""
                        }
                    }
                    Div { Text("${provider.name}: $providerStatus") }
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
                        val actualTorrent = resolvedTorrents[torrent.title] ?: torrent
                        TorrentItem(actualTorrent, onResolve)
                    }
                }
            }
        }
    }
}

private fun updateQueryParam(name: String, value: String?, vararg reset: String) {
    val params = URLSearchParams(window.location.search)
    reset.forEach { params.delete(it) }
    if (value.isNullOrBlank()) {
        params.delete(name)
    } else {
        params.set(name, value.encodeURLQueryComponent())
    }
    window.history.replaceState(null, window.document.title, "?${params}")
}