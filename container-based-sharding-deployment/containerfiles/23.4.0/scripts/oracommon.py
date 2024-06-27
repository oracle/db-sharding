#!/usr/bin/python
# LICENSE UPL 1.0
#
# Copyright (c) 2020,2021 Oracle and/or its affiliates.
#
# Since: January, 2020
# Author: sanjay.singh@oracle.com, paramdeep.saini@oracle.com

from oralogger import *
from oraenv import *
import subprocess
import sys
import time
import datetime
import os
import getopt
import shlex
import json
import logging
import socket
import re
import os.path
import socket
import string
import random

class OraCommon:
      def __init__(self,oralogger,orahandler,oraenv):
        self.ologger = oralogger
        self.ohandler = orahandler
        self.oenv  = oraenv.get_instance()
        self.ora_env_dict = oraenv.get_env_vars()
        self.file_name  = os.path.basename(__file__)

      def run_sqlplus(self,cmd,sql_cmd,dbenv):
          """
          This function execute the ran sqlplus or rman script and return the output
          """
          try:
            message="Received Command : {0}\n{1}".format(self.mask_str(cmd),self.mask_str(sql_cmd))
            self.log_info_message(message,self.file_name)
            sql_cmd=self.unmask_str(sql_cmd)
            cmd=self.unmask_str(cmd)
#            message="Received Command : {0}\n{1}".format(cmd,sql_cmd)
#            self.log_info_message(message,self.file_name)
            p = subprocess.Popen(cmd,stdin=subprocess.PIPE,stdout=subprocess.PIPE,stderr=subprocess.PIPE,env=dbenv,shell=True,universal_newlines=True)
            p.stdin.write(sql_cmd)
            # (stdout,stderr), retcode = p.communicate(sqlplus_script.encode('utf-8')), p.returncode
            (stdout,stderr),retcode = p.communicate(),p.returncode
            #    stdout_lines = stdout.decode('utf-8').split("\n")
          except:
            error_msg=sys.exc_info()
            self.log_error_message(error_msg,self.file_name)
            self.prog_exit(self)

          return stdout.replace("\n\n", "\n"),stderr,retcode

      def execute_cmd(self,cmd,env,dir):
          """
          Execute the OS command on host
          """
          try:
            message="Received Command : {0}".format(self.mask_str(cmd))
            self.log_info_message(message,self.file_name)
            cmd=self.unmask_str(cmd)
            out = subprocess.Popen(cmd, shell=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE,universal_newlines=True)
            (output,error),retcode = out.communicate(),out.returncode
          except:
            error_msg=sys.exc_info()
            self.log_error_message(error_msg,self.file_name)
            self.prog_exit(self)

          return output,error,retcode

      def mask_str(self,mstr):
          """
           Function to mask the string.
          """
          newstr=None
          if self.oenv.encrypt_str__:
             newstr=mstr.replace('HIDDEN_STRING','********')
 #            self.log_info_message(newstr,self.file_name)
          if newstr:
     #        message = "Masked the string as encryption flag is set in the singleton class"
     #        self.log_info_message(message,self.file_name)
             return newstr
          else:
             return mstr
          

      def unmask_str(self,mstr):
          """
          Function to unmask the string.
          """
          newstr=None
          if self.oenv.encrypt_str__:
             newstr=mstr.replace('HIDDEN_STRING',self.oenv.original_str__.rstrip())
      #       self.log_info_message(newstr,self.file_name)
          if newstr:
      #       message = "Unmasked the encrypted string and returning original string from singleton class"
      #       self.log_info_message(message,self.file_name)
             return newstr
          else:
             return mstr

      def set_mask_str(self,mstr):
          """
          Function to unmask the string.
          """
          if mstr:
     #        message = "Setting encrypted String flag to True and original string in singleton class"
     #        self.log_info_message(message,self.file_name)
             self.oenv.encrypt_str__ = True
             self.oenv.original_str__ = mstr
          else:
             message = "Masked String is empty so no change required in encrypted String Flag and original string in singleton class"
             self.log_info_message(message,self.file_name)

      def unset_mask_str(self):
          """
          Function to unmask the string.
          """
    #      message = "Un-setting encrypted String flag and original string to None in Singleton class"
    #      self.log_info_message(message,self.file_name)
          self.oenv.encrypt_str__ = None
          self.oenv.original_str__ = None

      def prog_exit(self,message):
          """
          This function exit the program because of some error
          """
          sys.exit(127)

      def log_info_message(self,lmessage,fname):
          """
          Print the INFO message in the logger
          """
          funcname = sys._getframe(1).f_code.co_name
          message = '''{:^15}-{:^20}:{}'''.format(fname,funcname,lmessage)
          self.ologger.msg_ = message
          self.ologger.logtype_ = "INFO"
          self.ohandler.handle(self.ologger)

      def log_error_message(self,lmessage,fname):
          """
          Print the Error message in the logger
          """
          funcname=sys._getframe(1).f_code.co_name
          message='''{:^15}-{:^20}:{}'''.format(fname,funcname,lmessage)
          self.ologger.msg_=message
          self.ologger.logtype_="ERROR"
          self.ohandler.handle(self.ologger)

      def log_warn_message(self,lmessage,fname):
          """
          Print the Error message in the logger
          """
          funcname=sys._getframe(1).f_code.co_name
          message='''{:^15}-{:^20}:{}'''.format(fname,funcname,lmessage)
          self.ologger.msg_=message
          self.ologger.logtype_="WARN"
          self.ohandler.handle(self.ologger)

      def check_sql_err(self,output,err,retcode,status):
          """
          Check if there are any error in sql command output
          """
          match=None
          msg2='''Sql command  failed.Flag is set not to ignore this error.Please Check the logs,Exiting the Program!'''
          msg3='''Sql command  failed.Flag is set to ignore this error!'''
          self.log_info_message("output : " + str(output or "no Output"),self.file_name)
       #   self.log_info_message("Error  : " + str(err or  "no Error"),self.file_name)
       #   self.log_info_message("Sqlplus return code : " + str(retcode),self.file_name)
       #   self.log_info_message("Command Check Status Set to :" + str(status),self.file_name)

          if status:
             if (retcode!=0):
                self.log_info_message("Error  : " + str(err or  "no Error"),self.file_name)
                self.log_error_message("Sql Login Failed.Please Check the logs,Exiting the Program!",self.file_name)
                self.prog_exit(self)

          match=re.search("(?i)(?m)error",output)
          if status:
             if (match):
                self.log_error_message(msg2,self.file_name)
                self.prog_exit("error")
             else:
                self.log_info_message("Sql command completed successfully",self.file_name)
          else:
             if (match):
                self.log_warn_message("Sql command failed. Flag is set to ignore the error.",self.file_name)
             else:
                self.log_info_message("Sql command completed sucessfully.",self.file_name)

      def check_os_err(self,output,err,retcode,status):
          """
          Check if there are any error in OS command execution
          """
          msg1='''OS command returned code : {0} and returned output : {1}'''.format(str(retcode),str(output or "no Output"))
          msg2='''OS command returned code : {0}, returned error : {1} and returned output : {2}'''.format(str(retcode),str(err or  "no returned error"),str(output or "no retruned output"))
          msg3='''OS command  failed. Flag is set to ignore this error!'''

          if status:
            if (retcode != 0):
               self.log_error_message(msg2,self.file_name)
               self.prog_exit(self)
            else:
               self.log_info_message(msg1,self.file_name)
          else:
            if (retcode != 0):
               self.log_warn_message(msg2,self.file_name)
               self.log_warn_message(msg3,self.file_name)
            else:
               self.log_info_message(msg1,self.file_name)

      def check_key(self,key,env_dict):
          """
            Check the key if it exist in dictionary.
            Attributes:
               key (string): String to check if key exist in dictionary
               env_dict (dict): Contains the env variable related to seup
          """
          if key in env_dict:
             return True
          else:
             return False

      def empty_key(self,key):
          """
             key is empty and print failure message.
            Attributes:
               key (string): String is empty
          """
          msg='''Variable {0} is not defilned. Exiting!'''.format(key)
          self.log_error_message(msg,self.file_name)
          self.prog_exit(self)

      def add_key(self,key,value,env_dict):
          """
            Add the key in the dictionary.
            Attributes:
               key (string): key String to add in the dictionary
               value (String): value String to add in dictionary

            Return:
               dict
          """
          if self.check_key(key,env_dict):
             msg='''Variable {0} already exist in the env variables'''.format(key)
             self.log_info_message(msg,self.file_name)
          else:
             if value:
                env_dict[key] = value
                self.oenv.update_env_vars(env_dict)
             else:
                msg='''Variable {0} value is not defilned to add in the env variables. Exiting!'''.format(value)
                self.log_error_message(msg,self.file_name)
                self.prog_exit(self)

          return env_dict

      def update_key(self,key,value,env_dict):
          """
            update the key in the dictionary.
            Attributes:
               key (string): key String to update in the dictionary
               value (String): value String to update in dictionary

            Return:
               dict
          """
          if self.check_key(key,env_dict):
             if value:
                env_dict[key] = value
                self.oenv.update_env_vars(env_dict)
             else:
                msg='''Variable {0} value is not defilned to update in the env variables!'''.format(key)
                self.log_warn_message(msg,self.file_name)
          else:
             msg='''Variable {0} already exist in the env variables'''.format(key)
             self.log_info_message(msg,self.file_name)

          return env_dict

      def check_file(self,file,local,remote,user):
          """
            check locally or remotely
            Attributes:
               file (string): file to be created
               local (boolean): dir to craetes locally
               remote (boolean): dir to be created remotely
               node (string): remote node name on which dir to be created
               user (string): remote user to be connected
          """
          self.log_info_message("Inside check_file()",self.file_name)
          if local:
             if os.path.isfile(file):
                  return True
             else:
                  return False
               

      def read_file(self,fname):
          """
            Read the contents of a file and returns the contents to end user
            Attributes:
               fname (string): file to be read

            Return:
               file data (string)
          """
          f1 = open(fname, 'r')
          fdata = f1.read()
          f1.close
          return fdata

      def write_file(self,fname,fdata):
          """
            write the contents to a file
            Attributes:
               fname (string): file to be written
               fdata (string): COnetents to be written

            Return:
               file data (string)
          """
          f1 = open(fname, 'w')
          f1.write(fdata)
          f1.close

      def create_dir(self,dir,local,remote,user):
          """
            Create dir locally or remotely
            Attributes:
               dir (string): dir to be created
               local (boolean): dir to craetes locally
               remote (boolean): dir to be created remotely
               node (string): remote node name on which dir to be created
               user (string): remote user to be connected
          """
          self.log_info_message("Inside create_dir()",self.file_name)
          if local:
             if not os.path.isdir(dir):
                 cmd='''mkdir -p {0}'''.format(dir)
                 output,error,retcode=self.execute_cmd(cmd,None,None)
                 self.check_os_err(output,error,retcode,True)
             else:
                 msg='''Dir {0} already exist'''.format(dir)
                 self.log_info_message(msg,self.file_name)

          if remote:
             pass

      def create_file(self,file,local,remote,user):
          """
            Create dir locally or remotely
            Attributes:
               file (string): file to be created
               local (boolean): dir to craetes locally
               remote (boolean): dir to be created remotely
               node (string): remote node name on which dir to be created
               user (string): remote user to be connected
          """
          self.log_info_message("Inside create_file()",self.file_name)
          if local:
             if not os.path.isfile(file):
                 cmd='''touch  {0}'''.format(file)
                 output,error,retcode=self.execute_cmd(cmd,None,None)
                 self.check_os_err(output,error,retcode,True)

          if remote:
             pass

      def shutdown_db(self,env_dict):
           """
           Shutdown the database
           """
           file="/home/oracle/shutDown.sh"
           if not os.path.isfile(file): 
              self.log_info_message("Inside shutdown_db()",self.file_name)
              sqlpluslogincmd='''{0}/bin/sqlplus "/as sysdba"'''.format(env_dict["ORACLE_HOME"])
              sqlcmd='''
                shutdown immediate;
              '''
              self.log_info_message("Running the sqlplus command to shutdown the database: " + sqlcmd,self.file_name)
              output,error,retcode=self.run_sqlplus(sqlpluslogincmd,sqlcmd,None)
              self.log_info_message("Calling check_sql_err() to validate the sql command return status",self.file_name)
              self.check_sql_err(output,error,retcode,True)
           else:
              cmd='''sh {0} immediate'''.format(file)
              output,error,retcode=self.execute_cmd(cmd,None,None)
              self.check_os_err(output,error,retcode,True)
                 
      def mount_db(self,env_dict):
           """
           Mount the database
           """
           self.log_info_message("Inside mount_db()",self.file_name)
           sqlpluslogincmd='''{0}/bin/sqlplus "/as sysdba"'''.format(env_dict["ORACLE_HOME"])
           sqlcmd='''
                  startup mount;
           '''
           self.log_info_message("Running the sqlplus command to mount the database: " + sqlcmd,self.file_name)
           output,error,retcode=self.run_sqlplus(sqlpluslogincmd,sqlcmd,None)
           self.log_info_message("Calling check_sql_err() to validate the sql command return status",self.file_name)
           self.check_sql_err(output,error,retcode,True)

      def start_db(self,env_dict):
           """
           startup the database
           """
           file="/home/oracle/startUp.sh"
           if not os.path.isfile(file):
              self.log_info_message("Inside start_db()",self.file_name)
              sqlpluslogincmd='''{0}/bin/sqlplus "/as sysdba"'''.format(env_dict["ORACLE_HOME"])
              sqlcmd='''
                  startup;
              '''
              self.log_info_message("Running the sqlplus command to start the database: " + sqlcmd,self.file_name)
              output,error,retcode=self.run_sqlplus(sqlpluslogincmd,sqlcmd,None)
              self.log_info_message("Calling check_sql_err() to validate the sql command return status",self.file_name)
              self.check_sql_err(output,error,retcode,True)
           else:
              cmd='''sh {0}'''.format(file)
              output,error,retcode=self.execute_cmd(cmd,None,None)
              self.check_os_err(output,error,retcode,True)

      def nomount_db(self,env_dict):
           """
           No mount  the database
           """
           self.log_info_message("Inside start_db()",self.file_name)
           sqlpluslogincmd='''{0}/bin/sqlplus "/as sysdba"'''.format(env_dict["ORACLE_HOME"])
           sqlcmd='''
                 startup nomount;
           '''
           self.log_info_message("Running the sqlplus command to start the database: " + sqlcmd,self.file_name)
           output,error,retcode=self.run_sqlplus(sqlpluslogincmd,sqlcmd,None)
           self.log_info_message("Calling check_sql_err() to validate the sql command return status",self.file_name)
           self.check_sql_err(output,error,retcode,True)

      def stop_gsm(self,env_dict):
           """
           Stop the GSM
           """
           self.log_info_message("Inside stop_gsm()",self.file_name)
           gsmctl='''{0}/bin/gdsctl'''.format(env_dict["ORACLE_HOME"])
           gsmcmd='''
                  stop gsm;
           '''
           output,error,retcode=self.run_sqlplus(gsmctl,gsmcmd,None)
           self.log_info_message("Calling check_sql_err() to validate the gsm command return status",self.file_name)
           self.check_sql_err(output,error,retcode,None)

      def set_events(self,source):
         """
         Seting events at DB level
         """
         scope=''
         accepted_scope = ['spfile', 'memory', 'both']
 
         if self.check_key("DB_EVENTS",self.ora_env_dict):
            events=str(self.ora_env_dict["DB_EVENTS"]).split(";")

            for event in events:
              msg='''Setting up event {0}'''.format(event)
              self.log_info_message(msg,self.file_name)
              scope=''
              ohome=self.ora_env_dict["ORACLE_HOME"]
              inst_sid=self.ora_env_dict["ORACLE_SID"]
              sqlpluslogincmd=self.get_sqlplus_str(ohome,inst_sid,"sys",None,None,None,None,None,None,None)
              self.set_mask_str(self.ora_env_dict["ORACLE_PWD"])
              source=event.split(":")
              if len(source) > 1:
                 if source[1].split("=")[0] == "scope":
                    scope=source[1].split("=")[1]
                     
              if scope not in accepted_scope:
                 sqlcmd="""
                    alter system set events='{0}';""".format(source[0])
              else:
                 sqlcmd="""
                    alter system set event='{0}' scope={1};""".format(source[0],scope)
              output,error,retcode=self.run_sqlplus(sqlpluslogincmd,sqlcmd,None)
              self.log_info_message("Calling check_sql_err() to validate the sql command return status",self.file_name)
              self.check_sql_err(output,error,retcode,True)
              
      def start_gsm(self,env_dict):
           """
           Start the GSM
           """
           self.log_info_message("Inside start_gsm()",self.file_name)
           gsmctl='''{0}/bin/gdsctl'''.format(env_dict["ORACLE_HOME"])
           gsmcmd='''
                  start gsm;
           '''
           output,error,retcode=self.run_sqlplus(gsmctl,gsmcmd,None)
           self.log_info_message("Calling check_sql_err() to validate the gsm command return status",self.file_name)
           self.check_sql_err(output,error,retcode,None)

      def exec_gsm_cmd(self,gsmcmd,flag,env_dict):
           """
           Get the GSM command output 
           """
           self.log_info_message("Inside exec_gsm_cmd()",self.file_name)
           gsmctl='''{0}/bin/gdsctl'''.format(env_dict["ORACLE_HOME"])
           if gsmcmd:
              output,error,retcode=self.run_sqlplus(gsmctl,gsmcmd,None)
              self.log_info_message("Calling check_sql_err() to validate the gsm command return status",self.file_name)
              self.check_sql_err(output,error,retcode,flag)
           else:
              self.log_info_message("GSM Command was set to empty. Executing nothing and setting output to None",self.file_name) 
              output=None

           return output,error,retcode         


      def check_substr_match(self,source_str,sub_str):
           """
            CHeck if substring exist 
           """
           self.log_info_message("Inside check_substr_match()",self.file_name)
           if (source_str.find(sub_str) != -1):
              return True
           else:
              return False

      def find_str_in_string(self,source_str,delimeter,search_str):
         """AI is creating summary for find_str_in_string

         Args:
             source_str ([string]): [string where you need to search]
             delimeter ([character]): [string delimeter]
             search_str ([string]): [string to be searched]
         """
         if delimeter == 'comma':
            new_str=source_str.split(',')
            for str in new_str:
               if str.lower() == search_str.lower():
                  return True
            return False
         
         return False
      
      def check_status_value(self,match):
           """
             return completed or notcompleted
           """
           self.log_info_message("Inside check_status_value()",self.file_name)
           if match:
              return 'completed'
           else:
              return 'notcompleted'

      def remove_file(self,fname):
           """
             Remove if file exist
           """
           self.log_info_message("Inside remove_file()",self.file_name)
           if os.path.exists(fname):
              os.remove(fname)

      def get_sid_desc(self,gdbname,ohome,sid,sflag):
           """
             get the SID_LISTENER_DESCRIPTION
           """
           self.log_info_message("Inside get_sid_desc()",self.file_name)
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

      def get_lisora(self,port):
           """
             return listener.ora listener settings
           """
           self.log_info_message("Inside get_lisora()",self.file_name)
           listener='''LISTENER =
             (DESCRIPTION_LIST =
              (DESCRIPTION =
              (ADDRESS = (PROTOCOL = TCP)(HOST = 0.0.0.0)(PORT = {0}))
              (ADDRESS = (PROTOCOL = IPC)(KEY = EXTPROC{0}))
              )
             )
           '''.format(port)
           return listener

      def get_domain(self,ohost):
           """
           get the domain name from hostname
           """
           return ohost.partition('.')[2]
        
######### Get the DOMAIN##############
      def get_host_domain(self):
         """
         Return Public Hostname
         """
         domain=None
         domain=socket.getfqdn().split('.',1)[1]
         if domain is None:
            domain="example.info"

         return domain
   
 ######### get the public IP ##############
      def get_ip(self,hostname,domain):
         """
         Return the Ip based on hostname
         """
         if not domain:
           domain=self.get_host_domain()
 
         return socket.gethostbyname(hostname)

      def get_global_dbdomain(self,ohost,gdbname):
           """
           get the global dbname 
           """
           domain = self.get_domain(ohost) 
           if domain:
             global_dbname = gdbname + domain
           else:
             global_dbname = gdbname 
              
           return gdbname

######### Sqlplus connect string  ###########
      def get_sqlplus_str(self,home,osid,dbuser,password,hostname,port,svc,osep,role,wallet):
         """
         return the sqlplus connect string
         """
         path='''/usr/bin:/bin:/sbin:/usr/local/sbin:{0}/bin'''.format(home)
         ldpath='''{0}/lib:/lib:/usr/lib'''.format(home)
         export_cmd='''export ORACLE_HOME={0};export PATH={1};export LD_LIBRARY_PATH={2};export ORACLE_SID={3}'''.format(home,path,ldpath,osid)
         if dbuser == 'sys' and password and hostname and port and svc:
            return '''{5};{6}/bin/sqlplus {0}/{1}@//{2}:{3}/{4} as sysdba'''.format(dbuser,password,hostname,port,svc,export_cmd,home)
         elif dbuser != 'sys' and password and hostname and svc:
            return '''{5};{6}/bin/sqlplus {0}/{1}@//{2}:{3}/{4}'''.format(dbuser,password,hostname,"1521",svc,export_cmd,home)
         elif dbuser and osep:
            return dbuser
         elif dbuser == 'sys' and not password:
            return '''{1};{0}/bin/sqlplus "/ as sysdba"'''.format(home,export_cmd)
         elif dbuser == 'sys' and  password:
            return '''{1};{0}/bin/sqlplus {2}/{3} as sysdba'''.format(home,export_cmd,dbuser,password)
         elif dbuser != 'sys' and password:
            return '''{1};{0}/bin/sqlplus {2}/{3}'''.format(home,export_cmd,dbuser,password)
         else:
            self.log_info_message("Atleast specify db user and password for db connectivity. Exiting...",self.file_name)
            self.prog_exit("127")

######### Get Password ##############
      def get_os_password(self):
         """
         get the OS password
         """
         ospasswd=self.get_password(None)
         return ospasswd

      def get_asm_passwd(self):
         """
         get the ASM password
         """
         asmpasswd=self.get_password(None)
         return asmpasswd

      def get_db_passwd(self):
         """
         get the DB password
         """
         dbpasswd=self.get_password(None)
         return dbpasswd

      def get_sys_passwd(self):
         """
         get the sys user password
         """
         dbpasswd=self.get_password(None)
         return dbpasswd

      def get_password(self,key):
            """
            get the password
            """
            passwd_file_flag=False
            password=None
            password_file=None
            if self.check_key("SECRET_VOLUME",self.ora_env_dict):
               self.log_info_message("Secret_Volume set to : ",self.ora_env_dict["SECRET_VOLUME"])
               msg='''SECRET_VOLUME passed as an env variable and set to {0}'''.format(self.ora_env_dict["SECRET_VOLUME"])
            else:
               self.ora_env_dict=self.add_key("SECRET_VOLUME","/run/secrets",self.ora_env_dict)
               msg='''SECRET_VOLUME not passed as an env variable. Setting default to {0}'''.format(self.ora_env_dict["SECRET_VOLUME"])
               self.log_warn_message(msg,self.file_name)

            if self.check_key("KEY_SECRET_VOLUME",self.ora_env_dict):
               self.log_info_message("Secret_Volume set to : ",self.ora_env_dict["KEY_SECRET_VOLUME"])
               msg='''KEY_SECRET_VOLUME passed as an env variable and set to {0}'''.format(self.ora_env_dict["KEY_SECRET_VOLUME"])
            else:
                if self.check_key("SECRET_VOLUME",self.ora_env_dict):
                   self.ora_env_dict=self.add_key("KEY_SECRET_VOLUME",self.ora_env_dict["SECRET_VOLUME"],self.ora_env_dict)
                   msg='''KEY_SECRET_VOLUME not passed as an env variable. Setting default to {0}'''.format(self.ora_env_dict["KEY_SECRET_VOLUME"])
                   self.log_warn_message(msg,self.file_name)
                  
            if self.check_key("COMMON_OS_PWD_FILE",self.ora_env_dict):
               msg='''COMMON_OS_PWD_FILE passed as an env variable and set to {0}'''.format(self.ora_env_dict["COMMON_OS_PWD_FILE"])
            else:
               self.ora_env_dict=self.add_key("COMMON_OS_PWD_FILE","common_os_pwdfile.enc",self.ora_env_dict)
               msg='''COMMON_OS_PWD_FILE not passed as an env variable. Setting default to {0}'''.format(self.ora_env_dict["COMMON_OS_PWD_FILE"])
               self.log_warn_message(msg,self.file_name)

            if self.check_key("PWD_KEY",self.ora_env_dict):
               msg='''PWD_KEY passed as an env variable and set to {0}'''.format(self.ora_env_dict["PWD_KEY"])
            else:
               self.ora_env_dict=self.add_key("PWD_KEY","pwd.key",self.ora_env_dict)
               msg='''PWD_KEY not passed as an env variable. Setting default to {0}'''.format(self.ora_env_dict["PWD_KEY"])
               self.log_warn_message(msg,self.file_name)

            if self.check_key("PASSWORD_FILE",self.ora_env_dict):
               msg='''PASSWORD_FILE passed as an env variable and set to {0}'''.format(self.ora_env_dict["PASSWORD_FILE"])
            else:
               self.ora_env_dict=self.add_key("PASSWORD_FILE","dbpasswd.file",self.ora_env_dict)
               msg='''PASSWORD_FILE not passed as an env variable. Setting default to {0}'''.format(self.ora_env_dict["PASSWORD_FILE"])
               self.log_warn_message(msg,self.file_name)          
                    
            secret_volume = self.ora_env_dict["SECRET_VOLUME"]
            key_secret_volume= self.ora_env_dict["KEY_SECRET_VOLUME"]
            common_os_pwd_file = self.ora_env_dict["COMMON_OS_PWD_FILE"]
            pwd_volume=None
            if self.check_key("PWD_VOLUME",self.ora_env_dict):
               pwd_volume=self.ora_env_dict["PWD_VOLUME"]
            else:
               pwd_volume="/var/tmp"
            pwd_key = self.ora_env_dict["PWD_KEY"]
            passwd_file='''{0}/{1}'''.format(secret_volume,self.ora_env_dict["COMMON_OS_PWD_FILE"])
            dbpasswd_file='''{0}/{1}'''.format(secret_volume,self.ora_env_dict["PASSWORD_FILE"])
            key_file='''{0}/{1}'''.format(self.ora_env_dict["KEY_SECRET_VOLUME"],self.ora_env_dict["PWD_KEY"])
            key_secret_volume='''{0}'''.format(self.ora_env_dict["KEY_SECRET_VOLUME"])
            self.log_info_message("Password file set to : " + passwd_file,self.file_name)
            self.log_info_message("key file set to : " + key_file,self.file_name)
            self.log_info_message("dbpasswd file set to : " + dbpasswd_file,self.file_name)
            self.log_info_message("key secret voluem set to  file set to : " + key_secret_volume,self.file_name)
            self.log_info_message("pwd volume set : " + pwd_volume,self.file_name)
            #print(passwd_file)
            if (os.path.isfile(passwd_file)) and (os.path.isfile(key_file)):
               msg='''Passwd file {0} and key file {1} exist. Password file Check passed!'''.format(passwd_file,key_file)
               self.log_info_message(msg,self.file_name)
               msg='''Reading encrypted passwd from file {0}.'''.format(passwd_file)
               self.log_info_message(msg,self.file_name)
               cmd=None
               if self.check_key("ENCRYPTION_TYPE",self.ora_env_dict):
                  if self.ora_env_dict["ENCRYPTION_TYPE"].lower() == "aes256":
                     cmd='''openssl enc -d -aes-256-cbc -in \"{0}/{1}\" -out {2}/{1} -pass file:\"{3}/{4}\"'''.format(secret_volume,common_os_pwd_file,pwd_volume,key_secret_volume,pwd_key)
                  elif self.ora_env_dict["ENCRYPTION_TYPE"].lower() == "rsautl":
                     cmd ='''openssl rsautl -decrypt -in \"{0}/{1}\" -out {2}/{1} -inkey \"{3}/{4}\"'''.format(secret_volume,common_os_pwd_file,pwd_volume,key_secret_volume,pwd_key)
                  else:
                     pass
               else:
                  cmd ='''openssl pkeyutl -decrypt -in \"{0}/{1}\" -out {2}/{1} -inkey \"{3}/{4}\"'''.format(secret_volume,common_os_pwd_file,pwd_volume,key_secret_volume,pwd_key)
      
               output,error,retcode=self.execute_cmd(cmd,None,None)
               self.check_os_err(output,error,retcode,True)
               passwd_file_flag = True
               password_file='''{0}/{1}'''.format(pwd_volume,self.ora_env_dict["COMMON_OS_PWD_FILE"])
            elif os.path.isfile(dbpasswd_file):
               msg='''Passwd file {0} exist. Password file Check passed!'''.format(dbpasswd_file)
               self.log_info_message(msg,self.file_name)
               msg='''Reading encrypted passwd from file {0}.'''.format(dbpasswd_file)
               self.log_info_message(msg,self.file_name)
               cmd='''openssl base64 -d -in \"{0}\" -out \"{2}/{1}\"'''.format(dbpasswd_file,self.ora_env_dict["PASSWORD_FILE"],pwd_volume)
               output,error,retcode=self.execute_cmd(cmd,None,None)
               self.check_os_err(output,error,retcode,True)
               passwd_file_flag = True
               password_file='''{1}/{0}'''.format(self.ora_env_dict["PASSWORD_FILE"],pwd_volume)      

            if not passwd_file_flag:
               # get random password pf length 8 with letters, digits, and symbols
               characters1 = string.ascii_letters +  string.digits + "_-%#"
               str1 = ''.join(random.choice(string.ascii_uppercase) for i in range(4))
               str2 = ''.join(random.choice(characters1) for i in range(8))
               password=str1+str2
            else:
               fname='''{0}'''.format(password_file)
               fdata=self.read_file(fname)
               password=fdata
               self.remove_file(fname)
               
            if self.check_key("ORACLE_PWD",self.ora_env_dict):
               if len(self.ora_env_dict["ORACLE_PWD"]) > 0:
                  msg="ORACLE_PWD is passed as an env variable. Check Passed!"
                  self.log_info_message(msg,self.file_name)
               else:
                 msg="ORACLE_PWD passed as 0 length string"
                 self.log_info_message(msg,self.file_name)
                 self.ora_env_dict=self.update_key("ORACLE_PWD",password,self.ora_env_dict)
                 msg="ORACLE_PWD set to HIDDEN_STRING generated using encrypted password file"
                 self.log_info_message(msg,self.file_name)                  
            else:
               self.ora_env_dict=self.add_key("ORACLE_PWD",password,self.ora_env_dict)
               msg="ORACLE_PWD set to HIDDEN_STRING generated using encrypted password file"
               self.log_info_message(msg,self.file_name)

######### Get oraversion ##############
      def get_oraversion(self,home):
         """
         get the software version
         """
         cmd='''{0}/bin/oraversion -majorVersion'''.format(home)
         output,error,retcode=self.execute_cmd(cmd,None,None)
         self.check_os_err(output,error,retcode,True)

         return output 
      
####### Get db lock file location #######
      def get_db_lock_location(self):
         """
         get the db location
         """
         if self.check_key("DB_LOCK_FILE_LOCATION",self.ora_env_dict):
            return self.ora_env_dict["DB_LOCK_FILE_LOCATION"]
         else:
            ### Please note that you should not change following path as SIDB team is maintaining lock files under following location
            return "/tmp/."

####### Get the TDE Key ###############
      def export_tde_key(self,filename):
         """
         This function export the tde.
         """
         self.log_info_message("Inside gettdekey()",self.file_name)
         sqlpluslogincmd='''{0}/bin/sqlplus "/as sysdba"'''.format(self.ora_env_dict["ORACLE_HOME"])
         self.set_mask_str(self.ora_env_dict["ORACLE_PWD"])
         sqlcmd='''
           ALTER SESSION DISABLE SHARD DDL;
           ADMINISTER KEY MANAGEMENT EXPORT ENCRYPTION KEYS WITH SECRET {0} TO {1} IDENTIFIED BY {0};
         '''.format('HIDDEN_STRING',filename)
         self.log_info_message("Running the sqlplus command to export the tde: " + sqlcmd,self.file_name)
         output,error,retcode=self.run_sqlplus(sqlpluslogincmd,sqlcmd,None)
         self.log_info_message("Calling check_sql_err() to validate the sql command return status",self.file_name)
         self.check_sql_err(output,error,retcode,True)

####### Get the TDE Key ###############
      def import_tde_key(self,filename):
         """
         This function import the TDE key.
         """
         self.log_info_message("Inside importtdekey()",self.file_name)
         sqlpluslogincmd='''{0}/bin/sqlplus "/as sysdba"'''.format(self.ora_env_dict["ORACLE_HOME"])
         self.set_mask_str(self.ora_env_dict["ORACLE_PWD"])
         sqlcmd='''
         ADMINISTER KEY MANAGEMENT SET KEYSTORE OPEN IDENTIFIED BY {0};
         ADMINISTER KEY MANAGEMENT IMPORT ENCRYPTION KEYS WITH SECRET {0} FROM {1} IDENTIFIED BY {0} WITH BACKUP
         '''.format('HIDDEN_STRING',filename)
         self.log_info_message("Running the sqlplus command to import the tde key: " + sqlcmd,self.file_name)
         output,error,retcode=self.run_sqlplus(sqlpluslogincmd,sqlcmd,None)
         self.log_info_message("Calling check_sql_err() to validate the sql command return status",self.file_name)
         self.check_sql_err(output,error,retcode,True)

####### Check PDB if it exist ###############
      def check_pdb(self,pdbname):
         """
         This function check the PDB.
         """
         self.log_info_message("Inside check_pdb()",self.file_name)
         sqlpluslogincmd='''{0}/bin/sqlplus "/as sysdba"'''.format(self.ora_env_dict["ORACLE_HOME"])
         self.set_mask_str(self.ora_env_dict["ORACLE_PWD"])
         sqlcmd='''
         set heading off
         set feedback off
         select NAME from gv$pdbs;
         '''
         output,error,retcode=self.run_sqlplus(sqlpluslogincmd,sqlcmd,None)
         self.log_info_message("Calling check_sql_err() to validate the sql command return status",self.file_name)
         self.check_sql_err(output,error,retcode,None)
         pdblist=output.splitlines()
         self.log_info_message("Checking pdb " + pdbname, self.file_name)
         if pdbname in pdblist:
            return True
         else:
            return False

####### Create PDB if it does not exist ###############
      def create_pdb(self,ohome,opdb,inst_sid):
         """
         This function create the PDB.
         """
         self.log_info_message("Inside create_pdb()",self.file_name)
         self.set_mask_str(self.ora_env_dict["ORACLE_PWD"])
         cmd='''{0}/bin/dbca -silent -createPluggableDatabase -pdbName {1}  -sourceDB {2} <<< HIDDEN_STRING'''.format(ohome,opdb,inst_sid)
         output,error,retcode=self.execute_cmd(cmd,None,None)
         self.unset_mask_str()
         self.check_os_err(output,error,retcode,True)
      
######## Reset the DB Password in database ########
      def reset_passwd(self):
         """
         This function reset the password.
         """ 
         password_script='''{0}/{1}'''.format(self.ora_env_dict["HOME"],"setPassword.sh")
         self.log_info_message("Executing password reset", self.file_name)
         if self.check_key("ORACLE_PWD",self.ora_env_dict) and self.check_key("HOME",self.ora_env_dict) and os.path.isfile(password_script):
            cmd='''{0} {1} '''.format(password_script,'HIDDEN_STRING')
            self.set_mask_str(self.ora_env_dict["ORACLE_PWD"])
            output,error,retcode=self.execute_cmd(cmd,None,None)
            self.check_os_err(output,error,retcode,True)
            self.unset_mask_str()
         else:
            msg='''Error Occurred! Either HOME DIR {0} does not exist, ORACLE_PWD {1} is not set or PASSWORD SCRIPT {2} does not exist'''.format(self.ora_env_dict["HOME"],self.ora_env_dict["ORACLE_PWD"],password_script)  
            self.log_error_message(msg,self.file_name)
            self.oracommon.prog_exit()
