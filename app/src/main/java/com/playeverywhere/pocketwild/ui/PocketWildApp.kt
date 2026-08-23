package com.playeverywhere.pocketwild.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.playeverywhere.pocketwild.game.CareAction
import com.playeverywhere.pocketwild.audio.PetVoice
import com.playeverywhere.pocketwild.audio.VoiceCue
import com.playeverywhere.pocketwild.game.GameEngine
import com.playeverywhere.pocketwild.game.GameState
import com.playeverywhere.pocketwild.game.GameViewModel
import com.playeverywhere.pocketwild.game.Habitat
import com.playeverywhere.pocketwild.game.PetStats
import com.playeverywhere.pocketwild.game.Species
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.sin

private enum class AppTab(val title: String, val emoji: String) {
    HOME("Дом", "🏠"),
    EXPLORE("Мир", "🗺️"),
    JOURNAL("Дневник", "📖")
}

private data class ActionCue(val action: CareAction, val id: Int)

@Composable
fun PocketWildApp(viewModel: GameViewModel) {
    val state by viewModel.state
    if (!state.hasPet) {
        WelcomeScreen(onStart = viewModel::createPet)
    } else {
        GameScreen(
            state = state,
            onAction = viewModel::act,
            onVisit = viewModel::visit
        )
    }
}

@Composable
private fun WelcomeScreen(onStart: (String, Species) -> Unit) {
    var selected by remember { mutableStateOf(Species.FOX) }
    var name by remember { mutableStateOf("Люми") }

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF253239), Color(0xFF376E63), Color(0xFFF4B47D))))
            .statusBarsPadding()
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("POCKETWILD", color = Color(0xFFFFE2A9), fontWeight = FontWeight.Black, letterSpacing = 3.sp)
            Text(
                "Маленький друг.\nБольшой живой мир.",
                color = Color.White,
                fontSize = 32.sp,
                lineHeight = 37.sp,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 12.dp, bottom = 18.dp)
            )
            PetCharacter(species = selected, mood = "счастлив", modifier = Modifier.size(200.dp))
            Text("Выбери своего первого спутника", color = Color.White, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Species.entries.forEach { species ->
                    SpeciesCard(
                        species = species,
                        selected = selected == species,
                        modifier = Modifier.weight(1f),
                        onClick = { selected = species }
                    )
                }
            }
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(14) },
                label = { Text("Имя питомца") },
                singleLine = true,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 18.dp),
            )
            Button(
                onClick = { onStart(name, selected) },
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFC66D), contentColor = Color(0xFF362812)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp)
                    .height(56.dp)
            ) {
                Text("Начать дружбу", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            Text("Игра работает без интернета", color = Color.White.copy(alpha = .72f), fontSize = 12.sp, modifier = Modifier.padding(12.dp))
        }
    }
}

@Composable
private fun SpeciesCard(species: Species, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        color = if (selected) Color(0xFFFFF3D8) else Color.White.copy(alpha = .16f),
        contentColor = if (selected) Color(0xFF253239) else Color.White,
        shape = RoundedCornerShape(18.dp),
        modifier = modifier
            .border(if (selected) 2.dp else 1.dp, if (selected) Color(0xFFFFC66D) else Color.White.copy(alpha = .25f), RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
    ) {
        Column(Modifier.padding(vertical = 12.dp, horizontal = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(species.emoji, fontSize = 24.sp)
            Text(species.title, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun GameScreen(
    state: GameState,
    onAction: (CareAction) -> Unit,
    onVisit: (Habitat) -> Unit
) {
    var tab by remember { mutableStateOf(AppTab.HOME) }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = { BottomTabs(selected = tab, onSelect = { tab = it }) }
    ) { padding ->
        when (tab) {
            AppTab.HOME -> HomeScreen(state, onAction, Modifier.padding(padding))
            AppTab.EXPLORE -> ExploreScreen(state, onVisit, Modifier.padding(padding))
            AppTab.JOURNAL -> JournalScreen(state, Modifier.padding(padding))
        }
    }
}

@Composable
private fun HomeScreen(state: GameState, onAction: (CareAction) -> Unit, modifier: Modifier = Modifier) {
    var cue by remember { mutableStateOf<ActionCue?>(null) }
    var cueId by remember { mutableStateOf(0) }

    LaunchedEffect(cue) {
        val activeId = cue?.id ?: return@LaunchedEffect
        delay(1_450)
        if (cue?.id == activeId) cue = null
    }

    fun perform(action: CareAction) {
        cueId += 1
        cue = ActionCue(action, cueId)
        PetVoice.play(state.species, VoiceCue.from(action))
        onAction(action)
    }

    Box(modifier.fillMaxSize()) {
        HabitatBackground(state.habitat, Modifier.fillMaxSize())
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PlayerHeader(state)
            SpeechBubble(state.lastMessage, Modifier.padding(top = 10.dp))
            PetCharacter(
                species = state.species,
                mood = GameEngine.mood(state.stats),
                action = cue?.action,
                actionKey = cue?.id ?: 0,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clickable { PetVoice.play(state.species, VoiceCue.HELLO) }
            )
            StatsCard(state.stats)
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CareAction.entries.forEach { action ->
                    ActionButton(action, modifier = Modifier.weight(1f)) { perform(action) }
                }
            }
        }
    }
}

@Composable
private fun PlayerHeader(state: GameState) {
    Surface(color = Color.White.copy(alpha = .90f), shape = RoundedCornerShape(22.dp), shadowElevation = 2.dp) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(state.petName, fontSize = 21.sp, fontWeight = FontWeight.Black)
                Text("Уровень ${state.level} • ${GameEngine.mood(state.stats)}", fontSize = 12.sp, color = Color(0xFF60736C))
                LinearProgressIndicator(
                    progress = { state.levelProgress },
                    modifier = Modifier
                        .fillMaxWidth(.7f)
                        .padding(top = 5.dp)
                        .height(5.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = Color(0xFFDCE5DF),
                    strokeCap = StrokeCap.Round
                )
            }
            Surface(color = Color(0xFFFFE3A8), shape = CircleShape) {
                Text("🍓 ${state.coins}", modifier = Modifier.padding(horizontal = 11.dp, vertical = 8.dp), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun SpeechBubble(text: String, modifier: Modifier = Modifier) {
    Surface(modifier, color = Color.White.copy(alpha = .88f), shape = RoundedCornerShape(18.dp)) {
        Text(text, Modifier.padding(horizontal = 15.dp, vertical = 9.dp), fontSize = 13.sp, textAlign = TextAlign.Center)
    }
}

@Composable
private fun StatsCard(stats: PetStats) {
    Surface(color = Color.White.copy(alpha = .92f), shape = RoundedCornerShape(22.dp), shadowElevation = 2.dp) {
        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatPill("Сытость", "🍎", stats.hunger, Color(0xFFFF8B5E), Modifier.weight(1f))
            StatPill("Радость", "💛", stats.joy, Color(0xFFFFC857), Modifier.weight(1f))
            StatPill("Силы", "⚡", stats.energy, Color(0xFF63B995), Modifier.weight(1f))
            StatPill("Чистота", "💧", stats.hygiene, Color(0xFF71A9E2), Modifier.weight(1f))
        }
    }
}

@Composable
private fun StatPill(label: String, emoji: String, value: Int, color: Color, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(emoji, fontSize = 17.sp)
        LinearProgressIndicator(
            progress = { value / 100f },
            modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp).height(6.dp),
            color = color,
            trackColor = color.copy(alpha = .18f),
            strokeCap = StrokeCap.Round
        )
        Text(label, fontSize = 9.sp, color = Color(0xFF60736C), maxLines = 1)
    }
}

@Composable
private fun ActionButton(action: CareAction, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        color = Color(0xFFFFFBF4),
        shape = RoundedCornerShape(18.dp),
        shadowElevation = 3.dp,
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Column(Modifier.padding(vertical = 10.dp, horizontal = 3.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(action.emoji, fontSize = 22.sp)
            Text(action.title, fontSize = 10.sp, maxLines = 1, fontWeight = FontWeight.SemiBold)
            if (action == CareAction.FEED) Text("−5 🍓", fontSize = 8.sp, color = Color(0xFF7B6A5C))
        }
    }
}

@Composable
private fun BottomTabs(selected: AppTab, onSelect: (AppTab) -> Unit) {
    Surface(shadowElevation = 8.dp, color = Color(0xFFFFFBF4)) {
        Row(
            Modifier.fillMaxWidth().navigationBarsPadding().height(64.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            AppTab.entries.forEach { tab ->
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable { onSelect(tab) }
                        .padding(top = 7.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(tab.emoji, fontSize = 21.sp)
                    Text(
                        tab.title,
                        fontSize = 11.sp,
                        fontWeight = if (selected == tab) FontWeight.Bold else FontWeight.Normal,
                        color = if (selected == tab) MaterialTheme.colorScheme.primary else Color(0xFF748079)
                    )
                }
            }
        }
    }
}

@Composable
private fun ExploreScreen(state: GameState, onVisit: (Habitat) -> Unit, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Живой мир", fontSize = 30.sp, fontWeight = FontWeight.Black)
            Text("Исследуйте места вместе и находите истории.", color = Color(0xFF60736C), modifier = Modifier.padding(top = 3.dp, bottom = 8.dp))
        }
        items(Habitat.entries) { habitat ->
            val unlocked = state.level >= habitat.unlockLevel
            val current = state.habitat == habitat
            Card(
                colors = CardDefaults.cardColors(containerColor = if (current) Color(0xFFD9EEE6) else Color(0xFFFFFBF4)),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onVisit(habitat) }
            ) {
                Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(color = habitatColor(habitat), shape = RoundedCornerShape(20.dp)) {
                        Text(habitat.emoji, fontSize = 35.sp, modifier = Modifier.padding(15.dp))
                    }
                    Column(Modifier.weight(1f).padding(start = 15.dp)) {
                        Text(habitat.title, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text(habitat.subtitle, fontSize = 13.sp, color = Color(0xFF60736C))
                        Text(
                            when {
                                current -> "Вы здесь"
                                unlocked -> "+2 опыта за визит"
                                else -> "🔒 Нужен уровень ${habitat.unlockLevel}"
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (unlocked) MaterialTheme.colorScheme.primary else Color(0xFF9A7661),
                            modifier = Modifier.padding(top = 7.dp)
                        )
                    }
                    Text(if (unlocked) "›" else "", fontSize = 28.sp, color = Color(0xFF60736C))
                }
            }
        }
        item {
            Surface(color = Color(0xFF283A42), contentColor = Color.White, shape = RoundedCornerShape(24.dp)) {
                Column(Modifier.padding(18.dp)) {
                    Text("Скоро", color = Color(0xFFFFD084), fontWeight = FontWeight.Bold)
                    Text("Мини-игры, коллекция находок и взросление питомца", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 5.dp))
                }
            }
        }
    }
}

@Composable
private fun JournalScreen(state: GameState, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("Дневник дружбы", fontSize = 30.sp, fontWeight = FontWeight.Black)
            Text("Сегодня с ${state.petName}", color = Color(0xFF60736C))
        }
        item {
            Surface(color = Color(0xFF2F6B5F), contentColor = Color.White, shape = RoundedCornerShape(25.dp)) {
                Column(Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Ежедневное приключение", fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Text(if (state.claimedQuest) "✓ +30 🍓" else "+30 🍓", color = Color(0xFFFFD084), fontWeight = FontWeight.Bold)
                    }
                    Text("Позаботься о друге и получи награду", fontSize = 12.sp, color = Color.White.copy(alpha = .75f))
                    QuestRow("Покормить дважды", state.feedCount.coerceAtMost(2), 2)
                    QuestRow("Поиграть", state.playCount.coerceAtMost(1), 1)
                    QuestRow("Умыть", state.cleanCount.coerceAtMost(1), 1)
                    LinearProgressIndicator(
                        progress = { state.questProgress / 4f },
                        color = Color(0xFFFFD084),
                        trackColor = Color.White.copy(alpha = .16f),
                        strokeCap = StrokeCap.Round,
                        modifier = Modifier.fillMaxWidth().padding(top = 14.dp).height(7.dp)
                    )
                }
            }
        }
        item { SectionTitle("Портрет питомца") }
        item {
            Surface(color = Color(0xFFFFFBF4), shape = RoundedCornerShape(24.dp)) {
                Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(84.dp).background(Color(state.species.accent).copy(alpha = .20f), CircleShape)) {
                        PetCharacter(state.species, GameEngine.mood(state.stats), Modifier.fillMaxSize())
                    }
                    Column(Modifier.padding(start = 16.dp)) {
                        Text(state.petName, fontSize = 23.sp, fontWeight = FontWeight.Black)
                        Text("${state.species.title} • ${GameEngine.mood(state.stats)}", color = Color(0xFF60736C))
                        Text("${state.xp} опыта • ${state.coins} ягод-монет", fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
                    }
                }
            }
        }
        item { SectionTitle("Как всё устроено") }
        item {
            InfoCard("🌱", "Растёт вместе с вами", "Уход и исследования дают опыт. Новые уровни открывают места и будущие формы питомца.")
        }
        item {
            InfoCard("🛡️", "Без наказаний", "Питомец никогда не исчезнет. После долгого перерыва он просто соскучится и будет ждать вас.")
        }
    }
}

@Composable
private fun QuestRow(title: String, progress: Int, target: Int) {
    Row(Modifier.fillMaxWidth().padding(top = 13.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(if (progress >= target) "✅" else "○", fontSize = 18.sp)
        Text(title, Modifier.weight(1f).padding(start = 9.dp), fontSize = 14.sp)
        Text("$progress/$target", fontSize = 12.sp, color = Color.White.copy(alpha = .75f))
    }
}

@Composable
private fun SectionTitle(text: String) = Text(text, fontSize = 18.sp, fontWeight = FontWeight.Bold)

@Composable
private fun InfoCard(emoji: String, title: String, body: String) {
    Surface(color = Color(0xFFFFFBF4), shape = RoundedCornerShape(22.dp)) {
        Row(Modifier.padding(17.dp)) {
            Text(emoji, fontSize = 29.sp)
            Column(Modifier.padding(start = 13.dp)) {
                Text(title, fontWeight = FontWeight.Bold)
                Text(body, fontSize = 13.sp, lineHeight = 18.sp, color = Color(0xFF60736C), modifier = Modifier.padding(top = 3.dp))
            }
        }
    }
}

@Composable
private fun HabitatBackground(habitat: Habitat, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val colors = when (habitat) {
            Habitat.HOME -> listOf(Color(0xFFF6D7A7), Color(0xFFE8B882), Color(0xFF77A37A))
            Habitat.GARDEN -> listOf(Color(0xFF9ED7E3), Color(0xFFDFF2C2), Color(0xFF6EAE6C))
            Habitat.FOREST -> listOf(Color(0xFF152F39), Color(0xFF27544D), Color(0xFF446D55))
            Habitat.SHORE -> listOf(Color(0xFF789ED2), Color(0xFFE4BFD0), Color(0xFF73A8B2))
        }
        drawRect(Brush.verticalGradient(colors))
        val horizon = size.height * .62f
        drawCircle(Color.White.copy(alpha = .45f), radius = size.minDimension * .09f, center = Offset(size.width * .78f, size.height * .24f))
        when (habitat) {
            Habitat.HOME -> {
                drawRoundRect(Color(0xFFB86F52), Offset(size.width * .08f, horizon - 110f), Size(size.width * .35f, 180f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(20f))
                val roof = Path().apply { moveTo(size.width * .04f, horizon - 110f); lineTo(size.width * .25f, horizon - 230f); lineTo(size.width * .47f, horizon - 110f); close() }
                drawPath(roof, Color(0xFF754B45))
            }
            Habitat.GARDEN -> repeat(8) { index ->
                val x = size.width * (index + .5f) / 8f
                drawLine(Color(0xFF397555), Offset(x, horizon), Offset(x + 8f, horizon - 40f - index % 3 * 12f), 5f)
                drawCircle(Color(if (index % 2 == 0) 0xFFFFC857 else 0xFFFF8B8B), 10f, Offset(x + 8f, horizon - 47f - index % 3 * 12f))
            }
            Habitat.FOREST -> repeat(6) { index ->
                val x = size.width * (index + .2f) / 5f
                drawLine(Color(0xFF17382F), Offset(x, horizon + 100f), Offset(x + 20f, horizon - 260f), 35f)
                drawCircle(Color(0xFF3D775A), 85f, Offset(x + 18f, horizon - 260f))
                drawCircle(Color(0xFFFFE990).copy(alpha = .75f), 4f, Offset(x + 45f, horizon - 140f - index * 11f))
            }
            Habitat.SHORE -> {
                drawRect(Color(0xFF6EA8B2), Offset(0f, horizon), Size(size.width, size.height - horizon))
                repeat(4) { index -> drawArc(Color.White.copy(alpha = .45f), 180f, 170f, false, Offset(index * size.width / 3f - 60f, horizon - 25f), Size(150f, 45f), style = Stroke(5f)) }
            }
        }
        drawRect(colors.last(), Offset(0f, horizon), Size(size.width, size.height - horizon))
    }
}

@Composable
private fun PetCharacter(
    species: Species,
    mood: String,
    modifier: Modifier = Modifier,
    action: CareAction? = null,
    actionKey: Int = 0
) {
    val transition = rememberInfiniteTransition(label = "pet-life")
    val bob by transition.animateFloat(
        initialValue = -5f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(tween(1_600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pet-breathe"
    )
    val idleWave by transition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(720, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "tail-and-wings"
    )
    val blink by transition.animateFloat(
        initialValue = 0f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            keyframes {
                durationMillis = 4_200
                0f at 0
                0f at 3_100
                1f at 3_170
                0f at 3_270
                0f at 4_200
            }
        ),
        label = "blink"
    )
    val actionProgress = remember { Animatable(1f) }
    LaunchedEffect(actionKey) {
        if (action != null) {
            actionProgress.snapTo(0f)
            actionProgress.animateTo(1f, tween(1_320, easing = FastOutSlowInEasing))
        }
    }

    Canvas(modifier) {
        val s = minOf(size.width, size.height) / 260f
        val p = actionProgress.value
        val jump = when (action) {
            CareAction.PLAY -> -sin(PI * p).toFloat() * 48f
            CareAction.FEED -> -sin(PI * p).toFloat() * 13f
            CareAction.CLEAN -> -sin(PI * p * 2).toFloat().coerceAtLeast(0f) * 8f
            CareAction.REST -> p * 10f
            null -> 0f
        }
        val tilt = when (action) {
            CareAction.PLAY -> sin(PI * p * 3).toFloat() * 13f
            CareAction.CLEAN -> sin(PI * p * 8).toFloat() * 6f
            CareAction.FEED -> sin(PI * p * 4).toFloat() * 3f
            else -> 0f
        }
        val motion = when (action) {
            CareAction.PLAY -> sin(PI * p * 5).toFloat()
            CareAction.CLEAN -> sin(PI * p * 8).toFloat()
            else -> idleWave
        }
        val ground = Offset(size.width / 2f, size.height / 2f + bob * s)
        val center = ground + Offset(0f, jump * s)
        val eyesClosed = mood == "сонный" || action == CareAction.REST || blink > .55f
        val mouthOpen = action == CareAction.FEED && p in .24f..82f

        drawOval(
            Color.Black.copy(alpha = .13f - (-jump / 48f).coerceIn(0f, 1f) * .06f),
            Offset(ground.x - 75f * s, ground.y + 91f * s),
            Size(150f * s, 25f * s)
        )
        drawActionEffects(action, p, center, s, behind = true)
        rotate(tilt, pivot = center) {
            when (species) {
                Species.FOX -> drawFox(center, s, mood, eyesClosed, mouthOpen, motion)
                Species.AXOLOTL -> drawAxolotl(center, s, mood, eyesClosed, mouthOpen, motion)
                Species.OWL -> drawOwl(center, s, mood, eyesClosed, mouthOpen, motion)
            }
        }
        drawActionEffects(action, p, center, s, behind = false)
    }
}

private fun DrawScope.drawFox(c: Offset, s: Float, mood: String, eyesClosed: Boolean, mouthOpen: Boolean, motion: Float) {
    val orange = Color(0xFFFF8B5E)
    val cheek = Color(0xFFFFE3C2)
    drawArc(orange, 180f + motion * 12f, 250f, false, Offset(c.x + 30*s, c.y + (20 + motion*3)*s), Size(100*s, 90*s), style = Stroke(22*s, cap = StrokeCap.Round))
    drawArc(cheek, 295f + motion * 12f, 70f, false, Offset(c.x + 30*s, c.y + (20 + motion*3)*s), Size(100*s, 90*s), style = Stroke(22*s, cap = StrokeCap.Round))
    drawOval(orange, Offset(c.x - 62*s, c.y + 22*s), Size(124*s, 85*s))
    val earWiggle = motion * 4f
    val leftEar = Path().apply { moveTo(c.x - 67*s, c.y - 42*s); lineTo(c.x + (-48 + earWiggle)*s, c.y - 102*s); lineTo(c.x - 16*s, c.y - 55*s); close() }
    val rightEar = Path().apply { moveTo(c.x + 67*s, c.y - 42*s); lineTo(c.x + (48 + earWiggle)*s, c.y - 102*s); lineTo(c.x + 16*s, c.y - 55*s); close() }
    drawPath(leftEar, orange); drawPath(rightEar, orange)
    drawCircle(orange, 68*s, Offset(c.x, c.y - 20*s))
    drawCircle(cheek, 32*s, Offset(c.x - 25*s, c.y - 2*s)); drawCircle(cheek, 32*s, Offset(c.x + 25*s, c.y - 2*s))
    drawOval(cheek, Offset(c.x - 35*s, c.y - 18*s), Size(70*s, 57*s))
    drawFace(c, s, mood, eyeY = -28f, eyesClosed = eyesClosed, mouthOpen = mouthOpen)
    drawCircle(Color(0xFF3B2B28), 7*s, Offset(c.x, c.y + 10*s))
}

private fun DrawScope.drawAxolotl(c: Offset, s: Float, mood: String, eyesClosed: Boolean, mouthOpen: Boolean, motion: Float) {
    val pink = Color(0xFFFF91B8)
    val dark = Color(0xFFD75D8C)
    drawOval(pink, Offset(c.x - 58*s, c.y + 14*s), Size(116*s, 96*s))
    repeat(3) { i ->
        val dy = (-52 + i * 25) * s
        val wave = motion * (7 - i) * s
        drawLine(dark, Offset(c.x - 52*s, c.y + dy), Offset(c.x - 92*s - wave, c.y + dy - 17*s + wave), 9*s, StrokeCap.Round)
        drawLine(dark, Offset(c.x + 52*s, c.y + dy), Offset(c.x + 92*s + wave, c.y + dy - 17*s - wave), 9*s, StrokeCap.Round)
    }
    drawRoundRect(pink, Offset(c.x - 68*s, c.y - 70*s), Size(136*s, 105*s), androidx.compose.ui.geometry.CornerRadius(55*s))
    drawFace(c, s, mood, eyeY = -29f, eyesClosed = eyesClosed, mouthOpen = mouthOpen)
    drawCircle(Color(0xFFFFCAE0), 10*s, Offset(c.x - 44*s, c.y)); drawCircle(Color(0xFFFFCAE0), 10*s, Offset(c.x + 44*s, c.y))
    drawArc(dark, 25f + motion * 8f, 260f, false, Offset(c.x + 35*s, c.y + 45*s), Size(85*s, 45*s), style = Stroke(12*s, cap = StrokeCap.Round))
}

private fun DrawScope.drawOwl(c: Offset, s: Float, mood: String, eyesClosed: Boolean, mouthOpen: Boolean, motion: Float) {
    val purple = Color(0xFF9B8AE6)
    val cream = Color(0xFFFFE7B0)
    val wingLift = kotlin.math.abs(motion) * 22f
    drawOval(purple, Offset(c.x - 66*s, c.y - 65*s), Size(132*s, 170*s))
    drawOval(Color(0xFF7A67C2), Offset(c.x - 83*s, c.y + (-10 - wingLift)*s), Size(55*s, 105*s))
    drawOval(Color(0xFF7A67C2), Offset(c.x + 28*s, c.y + (-10 - wingLift)*s), Size(55*s, 105*s))
    drawCircle(cream, 39*s, Offset(c.x - 31*s, c.y - 33*s)); drawCircle(cream, 39*s, Offset(c.x + 31*s, c.y - 33*s))
    drawFace(c, s, mood, eyeY = -35f, eyeSpread = 31f, eyesClosed = eyesClosed, mouthOpen = mouthOpen)
    val beakDrop = if (mouthOpen) 8f else 0f
    val beak = Path().apply { moveTo(c.x - 9*s, c.y - 8*s); lineTo(c.x + 9*s, c.y - 8*s); lineTo(c.x, c.y + (10 + beakDrop)*s); close() }
    drawPath(beak, Color(0xFFFFB457))
    repeat(3) { i -> drawArc(cream.copy(alpha = .8f), 200f, 140f, false, Offset(c.x - (38 - i*18)*s, c.y + (28 + i%2*13)*s), Size(40*s, 26*s), style = Stroke(4*s)) }
}

private fun DrawScope.drawFace(
    c: Offset,
    s: Float,
    mood: String,
    eyeY: Float,
    eyeSpread: Float = 24f,
    eyesClosed: Boolean,
    mouthOpen: Boolean
) {
    val dark = Color(0xFF302A31)
    if (eyesClosed) {
        drawArc(dark, 15f, 150f, false, Offset(c.x - (eyeSpread+10)*s, c.y + (eyeY-5)*s), Size(20*s, 12*s), style = Stroke(4*s, cap = StrokeCap.Round))
        drawArc(dark, 15f, 150f, false, Offset(c.x + (eyeSpread-10)*s, c.y + (eyeY-5)*s), Size(20*s, 12*s), style = Stroke(4*s, cap = StrokeCap.Round))
    } else {
        drawCircle(dark, 6*s, Offset(c.x - eyeSpread*s, c.y + eyeY*s))
        drawCircle(dark, 6*s, Offset(c.x + eyeSpread*s, c.y + eyeY*s))
        drawCircle(Color.White, 2*s, Offset(c.x - (eyeSpread+2)*s, c.y + (eyeY-2)*s))
        drawCircle(Color.White, 2*s, Offset(c.x + (eyeSpread-2)*s, c.y + (eyeY-2)*s))
    }
    if (mouthOpen) {
        drawOval(dark, Offset(c.x - 11*s, c.y + 19*s), Size(22*s, 22*s))
        drawArc(Color(0xFFFF8FA3), 180f, 180f, true, Offset(c.x - 7*s, c.y + 29*s), Size(14*s, 8*s))
    } else {
        val smileSweep = if (mood == "скучает" || mood == "голодный") -140f else 140f
        drawArc(dark, 20f, smileSweep, false, Offset(c.x - 13*s, c.y - 1*s), Size(26*s, 18*s), style = Stroke(3*s, cap = StrokeCap.Round))
    }
}

private fun DrawScope.drawActionEffects(action: CareAction?, p: Float, c: Offset, s: Float, behind: Boolean) {
    when (action) {
        CareAction.FEED -> if (!behind) {
            val travel = (p / .72f).coerceIn(0f, 1f)
            if (p < .8f) {
                val berry = Offset(
                    c.x + (125f * (1f - travel)) * s,
                    c.y - (18f + sin(PI * travel).toFloat() * 82f) * s
                )
                drawCircle(Color(0xFFE94C64), 13*s, berry)
                drawCircle(Color(0xFFFF7D8E), 3*s, berry + Offset(-4*s, -4*s))
                drawLine(Color(0xFF3C895B), berry + Offset(0f, -11*s), berry + Offset(7*s, -22*s), 4*s, StrokeCap.Round)
            } else {
                repeat(3) { i -> drawHeart(c + Offset((-42 + i*42)*s, (-105 - i%2*18)*s), 8*s, (1f-p).coerceIn(0f, .2f)*5f) }
            }
        }
        CareAction.PLAY -> if (!behind) {
            val ball = Offset(c.x + (115f - p*230f)*s, c.y + (60f - kotlin.math.abs(sin(PI*p*2)).toFloat()*100f)*s)
            drawCircle(Color(0xFFFFC857), 18*s, ball)
            drawArc(Color(0xFFFF8B5E), 25f, 150f, false, ball - Offset(13*s, 13*s), Size(26*s, 26*s), style = Stroke(5*s))
            repeat(4) { i -> drawSpark(c + Offset((-105 + i*70)*s, (-95 + i%2*35)*s), 7*s, Color(0xFFFFE38E)) }
        }
        CareAction.CLEAN -> {
            if (behind) repeat(8) { i ->
                val local = (p + i*.14f) % 1f
                val x = c.x + (-92 + (i*31)%185)*s
                val y = c.y + (95 - local*225)*s
                drawCircle(Color(0xFFBEEBFF).copy(alpha = .35f + (1f-local)*.45f), (7 + i%3*4)*s, Offset(x,y), style = Stroke(3*s))
            }
        }
        CareAction.REST -> if (!behind) {
            repeat(3) { i ->
                val alpha = (.3f + p*.7f - i*.12f).coerceIn(0f, 1f)
                drawZ(c + Offset((62+i*25)*s, (-70-i*35)*s), (11+i*2)*s, Color.White.copy(alpha = alpha))
            }
        }
        null -> Unit
    }
}

private fun DrawScope.drawSpark(c: Offset, r: Float, color: Color) {
    drawLine(color, c - Offset(r, 0f), c + Offset(r, 0f), 3f, StrokeCap.Round)
    drawLine(color, c - Offset(0f, r), c + Offset(0f, r), 3f, StrokeCap.Round)
}

private fun DrawScope.drawZ(c: Offset, r: Float, color: Color) {
    drawLine(color, c - Offset(r, r*.7f), c + Offset(r, -r*.7f), 4f, StrokeCap.Round)
    drawLine(color, c + Offset(r, -r*.7f), c - Offset(r, r*.7f), 4f, StrokeCap.Round)
    drawLine(color, c - Offset(r, r*.7f), c + Offset(r, r*.7f), 4f, StrokeCap.Round)
}

private fun DrawScope.drawHeart(c: Offset, r: Float, alpha: Float) {
    val color = Color(0xFFFF6F91).copy(alpha = alpha.coerceIn(0f,1f))
    drawCircle(color, r*.58f, c - Offset(r*.45f, r*.25f))
    drawCircle(color, r*.58f, c + Offset(r*.45f, -r*.25f))
    val bottom = Path().apply {
        moveTo(c.x-r, c.y)
        lineTo(c.x+r, c.y)
        lineTo(c.x, c.y+r*1.5f)
        close()
    }
    drawPath(bottom, color)
}

private fun habitatColor(habitat: Habitat): Color = when (habitat) {
    Habitat.HOME -> Color(0xFFFFD5A5)
    Habitat.GARDEN -> Color(0xFFD8EDB5)
    Habitat.FOREST -> Color(0xFFBBDAC9)
    Habitat.SHORE -> Color(0xFFB9DCE4)
}
