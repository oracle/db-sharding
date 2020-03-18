[SDB-terraform-onprem]: https://github.com/oracle/db-sharding/tree/master/deployment-with-terraform/sdb-terraform-onprem
[SDB-terraform-oci]: https://github.com/oracle/db-sharding/tree/master/deployment-with-terraform/sdb-terraform-oci
[SDB-terraform]: https://github.com/oracle/db-sharding/tree/master/deployment-with-terraform/
[MTR-Intro]: https://github.com/oracle/db-sharding/wiki/Sharded-Database-Mid-Tier-Routing#introduction
[SDB-prod-page]: https://www.oracle.com/database/technologies/high-availability/sharding.html
[SDB-prod-doc]: https://docs.oracle.com/en/database/oracle/oracle-database/19/shard/
[OCI]: https://www.oracle.com/cloud/
[SDB]: https://www.oracle.com/database/technologies/high-availability/sharding.html

- [About](#about)
- [Terraform based deployment of Oracle sharded database on Oracle Cloud Infrastructure](#terraform-based-deployment-of-oracle-sharded-database-on-oracle-cloud-infrastructure)
  * [Overview](#overview)
  * [Details](#details)
- [Terraform based On-Premise deployment of Oracle sharded database](#terraform-based-deployment-of-oracle-sharded-database-on-premise)
  * [Overview](#overview-1)
  * [Details](#details-1)

## About

This library provides the following Oracle Sharded database deployment automation tools:

* Terraform based deployment of Oracle sharded database on Oracle Cloud Infrastructure.
* Terraform based deployment of Oracle sharded database On-Premise.

If you want to learn more about Oracle sharding a.k.a Oracle Sharded Database, please refer to the [product page][SDB-prod-page] and [product documentation][SDB-prod-doc].

<strong> Note </strong> : Oracle Sharding and Oracle Sharded Database (SDB) are interchangeably used names in the documentation and refer to the same entity. 

 
## Terraform based deployment of Oracle sharded database on Oracle Cloud Infrastructure

### Overview 

Provides Terraform scripts that provisions the necessary components and resources for a quick and easy setup of [Oracle Sharded Database][SDB] (SDB) on [Oracle Cloud Infrastructure][OCI] (OCI). It creates and configures SDB infrastructure components necessary for a successful SDB setup, such as Oracle Shard Director(s), Shard Catalog(s), Shard(s) and optionally Shard Standby(s) as well as Catalog standby using Data Guard for replication to provide high-availability and/or disaster recovery of the sharded data.

### Details

For more details, please refer to the [SDB-Terraform-OCI][SDB-terraform-oci] documentation.

## Terraform based deployment of Oracle sharded database On-Premise

### Overview 

Provides [Oracle Sharded Database][SDB] (SDB) Terraform modules and scripts that provisions the necessary components and resources required for a quick and easy setup of Oracle Sharded Database on-prem. It creates and configures SDB infrastructure components necessary for a successful SDB setup, such as Oracle Shard Director(s), Shard Catalog(s), Shard(s) and optionally Shard Standby(s) as well as Catalog standby using Data Guard for replication to provide high-availability and/or disaster recovery of the sharded data.

### Details

For more details, please refer to the [SDB-Terraform-OnPrem][SDB-terraform-onprem] documentation.

