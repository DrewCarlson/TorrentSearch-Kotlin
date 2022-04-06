package torrentsearch.web.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import org.jetbrains.compose.web.attributes.selected
import org.jetbrains.compose.web.dom.Option
import org.jetbrains.compose.web.dom.Select
import org.jetbrains.compose.web.dom.Text
import torrentsearch.models.Category

@Composable
fun CategorySelect(
    selectedCategory: Category?,
    onCategorySelected: (Category?) -> Unit,
) {
    Select({
        onInput { event ->
            val selection = event.value?.takeUnless { it.startsWith("(") }?.run(Category::valueOf)
            onCategorySelected(selection)
        }
    }) {
        val categories = remember { Category.values().toList() - Category.MOVIES - Category.TV }
        Option("(category)", {
            if (selectedCategory == null) {
                selected()
            }
        }) { Text("(none)") }
        Option("MOVIES", {
            if (selectedCategory == Category.MOVIES) {
                selected()
            }
        }) { Text("Movies") }
        Option("TV", {
            if (selectedCategory == Category.TV) {
                selected()
            }
        }) { Text("Tv") }
        (categories - Category.MOVIES - Category.TV).forEach { category ->
            Option(category.name, {
                if (category == selectedCategory) {
                    selected()
                }
            }) {
                val name = remember { category.name.lowercase().replaceFirstChar { it.uppercaseChar() } }
                Text(name)
            }
        }
    }
}