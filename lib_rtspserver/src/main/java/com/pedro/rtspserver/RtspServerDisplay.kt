package com.pedro.rtspserver

import android.content.Context
import android.media.MediaCodec
import android.os.Build
import androidx.annotation.RequiresApi
import com.pedro.encoder.utils.CodecUtil
import com.pedro.rtplibrary.base.DisplayBase
import com.pedro.rtsp.rtsp.VideoCodec
import com.pedro.rtsp.utils.ConnectCheckerRtsp
import java.io.FileOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.nio.ByteBuffer
import java.util.*
import java.util.concurrent.LinkedBlockingDeque


@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
class RtspServerDisplay(context: Context, useOpengl: Boolean, connectCheckerRtsp: ConnectCheckerRtsp, port: Int) : DisplayBase(context, useOpengl) {
    private val h264BufferQueue = LinkedBlockingDeque<ByteArray>(100)
    var isStop = false
    private val rtspServer: RtspServer = RtspServer(context, connectCheckerRtsp, port)

    //广播的实现 :由客户端发出广播，服务器端接收
    var host = "255.255.255.255" //广播地址
    var port = 9999 //广播的目的端口
    val adds: InetAddress = InetAddress.getByName(host)
    var ds: DatagramSocket = DatagramSocket()
    fun setVideoCodec(videoCodec: VideoCodec) {
        videoEncoder.type = if (videoCodec == VideoCodec.H265) CodecUtil.H265_MIME else CodecUtil.H264_MIME
    }

    fun getEndPointConnection(): String = "rtsp://${rtspServer.serverIp}:${rtspServer.port}/1"

    override fun setAuthorization(user: String, password: String) { //not developed
    }

    fun startStream() {
        super.startStream("")
        isStop = false
        startSave()
    }

    private fun sendData(data: ByteArray) {
        val dp = DatagramPacket(data, data.size, adds, port)
        ds.send(dp)
    }

    private fun startSave() {
        val out = FileOutputStream("/sdcard/h264/test.h264")
        Thread {
            while (!isStop) {
                val buf = h264BufferQueue.take()
                sendData(buf)
/*

                if (buf != null) {
                    out.write(buf)
                    out.flush()
                }
*/
            }
        }.start()
    }

    override fun prepareAudioRtp(isStereo: Boolean, sampleRate: Int) {
        rtspServer.isStereo = isStereo
        rtspServer.sampleRate = sampleRate
    }

    override fun startStreamRtp(url: String) { //unused
    }

    override fun stopStreamRtp() {
        isStop = true
        rtspServer.stopServer()
    }

    override fun getAacDataRtp(aacBuffer: ByteBuffer, info: MediaCodec.BufferInfo) {
        rtspServer.sendAudio(aacBuffer, info)
    }

    override fun onSpsPpsVpsRtp(sps: ByteBuffer, pps: ByteBuffer, vps: ByteBuffer?) {
        val newSps = sps.duplicate()
        val newPps = pps.duplicate()
        val newVps = vps?.duplicate()
        rtspServer.setVideoInfo(newSps, newPps, newVps)
        rtspServer.startServer()
    }

    override fun getH264DataRtp(h264Buffer: ByteBuffer, info: MediaCodec.BufferInfo) {
        val size = info.size
        val byteArray = ByteArray(size)
        h264Buffer.get(byteArray, 0, size)
        h264BufferQueue.push(byteArray)
        rtspServer.sendVideo(h264Buffer, info)
    }

    /**
     * Unused functions
     */
    @Throws(RuntimeException::class)
    override fun resizeCache(newSize: Int) {
    }

    override fun shouldRetry(reason: String?): Boolean = false

    override fun reConnect(delay: Long) {
    }

    override fun setReTries(reTries: Int) {
    }

    override fun getCacheSize(): Int = 0

    override fun getSentAudioFrames(): Long = 0

    override fun getSentVideoFrames(): Long = 0

    override fun getDroppedAudioFrames(): Long = 0

    override fun getDroppedVideoFrames(): Long = 0

    override fun resetSentAudioFrames() {
    }

    override fun resetSentVideoFrames() {
    }

    override fun resetDroppedAudioFrames() {
    }

    override fun resetDroppedVideoFrames() {
    }
}