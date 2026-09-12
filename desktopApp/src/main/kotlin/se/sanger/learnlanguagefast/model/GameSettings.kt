package se.sanger.learnlanguagefast.model

data class GameSettings(
    val failsafeEnabled: Boolean = true,
    val failsafeMistakes: Int = DEFAULT_FAILSAFE_MISTAKES,
    val hardcoreEnabled: Boolean = false,
    val repeaterEnabled: Boolean = false,
    val repeaterCount: Int = DEFAULT_REPEATER_COUNT,
    val flowEnabled: Boolean = false,
    val imprintEnabled: Boolean = false,
    // Pronunciation options
    val pronunciationEnabled: Boolean = true,
    val autoPlayPronunciation: Boolean = true
) {
    fun normalized() = copy(
        failsafeMistakes = failsafeMistakes.coerceIn(MIN_FAILSAFE_MISTAKES, MAX_FAILSAFE_MISTAKES),
        repeaterCount = repeaterCount.coerceIn(MIN_REPEATER_COUNT, MAX_REPEATER_COUNT)
    )

    companion object {
        const val DEFAULT_FAILSAFE_MISTAKES = 2
        const val MIN_FAILSAFE_MISTAKES = 1
        const val MAX_FAILSAFE_MISTAKES = 10
        const val DEFAULT_REPEATER_COUNT = 1
        const val MIN_REPEATER_COUNT = 1
        const val MAX_REPEATER_COUNT = 20
    }
}