//
// Created by qwq233 on 2/24/2023.
//

#include "v2sign.h"

#include <jni.h>
#include <regex>
#include <cstring>
#include <fstream>
#include <android/log.h>
#include "nullmd5.h"
#include "../crashlytics.h"
#include "../log.h"

#define GITHUB_SIGNATURE 79F5947F1AC75D23F509DDC97A749DC7
#define PLAYSTORE_SIGNATURE 999014B8010E81DC52825616228ECEB9

#define __STRING(x) #x
#define STRING(x) __STRING(x)
namespace {

const uint64_t revertMagikFirst = 0x3234206b636f6c42L;
const uint64_t revertMagikSecond = 0x20676953204b5041L;
const uint32_t v2Id = 0x7109871a;

std::string getAppPath(JNIEnv *env) {
    jclass cMainHook = env->FindClass("org/telegram/messenger/ApplicationLoader");
    jclass cClass = env->FindClass("java/lang/Class");
    jmethodID mGetClassLoader = env->GetMethodID(cClass, "getClassLoader",
                                                 "()Ljava/lang/ClassLoader;");
    jobject classloader = env->CallObjectMethod(cMainHook, mGetClassLoader);
    jclass cClassloader = env->FindClass("java/lang/ClassLoader");
    jmethodID mGetResource = env->GetMethodID(cClassloader, "findResource",
                                              "(Ljava/lang/String;)Ljava/net/URL;");
    jstring manifestPath = env->NewStringUTF("AndroidManifest.xml");
    jobject url = env->CallObjectMethod(classloader, mGetResource, manifestPath);
    jclass cURL = env->FindClass("java/net/URL");
    jmethodID mGetPath = env->GetMethodID(cURL, "getPath", "()Ljava/lang/String;");
    auto file = (jstring) env->CallObjectMethod(url, mGetPath);
    const char *cStr = env->GetStringUTFChars(file, nullptr);
    std::string filePathStr(cStr);
    if (filePathStr.empty()) {
        return std::string();
    }
    std::string s = filePathStr.substr(5, filePathStr.size() - 26);
    LOGD("module path -> %s", s.c_str());
    env->ReleaseStringUTFChars(file, cStr);
    return s;
}

std::string getSignBlock(const std::string &path) {
    std::ifstream f(path);
    std::string file((std::istreambuf_iterator<char>(f)), std::istreambuf_iterator<char>());
    uint64_t curr = 0;
    const char *base = file.c_str();
    const char *ptr = base + file.size() - 1;
    std::string signBlock;
    while (ptr >= base) {
        curr = (curr << 8) | *ptr--;
        if (curr == revertMagikFirst) {
            uint64_t tmp = 0;
            for (int i = 0; i < 8; ++i) {
                tmp = (tmp << 8) | *(ptr - i);
            }
            if (tmp == revertMagikSecond) {
                for (int i = 8; i < 16; ++i) {
                    tmp = (tmp << 8) | *(ptr - i);
                }
                // TODO 只判断魔数“APK Sig Block 42”可能存在误判
                ptr -= 16;
                tmp -= 24;
                for (uint64_t i = 0; i < tmp; ++i) {
                    signBlock.push_back(*ptr--);
                }
                break;
            }
        }
    }
    std::reverse(signBlock.begin(), signBlock.end());
    return signBlock;
}

std::string getBlockMd5(const std::string &block) {
    return MD5(block).getDigest();
}

std::string getV2Signature(const std::string &block) {
    std::string signature;
    const char *p = block.c_str();
    const char *last = block.c_str() + block.size();
    while (p < last) {
        uint64_t blockSize = 0;
        for (int i = 0; i < 8; ++i) {
            blockSize = (blockSize >> 8) | (((uint64_t) *p++) << 56);
        }
        uint32_t id = 0;
        for (int i = 0; i < 4; ++i) {
            id = (id >> 8) | (((uint32_t) *p++) << 24);
        }
        if (id != v2Id) {
            p += blockSize - 12;
            continue;
        }
        p += 12;
        uint32_t size = 0;
        for (int i = 0; i < 4; ++i) {
            size = (size >> 8) | (((uint32_t) *p++) << 24);
        }
        p += size + 4;
        for (int i = 0; i < 4; ++i) {
            size = (size >> 8) | (((uint32_t) *p++) << 24);
        }
        for (uint32_t i = 0; i < size; ++i) {
            signature.push_back(*p++);
        }
        break;
    }
    return signature;
}

extern "C" bool checkSignature(JNIEnv *env) {
    firebase::crashlytics::Initialize();
    std::string path = getAppPath(env);
    if (path.empty()) {
        return false;
    }
    std::string block = getSignBlock(path);
    if (block.empty()) {
        return false;
    }

    std::string currSignature = getV2Signature(block);
    std::string md5 = getBlockMd5(currSignature);
    std::string githubSignature(STRING(GITHUB_SIGNATURE));
    std::string playStoreSignature(STRING(PLAYSTORE_SIGNATURE));
    if (githubSignature == md5) {
        LOGD("checkSignature: Match Github Signature");
        firebase::crashlytics::SetCustomKey("signature", "github");
        firebase::crashlytics::Log("Match Github Signature");
        return true;
    } else if (playStoreSignature == md5) {
        LOGD("checkSignature: Match Google Play Signature");
        firebase::crashlytics::Log("Match Google Play Signature");
        firebase::crashlytics::SetCustomKey("signature", "play");
        return true;
    } else {
        LOGD("checkSignature: Not Match Signature");
        firebase::crashlytics::Log("Not Match Signature");
        firebase::crashlytics::SetCustomKey("signature", "verify failed");
        return false;
    }

}
}
