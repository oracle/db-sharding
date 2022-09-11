/*
** Oracle Sharding Tools Library
**
** Copyright © 2017 Oracle and/or its affiliates. All rights reserved.
** Licensed under the Universal Permissive License v 1.0 as shown at 
**   http://oss.oracle.com/licenses/upl 
*/

#ifndef JAVAUTILS_HPP
#define JAVAUTILS_HPP

#include <string>
#include <jni.h>

#define DECLARE_SELF(TYPE, ENV, OBJ) \
TYPE * self = TYPE::jniDesc.getSelf(ENV, OBJ);

#define JAVA_BLOCK_BEGIN(ENV) try {
#define JAVA_BLOCK_END(ENV) } catch (std::exception & ex) { \
    (ENV)->ThrowNew((ENV)->FindClass("java/lang/RuntimeException"), ex.what()); return; }

#define JAVA_BLOCK_ENDR(ENV, RR) } catch (std::exception & ex) { \
    (ENV)->ThrowNew((ENV)->FindClass("java/lang/RuntimeException"), ex.what()); return RR; }

template <typename T>
struct JavaClassDescriptor
{
  jclass   classId;
  jfieldID nativePointerId;

  T * getSelf(JNIEnv *env, jobject obj)
  {
    JAVA_BLOCK_BEGIN(env)
    jlong handle = env->GetLongField(obj, nativePointerId);
    return reinterpret_cast<T *>(handle);
    JAVA_BLOCK_ENDR(env, nullptr)
  }

  void setHandle(JNIEnv *env, jobject obj, T * nativeObject)
  {
    JAVA_BLOCK_BEGIN(env)
    jlong handle = reinterpret_cast<jlong>(nativeObject);
    env->SetLongField(obj, nativePointerId, handle);
    JAVA_BLOCK_END(env)
  }

  void initialize(JNIEnv *env, jclass cls, const char * nativePointerField)
  {
//    rteClass = env->FindClass("java/lang/RuntimeException");
    JAVA_BLOCK_BEGIN(env)
    classId = cls;
    nativePointerId = env->GetFieldID(cls, nativePointerField, "J");
    JAVA_BLOCK_END(env)
  }

  void dispose(JNIEnv* env, jobject _obj)
  {
    JAVA_BLOCK_BEGIN(env)
    T * self = getSelf(env, _obj);

    if (self != nullptr) {
      delete self;
      setHandle(env, _obj, nullptr);
    }

    JAVA_BLOCK_END(env)
  }
};

namespace jnihelpers {
static
std::string getJavaString(JNIEnv* env, jstring str)
{
  if (str == nullptr) { return std::string(); }

  const char * characters = env->GetStringUTFChars(str, nullptr);
  int len = env->GetStringUTFLength(str);
  std::string result(characters, len);
  env->ReleaseStringUTFChars(str, characters);

  return result;
}

static
std::string getJavaString(JNIEnv* env, jbyteArray str)
{
  int len = env->GetArrayLength(str);
  void * data = env->GetPrimitiveArrayCritical(str, nullptr);
  std::string result((const char *) data, len);
  env->ReleasePrimitiveArrayCritical(str, data, JNI_ABORT);

  return result;
}
}

#endif /* JAVAUTILS_HPP */
