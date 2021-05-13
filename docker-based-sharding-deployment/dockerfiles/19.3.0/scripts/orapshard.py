#!/usr/bin/python

#############################
# Copyright 2020, Oracle Corporation and/or affiliates.  All rights reserved.
# Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl
# Author: paramdeep.saini@oracle.com
############################


import os
import sys
import os.path
import re
import socket
from oralogger import *
from oraenv import *
from oracommon import *
from oramachine import *
import traceback

class OraPShard:
      """
      This calss setup the primary shard after DB installation.
      """
      def __init__(self,oralogger,orahandler,oraenv,oracommon):
        """
        This constructor of OraPShard class to setup the shard on primary DB.

        Attributes:
           oralogger (object): object of OraLogger Class.
           ohandler (object): object of Handler class.
           oenv (object): object of singleton OraEnv class.
           ocommon(object): object of OraCommon class.
           ora_env_dict(dict): Dict of env variable populated based on env variable for the setup.
           file_name(string): Filename from where logging message is populated.
        """
        try:
          self.ologger             = oralogger
          self.ohandler            = orahandler
          self.oenv                = oraenv.get_instance()
          self.ocommon             = oracommon
          self.ora_env_dict        = oraenv.get_env_vars()
          self.file_name           = os.path.basename(__file__)
          self.omachine            = OraMachine(self.ologger,self.ohandler,self.oenv,self.ocommon)
        except BaseException as ex:
          ex_type, ex_value, ex_traceback = sys.exc_info()
          trace_back = traceback.extract_tb(ex_traceback)
          stack_trace = list()
          for trace in trace_back:
              stack_trace.append("File : %s , Line : %d, Func.Name : %s, Message : %s" % (trace[0], trace[1], trace[2], trace[3]))
          ocommon.log_info_message(ex_type.__name__,self.file_name)
          ocommon.log_info_message(ex_value,self.file_name)
          ocommon.log_info_message(stack_trace,self.file_name)
      def setup(self):
          """
           This function setup the shard on Primary DB.
          """
          if self.ocommon.check_key("RESET_LISTENER",self.ora_env_dict):
            status = self.shard_setup_check()
            if not status:
               self.ocommon.log_info_message("Primary shard is still not setup.Exiting...",self.file_name)
               self.ocommon.prog_exit("127")
            self.reset_listener()
            self.restart_listener()
          if self.ocommon.check_key("RESTART_DB",self.ora_env_dict):
            status = self.shard_setup_check()
            if not status:
               self.ocommon.log_info_message("Primary shard is still not setup.Exiting...",self.file_name)
               self.ocommon.prog_exit("127")
            else:
               self.ocommon.shutdown_db(self.ora_env_dict)
               self.ocommon.start_db(self.ora_env_dict) 
          elif self.ocommon.check_key("CHECK_LIVENESS",self.ora_env_dict):
            status = self.shard_setup_check()
            if not status:
               self.ocommon.prog_exit("127")
            self.ocommon.log_info_message("Shard liveness check completed sucessfully!",self.file_name)
            sys.exit(0)
          elif self.ocommon.check_key("CREATE_DIR",self.ora_env_dict):
            status = self.shard_setup_check()
            if not status:
               self.ocommon.prog_exit("127")
            self.ocommon.create_dir(self.ora_env_dict["CREATE_DIR"],True,None,None) 
          else: 
            self.setup_machine() 
            self.db_checks()
            self.reset_shard_setup()
            status = self.shard_setup_check()
            if status:
              self.ocommon.log_info_message("Shard Setup is already completed on this database",self.file_name)
            else:
              self.reset_passwd()
              self.setup_cdb_shard()
              self.setup_pdb_shard ()
              self.update_shard_setup()
              self.set_primary_listener()
              self.restart_listener()
              self.register_services()
              self.list_services()
              self.backup_files() 
              self.gsm_completion_message()
              self.run_custom_scripts()

      ###########  SETUP_MACHINE begins here ####################
      ## Function to machine setup
      def setup_machine(self):
          """
           This function performs the compute before performing setup
          """
          self.omachine.setup()
      ###########  SETUP_MACHINE ENDS here ####################

      ###########  DB_CHECKS  Related Functions Begin Here  ####################
      ## Function to perfom DB checks ######
      def db_checks(self):
          """
           This function perform db checks before starting the setup
          """
          self.ohome_check()
          self.passwd_check()
          self.set_user()
          self.sid_check()
          self.dbuinque_name_check()
          self.hostname_check()
          self.dbport_check()
          self.dbr_dest_checks()
          self.dpump_dir_checks()
 
      def ohome_check(self):
          """
             This function performs the oracle home related checks
          """
          if self.ocommon.check_key("ORACLE_HOME",self.ora_env_dict):
             self.ocommon.log_info_message("ORACLE_HOME variable is set. Check Passed!",self.file_name)   
          else:
             self.ocommon.log_error_message("ORACLE_HOME variable is not set. Exiting!",self.file_name)
             self.ocommon.prog_exit()

          if os.path.isdir(self.ora_env_dict["ORACLE_HOME"]):
             msg='''ORACLE_HOME {0} dirctory exist. Directory Check passed!'''.format(self.ora_env_dict["ORACLE_HOME"])
             self.ocommon.log_info_message(msg,self.file_name)
          else:
             msg='''ORACLE_HOME {0} dirctory does not exist. Directory Check Failed!'''.format(self.ora_env_dict["ORACLE_HOME"])
             self.ocommon.log_error_message(msg,self.file_name)
             self.ocommon.prog_exit()

      def passwd_check(self):
           """
           This funnction perform password related checks
           """
           passwd_file_flag = False
           if self.ocommon.check_key("SECRET_VOLUME",self.ora_env_dict) and self.ocommon.check_key("COMMON_OS_PWD_FILE",self.ora_env_dict) and self.ocommon.check_key("PWD_KEY",self.ora_env_dict):
              msg='''SECRET_VOLUME passed as an env variable and set to {0}'''.format(self.ora_env_dict["SECRET_VOLUME"])
           else:
              self.ora_env_dict=self.ocommon.add_key("SECRET_VOLUME","/run/secrets",self.ora_env_dict) 
              msg='''SECRET_VOLUME not passed as an env variable. Setting default to {0}'''.format(self.ora_env_dict["SECRET_VOLUME"])

           self.ocommon.log_warn_message(msg,self.file_name)

           if self.ocommon.check_key("COMMON_OS_PWD_FILE",self.ora_env_dict):
              msg='''COMMON_OS_PWD_FILE passed as an env variable and set to {0}'''.format(self.ora_env_dict["COMMON_OS_PWD_FILE"])
           else:
              self.ora_env_dict=self.ocommon.add_key("COMMON_OS_PWD_FILE","common_os_pwdfile.enc",self.ora_env_dict)
              msg='''COMMON_OS_PWD_FILE not passed as an env variable. Setting default to {0}'''.format(self.ora_env_dict["COMMON_OS_PWD_FILE"])

           self.ocommon.log_warn_message(msg,self.file_name)
 
           if self.ocommon.check_key("PWD_KEY",self.ora_env_dict):
              msg='''PWD_KEY passed as an env variable and set to {0}'''.format(self.ora_env_dict["PWD_KEY"])
           else:
              self.ora_env_dict=self.ocommon.add_key("PWD_KEY","pwd.key",self.ora_env_dict)
              msg='''PWD_KEY not passed as an env variable. Setting default to {0}'''.format(self.ora_env_dict["PWD_KEY"])

           self.ocommon.log_warn_message(msg,self.file_name)
              
           secret_volume = self.ora_env_dict["SECRET_VOLUME"]
           common_os_pwd_file = self.ora_env_dict["COMMON_OS_PWD_FILE"]
           pwd_key = self.ora_env_dict["PWD_KEY"]
           passwd_file='''{0}/{1}'''.format(self.ora_env_dict["SECRET_VOLUME"],self.ora_env_dict["COMMON_OS_PWD_FILE"])
           if os.path.isfile(passwd_file):
              msg='''Passwd file {0} exist. Password file Check passed!'''.format(passwd_file)
              self.ocommon.log_info_message(msg,self.file_name)
              msg='''Reading encrypted passwd from file {0}.'''.format(passwd_file)
              self.ocommon.log_info_message(msg,self.file_name)
              cmd='''openssl enc -d -aes-256-cbc -in \"{0}/{1}\" -out /tmp/{1} -pass file:\"{0}/{2}\"'''.format(secret_volume,common_os_pwd_file,pwd_key)
              output,error,retcode=self.ocommon.execute_cmd(cmd,None,None)
              self.ocommon.check_os_err(output,error,retcode,True) 
              passwd_file_flag = True

           if not passwd_file_flag:
              s = "abcdefghijklmnopqrstuvwxyz01234567890ABCDEFGHIJKLMNOPQRSTUVWXYZ!@#$%^&*()?"
              passlen = 8
              password  =  "".join(random.sample(s,passlen ))
           else:
              fname='''/tmp/{0}'''.format(common_os_pwd_file)
              fdata=self.ocommon.read_file(fname)
              password=fdata

           if self.ocommon.check_key("ORACLE_PWD",self.ora_env_dict):
              msg="ORACLE_PWD is passed as an env variable. Check Passed!"
              self.ocommon.log_info_message(msg,self.file_name)              
           else:
              self.ora_env_dict=self.ocommon.add_key("ORACLE_PWD",password,self.ora_env_dict)
              msg="ORACLE_PWD set to HIDDEN_STRING generated using encrypted password file"
              self.ocommon.log_info_message(msg,self.file_name)

      def set_user(self):
           """
           This funnction set the user for pdb and cdb.
           """
           if self.ocommon.check_key("SHARD_ADMIN_USER",self.ora_env_dict):
               msg='''SHARD_ADMIN_USER {0} is passed as an env variable. Check Passed!'''.format(self.ora_env_dict["SHARD_ADMIN_USER"])
               self.ocommon.log_info_message(msg,self.file_name)
           else:
               self.ora_env_dict=self.ocommon.add_key("SHARD_ADMIN_USER","mysdbadmin",self.ora_env_dict)
               msg="SHARD_ADMIN_USER is not set, setting default to mysdbadmin"
               self.ocommon.log_info_message(msg,self.file_name)

           if self.ocommon.check_key("PDB_ADMIN_USER",self.ora_env_dict):
               msg='''PDB_ADMIN_USER {0} is passed as an env variable. Check Passed!'''.format(self.ora_env_dict["PDB_ADMIN_USER"])
               self.ocommon.log_info_message(msg,self.file_name)
           else:
               self.ora_env_dict=self.ocommon.add_key("PDB_ADMIN_USER","PDBADMIN",self.ora_env_dict)
               msg="PDB_ADMIN_USER is not set, setting default to PDBADMIN."
               self.ocommon.log_info_message(msg,self.file_name)

      def sid_check(self):
           """
           This funnction heck and set the SID for cdb and PDB.
           """
           if self.ocommon.check_key("ORACLE_SID",self.ora_env_dict):
               msg='''ORACLE_SID {0} is passed as an env variable. Check Passed!'''.format(self.ora_env_dict["ORACLE_SID"])
               self.ocommon.log_info_message(msg,self.file_name)
           else:
               msg="ORACLE_SID is not set, existing!"
               self.ocommon.log_error_message(msg,self.file_name)
               self.ocommon.prog_exit()

      def dbuinque_name_check(self):
           """
           This funnction check and set the db unique name for standby
           """
           if self.ocommon.check_key("DB_UNIQUE_NAME",self.ora_env_dict):
               msg='''DB_UNIQUE_NAME {0} is passed as an env variable. Check Passed!'''.format(self.ora_env_dict["DB_UNIQUE_NAME"])
               self.ocommon.log_info_message(msg,self.file_name)
           else:
               msg="DB_UNIQUE_NAME is not set. Setting DB_UNIQUE_NAME to Oracle_SID"
               self.ocommon.log_info_message(msg,self.file_name)
               dbsid=self.ora_env_dict["ORACLE_SID"]
               self.ora_env_dict=self.ocommon.add_key("DB_UNIQUE_NAME",dbsid,self.ora_env_dict)

      def hostname_check(self):
           """
           This function check and set the hostname.
           """
           if self.ocommon.check_key("ORACLE_HOSTNAME",self.ora_env_dict):
              msg='''ORACLE_HOSTNAME {0} is passed as an env variable. Check Passed!'''.format(self.ora_env_dict["ORACLE_HOSTNAME"])
              self.ocommon.log_info_message(msg,self.file_name)
           else:
              if self.ocommon.check_key("KUBE_SVC",self.ora_env_dict):
                 hostname='''{0}.{1}'''.format(socket.gethostname(),self.ora_env_dict["KUBE_SVC"])
              else:
                 hostname='''{0}'''.format(socket.gethostname())
              msg='''ORACLE_HOSTNAME is not set, setting it to hostname {0} of the compute!'''.format(hostname)
              self.ora_env_dict=self.ocommon.add_key("ORACLE_HOSTNAME",hostname,self.ora_env_dict)                 
              self.ocommon.log_info_message(msg,self.file_name) 

      def dbport_check(self):
           """
           This funnction checks and set the SID for cdb and PDB.
           """
           if self.ocommon.check_key("DB_PORT",self.ora_env_dict):
               msg='''DB_PORT {0} is passed as an env variable. Check Passed!'''.format(self.ora_env_dict["DB_PORT"])
               self.ocommon.log_info_message(msg,self.file_name)
           else:
               self.ora_env_dict=self.ocommon.add_key("DB_PORT","1521",self.ora_env_dict)
               msg="DB_PORT is not set, setting default to 1521"
               self.ocommon.log_info_message(msg,self.file_name)

      def dbr_dest_checks(self):
           """
           This funnction checks and set the DB_CREATE_FILE_DEST and DB_CREATE_FILE_DEST_SIZE.
           """
           if self.ocommon.check_key("DB_RECOVERY_FILE_DEST",self.ora_env_dict):
               msg='''DB_RECOVERY_FILE_DEST {0} is passed as an env variable. Check Passed!'''.format(self.ora_env_dict["DB_RECOVERY_FILE_DEST"])
               self.ocommon.log_info_message(msg,self.file_name)
           else:
               dest='''{0}/oradata/fast_recovery_area/{1}'''.format(self.ora_env_dict["ORACLE_BASE"],self.ora_env_dict["ORACLE_SID"])
               self.ora_env_dict=self.ocommon.add_key("DB_RECOVERY_FILE_DEST",dest,self.ora_env_dict)
               msg='''DB_RECOVERY_FILE_DEST set to {0}'''.format(dest)
               self.ocommon.log_info_message(msg,self.file_name)
           msg='''Checking dir {0} on local machine. If not then create the dir {0} on local machine'''.format(self.ora_env_dict["DB_RECOVERY_FILE_DEST"])
           self.ocommon.log_info_message(msg,self.file_name) 
           self.ocommon.create_dir(self.ora_env_dict["DB_RECOVERY_FILE_DEST"],True,None,None)
  
           # Checking the DB_RECOVERY_FILE_DEST_SIZE
 
           if self.ocommon.check_key("DB_RECOVERY_FILE_DEST_SIZE",self.ora_env_dict):
               msg='''DB_RECOVERY_FILE_DEST_SIZE {0} is passed as an env variable. Check Passed!'''.format(self.ora_env_dict["DB_RECOVERY_FILE_DEST_SIZE"])
               self.ocommon.log_info_message(msg,self.file_name)
           else:
               self.ora_env_dict=self.ocommon.add_key("DB_RECOVERY_FILE_DEST_SIZE","40G",self.ora_env_dict)
               msg='''DB_RECOVERY_FILE_DEST_SIZE set to {0}'''.format("40G")
               self.ocommon.log_info_message(msg,self.file_name)

           # Checking the DB_CREATE_FILE_DEST

           if self.ocommon.check_key("DB_CREATE_FILE_DEST",self.ora_env_dict):
               msg='''DB_CREATE_FILE_DEST {0} is passed as an env variable. Check Passed!'''.format(self.ora_env_dict["DB_CREATE_FILE_DEST"])
               self.ocommon.log_info_message(msg,self.file_name)
           else:
               dest='''{0}/oradata/{1}'''.format(self.ora_env_dict["ORACLE_BASE"],self.ora_env_dict["ORACLE_SID"])
               self.ora_env_dict=self.ocommon.add_key("DB_CREATE_FILE_DEST",dest,self.ora_env_dict)
               msg='''DB_CREATE_FILE_DEST set to {0}'''.format("40")
               self.ocommon.log_info_message(msg,self.file_name)
           msg='''Checking dir {0} on local machine. If not then create the dir {0} on local machine'''.format(self.ora_env_dict["DB_CREATE_FILE_DEST"])
           self.ocommon.log_info_message(msg,self.file_name)
           self.ocommon.create_dir(self.ora_env_dict["DB_CREATE_FILE_DEST"],True,None,None)


      def dpump_dir_checks(self):
           """
           This funnction checks and set the DATA_PUMP dir and location.
           """
           if self.ocommon.check_key("DATA_PUMP_DIR",self.ora_env_dict):
               msg='''DATA_PUMP_DIR {0} is passed as an env variable. Check Passed!'''.format(self.ora_env_dict["DATA_PUMP_DIR"])
               self.ocommon.log_info_message(msg,self.file_name)
           else:
               dest='''{0}/oradata/data_pump_dir'''.format(self.ora_env_dict["ORACLE_BASE"])
               self.ora_env_dict=self.ocommon.add_key("DATA_PUMP_DIR",dest,self.ora_env_dict)
               msg='''DATA_PUMP_DIR set to {0}'''.format(dest)
               self.ocommon.log_info_message(msg,self.file_name)
           msg='''Checking dir {0} on local machine. If not then create the dir {0} on local machine'''.format(self.ora_env_dict["DATA_PUMP_DIR"])
           self.ocommon.log_info_message(msg,self.file_name)
           self.ocommon.create_dir(self.ora_env_dict["DATA_PUMP_DIR"],True,None,None)

       ###########  DB_CHECKS  Related Functions Begin Here  #################### 

            
       ########## RESET_PASSWORD function Begin here #############################
       ## Function to perform password reset
      def reset_passwd(self):
         """
           This function reset the password.
         """ 
         password_script='''{0}/{1}'''.format(self.ora_env_dict["HOME"],"setPassword.sh")
         self.ocommon.log_info_message("Executing password reset", self.file_name)
         if self.ocommon.check_key("ORACLE_PWD",self.ora_env_dict) and self.ocommon.check_key("HOME",self.ora_env_dict) and os.path.isfile(password_script):
            cmd='''{0} {1} '''.format(password_script,'HIDDEN_STRING')
            self.ocommon.set_mask_str(self.ora_env_dict["ORACLE_PWD"])
            output,error,retcode=self.ocommon.execute_cmd(cmd,None,None)
            self.ocommon.check_os_err(output,error,retcode,True)
            self.ocommon.unset_mask_str()
         else:
            msg='''Error Occurred! Either HOME DIR {0} does not exist, ORACLE_PWD {1} is not set or PASSWORD SCRIPT {2} does not exist'''.format(self.ora_env_dict["HOME"],self.ora_env_dict["ORACLE_PWD"],password_script)  
            self.ocommon.log_error_message(msg,self.file_name)
            self.oracommon.prog_exit()

       ########## RESET_PASSWORD function ENDS here #############################

       ########## SETUP_CDB_SHARD FUNCTION BEGIN HERE ###############################

      def reset_shard_setup(self):
           """
            This function drop teh shard setup table and reste the env to default values. 
           """
      #     systemStr='''{0}/bin/sqlplus {1}/{2}'''.format(self.ora_env_dict["ORACLE_HOME"],"system",self.ora_env_dict["ORACLE_PWD"])
           sqlpluslogincmd='''{0}/bin/sqlplus "/as sysdba"'''.format(self.ora_env_dict["ORACLE_HOME"])
           self.ocommon.log_info_message("Inside reset_shard_setup",self.file_name)
           shard_reset_file='''{0}/.shard/reset_shard_completed'''.format(self.ora_env_dict["HOME"])
           if self.ocommon.check_key("RESET_ENV",self.ora_env_dict):
              if self.ora_env_dict["RESET_ENV"]:
                 if not os.path.isfile(shard_reset_file):
                    msg='''Dropping shardsetup table from CDB'''
                    self.ocommon.log_info_message(msg,self.file_name)
                    sqlcmd='''
                     drop table system.shardsetup;
                    '''
                    output,error,retcode=self.ocommon.run_sqlplus(sqlpluslogincmd,sqlcmd,None)
                    self.ocommon.log_info_message("Calling check_sql_err() to validate the sql command return status",self.file_name)
                    self.ocommon.check_sql_err(output,error,retcode,True)
                 else:
                    msg='''Reset env already completed on this enviornment as {0} exist on this machine and not executing env reset'''.format(shard_reset_file)
                    self.ocommon.log_info_message(msg,self.file_name)                    


      def shard_setup_check(self):
           """
            This function check the shard status.
           """
           systemStr='''{0}/bin/sqlplus "/as sysdba"'''.format(self.ora_env_dict["ORACLE_HOME"])

        #   self.ocommon.set_mask_str(self.ora_env_dict["ORACLE_PWD"])
           msg='''Checking shardsetup table in CDB'''
           self.ocommon.log_info_message(msg,self.file_name)
           sqlcmd='''
            set heading off
            set feedback off
            set  term off
            SET NEWPAGE NONE
            spool /tmp/shard_setup.txt
            select * from system.shardsetup WHERE ROWNUM = 1; 
            spool off
            exit;
           '''
           output,error,retcode=self.ocommon.run_sqlplus(systemStr,sqlcmd,None)
           # self.ocommon.log_info_message("Calling check_sql_err() to validate the sql command return status",self.file_name)
           self.ocommon.check_sql_err(output,error,retcode,None)
           fname='''/tmp/{0}'''.format("shard_setup.txt")
           fdata=self.ocommon.read_file(fname)
           ### Unsetting the encrypt value to None
         #  self.ocommon.unset_mask_str()

           if re.search('completed',fdata):
              return True
           else:
              return False

      def setup_cdb_shard(self):
           """
            This function setup the shard.
           """
           sqlpluslogincmd='''{0}/bin/sqlplus "/as sysdba"'''.format(self.ora_env_dict["ORACLE_HOME"])            
           # Assigning variable
           dbf_dest=self.ora_env_dict["DB_CREATE_FILE_DEST"]
           dbr_dest=self.ora_env_dict["DB_RECOVERY_FILE_DEST"]
           dbr_dest_size=self.ora_env_dict["DB_RECOVERY_FILE_DEST_SIZE"]
           host_name=self.ora_env_dict["ORACLE_HOSTNAME"]
           dpump_dir = self.ora_env_dict["DATA_PUMP_DIR"]
           db_port=self.ora_env_dict["DB_PORT"]
           obase=self.ora_env_dict["ORACLE_BASE"]
           dbuname=self.ora_env_dict["DB_UNIQUE_NAME"] 
                 
           self.ocommon.set_mask_str(self.ora_env_dict["ORACLE_PWD"])
           msg='''Setting up Shard CDB'''
           self.ocommon.log_info_message(msg,self.file_name)
           sqlcmd='''
             alter system set db_create_file_dest=\"{0}\" scope=both;
             alter system set db_recovery_file_dest_size={1} scope=both;
             alter system set db_recovery_file_dest=\"{2}\" scope=both; 
             alter system set open_links=16 scope=spfile;
             alter system set dg_broker_config_file1=\"{6}/oradata/{7}/{8}/dr2{8}.dat\" scope=spfile; 
             alter system set dg_broker_config_file2=\"{6}/oradata/{7}/{8}/dr1{8}.dat\" scope=spfile;
             alter system set open_links_per_instance=16 scope=spfile;
             alter system set db_file_name_convert='*','{0}/' scope=spfile;
             alter user gsmrootuser account unlock;
             grant sysdg to gsmrootuser;
             grant sysbackup to gsmrootuser;
             alter user gsmrootuser identified by HIDDEN_STRING  container=all;
             alter user GSMUSER account unlock;
             alter user GSMUSER  identified by HIDDEN_STRING  container=all;
             grant sysdg to GSMUSER;
             grant sysbackup to GSMUSER;
             alter system set dg_broker_start=true scope=both;
             create or replace directory DATA_PUMP_DIR as '{3}';
             grant read,write on directory DATA_PUMP_DIR to GSMADMIN_INTERNAL;
             alter system set local_listener='{4}:{5}' scope=both;
           '''.format(dbf_dest,dbr_dest_size,dbr_dest,dpump_dir,host_name,db_port,obase,"dbconfig",dbuname) 
                  
           output,error,retcode=self.ocommon.run_sqlplus(sqlpluslogincmd,sqlcmd,None)
           self.ocommon.log_info_message("Calling check_sql_err() to validate the sql command return status",self.file_name)
           self.ocommon.check_sql_err(output,error,retcode,True)

           ### Unsetting the encrypt value to None 
           self.ocommon.unset_mask_str()             
                             
           self.ocommon.log_info_message("Calling shutdown_db() to shutdown the database",self.file_name)
           self.ocommon.shutdown_db(self.ora_env_dict)
           self.ocommon.log_info_message("Calling startup_mount() to mount the database",self.file_name)
           self.ocommon.mount_db(self.ora_env_dict)
       
           self.ocommon.log_info_message("Enabling archivelog at DB level",self.file_name)              
           sqlcmd='''
           alter database archivelog;
           alter database open;
           '''
           output,error,retcode=self.ocommon.run_sqlplus(sqlpluslogincmd,sqlcmd,None)
           self.ocommon.log_info_message("Calling check_sql_err() to validate the sql command return status",self.file_name)
           self.ocommon.check_sql_err(output,error,retcode,True)

           self.ocommon.log_info_message("Enabling flashback and force logging at DB level",self.file_name)
           sqlcmd='''
           alter database flashback on;
           alter database force logging;
           '''
           output,error,retcode=self.ocommon.run_sqlplus(sqlpluslogincmd,sqlcmd,None)
           self.ocommon.log_info_message("Calling check_sql_err() to validate the sql command return status",self.file_name)
           self.ocommon.check_sql_err(output,error,retcode,None)

           self.ocommon.log_info_message("Opening PDB",self.file_name)
           sqlcmd='''
           ALTER PLUGGABLE DATABASE ALL OPEN;
           '''
           output,error,retcode=self.ocommon.run_sqlplus(sqlpluslogincmd,sqlcmd,None)
           self.ocommon.log_info_message("Calling check_sql_err() to validate the sql command return status",self.file_name)
           self.ocommon.check_sql_err(output,error,retcode,None)

      def setup_pdb_shard(self):
           """
            This function setup the shard.
           """
           sqlpluslogincmd='''{0}/bin/sqlplus "/as sysdba"'''.format(self.ora_env_dict["ORACLE_HOME"])
           # Assigning variable
           if self.ocommon.check_key("ORACLE_PDB",self.ora_env_dict):
              msg='''Setting up Shard PDB'''
              self.ocommon.log_info_message(msg,self.file_name)
              sqlcmd='''
              alter pluggable database {0} close immediate;
              alter pluggable database {0} open services=All;
              ALTER PLUGGABLE DATABASE {0} SAVE STATE;
              alter system register;
              alter session set container={0};
              grant read,write on directory DATA_PUMP_DIR to GSMADMIN_INTERNAL;
              grant sysdg to GSMUSER;
              grant sysbackup to GSMUSER;
              execute DBMS_GSM_FIX.validateShard;
              alter system register;
              '''.format(self.ora_env_dict["ORACLE_PDB"])

              output,error,retcode=self.ocommon.run_sqlplus(sqlpluslogincmd,sqlcmd,None)
              self.ocommon.log_info_message("Calling check_sql_err() to validate the sql command return status",self.file_name)
              self.ocommon.check_sql_err(output,error,retcode,True)
              

      def update_shard_setup(self):
           """
            This function update the shard setup on this DB.
           """
       #    systemStr='''{0}/bin/sqlplus {1}/{2}'''.format(self.ora_env_dict["ORACLE_HOME"],"system","HIDDEN_STRING")
           systemStr='''{0}/bin/sqlplus "/as sysdba"'''.format(self.ora_env_dict["ORACLE_HOME"])

#           self.ocommon.set_mask_str(self.ora_env_dict["ORACLE_PWD"])

           msg='''Updating shardsetup table'''
           self.ocommon.log_info_message(msg,self.file_name)
           sqlcmd='''
            set heading off
            set feedback off
            create table system.shardsetup (status varchar2(10));
            insert into system.shardsetup values('completed');
            commit; 
            exit;
           '''
           output,error,retcode=self.ocommon.run_sqlplus(systemStr,sqlcmd,None)
           self.ocommon.log_info_message("Calling check_sql_err() to validate the sql command return status",self.file_name)
           self.ocommon.check_sql_err(output,error,retcode,True)

           ### Reset File
           shard_reset_dir='''{0}/.shard'''.format(self.ora_env_dict["HOME"]) 
           shard_reset_file='''{0}/.shard/reset_shard_completed'''.format(self.ora_env_dict["HOME"])

           self.ocommon.log_info_message("Creating reset_file_fir if it does not exist",self.file_name)
           if not os.path.isdir(shard_reset_dir):
              self.ocommon.create_dir(shard_reset_dir,True,None,None)

           if not os.path.isfile(shard_reset_file):
              self.ocommon.create_file(shard_reset_file,True,None,None)
 
#          self.ocommon.unset_mask_str()
        
       ########## SETUP_CDB_SHARD FUNCTION ENDS HERE ###############################
          ###################################### Run custom scripts ##################################################
      def run_custom_scripts():
          """
           Custom script to be excuted on every restart of enviornment
          """
          self.ocommon.log_info_message("Inside run_custom_scripts()",self.file_name)
          if self.ocommon.check_key("CUSTOM_SHARD_SCRIPT_DIR",self.ora_env_dict):
             shard_dir=self.ora_env_dict["CUSTOM_SHARD_SCRIPT_DIR"]

          if self.ocommon.check_key("CUSTOM_SHARD_SCRIPT_FILE",self.ora_env_dict):
             shard_file=self.ora_env_dict["CUSTOM_SHARD_SCRIPT_FILE"]

          script_file = '''{0}/{1}'''.format(shard_dir,shard_file)

          if os.path.isfile(script_file):
             msg='''Custom shard script exist {0}'''.format(script_file)
             self.ocommon.log_info_message(msg,self.file_name)
             cmd='''sh {0}'''.format(script_file)
             output,error,retcode=self.ocommon.execute_cmd(cmd,None,None)
             self.ocommon.check_os_err(output,error,retcode,True)

          ###################################### Run custom scripts ##################################################
      def run_custom_scripts(self):
          """
           Custom script to be excuted on every restart of enviornment
          """
          self.ocommon.log_info_message("Inside run_custom_scripts()",self.file_name)
          if self.ocommon.check_key("CUSTOM_SHARD_SCRIPT_DIR",self.ora_env_dict):
             shard_dir=self.ora_env_dict["CUSTOM_SHARD_SCRIPT_DIR"]
             if self.ocommon.check_key("CUSTOM_SHARD_SCRIPT_FILE",self.ora_env_dict):
                shard_file=self.ora_env_dict["CUSTOM_SHARD_SCRIPT_FILE"]
                script_file = '''{0}/{1}'''.format(shard_dir,shard_file)
                if os.path.isfile(script_file):
                   msg='''Custom shard script exist {0}'''.format(script_file)
                   self.ocommon.log_info_message(msg,self.file_name)
                   cmd='''sh {0}'''.format(script_file)
                   output,error,retcode=self.ocommon.execute_cmd(cmd,None,None)
                   self.ocommon.check_os_err(output,error,retcode,True)

      ############################### GSM Completion Message #######################################################
      def gsm_completion_message(self):
          """
           Funtion print completion message
          """
          self.ocommon.log_info_message("Inside gsm_completion_message()",self.file_name)
          msg=[]
          msg.append('==============================================')
          msg.append('     GSM Sahrd Setup Completed                ')
          msg.append('==============================================')

          for text in msg:
              self.ocommon.log_info_message(text,self.file_name)
      ################################  Reset and standby functions #################################################
      
      def set_primary_listener(self):
          """
           Function to set the primary listener
          """
          global_dbname=self.ocommon.get_global_dbdomain(self.ora_env_dict["ORACLE_HOSTNAME"],self.ora_env_dict["DB_UNIQUE_NAME"] + "_DGMRL")
          self.set_db_listener(global_dbname,self.ora_env_dict["DB_UNIQUE_NAME"])
          global_dbname=self.ocommon.get_global_dbdomain(self.ora_env_dict["ORACLE_HOSTNAME"],self.ora_env_dict["DB_UNIQUE_NAME"])
          self.set_db_listener(global_dbname,self.ora_env_dict["DB_UNIQUE_NAME"])

      def set_db_listener(self,gdbname,sid):
          """
           Funtion to reset the listener
          """
          self.ocommon.log_info_message("Inside reset_listener()",self.file_name)
          start = 'SID_LIST_LISTENER'
          end = '^\)$'
          oracle_home=self.ora_env_dict["ORACLE_HOME"]
          lisora='''{0}/network/admin/listener.ora'''.format(oracle_home)
          buffer = "SID_LIST_LISTENER=" + '\n'
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
              buffer +=  self.ocommon.get_sid_desc(gdbname,oracle_home,sid,"SID_DESC1")
              listener =  self.ocommon.get_lisora(1521)
              listener += '\n' + buffer
          else:
              buffer += self.ocommon.get_sid_desc(gdbname,oracle_home,sid,"SID_DESC")
              listener =  self.ocommon.get_lisora(1521)
              listener += '\n' + buffer

          wr = open(lisora, 'w')
          wr.write(listener)

      def restart_listener(self):
          """
          restart listener
          """
          self.ocommon.log_info_message("Stopping Listener",self.file_name)
          ohome=self.ora_env_dict["ORACLE_HOME"]
          cmd='''{0}/bin/lsnrctl stop'''.format(ohome)
          output,error,retcode=self.ocommon.execute_cmd(cmd,None,None)
          self.ocommon.check_os_err(output,error,retcode,None)

          self.ocommon.log_info_message("Starting Listener",self.file_name) 
          ohome=self.ora_env_dict["ORACLE_HOME"]
          cmd='''{0}/bin/lsnrctl start'''.format(ohome)
          output,error,retcode=self.ocommon.execute_cmd(cmd,None,None)
          self.ocommon.check_os_err(output,error,retcode,None)
   
      def register_services(self):
           """
            This function setup the catalog.
           """
           sqlpluslogincmd='''{0}/bin/sqlplus "/as sysdba"'''.format(self.ora_env_dict["ORACLE_HOME"])
           # Assigning variable
           self.ocommon.set_mask_str(self.ora_env_dict["ORACLE_PWD"])
           if self.ocommon.check_key("ORACLE_PDB",self.ora_env_dict):
              msg='''Setting up catalog PDB'''
              self.ocommon.log_info_message(msg,self.file_name)
              sqlcmd='''
              alter system register;
              alter session set container={0};
              alter system register;
              exit;
              '''.format(self.ora_env_dict["ORACLE_PDB"],self.ora_env_dict["SHARD_ADMIN_USER"])

              output,error,retcode=self.ocommon.run_sqlplus(sqlpluslogincmd,sqlcmd,None)
              self.ocommon.log_info_message("Calling check_sql_err() to validate the sql command return status",self.file_name)
              self.ocommon.check_sql_err(output,error,retcode,True)

           ### Unsetting the encrypt value to None
           self.ocommon.unset_mask_str()

      def list_services(self):
          """
          restart listener
          """
          self.ocommon.log_info_message("Listing Services",self.file_name)
          ohome=self.ora_env_dict["ORACLE_HOME"]
          cmd='''{0}/bin/lsnrctl services'''.format(ohome)
          output,error,retcode=self.ocommon.execute_cmd(cmd,None,None)
          self.ocommon.check_os_err(output,error,retcode,None)
 
      def backup_files(self):
          """
           This function backup the files such as spfile, password file and other required files to a under oradata/dbconfig
          """
          self.ocommon.log_info_message("Inside backup_files_on_standby()",self.file_name)
          obase=self.ora_env_dict["ORACLE_BASE"]
          dbuname=self.ora_env_dict["DB_UNIQUE_NAME"]
          dbsid=self.ora_env_dict["ORACLE_SID"]
          ohome=self.ora_env_dict["ORACLE_HOME"]
          cmd_names='''
               mkdir -p {0}/oradata/{1}/{2}
               cp {3}/dbs/spfile{2}.ora {0}/oradata/{1}/{2}/
               cp {3}/dbs/orapw{2}   {0}/oradata/{1}/{2}/
               cp {3}/network/admin/sqlnet.ora {0}/oradata/{1}/{2}/
               cp {3}/network/admin/listener.ora {0}/oradata/{1}/{2}/
               cp {3}/network/admin/tnsnames.ora {0}/oradata/{1}/{2}/
               touch {0}/oradata/{1}/{2}/status_completed
          '''.format(obase,"dbconfig",dbuname,ohome)
          cmd_list = [y for y in (x.strip() for x in cmd_names.splitlines()) if y]
          for cmd in cmd_list:
             msg='''Executing cmd {0}'''.format(cmd)
             self.ocommon.log_info_message(msg,self.file_name)
             output,error,retcode=self.ocommon.execute_cmd(cmd,None,None)
