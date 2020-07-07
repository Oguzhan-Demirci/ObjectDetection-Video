package com.demo.objectdetection

import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.util.Log
import android.util.SparseArray
import android.view.SurfaceHolder
import androidx.core.util.forEach
import androidx.core.util.isNotEmpty
import androidx.core.util.valueIterator
import com.huawei.hms.mlsdk.common.MLAnalyzer
import com.huawei.hms.mlsdk.objects.MLObject

class ObjectAnalyzerTransactor : MLAnalyzer.MLTransactor<MLObject> {

    companion object {
        private const val TAG = "ML_ObAnalyzerTransactor"
    }

    private var mSurfaceHolderOverlay: SurfaceHolder? = null

    fun setSurfaceHolderOverlay(surfaceHolder: SurfaceHolder) {
        mSurfaceHolderOverlay = surfaceHolder
    }

    override fun transactResult(results: MLAnalyzer.Result<MLObject>?) {
        val items = results?.analyseList

        items?.forEach { key, value ->
            Log.d(TAG, "transactResult -> " +
                    "Border: ${value.border} " + //Rectangle around this object
                    "Type Possibility: ${value.typePossibility} " + //Possibility between 0-1
                    "Tracing Identity: ${value.tracingIdentity} " + //Tracing number of this object
                    "Type Identity: ${value.typeIdentity}") //Furniture, Plant, Food etc.
        }

        items?.also {
            draw(it)
        }

    }

    private fun draw(items: SparseArray<MLObject>) {

        val canvas = mSurfaceHolderOverlay?.lockCanvas()

        if (canvas != null) {

            //Clear canvas first
            canvas.drawColor(0, PorterDuff.Mode.CLEAR)

            for (item in items.valueIterator()) {
                val type = getItemType(item)

                //Draw a rectangle around detected object.
                val rectangle = item.border
                Paint().also {
                    it.color = Color.YELLOW
                    it.style = Paint.Style.STROKE
                    it.strokeWidth = 8F
                    canvas.drawRect(rectangle, it)
                }

                //Draw text on the upper left corner of the detected object, writing its type.
                Paint().also {
                    it.color = Color.BLACK
                    it.style = Paint.Style.FILL
                    it.textSize = 24F
                    canvas.drawText(type, (rectangle.left).toFloat(), (rectangle.top).toFloat(), it)
                }
            }
        }

        mSurfaceHolderOverlay?.unlockCanvasAndPost(canvas)
    }

    private fun getItemType(item: MLObject) = when(item.typeIdentity) {
        MLObject.TYPE_OTHER -> "Other"
        MLObject.TYPE_FACE -> "Face"
        MLObject.TYPE_FOOD -> "Food"
        MLObject.TYPE_FURNITURE -> "Furniture"
        MLObject.TYPE_PLACE -> "Place"
        MLObject.TYPE_PLANT -> "Plant"
        MLObject.TYPE_GOODS -> "Goods"
        else -> "No match"
    }

    override fun destroy() {
        Log.d(TAG, "destroy")
    }
}