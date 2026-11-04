package ir.hamkalame.app.data

data class WordCategory(
    val id: Int,
    val name: String,
    val isCustom: Boolean = false
)

data class WordEntry(
    val id: String,
    val categoryId: Int,
    val text: String,
    val difficulty: Int,
    val isCustom: Boolean = false
)

data class WordLibrary(
    val categories: List<WordCategory>,
    val words: List<WordEntry>
)

data class GameConfig(
    val playerCount: Int = 4,
    val playerNames: List<String> = (1..10).map { "بازیکن $it" },
    val teamNames: List<String> = listOf("تیم آبی", "تیم نارنجی", "تیم بنفش", "تیم سبز", "تیم قرمز"),
    val rounds: Int = 3,
    val turnSeconds: Int = 60,
    val difficulty: Int = 0,
    val selectedCategoryIds: Set<Int> = emptySet()
)

data class GameTeam(
    val name: String,
    val players: List<String>,
    val score: Int = 0,
    val turnsTaken: Int = 0
)

data class GameSession(
    val config: GameConfig,
    val teams: List<GameTeam>,
    val round: Int,
    val activeTeamIndex: Int,
    val secondsRemaining: Int,
    val currentWord: WordEntry,
    val usedWordIds: Set<String>,
    val isRunning: Boolean = false,
    val isFinished: Boolean = false,
    val turnSerial: Int = 0,
    val lastTurnExpired: Boolean = false
)
