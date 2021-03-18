package com.pedro.rtsp.rtp.packets;

import android.media.MediaCodec;
import android.util.Log;

import com.pcyfox.h264.H264HandlerNative;
import com.pedro.rtsp.rtsp.RtpFrame;
import com.pedro.rtsp.utils.RtpConstants;

import java.nio.ByteBuffer;

/**
 * Created by pedro on 27/11/18.
 * <p>
 * RFC 3984
 */
public class H264Packet extends BasePacket {
    private final byte[] header = new byte[5];
    private byte[] STAP_A;
    private final VideoPacketCallback videoPacketCallback;
    private static final String TAG = "H264Packet";
    private final H264HandlerNative h264HandlerNative;

    public H264Packet(byte[] sps, byte[] pps, VideoPacketCallback videoPacketCallback) {
        super(RtpConstants.clockVideoFrequency);
        h264HandlerNative = new H264HandlerNative();
        this.videoPacketCallback = videoPacketCallback;
        channelIdentifier = (byte) 2;
        setSpsPps(sps, pps);
    }

    private byte[] getTestData(int len) {
        final byte[] buf = new byte[len];
        for (int i = 0; i < len; i++) {
            if (i < 4) {
                buf[i] = 0;
                continue;
            }
            buf[i] = (byte) (((i - 5) % 10) + 1);
        }
        buf[4] = 65;
        buf[3] = 1;
        return buf;
    }

    @Override
    public void createAndSendPacket(ByteBuffer byteBuffer, MediaCodec.BufferInfo bufferInfo) {
        final long ts = bufferInfo.presentationTimeUs * 1000L;
        byte[] buf = new byte[bufferInfo.size - bufferInfo.offset];
        byteBuffer.get(buf, bufferInfo.offset, bufferInfo.size);
        h264HandlerNative.packH264ToRTP(buf, buf.length, maxPacketSize, ts, getClock(), true, new H264HandlerNative.ResultCallback() {
            @Override
            public void onCallback(byte[] data) {
                RtpFrame rtpFrame = new RtpFrame(data, ts, data.length, rtpPort, rtcpPort, channelIdentifier);
                rtpFrame.setSequence(getSeq());
                videoPacketCallback.onVideoFrameCreated(rtpFrame);
            }
        });
        byteBuffer.clear();

    }

    public static String bytesToHexString(byte[] src) {
        StringBuilder stringBuilder = new StringBuilder();
        if (src == null || src.length <= 0) {
            return null;
        }
        for (byte b : src) {
            int v = b & 0xFF;
            String hv = Integer.toHexString(v);
            if (hv.length() < 2) {
                stringBuilder.append(0);
            }
            stringBuilder.append(hv).append(" ");
        }
        return stringBuilder.toString();
    }

    //设置聚合方式:STAP-A单时间聚合
    private void setSpsPps(byte[] sps, byte[] pps) {
        String spsHex = bytesToHexString(sps);
        String ppsHex = bytesToHexString(pps);
        h264HandlerNative.updateSPS_PPS(sps, sps.length, pps, pps.length);
        Log.d(TAG, "setSpsPps() called with: sps = [" + spsHex);
        Log.d(TAG, "setSpsPps() called with:  pps = [" + ppsHex + "]");

        STAP_A = new byte[sps.length + pps.length + 5];
        // STAP-A NAL header is 24
        STAP_A[0] = 24;

        //长度占两个字节
        // Write NALU 1 size into the array (NALU 1 is the SPS).
        STAP_A[1] = (byte) (sps.length >> 8);
        STAP_A[2] = (byte) (sps.length & 0xFF);

        // Write NALU 2 size into the array (NALU 2 is the PPS).
        STAP_A[sps.length + 3] = (byte) (pps.length >> 8);
        STAP_A[sps.length + 4] = (byte) (pps.length & 0xFF);

        // Write NALU 1 into the array, then write NALU 2 into the array.
        System.arraycopy(sps, 0, STAP_A, 3, sps.length);
        System.arraycopy(pps, 0, STAP_A, 5 + sps.length, pps.length);
    }

    @Override
    public void reset() {
        super.reset();
    }
}

