//
// Created by Philip Boger on 22.03.21.
//

#ifndef ZLN_EMIT_H
#define ZLN_EMIT_H

#include <runtime.h>

size_t emit_test_program(byte_t *code, size_t code_size, HeapLayout *heap);

void print_globals(const HeapLayout *heap);

#endif //ZLN_EMIT_H
