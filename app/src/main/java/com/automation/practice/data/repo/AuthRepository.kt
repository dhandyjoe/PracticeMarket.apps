package com.automation.practice.data.repo

import android.content.res.AssetManager
import com.automation.practice.data.db.MetaRow
import com.automation.practice.data.db.PracticeDao
import com.automation.practice.data.db.UserRow
import com.automation.practice.domain.Hashing
import kotlinx.coroutines.delay
import org.json.JSONArray

enum class Behavior { NORMAL, LOCKED, SLOW, GLITCH, NEW }

data class User(
    val username: String,
    val passwordHash: String,
    val displayName: String,
    val email: String,
    val behavior: Behavior,
)

sealed interface LoginResult {
    data class Success(val user: User) : LoginResult
    data object Invalid : LoginResult
    data object Locked : LoginResult
}

/**
 * Akun uji (seed assets/seed/users.json) dan akun hasil Register di tabel `users`; sesi login di `meta`.
 * Bertahan setelah kill & update APK (PRD FR-AUTH-17). Password hanya disimpan sebagai hash.
 */
class AuthRepository(private val assets: AssetManager, private val dao: PracticeDao) {
    private val users = mutableListOf<User>()

    var currentUser: User? = null
        private set

    init { load() }

    /** Baca ulang dari database; seed bila tabel kosong (first run, Clear data, Reset) dan pulihkan sesi login. */
    fun load() {
        if (dao.users().isEmpty()) dao.upsertUsers(loadSeed(assets).map { it.toRow() })
        users.clear()
        users += dao.users().map { User(it.username, it.passwordHash, it.displayName, it.email, Behavior.valueOf(it.behavior)) }
        currentUser = dao.meta(SESSION_USER)?.let { u -> users.firstOrNull { it.username == u } }
    }

    suspend fun login(identity: String, password: String): LoginResult {
        val user = users.firstOrNull { it.username == identity || it.email.equals(identity, ignoreCase = true) }
        if (user?.behavior == Behavior.SLOW) delay(SLOW_USER_DELAY_MS)
        return when {
            user == null || !Hashing.verify(password, user.passwordHash) -> LoginResult.Invalid
            user.behavior == Behavior.LOCKED -> LoginResult.Locked
            else -> LoginResult.Success(user.also(::startSession))
        }
    }

    /** Dipakai login sidik jari: kredensial sudah terbukti saat login pertama. */
    fun loginAs(username: String): User? =
        users.firstOrNull { it.username == username && it.behavior != Behavior.LOCKED }?.also(::startSession)

    /** Null bila email sudah terdaftar. Username akun baru = email. */
    fun register(name: String, email: String, password: String): User? {
        if (findByEmail(email) != null) return null
        return User(email, Hashing.hash(password), name, email, Behavior.NORMAL).also {
            dao.upsertUsers(listOf(it.toRow()))
            users += it
        }
    }

    fun findByEmail(email: String): User? = users.firstOrNull { it.email.equals(email, ignoreCase = true) }

    fun changePassword(email: String, newPassword: String): Boolean {
        val i = users.indexOfFirst { it.email.equals(email, ignoreCase = true) }
        if (i < 0) return false
        users[i] = users[i].copy(passwordHash = Hashing.hash(newPassword)).also { dao.upsertUsers(listOf(it.toRow())) }
        return true
    }

    fun logout() {
        currentUser = null
        dao.deleteMeta(SESSION_USER)
    }

    private fun startSession(user: User) {
        currentUser = user
        dao.setMeta(MetaRow(SESSION_USER, user.username))
    }

    private fun User.toRow() = UserRow(username, email, displayName, behavior.name, passwordHash)

    private companion object {
        const val SLOW_USER_DELAY_MS = 4_000L // PRD §11.1: loading 3–5 dtk
        const val SESSION_USER = "session_user"

        fun loadSeed(assets: AssetManager): List<User> {
            val arr = JSONArray(assets.open("seed/users.json").bufferedReader().use { it.readText() })
            return List(arr.length()) { i ->
                arr.getJSONObject(i).let {
                    User(
                        it.getString("username"),
                        Hashing.hash(it.getString("password")),
                        it.getString("displayName"),
                        it.getString("email"),
                        Behavior.valueOf(it.getString("behavior").uppercase()),
                    )
                }
            }
        }
    }
}
