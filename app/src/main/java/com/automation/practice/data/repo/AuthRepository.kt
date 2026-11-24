package com.automation.practice.data.repo

import android.util.Log
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

data class User(val uid: String, val displayName: String, val email: String)

/** Error Auth yang dibedakan UI (teks di contract §6). */
enum class AuthError { INVALID_CREDENTIALS, EMAIL_TAKEN, TOO_MANY_REQUESTS, NETWORK, FAILED }

sealed interface LoginResult {
    data class Success(val user: User) : LoginResult
    data class Failed(val error: AuthError) : LoginResult
}

/**
 * Akun & sesi login di Firebase Authentication, profil di `users/{uid}` (contract §8.1–§8.2).
 * Sesi bertahan setelah kill & update APK karena dikelola SDK (PRD FR-AUTH-17). Password tidak pernah disimpan app.
 */
class AuthRepository(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore,
    private val realNow: () -> Long = System::currentTimeMillis,
) {
    var currentUser: User? = auth.currentUser?.toUser()
        private set

    /** Login email + password; buat `users/{uid}` bila Register sebelumnya terputus (contract §8.1). */
    suspend fun login(email: String, password: String): LoginResult = try {
        val fu = auth.signInWithEmailAndPassword(email, password).await().user!!
        val ref = db.collection(USERS).document(fu.uid)
        val name = ref.get().await().getString(DISPLAY_NAME)
            ?: (fu.displayName ?: email.substringBefore('@')).also { ref.set(userDoc(fu, it)).await() }
        // Nama disalin ke profil Auth supaya sapaan Beranda tersedia saat sesi dipulihkan tanpa jaringan.
        if (fu.displayName != name) fu.updateProfile(UserProfileChangeRequest.Builder().setDisplayName(name).build()).await()
        LoginResult.Success(User(fu.uid, name, fu.email ?: email).also { currentUser = it })
    } catch (e: Exception) {
        if (e is CancellationException) throw e
        auth.signOut()
        LoginResult.Failed(authError(e))
    }

    /**
     * Register (PRD FR-AUTH-12): buat akun → tulis `users/{uid}` → `signOut`. Null bila sukses.
     * Gagal menulis `users/{uid}` tidak menggagalkan Register; login berikutnya membuatnya.
     */
    suspend fun register(name: String, email: String, password: String): AuthError? = try {
        val fu = auth.createUserWithEmailAndPassword(email, password).await().user!!
        try {
            db.collection(USERS).document(fu.uid).set(userDoc(fu, name)).await()
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.w(TAG, "users/${fu.uid} belum tertulis; dibuat saat login pertama", e)
        }
        null
    } catch (e: Exception) {
        if (e is CancellationException) throw e
        authError(e)
    } finally {
        auth.signOut()
    }

    fun logout() {
        auth.signOut()
        currentUser = null
    }

    // email dari Firebase (huruf kecil), bukan input user: Security Rules membandingkannya dengan token (contract §8.4).
    private fun userDoc(fu: FirebaseUser, name: String) = mapOf(
        "email" to fu.email,
        DISPLAY_NAME to name,
        "createdAt" to realNow(),
        "schemaVersion" to SCHEMA_VERSION,
    )

    private fun FirebaseUser.toUser() = User(uid, displayName ?: email.orEmpty().substringBefore('@'), email.orEmpty())

    companion object {
        private const val TAG = "AuthRepository"
        private const val USERS = "users"
        private const val DISPLAY_NAME = "displayName"
        private const val SCHEMA_VERSION = 1

        fun authError(e: Exception): AuthError = when (e) {
            is FirebaseAuthUserCollisionException -> AuthError.EMAIL_TAKEN
            // INVALID_LOGIN_CREDENTIALS: email tidak terdaftar & password salah sengaja tidak dibedakan.
            is FirebaseAuthInvalidCredentialsException, is FirebaseAuthInvalidUserException -> AuthError.INVALID_CREDENTIALS
            is FirebaseTooManyRequestsException -> AuthError.TOO_MANY_REQUESTS
            is FirebaseNetworkException -> AuthError.NETWORK
            is FirebaseFirestoreException ->
                if (e.code == FirebaseFirestoreException.Code.UNAVAILABLE) AuthError.NETWORK else AuthError.FAILED
            else -> AuthError.FAILED
        }
    }
}
