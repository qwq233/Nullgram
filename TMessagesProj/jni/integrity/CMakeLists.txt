add_library(checkSignature STATIC
        integrity/v2sign.cc)
target_compile_options(checkSignature PUBLIC
        -Oz -Wall -Wextra -Wshadow -fvisibility=hidden)
target_include_directories(checkSignature PUBLIC
        integrity
        ../../libs/linux-syscall-support)
target_link_libraries(checkSignature
    rust)
