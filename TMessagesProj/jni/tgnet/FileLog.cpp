/*
 * This is the source code of tgnet library v. 1.1
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2015-2018.
 */

#include <stdio.h>
#include <stdarg.h>
#include <time.h>
#include "FileLog.h"
#include "ConnectionsManager.h"

#ifdef ANDROID
#include <android/log.h>
#endif

#ifdef DEBUG_VERSION
bool LOGS_ENABLED = true;
#else
bool LOGS_ENABLED = true;
#endif

#include "../integrity/include/rust.h"

bool REF_LOGS_ENABLED = false;

FileLog &FileLog::getInstance() {
    static FileLog instance;
    return instance;
}

FileLog::FileLog() {
    pthread_mutex_init(&mutex, NULL);
}

void FileLog::init(std::string path) {
    pthread_mutex_lock(&mutex);
    if (path.size() > 0 && logFile == nullptr) {
        logFile = fopen(path.c_str(), "w");
    }
    pthread_mutex_unlock(&mutex);
}

void FileLog::fatal(const char *message, ...) {
    if (!LOGS_ENABLED) {
        return;
    }
    char buf[256];

    va_list argptr;
    va_start(argptr, message);
    vsnprintf(buf, 256, message, argptr);
    loge("tgnet fatal", buf);
    va_end(argptr);
}

void FileLog::e(const char *message, ...) {
    if (!LOGS_ENABLED) {
        return;
    }
    char buf[256];

    va_list argptr;
    va_start(argptr, message);
    vsnprintf(buf, 256, message, argptr);
    loge("tgnet", buf);
    va_end(argptr);
}

void FileLog::w(const char *message, ...) {
    if (!LOGS_ENABLED) {
        return;
    }
    char buf[256];

    va_list argptr;
    va_start(argptr, message);
    vsnprintf(buf, 256, message, argptr);
    logw("tgnet", buf);
    va_end(argptr);
}

void FileLog::d(const char *message, ...) {
    if (!LOGS_ENABLED) {
        return;
    }
    char buf[256];

    va_list argptr;
    va_start(argptr, message);
    vsnprintf(buf, 256, message, argptr);
    logd("tgnet", buf);
    va_end(argptr);
}

static int refsCount = 0;

void FileLog::ref(const char *message, ...) {
    if (!REF_LOGS_ENABLED) {
        return;
    }
    va_list argptr;
    va_start(argptr, message);
    refsCount++;
#ifdef ANDROID
    std::ostringstream s;
    s << refsCount << " refs (+ref): " << message;
    __android_log_vprint(ANDROID_LOG_VERBOSE, "tgnetREF", s.str().c_str(), argptr);
    va_end(argptr);
    va_start(argptr, message);
#endif
    va_end(argptr);
}

void FileLog::delref(const char *message, ...) {
    if (!REF_LOGS_ENABLED) {
        return;
    }
    va_list argptr;
    va_start(argptr, message);
    refsCount--;
#ifdef ANDROID
    std::ostringstream s;
    s << refsCount << " refs (-ref): " << message;
    __android_log_vprint(ANDROID_LOG_VERBOSE, "tgnetREF", s.str().c_str(), argptr);
    va_end(argptr);
    va_start(argptr, message);
#endif
    va_end(argptr);
}
