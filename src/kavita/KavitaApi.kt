package kavita

data class KavitaChapter(val id: String, val number: String, var releaseDate: String?)
data class KavitaSeries(val id: String, val title: String, val chapters: List<KavitaChapter>)

class KavitaApi(val baseUrl: String, val apiKey: String) {
    fun getSeries(): List<KavitaSeries> {
        // TODO: Implement API call to Kavita to get series and chapters
        return emptyList()
    }
    fun updateChapterReleaseDate(chapterId: String, newDate: String) {
        // TODO: Implement API call to update chapter release date in Kavita
        println("Would update Kavita chapter $chapterId to $newDate")
    }
}