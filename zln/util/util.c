//
// Created by smackem on 20.03.21.
//

#include <stdarg.h>
#include <stdio.h>
#include "util.h"

void assert(bool condition, const char *format, ...) {
    va_list arg_ptr;
    va_start(arg_ptr, format);
    if (condition == false) {
        fprintf(stderr, "runtime assertion failed: ");
        vfprintf(stderr, format, arg_ptr);
        putchar('\n');
        exit(1);
    }
    va_end(arg_ptr);
}
