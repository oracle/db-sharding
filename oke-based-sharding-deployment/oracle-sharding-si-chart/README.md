# Oracle Sharding 
Oracle Sharding is a scalability and availability feature for custom-designed OLTP applications that enables the distribution and replication of data across a pool of Oracle databases that do not share hardware or software. The pool of databases is presented to the application as a single logical database.

## Introduction
This chart bootstraps a single catalog, 3 shards along with 2 shard directors deployment on a Kubernetes cluster using the Helm package manager. 

## Prerequisites
* Kubernetes 1.10+
* To Deploy kubernetes cluster with 3 nodes in worker node pool and 2 worker nodes in gsm (for shard director) node pool on OKE, please refer Deploy Kubernetes cluster on OKE for Sharding. 
* PV provisioner support in the underlying infrastructure. Since we have used OCI, we used the oci class for persistent volumes.
* Oracle Database 19.3 image must be available to be pulled from your registry server.
* Oracle GSM 19.3 image must be available to be pulled from your registry server.
* Pods must have GitHub access to pull required scripts to setup the shard on Oracle Databases. Otherwise, you need to specify the script staging location available on pods.

## Creating the Kubernetes Secret
### Docker Registry Secret
If you need to seed the password during image pull for Oracle Database and GSM, you need to create the secret so that kubernetes can pull the images without user involvement.
```
kubectl create secret docker-registry oshardsecret --docker-username=<USER_NAME> --docker-password=<PASWORD> --docker-server=<DOCKER_REGISTRY_SECRET>
```
We have specified **oshardsecret** in the chart to pull the image. If you change the secret name, you need to make the changes in chart values.

### Password Management
Specify the secret volume for resetting Oracle database users' password during Oracle Sharding setup. It can be shared volume among all the pods

```
mkdir /tmp/.secrets/
openssl rand -hex 64 -out /tmp/.secrets/pwd.key
```

Edit the `/opt/.secrets/common_os_pwdfile` and seed the password for grid/oracle and database. It will be a common password for all the database users. Execute following command:

```
openssl enc -aes-256-cbc -md sha256 -salt -in /tmp/.secrets/common_os_pwdfile -out /tmp/.secrets/common_os_pwdfile.enc -pass file:/tmp/.secrets/pwd.key
rm -f /tmp/.secrets/common_os_pwdfile
```
Create the kubernetes a secret. In the chart, we are using db-user-pass secret so create the same or you need to override the value during chart creation.

```
kubectl create secret generic db-user-pass --from-file=/tmp/.secrets/common_os_pwdfile.enc --from-file=/tmp/.secrets/pwd.key
```
Check the secret details:
```
kubectl get secret
```

## Installing the Chart
install the chart with the release name my-release:

```
$ helm install --name my-release oracle-sharding-si-chart
```
The command deploys Oracle Sharding on the Kubernetes cluster in the default configuration. The configuration section lists the parameters that can be configured during installation.

## Uninstall
```
helm delete my-release
```
## Persistence
The Oracle Database image stores the datafiles and configurations at the /opt/oracle/oradata/ORACLE_SID path of the container.

By default, persistence is enabled, and a PersistentVolumeClaim is created and mounted in that directory.

## Configuration
The following table lists the configurable parameters of the Oracle Sharding chart and their default values.

### Global Parameters for Shard Director, Catalog and Shards
```
global:
  gsmimage:
   repository: < GSM Image Repository >
   tag: < Database Image Version. Default set to 19.3.0 >
   pullPolicy: < Image pull policy. Default set to IfNotPresent >
  dbimage:
   repository: < DB Image Repository >
   tag: < Database Image Version. Default set to 19.3.0-ee >
   pullPolicy: < Image pull policy. Default set to IfNotPresent >
  secret:
   oraclePwd: < Kubernetes secret created for db password >
   oraclePwdLoc: < Secret mounting location >
  strategy: < Pod creation Strategy >
  getScrCmd: < Init container scripts >
  registrySecret: < Registry Secret >
  gsmports:
   containerGSMProtocol: < GSM Protocol. Default set to TCP >
   containerGSMPortName: < GSM PORT Name>
   containerGSMPort: < Default value of GSM Port is 1521 >
   containerONSrPortName:  < GSM ONS PORT>
   containerONSrPort: 6234
   containerONSlPortName:  gsm-onslport
   containerONSlPort: 6123
   containerAgentPortName:  gsm-agentport
   containerAgentPort: 8080
  dbports:
   containerDBProtocol: TCP
   containerDBPortName: db1-port
   containerDBPort: 1521
   containerONSrPortName:  db1-onsrport
   containerONSrPort: 6234
   containerONSlPortName:  db1-onslport
   containerONSlPort: 6123
   containerAgentPortName:  db1-agentport
   containerAgentPort: 8080
  service:
   type: LoadBalancer
   port: 1521
```
### Shard Director configuration Parameters
```
gsm:
  replicaCount: 1
  gsmHostName: gsmhost
  nodeselector: ad3
  oci:
   region: phx
   zone: PHX-AD-1
  pvc:
   ociAD: "PHX-AD-1"
   storageSize: 50Gi
   accessModes: ReadWriteOnce
   storageClassName: oci
   DBMountLoc: /u01/app/oracle/gsmdata
   stagingLoc: /opt/oracle/gsm/scripts/setup
  service:
   type: LoadBalancer
   port: 1521
  env:
   SHARD_DIRECTOR_PARAMS: "director_name=sharddirector1;director_region=region1;director_port=1521"
   SHARD1_GROUP_PARAMS: "group_name=shardgroup1;deploy_as=primary;group_region=region1"
   CATALOG_PARAMS: "catalog_host=oshard-catalog-0.oshard-catalog;catalog_db=CATCDB;catalog_pdb=CAT1PDB;catalog_port=1521;catalog_name=shardcatalog1;catalog_region=region1,region2"
   SHARD1_PARAMS: "shard_host=oshard1-0.oshard1;shard_db=ORCL1CDB;shard_pdb=ORCL1PDB;shard_port=1521;shard_group=shardgroup1"
   SHARD2_PARAMS: "shard_host=oshard2-0.oshard2;shard_db=ORCL2CDB;shard_pdb=ORCL2PDB;shard_port=1521;shard_group=shardgroup1"
   SHARD3_PARAMS: "shard_host=oshard3-0.oshard3;shard_db=ORCL3CDB;shard_pdb=ORCL3PDB;shard_port=1521;shard_group=shardgroup1"
   SERVICE1_PARAMS: "service_name=oltp_rw_svc;service_role=primary"
   SERVICE2_PARAMS: "service_name=oltp_ro_svc;service_role=primary"
   BASE_DIR: /opt/oracle/gsm/scripts/setup
   COMMON_OS_PWD_FILE: common_os_pwdfile.enc
   PWD_KEY: pwd.key
   OP_TYPE: gsm
```

### Shard1 Configuration parameters

```
oshard1:
  replicaCount: 1
  app: oshard-db1
  nodeselector: ad3
  shardHostName: oshard1
  storageType: "bv"
  oci:
   region: phx
   zone: PHX-AD-1
  pvc:
   ociAD: "PHX-AD-1"
   storageSize: 50Gi
   accessModes: ReadWriteOnce
   storageClassName: oci
   DBMountLoc: /opt/oracle/oradata
   stagingLoc: /opt/oracle/scripts/setup
  env:
   ORACLE_SID: ORCL1CDB
   ORACLE_PDB: ORCL1PDB
   OP_TYPE: primaryshard
   DB_MEMORY: 12G
   COMMON_OS_PWD_FILE: common_os_pwdfile.enc
   PWD_KEY: pwd.key
  clone:
   db: "no"
   ocid:
  nfs:
   storageClassName: oci-fss
   mountOptions: nosuid
   serverName: xx.xx.xx.xx
   path: /shard_nfs
```
### Shard2 Configuration parameters

```
oshard2:
  replicaCount: 1
  app: oshard-db2
  nodeselector: ad3
  shardHostName: oshard2
  storageType: "bv"
  oci:
   region: phx
   zone: PHX-AD-1
  pvc:
   ociAD: "PHX-AD-1"
   storageSize: 50Gi
   accessModes: ReadWriteOnce
   storageClassName: oci
   DBMountLoc: /opt/oracle/oradata
   stagingLoc: /opt/oracle/scripts/setup
  env:
   ORACLE_SID: ORCL2CDB
   ORACLE_PDB: ORCL2PDB
   OP_TYPE: primaryshard
   DB_MEMORY: 12G
   COMMON_OS_PWD_FILE: common_os_pwdfile.enc
   PWD_KEY: pwd.key
  clone:
   db: "no"
   ocid:
  nfs:
   storageClassName: oci-fss
   mountOptions: nosuid
   serverName: xx.xx.xx.xx
   path: /shard_nfs
```

### Shard3 Configuration parameters

```
oshard3:
  replicaCount: 1
  app: oshard-db3
  nodeselector: ad3
  shardHostName: oshard3
  storageType: "bv"
  oci:
   region: phx
   zone: PHX-AD-1
  pvc:
   ociAD: "PHX-AD-1"
   storageSize: 50Gi
   accessModes: ReadWriteOnce
   storageClassName: oci
   DBMountLoc: /opt/oracle/oradata
   stagingLoc: /opt/oracle/scripts/setup
  env:
   ORACLE_SID: ORCL3CDB
   ORACLE_PDB: ORCL3PDB
   OP_TYPE: primaryshard
   DB_MEMORY: 12G
   COMMON_OS_PWD_FILE: common_os_pwdfile.enc
   PWD_KEY: pwd.key
  clone:
   db: "no"
   ocid:
  nfs:
   storageClassName: oci-fss
   mountOptions: nosuid
   serverName: xx.xx.xx.xx
   path: /shard_nfs
```

### Catalog Configuration Parameters

```
oshard-catalog:
  replicaCount: 1
  app: oshard-cat
  nodeselector: ad3
  shardHostName: oshard-catalog
  storageType: "bv"
  oci:
   region: phx
   zone: PHX-AD-1
  pvc:
   ociAD: "PHX-AD-1"
   storageSize: 50Gi
   accessModes: ReadWriteOnce
   storageClassName: oci
   DBMountLoc: /opt/oracle/oradata
   stagingLoc: /opt/oracle/scripts/setup
  env:
   ORACLE_SID: CATCDB
   ORACLE_PDB: CAT1PDB
   OP_TYPE: catalog
   DB_MEMORY: 12G
   COMMON_OS_PWD_FILE: common_os_pwdfile.enc
   PWD_KEY: pwd.key
  clone:
   db: "no"
   ocid:
  nfs:
   storageClassName: oci-fss
   mountOptions: nosuid
   serverName: xx.xx.xx.xx
   path: /shard_nfs
```
