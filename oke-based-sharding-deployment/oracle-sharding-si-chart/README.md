# Oracle Sharding 
Oracle Sharding is a scalability and availability feature for custom-designed OLTP applications that enables the distribution and replication of data across a pool of Oracle databases that do not share hardware or software. The pool of databases is presented to the application as a single logical database.

## Introduction
This chart bootstraps a single catalog, 3 shards along with 2 shard directors deployment on a Kubernetes cluster using the Helm package manager. 

## Prerequisites
* Kubernetes 1.10+
* To Deploy kubernetes cluster with 3 nodes in worker node pool and 2 worker nodes in gsm (for shard director) node pool on OKE, please refer Deploy Kubernetes cluster on OKE for Sharding. 
* PV provisioner support in the underlying infrastructure. Since we have used OCI, we used the oci class for persistent volumes.
* Oracle Database 21.3 image must be available to be pulled from your registry server.
* Oracle GSM 21.3 image must be available to be pulled from your registry server.
* Pods must have GitHub access to pull required scripts to setup the shard on Oracle Databases. Otherwise, you need to specify the script staging location available on pods.

## Creating the Kubernetes Secret
### Label the Nodes
You need to label the nodes so that shard deployment happens on the specified worker nodes. 
* Get the worker node details along with labels in your k8s cluster.
  ```
  kubectl get nodes -o wide --show-labels
  ```
* Select the nodes based on your enviornment. You need to select the nodes for the sharding topology. In this guide, we have 3 shards, 1 catalog and 1 GSM
* If you are not deploying on Oracle OCI Kubernetes service then label all the nodes where you want to deploy sharding topology as shown below. If you are on OCI OKE enviornment then reord the values of  `failure-domain.beta.kubernetes.io/region` and `failure-domain.beta.kubernetes.io/zone` because we will be using these values during setting values in the values.yaml.
  ```
   kubectl label node <node name> failure-domain.beta.kubernetes.io/region=primary_region
   kubectl label node <node name> failure-domain.beta.kubernetes.io/zone=primary_zone
  ```
* Label the node for gsm deployment.
  ```
  kubectl label node <node name> oraclegsm=ogsm
  ```
* Label the nodes for shard and catalog deplpyment
  ```
  kubectl label node <node name> oracleshard=oshard
  ```
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

Edit the `/tmp/.secrets/common_os_pwdfile` and seed the password for grid/oracle and database. It will be a common password for all the database users. Execute following command:

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
## Modify Configuration
Before you install chart, you need to review "Configuration" section and make changes in values.yaml based on your enviornment. You can also refer values.yaml.sample file to see the parameter details.

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
   repository:                              ## < GSM Image Repository >
   tag:                                     ## << Database Image Version. E.g. 21.3.0 >>
   pullPolicy:                              ## << Image pull policy. E.g. IfNotPresent >>
  dbimage:
   repository:                              ## << DB Image Repository >>
   tag:                                     ## << Database Image Version. E.g. 21.3.0-ee >>
   pullPolicy:                              ## << Image pull policy. E.g. IfNotPresent >>
  secret:
   oraclePwd:                               ## << Kubernetes secret created for db password. E.g. db-user-pass >>
   oraclePwdLoc:                            ## << Secret mounting location inside the pod. E.g. /mnt/secrets >>
  strategy:                                 ## << Pod creation Strategy. E.g. Recreate >>
  getScrCmd:                                ## << Init container scripts. If you are behind proxy you need to add "export https_proxy=<PROXY_NAME:PORT" and change it to "export https_proxy=<PROXY_NAME:PORT> ; curl https://codeload.github.com/oracle/db-sharding/tar.gz/master |   tar -xz --strip=4 db-sharding-master/docker-based-sharding-deployment/dockerfiles/21.3.0/scripts" >>
  registrySecret:                           ## << Registry Secret. E.g. oshardsecret >>
  gsmports:
   containerGSMProtocol:                    ## << GSM Protocol. E.g. TCP >>
   containerGSMPortName:                    ## << GSM PORT Name. E.g. oshardgsm-port>>
   containerGSMPort:                        ## << Set the value to 1521 or to some other port based on your enviornment>>
   containerONSrPortName:                   ## << GSM ONS Remote Port. E.g. gsm-onsrport>>
   containerONSrPort:                       ## << Set the value 6234 or some other port based on your enviornment>>
   containerONSlPortName:                   ## << GSM INS local Port. E.g. gsm-onslport >>
   containerONSlPort:                       ## << Set the value to 6123 or some other port based on your enviornment>>
   containerAgentPortName:                  ## << Set the agent Port Name. E.g. gsm-agentport >>
   containerAgentPort:                      ## << Set the value 8080 or to some other port based on your enviornment>>
  dbports:
   containerDBProtocol:                     ## << DB Protocol. E.g. TCP >>
   containerDBPortName:                     ## << DB Port Name. E.g. db1-port >>
   containerDBPort:                         ## << Set the value to 1521 or some other value based on your enviornment >>
   containerONSrPortName:                   ## << DB Ons Remote Port Name. E.g. db1-onsrport >>
   containerONSrPort:                       ## << Set the value of DB ONS Remote Port. E.g. 6234 >>
   containerONSlPortName:                   ## << DB Ons local Port Name. E.g. db1-onslport >>
   containerONSlPort:                       ## << Set the value of DB ONS local Port. E.g. 6123 >>
   containerAgentPortName:                  ## << Set the value of Agent Port Name. E.g. db1-agentport>>
   containerAgentPort:                      ## << Set the value of Agent Port Valure. E.g. 8080>>
  service:
   type: LoadBalancer                       ## << Set the service type. E.g. LoadBalancer >>
   port:                                    ## << Set the K8s service port for DB. E.g. 1521 >>
```
### Shard Director configuration Parameters
```
gsm:
  replicaCount:                             ## << Count of Replica Pods for Shard Director/GSM . We are keeping it 1 as are going to have 1 pod for shard director >>
  gsmHostName:                              ## << Name of GSM host. Example: gsmhost  >>
  nodeselector:                             ## << select the values you used in "Label the Nodes" for "oraclegsm". Example: "ogsm" >>
  oci:
   region:                                  ## << select the values you used in "Label the Nodes" for "failure-domain.beta.kubernetes.io/region". Example: "primary_region" >>
   zone:                                    ## << select the values you used in "Label the Nodes" for "failure-domain.beta.kubernetes.io/zone" . Example: "primary_zone" >>
  pvc:                                      ## << Persistent Volume Claim  >>
   storageSize:                             ## << Size of the PVC Storage. Example: 50Gi  >>
   accessModes:                             ## << Access Mode of the PVC Storage. Example: ReadWriteOnce  >> 
   storageClassName:                        ## << Plugin for Volume plugin used by a Persistent Volume Claim. Example: oci  >> 
   stagingLoc:                              ## << Staging location for GSM scripts inside the POD. Example: /opt/oracle/gsm/scripts/setup  >> 
  service:
   type:                                    ## << Kubernetes service type. Example: LoadBalancer  >> 
   port:                                    ## << Port for the Kubernetes service type. Example: 1521  >> 
  env:
   SHARD_DIRECTOR_PARAMS:                   ## << Parameters director_name,director_region,director_port for Shard Director. Example: "director_name=sharddirector1;director_region=region1;director_port=1521"  >> 
   SHARD1_GROUP_PARAMS:                       ## << Parameters group_name,deploy_as,group_region for the Shard Group "group_name=shardgroup1;deploy_as=primary;group_region=region1"  >> 
   CATALOG_PARAMS:                          ## << Parameters catalog_host,catalog_db,catalog_pdb,catalog_port,catalog_name,catalog_region for Shard Catalog. Example: "catalog_host=oshard-catalog-0.oshard-catalog;catalog_db=CATCDB;catalog_pdb=CAT1PDB;catalog_port=1521;catalog_name=shardcatalog1;catalog_region=region1,region2;catalog_chunks=24">> 
   SHARD1_PARAMS:                           ## << Parameters like shard_host,shard_db,shard_pdb, shard_port, shard_group for shard1. Example: "shard_host=oshard1-0.oshard1;shard_db=ORCL1CDB;shard_pdb=ORCL1PDB;shard_port=1521;shard_group=shardgroup1" >> 
   SHARD2_PARAMS:                           ## << Parameters like shard_host,shard_db,shard_pdb, shard_port, shard_group for shard2. Example: "shard_host=oshard2-0.oshard2;shard_db=ORCL2CDB;shard_pdb=ORCL2PDB;shard_port=1521;shard_group=shardgroup1" >> 
   SHARD3_PARAMS:                           ## << Parameters like shard_host,shard_db,shard_pdb, shard_port, shard_group for shard3. Example: "shard_host=oshard3-0.oshard3;shard_db=ORCL3CDB;shard_pdb=ORCL3PDB;shard_port=1521;shard_group=shardgroup1" >> 
   SERVICE1_PARAMS:                         ## << Parameters service_name,service_role for RW DB service. Example: "service_name=oltp_rw_svc;service_role=primary" >> 
   SERVICE2_PARAMS:                         ## << Parameters service_name,service_role for RO DB service. Example: "service_name=oltp_ro_svc;service_role=primary" >> 
   BASE_DIR:                                ## << Base Directory for the GSM Setup. Example: /opt/oracle/gsm/scripts/setup >> 
   COMMON_OS_PWD_FILE:                      ## << Encrupted OS Password file >>
   PWD_KEY:                                 ## << File with the password key >>
   OP_TYPE:                                 ## << Mandatory parameter and value must be set to "gsm". >> 
   SECRET_VOLUME:                           ## << Mandatory parameter for secret volume inside the pod. Default value "/mnt/secrets">>
   MASTER_GSM:                              ## << Mandatory parameter and value must be set to "CONFIGURE". >>
```

### Shard1 Configuration parameters

```
oshard1:
  replicaCount:                             ## << Count of Replica Pods for Shard1. We are keeping it 1 as are going to have 1 pod for shard 1 >>
  app:                                      ## << Application name for shard 1. Example: oshard-db1 >>
  nodeselector:                             ## << select the values you used in "Label the Nodes" for "oracleshard". Example: "oshard" >>
  shardHostName:                            ## << Host name for the shard 1 pod. Example: oshard1 >>
  storageType:                              ## << Type of the storage. E.g. "bv" for block volume >>
  oci:
   region:                                  ## << select the values you used in "Label the Nodes" for "failure-domain.beta.kubernetes.io/region". Example: "primary_region" >>
   zone:                                    ## << select the values you used in "Label the Nodes" for "failure-domain.beta.kubernetes.io/zone" . Example: "primary_zone" >>
  pvc:                                      ## << Persistent Volume Claim  >>
   storageSize:                             ## << Shard 1 Size of the PVC Storage. Example: 50Gi  >>
   accessModes:                             ## << Shard 1 Access Mode of the PVC Storage. Example: ReadWriteOnce >> 
   storageClassName:                        ## << Shard 1 Plugin for Volume plugin used by a Persistent Volume Claim. Example: oci. >> 
   stagingLoc:                              ## << Shard 1 Staging location for shard1 setup scripts inside the pod. Example: /opt/oracle/scripts/setup >> 
  env:
   ORACLE_SID:                              ## << Mandatory variable for CDB SID Name for shard 1. Example: ORCL1CDB >> 
   ORACLE_PDB:                              ## << Mandatory variable for PDB Name for shard 1. Example: ORCL1PDB >> 
   OP_TYPE:                                 ## << Mandatory variable for operation type for shard 1, set to primaryshard >> 
   SGA_SIZE:                                ## << Optional variable for SGA SIZE for Catalog. Example: 6144. The values are in MB. Do not add "MB" after numeric value" >> 
   PGA_SIZE:                                ## << Optional variable for PGA SIZE for Catalog. Example: 2048. The values are in MB. Do not add "MB" after numeric value" >>
   COMMON_OS_PWD_FILE:                      ## << Encrypted OS Password file. Example: common_os_pwdfile.enc >>
   PWD_KEY:                                 ## << File with the password key. Example: pwd.key >>
   OLD_ORACLE_SID:                          ## << If you are using DB clone feature, you need to set the old cdb name to be cloned. >>
   OLD_ORACLE_PDB:                          ## << If you are using DB clone feature, you need to set the old pdb name to be cloned. >>
   SECRET_VOLUME:                           ## << Mandatory parameter for secret volume inside the pod. Default value set to  "/mnt/secrets". >> 
  clone:
   db:                                      ## << Optional parameter and default value is set to "false" in values.yaml. Set to "true", if you are using db clone feature. >>
   ocid:                                    ## << Optional parameter and value is set to OCID of the Block Volume having the Gold Image of the database >>
  nfs:
   storageClassName:                        ## << Shard 1 - Type of the storage class for the nfs. example: oci-fss >>
   mountOptions:                            ## << Shard 1 - Mount option for the storage. Example: To block operation of suid,sgid bits, use nosuid >>
   serverName:                              ## << Shard 1 - IP for the NFS Server. Example: Use an IP in format: xx.xx.xx.xx >> 
   path:                                    ## << Shard 1 - to mount the nfs. Example: /shard_nfs >>
```
### Shard2 Configuration parameters

```
oshard2:
  replicaCount:                             ## << Count of Replica Pods for Shard2. We are keeping it 1 as are going to have 1 pod for shard 2>>
  app:                                      ## << Application name for shard 2. Example: oshard-db2 >>
  nodeselector:                             ## << select the values you used in "Label the Nodes" for "oracleshard". Example: "oshard" >>
  shardHostName:                            ## << Host name for the shard 2 pod. Example: oshard2 >>
  storageType:                              ## << Type of the storage. E.g. "bv" for block volume >>
  oci:
   region:                                  ## << select the values you used in "Label the Nodes" for "failure-domain.beta.kubernetes.io/region". Example: "primary_region" >>
   zone:                                    ## << select the values you used in "Label the Nodes" for "failure-domain.beta.kubernetes.io/zone" . Example: "primary_zone" >>
  pvc:                                      ## << Persistent Volume Claim  >>
   storageSize:                             ## << Shard 2 Size of the PVC Storage. Example: 50Gi  >>
   accessModes:                             ## << Shard 2 Access Mode of the PVC Storage. Example: ReadWriteOnce >> 
   storageClassName:                        ## << Shard 2 Plugin for Volume plugin used by a Persistent Volume Claim. Example: oci  >> 
   stagingLoc:                              ## << Shard 2 Staging location for shard1 setup scripts inside the pod. Example: /opt/oracle/scripts/setup >> 
  env:
   ORACLE_SID:                              ## << Mandatory variable for CDB SID Name for shard 2. Example: ORCL2CDB >> 
   ORACLE_PDB:                              ## << Mandatory variable for PDB Name for shard 2. Example: ORCL1PDB >> 
   OP_TYPE:                                 ## << Mandatory variable for operation type for shard 2, set to primaryshard >> 
   SGA_SIZE:                                ## << Optional variable for SGA SIZE for Catalog. Example: 6144. The values are in MB. Do not add "MB" after numeric value" >> 
   PGA_SIZE:                                ## << Optional variable for PGA SIZE for Catalog. Example: 2048. The values are in MB. Do not add "MB" after numeric value" >>
   COMMON_OS_PWD_FILE:                      ## << Encrupted OS Password file. Example: common_os_pwdfile.enc >>
   PWD_KEY:                                 ## << File with the password key. Example: pwd.key >>
   OLD_ORACLE_SID:                          ## << If you are using DB clone feature, you need to set the old cdb name to be cloned. >>
   OLD_ORACLE_PDB:                          ## << If you are using DB clone feature, you need to set the old pdb name to be cloned. >>
   SECRET_VOLUME:                           ## << Mandatory parameter for secret volume inside the pod. Default value set to  "/mnt/secrets". >> 
  clone:
   db:                                      ## << Optional parameter and default value is set to "false" in values.yaml. Set to "true", if you are using db clone feature. >>
   ocid:                                    ## << Optional parameter and value is set to OCID of the Block Volume having the Gold Image of the database >>
  nfs:
   storageClassName:                        ## << Shard 2 - Type of the storage class for the nfs. example: oci-fss >>
   mountOptions:                            ## << Shard 2 - Mount option for the storage. Example: To block operation of suid,sgid bits, use nosuid >>
   serverName:                              ## << Shard 2 - IP for the NFS Server. Example: Use an IP in format: xx.xx.xx.xx >> 
   path:                                    ## << Shard 2 - to mount the nfs. Example: /shard_nfs >>
```

### Shard3 Configuration parameters

```
oshard3:
  replicaCount:                             ## << Count of Replica Pods for Shard3. We are keeping it 1 as are going to have 1 pod for shard 2>>
  app:                                      ## << Application name for shard 3. Example: oshard-db3 >>
  nodeselector:                             ## << select the values you used in "Label the Nodes" for "oracleshard". Example: "oshard ">>
  shardHostName:                            ## << Host name for the shard 3 pod. Example: oshard3 >>
  storageType:                              ## << Type of the storage. E.g. "bv" for block volume >>
  oci:
   region:                                  ## << select the values you used in "Label the Nodes" for "failure-domain.beta.kubernetes.io/region". Example: "primary_region" >>
   zone:                                    ## << select the values you used in "Label the Nodes" for "failure-domain.beta.kubernetes.io/zone" . Example: "primary_zone" >>
  pvc:                                      ## << Persistent Volume Claim  >>
   storageSize:                             ## << Shard 3 Size of the PVC Storage. Example: 50Gi  >>
   accessModes:                             ## << Shard 3 Access Mode of the PVC Storage. Example: ReadWriteOnce >> 
   storageClassName:                        ## << Shard 3 Plugin for Volume plugin used by a Persistent Volume Claim. Example: oci  >> 
   stagingLoc:                              ## << Shard 3 Staging location for shard1 setup scripts inside the pod. Example: /opt/oracle/scripts/setup >> 
  env:
   ORACLE_SID:                              ## << Mandatory variable for CDB SID Name for shard 3. Example: ORCL3CDB >> 
   ORACLE_PDB:                              ## << Mandatory variable for PDB Name for shard 3. Example: ORCL13PDB >> 
   OP_TYPE:                                 ## << Mandatory variable for operation type for shard 3, set to primaryshard >> 
   SGA_SIZE:                                ## << Optional variable for SGA SIZE for Catalog. Example: 6144. The values are in MB. Do not add "MB" after numeric value" >> 
   PGA_SIZE:                                ## << Optional variable for PGA SIZE for Catalog. Example: 2048. The values are in MB. Do not add "MB" after numeric value" >>
   COMMON_OS_PWD_FILE:                      ## << Encrupted OS Password file. Example: common_os_pwdfile.enc >>
   PWD_KEY:                                 ## << File with the password key. Example: pwd.key >>
   OLD_ORACLE_SID:                          ## << If you are using DB clone feature, you need to set the old cdb name to be cloned. >>
   OLD_ORACLE_PDB:                          ## << If you are using DB clone feature, you need to set the old pdb name to be cloned. >>
   SECRET_VOLUME:                           ## << Mandatory parameter for secret volume inside the pod. Default value set to  "/mnt/secrets". >> 
  clone:
   db:                                      ## << Optional parameter and default value is set to "false" in values.yaml. Set to "true", if you are using db clone feature. >>
   ocid:                                    ## << Optional parameter and value is set to OCID of the Block Volume having the Gold Image of the database >>
  nfs:
   storageClassName:                        ## << Shard 3 - Type of the storage class for the nfs. example: oci-fss >>
   mountOptions:                            ## << Shard 3 - Mount option for the storage. Example: To block operation of suid,sgid bits, use nosuid >>
   serverName:                              ## << Shard 3 - IP for the NFS Server. Example: Use an IP in format: xx.xx.xx.xx >> 
   path:                                    ## << Shard 3 - to mount the nfs. Example: /shard_nfs >>
```

### Catalog Configuration Parameters

```
oshard-catalog:
  replicaCount:                             ## << Count of Replica Pods for Shard3. We are keeping it 1 as are going to have 1 pod for catalog>>
  app:                                      ## << Application name for shard 3. Example: oshard-cat >>
  nodeselector:                             ## << select the values you used in "Label the Nodes" for "oracleshard". Example: "oshard" or  >>
  shardHostName:                            ## << Host name for the shard 3 pod. Example: oshard-catalog >>
  storageType:                              ## << Type of the storage. E.g. "bv" for block volume >>
  oci:
   region:                                  ## << select the values you used in "Label the Nodes" for "failure-domain.beta.kubernetes.io/region". Example: "primary_region" >>
   zone:                                    ## << select the values you used in "Label the Nodes" for "failure-domain.beta.kubernetes.io/zone" . Example: "primary_zone" >>
  pvc:                                      ## << Persistent Volume Claim  >>
   storageSize:                             ## << Catalog Size of the PVC Storage. Example: 50Gi  >>
   accessModes:                             ## << Catalog Access Mode of the PVC Storage. Example: ReadWriteOnce >> 
   storageClassName:                        ## << Catalog Plugin for Volume plugin used by a Persistent Volume Claim. Example: oci  >> 
   stagingLoc:                              ## << Catalog Staging location for shard1 setup scripts inside the pod. Example: /opt/oracle/scripts/setup >> 
  env:
   ORACLE_SID:                              ## << Mandatory variable for CDB SID Name for Catalog. Example: CATCDB >> 
   ORACLE_PDB:                              ## << Mandatory variable for PDB Name for Catalog. Example: ORCL13PDB >> 
   OP_TYPE:                                 ## << Mandatory variable for operation type for Catalog, set to catalog >> 
   SGA_SIZE:                                ## << Optional variable for SGA SIZE for Catalog. Example: 6144. The values are in MB. Do not add "MB" after numeric value" >> 
   PGA_SIZE:                                ## << Optional variable for PGA SIZE for Catalog. Example: 2048. The values are in MB. Do not add "MB" after numeric value" >>
   COMMON_OS_PWD_FILE:                      ## << Encrupted OS Password file. Example: common_os_pwdfile.enc >>
   PWD_KEY:                                 ## << File with the password key. Example: pwd.key >>
   OLD_ORACLE_SID:                          ## << If you are using DB clone feature, you need to set the old cdb name to be cloned. >>
   OLD_ORACLE_PDB:                          ## << If you are using DB clone feature, you need to set the old pdb name to be cloned. >>
   SECRET_VOLUME:                           ## << Mandatory parameter for secret volume inside the pod. Default value set to  "/mnt/secrets". >> 
  clone:
   db:                                      ## << Optional parameter and default value is set to "false" in values.yaml. Set to "true", if you are using db clone feature. >>
   ocid:                                    ## << Optional parameter and value is set to OCID of the Block Volume having the Gold Image of the database >>
  nfs:
   storageClassName:                        ## << Catalog - Type of the storage class for the nfs. example: oci-fss >>
   mountOptions:                            ## << Catalog - Mount option for the storage. Example: To block operation of suid,sgid bits, use nosuid >>
   serverName:                              ## << Catalog - IP for the NFS Server. Example: Use an IP in format: xx.xx.xx.xx >> 
   path:                                    ## << Catalog - to mount the nfs. Example: /shard_nfs >>
```
