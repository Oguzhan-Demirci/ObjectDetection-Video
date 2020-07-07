package com.demo.objectdetection

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.SurfaceHolder
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.huawei.hms.mlsdk.MLAnalyzerFactory
import com.huawei.hms.mlsdk.common.LensEngine
import com.huawei.hms.mlsdk.objects.MLObjectAnalyzer
import com.huawei.hms.mlsdk.objects.MLObjectAnalyzerSetting
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "ML_MainActivity"
        private const val PERMISSION_REQUEST_CODE = 8
        private val requiredPermissions = arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
    }

    private lateinit var mAnalyzer: MLObjectAnalyzer
    private lateinit var mLensEngine: LensEngine

    private lateinit var mSurfaceHolderCamera: SurfaceHolder
    private lateinit var mSurfaceHolderOverlay: SurfaceHolder

    private lateinit var mObjectAnalyzerTransactor: ObjectAnalyzerTransactor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (hasPermissions(requiredPermissions))
            init()
        else
            ActivityCompat.requestPermissions(this, requiredPermissions, PERMISSION_REQUEST_CODE)
    }

    private fun init() {
        mAnalyzer = createAnalyzer()
        mLensEngine = createLensEngine(resources.configuration.orientation)

        mSurfaceHolderCamera = surface_view_camera.holder
        mSurfaceHolderOverlay = surface_view_overlay.holder

        mSurfaceHolderOverlay.setFormat(PixelFormat.TRANSPARENT)
        mSurfaceHolderCamera.addCallback(surfaceHolderCallback)

        mObjectAnalyzerTransactor = ObjectAnalyzerTransactor()
        mObjectAnalyzerTransactor.setSurfaceHolderOverlay(mSurfaceHolderOverlay)
        mAnalyzer.setTransactor(mObjectAnalyzerTransactor)
    }

    private fun createAnalyzer(): MLObjectAnalyzer {
        val analyzerSetting = MLObjectAnalyzerSetting.Factory()
            .setAnalyzerType(MLObjectAnalyzerSetting.TYPE_VIDEO)
            .allowMultiResults()
            .allowClassification()
            .create()

        return MLAnalyzerFactory.getInstance().getLocalObjectAnalyzer(analyzerSetting)
    }

    private fun createLensEngine(orientation: Int): LensEngine {
        val lensEngineCreator = LensEngine.Creator(applicationContext, mAnalyzer)
            .setLensType(LensEngine.BACK_LENS)
            .applyFps(10F)
            .enableAutomaticFocus(true)

        return when(orientation) {
            Configuration.ORIENTATION_PORTRAIT ->
                lensEngineCreator.applyDisplayDimension(getDisplayMetrics().heightPixels, getDisplayMetrics().widthPixels).create()
            else ->
                lensEngineCreator.applyDisplayDimension(getDisplayMetrics().widthPixels, getDisplayMetrics().heightPixels).create()
        }
    }

    private val surfaceHolderCallback = object : SurfaceHolder.Callback {

        override fun surfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {
            mLensEngine.close()
            init()
            mLensEngine.run(holder)
        }

        override fun surfaceDestroyed(holder: SurfaceHolder?) {
            mLensEngine.release()
        }

        override fun surfaceCreated(holder: SurfaceHolder?) {
            mLensEngine.run(holder)
        }

    }

    override fun onDestroy() {
        super.onDestroy()

        //Release resources
        mAnalyzer.stop()
        mLensEngine.release()
    }

    private fun getDisplayMetrics() = DisplayMetrics().let {
        (getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay.getMetrics(it)
        it
    }

    private fun hasPermissions(permissions: Array<String>) = permissions.all {
        ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PERMISSION_REQUEST_CODE && hasPermissions(requiredPermissions))
            init()
    }

}
