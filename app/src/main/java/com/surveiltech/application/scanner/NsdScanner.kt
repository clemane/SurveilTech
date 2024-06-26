package com.surveiltech.application.scanner

import android.app.Application
import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import com.surveiltech.application.Protocol
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.net.Inet4Address

class NsdScanner(application: Application, private val onUpdate: (ScanResult) -> Unit) {
    companion object {
        val TAG = NsdScanner::class.java.name
    }

    val nsdManager =
        application.applicationContext.getSystemService(Context.NSD_SERVICE) as NsdManager

    /** mDNS Service Types, that the application checks for.
     * Unfortunately, android does not offer an API for discovering all services
     * See: http://www.dns-sd.org/servicetypes.html
     */
    private val serviceTypes = setOf(
        "_services._dns-sd._udp",
        "_workstation._tcp",
        "_companion-link._tcp",
        "_ssh._tcp",
        "_adisk._tcp",
        "_afpovertcp._tcp",
        "_device-info._tcp",
        "_googlecast._tcp",
        "_printer._tcp",
        "_ipp._tcp",
        "_http._tcp",
        "_smb._tcp",
        "_hap._tcp",
        "_coap._tcp"
    )

    suspend fun scan() = withContext(Dispatchers.IO) {
        serviceTypes.chunked(8).map { serviceTypes ->
            async {
                serviceTypes.forEach { serviceType ->
                    nsdManager.discoverServices(
                        serviceType,
                        NsdManager.PROTOCOL_DNS_SD,
                        NsdListener()
                    )
                    delay(20000)
                }
            }
        }
    }

    inner class NsdResolveListener : NsdManager.ResolveListener {
        override fun onResolveFailed(serviceInfo: NsdServiceInfo?, errorCode: Int) {
            if (errorCode == NsdManager.FAILURE_ALREADY_ACTIVE) return
            Log.e(TAG, "failed $serviceInfo $errorCode")
        }

        override fun onServiceResolved(serviceInfo: NsdServiceInfo?) {
            if (serviceInfo == null) return
            val host = serviceInfo.host
            if (host !is Inet4Address) return
            Log.d(TAG, "resolved!")
            Log.d(TAG, serviceInfo.toString())
            val protocolType = when {
                serviceInfo.serviceType.contains("_tcp") -> Protocol.TCP
                serviceInfo.serviceType.contains("_udp") -> Protocol.UDP
                else -> TODO()
            }
            onUpdate(
                ScanResult(
                    host,
                    if (serviceInfo.port == 0) 0 else serviceInfo.port,
                    serviceInfo.serviceName,
                    protocolType
                )
            )

        }
    }

    inner class NsdListener : NsdManager.DiscoveryListener {
        override fun onServiceFound(serviceInfo: NsdServiceInfo?) {
            nsdManager.resolveService(serviceInfo, NsdResolveListener())
        }

        override fun onStopDiscoveryFailed(serviceType: String?, errorCode: Int) {
            Log.e(TAG, "discovery stop failed $serviceType $errorCode")
        }

        override fun onStartDiscoveryFailed(serviceType: String?, errorCode: Int) {
            Log.e(TAG, "discovery start failed $serviceType $errorCode")
        }

        override fun onDiscoveryStarted(serviceType: String?) {
        }

        override fun onDiscoveryStopped(serviceType: String?) {
            Log.i(TAG, "Discovery stopped: $serviceType")
        }

        override fun onServiceLost(serviceInfo: NsdServiceInfo?) {
        }

    }

    data class ScanResult(
        val ipAddress: Inet4Address,
        /**
         * Will be null, if the port is likely not a valid port. (For example `_device_info` returns
         * port 0
         */
        val port: Int?,
        val name: String,
        val protocol: Protocol
    )
}