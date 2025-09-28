package me.mengxiao.translatorlens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.RectF
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.launch
import java.util.concurrent.Executors


class MainActivity : AppCompatActivity() {
    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private lateinit var previewView: PreviewView
    private lateinit var frozenPreviewView: OCRImagePreviewView
    private lateinit var camera: Camera
    private lateinit var imageCapture: ImageCapture
    private lateinit var outputTextView: TextView

    private var workloadExecutor = Executors.newSingleThreadExecutor()
    private var isTranslating = false

    private var modelRunnerHolder = LeapModelRunnerHolder(lifecycleScope)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // binding camera to the preview
        previewView = findViewById<PreviewView>(R.id.previewView)
        frozenPreviewView = findViewById(R.id.frozenPreviewImageView)
        outputTextView = findViewById(R.id.outputTextView)
        frozenPreviewView.onOCRBoundingBoxClickListener =
            object : OCRImagePreviewView.OnOCRBoundingBoxClickListener {
                override fun onBoundingBoxClick(
                    text: String,
                    boundingBox: RectF
                ) {
                    if (isTranslating) {
                        return
                    }
                    isTranslating = true
                    outputTextView.text = "Translating:\n$text"
                    lifecycleScope.launch {
                        val translateResult = modelRunnerHolder.translateChineseToEnglish(text)
                        Log.d("MainActivity", translateResult)
                        outputTextView.text = "Translation result:\n$translateResult"
                        isTranslating = false
                    }
                }
            }
        previewView.implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        previewView.scaleType = PreviewView.ScaleType.FIT_CENTER

        cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener(Runnable {
            val cameraProvider = cameraProviderFuture.get()
            bindPreview(cameraProvider)
        }, ContextCompat.getMainExecutor(this))

        setupButtonOnClick()
        // request permission
        val requestPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) {
                // do nothing
            }
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Permission is not granted, request it
            requestPermissionLauncher.launch(
                Manifest.permission.CAMERA
            )
        }
    }

    fun bindPreview(cameraProvider: ProcessCameraProvider) {
        val preview: Preview = Preview.Builder()
            .build()

        val cameraSelector: CameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build()

        preview.surfaceProvider = previewView.getSurfaceProvider()

        imageCapture = ImageCapture.Builder()
            .setTargetRotation(previewView.display.rotation)
            .build()


        camera = cameraProvider.bindToLifecycle(
            this as LifecycleOwner,
            cameraSelector,
            imageCapture,
            preview
        )
    }

    fun onCaptureClick() {
        imageCapture.takePicture(
            workloadExecutor, OCROnImageCapturedCallback(
                onBitmapReady = { bitmap ->
                    ContextCompat.getMainExecutor(this).execute {
                        frozenPreviewView.imageBitmap = bitmap
                        frozenPreviewView.visibility = View.VISIBLE
                    }
                },
                onOCRResultReady = { results ->
                    frozenPreviewView.setOCRResult(results)
                })
        )
    }

    fun setupButtonOnClick() {
        val captureButton = findViewById<Button>(R.id.captureButton)
        val resetButton = findViewById<Button>(R.id.resetButton)

        captureButton.setOnClickListener {
            captureButton.visibility = View.GONE
            resetButton.visibility = View.VISIBLE
            onCaptureClick()
        }
        resetButton.setOnClickListener {
            captureButton.visibility = View.VISIBLE
            resetButton.visibility = View.GONE
            frozenPreviewView.imageBitmap = null
            outputTextView.text = ""
        }
    }
}