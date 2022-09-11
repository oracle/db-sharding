/*
** Oracle Sharding Tools Library
**
** Copyright © 2019 Oracle and/or its affiliates. All rights reserved.
** Licensed under the Universal Permissive License v 1.0 as shown at 
**   http://oss.oracle.com/licenses/upl 
*/


/**
 * sdb-mtr-demo-app-backend
 */

var express = require('express');        
var registry = require('./com/oracle/sdb/registry/Registry');
var config = require('./com/oracle/sdb/config/Configuration');
var https  = require('https');

// Setup and Start the Invoice backend REST API Server 
var invoiceRegistry = express(); // define our Invoice backend REST API Server using express
invoiceRegistry = registry(invoiceRegistry);
invoiceRegistry.listen(config.registryPort); // Start the Invoice Registry REST API Server
console.log("SDB Mid-Tier Demo Backend app is up and running at port : " + config.registryPort);
