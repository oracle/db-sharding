SHUTDOWN IMMEDIATE
STARTUP MOUNT
ALTER DATABASE ARCHIVELOG;
ALTER DATABASE OPEN;
ARCHIVE LOG LIST;
alter database flashback on;
ALTER DATABASE FORCE LOGGING;

alter user gsmuser account unlock;
alter user gsmuser identified by ${gsmuser_pass};
grant debug connect session to gsmuser;
grant sysdg to gsmuser;
grant sysbackup to gsmuser;

ALTER SYSTEM SET DG_BROKER_START=TRUE scope=both sid='*';
alter system set events 'immediate trace name GWM_TRACE level 1';
alter system set db_file_name_convert='*','${oradata}/' scope=spfile;

-- Create DATA_PUMP_DIR (for chunk migration)
create or replace directory data_pump_dir as '${oradata}';
select DIRECTORY_PATH from dba_directories where DIRECTORY_NAME='DATA_PUMP_DIR';
grant read,write on directory DATA_PUMP_DIR to gsmadmin_internal;
grant read,write on directory DATA_PUMP_DIR to gsmuser;

set serveroutput on
execute DBMS_GSM_FIX.validateShard

exit