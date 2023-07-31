package torrentsearch.models

/**
 * [ResolveResultSet] is a container for a collection of [ProviderResult]s
 * which simplifies accessing the resolve details across multiple providers.
 */
public class ResolveResultSet(
    /**
     * The original list of [ResolveResult]s from each provider.
     */
    public val results: List<ResolveResult>,
) {

    /**
     * Contains a list of all the successfully resolved [TorrentDescription]s.
     */
    public val resolved: List<TorrentDescription> = results.flatMap { it.torrents }

    /**
     * Indicates if any of the [ResolveResult]s in [results] are an error.
     */
    public val hasErrors: Boolean = results.any { it is ResolveResult.Error }

    /**
     * Returns a list of all the collected [ResolveResult.Error] results.
     */
    public val failed: List<ResolveResult.Error> = results.filterIsInstance<ResolveResult.Error>()
}
