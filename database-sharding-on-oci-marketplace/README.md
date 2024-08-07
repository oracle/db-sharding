- [Deploying Oracle Globally Distributed Database on Oracle Cloud Marketplace](#deploying-oracle-globally-distributed-database-on-oracle-cloud-marketplace)
  * [Finding Oracle Globally Distributed Database on Oracle Cloud (OCI) Marketplace](#finding-oracle-globally-distributed-database-on-oracle-cloud--oci--marketplace)
    + [Finding Oracle Globally Distributed Database on Public cloud marketplace](#finding-oracle-globally-distributed-database-on-public-cloud-marketplace)
    + [Finding Oracle Globally Distributed Database on OCI console](#finding-oracle-globally-distributed-database-on-oci-console)
  * [Prerequisites for creating an Oracle Globally Distributed Database instance on OCI marketplace](#prerequisites-for-creating-an-oracle-globally-distributed-database-instance-on-oci-marketplace)
  * [Fill in the required Stack information](#fill-in-the-required-stack-information)
  * [Fill in the Sharding configuration details](#fill-in-the-sharding-configuration-details)
  * [Oracle Globally Distributed Database Deployment job](#oracle-globally-distributed-database-deployment-job)
    + [Oracle Globally Distributed Database Deployment job status](#oracle-globally-distributed-database-deployment-job-status)
    + [Oracle Globally Distributed Database Deployment job outputs](#oracle-globally-distributed-database-deployment-job-outputs)
    + [Oracle Globally Distributed Database Deployment job associated resources](#oracle-globally-distributed-database-deployment-job-associated-resources)
      - [Navigating to a Shard Director from associated resources](#navigating-to-a-shard-director-from-associated-resources)
      - [Navigating to a Shard from associated resources](#navigating-to-a-shard-from-associated-resources)
    + [Oracle Globally Distributed Database initial random credentials](#oracle-globally-distributed-database-initial-random-credentials)
- [Managing Oracle Globally Distributed Databasepreviously created from OCI Marketplace](#managing-oracle-globally-distributed-database-previously-created-from-oci-marketplace)
  * [Scaling Shards](#scaling-shards)
    + [Oracle Globally Distributed Database edit stack apply job](#oracle-globally-distributed-database-edit-stack-apply-job)
      - [Oracle Globally Distributed Database edit stack job running](#oracle-globally-distributed-database-edit-stack-job-running)
      - [Oracle Globally Distributed Database edit stack job completion](#oracle-globally-distributed-database-edit-stack-job-completion)
      - [Oracle Globally Distributed Database edit stack job outputs](#oracle-globally-distributed-database-edit-stack-job-outputs)
  * [Scaling Shard Directors](#scaling-shard-directors)
- [Terminating Oracle Globally Distributed Database created from Oracle Cloud Marketplace](#terminating-oracle-globally-distributed-database-created-from-oracle-cloud-marketplace)
- [Oracle Globally Distributed Database Resources](#oracle-database-sharding-resources)


# Dracle Globally Distributed Database on Oracle Cloud Marketplace

## Finding Oracle Globally Distributed Database on Oracle Cloud (OCI) Marketplace

### Finding Oracle Globally Distributed Database on Public cloud marketplace

* Choose IaaS 
* Choose Oracle Cloud Infrastructure
* Select Oracle Globally Distributed Database from the list of Featured Apps.

Oracle Globally Distributed Database can be found on the featured list right at the top of the OCI applications on Oracle public marketplace.

![Oracle Globally Distributed Database on OCI Public Marketplace](
        https://github.com/oracle/db-sharding/blob/master/database-sharding-on-oci-marketplace/static-assets/ODS-Featured-App-on-Public-Marketplace.png
      )

![Oracle Globally Distributed Database on OCI Public Marketplace Featured Applications](
        https://github.com/oracle/db-sharding/blob/master/database-sharding-on-oci-marketplace/static-assets/Oracle-Database-Sharding-on-OCI-Public-Marketplace-featured.png
      )

2. Go to the following listing link:  
[Oracle Database Sharding Listing on Oracle Cloud Public Marketplace ](https://cloudmarketplace.oracle.com/marketplace/en_US/listing/74654105) 

### Finding Oracle Globally Distributed Database on OCI console

1. Log in to the OCI console and click on the left tooltips navigation menu.
2. Select <strong>OCI marketplace</strong> under the <strong>solutions and marketplace</strong> section in the left navigation menu.
3. Find Oracle Globally Distributed Database in the Featured Apps.
4. If you don't find Oracle Globally Distributed Database for some reason, then search for it in the marketplace search bar.
5. Click the Oracle Globally Distributed Database Tile. The listing for Oracle Globally Distributed Database opens.

![Oracle Globally Distributed Database on OCI console marketplace ](
        https://github.com/oracle/db-sharding/blob/master/database-sharding-on-oci-marketplace/static-assets/OracleDatabaseSharding-Featured-Marketplace-App-on-OCI-Console.png
      )

Oracle Globally Distributed Database Listing

![Oracle Database Sharding Listing](
        https://github.com/oracle/db-sharding/blob/master/database-sharding-on-oci-marketplace/static-assets/ODS-Listing-page-1.png
      )

## Prerequisites for creating an Oracle Globally Distributed Database instance on OCI marketplace

Before you attempt to deploy an Oracle Globally Distributed Database instance, Oracle recommends that you check the prerequisites for creating an instance as specified in the system requirements section of the Oracle Globally Distributed Database listing. 

Because automatic and uniform distribution of Oracle Globally Distributed Database resources within a region is employed, Oracle recommends that before you perform an initial deployment, or before you scale the deployment, check to ensure there is a sufficient number of the resources and services listed below in each availability domain within the region selected. 

The resources and services to check for limits, quotas and availability are in the Governance section of the tooltips menu on the left navigation pane. These resources and services are:

1. Database service for Shard and Catalog databases (based on the shape that you want to select).

2. Compute service for Shard Directors (based on the shape that you want to select).

## Fill in the required Stack information

After clicking <strong>Launch instance</strong> on the Oracle Database Sharding Marketplace product page, fill in the required stack information.

![Oracle Globally Distributed Database Fill in Stack Info - part 1](
        https://github.com/oracle/db-sharding/blob/master/database-sharding-on-oci-marketplace/static-assets/ODS-Fill-in-stack-Info.png
      )

![Oracle Globally Distributed Database Fill in Stack Info - part 2](
        https://github.com/oracle/db-sharding/blob/master/database-sharding-on-oci-marketplace/static-assets/ODS-Fill-in-Stack-Info-part2.png
      )

* Name - Name of the Stack. The stack is given a default name, and a date time stamp. You can edit this detail, if required.
* Description - The description that you provide while creating the Stack.
* Create In Compartment – The compartment defaults to the compartment you had selected on the Oracle Globally Distributed Database Marketplace page before clicking <strong>Launch stack</strong>.
* Terraform Version - It defaults to 0.12x.
* Tags (optional) – Tags are a convenient way to assign a tracking mechanism but are not mandatory. You can assign a tag of your choice for easy tracking. You have to assign a tag for some environments for cost analysis purposes.
 
Click <strong>Next</strong>.

## Fill in the Sharding configuration details 

**Oracle Globally Distributed Database General Configuration**

![Oracle Globally Distributed Database General Configuration - part 1](
        https://github.com/oracle/db-sharding/blob/master/database-sharding-on-oci-marketplace/static-assets/ODS-General-config-part-1.png
      )

![Oracle Globally Distributed Database General Configuration - part 2](
        https://github.com/oracle/db-sharding/blob/master/database-sharding-on-oci-marketplace/static-assets/ODS-General-config-part-2.png
      )

* SHARDED DATABASE NAME - The name is used as the sharded database name as well as a prefix for all sharded database resource display names. The name specified must be unique within the subnet.
* SHARDING METHOD 
* DATABASE SOFTWARE EDITION 
* CHOOSE STORAGE MANAGEMENT SOFTWARE
* CHOOSE A LICENSE TYPE 
* DATABASE VERSION

**Shard configuration**

![Oracle Globally Distributed Database Shard Configuration](
        https://github.com/oracle/db-sharding/blob/master/database-sharding-on-oci-marketplace/static-assets/ODS-Shard-config.png
      )

* SHARD SHAPE
* NUMBER OF PRIMARY SHARDS - The number of primary shard catalog databases. The total number of Shard catalog databases provisioned is determined by the value of the Replication Factor chosen. Shard Catalogs will be uniformly distributed across all availability domains in the current region and across fault domains within each availability domain.
* SHARD DATABASE AVAILABLE STORAGE SIZE IN GB


**Shard Catalog configuration**

![Oracle Globally Distributed Database Shard Catalog Configuration](
        https://github.com/oracle/db-sharding/blob/master/database-sharding-on-oci-marketplace/static-assets/ODS-Shard-catalog-config.png
      )

* SHARD CATALOG SHAPE
* NUMBER OF PRIMARY SHARD CATALOGS
* SHARD CATALOG DATABASE AVAILABLE STORAGE SIZE IN GB


**Replication configuration**

![Oracle Globally Distributed Database Replication Configuration](
        https://github.com/oracle/db-sharding/blob/master/database-sharding-on-oci-marketplace/static-assets/ODS-Replication-Config.png
      )

* REPLICATION FACTOR - Choose a replicaton factor. The total number of shard databases provisioned will be equal to the number of primary shards you have selected, multiplied by the chosen Replication Factor. The total number of primary catalog databases provisioned will be equal to the Replication Factor chosen.

**Shard Director configuration**

![Oracle Globally Distributed Database Shard Director Configuration](
        https://github.com/oracle/db-sharding/blob/master/database-sharding-on-oci-marketplace/static-assets/ODS-Shard-Director-Config.png
      )

* SHARD DIRECTOR SHAPE
* NUMBER OF SHARD DIRECTORS - Choose the number of shard directors to be deployed.

**Network Settings**

![Oracle Globally Distributed Database Network Configuration - part 1](
       https://github.com/oracle/db-sharding/blob/master/database-sharding-on-oci-marketplace/static-assets/ODS-Create-new-network.png
      )

![Oracle Globally Distributed Database Network Configuration - part 2](
       https://github.com/oracle/db-sharding/blob/master/database-sharding-on-oci-marketplace/static-assets/ODS-Create-new-network-part-2.png
      )


* CREATE NEW NETWORK - Select this option to create a new regional network (recommended if this is your first sharded database), or Uncheck this option to use an existing regional network from a previously created sharded database
* VCN NETWORK COMPARTMENT- Compartment for new or existing Virtual Cloud Network (VCN)
* VCN - Existing VCN to use for sharded database if not creating a new network
* SUBNET NETWORK COMPARTMENT - Compartment for new or existing Subnet
* CHOOSE A REGIONAL SUBNET - Existing Regional Subnet to use for sharded database if not creating a new network.

**ssh configuration**

![Oracle Globally Distributed Database SSH Configuration](
       https://github.com/oracle/db-sharding/blob/master/database-sharding-on-oci-marketplace/static-assets/ODS-ssh-config.png
      )

* SSH PUBLIC KEY - Public key for allowing SSH access to the sharded database hosts.

![Oracle Globally Distributed Database SSH Configuration](
       https://github.com/oracle/db-sharding/blob/master/database-sharding-on-oci-marketplace/static-assets/ODS-config-review.png
      )

![Oracle Globally Distributed Database SSH Configuration](
       https://github.com/oracle/db-sharding/blob/master/database-sharding-on-oci-marketplace/static-assets/ODS-config-review-part-2.png
      )

* On the Review page, review the information you provided and then click <strong>Create</strong>.
* After clicking <strong>Create</strong>, you will be navigated to the Stacks Job Details page. You can monitor the creation of the Oracle Database Sharding using this page.
* Upon completion, you can view the output pane on the left for connection information to Oracle Globally Distributed Database.

## Oracle Globally Distributed Database Deployment job

Once you click create on the review page show above, an Oracle Resource Manager (ORM) job is automatically created which starts deploying Oracle Sharded Database.

![Oracle Sharded Database deployment job](
       https://github.com/oracle/db-sharding/blob/master/database-sharding-on-oci-marketplace/static-assets/ODS-post-clicking-Create-ORM-job.png
      )

### Oracle Globally Distributed Database Deployment job status

Deployments with Replication Factor set to 1 take about 15 minutes. Deployments with Replication Factor set to 2 will take about 45 minutes. When completed, the Oracle Sharded Database deployment job will have succeeded For example:

![Oracle Globally Distributed Database Deployment success](
       https://github.com/oracle/db-sharding/blob/master/database-sharding-on-oci-marketplace/static-assets/ODS-stack-orm-job-success.png
      )

![Oracle Globally Distributed Database Deployment orm job success details](
       https://github.com/oracle/db-sharding/blob/master/database-sharding-on-oci-marketplace/static-assets/ODS-orm-job-success-details.png
      ) 

### Oracle Globally Distributed Database Deployment job outputs

![Oracle Sharded Database Deployment orm job success details](
       https://github.com/oracle/db-sharding/blob/master/database-sharding-on-oci-marketplace/static-assets/ODS-orm-job-outputs.png
      )


### Oracle Globally Distributed Database Deployment job associated resources

All resources that are provisioned for the successful deployment of Oracle Globally Distributed Database are listed in the associated resources pane on the left menu within the job page, as shown below.

![Oracle Globally Distributed Database Deployment orm job associated resources](
       https://github.com/oracle/db-sharding/blob/master/database-sharding-on-oci-marketplace/static-assets/ODS-associated-resources.png
      )


#### Navigating to a Shard Director from associated resources

A _Shard Director_ is a compute that can be directly accessed by clicking on the *sd* resource link.

1. Click the shard director resource. For example: 

![Oracle Globally Distributed Database Deployment ORM job associated resource shard director selection](
       https://github.com/oracle/db-sharding/blob/master/database-sharding-on-oci-marketplace/static-assets/ODS-associated-resource-shard-director-compute.png
      )
2. A new tab opens up with the details of the compute on which the Shard Director runs. For example: 

![Oracle Globally Distributed Database Deployment ORM job associated resource shard director compute details](
       https://github.com/oracle/db-sharding/blob/master/database-sharding-on-oci-marketplace/static-assets/ODS-associated-compute-details-for-shard-director.png
      )

#### Navigating to a Shard from associated resources

Shard which is a Oracle database can be directly accessed by clicking on the *sh* resource link.

1. Click the shard resource. For example: 

![Oracle Globally Distributed Database Deployment orm job associated resource shard database selection](
       https://github.com/oracle/db-sharding/blob/master/database-sharding-on-oci-marketplace/static-assets/ODS-Associated-resource-Select-shard.png
      )

2. A new tab opens up with the details of the database db system on which the Shard is located. For example: 

![Oracle Globally Distributed Database Deployment orm job associated resource shard database details](
       https://github.com/oracle/db-sharding/blob/master/database-sharding-on-oci-marketplace/static-assets/ODS-dbvm.png
      )

### Oracle Globally Distributed Database initial random credentials

The initial random credentials for Oracle Globally Distributed Database is available in the view state which is securely stored within Oracle Resource Manager. Users are recommended to change this on their first attempt to use these initial random passwords.

![Oracle Globally Distributed Database Deployment orm job associated resource shard database details](
       https://github.com/oracle/db-sharding/blob/master/database-sharding-on-oci-marketplace/static-assets/ODS-Default-initial-random-passwords.png
      )

# Managing Oracle Globally Distributed Database previously created from OCI Marketplace

* Click the left tooltips navigation Menu. Choose <strong>Resource Manager</strong> under <strong>Solutions and Marketplace</strong>.
* Select the stack that you created from marketplace.
* Click <strong>Edit Stack</strong>.

## Scaling Shards

![Oracle Globally Distributed Database Edit stack select Configuration](
      https://github.com/oracle/db-sharding/blob/master/database-sharding-on-oci-marketplace/static-assets/ODS-Edit-Stack-select.png
      )

![Oracle Globally Distributed Database Scaling Configuration](
       https://github.com/oracle/db-sharding/blob/master/database-sharding-on-oci-marketplace/static-assets/ODS-Stack-scale-both.png
      )

![Oracle Globally Distributed Database Edit stack toggling Configuration](
      https://github.com/oracle/db-sharding/blob/master/database-sharding-on-oci-marketplace/static-assets/ODS-edit-stack-after-toggling.png
      )

Shards can be _scaled in_ (decreased) or _scaled out_ (decreased), based on your requirements and as needed. You can scale the shards without affecting performance of queries per second (qps) or transactions per second (tps) and with zero downtime.

* _Update the number of Shard Directors_


* Review and Save changes

![Oracle Globally Distributed Database Review Edit Configuration](
       https://github.com/oracle/db-sharding/blob/master/database-sharding-on-oci-marketplace/static-assets/ODS-Edit-stack-review.png
      )

* Click <strong>Terraform Actions</strong>, and then click <strong>Apply</strong>.

![Oracle Sharded Database Review Edit Configuration](
       https://github.com/oracle/db-sharding/blob/master/database-sharding-on-oci-marketplace/static-assets/ODS-Apply-after-edit-stack.png
      )

### Oracle Globally Distributed Database edit stack apply job

After clicking <strong>Apply job</strong>, a confirmation dialog appears to confirm you want to apply the changes to the existing sharded database deployment.

![Oracle Globally Distributed Database edit stack apply job approve dialog](
       https://github.com/oracle/db-sharding/blob/master/database-sharding-on-oci-marketplace/static-assets/ODS-Apply-dialog-post-edit-stack.png
      )

#### Oracle Globally Distributed Database edit stack job running

![Oracle Sharded Database edit stack apply job running](
      https://github.com/oracle/db-sharding/blob/master/database-sharding-on-oci-marketplace/static-assets/ODS-post-apply-job-running.png
      )

#### Oracle Globally Distributed Database edit stack job completion

Depending upon the changes requested in the sharding edit stack, the apply job will make the necessary changes in the sharded database resources. After the changes are successfully applied, the job status will appear as follows:

![Oracle Globally Distributed Database edit stack apply job completed](
      https://github.com/oracle/db-sharding/blob/master/database-sharding-on-oci-marketplace/static-assets/ODS-Edit-stack-apply-job-completed.png
      )

The apply job details that is responsible for making the changes to the sharded database appears as follows when the changes are complete: 

![Oracle Globally Distributed Database edit stack apply job details completed](
      https://github.com/oracle/db-sharding/blob/master/database-sharding-on-oci-marketplace/static-assets/ODS-Edit-stack-apply-job-drill-down.png
      )

#### Oracle Globally Distributed Databaseedit stack job outputs

After the edit stack apply job has completed successfully, the outputs section on the left pane of the apply job page contain the details on how to access the sharded database. For example: 

![Oracle Globally Distributed Database edit stack job outputs](
      https://github.com/oracle/db-sharding/blob/master/database-sharding-on-oci-marketplace/static-assets/ODS-edit-stack-job-output.png
      )

## Scaling Shard Directors

Shard directors can be _scaled in_ (decreased) or _scaled out_ (decreased), based on your requirements and as needed. Scaling does not affect performance and is done with zero downtime.

The steps for scaling are the same as scaling shards as shown in the previous section. Change the shard director configuration as show below before saving changes, and then run an apply job to deploy the changes.

![Oracle Globally Distributed Database edit stack for shard directors](
      https://github.com/oracle/db-sharding/blob/master/database-sharding-on-oci-marketplace/static-assets/ODS-edit-stack-after-toggling.png
      )

# Terminating Oracle Globally Distributed Database created from Oracle Cloud Marketplace

To terminate an Oracle Globally Distributed Database created from OCI marketplace in the past, follow the steps below :

* Click on the left tooltips Menu. Choose <strong>Resource Manager</strong> under <strong>Solutions and Marketplace</strong>.
* Select the stack that you created from marketplace.
* Click <strong>Terraform Actions</strong>, and then click <strong>Destroy</strong>. After you click <strong>Destroy</strong>, all Sharded database resources including any user data will be deleted from OCI. 
* After the destroy operation has completed, the jobs section will show that the destroy job has been successfully completed. For example: 

![Oracle Globally Distributed Database Destroy selection](
      https://github.com/oracle/db-sharding/blob/master/database-sharding-on-oci-marketplace/static-assets/ODS-Destroy-select.png
      )

![Oracle Globally Distributed Database Destroy job approve](
      https://github.com/oracle/db-sharding/blob/master/database-sharding-on-oci-marketplace/static-assets/ODS-Destroy-Dialog.png
      )

![Oracle Globally Distributed Database Destroy job running](
      https://github.com/oracle/db-sharding/blob/master/database-sharding-on-oci-marketplace/static-assets/ODS-Destroy-job-running.png
      )

![Oracle Globally Distributed Database Destroy job completed](
      https://github.com/oracle/db-sharding/blob/master/database-sharding-on-oci-marketplace/static-assets/ODS-Destroy-job-completed-stack-view.png
      )

# Oracle Globally Distributed Database Resources

For more details on Oracle Database Sharding and usage, see: [Product page](https://www.oracle.com/database/technologies/high-availability/sharding.html). 

For product documentation, see: [product release documentation](https://docs.oracle.com/en/database/oracle/oracle-database/23/shard/overview1.html)

For GDSCTL CLI commands to manage sharded database manually once deployed on OCI, please refer to the [Oracle Globally Distributed Database GDSCTL CLI command reference](https://docs.oracle.com/en/database/oracle/oracle-database/23/gsmug/gdsctl-sharding-env.html)  