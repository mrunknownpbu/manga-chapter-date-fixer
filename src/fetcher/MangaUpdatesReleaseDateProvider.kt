package fetcher

class MangaUpdatesReleaseDateProvider(
    override val priority: Int
) : ReleaseDateProvider {
    override val name = "mangaUpdates"

    override fun fetchReleaseDate(seriesTitle: String, chapterNumber: String): String? {
        println("Fetching release date from MangaUpdates for $seriesTitle Chapter $chapterNumber")
        return null // Implement API logic
    }
}