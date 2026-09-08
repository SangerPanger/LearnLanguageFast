package se.sanger.learnlanguagefast

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform