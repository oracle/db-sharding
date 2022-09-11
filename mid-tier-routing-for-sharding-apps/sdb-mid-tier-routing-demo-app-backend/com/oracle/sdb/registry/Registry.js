/*
** Oracle Sharding Tools Library
**
** Copyright © 2019 Oracle and/or its affiliates. All rights reserved.
** Licensed under the Universal Permissive License v 1.0 as shown at 
**   http://oss.oracle.com/licenses/upl 
*/


/** 
 * Defines the Invoice backend REST API registry server 
 */ 

var express = require('express');
var bodyParser = require('body-parser');
var morgan     = require('morgan');
var fs  = require('fs');
var config = require('../config/Configuration');
var cors = require('cors');
var invoiceDao = require('../dao/InvoiceDao');
var authDao = require('../dao/AuthDao')
 
module.exports = function(registry) {

var router = express.Router();              // get an instance of the express Router

registry.use(morgan('dev')); // log requests to the console
registry.use(bodyParser.urlencoded({ extended: true })); // configure app to use bodyParser()
registry.use(bodyParser.json()); // this will let us get the data from a POST

registry.use(cors());
// Register our routes -------------------------------
registry.use('/api', router);

// middleware to use for all requests
router.use(function(req, res, next) {
    next(); // make sure we go to the next routes and don't stop here
});

router.get('/', function(req, res) {
    res.json({ message: 'You have hit the index route. Try better routes' });   
});

// routes that end in /invoice
// ----------------------------------------------------
router.route('/invoice')
    .get( async function(req, res) {
        
        try {
            let invoiceResponse = await invoiceDao.getInvoiceDetails(req.query.custId);
            console.log('invoice response from shard: ');
            console.dir(invoiceResponse);
            res.json(invoiceResponse);
          } catch (err) {
            console.log('There has been an error parsing the invoice data.');
            console.error(err);
            res.send(err);
          }  
    });

router.route('/auth')    
    .post(async function(req, res) {
        console.dir(req.body);

        try {
            let authResp = await authDao.performAuth(req.body.username, req.body.password, req.body.custType);
            console.log('auth response from shard: ');
            console.dir(authResp);
            if(authResp.length > 0){
                res.status(200).json(authResp);
            }else{
                res.status(400).send({error : "Incorrect credentials. Please try again."});
            }  
          } catch (err) {
            console.log('There has been an error parsing the invoice data.');
            console.error(err);
            res.send(err);
          }                  
   });
return registry;
} //end of Invoice registry module  