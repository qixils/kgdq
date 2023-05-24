package club.speedrun.vods

import club.speedrun.vods.db.Database

class RootDatabase : Database("api") {
    private val users = getCollection(User.serializer(), User.COLLECTION_NAME)

    fun getFromDiscord(id: Long): User? = users.find { it.discord?.user?.id == id }
    fun getFromDiscord(id: String): User? {
        return getFromDiscord(id.toLongOrNull() ?: return null)
    }
    fun getFromDiscord(user: DiscordUser): User? = getFromDiscord(user.id)
    fun getFromDiscord(data: DiscordOAuth): User? {
        val oauth = getFromDiscord(data.user ?: return null)
        if (oauth != null) {
            oauth.discord = data
            users.update(oauth)
        }
        return oauth
    }

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