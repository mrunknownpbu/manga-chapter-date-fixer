package fetcher

class MalReleaseDateProvider(
    override val priority: Int,
    val clientId: String? = null
) : ReleaseDateProvider {
    override val name = "mal"

    override fun fetchReleaseDate(seriesTitle: String, chapterNumber: String): String? {
        println("Fetching release date from MAL for $seriesTitle Chapter $chapterNumber")
        return null // Implement API logic
    }
}