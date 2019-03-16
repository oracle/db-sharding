/*
** Oracle Sharding Tools Library
**
** Copyright © 2019 Oracle and/or its affiliates. All rights reserved.
** Licensed under the Universal Permissive License v 1.0 as shown at 
**   http://oss.oracle.com/licenses/upl 
*/


/**
Create sharded tables for customer and their corresponding invoices belonging to
 each of the customer's vendors for sdb invoice app
*/


create user app_schema identified by app_schema;
grant connect, resource, alter session to app_schema;
grant execute on dbms_crypto to app_schema;
grant create table, create procedure, create tablespace, create
materialized view to app_schema;
grant unlimited tablespace to app_schema;
grant select_catalog_role to app_schema;
grant all privileges to app_schema;
grant gsmadmin_role to app_schema;
grant dba to app_schema;

CREATE TABLESPACE SET tbsset1 IN SHARDSPACE shd1;
CREATE TABLESPACE SET tbsset2 IN SHARDSPACE shd2;

connect app_schema/app_schema
alter session enable shard ddl;


/* Customer shard table */

CREATE SHARDED TABLE customer
( cust_id NUMBER NOT NULL,
  cust_passwd VARCHAR2(20) NOT NULL,
  cust_name VARCHAR2(60) NOT NULL,
  cust_type VARCHAR2(10) NOT NULL,
  cust_email VARCHAR2(100) NOT NULL)
partitionset by list (cust_type)
partition by consistent hash (cust_id) partitions auto
(partitionset individual values ('individual') tablespace set tbsset1,
partitionset  business values ('business') tablespace set tbsset2
);

/* Invoice shard table */

CREATE SHARDED TABLE invoice 
( invoice_id  NUMBER NOT NULL,
  cust_id  NUMBER NOT NULL,
  cust_type VARCHAR2(10) NOT NULL,
  vendor_name VARCHAR2(60) NOT NULL,
  balance FLOAT(10) NOT NULL,
  total FLOAT(10) NOT NULL,    
  status VARCHAR2(20),  
  CONSTRAINT InvoicePK PRIMARY KEY (cust_id, invoice_id))
PARENT customer
partitionset by list (cust_type)
partition by consistent hash (cust_id) partitions auto
(partitionset individual values ('individual') tablespace set tbsset1,
partitionset  business values ('business') tablespace set tbsset2
);


/* Data */

insert into customer values (999, 'pass', 'Customer 999', 'individual', 'customer999@gmail.com');
insert into customer values (250251, 'pass', 'Customer 250251', 'individual', 'customer250251@yahoo.com');
insert into customer values (350351, 'pass', 'Customer 350351', 'individual', 'customer350351@gmail.com');
insert into customer values (550551, 'pass', 'Customer 550551', 'business', 'customer550551@hotmail.com');
insert into customer values (650651, 'pass', 'Customer 650651', 'business', 'customer650651@live.com');


insert into invoice values (1001, 999, 'individual', 'VendorA', 10000, 20000, 'Due');
insert into invoice values (1002, 999, 'individual', 'VendorB', 10000, 20000, 'Due');

insert into invoice values (1001, 250251, 'individual', 'VendorA', 10000, 20000, 'Due');
insert into invoice values (1002, 250251, 'individual', 'VendorB', 0, 10000, 'Paid');
insert into invoice values (1003, 250251, 'individual', 'VendorC', 14000, 15000, 'Due');


insert into invoice values (1001, 350351, 'individual', 'VendorD', 10000, 20000, 'Due');
insert into invoice values (1002, 350351, 'individual', 'VendorE', 0, 10000, 'Paid');
insert into invoice values (1003, 350351, 'individual', 'VendorF', 14000, 15000, 'Due');
insert into invoice values (1004, 350351, 'individual', 'VendorG', 12000, 15000, 'Due');


insert into invoice values (1001, 550551, 'business', 'VendorH', 10000, 20000, 'Due');
insert into invoice values (1002, 550551, 'business', 'VendorI', 0, 10000, 'Paid');
insert into invoice values (1003, 550551, 'business', 'VendorJ', 14000, 15000, 'Due');
insert into invoice values (1004, 550551, 'business', 'VendorK', 10000, 20000, 'Due');
insert into invoice values (1005, 550551, 'business', 'VendorL', 10000, 20000, 'Due');
insert into invoice values (1006, 550551, 'business', 'VendorM', 0, 10000, 'Paid');
insert into invoice values (1007, 550551, 'business', 'VendorN', 14000, 15000, 'Due');
insert into invoice values (1008, 550551, 'business', 'VendorO', 10000, 20000, 'Due');


insert into invoice values (1001, 650651, 'business', 'VendorT', 10000, 20000, 'Due');
insert into invoice values (1002, 650651, 'business', 'VendorU', 0, 10000, 'Paid');
insert into invoice values (1003, 650651, 'business', 'VendorV', 14000, 15000, 'Due');
insert into invoice values (1004, 650651, 'business', 'VendorW', 10000, 20000, 'Due');
insert into invoice values (1005, 650651, 'business', 'VendorX', 0, 20000, 'Paid');
insert into invoice values (1006, 650651, 'business', 'VendorY', 0, 30000, 'Paid');
insert into invoice values (1007, 650651, 'business', 'VendorZ', 0, 10000, 'Paid');




