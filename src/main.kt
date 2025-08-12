import config.ReleaseDateProvidersConfig
import fetcher.*
import komga.KomgaApi
import kavita.KavitaApi
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.http.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.serialization.jackson.*
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule

data class HealthResponse(val status: String)
data class RunResponse(val message: String, val summary: String)

fun runUpdate(configPath: String): String {
    val results = StringBuilder()
    
    // Use the passed configPath parameter
    val actualConfigPath = configPath.ifEmpty { "chapterReleaseDateProviders.yaml" }
    
    results.appendLine("Loading configuration from: $actualConfigPath")
    val config = ReleaseDateProvidersConfig.load(actualConfigPath)

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

    results.appendLine("Using provider: ${provider.name}")

    var komgaUpdates = 0
    var kavitaUpdates = 0

    // Komga update
    for (series in komgaApi.getSeries()) {
        for (chapter in series.chapters) {
            val fetchedDate = provider.fetchReleaseDate(series.title, chapter.number)
            if (fetchedDate != null && (config.updateIfDifferent && chapter.releaseDate != fetchedDate)) {
                if (!config.dryRun) komgaApi.updateChapterReleaseDate(chapter.id, fetchedDate)
                results.appendLine("Komga: Updated ${series.title} Chapter ${chapter.number}: $fetchedDate")
                komgaUpdates++
            }
        }
    }

    // Kavita update
    for (series in kavitaApi.getSeries()) {
        for (chapter in series.chapters) {
            val fetchedDate = provider.fetchReleaseDate(series.title, chapter.number)
            if (fetchedDate != null && (config.updateIfDifferent && chapter.releaseDate != fetchedDate)) {
                if (!config.dryRun) kavitaApi.updateChapterReleaseDate(chapter.id, fetchedDate)
                results.appendLine("Kavita: Updated ${series.title} Chapter ${chapter.number}: $fetchedDate")
                kavitaUpdates++
            }
        }
    }
    
    results.appendLine("Summary: Updated $komgaUpdates Komga chapters and $kavitaUpdates Kavita chapters")
    return results.toString()
}

fun main(args: Array<String>) {
    // Priority: CLI argument > environment variable > default
    val configPath = when {
        args.isNotEmpty() -> args[0]
        System.getenv("CONFIG_PATH") != null -> System.getenv("CONFIG_PATH")
        else -> "chapterReleaseDateProviders.yaml"
    }
    
    println("Starting Manga Chapter Date Fixer Web Server...")
    println("Configuration: $configPath")
    
    embeddedServer(Netty, port = 1996, host = "0.0.0.0") {
        install(ContentNegotiation) {
            jackson {
                registerKotlinModule()
            }
        }
        
        routing {
            get("/health") {
                call.respond(HttpStatusCode.OK, HealthResponse("ok"))
            }
            
            post("/run") {
                try {
                    val summary = runUpdate(configPath)
                    call.respond(HttpStatusCode.OK, RunResponse("Update completed successfully", summary))
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, RunResponse("Update failed: ${e.message}", ""))
                }
            }
        }
    }.start(wait = true)
}