#!/usr/bin/python

#############################
# Copyright 2020, Oracle Corporation and/or affiliates.  All rights reserved.
# Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl
# Author: paramdeep.saini@oracle.com
############################

import os
import os.path
import re
import socket
from oralogger import *
from oraenv import *
from oracommon import *
from oramachine import *

class OraSShard:
      """
      This calss setup the primary shard after DB installation.
      """
      def __init__(self,oralogger,orahandler,oraenv,oracommon):
        """
        This constructor of OraSShard class to setup the shard on primary DB.

        Attributes:
           oralogger (object): object of OraLogger Class.
           ohandler (object): object of Handler class.
           oenv (object): object of singleton OraEnv class.
           ocommon(object): object of OraCommon class.
           ora_env_dict(dict): Dict of env variable populated based on env variable for the setup.
           file_name(string): Filename from where logging message is populated.
        """
        self.ologger             = oralogger
        self.ohandler            = orahandler
        self.oenv                = oraenv.get_instance()
        self.ocommon             = oracommon
        self.ora_env_dict        = oraenv.get_env_vars()
        self.file_name           = os.path.basename(__file__)
        self.omachine            = OraMachine(self.ologger,self.ohandler,self.oenv,self.ocommon)

      def setup(self):
          """
           This function setup the shard on Primary DB.
          """
          if self.ocommon.check_key("CHECK_LIVENESS",self.ora_env_dict):
            status = self.shard_setup_check()
            if not status:
               self.ocommon.prog_exit("127")
          else:
            self.setup_machine()
            self.db_checks()
            self.reset_shard_setup()
       #     status = self.shard_setup_check()
       #     if status:
       #       self.ocommon.log_info_message("Shard Setup is already completed on this database",self.file_name)
       #     else:
            self.standby_create_dirs()
            self.standby_remote_dirs()
            self.set_listener()
            self.restart_listener()              
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
          self.hostname_check()
          self.dbport_check()
          self.dbr_dest_checks()
          self.dpump_dir_checks()
          self.dbuinque_name_check()
   	  self.primary_hostname_check()
	  self.primary_db_port_check()
	  self.primary_db_name_check()
	  self.remote_script_check()
	  self.remote_job_check()
	  self.primary_db_check()

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
               msg="DB_UNIQUE_NAME is not set, existing!"
               self.ocommon.log_error_message(msg,self.file_name)
               self.ocommon.prog_exit()

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

      def primary_hostname_check(self):
           """
           This function check and set the hostname.
           """
           if self.ocommon.check_key("PRIMARY_HOSTNAME",self.ora_env_dict):
              msg='''PRIMARY_HOSTNAME {0} is passed as an env variable. Check Passed!'''.format(self.ora_env_dict["PRIMARY_HOSTNAME"])
              self.ocommon.log_info_message(msg,self.file_name)
           else:
              msg='''PRIMARY_HOSTNAME is not set'''
              self.ocommon.log_info_message(msg,self.file_name)
              self.ocommon.prog_exit("127")

      def primary_db_port_check(self):
           """
           This function check and set the hostname.
           """
           dport="1521"
           if self.ocommon.check_key("PRIMARY_DB_PORT",self.ora_env_dict):
              msg='''PRIMARY_DB_PORT {0} is passed as an env variable. Check Passed!'''.format(self.ora_env_dict["PRIMARY_DB_PORT"])
              self.ocommon.log_info_message(msg,self.file_name)
           else:
              msg='''PRIMARY_DB_PORT is not set'''
              self.ocommon.log_info_message(msg,self.file_name)
              self.ora_env_dict=self.ocommon.add_key("PRIMARY_DB_PORT",dport,self.ora_env_dict)
              self.ocommon.log_info_message(msg,self.file_name)
    
      def primary_db_name_check(self):
           """
           This function check and set the hostname.
           """
           if self.ocommon.check_key("PRIMARY_DB_NAME",self.ora_env_dict):
              msg='''PRIMARY_DB_NAME {0} is passed as an env variable. Check Passed!'''.format(self.ora_env_dict["PRIMARY_DB_NAME"])
              self.ocommon.log_info_message(msg,self.file_name)
           else:
              msg='''PRIMARY_DB_NAME is not set'''
              self.ocommon.log_info_message(msg,self.file_name)
              self.ocommon.prog_exit("127")

      def remote_script_check(self):
           """
           This function check the remote script.
           """
           sname='/opt/oracle/scripts/setup/main.py'
           if self.ocommon.check_key("REMOTE_SCRIPT_NAME",self.ora_env_dict):
              msg='''REMOTE_SCRIPT_NAME {0} is passed as an env variable. Check Passed!'''.format(self.ora_env_dict["REMOTE_SCRIPT_NAME"])
              self.ocommon.log_info_message(msg,self.file_name)
           else:
              msg='''REMOTE_SCRIPT_NAME is not set and setting to {0}'''.format(sname)
              self.ocommon.log_info_message(msg,self.file_name)
              self.ora_env_dict=self.ocommon.add_key("REMOTE_SCRIPT_NAME",sname,self.ora_env_dict)
              self.ocommon.log_info_message(msg,self.file_name)


      def remote_job_check(self):
           """
           This function check the remote script.
           """
           jname='OS_JOB'
           if self.ocommon.check_key("REMOTE_JOB_NAME",self.ora_env_dict):
              msg='''REMOTE_JOB_NAME {0} is passed as an env variable. Check Passed!'''.format(self.ora_env_dict["REMOTE_JOB_NAME"])
              self.ocommon.log_info_message(msg,self.file_name)
           else:
              msg='''REMOTE_JOB_NAME is not set and setting to {0}'''.format(jname)
              self.ocommon.log_info_message(msg,self.file_name)
              self.ora_env_dict=self.ocommon.add_key("REMOTE_JOB_NAME",jname,self.ora_env_dict)
              self.ocommon.log_info_message(msg,self.file_name)


      def  primary_db_check(self):
           """
           This function check if the primary DB check or not
           """
           phname =  self.ora_env_dict["PRIMARY_HOSTNAME"]
           pdbname = self.ora_env_dict["PRIMARY_DB_NAME"]
           dbport = self.ora_env_dict["PRIMARY_DB_PORT"]

           systemStr='''{0}/bin/sqlplus "/as sysdba"'''.format(self.ora_env_dict["ORACLE_HOME"])

        #   self.ocommon.set_mask_str(self.ora_env_dict["ORACLE_PWD"])
           msg='''Testing the connection to primary DB'''
           self.ocommon.log_info_message(msg,self.file_name)
           self.ocommon.set_mask_str(self.ora_env_dict["ORACLE_PWD"])

           sqlcmd='''
            connect sys/HIDDEN_STRING@//{0}:{1}/{2} as sysdba
            select * from dual; 
            exit;
           '''.format(phname,dbport,pdbname)
           output,error,retcode=self.ocommon.run_sqlplus(systemStr,sqlcmd,None)
           # self.ocommon.log_info_message("Calling check_sql_err() to validate the sql command return status",self.file_name)
           self.ocommon.check_sql_err(output,error,retcode,None)
           self.ocommon.unset_mask_str()
 
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
               dest='''{0}/oradata/fast_recovery_area/{1}'''.format(self.ora_env_dict["ORACLE_BASE"],self.ora_env_dict["DB_UNIQUE_NAME"])
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
               dest='''{0}/oradata/{1}'''.format(self.ora_env_dict["ORACLE_BASE"],self.ora_env_dict["DB_UNIQUE_NAME"])
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

      def standby_create_dirs(self):
           """
            This function create dirs for the standby database
           """
           obase=self.ora_env_dict["ORACLE_BASE"]
           dbuname=self.ora_env_dict["DB_UNIQUE_NAME"]
           dbsid=self.ora_env_dict["ORACLE_SID"]
           dir_names='''
              mkdir -p {0}/oradata/{2}
              mkdir -p {0}/oradata/{1}
              mkdir -p {0}/fast_recovery_area/{2}
              mkdir -p {0}/fast_recovery_area/{1}
              mkdir -p {0}/admin/{2}/adump
              mkdir -p {0}/admin/{2}/dpdump
              mkdir -p {0}/admin/{2}/pfile
              mkdir -p {0}/admin/{2}/xdb_wallet
              mkdir -p {0}/audit/{1}
              mkdir -p {0}/audit/{2} 
           '''.format(obase,dbuname,dbsid)
           dir_list = [y for y in (x.strip() for x in dir_names.splitlines()) if y]
           for dir in dir_list:
               msg='''Setting up dir {0} on standby'''.format(dir)
               self.ocommon.log_info_message(msg,self.file_name)
               self.ocommon.create_dir(dir,True,None,None)           

      def drop_os_job(self):
           ''' 
           drop the job on primary if exist
           '''
           obase=self.ora_env_dict["ORACLE_BASE"]
           dbuname=self.ora_env_dict["DB_UNIQUE_NAME"]
           dbsid=self.ora_env_dict["ORACLE_SID"]
           host=self.ora_env_dict["PRIMARY_HOSTNAME"]
           port=self.ora_env_dict["PRIMARY_DB_PORT"]
           svc=self.ora_env_dict["PRIMARY_DB_NAME"]
           jobname=self.ora_env_dict["REMOTE_JOB_NAME"]

           sqlpluslogincmd='''{0}/bin/sqlplus "/as sysdba"'''.format(self.ora_env_dict["ORACLE_HOME"])
           # Assigning variable
           self.ocommon.set_mask_str(self.ora_env_dict["ORACLE_PWD"])
           #self.ocommon.log_info_message(msg,self.file_name)
           sqlcmd='''
             connect sys/HIDDEN_STRING@//{0}:{1}/{2} as sysdba
              exec dbms_scheduler.drop_job('{3}');
              exit;
           '''.format(host,port,svc,jobname)
           output,error,retcode=self.ocommon.run_sqlplus(sqlpluslogincmd,sqlcmd,None)
           self.ocommon.log_info_message("Calling check_sql_err() to validate the sql command return status",self.file_name)
           self.ocommon.check_sql_err(output,error,retcode,False)
           ## Unsetting the encrypt value to None
           self.ocommon.unset_mask_str()
 
      def standby_remote_dirs(self):
           """
            This function create dirs for the standby database
           """
           obase=self.ora_env_dict["ORACLE_BASE"]
           dbuname=self.ora_env_dict["DB_UNIQUE_NAME"]
           dbsid=self.ora_env_dict["ORACLE_SID"]
           host=self.ora_env_dict["PRIMARY_HOSTNAME"]
           port=self.ora_env_dict["PRIMARY_DB_PORT"]
           svc=self.ora_env_dict["PRIMARY_DB_NAME"] 
           sname=self.ora_env_dict["REMOTE_SCRIPT_NAME"]
           jobname=self.ora_env_dict["REMOTE_JOB_NAME"] 
           optype="primaryshard"

           dir_names='''
              {0}/oradata/{2}
              {0}/oradata/{1}
              {0}/fast_recovery_area/{2}
              {0}/fast_recovery_area/{1}
              {0}/admin/{2}/adump
              {0}/admin/{2}/dpdump
              {0}/admin/{2}/pfile
              {0}/admin/{2}/xdb_wallet
              {0}/audit/{1}
              {0}/audit/{2}
           '''.format(obase,dbuname,dbsid)

           sqlpluslogincmd='''{0}/bin/sqlplus "/as sysdba"'''.format(self.ora_env_dict["ORACLE_HOME"])
           # Assigning variable
           dir_list = [y for y in (x.strip() for x in dir_names.splitlines()) if y]
           for dir in dir_list:
               msg='''Setting up remote dir {0} on primary'''.format(dir)
               self.ocommon.log_info_message(msg,self.file_name)
               self.drop_os_job()
               self.ocommon.set_mask_str(self.ora_env_dict["ORACLE_PWD"]) 
               sqlcmd='''
                 connect sys/HIDDEN_STRING@//{0}:{1}/{2} as sysdba
                 begin
                 dbms_scheduler.create_job (job_name    => '{4}',
                   job_type    => 'executable',
                   job_action  => '/bin/python',
                   number_of_arguments => 3,
                   auto_drop   => false);
                 dbms_scheduler.set_job_argument_value ('{4}', 1, '{5}');
                 dbms_scheduler.set_job_argument_value ('{4}', 2, '--resetlistener="gdb_name=ORCL1CDB;sid_name=ORCL1CDB"');
                 dbms_scheduler.set_job_argument_value ('{4}', 3, '--optype={6}');
                 dbms_scheduler.run_job ('{4}');
                 end;
                 /
                 '''.format(host,port,svc,dir,jobname,sname,optype)

               output,error,retcode=self.ocommon.run_sqlplus(sqlpluslogincmd,sqlcmd,None)
               self.ocommon.log_info_message("Calling check_sql_err() to validate the sql command return status",self.file_name)
               self.ocommon.check_sql_err(output,error,retcode,True)
           ### Unsetting the encrypt value to None
               self.ocommon.unset_mask_str()

      def set_listener(self):
          """
           Function to set the tsnadby listener
          """
          self.set_standby_listener(self.ora_env_dict["DB_UNIQUE_NAME"],self.ora_env_dict["ORACLE_SID"])
          self.set_standby_listener(self.ora_env_dict["PRIMARY_DB_NAME"],self.ora_env_dict["ORACLE_SID"])

      def set_standby_listener(self,gdbname,sid):
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
              buffer +=  self.ocommon.get_sid_desc(gdbname,sid,"SID_DESC1")
              listener =  self.ocommon.get_lisora(1521)
              listener += '\n' + buffer
          else:
              buffer += self.ocommon.get_sid_desc(gdbname,sid,"SID_DESC")
              listener =  self.ocommon.get_lisora(1521)
              listener += '\n' + buffer

          wr = open(lisora, 'w')
          wr.write(listener)

      def restart_listener(self,key):
          """
          restart listener
          """
          cmd='''restart listener'''
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
