package fetcher

import java.io.File

class CustomCsvReleaseDateProvider(
    override val priority: Int,
    val filePath: String
) : ReleaseDateProvider {
    override val name = "customCsv"

    private val cache = mutableMapOf<Pair<String, String>, String>()

    init {
        loadCsv()
    }

    private fun loadCsv() {
        File(filePath).takeIf { it.exists() }?.forEachLine { line ->
            val (title, chapter, date) = line.split(",").map { it.trim() }
            cache[title to chapter] = date
        }
    }

    override fun fetchReleaseDate(seriesTitle: String, chapterNumber: String): String? {
        println("Fetching release date from custom CSV for $seriesTitle Chapter $chapterNumber")
        return cache[seriesTitle to chapterNumber]
    }
}