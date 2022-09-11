# sdb-terraform-oci

Terraform for deployment of [Oracle Sharded Database][SDB] on [Oracle Cloud Infrastructure][OCI]

[terraform]: https://releases.hashicorp.com/terraform/
[SDB]: https://www.oracle.com/database/technologies/high-availability/sharding.html
[SDB-Demo-zip]: https://support.oracle.com/epmos/faces/DocumentDisplay?id=2226341.1
[DG-Troubleshoot]: https://docs.oracle.com/en/database/oracle/oracle-database/19/dgbkr/troubleshooting-oracle-data-guard-broker.html#GUID-5B9D54C8-C446-4678-A770-2851C41C9265
[SDB-Deploy-Doc]: https://docs.oracle.com/en/database/oracle/oracle-database/19/shard/sharding-deployment.html#GUID-4E77F1B8-F665-40C4-B4AC-B321C7302AA9
[DG-Doc]: https://docs.oracle.com/database/121/SBYDB/create_ps.htm#SBYDB4722
[SDB-Issues]: https://orahub.oraclecorp.com/sharding/sdb-terraform/issues
[SDB-Repo]: https://orahub.oraclecorp.com/sharding/sdb-terraform
[SDB-Demo]: https://docs.oracle.com/en/database/oracle/oracle-database/19/shard/sharding-deployment.html#GUID-A3433C97-90F8-4CBF-ADA8-2AE2145612DB
[OCI]: https://docs.cloud.oracle.com/iaas/Content/home.htm
[subnet]: https://docs.cloud.oracle.com/iaas/Content/Network/Tasks/managingVCNs.htm#
[api-signing-key]: https://docs.cloud.oracle.com/iaas/Content/API/Concepts/apisigningkey.htm#How
[adding-users]: https://docs.cloud.oracle.com/iaas/Content/GSG/Tasks/addingusers.htm

# Terraform for deploying Oracle Sharded Database on [OCI][OCI]

## About

Provides Terraform scripts that provisions the necessary components and resources for a quick and easy setup of [Oracle Sharded Database][SDB] (SDB) on [Oracle Cloud Infrastructure][OCI] (OCI). It creates and configures SDB infrastructure components necessary for a successful SDB setup, such as Oracle Shard Director(s), Shard Catalog(s), Shard(s) and optionally Shard Standby(s) as well as Catalog standby using Data Guard for replication to provide high-availability and/or disaster recovery of the sharded data.

## Pre-requisites

1. An Oracle Cloud Infrastructure account

2. A user created in that account, in a group with a policy that grants the desired permissions. For an example of how to set up a new user, group, compartment, and policy, refer[Adding Users on OCI documentation][adding-users].

3. A keypair used for signing API requests, with the public key uploaded to Oracle. Only the user calling the API should be in possession of the private key. Please follow the [OCI API signing key documentation][api-signing-key] for steps to upload public key to OCI and make a copy of finger print which will need to be keyed into terraform.tfvars.

4. Atleast one <strong>non-regional [subnet][subnet]</strong> per AD exists and are created along with any  route/ingress/egress rules. (Make sure ingress and egress rules are defined for tcp ports 1521 and 1522 to be open on the subnets which are going to be used for sharded database deployment).

NOTE: regional subnets are not supported at this time.

5. Download the latest version of [Terraform][terraform] (minimum required version is 0.12)  
   and extract the <em>terraform</em> binary to any directory on the machine 
   (will be referred to as current machine in the rest of the documentation) where you are going to clone or download the sdb-terraform repository and want to invoke the SDB terraform.

6. Download the Global Service Manager(GSM) zip from OTN onto the current machine and provide 
   the full path for the same in terraform.tfvars

7. Ensure that you have ssh rsa key pair generated on your current/local machine. If not generate them with empty   
   passphrase. 
   
   ssh-keygen -b 2048 -t rsa

8. Ensure that you copy the local machine's ssh pub key into your local machine's list of authorized keys.

   cat ~/.ssh/id_rsa.pub >> ~/.ssh/authorized_keys

 
## Environment variables

1. Set proxy environment variables if you're running behind a proxy in your current machine's ~/.bashrc and save it.

```
 export http_proxy=http://<address_of_your_proxy>.com:80/
 export https_proxy=http://<address_of_your_proxy>:80/
```

2. Add the following exports to your current machine's ~/.bashrc and save it.

```
 export PATH=<replace-with-path-to-terraform-binary>:$PATH
 export TF_LOG=
```

## Quickstart

1. Download SDB Terraform for OCI source and setup input files.
    ```
    $ bash
    $ source ~/.bashrc  
    $ git clone https://github.com/oracle/db-sharding.git
    $ cd db-sharding/deployment-with-terraform/sdb-terraform-oci
    $ cp terraform.tfvars-example terraform.tfvars
    $ cp secrets.auto.tfvars-example secrets.auto.tfvars
    $ cp optional-variables.auto.tfvars-example optional-variables.auto.tfvars
    $ chmod 700 sdb-setup.sh
    ```
Note: Make sure terraform -v works and shows a version later than 0.12

2. Optionally, if you also want to setup the [SDB Demo application][SDB-Demo] with sample (dummy) data in all the shards and monitor the shards, copy the [SDB demo zip][SDB-Demo-zip] and place the sdb_demo_app.zip file into a directory on the current machine and point the full path to it in the optional-variables.auto.tfvars input file.
    
3. Override the values in the *.tfvars as per your sharding needs. Refer to the variables.tf file for more details on each of the variables found in the *.tfvars files.

4. Execute these following steps for SDB deployment on OCI (planning step is optional: Refer ./sdb-setup.sh -h for all the options).

   ```
   $  terraform init
   $  nohup ./sdb-setup.sh deploy >> nohup-setup-sdb.log 2>&1 &
   ```
## To teardown an existing sharded database setup

 ```
   $ nohup ./sdb-setup.sh destroy >> nohup-destroy-sdb.log 2>&1 &
 ```

## Current limitations of SDB Terraform OCI

1. Doesn't support user-defined and composite sharding.
2. Doesn't support Oracle Golden Gate (OGG) replication.
