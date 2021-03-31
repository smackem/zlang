//
// Created by smackem on 26.03.21.
//

#include <util.h>
#include "types.h"

size_t sizeof_type(Type type) {
    switch (type) {
        case TYPE_Int32:
            return sizeof(int32_t);
        case TYPE_Ref:
        case TYPE_String:
            return sizeof(addr_t);
        case TYPE_Unsigned8:
            return sizeof(byte_t);
        case TYPE_Float64:
            return sizeof(double);
        case TYPE_NativePtr:
            return sizeof(intptr_t);
        default:
            assert_that(false, "unsupported type %d", type);
            return 0;
    }
}
