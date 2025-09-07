package com.example.DhoonHub.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

sealed class ConnectionStatus {
    object Available : ConnectionStatus()
    object Unavailable : ConnectionStatus()
}

class NetworkConnectivityObserver(private val context: Context) {

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    fun observe(): Flow<ConnectionStatus> {
        val observerContext = this.context // Capture context here
        return callbackFlow {
            // Emit current status immediately
            val currentStatus = if (NetworkUtils.isInternetAvailable(observerContext)) {
                ConnectionStatus.Available
            } else {
                ConnectionStatus.Unavailable
            }
            trySend(currentStatus)

            val callback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    super.onAvailable(network)
                    trySend(ConnectionStatus.Available)
                }

                override fun onLost(network: Network) {
                    super.onLost(network)
                    trySend(ConnectionStatus.Unavailable)
                }

                override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                    super.onCapabilitiesChanged(network, networkCapabilities)
                    if (networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                        networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) {
                        trySend(ConnectionStatus.Available)
                    } else {
                        trySend(ConnectionStatus.Unavailable)
                    }
                }
            }

            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()

            connectivityManager.registerNetworkCallback(request, callback)

            awaitClose { connectivityManager.unregisterNetworkCallback(callback) }
        }.distinctUntilChanged()
    }
}