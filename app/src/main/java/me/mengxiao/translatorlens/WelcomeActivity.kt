package me.mengxiao.translatorlens

import ai.liquid.leap.downloader.LeapDownloadableModel
import ai.liquid.leap.downloader.LeapModelDownloader
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.delay
import kotlinx.parcelize.Parcelize

enum class ModelStatus {
    Checking,
    DownloadInProgress,
    Downloaded,
    NotOnLocal,
    Error
}

@Parcelize
enum class TranslationCase: Parcelable{
    ChineseToEnglish,
    EnglishToChinese,
}
const val MODEL_SLUG = "qwen-0.6b"
const val MODEL_QUANTIZATION_SLUG="qwen-0.6b-20250610-8da4w"

class WelcomeActivity : ComponentActivity() {
    private lateinit var modelDownloader: LeapModelDownloader
    private val modelStatus = MutableLiveData<ModelStatus>(ModelStatus.Checking)
    private val downloadingProgress = MutableLiveData<Double>(0.0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        modelDownloader = LeapModelDownloader(this)
        launchCheckModelStatus()
        enableEdgeToEdge()
        setContent {
            val modelStatusState by modelStatus.observeAsState(ModelStatus.Checking)
            val downloadingProgressState by downloadingProgress.observeAsState(0.0)
            Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                when (modelStatusState) {
                    ModelStatus.Checking -> {
                        ModelStatusCheckingPage(Modifier.padding(innerPadding))
                    }
                    ModelStatus.DownloadInProgress -> {
                        ModelDownloadingPage(downloadingProgressState, Modifier.padding(innerPadding))
                    }
                    ModelStatus.Downloaded -> {
                        ModelReadyPage(onCaseSelect = {
                            val model = LeapDownloadableModel(
                                MODEL_SLUG,
                                MODEL_QUANTIZATION_SLUG,
                                null
                            )
                            val intent = Intent(this@WelcomeActivity, MainActivity::class.java)
                            intent.putExtra("case", it as Parcelable)
                            intent.putExtra("model", modelDownloader.getModelFile(model).absolutePath)
                            this@WelcomeActivity.startActivity(intent)
                        },
                            Modifier.padding(innerPadding)
                        )
                    }
                    ModelStatus.NotOnLocal -> {
                        ModelNotAvailablePage(
                            onDownloadButtonClick = { launchDownloadModel() },
                            modifier=Modifier.padding(innerPadding)
                        )
                    }
                    ModelStatus.Error -> {
                        ModelStatusErrorPage(Modifier.padding(innerPadding))
                    }
                }
            }
        }
    }

    fun launchCheckModelStatus() {
        lifecycleScope.launch {
            val model = LeapDownloadableModel(
                MODEL_SLUG,
                MODEL_QUANTIZATION_SLUG,
                null
            )
            val status = modelDownloader.queryStatus(model)
            updateUIModelStateWithDownloaderStatus(status)
        }
    }

    fun launchDownloadModel() {
        lifecycleScope.launch {
            val model = LeapDownloadableModel.resolve(
                MODEL_SLUG,
                MODEL_QUANTIZATION_SLUG
            )
            if (model == null) {
                modelStatus.value = ModelStatus.Error
                return@launch
            }
            modelDownloader.requestDownloadModel(model, true)
            // keep pooling status
            while(true) {
                val status = modelDownloader.queryStatus(model)
                updateUIModelStateWithDownloaderStatus(status)
                if (status is LeapModelDownloader.ModelDownloadStatus.Downloaded) {
                    break
                }
                delay(300L)
            }
        }
    }

    fun updateUIModelStateWithDownloaderStatus(status: LeapModelDownloader.ModelDownloadStatus) {
        when (status) {
            is LeapModelDownloader.ModelDownloadStatus.DownloadInProgress -> {
                modelStatus.value = ModelStatus.DownloadInProgress
                downloadingProgress.value = status.downloadedSizeInBytes.toDouble() / status.totalSizeInBytes
            }
            is LeapModelDownloader.ModelDownloadStatus.Downloaded -> {
                modelStatus.value = ModelStatus.Downloaded
            }
            LeapModelDownloader.ModelDownloadStatus.NotOnLocal -> {
                modelStatus.value = ModelStatus.NotOnLocal
            }
        }
    }
}

@Composable
fun ModelReadyPage(onCaseSelect: (TranslationCase) -> Unit, modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier= modifier.fillMaxHeight()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Choose a translation case"
            )
            Button(onClick = {
                onCaseSelect(TranslationCase.ChineseToEnglish)
            }) {
                Text(text = "Chinese -> English")
            }
            Button(onClick = {
                onCaseSelect(TranslationCase.EnglishToChinese)
            }) {
                Text(text = "English -> Chinese")
            }
        }
    }
}


@Composable
fun ModelNotAvailablePage(onDownloadButtonClick: (() -> Unit), modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier= modifier.fillMaxHeight()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Local translation model is not downloaded."
            )
            Button(onClick = {
                onDownloadButtonClick()
            }) {
                Text(text = "Download now!")
            }
        }
    }
}

@Composable
fun ModelDownloadingPage(progress: Double, modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier= modifier.fillMaxHeight()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth().padding(8.dp)
        ) {
            Text(
                text = "Downloading the model"
            )
            LinearProgressIndicator(
                progress = { progress.toFloat() },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
fun ModelStatusCheckingPage(modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier= modifier.fillMaxHeight()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth().padding(8.dp)
        ) {
            Text(
                text = "Checking model status"
            )
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
            )
        }
    }
}

@Composable
fun ModelStatusErrorPage(modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier= modifier.fillMaxHeight()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth().padding(8.dp)
        ) {
            Text(
                text = "Some error happens! Restart this app to have a retry"
            )
        }
    }
}