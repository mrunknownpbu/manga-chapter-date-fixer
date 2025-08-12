package fetcher

class AniListReleaseDateProvider(
    override val priority: Int,
    val clientId: String? = null
) : ReleaseDateProvider {
    override val name = "aniList"

    override fun fetchReleaseDate(seriesTitle: String, chapterNumber: String): String? {
        println("Fetching release date from AniList for $seriesTitle Chapter $chapterNumber")
        return null // Implement API logic
    }
}