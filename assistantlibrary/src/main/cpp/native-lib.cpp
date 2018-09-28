/* ************************************************************************************************
 * Copyright 2018 Qualcomm Technologies International, Ltd.                                       *
 **************************************************************************************************/

#include <jni.h>
#include "sbc/csr_sbc_api.h"
#include "sbc/csr_sbc.h"

extern "C"
JNIEXPORT jlong
JNICALL
Java_com_qualcomm_qti_libraries_assistant_AssistantManager_open(JNIEnv *env, jobject ) {
    void *handle = CsrSbcOpen();

    CsrSbcInitDecoder(handle);

    return (jlong)handle;
}

extern "C"
JNIEXPORT void
JNICALL
Java_com_qualcomm_qti_libraries_assistant_AssistantManager_close(JNIEnv *env, jobject, jlong handle ) {
    CsrSbcClose((void **)handle);

    return ;
}

jbyteArray Java_com_qualcomm_qti_libraries_assistant_sbctestorama_shortToByte(JNIEnv *env, jshortArray input)  {
    jshort *input_elements = (env)->GetShortArrayElements(input, 0);
    int input_length = (env)->GetArrayLength(input);
    jbyteArray output = (jbyteArray) ((env)->NewByteArray(input_length * 2));
    jbyte *output_elements = (env)->GetByteArrayElements(output, 0);

    memcpy(output_elements, input_elements, input_length * 2);

    (env)->ReleaseShortArrayElements(input, input_elements, JNI_ABORT);
    (env)->ReleaseByteArrayElements(output, output_elements, 0);

    return output;
}

extern "C"
JNIEXPORT jbyteArray
JNICALL
Java_com_qualcomm_qti_libraries_assistant_AssistantManager_decode(JNIEnv *env, jobject, jlong handle, jbyteArray frame) {
    uint8_t *array = (uint8_t *)env->GetByteArrayElements(frame, 0);
    CsrInt16 decoded_sample[16][2][8];

    /* Use the SbcHandle to lookup information about the frame */
    SbcHandle_t *hdl = (SbcHandle_t *)handle;

    /* Read the frame header which computes the size of the complete frame. */
    CsrUint16 frame_size = CsrSbcReadHeader((void **)handle, array);
    CsrUint16 audio_length = 0;

    if (frame_size == 0) {
        jbyteArray result = (env)->NewByteArray(audio_length);
        return result;
    }

    /* Decode the frame into decoded_sample. */
    CsrSbcDecode((void **)handle, array, decoded_sample);

    /* Restructure the data into chronological order. */
    CsrUint8 b, sb, ch = 0;
    short pcm[frame_size * 2];

    for (b = 0; b < hdl->sbc->blocks; b++) {
        for (sb = 0; sb < hdl->sbc->subbands; sb++) {
            for (ch = 0; ch < hdl->sbc->channels; ch++) {
                pcm[audio_length] = decoded_sample[b][ch][sb];
                audio_length++;
            }
        }
    }

    /* Move the audio data into a JNI short array. */
    jshortArray result = (env)->NewShortArray(audio_length);
    (env)->SetShortArrayRegion(result, 0, audio_length, pcm);

    /* Change the data from a short array to a byte array */
    jbyteArray byteArray = Java_com_qualcomm_qti_libraries_assistant_sbctestorama_shortToByte(env, result);

    return byteArray;
}
