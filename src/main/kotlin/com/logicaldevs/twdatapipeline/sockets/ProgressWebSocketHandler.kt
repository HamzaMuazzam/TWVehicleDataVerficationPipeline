package com.logicaldevs.twdatapipeline.sockets

import org.springframework.stereotype.Component
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketHandler
import org.springframework.web.socket.WebSocketMessage
import org.springframework.web.socket.WebSocketSession
import java.util.concurrent.ConcurrentHashMap

@Component
class ProgressWebSocketHandler : WebSocketHandler {

    private val sessions = ConcurrentHashMap<String, WebSocketSession>()

    override fun afterConnectionEstablished(session: WebSocketSession) {
        sessions[session.id] = session
        println("WebSocket connection established: ${session.id}")
    }

    override fun handleMessage(session: WebSocketSession, message: WebSocketMessage<*>) {
        // Handle incoming messages if needed
    }

    override fun handleTransportError(session: WebSocketSession, exception: Throwable) {
        println("WebSocket transport error: ${exception.message}")
        sessions.remove(session.id)
    }

    override fun afterConnectionClosed(session: WebSocketSession, closeStatus: CloseStatus) {
        println("WebSocket connection closed: ${session.id}")
        sessions.remove(session.id)
    }

    override fun supportsPartialMessages(): Boolean = false

    fun sendProgressUpdate(message: String) {
        val textMessage = TextMessage(message)
        sessions.values.forEach { session ->
            try {
                if (session.isOpen) {
                    session.sendMessage(textMessage)
                }
            } catch (e: Exception) {
                println("Error sending message to session ${session.id}: ${e.message}")
                sessions.remove(session.id)
            }
        }
    }

    fun sendProgressUpdate(progress: Int, message: String) {
        val jsonMessage = """{"progress": $progress, "message": "$message", "timestamp": "${System.currentTimeMillis()}"}"""
        sendProgressUpdate(jsonMessage)
    }
}