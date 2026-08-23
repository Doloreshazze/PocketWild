package com.playeverywhere.pocketwild.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.playeverywhere.pocketwild.game.CareAction
import com.playeverywhere.pocketwild.game.Species
import kotlin.concurrent.thread
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

enum class VoiceCue {
    HELLO, FEED, PLAY, CLEAN, REST;

    companion object {
        fun from(action: CareAction): VoiceCue = when (action) {
            CareAction.FEED -> FEED
            CareAction.PLAY -> PLAY
            CareAction.CLEAN -> CLEAN
            CareAction.REST -> REST
        }
    }
}

/** Small procedural voices: no recordings, network, or extra storage required. */
object PetVoice {
    private const val SAMPLE_RATE = 22_050
    private val lock = Any()
    private var generation = 0
    private var current: AudioTrack? = null

    fun play(species: Species, cue: VoiceCue) {
        val ticket = synchronized(lock) {
            generation += 1
            current?.runCatching { stop() }
            current?.release()
            current = null
            generation
        }

        thread(name = "PocketWildVoice", isDaemon = true) {
            val samples = synthesize(species, cue)
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(samples.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            track.write(samples, 0, samples.size)
            val shouldPlay = synchronized(lock) {
                if (ticket == generation) {
                    current = track
                    true
                } else false
            }
            if (!shouldPlay) {
                track.release()
                return@thread
            }

            track.setVolume(0.62f)
            track.play()
            Thread.sleep(samples.size * 1_000L / SAMPLE_RATE + 40L)
            synchronized(lock) {
                if (current === track) {
                    current = null
                    track.release()
                }
            }
        }
    }

    private fun synthesize(species: Species, cue: VoiceCue): ShortArray {
        val base = when (species) {
            Species.FOX -> 560.0
            Species.AXOLOTL -> 760.0
            Species.OWL -> 285.0
        }
        val notes = pattern(cue)
        val totalSeconds = notes.sumOf { it.duration + it.pause }
        val output = ShortArray((totalSeconds * SAMPLE_RATE).toInt().coerceAtLeast(1))
        var cursor = 0
        var noiseSeed = species.ordinal * 7_919 + cue.ordinal * 1_237 + 17

        notes.forEach { note ->
            val count = (note.duration * SAMPLE_RATE).toInt()
            repeat(count) { index ->
                val local = index.toDouble() / SAMPLE_RATE
                val position = index.toDouble() / count.coerceAtLeast(1)
                val attack = (position / 0.12).coerceIn(0.0, 1.0)
                val release = ((1.0 - position) / 0.24).coerceIn(0.0, 1.0)
                val envelope = attack * release * exp(-position * 0.35)
                val glide = 1.0 + note.glide * (position - 0.5)
                val vibrato = 1.0 + sin(2.0 * PI * local * 8.0) * when (species) {
                    Species.FOX -> 0.022
                    Species.AXOLOTL -> 0.045
                    Species.OWL -> 0.012
                }
                val frequency = base * note.ratio * glide * vibrato
                val phase = 2.0 * PI * frequency * local
                noiseSeed = noiseSeed * 1_103_515_245 + 12_345
                val noise = ((noiseSeed ushr 16) and 0x7fff) / 16_383.5 - 1.0
                val wave = when (species) {
                    Species.FOX -> sin(phase) * 0.78 + sin(phase * 2.03) * 0.18
                    Species.AXOLOTL -> sin(phase + sin(phase * 0.47) * 1.25) * 0.72 + noise * 0.08
                    Species.OWL -> sin(phase) * (0.72 + 0.18 * sin(2.0 * PI * local * 5.0)) + sin(phase * 1.5) * 0.12
                }
                if (cursor + index < output.size) {
                    output[cursor + index] = (wave * envelope * 13_500.0)
                        .toInt()
                        .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                        .toShort()
                }
            }
            cursor += count + (note.pause * SAMPLE_RATE).toInt()
        }
        return output
    }

    private fun pattern(cue: VoiceCue): List<Note> = when (cue) {
        VoiceCue.HELLO -> listOf(Note(1.0, .13, .035, .18), Note(1.28, .18, 0.0, .12))
        VoiceCue.FEED -> listOf(Note(.92, .12, .035, -.12), Note(1.08, .12, .025, .08), Note(1.2, .18, 0.0, -.08))
        VoiceCue.PLAY -> listOf(Note(1.0, .11, .025, .18), Note(1.35, .11, .025, .16), Note(1.68, .22, 0.0, .12))
        VoiceCue.CLEAN -> listOf(Note(1.45, .09, .035, .2), Note(1.18, .11, .035, -.2), Note(1.52, .15, 0.0, .16))
        VoiceCue.REST -> listOf(Note(.78, .3, .08, -.12), Note(.63, .42, 0.0, -.15))
    }

    private data class Note(
        val ratio: Double,
        val duration: Double,
        val pause: Double,
        val glide: Double
    )
}
