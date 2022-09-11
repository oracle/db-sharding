# Oracle Sharding tools and deployment automation library

[SDB-terraform-onprem]: https://github.com/oracle/db-sharding/tree/master/deployment-with-terraform/sdb-terraform-onprem
[SDB-terraform-oci]: https://github.com/oracle/db-sharding/tree/master/deployment-with-terraform/sdb-terraform-oci
[SDB-terraform]: https://github.com/oracle/db-sharding/tree/master/deployment-with-terraform/
[SDB-Mid-Tier-Routing]: https://github.com/oracle/db-sharding/tree/master/Mid-Tier-Routing
[SDB-Fast-Data-Ingest]: https://github.com/oracle/db-sharding/tree/master/sharding-fast-data-ingest
[OKE-sharding]: https://github.com/oracle/db-sharding/tree/master/oke-based-sharding-deployment
[DOCKER-sharding]: https://github.com/oracle/db-sharding/tree/master/docker-based-sharding-deployment
[MTR-Intro]: https://github.com/oracle/db-sharding/wiki/Sharded-Database-Mid-Tier-Routing#introduction
[SDB-prod-page]: https://www.oracle.com/database/technologies/high-availability/sharding.html
[SDB-prod-doc]: https://docs.oracle.com/en/database/oracle/oracle-database/19/shard/
[OCI]: https://www.oracle.com/cloud/
[SDB]: https://www.oracle.com/database/technologies/high-availability/sharding.html

- [About](#about)
- [Terraform based deployment of Oracle sharded database](#terraform-based-deployment-of-oracle-sharded-database)
  * [Overview](#overview)
  * [Details](#details)
- [Mid-tier routing for use in Oracle sharded database client applications](#mid-tier-routing-for-use-in-oracle-sharded-database-client-applications)
  * [Overview](#overview-1)
  * [Details](#details-1)
- [OKE based deployment of Oracle sharded database](#oke-based-deployment-of-oracle-sharded-database)
  * [Overview](#overview-2)
  * [Details](#details-2)
- [Docker based deployment of Oracle sharded database](#docker-based-deployment-of-oracle-sharded-database)
  * [Overview](#overview-3)
  * [Details](#details-3)
- [Sharding Fast Data Ingest](#routing-implementations-for-use-in-oracle-sharding-client-applications)
  * [Overview](#overview-4)
  * [Details](#details-4)

## About

This repository provides Oracle Sharded database deployment automation and tools :

* Terraform based deployment of Oracle sharded database.
* Mid-tier routing for use in Oracle sharded database applications.
* OKE based deployment of Oracle sharded database.
* Fast data ingest for sharding applications.

If you want to learn more about Oracle sharding a.k.a Oracle Sharded Database, please refer to the [product page][SDB-prod-page] and [product documentation][SDB-prod-doc].

<strong> Note </strong> : Oracle Sharding and Oracle Sharded Database (SDB) are interchangeably used names in the documentation and refer to the same entity. 

 
## Terraform based deployment of Oracle sharded database

### Overview 

Provides Terraform modules, configuration and scripts that provisions the necessary components and resources for a quick and easy setup of [Oracle Sharded Database][SDB] (SDB) on either [Oracle Cloud Infrastructure][OCI] (OCI) or On-Premise. It creates and configures SDB infrastructure components necessary for a successful Sharded Database setup, such as Oracle Shard Director(s), Shard Catalog(s), Shard(s) and optionally Shard Standby(s) as well as Catalog standby using Data Guard for replication to provide high-availability and/or disaster recovery of the sharded data.

### Details

For more details, please refer to the [SDB-Terraform][SDB-terraform] documentation.

## Mid-tier routing for use in Oracle sharded database client applications

### Overview

Please refer to the Sharded database [Mid-Tier Routing introduction][MTR-Intro] for an overview.

### Details

For more details, please refer to the [SDB Mid-Tier routing][SDB-Mid-Tier-Routing] documentation.

## OKE based deployment of Oracle sharded database 

### Overview 

Oracle Kubernetes Engine (OKE) based deployment of sharded database.

### Details

For more details, please refer to the [OKE sharding][OKE-sharding] documentation.

## Docker based deployment of Oracle sharded database 

### Overview 

Docker based deployment of sharded database.

### Details

For more details, please refer to the [Docker sharding][DOCKER-sharding] documentation.

## Fast data ingest

### Overview

The Fast data ingest library consist of several parts:
 * Generic routing table interfaces
 * Sharding metadata implementation
 * Metadata reader (encapsulated select queries)
 * Tools for efficient parallel splitting with respect to sharding key

### Details

For more details, please refer to the [Fast data ingest][SDB-Fast-Data-Ingest] documentation.
