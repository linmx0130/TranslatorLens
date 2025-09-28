package me.mengxiao.translatorlens

import ai.liquid.leap.LeapClient
import ai.liquid.leap.ModelRunner
import ai.liquid.leap.message.MessageResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch


class LeapModelRunnerHolder(coroutineScope: CoroutineScope, modelPath: String){
    private lateinit var modelRunner: ModelRunner
    init {
        coroutineScope.launch {
            modelRunner = LeapClient.loadModel(modelPath)
        }
    }

    suspend fun translateChineseToEnglish(text: String): String {
        while (!this::modelRunner.isInitialized) {
            delay(100L)
        }
        val conversation = modelRunner.createConversation("Translate from Chinese to English.")
        val result = StringBuilder()
        conversation.generateResponse(text).onEach {
            if (it is MessageResponse.Chunk) {
                result.append(it.text)
            }
        }.collect()
        return result.toString()
    }

    suspend fun translateEnglishToChinese(text: String): String {
        while (!this::modelRunner.isInitialized) {
            delay(100L)
        }
        val conversation = modelRunner.createConversation("将英文内容翻译为中文。")
        val result = StringBuilder()
        conversation.generateResponse(text).onEach {
            if (it is MessageResponse.Chunk) {
                result.append(it.text)
            }
        }.collect()
        return result.toString()
    }
}