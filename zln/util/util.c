//
// Created by smackem on 20.03.21.
//

#include <stdarg.h>
#include <stdio.h>
#include "util.h"

void assert_that(bool condition, const char *format, ...) {
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

void assert_equal_i(int32_t actual, int32_t expected, const char *category) {
    assert_that(actual == expected, "%s: failed equality check. actual: %d|0x%x, expected: %d|0x%x", category, actual, actual, expected, expected);
}

void assert_equal_f(double actual, double expected, const char *category) {
    assert_that(actual == expected, "%s: failed equality check. actual: %lf, expected: %lf", category, actual, expected);
}

void assert_not_null(void *ptr, const char *category) {
    assert_that(ptr != NULL, "%s: value is null", category);
}
