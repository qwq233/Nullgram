//
// Created by qwq233 on 2/24/2023.
//

#include "v2sign.h"

#include <jni.h>
#include <regex>
#include <cstring>
#include <string>
#include <string_view>

#include <unistd.h>
#include <errno.h>
#include <dirent.h>

#include <linux_syscall_support.h>

#include <android/log.h>
#include "../crashlytics.h"
#include "../log.h"

namespace {
extern "C" bool checkSignature(JavaVM *vm) {
    bool match = false;
    uint8_t result = verify_signature(vm);
    if (result == 1) {
        LOGD("checkSignature: Match Github Signature");
        firebase::crashlytics::SetCustomKey("signature", "github");
        firebase::crashlytics::Log("Match Github Signature");
        match = true;
    } else if (result == 2) {
        LOGD("checkSignature: Match Google Play Signature");
        firebase::crashlytics::Log("Match Google Play Signature");
        firebase::crashlytics::SetCustomKey("signature", "play");
        match = true;
    } else {
        LOGD("checkSignature: Not Match Signature");
        firebase::crashlytics::Log("Not Match Signature");
        firebase::crashlytics::SetCustomKey("signature", "verify failed");
        match = false;
    }

    if (!match) {
        LOGD("checkSignature: kill itself");
        sys_kill(sys_getpid(), SIGKILL);
    }
    return match;
}
}

#undef __STRING
#undef STRING
