package ir.hamkalame.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.ui.text.style.TextOverflow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import ir.hamkalame.app.data.GameConfig
import ir.hamkalame.app.data.GameSession
import ir.hamkalame.app.data.GameTeam
import ir.hamkalame.app.data.WordCategory
import ir.hamkalame.app.data.WordEntry
import ir.hamkalame.app.data.WordLibrary
import ir.hamkalame.app.data.WordRepository

private val Bg = Color(0xFF10131B)
private val Surface1 = Color(0xFF1A1F2B)
private val Surface2 = Color(0xFF232A38)
private val Line = Color(0xFF343D4E)
private val TextMain = Color(0xFFF4F6FA)
private val TextMuted = Color(0xFFA9B2C2)
private val Lime = Color(0xFFD7FF3F)
private val Cyan = Color(0xFF38D6CD)
private val Coral = Color(0xFFFF6B62)
private val Violet = Color(0xFF9A86FF)
private val Gold = Color(0xFFFFC857)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { HamKalameApp() }
    }
}

private enum class AppScreen { HOME, SETUP, GAME, WORDS, RESULTS }

@Composable
private fun HamKalameApp() {
    val context = LocalContext.current
    val repository = remember { WordRepository(context) }
    var library by remember { mutableStateOf<WordLibrary?>(null) }
    var screen by remember { mutableStateOf(AppScreen.HOME) }
    var config by remember { mutableStateOf(GameConfig()) }
    var session by remember { mutableStateOf<GameSession?>(null) }
    var gameWords by remember { mutableStateOf<List<WordEntry>>(emptyList()) }
    var categoriesInitialized by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        library = withContext(Dispatchers.IO) { repository.load() }
    }
    LaunchedEffect(library) {
        val loaded = library
        if (loaded != null && !categoriesInitialized) {
            config = config.copy(selectedCategoryIds = loaded.categories.map { it.id }.toSet())
            categoriesInitialized = true
        }
    }
    LaunchedEffect(session?.isFinished) {
        if (session?.isFinished == true) screen = AppScreen.RESULTS
    }

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Lime,
            onPrimary = Bg,
            secondary = Cyan,
            background = Bg,
            surface = Surface1,
            onSurface = TextMain,
            onBackground = TextMain,
            error = Coral
        )
    ) {
        androidx.compose.runtime.CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Surface(modifier = Modifier.fillMaxSize(), color = Bg) {
                val currentLibrary = library
                if (currentLibrary == null) {
                    LoadingScreen()
                } else {
                    when (screen) {
                        AppScreen.HOME -> HomeScreen(
                            wordCount = currentLibrary.words.size,
                            categoryCount = currentLibrary.categories.size,
                            onStart = { screen = AppScreen.SETUP },
                            onWords = { screen = AppScreen.WORDS }
                        )
                        AppScreen.SETUP -> SetupScreen(
                            library = currentLibrary,
                            config = config,
                            onConfigChange = { config = it },
                            onBack = { screen = AppScreen.HOME },
                            onStart = {
                                val selectedWords = currentLibrary.words.filter { word ->
                                    word.categoryId in config.selectedCategoryIds &&
                                        (config.difficulty == 0 || word.difficulty == config.difficulty)
                                }
                                if (selectedWords.isNotEmpty()) {
                                    gameWords = selectedWords
                                    val teams = (0 until (config.playerCount / 2)).map { index ->
                                        val first = config.playerNames.getOrNull(index * 2).orEmpty().ifBlank { "بازیکن ${index * 2 + 1}" }
                                        val second = config.playerNames.getOrNull(index * 2 + 1).orEmpty().ifBlank { "بازیکن ${index * 2 + 2}" }
                                        val teamName = config.teamNames.getOrNull(index).orEmpty().ifBlank { "تیم ${index + 1}" }
                                        GameTeam(teamName, listOf(first, second))
                                    }
                                    val firstWord = selectedWords.random()
                                    session = GameSession(
                                        config = config,
                                        teams = teams,
                                        round = 1,
                                        activeTeamIndex = 0,
                                        secondsRemaining = config.turnSeconds,
                                        currentWord = firstWord,
                                        usedWordIds = setOf(firstWord.id)
                                    )
                                    screen = AppScreen.GAME
                                }
                            }
                        )
                        AppScreen.GAME -> {
                            val currentSession = session
                            if (currentSession == null) {
                                screen = AppScreen.HOME
                            } else if (currentSession.isFinished) {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text("نتیجه بازی آماده است…", color = TextMuted)
                                }
                            } else {
                                GameScreen(
                                    session = currentSession,
                                    wordPool = gameWords,
                                    onSessionChange = { session = it },
                                    onBack = {
                                        session = null
                                        screen = AppScreen.HOME
                                    }
                                )
                            }
                        }
                        AppScreen.WORDS -> WordManagerScreen(
                            library = currentLibrary,
                            onLibraryChange = { library = it },
                            repository = repository,
                            onBack = { screen = AppScreen.HOME }
                        )
                        AppScreen.RESULTS -> {
                            val finished = session
                            if (finished == null) {
                                screen = AppScreen.HOME
                            } else {
                                ResultsScreen(
                                    session = finished,
                                    onHome = {
                                        session = null
                                        screen = AppScreen.HOME
                                    },
                                    onReplay = {
                                        val firstWord = gameWords.random()
                                        session = GameSession(
                                            config = finished.config,
                                            teams = finished.teams.map { it.copy(score = 0, turnsTaken = 0) },
                                            round = 1,
                                            activeTeamIndex = 0,
                                            secondsRemaining = finished.config.turnSeconds,
                                            currentWord = firstWord,
                                            usedWordIds = setOf(firstWord.id)
                                        )
                                        screen = AppScreen.GAME
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingScreen() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("هم‌کلمه", color = Lime, fontSize = 30.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(10.dp))
            Text("در حال آماده‌سازی بانک کلمات…", color = TextMuted, fontSize = 14.sp)
        }
    }
}

@Composable
private fun HomeScreen(
    wordCount: Int,
    categoryCount: Int,
    onStart: () -> Unit,
    onWords: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 22.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                modifier = Modifier.size(46.dp).clip(RoundedCornerShape(15.dp)).background(Lime),
                contentAlignment = Alignment.Center
            ) {
                Text("هـ", fontSize = 27.sp, color = Bg, fontWeight = FontWeight.Black)
            }
            Column {
                Text("هم‌کلمه", color = TextMain, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold)
                Text("بازی گروهی آفلاین", color = TextMuted, fontSize = 12.sp)
            }
            Spacer(Modifier.weight(1f))
            Surface(shape = CircleShape, color = Color(0xFF243A35)) {
                Text("بدون اینترنت", color = Cyan, modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Surface2)
        ) {
            Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("دورهمی رو\nبا یک کلمه شروع کن!", color = TextMain, fontSize = 28.sp, lineHeight = 35.sp, fontWeight = FontWeight.Black)
                Text("توضیح بده، حدس بزن، امتیاز بگیر. از ۴ تا ۱۰ نفر، بدون نیاز به اینترنت.", color = TextMuted, fontSize = 14.sp, lineHeight = 23.sp)
                Button(
                    onClick = onStart,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Lime, contentColor = Bg)
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(7.dp))
                    Text("شروع بازی", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PlayerCountPill("۴ نفر", Cyan)
                    PlayerCountPill("۶ نفر", Violet)
                    PlayerCountPill("۸ نفر", Gold)
                    PlayerCountPill("۱۰ نفر", Coral)
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard(value = wordCount.toPersianDigits(), label = "کلمه آماده", modifier = Modifier.weight(1f), accent = Lime)
            StatCard(value = categoryCount.toPersianDigits(), label = "دسته‌بندی", modifier = Modifier.weight(1f), accent = Cyan)
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Surface1),
            onClick = onWords
        ) {
            Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(Color(0xFF303044)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.LibraryBooks, contentDescription = null, tint = Violet)
                }
                Spacer(Modifier.width(13.dp))
                Column(Modifier.weight(1f)) {
                    Text("مدیریت بانک کلمات", color = TextMain, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text("افزودن کلمه و دسته‌بندی اختصاصی", color = TextMuted, fontSize = 12.sp)
                }
                Icon(Icons.Filled.Add, contentDescription = null, tint = Lime)
            }
        }

        Text("همه‌چیز روی گوشی ذخیره می‌شود؛ حتی کلماتی که خودت اضافه می‌کنی.", color = TextMuted, fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun PlayerCountPill(label: String, accent: Color) {
    Surface(shape = RoundedCornerShape(12.dp), color = accent.copy(alpha = 0.13f)) {
        Text(label, color = accent, modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp), fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun StatCard(value: String, label: String, modifier: Modifier = Modifier, accent: Color) {
    Card(modifier = modifier, shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Surface1)) {
        Column(Modifier.fillMaxWidth().padding(17.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(value, color = accent, fontSize = 25.sp, fontWeight = FontWeight.Black)
            Text(label, color = TextMuted, fontSize = 12.sp)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun SetupScreen(
    library: WordLibrary,
    config: GameConfig,
    onConfigChange: (GameConfig) -> Unit,
    onBack: () -> Unit,
    onStart: () -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        ScreenHeader(title = "تنظیم بازی", onBack = onBack)
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            SectionTitle("تعداد بازیکنان", "هر دو بازیکن یک تیم می‌سازند")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                listOf(4, 6, 8, 10).forEach { count ->
                    ChoiceChip(
                        label = "$count نفر",
                        selected = config.playerCount == count,
                        modifier = Modifier.weight(1f),
                        onClick = { onConfigChange(config.copy(playerCount = count)) }
                    )
                }
            }

            SectionTitle("تیم‌ها و بازیکنان", "نام‌ها را وارد کن یا همان نام‌های پیش‌فرض را نگه دار")
            repeat(config.playerCount / 2) { teamIndex ->
                Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Surface1)) {
                    Column(Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                        OutlinedTextField(
                            value = config.teamNames.getOrElse(teamIndex) { "تیم ${teamIndex + 1}" },
                            onValueChange = { value -> onConfigChange(config.copy(teamNames = config.teamNames.updated(teamIndex, value))) },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("نام تیم ${teamIndex + 1}") },
                            singleLine = true,
                            colors = fieldColors()
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = config.playerNames.getOrElse(teamIndex * 2) { "بازیکن ${teamIndex * 2 + 1}" },
                                onValueChange = { value -> onConfigChange(config.copy(playerNames = config.playerNames.updated(teamIndex * 2, value))) },
                                modifier = Modifier.weight(1f),
                                label = { Text("بازیکن ${teamIndex * 2 + 1}") },
                                singleLine = true,
                                colors = fieldColors()
                            )
                            OutlinedTextField(
                                value = config.playerNames.getOrElse(teamIndex * 2 + 1) { "بازیکن ${teamIndex * 2 + 2}" },
                                onValueChange = { value -> onConfigChange(config.copy(playerNames = config.playerNames.updated(teamIndex * 2 + 1, value))) },
                                modifier = Modifier.weight(1f),
                                label = { Text("بازیکن ${teamIndex * 2 + 2}") },
                                singleLine = true,
                                colors = fieldColors()
                            )
                        }
                    }
                }
            }

            SectionTitle("تعداد دورها", "هر تیم در هر دور یک نوبت بازی می‌کند")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                listOf(3, 5, 7).forEach { rounds ->
                    ChoiceChip(label = "$rounds دور", selected = config.rounds == rounds, modifier = Modifier.weight(1f)) {
                        onConfigChange(config.copy(rounds = rounds))
                    }
                }
            }

            SectionTitle("زمان هر نوبت", "زمان از لحظه شروع نوبت محاسبه می‌شود")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                listOf(60, 120).forEach { seconds ->
                    ChoiceChip(label = if (seconds == 60) "۱ دقیقه" else "۲ دقیقه", selected = config.turnSeconds == seconds, modifier = Modifier.weight(1f)) {
                        onConfigChange(config.copy(turnSeconds = seconds))
                    }
                }
            }

            SectionTitle("درجه سختی", "کلمات انتخاب‌شده از سطح دلخواه نمایش داده می‌شوند")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(0 to "همه", 1 to "آسان", 2 to "متوسط", 3 to "سخت").forEach { (value, label) ->
                    ChoiceChip(label, config.difficulty == value) { onConfigChange(config.copy(difficulty = value)) }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionTitle("دسته‌بندی کلمات", "حداقل یک دسته را انتخاب کن", modifier = Modifier.weight(1f))
                TextButton(onClick = {
                    val allIds = library.categories.map { it.id }.toSet()
                    onConfigChange(config.copy(selectedCategoryIds = if (config.selectedCategoryIds.size == allIds.size) emptySet() else allIds))
                }) {
                    Text(if (config.selectedCategoryIds.size == library.categories.size) "پاک کردن انتخاب" else "انتخاب همه", color = Cyan, fontSize = 11.sp)
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                library.categories.forEach { category ->
                    val count = library.words.count { it.categoryId == category.id }
                    ChoiceChip(
                        label = "${category.name} · ${count.toPersianDigits()}",
                        selected = category.id in config.selectedCategoryIds,
                        onClick = {
                            val updated = config.selectedCategoryIds.toMutableSet()
                            if (!updated.add(category.id)) updated.remove(category.id)
                            onConfigChange(config.copy(selectedCategoryIds = updated))
                        }
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }
        Column(Modifier.fillMaxWidth().background(Bg).padding(horizontal = 18.dp, vertical = 12.dp)) {
            Button(
                onClick = onStart,
                enabled = config.selectedCategoryIds.isNotEmpty(),
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(17.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Lime, contentColor = Bg, disabledContainerColor = Surface2, disabledContentColor = TextMuted)
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("بزن بریم", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GameScreen(
    session: GameSession,
    wordPool: List<WordEntry>,
    onSessionChange: (GameSession) -> Unit,
    onBack: () -> Unit
) {
    val latestSession by rememberUpdatedState(session)
    val latestOnChange by rememberUpdatedState(onSessionChange)

    LaunchedEffect(session.isRunning, session.secondsRemaining, session.turnSerial) {
        if (session.isRunning && !session.isFinished) {
            delay(1000)
            val current = latestSession
            if (current.isRunning && current.turnSerial == session.turnSerial) {
                if (current.secondsRemaining <= 1) {
                    latestOnChange(advanceTurn(current.copy(secondsRemaining = 0), wordPool, expired = true))
                } else {
                    latestOnChange(current.copy(secondsRemaining = current.secondsRemaining - 1))
                }
            }
        }
    }

    val activeTeam = session.teams[session.activeTeamIndex]
    val speakerIndex = activeTeam.turnsTaken % 2
    val speaker = activeTeam.players.getOrElse(speakerIndex) { "بازیکن" }
    val guesser = activeTeam.players.getOrElse(1 - speakerIndex) { "هم‌تیمی" }
    val timerColor = when {
        session.secondsRemaining <= 10 -> Coral
        session.secondsRemaining <= 30 -> Gold
        else -> Lime
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, modifier = Modifier.size(42.dp)) {
                Icon(Icons.Filled.Close, contentDescription = "خروج از بازی", tint = TextMuted)
            }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("هم‌کلمه", color = TextMain, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
                Text("دور ${session.round.toPersianDigits()} از ${session.config.rounds.toPersianDigits()}", color = TextMuted, fontSize = 11.sp)
            }
            Surface(shape = RoundedCornerShape(12.dp), color = Surface2) {
                Text("${session.activeTeamIndex + 1}/${session.teams.size}", color = Cyan, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), fontWeight = FontWeight.Bold)
            }
        }

        Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            session.teams.forEachIndexed { index, team ->
                val active = index == session.activeTeamIndex
                Card(
                    shape = RoundedCornerShape(15.dp),
                    colors = CardDefaults.cardColors(containerColor = if (active) Color(0xFF34402A) else Surface1),
                    border = if (active) BorderStroke(1.dp, Lime.copy(alpha = 0.8f)) else null
                ) {
                    Column(Modifier.widthIn(min = 82.dp).padding(horizontal = 13.dp, vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(team.name, color = if (active) Lime else TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(team.score.toString().toPersianDigits(), color = TextMain, fontSize = 21.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(26.dp), colors = CardDefaults.cardColors(containerColor = Surface1)) {
            Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Text("نوبت ${activeTeam.name}", color = Cyan, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text("$speaker توضیح می‌دهد  ←  $guesser حدس می‌زند", color = TextMuted, fontSize = 12.sp, textAlign = TextAlign.Center)
                Spacer(Modifier.height(12.dp))
                Text(session.currentWord.text, color = TextMain, fontSize = 31.sp, lineHeight = 40.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                Text("سطح ${listOf("آسان", "متوسط", "سخت").getOrElse(session.currentWord.difficulty - 1) { "آسان" }}", color = TextMuted, fontSize = 11.sp)
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.Timer, contentDescription = null, tint = timerColor, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                text = "${(session.secondsRemaining / 60).toString().toPersianDigits().padStart(2, '۰')}:${(session.secondsRemaining % 60).toString().toPersianDigits().padStart(2, '۰')}",
                color = timerColor,
                fontSize = 29.sp,
                fontWeight = FontWeight.Black
            )
        }

        if (!session.isRunning) {
            Card(shape = RoundedCornerShape(15.dp), colors = CardDefaults.cardColors(containerColor = if (session.lastTurnExpired) Color(0xFF452C2D) else Surface2)) {
                Text(
                    text = when {
                        session.lastTurnExpired -> "زمان این نوبت تمام شد؛ نوبت تیم بعدی است."
                        session.secondsRemaining < session.config.turnSeconds -> "بازی متوقف شده است. برای ادامه، تایمر را فعال کن."
                        else -> "وقتی آماده‌اید، تایمر را شروع کنید."
                    },
                    color = if (session.lastTurnExpired) Coral else TextMuted,
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            }
            Button(
                onClick = { onSessionChange(session.copy(isRunning = true, lastTurnExpired = false)) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Lime, contentColor = Bg)
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text(if (session.secondsRemaining == session.config.turnSeconds) "شروع نوبت" else "ادامه بازی", fontWeight = FontWeight.ExtraBold)
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = {
                        val next = drawNextWord(wordPool, session.usedWordIds)
                        val teams = session.teams.toMutableList()
                        teams[session.activeTeamIndex] = teams[session.activeTeamIndex].copy(score = teams[session.activeTeamIndex].score + 1)
                        onSessionChange(session.copy(teams = teams, currentWord = next.first, usedWordIds = next.second))
                    },
                    modifier = Modifier.weight(1.15f).height(54.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Lime, contentColor = Bg)
                ) {
                    Icon(Icons.Filled.Check, contentDescription = null)
                    Spacer(Modifier.width(5.dp))
                    Text("درست بود +۱", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                }
                Button(
                    onClick = {
                        val next = drawNextWord(wordPool, session.usedWordIds)
                        onSessionChange(session.copy(currentWord = next.first, usedWordIds = next.second))
                    },
                    modifier = Modifier.weight(0.85f).height(54.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Surface2, contentColor = TextMain)
                ) {
                    Icon(Icons.Filled.SkipNext, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("رد کردن", fontSize = 13.sp)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = { onSessionChange(session.copy(isRunning = false)) },
                    modifier = Modifier.weight(1f).height(46.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Surface2, contentColor = TextMain)
                ) {
                    Icon(Icons.Filled.Pause, contentDescription = null)
                    Spacer(Modifier.width(5.dp))
                    Text("مکث")
                }
                Button(
                    onClick = { onSessionChange(advanceTurn(session, wordPool, expired = false)) },
                    modifier = Modifier.weight(1f).height(46.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Coral, contentColor = Color.White)
                ) {
                    Text("پایان نوبت", fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(Modifier.weight(1f))
        Text("هر پاسخ درست یک امتیاز دارد · کلمات در طول بازی تا حد امکان تکرار نمی‌شوند", color = TextMuted, fontSize = 10.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    }
}

private fun drawNextWord(pool: List<WordEntry>, usedIds: Set<String>): Pair<WordEntry, Set<String>> {
    val remaining = pool.filterNot { it.id in usedIds }
    val next = (remaining.ifEmpty { pool }).random()
    val updatedUsed = if (remaining.isEmpty()) setOf(next.id) else usedIds + next.id
    return next to updatedUsed
}

private fun advanceTurn(session: GameSession, pool: List<WordEntry>, expired: Boolean): GameSession {
    val updatedTeams = session.teams.mapIndexed { index, team ->
        if (index == session.activeTeamIndex) team.copy(turnsTaken = team.turnsTaken + 1) else team
    }
    val isLastTeam = session.activeTeamIndex == session.teams.lastIndex
    val finished = isLastTeam && session.round >= session.config.rounds
    val nextWord = drawNextWord(pool, session.usedWordIds)
    return session.copy(
        teams = updatedTeams,
        round = if (isLastTeam && !finished) session.round + 1 else session.round,
        activeTeamIndex = if (isLastTeam) 0 else session.activeTeamIndex + 1,
        secondsRemaining = session.config.turnSeconds,
        currentWord = nextWord.first,
        usedWordIds = nextWord.second,
        isRunning = false,
        isFinished = finished,
        turnSerial = session.turnSerial + 1,
        lastTurnExpired = expired
    )
}

@Composable
private fun ResultsScreen(session: GameSession, onHome: () -> Unit, onReplay: () -> Unit) {
    val ranked = session.teams.sortedByDescending { it.score }
    val topScore = ranked.firstOrNull()?.score ?: 0
    val winners = ranked.filter { it.score == topScore }
    Column(Modifier.fillMaxSize().padding(22.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Spacer(Modifier.height(18.dp))
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Box(Modifier.size(82.dp).clip(CircleShape).background(Color(0xFF3D3724)), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.EmojiEvents, contentDescription = null, tint = Gold, modifier = Modifier.size(43.dp))
            }
        }
        Text(if (winners.size == 1) "${winners.first().name} برنده شد!" else "بازی مساوی شد!", color = TextMain, fontSize = 25.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Text("نتیجه نهایی پس از ${session.config.rounds.toPersianDigits()} دور", color = TextMuted, fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Column(verticalArrangement = Arrangement.spacedBy(9.dp), modifier = Modifier.weight(1f)) {
            ranked.forEachIndexed { index, team ->
                Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = if (team.score == topScore) Color(0xFF303D2A) else Surface1)) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text((index + 1).toString().toPersianDigits(), color = if (team.score == topScore) Lime else TextMuted, fontWeight = FontWeight.Black, fontSize = 20.sp)
                        Spacer(Modifier.width(13.dp))
                        Column(Modifier.weight(1f)) {
                            Text(team.name, color = TextMain, fontWeight = FontWeight.Bold)
                            Text(team.players.joinToString(" و "), color = TextMuted, fontSize = 11.sp)
                        }
                        Text("${team.score.toString().toPersianDigits()} امتیاز", color = if (team.score == topScore) Lime else TextMain, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
        Button(onClick = onReplay, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = Lime, contentColor = Bg)) {
            Icon(Icons.Filled.RestartAlt, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text("بازی دوباره", fontWeight = FontWeight.ExtraBold)
        }
        TextButton(onClick = onHome, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.Home, contentDescription = null, tint = TextMuted)
            Spacer(Modifier.width(5.dp))
            Text("بازگشت به خانه", color = TextMuted)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WordManagerScreen(
    library: WordLibrary,
    onLibraryChange: (WordLibrary) -> Unit,
    repository: WordRepository,
    onBack: () -> Unit
) {
    var selectedCategoryId by remember { mutableStateOf(library.categories.firstOrNull()?.id ?: 1) }
    var query by remember { mutableStateOf("") }
    var showAddWord by remember { mutableStateOf(false) }
    var showAddCategory by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    val filteredWords = remember(library.words, selectedCategoryId, query) {
        library.words.asSequence()
            .filter { it.categoryId == selectedCategoryId }
            .filter { query.isBlank() || it.text.contains(query.trim(), ignoreCase = true) }
            .sortedBy { it.text }
            .toList()
    }
    val selectedCategory = library.categories.firstOrNull { it.id == selectedCategoryId }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader(title = "بانک کلمات", onBack = onBack, trailing = {
            TextButton(onClick = { showAddCategory = true }) { Text("دسته جدید +", color = Cyan, fontSize = 12.sp) }
        })
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            library.categories.forEach { category ->
                ChoiceChip(
                    label = category.name,
                    selected = selectedCategoryId == category.id,
                    onClick = { selectedCategoryId = category.id }
                )
            }
        }
        Spacer(Modifier.height(11.dp))
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            placeholder = { Text("جست‌وجو در کلمات…", color = TextMuted) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = TextMuted) },
            singleLine = true,
            colors = fieldColors()
        )
        Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(selectedCategory?.name ?: "دسته", color = TextMain, fontWeight = FontWeight.Bold)
                Text("${filteredWords.size.toString().toPersianDigits()} کلمه", color = TextMuted, fontSize = 11.sp)
            }
            Button(onClick = { showAddWord = true }, shape = RoundedCornerShape(13.dp), colors = ButtonDefaults.buttonColors(containerColor = Lime, contentColor = Bg)) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Text("افزودن کلمه", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
        message?.let { msg ->
            Text(msg, color = Gold, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 18.dp, vertical = 2.dp))
        }
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 5.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            items(filteredWords, key = { it.id }) { word ->
                Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Surface1)) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 13.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(word.text, color = TextMain, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text(
                                text = "${listOf("آسان", "متوسط", "سخت").getOrElse(word.difficulty - 1) { "آسان" }}${if (word.isCustom) "  ·  اختصاصی" else "  ·  بانک اولیه"}",
                                color = if (word.isCustom) Cyan else TextMuted,
                                fontSize = 10.sp
                            )
                        }
                        if (word.isCustom) {
                            IconButton(onClick = { onLibraryChange(repository.removeCustomWord(library, word.id)) }, modifier = Modifier.size(34.dp)) {
                                Icon(Icons.Filled.Delete, contentDescription = "حذف کلمه", tint = Coral, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddWord) {
        AddWordDialog(
            categoryName = selectedCategory?.name ?: "دسته",
            onDismiss = { showAddWord = false },
            onAdd = { text, difficulty ->
                val updated = repository.addWord(library, selectedCategoryId, text, difficulty)
                if (updated == null) {
                    message = if (text.isBlank()) "متن کلمه را وارد کن." else "این کلمه از قبل در این دسته وجود دارد."
                    false
                } else {
                    onLibraryChange(updated)
                    message = "کلمه اضافه شد."
                    true
                }
            }
        )
    }
    if (showAddCategory) {
        AddCategoryDialog(
            onDismiss = { showAddCategory = false },
            onAdd = { name ->
                val updated = repository.addCategory(library, name)
                if (updated == null) {
                    message = "نام دسته خالی است یا قبلاً وجود دارد."
                    false
                } else {
                    onLibraryChange(updated)
                    selectedCategoryId = updated.categories.last().id
                    message = "دسته جدید ساخته شد. حالا کلماتش را اضافه کن."
                    true
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddWordDialog(categoryName: String, onDismiss: () -> Unit, onAdd: (String, Int) -> Boolean) {
    var text by remember { mutableStateOf("") }
    var difficulty by remember { mutableStateOf(1) }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Surface2,
        title = { Text("افزودن کلمه", color = TextMain, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("دسته: $categoryName", color = TextMuted, fontSize = 12.sp)
                OutlinedTextField(value = text, onValueChange = { text = it; error = null }, modifier = Modifier.fillMaxWidth(), label = { Text("کلمه یا عبارت") }, singleLine = true, colors = fieldColors())
                Text("درجه سختی", color = TextMuted, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    listOf(1 to "آسان", 2 to "متوسط", 3 to "سخت").forEach { (value, label) ->
                        ChoiceChip(label, difficulty == value, modifier = Modifier.weight(1f)) { difficulty = value }
                    }
                }
                error?.let { Text(it, color = Coral, fontSize = 11.sp) }
            }
        },
        confirmButton = {
            Button(onClick = { if (onAdd(text, difficulty)) onDismiss() else error = "این کلمه قابل افزودن نیست." }, colors = ButtonDefaults.buttonColors(containerColor = Lime, contentColor = Bg)) { Text("ذخیره") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف", color = TextMuted) } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddCategoryDialog(onDismiss: () -> Unit, onAdd: (String) -> Boolean) {
    var name by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Surface2,
        title = { Text("دسته‌بندی جدید", color = TextMain, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it; error = null }, modifier = Modifier.fillMaxWidth(), label = { Text("نام دسته") }, singleLine = true, colors = fieldColors())
                error?.let { Text(it, color = Coral, fontSize = 11.sp) }
            }
        },
        confirmButton = { Button(onClick = { if (onAdd(name)) onDismiss() else error = "نام خالی یا تکراری است." }, colors = ButtonDefaults.buttonColors(containerColor = Lime, contentColor = Bg)) { Text("ساخت دسته") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف", color = TextMuted) } }
    )
}

@Composable
private fun ScreenHeader(title: String, onBack: () -> Unit, trailing: @Composable (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "بازگشت", tint = TextMain) }
        Text(title, color = TextMain, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
        trailing?.invoke()
    }
}

@Composable
private fun SectionTitle(title: String, subtitle: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(title, color = TextMain, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Text(subtitle, color = TextMuted, fontSize = 11.sp)
    }
}

@Composable
private fun ChoiceChip(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        modifier = modifier,
        onClick = onClick,
        shape = RoundedCornerShape(13.dp),
        color = if (selected) Lime.copy(alpha = 0.14f) else Surface1,
        border = BorderStroke(1.dp, if (selected) Lime else Line)
    ) {
        Text(
            label,
            color = if (selected) Lime else TextMuted,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = TextMain,
    unfocusedTextColor = TextMain,
    focusedBorderColor = Lime,
    unfocusedBorderColor = Line,
    focusedLabelColor = Lime,
    unfocusedLabelColor = TextMuted,
    cursorColor = Lime,
    focusedContainerColor = Surface2,
    unfocusedContainerColor = Surface2
)

private fun List<String>.updated(index: Int, value: String): List<String> {
    val mutable = toMutableList()
    while (mutable.size <= index) mutable += ""
    mutable[index] = value
    return mutable
}

private fun Int.toPersianDigits(): String = toString().toPersianDigits()
private fun String.toPersianDigits(): String = map { char ->
    when (char) {
        '0' -> '۰'; '1' -> '۱'; '2' -> '۲'; '3' -> '۳'; '4' -> '۴'
        '5' -> '۵'; '6' -> '۶'; '7' -> '۷'; '8' -> '۸'; '9' -> '۹'
        else -> char
    }
}.joinToString("")
