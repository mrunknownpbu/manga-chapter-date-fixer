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
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import io.ktor.server.plugins.cors.routing.*

// Simple normalization: accept ISO-8601 date or datetime and output YYYY-MM-DD
private fun normalizeDate(input: String): String? {
    val dateFormats = listOf(
        DateTimeFormatter.ISO_LOCAL_DATE,
        DateTimeFormatter.ISO_OFFSET_DATE_TIME,
        DateTimeFormatter.ISO_LOCAL_DATE_TIME
    )
    for (fmt in dateFormats) {
        try {
            return when (fmt) {
                DateTimeFormatter.ISO_LOCAL_DATE -> LocalDate.parse(input, fmt).toString()
                DateTimeFormatter.ISO_OFFSET_DATE_TIME -> OffsetDateTime.parse(input, fmt).toLocalDate().toString()
                DateTimeFormatter.ISO_LOCAL_DATE_TIME -> java.time.LocalDateTime.parse(input, fmt).toLocalDate().toString()
                else -> null
            }
        } catch (_: DateTimeParseException) { /* try next */ }
    }
    return null
}

data class HealthResponse(val status: String)
data class RunResponse(val message: String, val summary: String)

data class UpdateResultSummary(
    val komgaUpdated: Int,
    val kavitaUpdated: Int,
    val attempts: List<String>
)

private fun fetchWithFallback(
    providers: List<ReleaseDateProvider>,
    title: String,
    chapter: String,
    attempts: MutableList<String>
): ReleaseDateFinding? {
    for (provider in providers) {
        val finding = provider.fetchReleaseDate(title, chapter)
        attempts.add("${provider.name}: ${finding?.normalizedDate ?: "no-match"}")
        if (finding != null) {
            val normalized = normalizeDate(finding.normalizedDate)
            if (normalized != null) {
                return finding.copy(normalizedDate = normalized)
            }
        }
    }
    return null
}

private fun enabledProviders(config: ReleaseDateProvidersConfig): List<ReleaseDateProvider> = listOfNotNull(
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
).filter { config.releaseDateProviders[it.name]?.enabled == true }
 .sortedBy { it.priority }

fun runUpdate(configPath: String): String {
    val results = StringBuilder()
    
    val actualConfigPath = configPath.ifEmpty { "chapterReleaseDateProviders.yaml" }
    
    results.appendLine("Loading configuration from: $actualConfigPath")
    val config = ReleaseDateProvidersConfig.load(actualConfigPath)

    val komgaApi = KomgaApi(baseUrl = "http://localhost:25600", apiKey = "YOUR_KOMGA_API_KEY")
    val kavitaApi = KavitaApi(baseUrl = "http://localhost:5000", apiKey = "YOUR_KAVITA_API_KEY")

    val providers = enabledProviders(config)
    if (providers.isEmpty()) error("No enabled release date provider found")

    results.appendLine("Using providers (by priority): ${providers.joinToString { it.name }}")

    var komgaUpdates = 0
    var kavitaUpdates = 0

    // Komga update
    for (series in komgaApi.getSeries()) {
        for (chapter in series.chapters) {
            val attempts = mutableListOf<String>()
            val finding = fetchWithFallback(providers, series.title, chapter.number, attempts)
            if (finding != null && (config.updateIfDifferent && chapter.releaseDate != finding.normalizedDate)) {
                if (!config.dryRun) komgaApi.updateChapterReleaseDate(chapter.id, finding.normalizedDate)
                results.appendLine("Komga: Updated ${series.title} Chapter ${chapter.number}: ${finding.normalizedDate} (${finding.source}, conf=${finding.confidence})")
                komgaUpdates++
            } else {
                results.appendLine("Komga: No update for ${series.title} Chapter ${chapter.number} [${attempts.joinToString(" | ")}]")
            }
        }
    }

    // Kavita update
    for (series in kavitaApi.getSeries()) {
        for (chapter in series.chapters) {
            val attempts = mutableListOf<String>()
            val finding = fetchWithFallback(providers, series.title, chapter.number, attempts)
            if (finding != null && (config.updateIfDifferent && chapter.releaseDate != finding.normalizedDate)) {
                if (!config.dryRun) kavitaApi.updateChapterReleaseDate(chapter.id, finding.normalizedDate)
                results.appendLine("Kavita: Updated ${series.title} Chapter ${chapter.number}: ${finding.normalizedDate} (${finding.source}, conf=${finding.confidence})")
                kavitaUpdates++
            } else {
                results.appendLine("Kavita: No update for ${series.title} Chapter ${chapter.number} [${attempts.joinToString(" | ")}]")
            }
        }
    }
    
    results.appendLine("Summary: Updated $komgaUpdates Komga chapters and $kavitaUpdates Kavita chapters")
    return results.toString()
}

fun main(args: Array<String>) {
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
        install(CORS) {
            anyHost()
            allowHeader(HttpHeaders.ContentType)
            allowHeader(HttpHeaders.Authorization)
            allowMethod(HttpMethod.Get)
            allowMethod(HttpMethod.Post)
            allowMethod(HttpMethod.Options)
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

            // Preview providers for a specific series/chapter
            get("/preview") {
                val title = call.request.queryParameters["title"]
                val chapter = call.request.queryParameters["chapter"]
                if (title.isNullOrBlank() || chapter.isNullOrBlank()) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing title or chapter"))
                    return@get
                }
                val cfg = ReleaseDateProvidersConfig.load(configPath)
                val providers = enabledProviders(cfg)
                val attempts = mutableListOf<String>()
                val finding = fetchWithFallback(providers, title, chapter, attempts)
                call.respond(HttpStatusCode.OK, mapOf(
                    "attempts" to attempts,
                    "best" to finding
                ))
            }

            // Update a specific Komga or Kavita chapter
            post("/update-chapter") {
                val library = call.request.queryParameters["library"] // komga|kavita
                val seriesTitle = call.request.queryParameters["title"]
                val chapterNumber = call.request.queryParameters["chapter"]
                if (library.isNullOrBlank() || seriesTitle.isNullOrBlank() || chapterNumber.isNullOrBlank()) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing library, title or chapter"))
                    return@post
                }
                val cfg = ReleaseDateProvidersConfig.load(configPath)
                val providers = enabledProviders(cfg)
                val attempts = mutableListOf<String>()
                val finding = fetchWithFallback(providers, seriesTitle, chapterNumber, attempts)
                if (finding == null) {
                    call.respond(HttpStatusCode.NotFound, mapOf("message" to "No release date found", "attempts" to attempts))
                    return@post
                }
                val dryRun = cfg.dryRun
                when (library.lowercase()) {
                    "komga" -> {
                        val api = KomgaApi("http://localhost:25600", "YOUR_KOMGA_API_KEY")
                        if (!dryRun) api.updateChapterReleaseDate("UNKNOWN", finding.normalizedDate)
                    }
                    "kavita" -> {
                        val api = KavitaApi("http://localhost:5000", "YOUR_KAVITA_API_KEY")
                        if (!dryRun) api.updateChapterReleaseDate("UNKNOWN", finding.normalizedDate)
                    }
                    else -> {
                        call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Unsupported library"))
                        return@post
                    }
                }
                call.respond(HttpStatusCode.OK, mapOf(
                    "updated" to !dryRun,
                    "date" to finding.normalizedDate,
                    "source" to finding.source,
                    "attempts" to attempts
                ))
            }
        }
    }.start(wait = true)
}