package com.playeverywhere.pocketwild.game

import java.util.Calendar
import kotlin.math.floor

object GameEngine {
    private const val MAX_AWAY_HOURS = 24.0
    private const val QUEST_REWARD = 30
    private const val EMOTION_DURATION = 12 * 60_000L
    private const val PETTING_REWARD_COOLDOWN = 10_000L

    fun refresh(state: GameState, nowMillis: Long = System.currentTimeMillis()): GameState {
        val currentDay = epochDay(nowMillis)
        val daily = if (state.questDay != currentDay) {
            state.copy(feedCount = 0, playCount = 0, cleanCount = 0, questDay = currentDay, claimedQuest = false)
        } else state
        val seeded = seedPersonality(daily)
        val awayHours = ((nowMillis - seeded.lastUpdated).coerceAtLeast(0L) / 3_600_000.0).coerceAtMost(MAX_AWAY_HOURS)
        val refreshed = if (awayHours < 0.25) {
            seeded.copy(lastUpdated = nowMillis)
        } else {
            seeded.copy(
                stats = seeded.stats.copy(
                    hunger = lower(seeded.stats.hunger, floor(awayHours * 2.2).toInt()),
                    joy = lower(seeded.stats.joy, floor(awayHours * 0.9).toInt()),
                    energy = lower(seeded.stats.energy, floor(awayHours * 1.4).toInt()),
                    hygiene = lower(seeded.stats.hygiene, floor(awayHours * 1.1).toInt())
                ),
                lastUpdated = nowMillis,
                lastMessage = if (awayHours >= 4) "Я скучал. Побудешь немного со мной?" else seeded.lastMessage
            )
        }
        val resolvedEmotion = if (awayHours >= 4 && refreshed.stats.joy < 65) PetEmotion.LONELY else emotion(refreshed, nowMillis)
        return refreshed.copy(
            emotion = resolvedEmotion,
            emotionUntil = if (resolvedEmotion == PetEmotion.LONELY) nowMillis + 5 * 60_000L else refreshed.emotionUntil
        )
    }

    fun createPet(name: String, species: Species, nowMillis: Long = System.currentTimeMillis()): GameState {
        val traits = startingTraits(species)
        return GameState(
            hasPet = true,
            petName = name.trim().ifBlank { "Люми" }.take(14),
            species = species,
            playfulPoints = traits.first,
            gentlePoints = traits.second,
            curiousPoints = traits.third,
            emotion = PetEmotion.HAPPY,
            emotionUntil = nowMillis + EMOTION_DURATION,
            questDay = epochDay(nowMillis),
            lastUpdated = nowMillis,
            lastMessage = "Привет! Теперь мы одна команда."
        )
    }

    fun act(state: GameState, action: CareAction, nowMillis: Long = System.currentTimeMillis()): ActionResult {
        val fresh = refresh(state, nowMillis)
        val s = fresh.stats
        val message = actionMessage(fresh, action)
        val result = when (action) {
            CareAction.FEED -> {
                if (fresh.coins < 5) {
                    val noFood = "Ягод пока не хватает. Может, заработаем их в игре?"
                    return ActionResult(fresh.copy(lastMessage = noFood), noFood)
                }
                fresh.copy(
                    stats = s.copy(hunger = raise(s.hunger, 24), energy = raise(s.energy, 3)),
                    coins = fresh.coins - 5,
                    xp = fresh.xp + 5,
                    bondPoints = fresh.bondPoints + 4,
                    gentlePoints = fresh.gentlePoints + 3,
                    emotion = PetEmotion.HAPPY,
                    emotionUntil = nowMillis + EMOTION_DURATION,
                    feedCount = fresh.feedCount + 1
                )
            }
            CareAction.PLAY -> {
                if (s.energy < 12) {
                    val tired = "Я хочу играть, но лапки устали. Сначала отдохнём?"
                    return ActionResult(fresh.copy(emotion = PetEmotion.SLEEPY, lastMessage = tired), tired)
                }
                fresh.copy(
                    stats = s.copy(joy = raise(s.joy, 26), energy = lower(s.energy, 12), hygiene = lower(s.hygiene, 4)),
                    coins = fresh.coins + 3,
                    xp = fresh.xp + 8,
                    bondPoints = fresh.bondPoints + 7,
                    playfulPoints = fresh.playfulPoints + 4,
                    emotion = PetEmotion.EXCITED,
                    emotionUntil = nowMillis + EMOTION_DURATION,
                    playCount = fresh.playCount + 1
                )
            }
            CareAction.CLEAN -> fresh.copy(
                stats = s.copy(hygiene = raise(s.hygiene, 38), joy = lower(s.joy, 2)),
                xp = fresh.xp + 4,
                bondPoints = fresh.bondPoints + 3,
                gentlePoints = fresh.gentlePoints + 2,
                emotion = PetEmotion.PROUD,
                emotionUntil = nowMillis + EMOTION_DURATION,
                cleanCount = fresh.cleanCount + 1
            )
            CareAction.REST -> fresh.copy(
                stats = s.copy(energy = raise(s.energy, 34), hunger = lower(s.hunger, 6)),
                xp = fresh.xp + 3,
                bondPoints = fresh.bondPoints + 2,
                gentlePoints = fresh.gentlePoints + 1,
                emotion = PetEmotion.SLEEPY,
                emotionUntil = nowMillis + EMOTION_DURATION
            )
        }
        val updated = rewardQuestIfNeeded(result)
        return ActionResult(updated.copy(lastMessage = message), message)
    }

    fun visit(state: GameState, habitat: Habitat, nowMillis: Long = System.currentTimeMillis()): ActionResult {
        val fresh = refresh(state, nowMillis)
        if (fresh.level < habitat.unlockLevel) {
            val locked = "Откроется на ${habitat.unlockLevel} уровне"
            return ActionResult(fresh.copy(lastMessage = locked), locked)
        }
        val message = when (habitat) {
            Habitat.HOME -> "Дома столько знакомых запахов. Здесь хорошо."
            Habitat.GARDEN -> "Смотри, бабочка! Интересно, где её дом?"
            Habitat.FOREST -> "Здесь деревья шепчут истории. Давай послушаем."
            Habitat.SHORE -> "Я нашёл ракушку с шумом моря! Она что-то помнит."
        }
        return ActionResult(
            fresh.copy(
                habitat = habitat,
                xp = fresh.xp + 2,
                curiousPoints = fresh.curiousPoints + 4,
                emotion = PetEmotion.CURIOUS,
                emotionUntil = nowMillis + EMOTION_DURATION,
                lastMessage = message
            ),
            message
        )
    }

    fun completeBerryGame(state: GameState, score: Int, nowMillis: Long = System.currentTimeMillis()): ActionResult {
        val fresh = refresh(state, nowMillis)
        val safeScore = score.coerceIn(0, 99)
        val reward = safeScore * 2 + 5
        val message = when {
            safeScore >= 12 -> "Мы потрясающая команда! Я горжусь нами!"
            safeScore >= 7 -> "Здорово поймали! У меня до сих пор сердце скачет."
            safeScore >= 3 -> "Мне понравилось играть вместе!"
            else -> "Главное, что мы были одной командой."
        }
        val gameEmotion = if (safeScore >= 7) PetEmotion.PROUD else PetEmotion.EXCITED
        return ActionResult(
            fresh.copy(
                stats = fresh.stats.copy(
                    joy = raise(fresh.stats.joy, 12 + safeScore.coerceAtMost(8)),
                    energy = lower(fresh.stats.energy, 5)
                ),
                coins = fresh.coins + reward,
                xp = fresh.xp + safeScore * 3 + 5,
                bondPoints = fresh.bondPoints + safeScore * 2 + 3,
                playfulPoints = fresh.playfulPoints + safeScore.coerceAtMost(10) + 3,
                bestBerryScore = maxOf(fresh.bestBerryScore, safeScore),
                gamesPlayed = fresh.gamesPlayed + 1,
                emotion = gameEmotion,
                emotionUntil = nowMillis + EMOTION_DURATION,
                lastMessage = message
            ),
            message
        )
    }

    fun petPet(state: GameState, nowMillis: Long = System.currentTimeMillis()): ActionResult {
        val fresh = refresh(state, nowMillis)
        val earnsBond = nowMillis - fresh.lastPettingAt >= PETTING_REWARD_COOLDOWN
        val message = when (fresh.personalityTrait) {
            PersonalityTrait.PLAYFUL -> "Хи-хи! Щекотно! Но не останавливайся."
            PersonalityTrait.GENTLE -> "Я узнаю твою руку. Рядом с тобой спокойно."
            PersonalityTrait.CURIOUS -> "А почему поглаживание такое приятное?"
            PersonalityTrait.BALANCED -> "Вот так хорошо. Давай посидим рядом."
        }
        val updated = fresh.copy(
            bondPoints = fresh.bondPoints + if (earnsBond) 2 else 0,
            gentlePoints = fresh.gentlePoints + if (earnsBond) 2 else 0,
            emotion = PetEmotion.AFFECTIONATE,
            emotionUntil = nowMillis + EMOTION_DURATION,
            lastPettingAt = if (earnsBond) nowMillis else fresh.lastPettingAt,
            lastMessage = message
        )
        return ActionResult(updated, message)
    }

    fun emotion(state: GameState, nowMillis: Long = System.currentTimeMillis()): PetEmotion = when {
        state.stats.energy < 25 -> PetEmotion.SLEEPY
        state.stats.hunger < 25 -> PetEmotion.HUNGRY
        state.stats.hygiene < 25 -> PetEmotion.DIRTY
        state.stats.joy < 25 -> PetEmotion.LONELY
        nowMillis < state.emotionUntil -> state.emotion
        state.stats.wellbeing >= 82 -> PetEmotion.HAPPY
        else -> PetEmotion.CALM
    }

    fun mood(stats: PetStats): String = when {
        stats.energy < 25 -> PetEmotion.SLEEPY.title
        stats.hunger < 25 -> PetEmotion.HUNGRY.title
        stats.hygiene < 25 -> PetEmotion.DIRTY.title
        stats.joy < 25 -> PetEmotion.LONELY.title
        stats.wellbeing >= 82 -> PetEmotion.HAPPY.title
        else -> PetEmotion.CALM.title
    }

    private fun actionMessage(state: GameState, action: CareAction): String = when (action) {
        CareAction.FEED -> when (state.personalityTrait) {
            PersonalityTrait.PLAYFUL -> "Ам! Ягода исчезла. Загадочное дело!"
            PersonalityTrait.GENTLE -> "Спасибо. Давай поделимся последней ягодой."
            PersonalityTrait.CURIOUS -> "Интересно, почему красные ягоды вкуснее?"
            PersonalityTrait.BALANCED -> "М-м-м! Самая вкусная ягода."
        }
        CareAction.PLAY -> when (state.personalityTrait) {
            PersonalityTrait.PLAYFUL -> "Ещё раз! Я почти научился летать!"
            PersonalityTrait.GENTLE -> "Мне нравится играть именно с тобой."
            PersonalityTrait.CURIOUS -> "А если бросить игрушку немного выше?"
            PersonalityTrait.BALANCED -> "Вот это приключение! Ещё раз?"
        }
        CareAction.CLEAN -> when (state.personalityTrait) {
            PersonalityTrait.PLAYFUL -> "Пузырьки! Сейчас один поймаю носом."
            PersonalityTrait.GENTLE -> "Тёплая вода успокаивает. Спасибо."
            PersonalityTrait.CURIOUS -> "Куда исчезает пена после купания?"
            PersonalityTrait.BALANCED -> "Чистота! Теперь я сияю."
        }
        CareAction.REST -> when (state.personalityTrait) {
            PersonalityTrait.PLAYFUL -> "Я не сплю... Я просто закрыл глаза..."
            PersonalityTrait.GENTLE -> "Побудь рядом, пока я засыпаю."
            PersonalityTrait.CURIOUS -> "Интересно, куда мы уходим во сне?"
            PersonalityTrait.BALANCED -> "Мне приснился светящийся лес..."
        }
    }

    private fun seedPersonality(state: GameState): GameState {
        if (state.playfulPoints + state.gentlePoints + state.curiousPoints > 0) return state
        val traits = startingTraits(state.species)
        return state.copy(playfulPoints = traits.first, gentlePoints = traits.second, curiousPoints = traits.third)
    }

    private fun startingTraits(species: Species): Triple<Int, Int, Int> = when (species) {
        Species.FOX -> Triple(8, 4, 7)
        Species.AXOLOTL -> Triple(3, 8, 5)
        Species.OWL -> Triple(4, 6, 9)
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
