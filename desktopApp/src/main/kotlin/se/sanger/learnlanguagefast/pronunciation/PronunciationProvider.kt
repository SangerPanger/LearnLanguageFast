package se.sanger.learnlanguagefast.pronunciation

interface PronunciationProvider {
    /**
     * Generate pronunciation audio for given text and language.
     * Returns WAV bytes or null if not available.
     */
    suspend fun generate(text: String, languageCode: String): ByteArray?
}

class NoOpPronunciationProvider : PronunciationProvider {
    override suspend fun generate(text: String, languageCode: String): ByteArray? = null
}
