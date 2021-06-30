//
// Created by smackem on 13.06.21.
//

#include <vm.h>
#include "zap.h"

bool is_valid_zap(const ZapHeader *header) {
    return ((header->prefix & 0xff) == 'Z'
        && (header->prefix >> 8 & 0xff) == 'L'
        && (header->prefix >> 16 & 0xff) == BYTE_CODE_MAJOR_VERSION
        && (header->prefix >> 24 & 0xff) == BYTE_CODE_MINOR_VERSION);
}

int32_t execute_zap(const ZapHeader *header, byte_t *memory, uint32_t memory_size, bool debug) {
    const byte_t *code_segment = memory;
    uint32_t non_memory_size = header->code_segment_size;
    const RuntimeConfig config = {
            .register_count = header->register_count,
            .max_stack_depth = header->max_stack_depth,
            .debug_callback = debug ? dump_cpu : NULL,
    };
    const MemoryLayout layout = {
            .base = &memory[non_memory_size],
            .const_segment_size = header->const_segment_size,
            .global_segment_size = header->global_segment_size,
            .register_segment_size = config.register_count * config.max_stack_depth * sizeof(Register),
            .stack_frame_segment_size = config.max_stack_depth * sizeof(StackFrame),
            .total_size = memory_size - non_memory_size,
            .heap_size_limit = header->max_heap_size,
    };
    trace("code_size: %u\nconst_size: %u\nglob_size: %u\nentry_point_pc: 0x%x\nheap_offset: %u\ntotal_memory: %u\n",
          header->code_segment_size,
          header->const_segment_size,
          header->global_segment_size,
          header->entry_point_address,
          (uint32_t) (heap_segment_addr(&layout) - memory),
          layout.total_size);
#ifndef NDEBUG
    print_code(stdout, code_segment, header->code_segment_size);
#endif
    const FunctionMeta *entry_point = (FunctionMeta *) &layout.base[header->entry_point_address];
    execute(code_segment, entry_point, &layout, &config);
    trace("FINISH\n");
    fflush(stdout);
    return (int32_t) (heap_segment_addr(&layout) - memory);
}
