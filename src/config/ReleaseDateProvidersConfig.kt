package config

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.ObjectMapper
import org.yaml.snakeyaml.Yaml
import java.io.File

@JsonIgnoreProperties(ignoreUnknown = true)
data class ReleaseDateProviderConfig(
    val priority: Int = 0,
    val enabled: Boolean = false,
    val apiKey: String? = null,
    val clientId: String? = null,
    val token: String? = null,
    val mediaType: String? = null,
    val coverLanguages: List<String>? = null,
    val filePath: String? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ReleaseDateProvidersConfig(
    val releaseDateProviders: Map<String, ReleaseDateProviderConfig> = mapOf(),
    val defaultProvider: String = "mangaUpdates",
    val updateIfDifferent: Boolean = true,
    val dryRun: Boolean = false
) {
    companion object {
        fun load(configPath: String): ReleaseDateProvidersConfig {
            val yaml = Yaml()
            val input = File(configPath).inputStream()
            val map = yaml.load<Map<String, Any>>(input)
            val mapper = ObjectMapper().findAndRegisterModules()
            return mapper.convertValue(map, ReleaseDateProvidersConfig::class.java)
        }
    }
}