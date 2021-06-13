//
// Created by smackem on 13.06.21.
//

#ifndef ZLN_ZAP_H
#define ZLN_ZAP_H

#include <util.h>

typedef struct zap_header {
    uint32_t prefix; // 'Z' | 'L' << 8 ' | << MAJOR_VERSION << 16 | MINOR_VERSION << 24
    uint32_t code_segment_size;
    uint32_t const_segment_size;
    uint32_t global_segment_size;
    uint32_t entry_point_address;
    int32_t register_count;
    int32_t max_stack_depth;
    uint32_t max_heap_size;
    uint32_t reserved[2];
} ZapHeader;

bool is_valid_zap(const ZapHeader *header);
int32_t execute_zap(const ZapHeader *header, byte_t *memory, uint32_t memory_size, bool debug);
#endif //ZLN_ZAP_H
