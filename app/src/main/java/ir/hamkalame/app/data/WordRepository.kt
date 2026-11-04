package ir.hamkalame.app.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class WordRepository(context: Context) {
    private val appContext = context.applicationContext
    private val preferences = appContext.getSharedPreferences("hamkalame_local", Context.MODE_PRIVATE)

    fun load(): WordLibrary {
        val categories = mutableListOf<WordCategory>()
        val words = mutableListOf<WordEntry>()

        appContext.assets.open("word_bank.json").bufferedReader(Charsets.UTF_8).use { reader ->
            val root = JSONObject(reader.readText())
            val categoryArray = root.getJSONArray("categories")
            for (i in 0 until categoryArray.length()) {
                val categoryJson = categoryArray.getJSONObject(i)
                val categoryId = categoryJson.getInt("id")
                categories += WordCategory(categoryId, categoryJson.getString("name"))
                val wordArray = categoryJson.getJSONArray("words")
                for (j in 0 until wordArray.length()) {
                    val wordJson = wordArray.getJSONObject(j)
                    words += WordEntry(
                        id = wordJson.getString("id"),
                        categoryId = categoryId,
                        text = wordJson.getString("text"),
                        difficulty = wordJson.optInt("difficulty", 1).coerceIn(1, 3),
                        isCustom = wordJson.optString("source") == "hamkalame"
                    )
                }
            }
        }

        val customCategoriesJson = JSONArray(preferences.getString("custom_categories", "[]"))
        for (i in 0 until customCategoriesJson.length()) {
            val item = customCategoriesJson.getJSONObject(i)
            categories += WordCategory(item.getInt("id"), item.getString("name"), isCustom = true)
        }

        val customWordsJson = JSONArray(preferences.getString("custom_words", "[]"))
        for (i in 0 until customWordsJson.length()) {
            val item = customWordsJson.getJSONObject(i)
            words += WordEntry(
                id = item.getString("id"),
                categoryId = item.getInt("categoryId"),
                text = item.getString("text"),
                difficulty = item.optInt("difficulty", 1).coerceIn(1, 3),
                isCustom = true
            )
        }

        return WordLibrary(categories, words)
    }

    fun addWord(library: WordLibrary, categoryId: Int, rawText: String, difficulty: Int): WordLibrary? {
        val text = normalize(rawText)
        if (text.isBlank()) return null
        if (library.words.any { it.categoryId == categoryId && normalize(it.text).equals(text, ignoreCase = true) }) return null
        val newEntry = WordEntry(
            id = "c_${UUID.randomUUID()}",
            categoryId = categoryId,
            text = text,
            difficulty = difficulty.coerceIn(1, 3),
            isCustom = true
        )
        return library.copy(words = library.words + newEntry).also(::saveCustomData)
    }

    fun addCategory(library: WordLibrary, rawName: String): WordLibrary? {
        val name = normalize(rawName)
        if (name.isBlank()) return null
        if (library.categories.any { it.name.equals(name, ignoreCase = true) }) return null
        val nextId = (library.categories.maxOfOrNull { it.id } ?: 1000) + 1
        val category = WordCategory(nextId, name, isCustom = true)
        return library.copy(categories = library.categories + category).also(::saveCustomData)
    }

    fun removeCustomWord(library: WordLibrary, wordId: String): WordLibrary {
        return library.copy(words = library.words.filterNot { it.id == wordId && it.isCustom }).also(::saveCustomData)
    }

    private fun saveCustomData(library: WordLibrary) {
        val categories = JSONArray()
        library.categories.filter { it.isCustom }.forEach { category ->
            categories.put(JSONObject().put("id", category.id).put("name", category.name))
        }
        val words = JSONArray()
        library.words.filter { it.isCustom }.forEach { word ->
            words.put(
                JSONObject()
                    .put("id", word.id)
                    .put("categoryId", word.categoryId)
                    .put("text", word.text)
                    .put("difficulty", word.difficulty)
            )
        }
        preferences.edit()
            .putString("custom_categories", categories.toString())
            .putString("custom_words", words.toString())
            .apply()
    }

    private fun normalize(value: String): String = value.trim().replace(Regex("\\s+"), " ")
}
