add_library(checkSignature STATIC
        integrity/v2sign.cc
        integrity/nullmd5.cc)
target_compile_options(checkSignature PUBLIC
        -Oz -Wall -Wextra -Wshadow -fvisibility=hidden)
target_include_directories(checkSignature PUBLIC
        integrity)
target_link_libraries(checkSignature -Wl)
