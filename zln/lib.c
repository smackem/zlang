//
// Created by smackem on 20.03.21.
//

#include <util.h>
#include <types.h>
#include <stdio.h>
#include "net_smackem_zlang_interpret_Zln.h"

typedef struct zl_header {
    uint32_t prefix; // 'Z' | 'L' << 8 ' | << MAJOR_VERSION << 16 | MINOR_VERSION << 24
    uint32_t entry_point_base_pc;
    uint32_t entry_point_pc;
    uint32_t const_segment_size;
    uint32_t global_segment_size;
} ZLHeader;


JNIEXPORT jbyteArray JNICALL Java_net_smackem_zlang_interpret_Zln_executeProgram
        (JNIEnv *env_ptr, jclass cls, jobject buf) {
    JNIEnv env = *env_ptr;
    byte_t *bytes = env->GetDirectBufferAddress(env_ptr, buf);
    const ZLHeader *header = (ZLHeader *) bytes;
    const byte_t *const_segment = &bytes[sizeof(ZLHeader)];
    byte_t *global_segment = &bytes[sizeof(ZLHeader) + header->const_segment_size];
    const byte_t *code_segment = &bytes[sizeof(ZLHeader) + header->const_segment_size + header->global_segment_size];
}


void library_function() {
    printf("Hello from library!\n");
}
