package id.ac.pkm.sigapnetra.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/** Pengelola koneksi Wi-Fi lokal untuk mengikat proses ke jaringan hotspot tanpa internet. */
class SigapNetworkManager(private val context: Context) {

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    @Volatile
    var okHttpClient: OkHttpClient = createFallbackClient()
        private set

    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    fun bindToLocalWifi(onReady: ((OkHttpClient) -> Unit)? = null) {
        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) // Wajib untuk hotspot tanpa internet
            .build()

        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                // Kunci: Ikat socket factory ke network Wi-Fi lokal
                val client = OkHttpClient.Builder()
                    .socketFactory(network.socketFactory)
                    .connectTimeout(5, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(15, TimeUnit.SECONDS)
                    .build()

                okHttpClient = client
                onReady?.invoke(client)
            }

            override fun onLost(network: Network) {
                okHttpClient = createFallbackClient()
            }
        }

        connectivityManager.requestNetwork(request, networkCallback!!)
    }

    fun unbind() {
        networkCallback?.let {
            try {
                connectivityManager.unregisterNetworkCallback(it)
            } catch (_: Exception) {}
        }
    }

    private fun createFallbackClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }
}
