package fetcher

class BangumiReleaseDateProvider(
    override val priority: Int,
    val token: String? = null
) : ReleaseDateProvider {
    override val name = "bangumi"

    override fun fetchReleaseDate(seriesTitle: String, chapterNumber: String): String? {
        println("Fetching release date from Bangumi for $seriesTitle Chapter $chapterNumber")
        return null // Implement API logic
    }
}