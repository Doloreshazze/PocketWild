package com.playeverywhere.pocketwild.game

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf

class GameViewModel(application: Application) : AndroidViewModel(application) {
    private val store = GameStore(application)
    private val _state = mutableStateOf(GameEngine.refresh(store.load()))
    val state: State<GameState> = _state

    init { store.save(_state.value) }

    fun createPet(name: String, species: Species) = update(GameEngine.createPet(name, species))

    fun act(action: CareAction) {
        val result = GameEngine.act(_state.value, action)
        update(result.state)
    }

    fun visit(habitat: Habitat) {
        val result = GameEngine.visit(_state.value, habitat)
        update(result.state)
    }

    fun completeBerryGame(score: Int) {
        val result = GameEngine.completeBerryGame(_state.value, score)
        update(result.state)
    }

    fun petPet() {
        val result = GameEngine.petPet(_state.value)
        update(result.state)
    }

    private fun update(value: GameState) {
        _state.value = value
        store.save(value)
    }
}
