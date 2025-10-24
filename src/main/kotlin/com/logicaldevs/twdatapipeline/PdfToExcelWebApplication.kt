package com.logicaldevs.twdatapipeline

import com.logicaldevs.twdatapipeline.sockets.ProgressWebSocketHandler
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.web.socket.config.annotation.EnableWebSocket
import org.springframework.web.socket.config.annotation.WebSocketConfigurer
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry
import java.util.concurrent.Executor

@SpringBootApplication
@EnableWebSocket
@EnableAsync
@EnableConfigurationProperties
class PdfToExcelWebApplication : WebSocketConfigurer {

    override fun registerWebSocketHandlers(registry: WebSocketHandlerRegistry) {
        registry.addHandler(progressWebSocketHandler(), "/progress")
            .setAllowedOrigins("*")
            .withSockJS()
    }

    @Bean
    fun progressWebSocketHandler() = ProgressWebSocketHandler()

    @Bean(name = ["taskExecutor"])
    fun taskExecutor(): Executor {
        val executor = ThreadPoolTaskExecutor()
        executor.corePoolSize = 4
        executor.maxPoolSize = 8
        executor.queueCapacity = 100
        executor.setThreadNamePrefix("pdf-processor-")
        executor.initialize()
        return executor
    }
}

fun main(args: Array<String>) {
    runApplication<PdfToExcelWebApplication>(*args)
}
