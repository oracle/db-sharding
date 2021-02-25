#!/usr/bin/python

#############################
# Copyright 2020, Oracle Corporation and/or affiliates.  All rights reserved.
# Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl
# Author: paramdeep.saini@oracle.com
############################

"""
This is the file which create dirs and listener.ora for standby setup on primary
"""

import subprocess
import sys
import time
import datetime
import os
import commands
import getopt
import shlex
import json
import logging
import socket
import re

def Usage():
    pass

def exec_cmd(type):
    """
     Restart Listener
    """
    if type == 'RELOAD_LISTENER':
       ohome = oracle_home=read_env_variables("ORACLE_HOME")
       cmd = '''{0}/bin/lsnrctl reload'''.format(ohome)
       os.system(cmd)

def create_dir(dir):
    """
    Create dir locally or remotely
    Attributes:
      dir (string): dir to be created
     """
    Redirect_To_File("Inside create_dir()","INFO")
    if not os.path.isdir(dir):
       mode = 0o755
       os.mkdir(dir, mode)  


def copy_files(filesarg):
    """
    copy files from source to destination
    """
    Redirect_To_File("Inside create_dir()","INFO")
    srcfile,dstloc=process_list_vars(filesarg)
    cmd=''' cp {0} {1}'''.format(srcfile,dstloc)
    os.system(cmd)

def get_sid_desc(gdbname,ohome,sid,sflag):
    """
    get the SID_LISTENER_DESCRIPTION
    """
    sid_desc = ""
    if sflag == 'SID_DESC1':
       sid_desc = '''    )
       (SID_DESC =
         (GLOBAL_DBNAME = {0})
         (ORACLE_HOME = {1})
         (SID_NAME = {2})
        )
     )
    '''.format(gdbname,ohome,sid)
    elif sflag == 'SID_DESC':
       sid_desc = '''(SID_LIST =
       (SID_DESC =
         (GLOBAL_DBNAME = {0})
         (ORACLE_HOME = {1})
         (SID_NAME = {2})
        )
      )
    '''.format(gdbname,ohome,sid)
    else:
      pass

    return sid_desc

def get_lisora(port):
    """
    return listener.ora listener settings
    """
    listener='''LISTENER =
      (DESCRIPTION_LIST =
        (DESCRIPTION =
          (ADDRESS = (PROTOCOL = TCP)(HOST = 0.0.0.0)(PORT = {0}))
          (ADDRESS = (PROTOCOL = IPC)(KEY = EXTPROC{0}))
           )
          )
    '''.format(port)
    return listener

def process_list_vars(cvar_str):
    """
    This function process listener variables
    """
    gname=None
    sid=None
    Redirect_To_File("inside process_list_vars " + cvar_str, "INFO")
    olist=cvar_str.split(",")
    gname=olist[0]
    sid=olist[1] 
         ### Check values must be set
    Redirect_To_File("Global DB Name and SID Name set:  " + gname + "," + sid, "INFO")
    if  gname and sid:
      return gname.replace('"', ''),sid.replace('"', '')
    else:
      sys.exit(127) 

def read_env_variables(key):
    """
     read env variables and return the key
    """
    fname="/tmp/env_var_py.txt"
    cmd='''rm -f {0}'''.format(fname)
    os.system(cmd)
    cmd='''{0} | tr '\{2}' '\{3}' > {1}'''.format("cat /proc/1/environ",fname,"0","n")
    Redirect_To_File(cmd,"INFO")
    os.system(cmd)

    # Using readline() 
    file1 = open(fname, 'r') 
    Lines = file1.readlines() 
    file1.close()
    #Redirect_To_File(Lines,"INFO")
    for line in Lines: 
      str_arr = line.split("=")
      str1 = str_arr[0]
      str2 = str_arr[1]
      if str1 == key:
         return str2.rstrip() 
   
def reset_listener(listparams):
     """
     Funtion to reset the listener
     """
     Redirect_To_File("Inside reset_listener() " + listparams, "INFO")
     start = 'SID_LIST_LISTENER'
     end = '^\)$'
     oracle_home=read_env_variables("ORACLE_HOME")
    # Redirect_To_File("This is a ohome location " + oracle_home,"INFO")
    # oracle_home="/opt/oracle/product/19c/dbhome_1"
     Redirect_To_File("This is a ohome location " + oracle_home,"INFO")
     lisora='''{0}/network/admin/listener.ora'''.format(oracle_home)
     buffer = "SID_LIST_LISTENER=" + '\n'
     gdbname,sid=process_list_vars(listparams)
     start_flag = False
     try:
       with open(lisora) as f:
          for line1 in f:
              if start_flag == False:
                 if (re.match(start, line1.strip())):
                     start_flag = True
              elif (re.match(end, line1.strip())):
                 line2 = f.next()
                 if (re.match(end, line2.strip())):
                     break
                 else:
                     buffer += line1
                     buffer += line2
              else:
                 if start_flag == True:
                     buffer += line1
     except:
       pass

     if start_flag == True:
         buffer +=  get_sid_desc(gdbname,oracle_home,sid,"SID_DESC1")
         listener = get_lisora(1521)
         listener += '\n' + buffer
     else:
         buffer += get_sid_desc(gdbname,oracle_home,sid,"SID_DESC")
         listener = get_lisora(1521)
         listener += '\n' + buffer

    # cmd='''echo {0} > /tmp/list.ora'''.format(listener)
    # os.system(cmd)
     wr = open(lisora, 'w')
     wr.write(listener)


def Redirect_To_File(text,level):
    original = sys.stdout
    sys.stdout = open('/proc/1/fd/1', 'w')
    root = logging.getLogger()
    if not root.handlers:
       root.setLevel(logging.INFO)
       ch = logging.StreamHandler(sys.stdout)
       ch.setLevel(logging.INFO)
       formatter = logging.Formatter('%(asctime)s :%(message)s', "%Y-%m-%d %T %Z")
       ch.setFormatter(formatter)
       root.addHandler(ch)
    message = os.path.basename(__file__) + " : " + text
    root.info(' %s ' % message )
    sys.stdout = original


def main(): 
   # Checking Comand line Args
   Redirect_To_File("Passed Parameters " + str(sys.argv[1:]), "INFO")
   try:
     opts, args = getopt.getopt(sys.argv[1:], '', ['resetlistener=','createdir=','reloadlistener=','copyfiles=','help'])
   except getopt.GetoptError:
      Usage()
      sys.exit(2)
  
   for opt, arg in opts:
      if opt in ('--help'):
        usage()
        sys.exit(2)          
      elif opt in ('--resetlistener'):
        listparams = arg 
        reset_listener(listparams)    
      elif opt in ('--createdir'):
        dirparams = arg
        create_dir(dirparams)
      elif opt in ('--reloadlistener'):
        exec_cmd("RELOAD_LISTENER")
      elif opt in ('--copyfiles'):
        filesarg = arg
        copy_files(filesarg)
      else:
        Usage()
        sys.exit(2)

    
# Using the special variable  
if __name__=="__main__": 
    main() 
