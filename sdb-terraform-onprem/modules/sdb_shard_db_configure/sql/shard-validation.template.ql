alter user gsmuser account unlock;
alter user gsmuser identified by ${gsmuser_pass};
grant debug connect session to gsmuser;
grant sysdg to gsmuser;
grant sysbackup to gsmuser;
alter system set db_file_name_convert='*','${oradata}/' scope=spfile;
SHUTDOWN IMMEDIATE
STARTUP MOUNT
ALTER DATABASE OPEN;
set serveroutput on
execute DBMS_GSM_FIX.validateShard
exit