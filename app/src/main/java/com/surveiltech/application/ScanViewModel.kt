package com.surveiltech.application

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkRequest
import android.util.Log
import androidx.lifecycle.*
import com.surveiltech.application.ScanRepository.Companion.TAG
import com.surveiltech.application.scanner.*

class ScanViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.createInstance(application)

    val deviceDao = db.deviceDao()
    private val networkDao = db.networkDao()
    val portDao = db.portDao()
    private val scanDao = db.scanDao()
    private val networkScanRepository = ScanRepository(
        networkDao,
        scanDao,
        deviceDao,
        portDao,
        application
    )
    val scanProgress by lazy { MutableLiveData<ScanRepository.ScanProgress>() }
    private val currentNetworkId = MutableLiveData<Long>()
    val currentScanId = MutableLiveData<Long>()

    val devices: LiveData<List<DeviceWithName>> = currentNetworkId.switchMap { networkId ->
        liveData {
            emitSource(deviceDao.getAll(networkId))
        }
    }

    val currentNetworks: LiveData<List<Network>> = currentScanId.switchMap { scanId ->
        liveData {
            Log.d("asd", "netowkrsadss $scanId")
            emitSource(networkDao.getAll(scanId))
        }
    }

    fun fetchAvailableInterfaces() = networkScanRepository.fetchAvailableInterfaces()

    val availableInterfaces by lazy {
        val med = MediatorLiveData<List<InterfaceScanner.NetworkResult>>()
        // Initialization code for availableInterfaces if any
    }

    class NetworksLiveData(context: Context, private val networkScanRepository: ScanRepository) :
        LiveData<List<InterfaceScanner.NetworkResult>>() {
        private val connectivityManager: ConnectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        private val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: android.net.Network) {
                refresh()
            }
        }

        override fun onActive() {
            super.onActive()
            connectivityManager.registerNetworkCallback(NetworkRequest.Builder().build(), callback)
        }

        override fun onInactive() {
            super.onInactive()
            connectivityManager.unregisterNetworkCallback(callback)
        }

        fun refresh() {
            value = networkScanRepository.fetchAvailableInterfaces()
        }
    }

    suspend fun startScan(interfaceName: String): Network? {
        val network = networkScanRepository.startScan(interfaceName, scanProgress, currentNetworkId) ?: return null
        currentScanId.value = network.scanId
        return network
    }
}
