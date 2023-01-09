package club.speedrun.vods

import club.speedrun.vods.db.Database

class RootDatabase : Database("api") {
    private val users = getCollection(User.serializer(), User.COLLECTION_NAME)

    fun getFromDiscord(data: DiscordOAuth): User? = users.find { it.discord?.user?.id == data.user.id }
    fun getFromDiscord(id: Long): User? = users.find { it.discord?.user?.id == id }
    fun getFromDiscord(id: String): User? {
        val longId = id.toLongOrNull() ?: return null
        return users.find { it.discord?.user?.id == longId }
    }
    fun getFromDiscord(user: DiscordUser): User? = users.find { it.discord?.user?.id == user.id }

    fun getOrCreateFromDiscord(data: DiscordOAuth): User {
        val user = getFromDiscord(data)
        if (user != null) return user
        val newUser = User(discord = data)
        users.insert(newUser)
        return newUser
    }

    fun getFromSession(session: UserSession?): User? {
        if (session == null) return null
        return users.find { it.id == session.id && it.token == session.token }
    }

    fun getFromToken(token: String): User? = users.find { it.token == token }
}