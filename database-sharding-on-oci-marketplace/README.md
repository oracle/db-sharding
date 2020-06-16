# Deploying Oracle Database Sharding on Oracle Cloud Marketplace

## Finding Oracle Database Sharding on Oracle Cloud (OCI) Marketplace

### Finding Oracle Database Sharding  on Public cloud marketplace

* Choose IaaS 
* Choose Oracle Cloud Infrastructure
* Select Oracle Database Sharding from the list of Featured Apps.

Oracle Database Sharding can be found on the featured list right at the top of the OCI applications on Oracle public marketplace.

![Oracle Database Sharding on OCI Public Marketplace](
        https://github.com/oracle/db-sharding/blob/master/database-sharding-on-oci-marketplace/static-assets/ODS-Featured-App-on-Public-Marketplace.png
      )

![Oracle Database Sharding on OCI Public Marketplace Featured Applications](
        https://github.com/oracle/db-sharding/blob/master/database-sharding-on-oci-marketplace/static-assets/Oracle-Database-Sharding-on-OCI-Public-Marketplace-featured.png
      )

2. Go directly to the following listing link at :  
[Oracle Database Sharding Listing on Oracle Cloud Public Marketplace ](https://cloudmarketplace.oracle.com/marketplace/en_US/listing/74654105) 

### Finding Oracle Database Sharding on OCI console.

1. Once you login to the OCI console, Click on the left hamburger navigation menu.
2. Next, select OCI marketplace under solutions & marketplace section in the left navigation menu.
3. Next, you should be able to immediately find Oracle Database Sharding in the Featured Apps.
4. If you don't find Oracle Database Sharding for some reason, then search for it in the marketplace search bar.
5. Click on the Oracle Database Sharding Tile which will bring up the listing for Oracle Database Sharding.

![Oracle Database Sharding on OCI console marketplace ](
        https://github.com/oracle/db-sharding/blob/master/database-sharding-on-oci-marketplace/static-assets/OracleDatabaseSharding-Featured-Marketplace-App-on-OCI-Console.png
      )

Oracle Database Sharding Listing

![Oracle Database Sharding Listing](
        https://github.com/oracle/db-sharding/blob/master/database-sharding-on-oci-marketplace/static-assets/ODS-Listing-page-1.png
      )


### Fill in the required Stack information after clicking on Launch instance on the Oracle Database Sharding Marketplace product page 

![Oracle Database Sharding Fill in Stack Info - part 1](
        https://github.com/oracle/db-sharding/blob/master/database-sharding-on-oci-marketplace/static-assets/ODS-Fill-in-stack-Info.png
      )

![Oracle Database Sharding Fill in Stack Info - part 2](
        https://github.com/oracle/db-sharding/blob/master/database-sharding-on-oci-marketplace/static-assets/ODS-Fill-in-Stack-Info-part2.png
      )

* Name - Name of the Stack. It has a default name and provides a date time stamp. You can edit this detail, if required.
* Description - Description that you provide while creating the Stack.
* Create In Compartment – It defaults to the compartment you had selected on the Oracle Database Sharding Marketplace page before clicking on Launch stack.
* Terraform Version - It defaults to 0.12x.
* Tags (optional) – Tags are a convenient way to assign a tracking mechanism but are not mandatory. You can assign a tag of your choice for easy tracking. You have to assign a tag for some environments for cost analysis purposes.
 
Click Next.

### Fill in the required details to configure variables. 

**Oracle Sharded Database General Configuration**

![Oracle Sharded Database General Configuration - part 1](
        https://github.com/oracle/db-sharding/blob/master/database-sharding-on-oci-marketplace/static-assets/ODS-General-config-part-1.png
      )

![Oracle Sharded Database General Configuration - part 2](
        https://github.com/oracle/db-sharding/blob/master/database-sharding-on-oci-marketplace/static-assets/ODS-General-config-part-2.png
      )

* SHARDED DATABASE NAME - The name is used as the sharded database name as well as a prefix for all sharded database resource display names. The name specified must be unique within the subnet.
* SHARDING METHOD 
* DATABASE SOFTWARE EDITION 
* CHOOSE STORAGE MANAGEMENT SOFTWARE
* CHOOSE A LICENSE TYPE 
* DATABASE VERSION

**Shard configuration**

![Oracle Sharded Database Shard Configuration](
        https://github.com/oracle/db-sharding/blob/master/database-sharding-on-oci-marketplace/static-assets/ODS-Shard-config.png
      )

* SHARD SHAPE
* NUMBER OF PRIMARY SHARDS - The number of primary shard catalog databases. The total number of Shard catalog databases provisioned is determined by the value of the Replication Factor chosen. Shard Catalogs will be uniformly distributed across all availability domains in the current region and across fault domains within each availability domain.
* SHARD DATABASE AVAILABLE STORAGE SIZE IN GB


**Shard Catalog configuration**

![Oracle Sharded Database Shard Catalog Configuration](
        https://github.com/oracle/db-sharding/blob/master/database-sharding-on-oci-marketplace/static-assets/ODS-Shard-catalog-config.png
      )

* SHARD CATALOG SHAPE
* NUMBER OF PRIMARY SHARD CATALOGS
* SHARD CATALOG DATABASE AVAILABLE STORAGE SIZE IN GB


**Replication configuration**

![Oracle Sharded Database Replication Configuration](
        https://github.com/oracle/db-sharding/blob/master/database-sharding-on-oci-marketplace/static-assets/ODS-Replication-Config.png
      )

* REPLICATION FACTOR - Choose a replicaton factor. The total number of shard databases provisioned will be equal to the number of primary shards selected above multiplied by the chosen Replication Factor. The total number of primary catalog databases that will be provisioned will be equal to the Replication Factor chosen.

**Shard Director configuration**

![Oracle Sharded Database Shard Director Configuration](
        https://github.com/oracle/db-sharding/blob/master/database-sharding-on-oci-marketplace/static-assets/ODS-Shard-Director-Config.png
      )

* SHARD DIRECTOR SHAPE
* NUMBER OF SHARD DIRECTORS - Choose the number of shard directors to be deployed.

**Network Settings**

![Oracle Sharded Database Network Configuration - part 1](
       https://github.com/oracle/db-sharding/blob/master/database-sharding-on-oci-marketplace/static-assets/ODS-Create-new-network.png
      )

![Oracle Sharded Database Network Configuration - part 2](
       https://github.com/oracle/db-sharding/blob/master/database-sharding-on-oci-marketplace/static-assets/ODS-Create-new-network-part-2.png
      )


* CREATE NEW NETWORK - Check this to create a new regional network (recommended if this is your first sharded database) or Uncheck to use an existing regional network from a previously created sharded database
* VCN NETWORK COMPARTMENT- Compartment for new or existing Virtual Cloud Network (VCN)
* VCN - Existing VCN to use for sharded database if not creating a new network
* SUBNET NETWORK COMPARTMENT - Compartment for new or existing Subnet
* CHOOSE A REGIONAL SUBNET - Existing Regional Subnet to use for sharded database if not creating a new network.

**ssh configuration**

![Oracle Sharded Database SSH Configuration](
       https://github.com/oracle/db-sharding/blob/master/database-sharding-on-oci-marketplace/static-assets/ODS-ssh-config.png
      )

* SSH PUBLIC KEY - Public key for allowing SSH access to the sharded database hosts.

![Oracle Sharded Database SSH Configuration](
       https://github.com/oracle/db-sharding/blob/master/database-sharding-on-oci-marketplace/static-assets/ODS-config-review.png
      )

![Oracle Sharded Database SSH Configuration](
       https://github.com/oracle/db-sharding/blob/master/database-sharding-on-oci-marketplace/static-assets/ODS-config-review-part-2.png
      )

* On the Review page, review the information you provided and then click Create.
* After clicking Create, you will be navigated to the Stacks Job Details page. You can monitor the creation of the Oracle Database Sharding using this page.
* Upon completion, you can now view the output pane on the left for connection information to Oracle Sharded Database.


# Managing Oracle Database Sharding created from OCI Marketplace

* Click on the left hamburger navigation Menu. Choose Resource Manager under Solutions and Marketplace.
* Select the stack that you created from marketplace.
* Click on Edit Stack.

## **Scaling Shards**

![Oracle Sharded Database Edit stack select Configuration](
      https://github.com/oracle/db-sharding/blob/master/database-sharding-on-oci-marketplace/static-assets/ODS-Edit-Stack-select.png
      )

![Oracle Sharded Database Scaling Configuration](
       https://github.com/oracle/db-sharding/blob/master/database-sharding-on-oci-marketplace/static-assets/ODS-Stack-scale-both.png
      )

![Oracle Sharded Database Edit stack toggling Configuration](
      https://github.com/oracle/db-sharding/blob/master/database-sharding-on-oci-marketplace/static-assets/ODS-edit-stack-after-toggling.png
      )

Shards can be scaled in (decreased) or out (decreased)  based on user requirements and as needed without affecting performance of queries per second(qps) or transactions per second (tps) and with zero downtime.

* _Update the number of Shard Directors_


* Review and Save changes

![Oracle Sharded Database Review Edit Configuration](
       https://github.com/oracle/db-sharding/blob/master/database-sharding-on-oci-marketplace/static-assets/ODS-Edit-stack-review.png
      )

* Next click on Terraform Actions → Apply

![Oracle Sharded Database Review Edit Configuration](
       https://github.com/oracle/db-sharding/blob/master/database-sharding-on-oci-marketplace/static-assets/ODS-Apply-after-edit-stack.png
      )

## Scaling Shard Directors

Shard directors can be scaled in (decreased) or out (decreased) based on user requirements and as needed without affecting performance and with zero downtime.

The steps for scaling are the same as scaling shards as shown in the previous section. 


# Terminating Oracle Database Sharding created from Oracle Cloud Marketplace

To terminate an Oracle Sharded Database created from OCI marketplace in the past, follow the steps below :

* Click on the left hamburger Menu. Choose Resource Manager under Solutions and Marketplace.
* Select the stack that you created from marketplace.
* Click on Terraform Actions → Destroy. Once you click this all Sharded database resources including any user data will be deleted from OCI. 
* Once the destroy has completed the jobs section will show that the destroy job has been successfully completed.

![Oracle Sharded Database Destroy selection](
      https://github.com/oracle/db-sharding/blob/master/database-sharding-on-oci-marketplace/static-assets/ODS-Destroy-select.png
      )

![Oracle Sharded Database Destroy job approve](
      https://github.com/oracle/db-sharding/blob/master/database-sharding-on-oci-marketplace/static-assets/ODS-Destroy-Dialog.png
      )

![Oracle Sharded Database Destroy job running](
      https://github.com/oracle/db-sharding/blob/master/database-sharding-on-oci-marketplace/static-assets/ODS-Destroy-job-running.png
      )

![Oracle Sharded Database Destroy job completed](
      https://github.com/oracle/db-sharding/blob/master/database-sharding-on-oci-marketplace/static-assets/ODS-Destroy-job-completed-stack-view.png
      )

# Oracle Database Sharding Resources

For more details on Oracle Database Sharding and usage please refer to the [Product page](https://www.oracle.com/database/technologies/high-availability/sharding.html). 

For product documentation, please refer to the [product release documentation](https://docs.oracle.com/en/database/oracle/oracle-database/19/shard/sharding-overview.html)

For GDSCTL CLI commands to manage sharded database manually once deployed on OCI, please refer to the [Sharding GDSCTL CLI command reference](https://docs.oracle.com/en/database/oracle/oracle-database/19/gsmug/gdsctl-sharding-env.html)  