[SDB-terraform-onprem]: https://github.com/oracle/db-sharding/tree/master/deployment-with-terraform/sdb-terraform-onprem
[SDB-terraform-oci]: https://github.com/oracle/db-sharding/tree/master/deployment-with-terraform/sdb-terraform-oci
[SDB-terraform]: https://github.com/oracle/db-sharding/tree/master/deployment-with-terraform/
[MTR-Intro]: https://github.com/oracle/db-sharding/wiki/Sharded-Database-Mid-Tier-Routing#introduction
[SDB-prod-page]: https://www.oracle.com/database/technologies/high-availability/sharding.html
[SDB-prod-doc]: https://docs.oracle.com/en/database/oracle/oracle-database/19/shard/
[OCI]: https://www.oracle.com/cloud/
[SDB]: https://www.oracle.com/database/technologies/high-availability/sharding.html

- [About](#about)
- [Terraform based deployment of Oracle Globally Distributed Database on Oracle Cloud Infrastructure](#terraform-based-deployment-of-oracle-globally-distributed-database-on-oracle-cloud-infrastructure)
  * [Overview](#overview)
  * [Details](#details)
- [Terraform based On-Premise deployment of Oracle Globally Distributed Database](#terraform-based-deployment-of-oracle-globally-distributed-database-on-premise)
  * [Overview](#overview-1)
  * [Details](#details-1)

## About

This library provides the following Oracle Globally Distributed Database deployment automation tools:

* Terraform based deployment of Oracle Globally Distributed Database on Oracle Cloud Infrastructure.
* Terraform based deployment of Oracle Globally Distributed Database On-Premise.

If you want to learn more about Oracle Globally Distributed Database (previously known as Oracle Sharded Database), see: [product page][SDB-prod-page] and [product documentation][SDB-prod-doc].

<strong> Note </strong> : Oracle Sharding (SDB) and Oracle Globally Distributed Database are interchangeably used names in the documentation and refer to the same entity. 

 
## Terraform based deployment of Oracle Globally Distributed Database on Oracle Cloud Infrastructure

### Overview 

This deployment provides Terraform scripts that provision the necessary components and resources for a quick and easy setup of [Oracle Globally Distributed Database][SDB] (SDB) on [Oracle Cloud Infrastructure][OCI] (OCI). It creates and configures SDB infrastructure components necessary for a successful SDB setup, such as Oracle Shard Directors, Shard Catalogs, and Shards. It also gives you the option to create Shard Standbys as well as Catalog standby using Oracle Data Guard for replication to provide high-availability and disaster recovery of the sharded data.

### Details

For more details, see: [SDB-Terraform-OCI][SDB-terraform-oci] documentation.

## Terraform based deployment of Oracle Globally Distributed Database On-Premise

### Overview 

Provides [Oracle Globally Distributed Database][SDB] (SDB) Terraform modules and scripts that provisions the necessary components and resources required for a quick and easy setup of Oracle Sharded Database on-prem. It creates and configures SDB infrastructure components necessary for a successful SDB setup, such as Oracle Shard Directors, Shard Catalogs, and Shards. It also gives you the option to deploy Shard Standbys as well as Catalog standby using Oracle Data Guard for replication to provide high-availability and disaster recovery of the sharded data.

### Details

For more details, see: [SDB-Terraform-OnPrem][SDB-terraform-onprem] documentation.

