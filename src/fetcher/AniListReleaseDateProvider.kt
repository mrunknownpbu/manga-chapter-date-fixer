package fetcher

class AniListReleaseDateProvider(
    override val priority: Int,
    val clientId: String? = null
) : ReleaseDateProvider {
    override val name = "aniList"

    override fun fetchReleaseDate(seriesTitle: String, chapterNumber: String): ReleaseDateFinding? {
        println("Fetching release date from AniList for $seriesTitle Chapter $chapterNumber")
        return null // TODO: Implement API logic returning ReleaseDateFinding
    }
}