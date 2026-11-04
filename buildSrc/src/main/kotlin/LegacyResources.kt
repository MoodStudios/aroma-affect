import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import java.io.File

// Resource rewrites applied to pre-26 (1.21.1) builds after processResources
object LegacyResources {

    private val ingredientFields = listOf("ingredient", "template", "base", "addition")

    // 1.21.2 changed ingredients from {"item": ...}/{"tag": ...} objects to plain strings; 1.21.1 only reads the objects
    fun convertRecipes(dataDir: File) {
        if (!dataDir.isDirectory) return
        dataDir.listFiles()?.forEach { namespace ->
            val recipeDir = namespace.resolve("recipe")
            if (!recipeDir.isDirectory) return@forEach
            recipeDir.walkTopDown().filter { it.isFile && it.extension == "json" }.forEach { file ->
                @Suppress("UNCHECKED_CAST")
                val json = JsonSlurper().parse(file) as? MutableMap<String, Any?> ?: return@forEach
                var changed = false
                (json["key"] as? MutableMap<String, Any?>)?.let { key ->
                    key.entries.forEach { entry ->
                        entry.setValue(toLegacyIngredient(entry.value))
                    }
                    changed = true
                }
                (json["ingredients"] as? List<*>)?.let { list ->
                    json["ingredients"] = list.map { toLegacyIngredient(it) }
                    changed = true
                }
                ingredientFields.forEach { field ->
                    if (json[field] != null) {
                        json[field] = toLegacyIngredient(json[field])
                        changed = true
                    }
                }
                if (changed) {
                    file.writeText(JsonOutput.prettyPrint(JsonOutput.toJson(json)))
                }
            }
        }
    }

    private fun toLegacyIngredient(value: Any?): Any? = when (value) {
        is String -> if (value.startsWith("#")) mapOf("tag" to value.substring(1)) else mapOf("item" to value)
        is List<*> -> value.map { toLegacyIngredient(it) }
        else -> value
    }

    // src/legacy/mixins.json: {"remove": [names], "mixins": [names], "client": [names], "server": [names]}
    fun applyMixinOverrides(mixinConfig: File, overrides: File) {
        if (!mixinConfig.isFile || !overrides.isFile) return
        @Suppress("UNCHECKED_CAST")
        val config = JsonSlurper().parse(mixinConfig) as MutableMap<String, Any?>
        @Suppress("UNCHECKED_CAST")
        val changes = JsonSlurper().parse(overrides) as Map<String, Any?>
        val removed = (changes["remove"] as? List<*>)?.map { it.toString() }?.toSet() ?: emptySet()
        for (section in listOf("mixins", "client", "server")) {
            val current = (config[section] as? List<*>)?.map { it.toString() } ?: emptyList()
            val added = (changes[section] as? List<*>)?.map { it.toString() } ?: emptyList()
            val result = current.filterNot { it in removed } + added.filterNot { it in current }
            if (result.isNotEmpty() || config.containsKey(section)) {
                config[section] = result
            }
        }
        mixinConfig.writeText(JsonOutput.prettyPrint(JsonOutput.toJson(config)))
    }
}
