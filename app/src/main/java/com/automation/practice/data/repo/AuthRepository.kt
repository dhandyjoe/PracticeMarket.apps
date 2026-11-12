package com.automation.practice.data.repo

import android.content.res.AssetManager
import kotlinx.coroutines.delay
import org.json.JSONArray

enum class Behavior { NORMAL, LOCKED, SLOW, GLITCH, NEW }

data class User(
    val username: String,
    val password: String,
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
 * Akun uji dari assets/seed/users.json, disimpan di memori proses: kill app = kembali ke seed (PRD §6.3 no. 6).
 * shortcut: belum Room; pindah ke UserEntity saat milestone Belanja butuh database bersama.
 */
class AuthRepository(assets: AssetManager) {
    private val users = loadSeed(assets).toMutableList()

    var currentUser: User? = null
        private set

    suspend fun login(identity: String, password: String): LoginResult {
        val user = users.firstOrNull { it.username == identity || it.email.equals(identity, ignoreCase = true) }
        if (user?.behavior == Behavior.SLOW) delay(SLOW_USER_DELAY_MS)
        return when {
            user == null || user.password != password -> LoginResult.Invalid
            user.behavior == Behavior.LOCKED -> LoginResult.Locked
            else -> LoginResult.Success(user.also { currentUser = it })
        }
    }

    /** Dipakai login sidik jari: kredensial sudah terbukti saat login pertama. */
    fun loginAs(username: String): User? =
        users.firstOrNull { it.username == username && it.behavior != Behavior.LOCKED }?.also { currentUser = it }

    /** Null bila email sudah terdaftar. Username akun baru = email. */
    fun register(name: String, email: String, password: String): User? {
        if (findByEmail(email) != null) return null
        return User(email, password, name, email, Behavior.NORMAL).also { users += it }
    }

    fun findByEmail(email: String): User? = users.firstOrNull { it.email.equals(email, ignoreCase = true) }

    fun changePassword(email: String, newPassword: String): Boolean {
        val i = users.indexOfFirst { it.email.equals(email, ignoreCase = true) }
        if (i < 0) return false
        users[i] = users[i].copy(password = newPassword)
        return true
    }

    fun logout() {
        currentUser = null
    }

    private companion object {
        const val SLOW_USER_DELAY_MS = 4_000L // PRD §11.1: loading 3–5 dtk

        fun loadSeed(assets: AssetManager): List<User> {
            val arr = JSONArray(assets.open("seed/users.json").bufferedReader().use { it.readText() })
            return List(arr.length()) { i ->
                arr.getJSONObject(i).let {
                    User(
                        it.getString("username"),
                        it.getString("password"),
                        it.getString("displayName"),
                        it.getString("email"),
                        Behavior.valueOf(it.getString("behavior").uppercase()),
                    )
                }
            }
        }
    }
}
