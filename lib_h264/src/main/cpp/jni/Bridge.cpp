//
// Created by LN on 2021/3/11.
//

#include "Bridege.h"
#include <jni.h>
#include <malloc.h>
#include <string.h>


#ifdef __cplusplus
extern "C" {
#include "H264Pack.h"
#endif
#ifdef __cplusplus
}
#endif

static jmethodID jOnCallbackMid = NULL;
static jobject jCaller = NULL;
static JNIEnv *jCallerCurrentEnv = NULL;

extern "C"
JNIEXPORT void JNICALL
Java_com_pcyfox_h264_H264HandlerNative_init(JNIEnv *env, jobject thiz, jboolean is_debug) {
}


unsigned char *ByteArrayToChars(JNIEnv *env, jbyteArray bytes, int len) {
    jbyte *data = (env->GetByteArrayElements(bytes, JNI_FALSE));
    auto *dataCopy = (unsigned char *) calloc(len, sizeof(char));
    memcpy(dataCopy, data, len);
    env->ReleaseByteArrayElements(bytes, data, JNI_FALSE);
    return dataCopy;
}


jbyteArray CharsToArray(JNIEnv *env, unsigned char *chars, unsigned int length) {
    jbyteArray byteArray = env->NewByteArray(length);
    env->SetByteArrayRegion(byteArray, 0, length,
                            reinterpret_cast<const jbyte *>(chars));
    return byteArray;
}


void RTPCallback(Result result) {
    jbyteArray byteArray = CharsToArray(jCallerCurrentEnv, result->data, result->length);
    free(result->data);
    free(result);
    result = NULL;
    jCallerCurrentEnv->CallVoidMethod(jCaller, jOnCallbackMid, byteArray);
}

extern "C"
JNIEXPORT void JNICALL

Java_com_pcyfox_h264_H264HandlerNative_updateSPS_1PPS(JNIEnv
                                                      *env,
                                                      jobject thiz, jbyteArray
                                                      sps,
                                                      jint sps_len, jbyteArray
                                                      pps,
                                                      jint pps_len
) {
    unsigned char *spsChars = ByteArrayToChars(env, sps, sps_len);
    unsigned char *ppsChars = ByteArrayToChars(env, pps, pps_len);
    UpdateSPS_PPS(spsChars, sps_len, ppsChars, pps_len);
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_pcyfox_h264_H264HandlerNative_packH264ToRTP(JNIEnv *env, jobject thiz,
                                                     jbyteArray h264_pkt,
                                                     jint
                                                     length,
                                                     jint max_pkt_len, jlong
                                                     ts,
                                                     jlong clock,
                                                     jboolean isAutoPackSPS_PPS,
                                                     jobject callback) {

    if (jOnCallbackMid == NULL) {
        jCallerCurrentEnv = env;
        jCaller = env->NewGlobalRef(callback);
        jclass jCallback = env->GetObjectClass(callback);
        jOnCallbackMid = env->GetMethodID(jCallback, "onCallback", "([B)V");
    }

    return PackRTP(ByteArrayToChars(env, h264_pkt, length), length, max_pkt_len, ts, clock,
                   isAutoPackSPS_PPS,
                   RTPCallback);

}


extern "C"
JNIEXPORT jbyteArray JNICALL
Java_com_pcyfox_h264_H264HandlerNative_getSPS_1PPS_1RTP_1Pkt(JNIEnv *env, jobject thiz, jlong ts,
                                                             jlong clock) {
    Result result = GetSPS_PPS_RTP_STAP_Pkt(ts, clock);
    if (result == NULL) {
        return NULL;
    }
    FreeResult(result);
    return CharsToArray(env, result->data, result->length);
}
