package com.playeverywhere.pocketwild.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GameEngineTest {
    @Test
    fun awayTimeDecreasesNeedsButNeverBelowZero() {
        val start = 1_700_000_000_000L
        val state = GameState(
            hasPet = true,
            stats = PetStats(hunger = 20, joy = 10, energy = 15, hygiene = 12),
            lastUpdated = start,
            questDay = -1
        )

        val refreshed = GameEngine.refresh(state, start + 48 * 3_600_000L)

        assertEquals(0, refreshed.stats.hunger)
        assertTrue(refreshed.stats.joy >= 0)
        assertTrue(refreshed.stats.energy >= 0)
        assertTrue(refreshed.stats.hygiene >= 0)
    }

    @Test
    fun dailyQuestRewardsExactlyOnce() {
        val start = System.currentTimeMillis()
        var state = GameEngine.createPet("Люми", Species.FOX, start).copy(coins = 100)
        state = GameEngine.act(state, CareAction.FEED, start).state
        state = GameEngine.act(state, CareAction.FEED, start).state
        state = GameEngine.act(state, CareAction.PLAY, start).state
        state = GameEngine.act(state, CareAction.CLEAN, start).state

        assertTrue(state.questComplete)
        assertTrue(state.claimedQuest)
        val rewardedCoins = state.coins

        state = GameEngine.act(state, CareAction.CLEAN, start).state
        assertEquals(rewardedCoins, state.coins)
    }

    @Test
    fun lockedHabitatCannotBeVisited() {
        val state = GameState(hasPet = true, xp = 0, habitat = Habitat.HOME)
        val result = GameEngine.visit(state, Habitat.FOREST)
        assertEquals(Habitat.HOME, result.state.habitat)
    }

    @Test
    fun berryGameBuildsBondAndKeepsBestScore() {
        val state = GameState(hasPet = true, coins = 10, bondPoints = 4, bestBerryScore = 8)
        val result = GameEngine.completeBerryGame(state, 6).state

        assertEquals(1, result.gamesPlayed)
        assertEquals(8, result.bestBerryScore)
        assertEquals(19, result.bondPoints)
        assertEquals(27, result.coins)
    }

    @Test
    fun pettingBuildsGentlenessButRewardHasCooldown() {
        val start = 1_700_000_000_000L
        val state = GameEngine.createPet("Люми", Species.FOX, start).copy(bondPoints = 0)

        val firstPet = GameEngine.petPet(state, start + 10_000L).state
        val secondPet = GameEngine.petPet(firstPet, start + 11_000L).state

        assertEquals(2, firstPet.bondPoints)
        assertEquals(firstPet.gentlePoints, secondPet.gentlePoints)
        assertEquals(2, secondPet.bondPoints)
        assertEquals(PetEmotion.AFFECTIONATE, secondPet.emotion)
    }

    @Test
    fun urgentNeedOverridesTemporaryEmotion() {
        val now = 1_700_000_000_000L
        val state = GameState(
            hasPet = true,
            stats = PetStats(hunger = 10),
            emotion = PetEmotion.EXCITED,
            emotionUntil = now + 60_000L
        )

        assertEquals(PetEmotion.HUNGRY, GameEngine.emotion(state, now))
    }

    @Test
    fun playingDevelopsPlayfulPersonality() {
        val start = 1_700_000_000_000L
        val state = GameEngine.createPet("Люми", Species.AXOLOTL, start)

        val afterPlaying = GameEngine.act(state, CareAction.PLAY, start + 1_000L).state

        assertTrue(afterPlaying.playfulPoints > state.playfulPoints)
        assertEquals(PetEmotion.EXCITED, afterPlaying.emotion)
    }
}
