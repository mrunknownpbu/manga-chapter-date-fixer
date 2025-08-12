package fetcher

class MangaUpdatesReleaseDateProvider(
    override val priority: Int
) : ReleaseDateProvider {
    override val name = "mangaUpdates"

    override fun fetchReleaseDate(seriesTitle: String, chapterNumber: String): ReleaseDateFinding? {
        println("Fetching release date from MangaUpdates for $seriesTitle Chapter $chapterNumber")
        return null // TODO: Implement API logic returning ReleaseDateFinding
    }
}