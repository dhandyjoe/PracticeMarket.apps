package com.automation.practice.data

import com.automation.practice.data.repo.AuthError
import com.automation.practice.data.repo.AuthRepository.Companion.authError
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.firestore.FirebaseFirestoreException
import org.junit.Assert.assertEquals
import org.junit.Test

/** Error Firebase → teks contract §6 (PRD FR-AUTH-06, FR-AUTH-12). */
class AuthErrorTest {
    @Test fun petaErrorFirebase() {
        assertEquals(AuthError.INVALID_CREDENTIALS, authError(FirebaseAuthInvalidCredentialsException("ERROR_INVALID_CREDENTIAL", "x")))
        assertEquals(AuthError.INVALID_CREDENTIALS, authError(FirebaseAuthInvalidUserException("ERROR_USER_NOT_FOUND", "x")))
        assertEquals(AuthError.EMAIL_TAKEN, authError(FirebaseAuthUserCollisionException("ERROR_EMAIL_ALREADY_IN_USE", "x")))
        assertEquals(AuthError.TOO_MANY_REQUESTS, authError(FirebaseTooManyRequestsException("x")))
        assertEquals(AuthError.NETWORK, authError(FirebaseNetworkException("x")))
        assertEquals(AuthError.NETWORK, authError(FirebaseFirestoreException("x", FirebaseFirestoreException.Code.UNAVAILABLE)))
        assertEquals(AuthError.FAILED, authError(FirebaseFirestoreException("x", FirebaseFirestoreException.Code.PERMISSION_DENIED)))
        assertEquals(AuthError.FAILED, authError(IllegalStateException("x")))
    }
}
