package com.google.ai.edge.gallery.server

import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import javax.inject.Inject

@Serializable
data class ChatRequest(
    val prompt: String,
    val temperature: Float = 0.7f,
    val max_tokens: Int = 512,
    val stream: Boolean = false
)

@Serializable
data class ChatResponse(
    val response: String,
    val model: String,
    val processing_time_ms: Long
)

@Serializable
data class ErrorResponse(
    val error: String,
    val code: Int
)

class ApiServer @Inject constructor(
    private val inferenceService: InferenceService
) {
    private var server: NettyApplicationEngine? = null
    private val scope = CoroutineScope(Dispatchers.IO)
    
    fun start(port: Int = 8080) {
        server = embeddedServer(Netty, port = port) {
            install(ContentNegotiation) {
                json()
            }
            
            install(CORS) {
                anyHost()
                allowHeader("Content-Type")
            }
            
            routing {
                // Health check
                get("/health") {
                    call.respond(mapOf("status" to "ok"))
                }
                
                // Model status
                get("/v1/models") {
                    call.respond(mapOf(
                        "loaded" to inferenceService.isModelLoaded.value,
                        "model" to "gemma-2b-it" // Get from config
                    ))
                }
                
                // Chat completion endpoint (OpenAI-compatible)
                post("/v1/chat/completions") {
                    try {
                        val request = call.receive<ChatRequest>()
                        val startTime = System.currentTimeMillis()
                        
                        val result = inferenceService.generateResponse(
                            prompt = request.prompt,
                            temperature = request.temperature,
                            maxTokens = request.max_tokens
                        )
                        
                        result.fold(
                            onSuccess = { response ->
                                val processingTime = System.currentTimeMillis() - startTime
                                call.respond(ChatResponse(
                                    response = response,
                                    model = "gemma-2b-it",
                                    processing_time_ms = processingTime
                                ))
                            },
                            onFailure = { error ->
                                call.respond(ErrorResponse(
                                    error = error.message ?: "Unknown error",
                                    code = 500
                                ))
                            }
                        )
                    } catch (e: Exception) {
                        call.respond(ErrorResponse(
                            error = e.message ?: "Invalid request",
                            code = 400
                        ))
                    }
                }
            }
        }.start(wait = false)
    }
    
    fun stop() {
        server?.stop(1000, 2000)
    }
    
    fun getLocalIpAddress(): String? {
        // Implementation to get device IP
        return "192.168.1.x" // Placeholder
    }
}