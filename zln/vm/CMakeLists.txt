add_library(vm vm.c cpu.c heap.c types.c)

include_directories(
        "${PROJECT_SOURCE_DIR}/util"
)
