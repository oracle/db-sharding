/*
** Oracle Sharding Tools Library
**
** Copyright © 2017 Oracle and/or its affiliates. All rights reserved.
** Licensed under the Universal Permissive License v 1.0 as shown at 
**   http://oss.oracle.com/licenses/upl 
*/

#ifndef OCI_HELP_HPP
#define OCI_HELP_HPP

#include <vector>
#include <stdexcept>
#include <oci.h>
#include <ocidfn.h>
#include <sstream>
#include <string.h>

#include <iostream>

struct BuildString {};

static inline std::string operator <<(std::ostream & stream, const BuildString &) {
  return static_cast<std::ostringstream &>(stream).str();
}

static inline std::string get_string(std::ostream & stream)
{
  return static_cast<std::ostringstream &>(stream).str();
}

namespace oci {

template<int HandleTypeId> struct HandleTypeDef        { typedef void          * HandleType; };
template<> struct HandleTypeDef<OCI_HTYPE_SERVER>      { typedef OCIServer     * HandleType; };
template<> struct HandleTypeDef<OCI_HTYPE_ERROR>       { typedef OCIError      * HandleType; };
template<> struct HandleTypeDef<OCI_HTYPE_SESSION>     { typedef OCISession    * HandleType; };
template<> struct HandleTypeDef<OCI_HTYPE_SVCCTX>      { typedef OCISvcCtx     * HandleType; };
template<> struct HandleTypeDef<OCI_HTYPE_DIRPATH_CTX> { typedef OCIDirPathCtx * HandleType; };
template<> struct HandleTypeDef<OCI_HTYPE_STMT>        { typedef OCIStmt       * HandleType; };

template<> struct HandleTypeDef<OCI_HTYPE_DIRPATH_COLUMN_ARRAY> { typedef OCIDirPathColArray * HandleType; };
template<> struct HandleTypeDef<OCI_HTYPE_DIRPATH_STREAM>       { typedef OCIDirPathStream * HandleType; };

template<int HandleTypeId> struct Handle;

class Exception;

template<int HandleTypeId>
struct Handle
{
  enum { handleType = HandleTypeId };

  typedef typename HandleTypeDef<HandleTypeId>::HandleType HandleTypeT;
  HandleTypeT handle = nullptr;

  Handle() : handle(nullptr) { }

  void init(void * parent);

  explicit Handle(void * parent) : handle(nullptr) {
    init(parent);
  }

  void destroy() {
    if (handle != nullptr) {
      OCIHandleFree((void *) handle, handleType);
      handle = nullptr;
    }
  }

  virtual ~Handle() {
    destroy();
  }
};

struct ErrorHandle : public Handle<OCI_HTYPE_ERROR>
{
  int  lastStatus = 0;
  sb4  errCode = 0;
  void * userTag = nullptr;

  void clear() {
    lastStatus = 0;
    errCode = 0;
  }

  int status() const { return lastStatus; }

  bool clean() const {
    return lastStatus == OCI_SUCCESS || lastStatus == OCI_SUCCESS_WITH_INFO;
  }

  bool fatal() const {
    return lastStatus == OCI_ERROR || lastStatus == OCI_INVALID_HANDLE;
  }

  operator bool() const { return clean(); }

  std::string getLastError()
  {
    if (lastStatus == OCI_ERROR || lastStatus == OCI_SUCCESS_WITH_INFO) {
      text errbuf[512];

      OCIErrorGet(handle, 1, NULL, &errCode,
        errbuf, (ub4) sizeof(errbuf), OCI_HTYPE_ERROR);

      return std::string((const char *) (errbuf + 0));
    } else if (lastStatus == OCI_INVALID_HANDLE) {
      return std::string("OCI_INVALID_HANDLE");
    } else if (lastStatus == OCI_NO_DATA) {
      return std::string("OCI_NO_DATA");
    } else {
      return std::string(std::ostringstream() << "Last status " << lastStatus << BuildString());
    }
  }

  bool check(int status)
  {
    lastStatus = status;
    return clean();
  }
};

#define OCI_CHECK_W(CALL, ERR_H, WARN) do {\
  if (!(ERR_H).clean()) { throw Exception(std::ostringstream() << \
        "Environment not clean, last status : "<< (ERR_H).lastStatus << BuildString(), __FILE__, __LINE__); }\
  (ERR_H).check(CALL);\
  if ((ERR_H).fatal()) { throw Exception((ERR_H), __FILE__, __LINE__); }\
  if ((ERR_H).lastStatus == OCI_SUCCESS_WITH_INFO) { \
    WARN(ERR_H); }\
} while(0)

class Exception: public std::exception
{
protected:
    int code;
    std::string msg_;

    const char * file_;
    int line_;
public:
    explicit Exception(const char * message, const char * file = NULL, int line = 0)
      : msg_(message), file_(file), line_(line) { }

    explicit Exception(const std::string & message, const char * file = NULL, int line = 0)
      : msg_(message), file_(file), line_(line) { }

    explicit Exception(ErrorHandle & errorHandle, const char * file = NULL, int line = 0)
      : msg_(errorHandle.getLastError()), file_(file), line_(line) 
    {
      char buf[1024];
      sprintf(buf, "%s:%d %s", file, line, errorHandle.getLastError().c_str());
      msg_ = std::string(buf);
//      code = errorHandle.errCode;
    }

    virtual ~Exception() throw () {}

    virtual const char* what() const throw () { return msg_.c_str(); }
};

typedef void (*WarningFunction)(ErrorHandle & errorHandle);

struct EnvironmentWrapper
{
  OCIEnv * envhd = nullptr;
  ErrorHandle errorCtx;
  WarningFunction warningFunction = 0;

  void init();

  EnvironmentWrapper() {
  }

  EnvironmentWrapper(int) {
    init();
  }

  void warning(ErrorHandle & errorCtx) {
    if (warningFunction != 0) { warningFunction(errorCtx); }
  }

  void destroy()
  {
    if (envhd != nullptr) {
      errorCtx.destroy();
      OCIHandleFree(envhd, OCI_HTYPE_ENV);
      envhd = nullptr;
    }
  }

  ~EnvironmentWrapper() {
    destroy();
  }
};

#define OCI_CHECK_E(CALL, ENV) OCI_CHECK_W(CALL, (ENV).errorCtx, (ENV).warning)


template<int HandleTypeId>
void Handle<HandleTypeId>::init(void* parent)
{
  int errorCode = OCIHandleAlloc((void*) parent, (void **) &handle, handleType, 0, nullptr);

  if (errorCode != 0) {
    handle = nullptr;

    throw Exception(
      std::ostringstream() << "OCIHandleAlloc failed with errcode = " 
        << errorCode << BuildString(),
          __FILE__, __LINE__);
  }
}

void EnvironmentWrapper::init()
{
  int errorCode = OCIEnvCreate(&envhd, OCI_THREADED,
    nullptr, (dvoid * (*)(dvoid *,size_t)) 0,
    (dvoid * (*)(dvoid *, dvoid *, size_t)) 0,
    (void (*)(dvoid *, dvoid *)) 0,
    (size_t) 0,  (dvoid **) 0);

  if (errorCode != 0) 
  {
    envhd = nullptr;

    throw Exception(
      std::ostringstream() << "OCIEnvCreate failed with errcode = " 
        << errorCode << BuildString(), 
          __FILE__, __LINE__);
  }

  errorCtx.init(envhd);
}

template<int HandleTypeId>
struct Attributes
{
  void * handle;
  EnvironmentWrapper & ociEnv;

  void init(void * a_handle) { handle = a_handle; }

  Attributes(Handle<HandleTypeId> & a_handle, EnvironmentWrapper & a_env)
    :   handle(a_handle.handle), ociEnv(a_env) { }

  Attributes(void * a_handle, EnvironmentWrapper & a_env)
    :   handle(a_handle), ociEnv(a_env) { }

  explicit Attributes(EnvironmentWrapper & a_env)
    :   handle(nullptr), ociEnv(a_env) { }

  ~Attributes() { }

  void set(ub4 attribute, void * ptr, ub4 sz) {
    OCI_CHECK_E(OCIAttrSet((void *) handle, (ub4) HandleTypeId,
          ptr, sz, attribute, ociEnv.errorCtx.handle), ociEnv);
  }

  void get(ub4 attribute, void * ptr, ub4 * sz) {
    OCI_CHECK_E(OCIAttrGet((void *) handle, (ub4) HandleTypeId,
          ptr, sz, attribute, ociEnv.errorCtx.handle), ociEnv);
  }

  template<typename ValueType>
  ValueType get(ub4 attribute) {
    ValueType ptr;
    get(attribute, &ptr, nullptr);
    return ptr;
  }

  void setS(ub4 attribute, const char * str) {
    set(attribute, (void *) str, strlen(str));
  }

  void setS(ub4 attribute, const std::string & str) {
    set(attribute, (void *) str.c_str(), str.length());
  }

  template<typename ValueType>
  void setT(ub4 attribute, const ValueType & x) {
    set(attribute, (ValueType *) &x, sizeof(ValueType));
  }
};

struct Parameter : public Attributes<OCI_DTYPE_PARAM>
{
  void init(void * a_handle, int pos) {
    OCIParamGet(a_handle, OCI_DTYPE_PARAM, ociEnv.errorCtx.handle, &handle, pos);
  };

  Parameter(void * a_handle, int pos, EnvironmentWrapper & a_env)
    : Attributes<OCI_DTYPE_PARAM>(a_env)
  {
    init(a_handle, pos);
  }

  ~Parameter()
  {
    if (handle != nullptr) {
      OCIDescriptorFree(handle, OCI_DTYPE_PARAM);
    }
  }
};

struct Connection
{
  EnvironmentWrapper & ociEnv;

  Handle<OCI_HTYPE_SERVER>  serverHandle;
  Handle<OCI_HTYPE_SVCCTX>  svcHandle;
  Handle<OCI_HTYPE_SESSION> authHandle;

  bool isConnected = false;
  bool isAttached = false;

  std::string dbConnectString;
  std::string username;

  explicit Connection(EnvironmentWrapper & a_ociEnv) : ociEnv(a_ociEnv) {}

  Connection(EnvironmentWrapper & a_ociEnv,
             const std::string & a_connect_string,
             const std::string & a_username)
    : ociEnv(a_ociEnv), dbConnectString(a_connect_string), username(a_username) {}

  void connect(const std::string & a_connect_string, const std::string & a_username, const std::string & a_password)
  {
    dbConnectString = a_connect_string;
    username = a_username;
    connect(a_password);
  }

  void connect(const std::string & password);

  void disconnect();

  void commit() {
    OCI_CHECK_E(OCITransCommit(svcHandle.handle, ociEnv.errorCtx.handle, OCI_DEFAULT), ociEnv);
  }

  virtual ~Connection() {
    disconnect();
  }
};

struct StandaloneConnection : public Connection
{
  EnvironmentWrapper ownedEnvironment;

  StandaloneConnection() : Connection(ownedEnvironment) {
    ownedEnvironment.init();
  }

  StandaloneConnection(const std::string & a_connect_string, const std::string & a_username, const std::string & a_password)
    : Connection(ownedEnvironment) 
  {
    ownedEnvironment.init();
    connect(a_connect_string, a_username, a_password);
  }

  virtual ~StandaloneConnection() {
    disconnect();
  }
};

void Connection::disconnect()
{
  if (isConnected) {
    OCISessionEnd(svcHandle.handle, ociEnv.errorCtx.handle, authHandle.handle, OCI_DEFAULT);
    ociEnv.errorCtx.clear();
    isConnected = false;
  }

  if (isAttached) {
    OCIServerDetach(serverHandle.handle, ociEnv.errorCtx.handle, OCI_DEFAULT);
    ociEnv.errorCtx.clear();
    isAttached = false;
  }

  authHandle.destroy();
  svcHandle.destroy();
  serverHandle.destroy();
}

void Connection::connect(const std::string & password)
{
  serverHandle.init(ociEnv.envhd);
  svcHandle.init(ociEnv.envhd);

  OCI_CHECK_E(OCIServerAttach(serverHandle.handle, ociEnv.errorCtx.handle,
    (const oratext *) dbConnectString.c_str(),
    dbConnectString.length(), 0), ociEnv);

  isAttached = true;

  Attributes<OCI_HTYPE_SVCCTX> svcAttr(svcHandle.handle, ociEnv);

  svcAttr.set(OCI_ATTR_SERVER, serverHandle.handle, 0);

  authHandle.init(ociEnv.envhd);

  Attributes<OCI_HTYPE_SESSION> sessionAttr(authHandle.handle, ociEnv);

  sessionAttr.setS(OCI_ATTR_USERNAME, username);
  sessionAttr.setS(OCI_ATTR_PASSWORD, password);

  OCI_CHECK_E(OCISessionBegin(svcHandle.handle, ociEnv.errorCtx.handle,
    authHandle.handle, OCI_CRED_RDBMS, (ub4) OCI_DEFAULT), ociEnv);

  isConnected = true;

  svcAttr.set(OCI_ATTR_SESSION, authHandle.handle, 0);
}

template<typename Type> struct TypeWrap { enum { OCI_Type = SQLT_CHR }; enum { Size = sizeof(Type) }; };

template<int ArraySize> struct TypeWrap<char[ArraySize]> { enum { OCI_Type = SQLT_CHR }; enum { Size = ArraySize }; };
template<int ArraySize> struct TypeWrap<ub1[ArraySize]> { enum { OCI_Type = SQLT_CHR }; enum { Size = ArraySize }; };

template<> struct TypeWrap<ub4> { enum { OCI_Type = SQLT_UIN }; enum { Size = sizeof(ub4) }; };
template<> struct TypeWrap<sb4> { enum { OCI_Type = SQLT_INT }; enum { Size = sizeof(sb4) }; };
template<> struct TypeWrap<ub2> { enum { OCI_Type = SQLT_UIN }; enum { Size = sizeof(ub2) }; };
template<> struct TypeWrap<sb2> { enum { OCI_Type = SQLT_INT }; enum { Size = sizeof(sb2) }; };
template<> struct TypeWrap<ub1> { enum { OCI_Type = SQLT_UIN }; enum { Size = sizeof(ub1) }; };
template<> struct TypeWrap<sb1> { enum { OCI_Type = SQLT_INT }; enum { Size = sizeof(sb1) }; };

struct OracleObject
{
  
};

struct DefineDynamicTypeBase
{
  OCIDefine * handle;
  OCIInd * ind;
  ub2 dataType;
  ub4 dataSize;
  ub2 rc;

  virtual ~DefineDynamicTypeBase() {};

  ub2 size() const { return dataSize; }

private:
  void define();
};


struct DefineTypeWithBuffer : public DefineDynamicTypeBase
{
  ub1 * buffer;

  virtual ~DefineTypeWithBuffer() { if (buffer != nullptr) { delete [] buffer; } };

  
};



struct DefineObject : public DefineDynamicTypeBase
{
  OracleObject object;

  virtual ~DefineObject() { };
};

struct DefineLob : public DefineDynamicTypeBase
{

  virtual ~DefineLob() { };
};

/*
template<typename Type>
struct DefineDynamicType : public DefineDynamicTypeBase
{
  OCIDefine * operator ()(int pos, Type & value, void * handle, EnvironmentWrapper & env)
  {
    OCI_CHECK_E(OCIDefineByPos(handle, &def, env.envhd, pos,
      value, (sword) TypeWrap<Type>::Size, TypeWrap<Type>::OCI_Type, 
          (dvoid *) 0, (ub2 *)0, (ub2 *)0, OCI_DEFAULT), env);

    return def;
  }
};

template<>
struct DefineDynamicType<std::string> : public DefineDynamicTypeBase
{
  static sb4 stringCallbackDefine(void * octxp, OCIDefine *defnp, ub4 iter,
    void **bufpp, ub4 ** alenpp, ub1 * piecep, void **indpp, ub2 **rcodep)
  {
     std::string * target = (std::string *) octxp;
     target->resize(**alenpp);
     *bufpp = const_cast<char *>(target->data());
     return OCI_CONTINUE;
  }

  OCIDefine * operator ()(int pos, std::string & value, void * handle, EnvironmentWrapper & env)
  {
    OCIDefine * def;

    OCI_CHECK_E(OCIDefineByPos(handle, &def, env.envhd, pos,
      nullptr, (sword) 0, SQLT_CHR, nullptr, nullptr, nullptr, OCI_DYNAMIC_FETCH), env);

    OCI_CHECK_E(OCIDefineDynamic(def, errhd, &value, &stringCallbackDefine), env);

    return def;
  }
};
*/

struct Statement
{
  Connection & connection;
  Handle<OCI_HTYPE_STMT> h;
  std::vector<OCIDefine *> defArray;
  std::string query;

  inline OCIError * errhd() { return connection.ociEnv.errorCtx.handle; };

  void prepare(const std::string & query);

  Statement(Connection & a_connection)
    : connection(a_connection) { }

  Statement(Connection & a_connection, const std::string & a_query)
    : connection(a_connection) { prepare(a_query); }

  void define(ub4 pos, void * value, size_t sz, int oratype);

  void autoDefine(ub4 pos, void * value, size_t sz, int oratype);

  void defineString(ub4 pos, std::string & result);

  template<typename Type>
  void defineT(ub4 pos, Type & value, size_t sz = TypeWrap<Type>::Size)
  {
    define(pos, &value, sz, TypeWrap<Type>::OCI_Type);
  }

  const ErrorHandle & execute(int rows = 0, int offset = 0);

  const ErrorHandle & fetch(ub4 nrows = 1);

private:
  struct DefineT {
    Statement & statement;
    ub4 pos;

    DefineT(Statement & a_statement, int a_pos) 
      : statement(a_statement), pos(a_pos) {};

    template<typename Type>
    DefineT operator >> (Type & value) {
      statement.defineT(pos, value);
      return DefineT(statement, pos + 1); 
    }
  };
public:
  DefineT defines(int pos = 1) { return DefineT(*this, pos); }
};

void DefineDynamicTypeBase::define()
{

}


static ub4 ttt = 0;

static sb4 stringCallbackDefine(void * octxp, OCIDefine *defnp, ub4 iter,
  void **bufpp, ub4 ** alenpp, ub1 * piecep, void **indpp, ub2 **rcodep)
{
  std::string * target = (std::string *) octxp;
  if (*alenpp == nullptr) { *alenpp = &ttt; }
  target->resize(128);
  *bufpp = const_cast<char *>(target->data());
  return OCI_CONTINUE;
}

void Statement::defineString(ub4 pos, std::string & value)
{
  OCIDefine * def;
  EnvironmentWrapper & env = connection.ociEnv;

  OCI_CHECK_E(OCIDefineByPos(h.handle, &def, errhd(), pos,
    nullptr, (sword) 0, SQLT_CHR, nullptr, nullptr, nullptr, OCI_DYNAMIC_FETCH), env);

  OCI_CHECK_E(OCIDefineDynamic(def, errhd(), &value, &stringCallbackDefine), env);
}

void Statement::prepare(const std::string & a_query)
{
  h.init(connection.ociEnv.envhd);

  query = a_query;

  OCI_CHECK_E(OCIStmtPrepare(h.handle, errhd(),
    (const oratext *) query.c_str(), query.length(),
      (ub4) OCI_NTV_SYNTAX, (ub4) OCI_DEFAULT), connection.ociEnv);
}

void Statement::define(ub4 pos, void* value, size_t sz, int oratype)
{
  std::cout << pos << " " << value << " " << sz << " " << oratype << std::endl;

  if (defArray.size() < pos) {
    defArray.resize(pos);
  }

  OCIDefine * def = defArray.at(pos - 1);

  OCI_CHECK_E(OCIDefineByPos(h.handle, &def, errhd(), pos,
    value, (sword) sz, oratype, (dvoid *) 0, (ub2 *)0, (ub2 *)0, OCI_DEFAULT), 
              connection.ociEnv);

  defArray[pos - 1] = def;
}

const ErrorHandle & Statement::fetch(ub4 nrows)
{
  OCI_CHECK_E(OCIStmtFetch(h.handle, errhd(), nrows, 0, OCI_DEFAULT),
    connection.ociEnv);

  return connection.ociEnv.errorCtx;
}

const ErrorHandle & Statement::execute(int rows, int offset)
{
  OCI_CHECK_E(OCIStmtExecute(connection.svcHandle.handle, h.handle,
    errhd(), rows, offset, NULL, NULL, OCI_DEFAULT), connection.ociEnv);

  return connection.ociEnv.errorCtx;
}


class DirectPathInsert : public Handle<OCI_HTYPE_DIRPATH_CTX>
{
  struct ColumnDescriptor {
    std::string name;
    ub2 id;
    ub2 exttyp;
    ub1 prec;
    sb1 scale;
    ub2 csid;
    ub4 size;

    ColumnDescriptor(const std::string & a_name, ub2 a_exttyp, ub1 a_prec, sb1 a_scale, ub2 a_csid, ub4 a_maxlen)
      : name(a_name), id(0), exttyp(a_exttyp), prec(a_prec), scale(a_scale), csid(a_csid), size(a_maxlen) {};
  };

  Connection & conn;

public:
  Handle<OCI_HTYPE_DIRPATH_COLUMN_ARRAY> carray;
  Handle<OCI_HTYPE_DIRPATH_STREAM> stream;

  typedef Attributes<OCI_HTYPE_DIRPATH_CTX> MyAttributes;

  std::vector<ColumnDescriptor> columns;

  DirectPathInsert(Connection & a_ociEnv) : conn(a_ociEnv) {}

  void init();

  inline EnvironmentWrapper & env() { return conn.ociEnv; };
  inline OCIError * errhd() { return conn.ociEnv.errorCtx.handle; };

  MyAttributes attr() { return MyAttributes(*this, env()); }

  void column(const std::string & name, ub2 exttyp, int size);
  void setSink(const std::string & schemaName, const std::string & tableName, const std::string & partitionName);
  void initStream(unsigned int rows);

  void reset();
  void value(unsigned int row, unsigned int col, const char * value, size_t len, ub1 flags);
  int load(unsigned int count, unsigned int offset);
  void save(ub4 action);
  void finish();
  void setStreamBuffer(void * buf, ub4 size);
};

#define DP_CHECK(CALL) OCI_CHECK_E(CALL, conn.ociEnv)

void DirectPathInsert::init()
{
  Handle<OCI_HTYPE_DIRPATH_CTX>::init((void *) conn.ociEnv.envhd);

  MyAttributes attributes(*this, conn.ociEnv);

  /* Set default attributes */
  attributes.setT<ub1>(OCI_ATTR_DIRPATH_MODE,  OCI_DIRPATH_LOAD);
  attributes.setT<ub1>(OCI_ATTR_DIRPATH_INPUT, OCI_DIRPATH_INPUT_TEXT);
  attributes.setT<ub4>(OCI_ATTR_DIRPATH_STORAGE_INITIAL, 100000);
  attributes.setT<ub4>(OCI_ATTR_DIRPATH_STORAGE_NEXT, 100000);
  attributes.setT<ub1>(OCI_ATTR_DIRPATH_NOLOG, 1);
  attributes.setT<ub1>(OCI_ATTR_DIRPATH_PARALLEL, 1);
}

void DirectPathInsert::column(const std::string & name, ub2 exttyp, int size)
{
  columns.push_back(ColumnDescriptor(name, exttyp, 0, 0, 873, size));
}

void DirectPathInsert::setSink(const std::string& schemaName, const std::string& tableName, const std::string& partitionName)
{
//  Attributes<OCI_HTYPE_DIRPATH_CTX> attributes(h.handle, conn.ociEnv);
  MyAttributes attributes(*this, conn.ociEnv);

  attributes.setS(OCI_ATTR_SCHEMA_NAME, schemaName);
  attributes.setS(OCI_ATTR_NAME, tableName);

  if (!partitionName.empty()) {
    attributes.setS(OCI_ATTR_SUB_NAME, partitionName);
  }
}

void DirectPathInsert::initStream(unsigned int rows)
{
  MyAttributes attributes(*this, conn.ociEnv);
/*
  attributes.setT<ub1>(OCI_ATTR_DIRPATH_MODE,  OCI_DIRPATH_LOAD);
  attributes.setT<ub1>(OCI_ATTR_DIRPATH_INPUT, OCI_DIRPATH_INPUT_TEXT);
  attributes.setT<ub4>(OCI_ATTR_DIRPATH_STORAGE_INITIAL, 100000);
  attributes.setT<ub4>(OCI_ATTR_DIRPATH_STORAGE_NEXT, 100000);
  attributes.setT<ub1>(OCI_ATTR_DIRPATH_NOLOG, 1);
  attributes.setT<ub1>(OCI_ATTR_DIRPATH_PARALLEL, 1);
*/
  attributes.setT<ub4>(OCI_ATTR_NUM_COLS, (ub4) columns.size());
  attributes.setT<ub4>(OCI_ATTR_NUM_ROWS, rows);

  void * ociColumnList = attributes.get<void *>(OCI_ATTR_LIST_COLUMNS);
//  parameter_type = attributes.getT<ub4>(OCI_ATTR_PTYPE);

  for (unsigned int i = 0; i < columns.size(); ++i) {
    Parameter columnInfo(ociColumnList, i+1, conn.ociEnv);
    ColumnDescriptor & cdsc = columns.at(i);

    columnInfo.setS(OCI_ATTR_NAME, cdsc.name);
    columnInfo.setT(OCI_ATTR_DATA_TYPE, cdsc.exttyp);
    columnInfo.setT(OCI_ATTR_DATA_SIZE, cdsc.size);
    columnInfo.setT(OCI_ATTR_CHARSET_ID, cdsc.csid);
  }

  DP_CHECK(OCIDirPathPrepare(handle, conn.svcHandle.handle, errhd()));

  carray.init(handle);
  stream.init(handle);

//  Attributes<OCI_HTYPE_DIRPATH_COLUMN_ARRAY> carrayAttr(carray, conn.ociEnv);
}

void DirectPathInsert::setStreamBuffer(void * buf, ub4 size)
{
  Attributes<OCI_HTYPE_DIRPATH_STREAM> streamAttr(stream, conn.ociEnv);

  streamAttr.setT<void *>(OCI_ATTR_BUF_ADDR, buf);
  streamAttr.setT<ub4>(OCI_ATTR_BUF_SIZE, (ub4) size);
}

void DirectPathInsert::reset()
{
  DP_CHECK(OCIDirPathColArrayReset(carray.handle, errhd()));
}

void DirectPathInsert::value(unsigned int row, unsigned int col, const char* value, size_t len, ub1 flags)
{
  DP_CHECK(OCIDirPathColArrayEntrySet(carray.handle, errhd(), row, col, (ub1 *) value, (ub4) len, flags));
}

int DirectPathInsert::load(unsigned int count, unsigned int offset)
{
  bool to_continue = false;
  int total_written = 0;

  do {
    DP_CHECK(OCIDirPathStreamReset(stream.handle,  errhd()));
    DP_CHECK(OCIDirPathColArrayToStream(carray.handle, handle, stream.handle, errhd(), count, offset));

    if ((to_continue = (conn.ociEnv.errorCtx.lastStatus == OCI_CONTINUE))) {
      conn.ociEnv.errorCtx.clear();
    }

    ub4 written = Attributes<OCI_HTYPE_DIRPATH_COLUMN_ARRAY>(carray, conn.ociEnv).get<ub4>(OCI_ATTR_ROW_COUNT);

    DP_CHECK(OCIDirPathLoadStream(handle, stream.handle, errhd()));

    total_written += Attributes<OCI_HTYPE_DIRPATH_STREAM>(stream, conn.ociEnv).get<ub4>(OCI_ATTR_ROW_COUNT);

    offset += written;
  } while (to_continue);

  return total_written;
}

void DirectPathInsert::save(ub4 action)
{
  DP_CHECK(OCIDirPathDataSave(handle, errhd(), action));
}

void DirectPathInsert::finish()
{
  DP_CHECK(OCIDirPathFinish(handle, errhd()));
}


#undef DP_CHECK

};

#endif /* OCI_HELP_HPP */
