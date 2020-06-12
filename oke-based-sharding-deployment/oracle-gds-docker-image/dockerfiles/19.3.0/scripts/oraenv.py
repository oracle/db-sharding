#!/usr/bin/python

#############################
# Copyright 2020, Oracle Corporation and/or affiliates.  All rights reserved.
# Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl
# Author: paramdeep.saini@oracle.com
############################

"""
 This file read the env variables from a file or using env command and populate them in  variable 
"""

import os

class OraEnv:
   __instance                                  = None
   __env_var_file                              = '/etc/rac_env_vars'
   __env_var_file_flag                         = None
   __env_var_dict                              = {}
   __ora_asm_diskgroup_name                    = '+DATA'
   __ora_gimr_flag                             = 'false' 
   __ora_grid_user                             = 'grid'
   __ora_db_user                               = 'oracle'
   __ora_oinstall_group_name                   = 'oinstall'
   encrypt_str__                               = None
   original_str__                              = None
   
   def __init__(self):
      """ Virtually private constructor. """
      if OraEnv.__instance != None:
         raise Exception("This class is a singleton!")
      else:
         OraEnv.__instance = self
         OraEnv.read_variable()
         OraEnv.add_variable()

   @staticmethod 
   def get_instance():
      """ Static access method. """
      if OraEnv.__instance == None:
         OraEnv()
      return OraEnv.__instance

   @staticmethod
   def read_variable():
      """ Read the variables from a file into dict """
      if OraEnv.__env_var_file_flag:
        with open(OraEnv.__env_var_file) as envfile:
           for line in envfile:
               name, var = line.partition("=")[::2]
               OraEnv.__env_var_dict[name.strip()] = var 
      else:
         OraEnv.__env_var_dict = os.environ

   @staticmethod
   def add_variable():
      """ Add more variable ased on enviornment with default values in __env_var_dict"""
      if "ORA_ASM_DISKGROUP_NAME" not in OraEnv.__env_var_dict:
         OraEnv.__env_var_dict["ORA_ASM_DISKGROUP_NAME"] = "+DATA"
 
      if "ORA_GRID_USER" not in OraEnv.__env_var_dict:
         OraEnv.__env_var_dict["ORA_GRID_USER"] = "grid"

      if "ORA_DB_USER" not in OraEnv.__env_var_dict:
         OraEnv.__env_var_dict["ORA_DB_USER"] = "oracle"
 
      if "ORA_OINSTALL_GROUP_NAME" not in OraEnv.__env_var_dict:
         OraEnv.__env_var_dict["ORA_OINSTALL_GROUP_NAME"] = "oinstall"
 
   @staticmethod
   def get_env_vars():
      """ Static access method to get the env vars. """
      return OraEnv.__env_var_dict

   @staticmethod
   def update_env_vars(env_dict):
      """ Static access method to get the env vars. """
      OraEnv.__env_var_dict = env_dict

   @staticmethod
   def logfile_name():
      """ Static access method to return the logfile name. """
      if "LOGFILE_NAME"  not in OraEnv.__env_var_dict:
          OraEnv.__env_var_dict["LOG_FILE_NAME"] = "/tmp/oracle_shrding_setup.log"

      return OraEnv.__env_var_dict["LOG_FILE_NAME"]          
