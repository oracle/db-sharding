-- alter system set db_create_file_dest='${oradata}' scope=both;
alter system set open_links=255 scope=spfile;
alter system set open_links_per_instance=255 scope=spfile;
shutdown immediate
startup
set echo on
set termout on
spool setup_grants_privs.lst
alter user gsmcatuser account unlock;
alter user gsmcatuser identified by ${gsmcatuser_pass};
-- alter session set "_ORACLE_SCRIPT"=true;
-- create user ${sdb_admin_username} identified by ${sdb_admin_pass};
-- grant connect, create session, gsmadmin_role to ${sdb_admin_username};
spool off
-- alter system set events 'immediate trace name GWM_TRACE level 1';
alter system set local_listener='${catalog_host}:${catalog_port}' scope=both;
alter system register reconnect;
exit
