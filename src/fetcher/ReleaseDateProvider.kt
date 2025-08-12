package fetcher

// Structured finding for a chapter release date
data class ReleaseDateFinding(
    val seriesTitle: String,
    val chapterNumber: String,
    val normalizedDate: String,
    val source: String,
    val confidence: Double = 0.5,
    val rawDate: String? = null,
    val notes: String? = null
)

interface ReleaseDateProvider {
    fun fetchReleaseDate(seriesTitle: String, chapterNumber: String): ReleaseDateFinding?
    val name: String
    val priority: Int
}