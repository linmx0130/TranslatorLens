package me.mengxiao.translatorlens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View

data class OCRDisplayedTextBlock(
    val text: String,
    val boundingBox: RectF
)
class OCRImagePreviewView(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int): View(context, attrs, defStyleAttr, defStyleRes) {
    fun interface OnOCRBoundingBoxClickListener {
        fun onBoundingBoxClick(text: String, boundingBox: RectF)
    }

    private val paint: Paint = Paint()
    constructor(context: Context) : this(context, null, 0, 0)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : this(
        context,
        attrs,
        defStyleAttr,
        0
    )

    var imageBitmap: Bitmap? = null
        set(value) {
            field = value
            ocrResults = null
            invalidate()
        }
    private var ocrResults: List<OCRDisplayedTextBlock>? = null
    var onOCRBoundingBoxClickListener: OnOCRBoundingBoxClickListener? = null

    init {
        paint.style = Paint.Style.FILL
        paint.isAntiAlias = true
    }

    fun setOCRResult(results: List<OCRDetectedTextBlock>) {
        val bitmap = this.imageBitmap
        if (bitmap == null) {
            return
        }
        val widthRatio = width.toFloat() / bitmap.width.toFloat()
        val heightRatio = height.toFloat() / bitmap.height.toFloat()
        ocrResults = results.map {
            OCRDisplayedTextBlock(
                text = it.text, boundingBox = RectF(
                    it.boundingBox.left * widthRatio,
                    it.boundingBox.top * heightRatio,
                    it.boundingBox.right * widthRatio,
                    it.boundingBox.bottom * heightRatio
                )
            )
        }
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        imageBitmap?.let {
            val srcRect = Rect(0, 0, it.width, it.height)
            val destRect = RectF(0f, 0f, width.toFloat(), height.toFloat())

            canvas.drawBitmap(it, srcRect, destRect, null)
            if (ocrResults != null) {
                paint.setColor(0x600000FF)
                for (item in ocrResults) {
                    canvas.drawRect(item.boundingBox, paint)
                }
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event == null) {
            return false
        }
        if (event.action == MotionEvent.ACTION_DOWN) {
            if (ocrResults?.let {
                    for (item in ocrResults) {
                        val displayedBox = item.boundingBox
                        if (displayedBox.left <= event.x && displayedBox.right >= event.x && displayedBox.top <= event.y && displayedBox.bottom >= event.y) {
                            Log.d("OCRImagePreviewViewTouch", item.text)
                            onOCRBoundingBoxClickListener?.onBoundingBoxClick(
                                item.text,
                                item.boundingBox
                            )
                            return@let true
                        }
                    }
                    return@let false
                } == true) {
                return true
            }
        }
        return super.onTouchEvent(event)
    }
}