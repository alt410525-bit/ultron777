package com.ultron.android.chat

/** One chat bubble. `fromUser=false` means Ultron said it. */
data class ChatMessage(
    val id: Long,
    val text: String,
    val fromUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)
