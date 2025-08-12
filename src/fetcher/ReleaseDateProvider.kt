package fetcher

interface ReleaseDateProvider {
    fun fetchReleaseDate(seriesTitle: String, chapterNumber: String): String?
    val name: String
    val priority: Int
}