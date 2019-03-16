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

var options = {
      key: fs.readFileSync('./certs/key.pem'),
      cert: fs.readFileSync('./certs/cert.pem')
  };

var proxyOptions = { 
    xfwd: false,
    secure: false, 
    rejectUnauthorized: false, 
    strictSSL: false, 
    prependPath: false
  };

var getInvoiceDetails = {
    hostname: 'localhost',
    port: 10000,
    path: '/api/invoice',
    method: 'GET'
  };

var shardConnectionParams = {
    user          : "app_schema",
    password      : "app_schema",
    connectString : "slc15zym:6216/sitemtrc.regress.rdbms.dev.us.oracle.com"
  };

var registryPort = process.env.PORT || 10000; // port which the registry server will listen on.

exports.getInvoiceDetails = getInvoiceDetails;
exports.options = options;
exports.proxyOptions = proxyOptions;
exports.registryPort = registryPort;
exports.shardConnectionParams = shardConnectionParams;  