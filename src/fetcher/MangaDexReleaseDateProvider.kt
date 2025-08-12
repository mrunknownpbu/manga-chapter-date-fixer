package fetcher

class MangaDexReleaseDateProvider(
    override val priority: Int,
    val coverLanguages: List<String>? = null
) : ReleaseDateProvider {
    override val name = "mangaDex"

    override fun fetchReleaseDate(seriesTitle: String, chapterNumber: String): String? {
        println("Fetching release date from MangaDex for $seriesTitle Chapter $chapterNumber")
        return null // Implement API logic
    }
}