/*
** Oracle Sharding Tools Library
**
** Copyright © 2017 Oracle and/or its affiliates. All rights reserved.
** Licensed under the Universal Permissive License v 1.0 as shown at 
**   http://oss.oracle.com/licenses/upl 
*/

#ifndef FIXEDBUFFER_HPP
#define FIXEDBUFFER_HPP

#include <algorithm>
#include <string.h>

class DynamicBuffer {
  struct Frame {
    size_t size;
    char * buf;
    char * p;
    Frame * next = nullptr;

    explicit Frame(size_t a_size) : size(a_size) {
      p = buf = new char[size];
    }

    ~Frame() { delete [] buf; }

    bool fits(size_t len) { return (int) (p - buf) <= (int) (size - len); }
  };

  Frame * begin = nullptr, * end = nullptr;

  Frame * createBuffer(size_t size) {
    Frame * frame = new Frame(size);
    frame->next = end->next;
    end->next = frame;
    return frame;
  }

public:
  DynamicBuffer(size_t a_frameSize = 8*1024)
  {
    begin = end = new Frame(a_frameSize);
  }

  ~DynamicBuffer() {
    for (Frame * x = begin; x != nullptr; ) {
      Frame * y = x->next;
      delete x;
      x = y;
    }
  }

  char * write(const char * c, size_t len)
  {
    Frame * frame = end;

    if (len >= frame->size) {
      /* Create overflow buffer */
      frame = createBuffer(len);
    } else {
      /* Find the nearest frame, which would fit the input buffer */

      while ((frame != nullptr) && !frame->fits(len)) {
        end = frame;
        frame = frame->next;
      }

      if (frame == nullptr) {
        end = frame = createBuffer(end->size);
      }
    }

    char * p_ = frame->p;
    memcpy(p_, c, len);
    frame->p += len;
    return p_;
  }

  void reset() {
    for (Frame * x = begin; x != nullptr; ) {
      x->p = x->buf;
      x = x->next;
    }

    end = begin;
  };
};

struct FixedBuffer {
  char * buf;
  char * ptr;

  FixedBuffer(size_t fixedlen) {
    buf = new char[fixedlen];
    ptr = buf;
  }

  ~FixedBuffer() { delete[] buf; }

  char * write(const char * c, size_t len)
  {
    char * p_ = ptr;
    memcpy(p_, c, len);
    ptr += len;
    return p_;
  }

  void reset() { ptr = buf; };
};

#endif /* FIXEDBUFFER_HPP */
