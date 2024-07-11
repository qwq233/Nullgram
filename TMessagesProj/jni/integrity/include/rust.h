//
// Created by qwq233 on 3/24/2024.
//

#include <cstdint>

extern "C" uint8_t logd(const char *tag, const char *msg);
extern "C" uint8_t logi(const char *tag, const char *msg);
extern "C" uint8_t logw(const char *tag, const char *msg);
extern "C" uint8_t loge(const char *tag, const char *msg);
