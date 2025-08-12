import config.ReleaseDateProvidersConfig
import fetcher.*
import komga.KomgaApi
import kavita.KavitaApi

fun main(args: Array<String>) {
    val configPath = args.getOrNull(0) ?: "chapterReleaseDateProviders.yaml"
    val config = ReleaseDateProvidersConfig.load(configPath)

    val komgaApi = KomgaApi(baseUrl = "http://localhost:25600", apiKey = "YOUR_KOMGA_API_KEY")
    val kavitaApi = KavitaApi(baseUrl = "http://localhost:5000", apiKey = "YOUR_KAVITA_API_KEY")

    val providers = listOfNotNull(
        config.releaseDateProviders["mangaUpdates"]?.let {
            MangaUpdatesReleaseDateProvider(priority = it.priority)
        },
        config.releaseDateProviders["mangaDex"]?.let {
            MangaDexReleaseDateProvider(priority = it.priority, coverLanguages = it.coverLanguages)
        },
        config.releaseDateProviders["aniList"]?.let {
            AniListReleaseDateProvider(priority = it.priority, clientId = it.clientId)
        },
        config.releaseDateProviders["mal"]?.let {
            MalReleaseDateProvider(priority = it.priority, clientId = it.clientId)
        },
        config.releaseDateProviders["bangumi"]?.let {
            BangumiReleaseDateProvider(priority = it.priority, token = it.token)
        },
        config.releaseDateProviders["comicVine"]?.let {
            ComicVineReleaseDateProvider(priority = it.priority, apiKey = it.apiKey)
        },
        config.releaseDateProviders["customCsv"]?.let {
            if (it.filePath != null) CustomCsvReleaseDateProvider(priority = it.priority, filePath = it.filePath) else null
        }
    ).sortedBy { it.priority }

    val provider = providers.firstOrNull { config.releaseDateProviders[it.name]?.enabled == true }
        ?: error("No enabled release date provider found")

    println("Using provider: ${provider.name}")

    // Komga update
    for (series in komgaApi.getSeries()) {
        for (chapter in series.chapters) {
            val fetchedDate = provider.fetchReleaseDate(series.title, chapter.number)
            if (fetchedDate != null && (config.updateIfDifferent && chapter.releaseDate != fetchedDate)) {
                if (!config.dryRun) komgaApi.updateChapterReleaseDate(chapter.id, fetchedDate)
                println("Komga: Updated ${series.title} Chapter ${chapter.number}: $fetchedDate")
            }
        }
    }

    // Kavita update
    for (series in kavitaApi.getSeries()) {
        for (chapter in series.chapters) {
            val fetchedDate = provider.fetchReleaseDate(series.title, chapter.number)
            if (fetchedDate != null && (config.updateIfDifferent && chapter.releaseDate != fetchedDate)) {
                if (!config.dryRun) kavitaApi.updateChapterReleaseDate(chapter.id, fetchedDate)
                println("Kavita: Updated ${series.title} Chapter ${chapter.number}: $fetchedDate")
            }
        }
    }
}