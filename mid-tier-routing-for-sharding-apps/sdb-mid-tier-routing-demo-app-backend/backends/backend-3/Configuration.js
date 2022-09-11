/*
** Oracle Sharding Tools Library
**
** Copyright © 2019 Oracle and/or its affiliates. All rights reserved.
** Licensed under the Universal Permissive License v 1.0 as shown at 
**   http://oss.oracle.com/licenses/upl 
*/


/* Defines the configuration for Invoice backend REST API registry server */

var fs  = require('fs');
var https = require('https');

var proxyOptions = { 
    xfwd: false,
    secure: false, 
    rejectUnauthorized: false, 
    strictSSL: false, 
    prependPath: false
  };

var getInvoiceDetails = {
    hostname: 'localhost',
    port: 30000,
    path: '/api/invoice',
    method: 'GET'
  };

var shardConnectionParams = {
    user          : "app_schema",
    password      : "",
    connectString : "host:port/service"
  };

var registryPort = process.env.PORT || 30000; // port which the registry server will listen on.

exports.getInvoiceDetails = getInvoiceDetails;
exports.proxyOptions = proxyOptions;
exports.registryPort = registryPort;
exports.shardConnectionParams = shardConnectionParams;  
