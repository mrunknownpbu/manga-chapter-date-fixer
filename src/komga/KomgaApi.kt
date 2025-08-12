package komga

data class KomgaChapter(val id: String, val number: String, var releaseDate: String?)
data class KomgaSeries(val id: String, val title: String, val chapters: List<KomgaChapter>)

class KomgaApi(val baseUrl: String, val apiKey: String) {
    fun getSeries(): List<KomgaSeries> {
        // TODO: Implement API call to Komga to get series and chapters
        return emptyList()
    }
    fun updateChapterReleaseDate(chapterId: String, newDate: String) {
        // TODO: Implement API call to update chapter release date in Komga
        println("Would update Komga chapter $chapterId to $newDate")
    }
}