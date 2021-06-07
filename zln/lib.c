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
    int32_t register_count;
    int32_t max_stack_depth;
    uint32_t max_heap_size;
    uint32_t reserved[2];
} ZLHeader;

JNIEXPORT jint JNICALL Java_net_smackem_zlang_interpret_Zln_executeProgram(JNIEnv *env_ptr, jclass cls, jobject buf) {
    JNIEnv env = *env_ptr;
    jlong buf_size = env->GetDirectBufferCapacity(env_ptr, buf);
    byte_t *bytes = env->GetDirectBufferAddress(env_ptr, buf);
    const ZLHeader *header = (ZLHeader *) bytes;
    const byte_t *code_segment = &bytes[sizeof(ZLHeader)];
    uint32_t non_memory_size = sizeof(ZLHeader) + header->code_segment_size;
    const RuntimeConfig config = {
            .register_count = header->register_count,
            .max_stack_depth = header->max_stack_depth,
            .debug_callback = dump_cpu,
    };
    const MemoryLayout memory = {
            .base = &bytes[non_memory_size],
            .const_segment_size = header->const_segment_size,
            .global_segment_size = header->global_segment_size,
            .register_segment_size = config.register_count * config.max_stack_depth * sizeof(Register),
            .stack_frame_segment_size = config.max_stack_depth * sizeof(StackFrame),
            .total_size = buf_size - non_memory_size,
    };
    trace("code_size: %u\nconst_size: %u\nglob_size: %u\nentry_point_pc: %u\nheap_offset: %u\ntotal_memory: %u\n",
          header->code_segment_size,
          header->const_segment_size,
          header->global_segment_size,
          header->entry_point_address,
          (uint32_t) (heap_segment_addr(&memory) - bytes),
          memory.total_size);
#ifndef NDEBUG
    print_code(stdout, code_segment, header->code_segment_size);
#endif
    const FunctionMeta *entry_point = (FunctionMeta *) &memory.base[header->entry_point_address];
    execute(code_segment, entry_point, &memory, &config);
    trace("FINISH\n");
    fflush(stdout);
    return (jint) (heap_segment_addr(&memory) - bytes);
}
