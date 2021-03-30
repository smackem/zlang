//
// Created by smackem on 20.03.21.
//

#ifndef ZLN_UTIL_H
#define ZLN_UTIL_H

#include <stdint.h>
#include <stdbool.h>
#include <stdlib.h>

#ifdef NDEBUG
#define NDEBUG_INLINE inline
#else
#define INLINE
#endif

void assert_that(bool condition, const char *format, ...);

#define assert_equal(actual, expected, category) \
    _Generic((actual)+(expected), default: assert_equal_i, double: assert_equal_f)(actual, expected, category)
void assert_equal_i(int32_t actual, int32_t expected, const char *category);
void assert_equal_f(double actual, double expected, const char *category);

void assert_not_null(void *ptr, const char *category);

#endif //ZLN_UTIL_H
