package torrentsearch.web.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectIndexed
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.isActive
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Text
import torrentsearch.models.SearchResult

@Composable
fun QueryStatus(searchResult: SearchResult?) {
    val completedState by produceState<String?>(null, searchResult) {
        if (searchResult == null) {
            value = null
        } else {
            value = "Loading 0 / ${searchResult.providerCount()}"
            searchResult.providerResults().collectIndexed { index, _ ->
                value = buildString {
                    append("Loading ")
                    append(index + 1)
                    append(" / ")
                    append(searchResult.providerResultCount())
                }
            }
            value = "Completed ${searchResult.providerResults().count()} (${searchResult.errors().count()} errors)"
        }
    }
    val loading by produceState<String?>(null, searchResult) {
        if (searchResult?.isCompleted() == false) {
            value = "."
            while (isActive && !searchResult.isCompleted()) {
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
            display(DisplayStyle.Flex)
            gap(.5.em)
            width(220.px)
        }
    }) {
        Div { Text("Status:") }
        Div { Text(completedState ?: "(idle)") }
        Div { Text(loading.orEmpty()) }
    }
}