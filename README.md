# Oracle Globally Distributed Database tools and deployment automation library

[SDB-terraform-onprem]: https://github.com/oracle/db-sharding/tree/master/deployment-with-terraform/sdb-terraform-onprem
[SDB-terraform-oci]: https://github.com/oracle/db-sharding/tree/master/deployment-with-terraform/sdb-terraform-oci
[SDB-terraform]: https://github.com/oracle/db-sharding/tree/master/deployment-with-terraform/
[SDB-Mid-Tier-Routing]: https://github.com/oracle/db-sharding/tree/master/mid-tier-routing-for-sharding-apps
[SDB-Fast-Data-Ingest]: https://github.com/oracle/db-sharding/tree/master/sharding-fast-data-ingest
[OKE-sharding]: https://github.com/oracle/db-sharding/tree/master/oke-based-sharding-deployment
[MTR-Intro]: https://github.com/oracle/db-sharding/wiki/Sharded-Database-Mid-Tier-Routing#introduction
[SDB-prod-page]: https://www.oracle.com/database/technologies/high-availability/sharding.html
[SDB-prod-doc]: https://docs.oracle.com/en/database/oracle/oracle-database/23/shard/
[OCI]: https://www.oracle.com/cloud/
[SDB]: https://www.oracle.com/database/technologies/high-availability/sharding.html

- [About](#about)
- [Terraform based deployment of Oracle Globally Distributed Database](#terraform-based-deployment-of-oracle-globally-distributed-database)
  * [Overview](#overview)
  * [Details](#details)
- [Mid-tier routing for use in Oracle Globally Distributed Database client applications](#mid-tier-routing-for-use-in-oracle-globally-distributed-database-client-applications)
  * [Overview](#overview-1)
  * [Details](#details-1)
- [OKE-based deployment of Oracle Globally Distributed Database](#oke-based-deployment-of-oracle-globally-distributed-database)
  * [Overview](#overview-2)
  * [Details](#details-2)
- [Container based deployment of Oracle Globally Distributed Database](#container-based-deployment-of-oracle-globally-distributed-database)
  * [Overview](#overview-3)
  * [Details](#details-3)
- [Sharding Fast Data Ingest](#routing-implementations-for-use-in-oracle-sharding-client-applications)
  * [Overview](#overview-4)
  * [Details](#details-4)

## About

This repository provides deployment automation and tools for Oracle Database using Oracle Globally Distributed Database. It includes the following:

* Terraform based deployment of Oracle Globally Distributed Database
* Mid-tier routing for use in Oracle Globally Distributed Database applications
* OKE based deployment of Oracle Globally Distributed Database
* Fast data ingest for sharding applications

To learn more about Oracle Globally Distributed Database (previously referred to as Oracle Sharded Database), review the [product page][SDB-prod-page] and [product documentation][SDB-prod-doc].

<strong> Note </strong> : Globally Distributed Database, Oracle Sharding, Sharded database, and Oracle Sharded Database (SDB) are interchangeably used names in the documentation and refer to the same entity.

 
## Terraform based deployment of Oracle Globally Distributed Database

### Overview 

The `SDB-terraform-oci` and `SDB-terraform-onprem` deployments provide Terraform modules, configuration and scripts that provision the necessary components and resources for a quick and easy setup of [Oracle Globally Distributed Database][SDB] (SDB) on either [Oracle Cloud Infrastructure][OCI] (OCI) or as an On-Premises (`onprem`) database. These deployments create and configures SDB infrastructure components that are necessary for a successful Globally Distributed Database setup, including Oracle Shard Directors, Shard Catalogs, Shards, and optionally, Shard Standbys. It also provides replication through a recovery catalog standby database using Oracle Data Guard. Oracle Data Guard provides both high availability (HA) and disaster recovery (DR) for data in the Globally Distributed Database.

### Details

For more details, see the [SDB-Terraform][SDB-terraform] documentation.

## Mid-tier routing for use in Oracle Globally Distributed Database client applications

### Overview

For an overview of the mid-tier Oracle Globally Distributed Database feature, see the Globally Distributed Database [Mid-Tier Routing introduction][MTR-Intro].

### Details

To learn more about the Oracle Globally Distributed Database features for mid-tier, see: [SDB Mid-Tier routing][SDB-Mid-Tier-Routing].

## OKE-based deployment of Oracle Globally Distributed Database 

### Overview 

The OKE deployment provides Oracle Kubernetes Engine (OKE)-based deployment of an Oracle Globally Distributed Database.

### Details

To learn more about the OKE sharding feature, see: [OKE sharding][OKE-sharding] documentation.

## Container-based deployment of Oracle Globally Distributed Database 

### Overview 

Oracle provides deployment tools for container-based deployment of Globally Distributed Database.

### Details

To learn more about container-based deployment of Oracle Globally Distributed Database, see: [Container sharding](./container-based-sharding-deployment/README.md).

## Fast data ingest

### Overview

The Fast data ingest library consists of several parts:
 * Generic routing table interfaces
 * Sharding metadata implementation
 * Metadata reader (encapsulated select queries)
 * Tools for efficient parallel splitting with respect to sharding key

### Details

To learn more about the Oracle Sharding Fast Data Ingest feature, see: [Fast data ingest][SDB-Fast-Data-Ingest].

### Contributing

This project welcomes contributions from the community. Before submitting a pull request, please [review our contribution guide](./CONTRIBUTING.md)

### Security

Please consult the [security guide](./SECURITY.md) for our responsible security vulnerability disclosure process

### License

Copyright (c) 2020, 2023 Oracle and/or its affiliates.

Released under the Universal Permissive License v1.0 as shown at
<https://oss.oracle.com/licenses/upl/>.
