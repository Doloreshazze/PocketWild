package com.playeverywhere.pocketwild.game

enum class Species(
    val title: String,
    val subtitle: String,
    val accent: Long,
    val emoji: String
) {
    FOX("Лисёнок", "Любопытный и смелый", 0xFFFF8B5EL, "🦊"),
    AXOLOTL("Аксолотль", "Спокойный и добрый", 0xFFFF91B8L, "🫧"),
    OWL("Совёнок", "Мудрый и ночной", 0xFF9B8AE6L, "🦉")
}

enum class Habitat(val title: String, val subtitle: String, val unlockLevel: Int, val emoji: String) {
    HOME("Уютный дом", "Тепло и безопасно", 1, "🏡"),
    GARDEN("Солнечный сад", "Бабочки и ягоды", 2, "🌿"),
    FOREST("Светящийся лес", "Тайны ждут ночью", 4, "✨"),
    SHORE("Лунный берег", "Ракушки и волны", 7, "🌊")
}

enum class CareAction(val title: String, val emoji: String) {
    FEED("Покормить", "🍓"),
    PLAY("Играть", "🪁"),
    CLEAN("Умыть", "🧼"),
    REST("Спать", "🌙")
}

enum class PetEmotion(val title: String, val emoji: String) {
    CALM("спокоен", "😌"),
    HAPPY("счастлив", "😊"),
    EXCITED("в восторге", "🤩"),
    CURIOUS("заинтригован", "🧐"),
    AFFECTIONATE("нежится", "🥰"),
    PROUD("горд собой", "😏"),
    SLEEPY("сонный", "😴"),
    HUNGRY("голодный", "🥺"),
    DIRTY("чумазый", "😅"),
    LONELY("скучает", "😔")
}

enum class PersonalityTrait { PLAYFUL, GENTLE, CURIOUS, BALANCED }

data class PetStats(
    val hunger: Int = 78,
    val joy: Int = 74,
    val energy: Int = 82,
    val hygiene: Int = 76
) {
    val wellbeing: Int get() = (hunger + joy + energy + hygiene) / 4
}

data class GameState(
    val hasPet: Boolean = false,
    val petName: String = "Люми",
    val species: Species = Species.FOX,
    val habitat: Habitat = Habitat.HOME,
    val stats: PetStats = PetStats(),
    val xp: Int = 0,
    val coins: Int = 40,
    val bondPoints: Int = 0,
    val bestBerryScore: Int = 0,
    val gamesPlayed: Int = 0,
    val playfulPoints: Int = 0,
    val gentlePoints: Int = 0,
    val curiousPoints: Int = 0,
    val emotion: PetEmotion = PetEmotion.CALM,
    val emotionUntil: Long = 0,
    val lastPettingAt: Long = 0,
    val feedCount: Int = 0,
    val playCount: Int = 0,
    val cleanCount: Int = 0,
    val questDay: Long = 0,
    val claimedQuest: Boolean = false,
    val lastUpdated: Long = System.currentTimeMillis(),
    val lastMessage: String = "Я так рад тебя видеть!"
) {
    val level: Int get() = xp / 60 + 1
    val levelProgress: Float get() = (xp % 60) / 60f
    val bondLevel: Int get() = bondPoints / 80 + 1
    val bondProgress: Float get() = (bondPoints % 80) / 80f
    val bondTitle: String get() = when {
        bondPoints < 40 -> "Новые друзья"
        bondPoints < 120 -> "Верные друзья"
        bondPoints < 280 -> "Лучшие друзья"
        else -> "Неразлучны"
    }
    val personalityTrait: PersonalityTrait get() {
        val highest = maxOf(playfulPoints, gentlePoints, curiousPoints)
        val lowest = minOf(playfulPoints, gentlePoints, curiousPoints)
        if (highest - lowest <= 3) return PersonalityTrait.BALANCED
        return when (highest) {
            playfulPoints -> PersonalityTrait.PLAYFUL
            gentlePoints -> PersonalityTrait.GENTLE
            else -> PersonalityTrait.CURIOUS
        }
    }
    val personalityTitle: String get() = when (personalityTrait) {
        PersonalityTrait.PLAYFUL -> "Весёлый непоседа"
        PersonalityTrait.GENTLE -> "Ласковое сердце"
        PersonalityTrait.CURIOUS -> "Любопытный исследователь"
        PersonalityTrait.BALANCED -> "Гармоничный друг"
    }
    val traitTotal: Int get() = (playfulPoints + gentlePoints + curiousPoints).coerceAtLeast(1)
    val questProgress: Int get() = minOf(feedCount, 2) + minOf(playCount, 1) + minOf(cleanCount, 1)
    val questComplete: Boolean get() = questProgress >= 4
}

data class ActionResult(val state: GameState, val message: String)
