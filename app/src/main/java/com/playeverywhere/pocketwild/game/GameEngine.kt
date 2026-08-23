package com.playeverywhere.pocketwild.game

import java.util.Calendar
import kotlin.math.floor

object GameEngine {
    private const val MAX_AWAY_HOURS = 24.0
    private const val QUEST_REWARD = 30

    fun refresh(state: GameState, nowMillis: Long = System.currentTimeMillis()): GameState {
        val currentDay = epochDay(nowMillis)
        val daily = if (state.questDay != currentDay) {
            state.copy(
                feedCount = 0,
                playCount = 0,
                cleanCount = 0,
                questDay = currentDay,
                claimedQuest = false
            )
        } else state

        val awayHours = ((nowMillis - daily.lastUpdated).coerceAtLeast(0L) / 3_600_000.0)
            .coerceAtMost(MAX_AWAY_HOURS)
        if (awayHours < 0.25) return daily.copy(lastUpdated = nowMillis)

        return daily.copy(
            stats = daily.stats.copy(
                hunger = lower(daily.stats.hunger, floor(awayHours * 2.2).toInt()),
                joy = lower(daily.stats.joy, floor(awayHours * 0.9).toInt()),
                energy = lower(daily.stats.energy, floor(awayHours * 1.4).toInt()),
                hygiene = lower(daily.stats.hygiene, floor(awayHours * 1.1).toInt())
            ),
            lastUpdated = nowMillis,
            lastMessage = if (awayHours >= 4) "Я скучал. Давай проведём время вместе!" else daily.lastMessage
        )
    }

    fun createPet(name: String, species: Species, nowMillis: Long = System.currentTimeMillis()): GameState =
        GameState(
            hasPet = true,
            petName = name.trim().ifBlank { "Люми" }.take(14),
            species = species,
            questDay = epochDay(nowMillis),
            lastUpdated = nowMillis,
            lastMessage = "Привет! Теперь мы одна команда."
        )

    fun act(state: GameState, action: CareAction, nowMillis: Long = System.currentTimeMillis()): ActionResult {
        val fresh = refresh(state, nowMillis)
        val s = fresh.stats
        val result = when (action) {
            CareAction.FEED -> {
                if (fresh.coins < 5) return ActionResult(fresh, "Нужно ещё 5 ягод-монет")
                fresh.copy(
                    stats = s.copy(hunger = raise(s.hunger, 24), energy = raise(s.energy, 3)),
                    coins = fresh.coins - 5,
                    xp = fresh.xp + 5,
                    feedCount = fresh.feedCount + 1
                ) to "М-м-м! Самая вкусная ягода!"
            }
            CareAction.PLAY -> {
                if (s.energy < 12) return ActionResult(fresh, "Я немного устал. Сначала отдохнём?")
                fresh.copy(
                    stats = s.copy(joy = raise(s.joy, 26), energy = lower(s.energy, 12), hygiene = lower(s.hygiene, 4)),
                    coins = fresh.coins + 3,
                    xp = fresh.xp + 8,
                    playCount = fresh.playCount + 1
                ) to "Вот это приключение! Ещё раз?"
            }
            CareAction.CLEAN -> fresh.copy(
                stats = s.copy(hygiene = raise(s.hygiene, 38), joy = lower(s.joy, 2)),
                xp = fresh.xp + 4,
                cleanCount = fresh.cleanCount + 1
            ) to "Чистота! Теперь я сияю."
            CareAction.REST -> fresh.copy(
                stats = s.copy(energy = raise(s.energy, 34), hunger = lower(s.hunger, 6)),
                xp = fresh.xp + 3
            ) to "Мне приснился светящийся лес..."
        }

        val updated = rewardQuestIfNeeded(result.first)
        return ActionResult(updated.copy(lastMessage = result.second), result.second)
    }

    fun visit(state: GameState, habitat: Habitat): ActionResult {
        if (state.level < habitat.unlockLevel) {
            return ActionResult(state, "Откроется на ${habitat.unlockLevel} уровне")
        }
        val message = when (habitat) {
            Habitat.HOME -> "Дома так уютно."
            Habitat.GARDEN -> "Смотри, бабочка! Пойдём за ней?"
            Habitat.FOREST -> "Здесь деревья шепчут истории."
            Habitat.SHORE -> "Я нашёл ракушку с шумом моря!"
        }
        return ActionResult(
            state.copy(habitat = habitat, xp = state.xp + 2, lastMessage = message),
            message
        )
    }

    fun mood(stats: PetStats): String = when {
        stats.energy < 25 -> "сонный"
        stats.hunger < 25 -> "голодный"
        stats.hygiene < 25 -> "чумазый"
        stats.joy < 25 -> "скучает"
        stats.wellbeing >= 82 -> "счастлив"
        else -> "спокоен"
    }

    private fun rewardQuestIfNeeded(state: GameState): GameState =
        if (state.questComplete && !state.claimedQuest) {
            state.copy(coins = state.coins + QUEST_REWARD, xp = state.xp + 15, claimedQuest = true)
        } else state

    private fun raise(value: Int, amount: Int) = (value + amount).coerceIn(0, 100)
    private fun lower(value: Int, amount: Int) = (value - amount).coerceIn(0, 100)

    private fun epochDay(millis: Long): Long = Calendar.getInstance().run {
        timeInMillis = millis
        get(Calendar.YEAR).toLong() * 512L + get(Calendar.DAY_OF_YEAR)
    }
}
