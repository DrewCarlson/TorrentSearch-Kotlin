package torrentsearch.web.components

import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.A
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Text
import torrentsearch.models.TorrentDescription

@Composable
fun TorrentItem(torrent: TorrentDescription) {
    Div({
        style {
            width(100.percent)
            display(DisplayStyle.Flex)
            gap(1.5.em)
            marginTop(.5.em)
            marginBottom(.5.em)
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
        Div {
            if (torrent.infoUrl.isNullOrBlank()) {
                Text("(no url)")
            } else {

            }
        }
    }
}
