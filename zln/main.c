#include <stdio.h>
#include <build_config.h>
#include "emit.h"

int main() {
    byte_t code[1024];
    HeapLayout heap;
    RuntimeConfig config = {4, 1};
    printf("zln v%d.%d\n", zln_VERSION_MAJOR, zln_VERSION_MINOR);
    size_t code_size = emit_test_program(code, sizeof(code), &heap);
    printf("code_size: %lu\n", code_size);
    execute(code, 0, 0, &heap, &config);
    print_globals(&heap);
    return 0;
}
