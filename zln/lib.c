//
// Created by smackem on 20.03.21.
//
#include <util.h>
#include <vm.h>
#include "net_smackem_zlang_interpret_Zln.h"

typedef struct zl_header {
    uint32_t prefix; // 'Z' | 'L' << 8 ' | << MAJOR_VERSION << 16 | MINOR_VERSION << 24
    uint32_t code_segment_size;
    uint32_t const_segment_size;
    uint32_t global_segment_size;
    uint32_t entry_point_address;
} ZLHeader;

JNIEXPORT jint JNICALL Java_net_smackem_zlang_interpret_Zln_executeProgram(JNIEnv *env_ptr, jclass cls, jobject buf) {
    assert_equal(sizeof(ZLHeader), 20, "header size");
    JNIEnv env = *env_ptr;
    jlong buf_size = env->GetDirectBufferCapacity(env_ptr, buf);
    byte_t *bytes = env->GetDirectBufferAddress(env_ptr, buf);
    const ZLHeader *header = (ZLHeader *) bytes;
    const byte_t *code_segment = &bytes[sizeof(ZLHeader)];
    uint32_t non_memory_size = sizeof(ZLHeader) + header->code_segment_size;
    const MemoryLayout memory = {
            .base = &bytes[non_memory_size],
            .const_segment_size = header->const_segment_size,
            .global_segment_size = header->global_segment_size,
            .total_size = buf_size - non_memory_size,
    };
    fprintf(stdout, "code_size: %u\nconst_size: %u\nglob_size: %u\nentry_point:%u\n",
            header->code_segment_size,
            header->const_segment_size,
            header->global_segment_size,
            header->entry_point_address);
    const RuntimeConfig config = {
            .register_count = 8,
            .max_stack_depth = 16,
            .debug_callback = dump_cpu,
    };
#ifndef NDEBUG
    print_code(stdout, code_segment, header->code_segment_size);
#endif
    const FunctionMeta *entry_point = (FunctionMeta *) &memory.base[header->entry_point_address];
    execute(code_segment, entry_point, &memory, &config);
    printf("FINISH\n");
    fflush(stdout);
    return 0;
}
