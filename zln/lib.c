//
// Created by smackem on 20.03.21.
//
#include <util.h>
#include <vm.h>
#include "zap.h"
#include "net_smackem_zlang_interpret_Zll.h"

JNIEXPORT jint JNICALL Java_net_smackem_zlang_interpret_Zll_executeProgram(JNIEnv *env_ptr, jclass cls, jobject buf) {
    JNIEnv env = *env_ptr;
    jlong buf_size = env->GetDirectBufferCapacity(env_ptr, buf);
    byte_t *bytes = env->GetDirectBufferAddress(env_ptr, buf);
    const ZapHeader *header = (ZapHeader *) bytes;
    if (is_valid_zap(header) == false) {
        return -1;
    }
    byte_t *program = &bytes[sizeof(ZapHeader)];
    int32_t heap_offset_to_program = execute_zap(header, program, buf_size - sizeof(ZapHeader), false);
    return (jint) (heap_offset_to_program + sizeof(ZapHeader));
}
