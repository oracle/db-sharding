#!/usr/bin/python

#############################
# Copyright 2020, Oracle Corporation and/or affiliates.  All rights reserved.
# Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl
# Author: paramdeep.saini@oracle.com
############################

"""
This is the main file which calls other file to setup the sharding.
"""

from oralogger import *
from orafactory import *
from oraenv import *
from oracommon import *


def main(): 

   # Checking Comand line Args
   try:
      opts, args = getopt.getopt(sys.argv[1:], '', ['addshard=','deleteshard=','validateshard=','checkliveness=','resetlistener=','restartdb=','createdir=','optype=','addshardgroup=','deployshard=','help'])
   except getopt.GetoptError:
      pass
  
   # Initializing oraenv instance 
   oenv=OraEnv()
   file_name  = os.path.basename(__file__)
   funcname = sys._getframe(1).f_code.co_name

   log_file_name = oenv.logfile_name()

   # Initialiing logger instance
   oralogger  = OraLogger(log_file_name)
   console_handler = CHandler()
   file_handler = FHandler()
   stdout_handler = StdHandler()
   # Setting next log handlers
   stdout_handler.nextHandler = file_handler
   file_handler.nextHandler = console_handler
   console_handler.nextHandler = PassHandler()

   for opt, arg in opts:
      if opt in ('--help'):
         oralogger.msg_ = '''{:^17}-{:^17} : You can pass parameter --addshard, --deleteshard, --validateshard, --checkliveness, --resetlistener, --restartdb, --createdir, --optype, --addshardgroup, --deployshard, or --help'''
         stdout_handler.handle(oralogger)
      elif opt in ('--addshard'):
           oenv.add_custom_variable("ADD_SHARD",arg)
      elif opt in ('--validateshard'):
          oenv.add_custom_variable("VALIDATE_SHARD",arg)
      elif opt in ('--deleteshard'):
          oenv.add_custom_variable("REMOVE_SHARD",arg)
      elif opt in ('--checkliveness'):
          oenv.add_custom_variable("CHECK_LIVENESS",arg)
      elif opt in ('--resetlistener'):
          oenv.add_custom_variable("RESET_LISTENER",arg)
      elif opt in ('--restartdb'):
          oenv.add_custom_variable("RESTART_DB",arg)
      elif opt in ('--createdir'):
          oenv.add_custom_variable("CREATE_DIR",arg)
      elif opt in ('--optype'):
          oenv.add_custom_variable("OP_TYPE",arg)
      elif opt in ('--addshardgroup'):
          oenv.add_custom_variable("ADD_SGROUP_PARAMS",arg)
      elif opt in ('--deployshard'):
          oenv.add_custom_variable("DEPLOY_SHARD",arg)
      else:
         pass

   ocommon = OraCommon(oralogger,stdout_handler,oenv)
   # Initializing orafactory instances   
   oralogger.msg_ = '''{:^17}-{:^17} : Calling OraFactory to start the setup'''.format(file_name,funcname)
   stdout_handler.handle(oralogger)
   orafactory = OraFactory(oralogger,stdout_handler,oenv,ocommon)
   
   # Get the ora objects
   ofactory=orafactory.get_ora_objs()

   # Traverse through returned factory objects and execute the setup function
   for obj in ofactory:
       obj.setup()
    
# Using the special variable  
if __name__=="__main__": 
    main() 
