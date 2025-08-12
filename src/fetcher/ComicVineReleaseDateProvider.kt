package fetcher

class ComicVineReleaseDateProvider(
    override val priority: Int,
    val apiKey: String? = null
) : ReleaseDateProvider {
    override val name = "comicVine"

    override fun fetchReleaseDate(seriesTitle: String, chapterNumber: String): ReleaseDateFinding? {
        println("Fetching release date from ComicVine for $seriesTitle Chapter $chapterNumber")
        return null // TODO: Implement API logic returning ReleaseDateFinding
    }
}