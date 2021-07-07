#include <stdio.h>
#include <build_config.h>
#include <callstack.h>
#include "zap.h"

#define DEFAULT_HEAP_SIZE (64 * 1024)

int main(int argc, char **argv) {
    printf("zl v%d.%d\n", zln_VERSION_MAJOR, zln_VERSION_MINOR);
    if (argc < 2) {
        printf("USAGE: zl <ZapFile>\n");
        return 0;
    }

    setbuf(stdout, NULL);
    FILE *file = fopen(argv[1], "rb");
    ZapHeader header;
    byte_t *memory = NULL;
    do {
        if (file == NULL) {
            printf("error opening '%s'\n", argv[1]);
            break;
        }
        if (fread(&header, sizeof(header), 1, file) < 1) {
            printf("error reading header from '%s'\n", argv[1]);
            break;
        }
        if (is_valid_zap(&header) == false) {
            printf("invalid zap header!\n");
            break;
        }
        uint32_t file_size = header.code_segment_size + header.const_segment_size;
        uint32_t memory_size = header.code_segment_size
                + header.const_segment_size
                + header.global_segment_size
                + header.register_count * header.max_stack_depth * sizeof(Register)
                + header.max_stack_depth * sizeof(StackFrame)
                + (header.max_heap_size > 0 ? header.max_heap_size : DEFAULT_HEAP_SIZE);
        memory = calloc(memory_size, 1);
        if (memory == NULL) {
            printf("out of memory!\n");
            break;
        }
        if (fread(memory, 1, file_size, file) < file_size) {
            printf("i/o error while reading from zap file!\n");
            break;
        }
        execute_zap(&header, memory, memory_size, false);//true);
    } while (false);

    if (file != NULL) {
        fclose(file);
    }
    if (memory != NULL) {
        free(memory);
    }
    return 0;
}
