<!-- /*
** Oracle Sharding Tools Library
**
** Copyright © 2019 Oracle and/or its affiliates. All rights reserved.
** Licensed under the Universal Permissive License v 1.0 as shown at 
**   http://oss.oracle.com/licenses/upl 
*/ -->

## Sharded database Mid-Tier Routing Services

**Note**: If you want to extend and build the sdb-mid-tier-routing-services project after cloning/forking, then follow the steps below after importing the project:

1.  Download ojdbc8.jar, ons.jar and ucp.jar from Oracle download center or the follwing link : https://www.oracle.com/technetwork/database/features/jdbc/jdbc-ucp-122-3110062.html
2.  Create a libs folder at the root of the sdb-mid-tier-routing-services project and copy the downloaded jars into it.
3.  Use mvn clean install

### To build a docker image : 

Note : Set the docker repository variables in pom.xml to your docker repository 

clean package dockerfile:build -X -e -DskipDockerPush

### To build and push a docker image into your repo : 

Note : Set the docker repository variables in pom.xml to your docker repository 

clean package dockerfile:build dockerfile:push -X -e
