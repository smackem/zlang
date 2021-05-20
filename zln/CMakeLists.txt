# prerequisites
cmake_minimum_required(VERSION 3.16)

# project definition with config header
project(zln LANGUAGES C VERSION 0.1)
configure_file(build_config.h.in build_config.h)

# settings
set(CMAKE_C_STANDARD 11)
set(CMAKE_VERBOSE_MAKEFILE on)

# child projects
add_subdirectory(util)
add_subdirectory(vm)

# reference libraries common to all targets
link_libraries(
        util
        vm
)

# add include directories for all targets
include_directories(
        "${PROJECT_BINARY_DIR}"
        "${PROJECT_SOURCE_DIR}/util"
        "${PROJECT_SOURCE_DIR}/vm"
)

# -------------------------------------
# main target
# -------------------------------------
add_executable(zln_executable main.c)

set_target_properties(zln_executable PROPERTIES OUTPUT_NAME "zln")

# -------------------------------------
# test target
# -------------------------------------
add_executable(zln_test test.c emit.c)


# -------------------------------------
# lib target
# -------------------------------------
add_library(zln SHARED lib.c)

message(STATUS ${CMAKE_SYSTEM_NAME})

if (${CMAKE_SYSTEM_NAME} MATCHES "Windows")
    set(JAVA_SYSTEM_INCLUDE_DIR win32)
else()
    string(TOLOWER ${CMAKE_SYSTEM_NAME} JAVA_SYSTEM_INCLUDE_DIR)
endif()

message(STATUS "system include: ${JAVA_SYSTEM_INCLUDE_DIR}")

target_include_directories(zln PUBLIC
        "$ENV{JAVA_HOME}/include"
        "$ENV{JAVA_HOME}/include/${JAVA_SYSTEM_INCLUDE_DIR}"
        )
