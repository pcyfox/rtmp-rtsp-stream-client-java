package com.pedro.rtsp.rtp.packets;

import android.media.MediaCodec;

import com.pedro.rtsp.utils.RtpConstants;

import java.nio.ByteBuffer;
import java.util.Random;

/**
 * Created by pedro on 27/11/18.
 */

public abstract class BasePacket {
    protected final static int maxPacketSize = RtpConstants.MTU;
    protected byte channelIdentifier;
    protected int rtpPort;
    protected int rtcpPort;
    private final long clock;
    private long seq = 0;
    private int ssrc;

    public BasePacket(long clock) {
        this.clock = clock;
        ssrc = new Random().nextInt();
    }

    public long getClock() {
        return clock;
    }

    public abstract void createAndSendPacket(ByteBuffer byteBuffer, MediaCodec.BufferInfo bufferInfo);

    public void setPorts(int rtpPort, int rtcpPort) {
        this.rtpPort = rtpPort;
        this.rtcpPort = rtcpPort;
    }

    public void reset() {
        seq = 0;
        ssrc = new Random().nextInt();
    }

    protected byte[] getBuffer(int size) {
        byte[] buffer = new byte[size];
        buffer[0] = (byte) Integer.parseInt("10000000", 2);
        buffer[1] = (byte) RtpConstants.payloadType;

        setLongSSRC(buffer, ssrc);
        requestBuffer(buffer);
        return buffer;
    }

    protected void updateTimeStamp(byte[] buffer, long timestamp) {
        long ts = timestamp * clock / 1000000000L;
        setLong(buffer, ts, 4, 8);
    }

    protected void setLong(byte[] buffer, long n, int begin, int end) {
        for (end--; end >= begin; end--) {
            buffer[end] = (byte) (n % 256);
            n >>= 8;
        }
    }

    protected long updateSeq(byte[] buffer) {
        setLong(buffer, ++seq, 2, 4);
        return seq;
    }

    public long getSeq() {
        return seq;
    }

    protected void markPacket(byte[] buffer) {
        buffer[1] |= 0x80;//00001000
    }

    private void setLongSSRC(byte[] buffer, int ssrc) {
        setLong(buffer, ssrc, 8, 12);
    }

    private void requestBuffer(byte[] buffer) {
        buffer[1] &= 0x7F;
    }


    public static String bytesToHexString(byte[] src) {
        StringBuilder stringBuilder = new StringBuilder("");
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

    public static String intToHex(int i) {
        int v = i & 0xFF;
        String hv = Integer.toHexString(v);
        if (hv.length() < 2) {
            return "0" + hv;
        } else {
            return hv;
        }
    }
}
