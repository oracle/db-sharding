/*
** Oracle Sharding Tools Library
**
** Copyright © 2019 Oracle and/or its affiliates. All rights reserved.
** Licensed under the Universal Permissive License v 1.0 as shown at 
**   http://oss.oracle.com/licenses/upl 
*/


/**
 * Fetch invoice details for a customer
 */

const oracledb = require('oracledb');
var config = require('../config/Configuration');

function getInvoiceDetails(customerId) {
  return new Promise(async function(resolve, reject) {
    let conn;

    try {
      conn = await oracledb.getConnection(config.shardConnectionParams);

      let result = await conn.execute(
        `SELECT invoice_id, vendor_name, balance, total, status
        FROM invoice
        WHERE cust_id = :id`,
        [customerId], 
        { outFormat: oracledb.OBJECT } 
      );
      resolve(result.rows);

    } catch (err) { // catches errors in getConnection and the query
      reject(err);
    } finally {
      if (conn) {   // cleanup conn by releasing it
        try {
          await conn.release();
        } catch (e) {
          console.error(e);
        }
      }
    }
  });
}

exports.getInvoiceDetails = getInvoiceDetails;