package com.playeverywhere.pocketwild.game

import android.content.Context

class GameStore(context: Context) {
    private val prefs = context.getSharedPreferences("pocket_wild_save", Context.MODE_PRIVATE)

    fun load(): GameState = GameState(
        hasPet = prefs.getBoolean("hasPet", false),
        petName = prefs.getString("petName", "Люми") ?: "Люми",
        species = enumValueOrDefault(prefs.getString("species", null), Species.FOX),
        habitat = enumValueOrDefault(prefs.getString("habitat", null), Habitat.HOME),
        stats = PetStats(
            hunger = prefs.getInt("hunger", 78),
            joy = prefs.getInt("joy", 74),
            energy = prefs.getInt("energy", 82),
            hygiene = prefs.getInt("hygiene", 76)
        ),
        xp = prefs.getInt("xp", 0),
        coins = prefs.getInt("coins", 40),
        bondPoints = prefs.getInt("bondPoints", 0),
        bestBerryScore = prefs.getInt("bestBerryScore", 0),
        gamesPlayed = prefs.getInt("gamesPlayed", 0),
        feedCount = prefs.getInt("feedCount", 0),
        playCount = prefs.getInt("playCount", 0),
        cleanCount = prefs.getInt("cleanCount", 0),
        questDay = prefs.getLong("questDay", 0),
        claimedQuest = prefs.getBoolean("claimedQuest", false),
        lastUpdated = prefs.getLong("lastUpdated", System.currentTimeMillis()),
        lastMessage = prefs.getString("lastMessage", "Я так рад тебя видеть!") ?: "Я так рад тебя видеть!"
    )

    fun save(state: GameState) {
        prefs.edit()
            .putBoolean("hasPet", state.hasPet)
            .putString("petName", state.petName)
            .putString("species", state.species.name)
            .putString("habitat", state.habitat.name)
            .putInt("hunger", state.stats.hunger)
            .putInt("joy", state.stats.joy)
            .putInt("energy", state.stats.energy)
            .putInt("hygiene", state.stats.hygiene)
            .putInt("xp", state.xp)
            .putInt("coins", state.coins)
            .putInt("bondPoints", state.bondPoints)
            .putInt("bestBerryScore", state.bestBerryScore)
            .putInt("gamesPlayed", state.gamesPlayed)
            .putInt("feedCount", state.feedCount)
            .putInt("playCount", state.playCount)
            .putInt("cleanCount", state.cleanCount)
            .putLong("questDay", state.questDay)
            .putBoolean("claimedQuest", state.claimedQuest)
            .putLong("lastUpdated", state.lastUpdated)
            .putString("lastMessage", state.lastMessage)
            .apply()
    }

    private inline fun <reified T : Enum<T>> enumValueOrDefault(value: String?, fallback: T): T =
        runCatching { enumValueOf<T>(value ?: "") }.getOrDefault(fallback)
}
