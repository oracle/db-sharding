#!/usr/bin/python
# LICENSE UPL 1.0
#
# Copyright (c) 2020,2021 Oracle and/or its affiliates.
#
# Since: January, 2020
# Author: sanjay.singh@oracle.com, paramdeep.saini@oracle.com

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
      opts, args = getopt.getopt(sys.argv[1:], '', ['addshard=','deleteshard=','validateshard=','checkliveness=','resetlistener=','restartdb=','createdir=','optype=','addshardgroup=','deployshard=','movechunks=','checkonlineshard=','cancelchunks=','checkchunks=','checkgsmshard=','checkreadyness=','validatenochunks=','invitednode=','resetpassword=','exporttdekey=','importtdekey=','help'])
   except getopt.GetoptError:
      pass
  
   # Initializing oraenv instance 
   oenv=OraEnv()
   file_name  = os.path.basename(__file__)
   funcname = sys._getframe(1).f_code.co_name

   log_file_name = oenv.logfile_name("NONE")

   # Initialiing logger instance
   oralogger  = OraLogger(log_file_name)
   console_handler = CHandler()
   file_handler = FHandler()
   stdout_handler = StdHandler()
   # Setting next log handlers
   stdout_handler.nextHandler = file_handler
   file_handler.nextHandler = console_handler
   console_handler.nextHandler = PassHandler()

   ocommon = OraCommon(oralogger,stdout_handler,oenv)

   for opt, arg in opts:
      if opt in ('--help'):
         oralogger.msg_ = '''{:^17}-{:^17} : You can pass parameter --addshard, --deleteshard, --validateshard, --checkliveness, --resetlistener, --restartdb, --createdir, --optype, --addshardgroup, --deployshard, '--checkonlineshard', '--cancelchunks', '--movechunks', '--checkchunks', '--checkgsmshard','--validatenochunks', '--checkreadyness','--invitednode', '--resetpassword','--exporttdekey','--importtdekey',or --help'''
         stdout_handler.handle(oralogger)
      elif opt in ('--addshard'):
           file_name = oenv.logfile_name("ADD_SHARD")   
           oralogger.filename_ =  file_name
           ocommon.log_info_message("=======================================================================",file_name)
           oenv.add_custom_variable("ADD_SHARD",arg)
      elif opt in ('--validateshard'):
           file_name = oenv.logfile_name("VALIDATE_SHARD")  
           oralogger.filename_ =  file_name    
           ocommon.log_info_message("=======================================================================",file_name)
           oenv.add_custom_variable("VALIDATE_SHARD",arg)
      elif opt in ('--deleteshard'):
           file_name = oenv.logfile_name("REMOVE_SHARD")  
           oralogger.filename_ =  file_name
           ocommon.log_info_message("=======================================================================",file_name)
           oenv.add_custom_variable("REMOVE_SHARD",arg)
      elif opt in ('--checkliveness'):
           oralogger.stdout_ = None
           file_name = oenv.logfile_name("CHECK_LIVENESS")  
           oralogger.filename_ =  file_name
           ocommon.log_info_message("=======================================================================",file_name)
           oenv.add_custom_variable("CHECK_LIVENESS",arg)
      elif opt in ('--checkreadyness'):
           oralogger.stdout_ = None
           file_name = oenv.logfile_name("CHECK_READYNESS")  
           oralogger.filename_ =  file_name
           ocommon.log_info_message("=======================================================================",file_name)
           oenv.add_custom_variable("CHECK_READYNESS",arg)
      elif opt in ('--resetlistener'):
           file_name = oenv.logfile_name("RESET_LISTENER")  
           oralogger.filename_ =  file_name
           ocommon.log_info_message("=======================================================================",file_name)
           oenv.add_custom_variable("RESET_LISTENER",arg)
      elif opt in ('--restartdb'):
           file_name = oenv.logfile_name("RESTART_DB")
           oralogger.filename_ =  file_name
           ocommon.log_info_message("=======================================================================",file_name)
           oenv.add_custom_variable("RESTART_DB",arg)
      elif opt in ('--createdir'):
           file_name = oenv.logfile_name("CREATE_DIR")
           oralogger.filename_ =  file_name
           ocommon.log_info_message("=======================================================================",file_name)
           oenv.add_custom_variable("CREATE_DIR",arg)
      elif opt in ('--addshardgroup'):
           file_name = oenv.logfile_name("ADD_SGROUP_PARAMS")
           oralogger.filename_ =  file_name
           ocommon.log_info_message("=======================================================================",file_name)
           oenv.add_custom_variable("ADD_SGROUP_PARAMS",arg)
      elif opt in ('--deployshard'):
           file_name = oenv.logfile_name("DEPLOY_SHARD")
           oralogger.filename_ =  file_name
           ocommon.log_info_message("=======================================================================",file_name)
           oenv.add_custom_variable("DEPLOY_SHARD",arg)
      elif opt in ('--cancelchunks'):
           file_name = oenv.logfile_name("CANCEL_CHUNKS")
           oralogger.filename_ =  file_name
           ocommon.log_info_message("=======================================================================",file_name)
           oenv.add_custom_variable("CANCEL_CHUNKS",arg)
      elif opt in ('--movechunks'):
           file_name = oenv.logfile_name("MOVE_CHUNKS")
           oralogger.filename_ =  file_name
           ocommon.log_info_message("=======================================================================",file_name)
           oenv.add_custom_variable("MOVE_CHUNKS",arg)
      elif opt in ('--checkchunks'):
           file_name = oenv.logfile_name("CHECK_CHUNKS")
           oralogger.filename_ =  file_name
           ocommon.log_info_message("=======================================================================",file_name)
           oenv.add_custom_variable("CHECK_CHUNKS",arg)
      elif opt in ('--validatenochunks'):
           file_name = oenv.logfile_name("VALIDATE_NOCHUNKS")
           oralogger.filename_ =  file_name
           ocommon.log_info_message("=======================================================================",file_name)
           oenv.add_custom_variable("VALIDATE_NOCHUNKS",arg)
      elif opt in ('--checkonlineshard'):
           file_name = oenv.logfile_name("CHECK_ONLINE_SHARD")
           oralogger.filename_ =  file_name
           ocommon.log_info_message("=======================================================================",file_name)
           oenv.add_custom_variable("CHECK_ONLINE_SHARD",arg)
      elif opt in ('--checkgsmshard'):
           file_name = oenv.logfile_name("CHECK_GSM_SHARD")
           oralogger.filename_ =  file_name
           ocommon.log_info_message("=======================================================================",file_name)
           oenv.add_custom_variable("CHECK_GSM_SHARD",arg)
      elif opt in ('--invitednode'):
           file_name = oenv.logfile_name("INVITED_NODE_OP")
           oralogger.filename_ =  file_name
           ocommon.log_info_message("=======================================================================",file_name)
           oenv.add_custom_variable("INVITED_NODE_OP",arg)
      elif opt in ('--resetpassword'):
           file_name = oenv.logfile_name("RESET_PASSWD")
           oralogger.filename_ =  file_name
           ocommon.log_info_message("=======================================================================",file_name)
           oenv.add_custom_variable("RESET_PASSWORD",arg)
      elif opt in ('--exporttdekey'):
           file_name = oenv.logfile_name("EXPORT_TDE_KEY")
           oralogger.filename_ =  file_name
           ocommon.log_info_message("=======================================================================",file_name)
           oenv.add_custom_variable("EXPORT_TDE_KEY",arg)
      elif opt in ('--importtdekey'):
           file_name = oenv.logfile_name("IMPORT_TDE_KEY")
           oralogger.filename_ =  file_name
           ocommon.log_info_message("=======================================================================",file_name)
           oenv.add_custom_variable("IMPORT_TDE_KEY",arg)
      elif opt in ('--optype'):
          oenv.add_custom_variable("OP_TYPE",arg)
      else:
         pass

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
