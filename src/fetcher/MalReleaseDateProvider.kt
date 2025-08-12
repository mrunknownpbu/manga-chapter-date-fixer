package fetcher

class MalReleaseDateProvider(
    override val priority: Int,
    val clientId: String? = null
) : ReleaseDateProvider {
    override val name = "mal"

    override fun fetchReleaseDate(seriesTitle: String, chapterNumber: String): ReleaseDateFinding? {
        println("Fetching release date from MAL for $seriesTitle Chapter $chapterNumber")
        return null // TODO: Implement API logic returning ReleaseDateFinding
    }
}