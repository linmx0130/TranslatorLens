package me.mengxiao.translatorlens

import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Rect
import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions

data class OCRDetectedTextBlock(
    val text: String,
    val boundingBox: Rect
)

class OCROnImageCapturedCallback(
    private val onBitmapReady: (Bitmap) -> Unit,
    private val onOCRResultReady: (List<OCRDetectedTextBlock>) -> Unit,
): ImageCapture.OnImageCapturedCallback() {

    @OptIn(ExperimentalGetImage::class)
    override fun onCaptureSuccess(imageProxy: ImageProxy) {
        super.onCaptureSuccess(imageProxy)
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            val recognizer = TextRecognition.getClient(JapaneseTextRecognizerOptions.Builder().build())
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    val results = mutableListOf<OCRDetectedTextBlock>()
                    for (block in visionText.textBlocks) {
                        val blockText = block.text
                        val boundingBox = block.boundingBox
                        if (boundingBox!=null) {
                            Log.d(TAG, "Detected: $blockText, box: $boundingBox")
                            results.add(OCRDetectedTextBlock(blockText, boundingBox))
                        }
                    }
                    onOCRResultReady(results)
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to OCR: $e")
                }
        }
        val bitmap = imageProxy.toBitmap()
        val rotateMatrix = Matrix()
        rotateMatrix.setRotate(imageProxy.imageInfo.rotationDegrees.toFloat())
        val rotatedBitmap =
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, rotateMatrix, true)
        onBitmapReady(rotatedBitmap)
        imageProxy.close()
    }

    companion object {
        const val TAG = "OCROnImageCapturedCallback"
    }
}