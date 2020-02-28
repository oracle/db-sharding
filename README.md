# Oracle Sharded Database Tools Library

[SDB-terraform-onprem]: https://github.com/oracle/db-sharding/tree/master/sdb-terraform-onprem
[SDB-terraform-oci]: https://github.com/oracle/db-sharding/tree/master/sdb-terraform-oci
[SDB-Mid-Tier-Routing]: https://github.com/oracle/db-sharding/tree/master/Mid-Tier-Routing
[MTR-Intro]: https://github.com/oracle/db-sharding/wiki/Sharded-Database-Mid-Tier-Routing#introduction
[SDB-prod-page]: https://www.oracle.com/database/technologies/high-availability/sharding.html
[SDB-prod-doc]: https://docs.oracle.com/en/database/oracle/oracle-database/19/shard/
[OCI]: https://www.oracle.com/cloud/
[SDB]: https://www.oracle.com/database/technologies/high-availability/sharding.html

- [About](#about)
- [Terraform based deployment of Oracle sharded database](#terraform-based-deployment-of-oracle-sharded-database-on-oracle-cloud-infrastructure)
  * [Overview](#overview)
  * [Details](#details)
- [Kubernetes based deployment of Oracle sharded database](#terraform-based-deployment-of-oracle-sharded-database-on-premise)
  * [Overview](#overview-1)
  * [Details](#details-1)
- [Mid-tier routing for use in Oracle sharded database client applications](#mid-tier-routing-for-use-in-oracle-sharded-database-client-applications)
  * [Overview](#overview-2)
  * [Details](#details-2)
- [Routing implementations for use in Oracle sharding client applications](#routing-implementations-for-use-in-oracle-sharding-client-applications)
  * [Overview](#overview-3)
  * [Details](#details-3)

## About

This library provides Oracle Sharded database deployment automation and tools :

* Terraform based deployment of Oracle sharded database.
* Kubernetes based deployment of Oracle sharded database.
* Mid-tier routing for use in Oracle sharded database client applications.
* Routing implementations for use in Oracle sharding client applications.

If you want to learn more about Oracle sharding a.k.a Oracle Sharded Database, please refer to the [product page][SDB-prod-page] and [product documentation][SDB-prod-doc].

<strong> Note </strong> : Oracle Sharding and Oracle Sharded Database (SDB) are interchangeably used names in the documentation and refer to the same entity. 

 
## Terraform based deployment of Oracle sharded database

### Overview 

Provides Terraform scripts that provisions the necessary components and resources for a quick and easy setup of [Oracle Sharded Database][SDB] (SDB) on [Oracle Cloud Infrastructure][OCI] (OCI). It creates and configures SDB infrastructure components necessary for a successful SDB setup, such as Oracle Shard Director(s), Shard Catalog(s), Shard(s) and optionally Shard Standby(s) as well as Catalog standby using Data Guard for replication to provide high-availability and/or disaster recovery of the sharded data.

### Details

For more details, please refer to the [SDB-Terraform-OCI][SDB-terraform-oci] documentation.

## Kubernetes based deployment of Oracle sharded database 

### Overview 

Provides [Oracle Sharded Database][SDB] (SDB) Terraform modules and scripts that provisions the necessary components and resources required for a quick and easy setup of Oracle Sharded Database on-prem. It creates and configures SDB infrastructure components necessary for a successful SDB setup, such as Oracle Shard Director(s), Shard Catalog(s), Shard(s) and optionally Shard Standby(s) as well as Catalog standby using Data Guard for replication to provide high-availability and/or disaster recovery of the sharded data.

### Details

For more details, please refer to the [SDB-Terraform-OnPrem][SDB-terraform-onprem] documentation.

## Mid-tier routing for use in Oracle sharded database client applications

### Overview

Please refer to the Sharded database [Mid-Tier Routing introduction][MTR-Intro] for an overview.

### Details

For more details, please refer to the [SDB Mid-Tier routing][SDB-Mid-Tier-Routing] documentation. 


## Routing implementations for use in Oracle sharding client applications 

### Overview

The library consist of several parts:
 * Generic routing table interfaces
 * Sharding metadata implementation
 * Metadata reader (encapsulated select queries)
 * Tools for efficient parallel splitting with respect to sharding key

### Details

For more details, please refer to the [Routing implementations][SDB-Mid-Tier-Routing] documentation.
