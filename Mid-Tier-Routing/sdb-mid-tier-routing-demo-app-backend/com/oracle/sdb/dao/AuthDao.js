/*
** Oracle Sharding Tools Library
**
** Copyright © 2019 Oracle and/or its affiliates. All rights reserved.
** Licensed under the Universal Permissive License v 1.0 as shown at 
**   http://oss.oracle.com/licenses/upl 
*/

/**
 * Perform authentication for a customer 
 */

const oracledb = require('oracledb');
var config = require('../config/Configuration');

function performAuth(username, password, custType) {
  return new Promise(async function(resolve, reject) {
    let conn;

    try {
      conn = await oracledb.getConnection(config.shardConnectionParams);

      let result = await conn.execute(
        `SELECT cust_name
        FROM customer
        WHERE cust_id = :username AND cust_passwd = :password AND cust_type = :custType`  ,
        [username, password, custType], 
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

exports.performAuth = performAuth;