package com.surveiltech.application.response

data class ChatRequest(
    val messages: List<Message>,
    val model: String
)
