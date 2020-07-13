# Copyright 2020 Oracle Corporation and/or affiliates.  All rights reserved.
# Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl

resource "null_resource" "sdb_demo_setup" {
  depends_on = ["null_resource.sdb_schema_setup"]
  count      = "${var.demo_setup == "false" ? 0 : local.sharding_methods[var.sharding_method] == local.system_sharding ? 1 : 0}"

  #creates ssh connection
  connection {
    type        = "ssh"
    user        = "${var.os_user}"
    private_key = "${tls_private_key.public_private_key_pair.private_key_pem}"
    host        = "${data.oci_core_vnic.catalog_db_node_vnic[0].public_ip_address}"
    agent       = false
    timeout     = "${var.ssh_timeout}"
  }

  provisioner "remote-exec" {
    inline = [
      "mkdir -p ${local.db_home_path}"
    ]
  }

  # copying sdb demo binary over
  provisioner "file" {
    source      = "${var.sdb_demo_binary_file_path}"
    destination = "${local.db_home_path}/${local.sdb_demo_dir}.zip"
  }

  provisioner "file" {
    content     = <<-EOF
      #! /bin/bash
      cd ${local.db_home_path}
      source ${local.db_home_path}/shardcat.sh
      chmod 700 ${local.sdb_demo_dir}.zip
      unzip -o ${local.sdb_demo_dir}.zip
      mv ${local.sdb_demo_dir}/monitor-install.sh ${local.sdb_demo_dir}/orig-monitor-install.sh
      mv ${local.sdb_demo_dir}/demo.properties ${local.sdb_demo_dir}/orig-demo-props
      mv ${local.sdb_demo_dir}/sql/app_schema_auto.sql ${local.sdb_demo_dir}/sql/orig-app_schema_auto.sql
      mv ${local.sdb_demo_dir}/sql/app_schema_user.sql ${local.sdb_demo_dir}/sql/orig-app_schema_user.sql
      mv ${local.sdb_demo_dir}/sql/catalog_monitor.sql ${local.sdb_demo_dir}/sql/orig-catalog_monitor.sql
      mv ${local.sdb_demo_dir}/sql/demo_app_ext.sql ${local.sdb_demo_dir}/sql/orig-demo_app_ext.sql
      EOF
    destination = "${local.db_home_path}/unzip-demo.sh"
  }

  provisioner "remote-exec" {
    inline = [
      "chmod 700 ${local.db_home_path}/unzip-demo.sh",
      "${local.db_home_path}/unzip-demo.sh"
    ]
  }

  provisioner "file" {
    content     = <<-EOF
      #! /bin/bash
      CSTR='(DESCRIPTION=(CONNECT_TIMEOUT=5)(TRANSPORT_CONNECT_TIMEOUT=3)(RETRY_COUNT=3)(ADDRESS_LIST=(LOAD_BALANCE=on)(ADDRESS=(PROTOCOL=TCP)(HOST=${data.oci_core_vnic.catalog_db_node_vnic[0].public_ip_address})(PORT=${oci_database_db_system.catalog_db[0].listener_port})))(CONNECT_DATA=(SERVICE_NAME=${data.oci_database_database.catalog_database[0].db_unique_name}.${oci_database_db_system.catalog_db[0].domain})))'

      echo -e '@sql/1_dba_global_views.sql\n' | sqlplus -S sys/sd${random_string.sys_pass.result}@$CSTR as sysdba
      echo -e '@sql/b3_mon_views.sql\n' | sqlplus -S app_schema/App_Schema_Pass_123@$CSTR
      echo -e '@sql/b4_mon_all.sql\n' | sqlplus -S sys/sd${random_string.sys_pass.result}@$CSTR as sysdba     
      EOF
    destination = "${local.db_home_path}/${local.sdb_demo_dir}/monitor-install.sh"
  }

  provisioner "file" {
    content     = <<-EOF
      connect app_schema/App_Schema_Pass_123@${data.oci_core_vnic.catalog_db_node_vnic[0].public_ip_address}:${oci_database_db_system.catalog_db[0].listener_port}/${data.oci_database_database.catalog_database[0].pdb_name}.${oci_database_db_system.catalog_db[0].domain}

      ALTER SESSION ENABLE SHARD DDL;

      create tablespace set tsp_set_1 using template (datafile size 100m autoextend on next 10M maxsize unlimited extent
              management local segment space management auto) in shardspace shardspaceora;

      create tablespace products_tsp datafile size 100m autoextend on;

      CREATE SHARDED TABLE Customers
      (
        CustId      VARCHAR2(60) NOT NULL,
        FirstName   VARCHAR2(60),
        LastName    VARCHAR2(60),
        Class       VARCHAR2(10),
        Geo         VARCHAR2(8),
        CustProfile VARCHAR2(4000),
        Passwd      RAW(60),
        CONSTRAINT pk_customers PRIMARY KEY (CustId),
        CONSTRAINT json_customers CHECK (CustProfile IS JSON)
      ) TABLESPACE SET tsp_set_1
      PARTITION BY CONSISTENT HASH (CustId) PARTITIONS AUTO;

      CREATE SHARDED TABLE Orders
      (
        OrderId     INTEGER NOT NULL,
        CustId      VARCHAR2(60) NOT NULL,
        OrderDate   TIMESTAMP NOT NULL,
        SumTotal    NUMBER(19,4),
        Status      CHAR(4),
        constraint  pk_orders primary key (CustId, OrderId),
        constraint  fk_orders_parent foreign key (CustId) 
          references Customers on delete cascade
      ) partition by reference (fk_orders_parent);

      CREATE SEQUENCE Orders_Seq;

      CREATE SHARDED TABLE LineItems
      (
        OrderId     INTEGER NOT NULL,
        CustId      VARCHAR2(60) NOT NULL,
        ProductId   INTEGER NOT NULL,
        Price       NUMBER(19,4),
        Qty         NUMBER,
        constraint  pk_items primary key (CustId, OrderId, ProductId),
        constraint  fk_items_parent foreign key (CustId, OrderId)
          references Orders on delete cascade
      ) partition by reference (fk_items_parent);

      CREATE DUPLICATED TABLE Products
      (
        ProductId  INTEGER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
        Name       VARCHAR2(128),
        DescrUri   VARCHAR2(128),
        LastPrice  NUMBER(19,4)
      ) TABLESPACE PRODUCTS_TSP;


      CREATE OR REPLACE FUNCTION PasswCreate(PASSW IN RAW)
        RETURN RAW
      IS
        Salt RAW(8);
      BEGIN
        Salt := sys.DBMS_CRYPTO.RANDOMBYTES(8);
        RETURN UTL_RAW.CONCAT(Salt, sys.DBMS_CRYPTO.HASH(UTL_RAW.CONCAT(Salt, PASSW), sys.DBMS_CRYPTO.HASH_SH256));
      END;

      /
      show errors;

      CREATE OR REPLACE FUNCTION PasswCheck(PASSW IN RAW, PHASH IN RAW)
        RETURN INTEGER IS
      BEGIN
        RETURN UTL_RAW.COMPARE(
            sys.DBMS_CRYPTO.HASH(UTL_RAW.CONCAT(UTL_RAW.SUBSTR(PHASH, 1, 8), PASSW), sys.DBMS_CRYPTO.HASH_SH256),
            UTL_RAW.SUBSTR(PHASH, 9));
      END;

      /
      show errors;
      EOF
    destination = "${local.db_home_path}/${local.sdb_demo_dir}/sql/app_schema_auto.sql"
  }

  provisioner "file" {
    content     = <<-EOF
      alter session set container=${var.pdb_name};

      alter session enable shard ddl;

      create user app_schema identified by App_Schema_Pass_123;
      grant connect, resource, alter session to app_schema;

      -- For demo app purposes
      grant execute on sys.dbms_crypto to app_schema;

      -- Bug Workaround. Normally, app_schema user does not need that.
      grant create view, create database link,
          alter database link, create materialized view, create tablespace to app_schema;

      grant unlimited tablespace to app_schema;
      -- End Workaround

      alter session disable shard ddl;
      EOF
    destination = "${local.db_home_path}/${local.sdb_demo_dir}/sql/app_schema_user.sql"
  }

  provisioner "file" {
    content     = <<-EOF
      alter session set container=${var.pdb_name};

      alter session enable shard ddl;

      create role shard_monitor_role;

      grant connect,
            alter session,
            select any dictionary,
            analyze any,
            select any table
          to shard_monitor_role;

      grant select on gv_$session       to shard_monitor_role;
      grant select on gv_$database      to shard_monitor_role;
      grant select on gv_$servicemetric to shard_monitor_role;

      alter session disable shard ddl;

      @global_views.header.sql
      /
      show errors

      @global_views.sql
      /
      show errors

      exec dbms_global_views.install;

      @shard_helpers.sql
      /
      show errors

      grant execute on sys.dbms_global_views   to shard_monitor_role;
      grant execute on sys.shard_helper_remote to shard_monitor_role;
      grant gsmadmin_role to shard_monitor_role;

      exec dbms_global_views.create_gv('session');
      exec dbms_global_views.create_gv('database');
      exec dbms_global_views.create_gv('servicemetric');
      exec dbms_global_views.create_any_view('LOCAL_CHUNKS', 'LOCAL_CHUNKS', 'GLOBAL_CHUNKS');
      exec dbms_global_views.create_dba_view('DBA_TAB_PARTITIONS');
      EOF
    destination = "${local.db_home_path}/${local.sdb_demo_dir}/sql/catalog_monitor.sql"
  }

  provisioner "file" {
    content     = <<-EOF
      -- Create catalog monitor packages
      connect / as sysdba
      @catalog_monitor.sql

      connect app_schema/App_Schema_Pass_123@${data.oci_core_vnic.catalog_db_node_vnic[0].public_ip_address}:${oci_database_db_system.catalog_db[0].listener_port}/${data.oci_database_database.catalog_database[0].pdb_name}.${oci_database_db_system.catalog_db[0].domain}

      alter session enable shard ddl;

      CREATE OR REPLACE VIEW SAMPLE_ORDERS AS
        SELECT OrderId, CustId, OrderDate, SumTotal FROM
          (SELECT * FROM ORDERS ORDER BY OrderId DESC)
            WHERE ROWNUM < 10;

      alter session disable shard ddl;

      -- Allow a special query for dbaview
      connect / as sysdba

      alter session set container=${var.pdb_name};

      -- For demo app purposes
      grant shard_monitor_role, gsmadmin_role to app_schema;

      alter session enable shard ddl;

      create user dbmonuser identified by Db_Monitor_Pass_123;
      grant connect, alter session, shard_monitor_role, gsmadmin_role to dbmonuser;

      grant all privileges on app_schema.products to dbmonuser;
      grant read on app_schema.sample_orders to dbmonuser;

      alter session disable shard ddl;
      -- End workaround

      exec dbms_global_views.create_any_view('SAMPLE_ORDERS', 'APP_SCHEMA.SAMPLE_ORDERS', 'GLOBAL_SAMPLE_ORDERS', 0, 1);

      EXIT  
      EOF
    destination = "${local.db_home_path}/${local.sdb_demo_dir}/sql/demo_app_ext.sql"
  }

  provisioner "file" {
    content     = <<-EOF
      #! /bin/bash
      cd ${local.db_home_path}
      source ${local.db_home_path}/shardcat.sh
      cd ${local.sdb_demo_dir}/sql
      sqlplus / as sysdba @demo_app_ext.sql
      exit
      EOF
    destination = "${local.db_home_path}/demo-additional-objects-setup.sh"
  }
  provisioner "remote-exec" {
    inline = [
      "chmod 700 ${local.db_home_path}/demo-additional-objects-setup.sh",
      "${local.db_home_path}/demo-additional-objects-setup.sh"
    ]
  }

  provisioner "file" {
    content     = <<EOF
name=demo
connect_string=(ADDRESS_LIST=(LOAD_BALANCE=off)(FAILOVER=on)(ADDRESS=(HOST=${oci_core_instance.gsm_vm[0].public_ip})(PORT=${var.shard_director_port})(PROTOCOL=tcp)))
monitor.user=dbmonuser
monitor.pass=Db_Monitor_Pass_123
app.service.write=oltp_rw_srvc.cust_sdb.oradbcloud
app.service.readonly=oltp_ro_srvc.cust_sdb.oradbcloud
app.user=app_schema
app.pass=App_Schema_Pass_123
app.threads=7
app.service.xs=GDS$CATALOG.oradbcloud
EOF
    destination = "${local.db_home_path}/${local.sdb_demo_dir}/demo.properties"
  }

  provisioner "file" {
    content     = <<-EOF
      #! /bin/bash
      cd ${local.db_home_path}/${local.sdb_demo_dir}
      source ${local.db_home_path}/shardcat.sh
      chmod 700 ${local.db_home_path}/${local.sdb_demo_dir}/run.sh
      cd ${local.db_home_path}/${local.sdb_demo_dir}      
      nohup ./run.sh demo >> nohup-run-demo.out 2>&1 &
      sleep 6
      EOF
    destination = "${local.db_home_path}/run-demo.sh"
  }

  provisioner "remote-exec" {
    inline = [
      "chmod 700 ${local.db_home_path}/run-demo.sh",
      "${local.db_home_path}/run-demo.sh"
    ]
  }

  # destroying
  provisioner "remote-exec" {
    when = "destroy"
    inline = [
      "kill $(ps aux | grep '[o]racle.demo.Main' | awk '{print $2}')",
      "rm -f ${local.db_home_path}/run-monitor.sh",
      "rm -f ${local.db_home_path}/run-demo.sh",
      "rm -f ${local.db_home_path}/demo-additional-objects-setup.sh",
      "rm -f ${local.db_home_path}/${local.sdb_demo_dir}.zip",
      "rm -rf ${local.db_home_path}/${local.sdb_demo_dir}",
      "rm -rf ${local.db_home_path}/__MACOSX",
      "rm -rf ${local.db_home_path}/unzip-demo.sh"
    ]
  }
}
