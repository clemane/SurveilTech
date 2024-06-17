package com.surveiltech.application.respository

import android.util.Log
import com.surveiltech.application.chatapi.ApiClient
import com.surveiltech.application.response.ChatRequest
import com.surveiltech.application.response.ChatResponse
import com.surveiltech.application.response.Choice
import com.surveiltech.application.response.Message
import com.surveiltech.application.util.CHATGPT_MODEL
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.concurrent.Executor

class ChatRespository {

    private val apiClient = ApiClient.getInstance()

    fun createChatCompletion(message:String){

            try {
                val chatRequest = ChatRequest(
                    arrayListOf(
                        Message(
                            "Introduce your self and Lets ask me what topic I want to discuss",
                            "system"
                        ),
                    Message(
                            message,
                           "user"
                        )
                    ),
                    CHATGPT_MODEL
                )
                apiClient.createChatCompletion(chatRequest).enqueue(object : Callback<ChatResponse>{
                    override fun onResponse(
                        call: Call<ChatResponse>,
                        response: Response<ChatResponse>
                    ) {
                        val code = response.code()
                        Log.d("message", "test")
                        if(code == 200){
                            response.body()?.choices?.get(0)?.message?.let {
                                Log.d("message", it.toString())
                            }
                        }else {
                            Log.d("error", "erreur")
                        }


                    }

                    override fun onFailure(call: Call<ChatResponse>, t: Throwable) {
                        t.printStackTrace()
                    }

                })
            } catch (e: Exception){
                e.printStackTrace()
            }

    }
}