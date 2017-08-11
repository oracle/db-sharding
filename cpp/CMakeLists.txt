project(directpathutilities)
CMAKE_MINIMUM_REQUIRED(VERSION 2.8)

macro(use_cxx11)
  if (CMAKE_VERSION VERSION_LESS "3.1")
    if (CMAKE_CXX_COMPILER_ID STREQUAL "GNU")
      set (CMAKE_CXX_FLAGS "-std=gnu++11 ${CMAKE_CXX_FLAGS}")
    endif ()
  else ()
    set (CMAKE_CXX_STANDARD 11)
  endif ()
endmacro(use_cxx11)

use_cxx11()

find_package(Java COMPONENTS Development)
find_package(JNI)

set(OCI_LIBRARY_PATH "/usr/lib/oracle/12.2/client64/lib/" CACHE STRING "Path to OCI library files")
set(OCI_HEADER_PATH "/usr/include/oracle/12.2/client64/" CACHE STRING "Path to OCI library file headers")

message("CMAKE_INSTALL_PREFIX=${CMAKE_INSTALL_PREFIX}")
message(STATUS "OCI_HEADER_PATH=${OCI_HEADER_PATH}")
message(STATUS "OCI_LIBRARY_PATH=${OCI_LIBRARY_PATH}")

include_directories(${CMAKE_SOURCE_DIR} ${OCI_HEADER_PATH})
include_directories(${JAVA_INCLUDE_PATH} ${JAVA_INCLUDE_PATH2})

link_directories(${OCI_LIBRARY_PATH})

add_library(dpjni SHARED javadpl/OCIDirectPathJNI.cpp)

target_link_libraries(dpjni clntsh nnz12 clntshcore ons mql1 ipc1 occi ociei pthread)

install(TARGETS dpjni LIBRARY DESTINATION ${CMAKE_INSTALL_PREFIX}/lib64)
