package com.automation.practice.core.net

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/** true selama jaringan default punya akses internet; emit tiap berubah (FR-PAYM-15). */
fun Context.onlineFlow(): Flow<Boolean> = callbackFlow {
    val cm = getSystemService(ConnectivityManager::class.java)
    // VALIDATED: jaringan yang "naik" tapi tidak sampai internet (mis. AVD) tidak dianggap online.
    fun hasInternet(c: NetworkCapabilities?) =
        c?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true && c.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) { trySend(hasInternet(caps)) }
        override fun onLost(network: Network) { trySend(false) }
    }
    trySend(hasInternet(cm.getNetworkCapabilities(cm.activeNetwork)))
    cm.registerDefaultNetworkCallback(callback)
    awaitClose { cm.unregisterNetworkCallback(callback) }
}.distinctUntilChanged()
