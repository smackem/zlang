#include <stdio.h>
#include <util.h>
#include <build_config.h>

int main() {
    printf("zln v%d.%d\n", zln_VERSION_MAJOR, zln_VERSION_MINOR);
    printf("Hello, World %d!\n", util_function(100));
    return 0;
}
