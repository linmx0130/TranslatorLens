package me.mengxiao.translatorlens

import ai.liquid.leap.LeapClient
import ai.liquid.leap.ModelRunner
import ai.liquid.leap.message.MessageResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch


class LeapModelRunnerHolder(private val coroutineScope: CoroutineScope){
    private lateinit var modelRunner: ModelRunner
    init {
        coroutineScope.launch {
            // modelRunner = LeapClient.loadModel("/data/local/tmp/liquid/LFM2-350M.bundle")
            // modelRunner = LeapClient.loadModel("/data/local/tmp/liquid/lfm2-350m-enjpmt.bundle")
            modelRunner = LeapClient.loadModel("/data/local/tmp/liquid/qwen3-0_6b.bundle")
        }
    }

    suspend fun translateJapaneseToEnglish(text: String): String {
        while (!this::modelRunner.isInitialized) {
            delay(100L)
        }
        val conversation = modelRunner.createConversation("Translate the Japanese content to English. Be accurate. Do not add any other content.")
        val result = StringBuilder()
        conversation.generateResponse(text).onEach {
            if (it is MessageResponse.Chunk) {
                result.append(it.text)
            }
        }.collect()
        return result.toString()
    }
}