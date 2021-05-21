//
// Created by smackem on 20.03.21.
//

#ifndef ZLN_UTIL_H
#define ZLN_UTIL_H

#include <stdint.h>
#include <stdbool.h>
#include <stdlib.h>
#include <string.h>
#include <stdio.h>

#ifdef NDEBUG
#define INLINE inline
#define trace (void)
#define ftrace (void)
#else
#define INLINE
#define trace printf
#define ftrace fprintf
#endif

#define zero_memory(b, len) memset(b, 0, len)

void assert_that(bool condition, const char *format, ...);

#define assert_equal(actual, expected, category) \
    _Generic((actual)+(expected), default: assert_equal_i, double: assert_equal_f)(actual, expected, category)
void assert_equal_i(int32_t actual, int32_t expected, const char *category);
void assert_equal_f(double actual, double expected, const char *category);

void assert_not_null(void *ptr, const char *category);

#ifndef min
#define min(a, b) ((a) < (b) ? (a) : (b))
#endif
#ifndef max
#define max(a, b) ((a) > (b) ? (a) : (b))
#endif

#endif //ZLN_UTIL_H
