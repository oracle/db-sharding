/*
** Oracle Sharding Tools Library
**
** Copyright © 2017 Oracle and/or its affiliates. All rights reserved.
** Licensed under the Universal Permissive License v 1.0 as shown at 
**   http://oss.oracle.com/licenses/upl 
*/

#include <memory>

#include <ociwrapper/ociwrapper.hpp>
#include <helpers/javautils.hpp>
#include <helpers/fixedbuffer.hpp>
#include "oracle_sharding_tools_OCIDirectPath.h"

struct JavaDirectPathAPI
{
  static JavaClassDescriptor<JavaDirectPathAPI> jniDesc;

  DynamicBuffer buffer;
  char * savedBuffer = nullptr;
  int currentRow = 0;
  int maxRowCount = 100*1024;
  bool closed = true;
  bool initialized = false;

  oci::StandaloneConnection connection;
  std::unique_ptr<oci::DirectPathInsert> dp;

  void connect(const std::string & a_connect_string, const std::string & a_username, const std::string & a_password) 
  {
    connection.dbConnectString = a_connect_string;
    connection.username = a_username;
    connection.connect(a_password);

    open();
  }

  void open()
  {
    if (closed)
    {
      dp.reset(new oci::DirectPathInsert(connection));
      dp->init();
      closed = false;
    }
  }

  void close()
  {
    reset();
    connection.disconnect();
  }

  void reset()
  {
    dp->reset();
    buffer.reset();
    currentRow = 0;
  }

  void next()
  {
    ++currentRow;

    if (currentRow == maxRowCount) {
        dp->load(currentRow, 0);
        dp->save(OCI_DIRPATH_DATASAVE_SAVEONLY);
        reset();
    }
  }

  void finish()
  {
    if (!closed) 
    {
      dp->load(currentRow, 0);
      dp->save(OCI_DIRPATH_DATASAVE_FINISH);
      connection.commit();
      closed = true;
    }

    discard();
  }

  void discard()
  {
    dp.reset(); /* NOTE: this is not a DPI rteset, this is a complete disposal of the buffer */
    buffer.reset();
    closed = true;
  }

  void begin()
  {
    dp->initStream(maxRowCount);
  }

  void reopen()
  {
    if (!closed) {
      finish();
    }

    open();
  }
};

JavaClassDescriptor<JavaDirectPathAPI> JavaDirectPathAPI::jniDesc;

static inline JavaDirectPathAPI * self(JNIEnv * env, jobject obj) { 
  return JavaDirectPathAPI::jniDesc.getSelf(env, obj); 
}

static jclass RuntimeExceptionClass;

using namespace jnihelpers;

void Java_oracle_sharding_tools_OCIDirectPath_initialize(JNIEnv * env, jclass cls)
{
  JavaDirectPathAPI::jniDesc.initialize(env, cls, "nativeObjectAddress");
  RuntimeExceptionClass = env->FindClass("java/lang/RuntimeException");
}

void Java_oracle_sharding_tools_OCIDirectPath_createInternal(JNIEnv * env, jobject obj)
{
  DECLARE_SELF(JavaDirectPathAPI, env, obj)

  if (self != nullptr) { 
    env->ThrowNew(RuntimeExceptionClass, "Object already created");
  }

  JAVA_BLOCK_BEGIN(env)
  JavaDirectPathAPI::jniDesc.setHandle(env, obj, new JavaDirectPathAPI());
  JAVA_BLOCK_END(env)
}

void Java_oracle_sharding_tools_OCIDirectPath_connectInternal(JNIEnv* env, jobject obj,
    jstring connect, jstring username, jbyteArray password)
{
  JAVA_BLOCK_BEGIN(env)

  self(env, obj)->connect(
    getJavaString(env, connect),
    getJavaString(env, username),
    getJavaString(env, password));

  JAVA_BLOCK_END(env)
}

void Java_oracle_sharding_tools_OCIDirectPath_closeInternal(JNIEnv * env, jobject obj)
{
  JAVA_BLOCK_BEGIN(env)
  DECLARE_SELF(JavaDirectPathAPI, env, obj)

  if (nullptr != self) {
    self->close();
  }

  JavaDirectPathAPI::jniDesc.dispose(env, obj);

  JAVA_BLOCK_END(env)
}

void Java_oracle_sharding_tools_OCIDirectPath_setTarget(JNIEnv* env, jobject obj, jstring schema, jstring table, jstring partition)
{
  JAVA_BLOCK_BEGIN(env)

  self(env, obj)->dp->setSink(
    getJavaString(env, schema),
    getJavaString(env, table),
    getJavaString(env, partition));

  JAVA_BLOCK_END(env)
}

void Java_oracle_sharding_tools_OCIDirectPath_addColumnDefinition(JNIEnv*env, jobject obj, jstring columnName, jint dty, jint size)
{
  JAVA_BLOCK_BEGIN(env)

  self(env, obj)->dp->column(getJavaString(env, columnName), (ub2) dty, (int) size);

  JAVA_BLOCK_END(env)
}

void Java_oracle_sharding_tools_OCIDirectPath_nextRow(JNIEnv*env, jobject obj)
{
  JAVA_BLOCK_BEGIN(env)

  self(env, obj)->next();

  JAVA_BLOCK_END(env)
}

void Java_oracle_sharding_tools_OCIDirectPath_finish(JNIEnv* env, jobject obj)
{
  JAVA_BLOCK_BEGIN(env)

  self(env, obj)->finish();

  JAVA_BLOCK_END(env)
}

void Java_oracle_sharding_tools_OCIDirectPath_reopen(JNIEnv* env, jobject obj)
{
  JAVA_BLOCK_BEGIN(env)

  self(env, obj)->reopen();

  JAVA_BLOCK_END(env)
}

void Java_oracle_sharding_tools_OCIDirectPath_discard(JNIEnv* env, jobject obj)
{
  JAVA_BLOCK_BEGIN(env)

  self(env, obj)->discard();

  JAVA_BLOCK_END(env)
}

void Java_oracle_sharding_tools_OCIDirectPath_setData(JNIEnv*env, jobject obj, jbyteArray data)
{
  JAVA_BLOCK_BEGIN(env)
  DECLARE_SELF(JavaDirectPathAPI, env, obj)

  if (!self->initialized) {
    env->ThrowNew(RuntimeExceptionClass, "Direct Path Loader is not initialized");
  }

  int len = env->GetArrayLength(data);
  void * bytes = env->GetPrimitiveArrayCritical(data, nullptr);
  self->savedBuffer = self->buffer.write((const char *) bytes, len);
  env->ReleasePrimitiveArrayCritical(data, bytes, JNI_ABORT);

  JAVA_BLOCK_END(env)
}

void Java_oracle_sharding_tools_OCIDirectPath_setValue__III(JNIEnv*env, jobject obj, jint column, jint offset, jint len)
{
  JAVA_BLOCK_BEGIN(env)
  DECLARE_SELF(JavaDirectPathAPI, env, obj)

  self->dp->value(self->currentRow, column, self->savedBuffer + offset, len, 0);

  JAVA_BLOCK_END(env)
}

void Java_oracle_sharding_tools_OCIDirectPath_setValue__I_3B(JNIEnv*env, jobject obj, jint column, jbyteArray data)
{
  JAVA_BLOCK_BEGIN(env)
  DECLARE_SELF(JavaDirectPathAPI, env, obj)

  if (!self->initialized) {
    env->ThrowNew(RuntimeExceptionClass, "Direct Path Loader is not initialized");
  }

  int len = env->GetArrayLength(data);
  void * bytes = env->GetPrimitiveArrayCritical(data, nullptr);
  char * ptr2  = self->buffer.write((const char *) bytes, len);
  env->ReleasePrimitiveArrayCritical(data, bytes, JNI_ABORT);

  self->dp->value(self->currentRow, column, ptr2, len, 0);

  JAVA_BLOCK_END(env)
}

void Java_oracle_sharding_tools_OCIDirectPath_setValue__I_3BII(JNIEnv*env, jobject obj, jint column, jbyteArray data, jint offset, jint len)
{
  JAVA_BLOCK_BEGIN(env)
  DECLARE_SELF(JavaDirectPathAPI, env, obj)

  if (!self->initialized) {
    env->ThrowNew(RuntimeExceptionClass, "Direct Path Loader is not initialized");
  }

  int len2 = env->GetArrayLength(data);

  if (len2 < offset + len) {
    env->ThrowNew(RuntimeExceptionClass, "Length out of bounds");
  }

  void * bytes = env->GetPrimitiveArrayCritical(data, nullptr);
  char * ptr2  = self->buffer.write(((const char *) bytes) + offset, len);
  env->ReleasePrimitiveArrayCritical(data, bytes, JNI_ABORT);

  self->dp->value(self->currentRow, column, ptr2, len, 0);

  JAVA_BLOCK_END(env)
}

void Java_oracle_sharding_tools_OCIDirectPath_begin(JNIEnv* env, jobject obj)
{
  JAVA_BLOCK_BEGIN(env)

  self(env, obj)->initialized = true;
  self(env, obj)->begin();

  JAVA_BLOCK_END(env)
}

void Java_oracle_sharding_tools_OCIDirectPath_setAttribute(JNIEnv*env, jobject obj, jstring a_name, jstring a_value)
{
  JAVA_BLOCK_BEGIN(env)
  DECLARE_SELF(JavaDirectPathAPI, env, obj)

  oci::DirectPathInsert::MyAttributes attributes = self->dp->attr();

  std::string name  = getJavaString(env, a_name);
  std::string value = getJavaString(env, a_value);

  char lastChr = name.data()[name.length() - 1];

#define XX_ATTR_VALUE_INT(ATTR, TYPE) if (name == #ATTR) { \
    attributes.setT<TYPE>(ATTR, atoi(value.c_str())); break; } else

#define XX_ATTR_VALUE_STR(ATTR) if (name == #ATTR) { \
    attributes.setS(ATTR, value); break; } else

  switch (lastChr) {
    case 'E' :
    XX_ATTR_VALUE_INT(OCI_ATTR_BUF_SIZE, ub4);
//    XX_ATTR_VALUE_INT(OCIP_ATTR_DIRPATH_SERVER_SLOT_SIZE, ub4);
    case 'L' :
    XX_ATTR_VALUE_INT(OCI_ATTR_DIRPATH_STORAGE_INITIAL, ub4);
    XX_ATTR_VALUE_INT(OCI_ATTR_DIRPATH_PARALLEL, ub1);
    case 'T' :
    if (name == "BUFFER_ROW_COUNT") { self->maxRowCount = atoi(value.c_str()); break; } else
    XX_ATTR_VALUE_INT(OCI_ATTR_DIRPATH_STORAGE_NEXT, ub4);
    XX_ATTR_VALUE_STR(OCI_ATTR_DATEFORMAT);
    case 'G' :
    XX_ATTR_VALUE_INT(OCI_ATTR_DIRPATH_NOLOG, ub1);
    case 'Y' :
    default :
      env->ThrowNew(RuntimeExceptionClass, "Unknown attribute");
  };

#undef XX_ATTR_VALUE_INT

  JAVA_BLOCK_END(env)
}
