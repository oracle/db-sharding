/*
** Oracle Sharding Tools Library
**
** Copyright © 2019 Oracle and/or its affiliates. All rights reserved.
** Licensed under the Universal Permissive License v 1.0 as shown at 
**   http://oss.oracle.com/licenses/upl 
*/

import config from 'config';

export const userService = {
    logout,
    userLogin,
    authzSwimLane,
    getUserInvoiceData
};

async function authzSwimLane(shardName){
    console.log('during swim lane call');
    console.log(shardName);

    const authzOptions = await {
        headers: { 'Content-Type': 'application/vnd.oracle.sdb.mtr.sk.datatype.mixed.swimlane.v1+json' }
    };

    let authzShardKeyResponse = await fetch(`${config.apiUrl}/sdb-mid-tier-routing-services/swimLane/${shardName}`);
    let data = await handleResponse(authzShardKeyResponse);
    return data;
}

async function getSwimLane(shardKey){
    console.log('during getSwimLane call');
    console.log(shardKey);

    const authzOptions = await {
        headers: { 'Content-Type': 'application/vnd.oracle.sdb.mtr.sk.datatype.mixed.swimlane.v1+json' }
    };

    let authzShardKeyResponse = await fetch(`${config.apiUrl}/sdb-mid-tier-routing-services/shardDetails/swimLane`);
    let data = await handleResponse(authzShardKeyResponse);
    return data;
}

async function getUserInvoiceData(swimLaneInfo, username){
    console.log('during user Invoice call');
    console.log(swimLaneInfo);
    
    let response = await fetch(swimLaneInfo.swimLaneURL + '/invoice?custId='+ username);
    let data = await handleResponse(response);
    return data;
}

async function userLogin(username, password, custType) {
    
    const authnOptions = await {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username, password, custType})
    };

    console.log(authnOptions.body);

    let authResponse = await fetch(`${config.apiUrl}/users/authenticate`, authnOptions);
    let user = await handleResponse(authResponse);

    if (user) {
     var authzOptions = await {
            method: 'POST',
            headers: { 'Content-Type': 'application/vnd.oracle.sdb.mtr.sk.datatype.mixed.swimlane.v1+json' },
            body: JSON.stringify(user.shardKey)
     };
     let authzShardKeyResponse  = await fetch(`${config.apiUrl}/sdb-mid-tier-routing-services/shardDetails/swimLane`, authzOptions);
     let data = await handleResponse(authzShardKeyResponse);  

     console.log('in the response of shardKey api');
     console.log(data);
     console.log('shardName = ' + data[0]);

    //add swimLaneInfo to user obj
    user.swimLaneInfo = data[0];

     let authnResp = await fetch(user.swimLaneInfo.swimLaneURL + '/auth', authnOptions);
     let authnResponse = await handleResponse(authnResp);
 
     console.log('authnResponse = ');
     console.dir(authnResponse);

     // login successful if there's a customer name in the response
     user.firstName = authnResponse[0].CUST_NAME;

     // store user details and basic auth credentials in local storage 
     // to keep user logged in between page refreshes.
     user.authdata = await window.btoa(username + ':' + password);

     console.log(user);
     await localStorage.setItem('user', JSON.stringify(user)); 
    }    

    return user;
  }

    function logout() {
        // remove user from local storage to log user out
        localStorage.removeItem('user');
    }

    function handleResponse(response) {
        return response.text().then(text => {
            const data = text && JSON.parse(text);

            console.log('error msg from auth backend');
            console.log(data);

            if (!response.ok) {
                if (response.status === 401) {
                    // auto logout if 401 response returned from api
                    logout();
                    location.reload(true);
                }

                console.log(response.statusText);

                var error = (data && data.error) || response.statusText;
                if(error === undefined || error === ''){
                    error = "Oh snap !.. Invoice app is currently unavailable. Please try again after sometime.";
                }
                return Promise.reject(error);
            }

            return data;
        });
    }