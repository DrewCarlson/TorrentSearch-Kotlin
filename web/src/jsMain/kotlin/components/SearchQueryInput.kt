package torrentsearch.web.components

import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.attributes.placeholder
import org.jetbrains.compose.web.dom.Input

@Composable
fun SearchQueryInput(
    placeholder: String,
    searchQuery: String?,
    onQueryChanged: (String) -> Unit,
) {
    Input(InputType.Text) {
        placeholder(placeholder)
        defaultValue(searchQuery.orEmpty())
        onInput { onQueryChanged(it.value) }
    }
}