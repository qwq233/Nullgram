// Copyright 2014 The Chromium Authors
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.


// This file is autogenerated by
//     third_party/jni_zero/jni_generator.py
// For
//     org/webrtc/RtcCertificatePem

#ifndef org_webrtc_RtcCertificatePem_JNI
#define org_webrtc_RtcCertificatePem_JNI

#include <jni.h>

#include "third_party/jni_zero/jni_export.h"
#include "webrtc/sdk/android/src/jni/jni_generator_helper.h"


// Step 1: Forward declarations.

JNI_ZERO_COMPONENT_BUILD_EXPORT extern const char kClassPath_org_webrtc_RtcCertificatePem[];
const char kClassPath_org_webrtc_RtcCertificatePem[] = "org/webrtc/RtcCertificatePem";
// Leaking this jclass as we cannot use LazyInstance from some threads.
JNI_ZERO_COMPONENT_BUILD_EXPORT std::atomic<jclass> g_org_webrtc_RtcCertificatePem_clazz(nullptr);
#ifndef org_webrtc_RtcCertificatePem_clazz_defined
#define org_webrtc_RtcCertificatePem_clazz_defined
inline jclass org_webrtc_RtcCertificatePem_clazz(JNIEnv* env) {
  return jni_zero::LazyGetClass(env, kClassPath_org_webrtc_RtcCertificatePem,
      &g_org_webrtc_RtcCertificatePem_clazz);
}
#endif


// Step 2: Constants (optional).


// Step 3: Method stubs.
namespace webrtc {
namespace jni {

static jni_zero::ScopedJavaLocalRef<jobject> JNI_RtcCertificatePem_GenerateCertificate(JNIEnv* env,
    const jni_zero::JavaParamRef<jobject>& keyType,
    jlong expires);

JNI_BOUNDARY_EXPORT jobject Java_org_webrtc_RtcCertificatePem_nativeGenerateCertificate(
    JNIEnv* env,
    jclass jcaller,
    jobject keyType,
    jlong expires) {
  return JNI_RtcCertificatePem_GenerateCertificate(env, jni_zero::JavaParamRef<jobject>(env,
      keyType), expires).Release();
}


static std::atomic<jmethodID> g_org_webrtc_RtcCertificatePem_Constructor2(nullptr);
static jni_zero::ScopedJavaLocalRef<jobject> Java_RtcCertificatePem_Constructor(JNIEnv* env, const
    jni_zero::JavaRef<jstring>& privateKey,
    const jni_zero::JavaRef<jstring>& certificate) {
  jclass clazz = org_webrtc_RtcCertificatePem_clazz(env);
  CHECK_CLAZZ(env, clazz,
      org_webrtc_RtcCertificatePem_clazz(env), nullptr);

  jni_zero::JniJavaCallContextChecked call_context;
  call_context.Init<
      jni_zero::MethodID::TYPE_INSTANCE>(
          env,
          clazz,
          "<init>",
          "(Ljava/lang/String;Ljava/lang/String;)V",
          &g_org_webrtc_RtcCertificatePem_Constructor2);

  jobject ret =
      env->NewObject(clazz,
          call_context.base.method_id, privateKey.obj(), certificate.obj());
  return jni_zero::ScopedJavaLocalRef<jobject>(env, ret);
}

static std::atomic<jmethodID> g_org_webrtc_RtcCertificatePem_getCertificate0(nullptr);
static jni_zero::ScopedJavaLocalRef<jstring> Java_RtcCertificatePem_getCertificate(JNIEnv* env,
    const jni_zero::JavaRef<jobject>& obj) {
  jclass clazz = org_webrtc_RtcCertificatePem_clazz(env);
  CHECK_CLAZZ(env, obj.obj(),
      org_webrtc_RtcCertificatePem_clazz(env), nullptr);

  jni_zero::JniJavaCallContextChecked call_context;
  call_context.Init<
      jni_zero::MethodID::TYPE_INSTANCE>(
          env,
          clazz,
          "getCertificate",
          "()Ljava/lang/String;",
          &g_org_webrtc_RtcCertificatePem_getCertificate0);

  jstring ret =
      static_cast<jstring>(env->CallObjectMethod(obj.obj(),
          call_context.base.method_id));
  return jni_zero::ScopedJavaLocalRef<jstring>(env, ret);
}

static std::atomic<jmethodID> g_org_webrtc_RtcCertificatePem_getPrivateKey0(nullptr);
static jni_zero::ScopedJavaLocalRef<jstring> Java_RtcCertificatePem_getPrivateKey(JNIEnv* env, const
    jni_zero::JavaRef<jobject>& obj) {
  jclass clazz = org_webrtc_RtcCertificatePem_clazz(env);
  CHECK_CLAZZ(env, obj.obj(),
      org_webrtc_RtcCertificatePem_clazz(env), nullptr);

  jni_zero::JniJavaCallContextChecked call_context;
  call_context.Init<
      jni_zero::MethodID::TYPE_INSTANCE>(
          env,
          clazz,
          "getPrivateKey",
          "()Ljava/lang/String;",
          &g_org_webrtc_RtcCertificatePem_getPrivateKey0);

  jstring ret =
      static_cast<jstring>(env->CallObjectMethod(obj.obj(),
          call_context.base.method_id));
  return jni_zero::ScopedJavaLocalRef<jstring>(env, ret);
}

}  // namespace jni
}  // namespace webrtc

#endif  // org_webrtc_RtcCertificatePem_JNI
