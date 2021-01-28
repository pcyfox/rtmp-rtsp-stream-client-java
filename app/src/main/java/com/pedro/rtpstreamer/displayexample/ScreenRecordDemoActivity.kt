package com.pedro.rtpstreamer.displayexample

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.blankj.utilcode.util.ScreenUtils
import com.pedro.encoder.input.video.CameraHelper
import com.pedro.rtpstreamer.R
import com.pedro.rtsp.utils.ConnectCheckerRtsp
import com.pedro.rtspserver.RtspServerDisplay

import kotlinx.android.synthetic.main.activity_screen_record.*


class ScreenRecordDemoActivity : AppCompatActivity(), ConnectCheckerRtsp, View.OnClickListener {

    private var serverDisplay: RtspServerDisplay? = null
    private val WRITE_EXTERNAL_STORAGE_REQUEST_CODE = 200
    private val RECORD_AUDIO_REQUEST_CODE = 201
    private val RECORD_CAMERA_REQUEST_CODE = 202
    private val REQUEST_CODE = 202
    private val isDisableAudio = true;
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(R.layout.activity_screen_record)
        initServer();
        requestSDPermission()
        requestAudioPermission();
        requestCameraPermission()
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun initServer() {
        serverDisplay = RtspServerDisplay(this, false, this, 1935)
    }

    override fun onNewBitrateRtsp(bitrate: Long) {

    }

    override fun onConnectionSuccessRtsp() {
        runOnUiThread {
            Toast.makeText(this@ScreenRecordDemoActivity, "Connection success", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onConnectionFailedRtsp(reason: String) {
        runOnUiThread {
            Toast.makeText(
                    this@ScreenRecordDemoActivity,
                    "Connection failed. $reason",
                    Toast.LENGTH_SHORT
            )
                    .show()
            serverDisplay!!.stopStream()
            btn_start_screen?.setText("start")
        }
    }

    override fun onDisconnectRtsp() {
        runOnUiThread {
            Toast.makeText(this@ScreenRecordDemoActivity, "Disconnected", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onAuthErrorRtsp() {
        runOnUiThread {
            Toast.makeText(this@ScreenRecordDemoActivity, "Auth error", Toast.LENGTH_SHORT).show()
            serverDisplay?.stopStream()
            btn_start_screen?.setText("start")
            tv_url.setText("")
        }
    }

    override fun onAuthSuccessRtsp() {
        runOnUiThread {
            Toast.makeText(this@ScreenRecordDemoActivity, "Auth success", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onClick(view: View) {
        val rotation = CameraHelper.getCameraOrientation(this)
        when (view.id) {
            R.id.btn_start_screen -> {
                startActivityForResult((getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager).createScreenCaptureIntent(), REQUEST_CODE)
            }

        }
    }


    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        serverDisplay?.setIntentResult(resultCode, data)
        val w = ScreenUtils.getAppScreenWidth()
        val h = ScreenUtils.getAppScreenHeight()
        val r = w * h * 0.5
        if (!serverDisplay!!.isStreaming) {
            var prepareAudio = false
            if (!isDisableAudio) {
                prepareAudio = serverDisplay!!.prepareAudio(32 * 1024, 16000, true, false, false);
            }
            val prepareVideo = serverDisplay!!.prepareVideo(w, h, 25, r.toInt(), 0, 160)
            if (isDisableAudio) {
                serverDisplay?.disableAudio()
            }
            if (isDisableAudio && prepareVideo || (!isDisableAudio && prepareAudio && prepareVideo)) {
                tv_url.setText("stop")
                serverDisplay!!.startStream()
                tv_url?.setText(serverDisplay?.getEndPointConnection())
            } else {
                Toast.makeText(this, "Error preparing stream, This device cant do it", Toast.LENGTH_SHORT)
                        .show()
            }


        } else {
            btn_start_screen!!.setText("stop    ")
            serverDisplay!!.stopStream()
            tv_url.setText("")
        }
    }


    private fun requestSDPermission(): Boolean {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                        WRITE_EXTERNAL_STORAGE_REQUEST_CODE)
                return false
            }
        }
        return true
    }


    private fun requestAudioPermission(): Boolean {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), RECORD_AUDIO_REQUEST_CODE)
                return false
            }
        }
        return true
    }

    private fun requestCameraPermission(): Boolean {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE), RECORD_CAMERA_REQUEST_CODE)
                return false
            }
        }
        return true
    }
}
