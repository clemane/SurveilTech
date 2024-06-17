package com.surveiltech.application

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.surveiltech.application.chatapi.ApiClient
import com.surveiltech.application.response.ChatRequest
import com.surveiltech.application.response.ChatResponse
import com.surveiltech.application.response.Message
import com.surveiltech.application.respository.ChatRespository
import com.surveiltech.application.scanner.PortScanner
import com.surveiltech.application.ui.RecyclerViewCommon
import com.surveiltech.application.util.AppPreferences
import com.surveiltech.application.util.CHATGPT_MODEL
import com.surveiltech.application.util.CopyUtil
//import kotlinx.android.synthetic.main.fragment_port_item.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


/**
 * A fragment representing a list of Items.
 * Activities containing this fragment MUST implement the
 * [DeviceInfoFragment.OnListFragmentInteractionListener] interface.
 */
class DeviceInfoFragment : Fragment() {
    val viewModel: ScanViewModel by activityViewModels()
    lateinit var scanAllPortsButton: Button
    private lateinit var myTextView: TextView
    private val chatRepository = ChatRespository()
    var ProtocolString: String = ""

    override fun onCreateView(

        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_deviceinfo_list, container, false)
        val recyclerView = view.findViewById<RecyclerViewCommon>(R.id.list)
        val argumentDeviceId = arguments?.getLong("deviceId")!!
        val copyUtil = CopyUtil(view)

        val deviceTypeTextView = view.findViewById<TextView>(R.id.deviceTypeTextView)
        val deviceIpTextView = view.findViewById<TextView>(R.id.deviceIpTextView)
        val deviceNameTextView = view.findViewById<TextView>(R.id.deviceNameTextView)
        val deviceHwAddressTextView = view.findViewById<TextView>(R.id.deviceHwAddressTextView)
        val deviceVendorTextView = view.findViewById<TextView>(R.id.deviceVendorTextView)
        myTextView = view.findViewById(R.id.textGPT)
        copyUtil.makeTextViewCopyable((deviceTypeTextView))
        copyUtil.makeTextViewCopyable((deviceIpTextView))
        copyUtil.makeTextViewCopyable(deviceNameTextView)
        copyUtil.makeTextViewCopyable(deviceHwAddressTextView)
        copyUtil.makeTextViewCopyable(deviceVendorTextView)
        viewModel.deviceDao.getById(argumentDeviceId).observe(viewLifecycleOwner, Observer {
            fetchInfo(it.asDevice)
            deviceTypeTextView.text = getString(it.deviceType.label)
            deviceIpTextView.text = it.ip.hostAddress
            deviceNameTextView.text = if (it.isScanningDevice) {
                getString(R.string.this_device)
            } else {
                it.deviceName
            }
            deviceHwAddressTextView.text =
                it.hwAddress?.getAddress(AppPreferences(this).hideMacDetails)
            deviceVendorTextView.text = it.vendorName
        })

        val ports = viewModel.portDao.getAllForDevice(argumentDeviceId)

        recyclerView.setHandler(requireContext(), this, object :
            RecyclerViewCommon.Handler<Port>(R.layout.fragment_port_item, ports) {
            override fun shareIdentity(a: Port, b: Port) = a.port == b.port
            override fun areContentsTheSame(a: Port, b: Port) = a == b
            override fun onClickListener(view: View, value: Port) {
                viewModel.viewModelScope.launch(context = Dispatchers.IO) {
                    val ip = viewModel.deviceDao.getByIdNow(value.deviceId).ip
                    val portDescription = PortDescription.commonPorts.find { it.port == value.port }
                    withContext(Dispatchers.Main) {
                        if (portDescription?.urlSchema == null) {
                            copyUtil.copyText("${ip.hostAddress}:${value.port}")
                        } else {
                            val intent = Intent(Intent.ACTION_VIEW)
                            intent.data = Uri.parse("${portDescription.urlSchema}://${ip}:${value.port}")
                            startActivity(intent)
                        }
                    }
                }
            }


            override fun onLongClickListener(view: View, value: Port): Boolean {
                viewModel.viewModelScope.launch(context = Dispatchers.IO) {
                    val ip = viewModel.deviceDao.getByIdNow(value.deviceId).ip
                    withContext(Dispatchers.Main) {
                        copyUtil.copyText("${ip.hostAddress}:${value.port}")
                    }
                }
                return true
            }

            override fun bindItem(view: View): (value: Port) -> Unit {
                val portNumberTextView: TextView = view.findViewById(R.id.portNumberTextView)
                val protocolTextView: TextView = view.findViewById(R.id.protocolTextView)
                val serviceTextView: TextView = view.findViewById(R.id.serviceNameTextView)

                copyUtil.makeTextViewCopyable(portNumberTextView)
                copyUtil.makeTextViewCopyable(protocolTextView)
                copyUtil.makeTextViewCopyable(serviceTextView)

                return { item ->
                    portNumberTextView.text = item.port.toString()
                    protocolTextView.text = item.protocol.toString()
                    ProtocolString += item.port.toString()
                    serviceTextView.text = item.description?.serviceName
                }
            }

        })

        val apiInterface = ApiClient.getInstance()


        val message = Message("Hello, how are you?", "user")
        val chatRequest = ChatRequest(listOf(message), "gpt-3.5-turbo-16k")
        var reply:String?
        GlobalScope.launch(Dispatchers.IO) {
            val response = apiInterface.createChatCompletion(chatRequest).execute()

            if (response.isSuccessful) {
                val chatResponse = response.body()
                reply = chatResponse?.choices?.get(0)?.message?.content
                Log.d("MainActivity", "Reply from OpenAI: $reply")
            } else {
                Log.e("MainActivity", "Error: ${response.message()}")
            }
        }

        return view
    }

    fun createChatCompletion(message:String){
        chatRepository.createChatCompletion(message)
    }

    fun fetchInfo(device: Device) {
        viewModel.viewModelScope.launch {
            withContext(Dispatchers.IO) {
                PortScanner(device.ip).scanPorts().forEach {
                    launch {
                        val result = it.await()
                        if (result.isOpen) {
                            viewModel.portDao.upsert(
                                Port(
                                    0,
                                    result.port,
                                    result.protocol,
                                    device.deviceId
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}