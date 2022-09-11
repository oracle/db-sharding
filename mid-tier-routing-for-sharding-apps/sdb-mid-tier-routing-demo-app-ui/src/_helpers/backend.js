/*
** Oracle Sharding Tools Library
**
** Copyright © 2019 Oracle and/or its affiliates. All rights reserved.
** Licensed under the Universal Permissive License v 1.0 as shown at 
**   http://oss.oracle.com/licenses/upl 
*/

export function configureBackend() {
    let users = [{ id: 1, username: '', password: '', firstName: 'Customer ', lastName: 'Customer', 
    shardKey : [{
          "shardKeyType":"number",
          "shardKeyValue":"",
          "isSuperShardKey": false
        },
        {
          "shardKeyType":"varchar2",
          "shardKeyValue":"individual",
          "isSuperShardKey": true
        }
    ]},
    { id: 2, username: '', password: '', firstName: 'Customer ', lastName: 'Customer',
    shardKey : [{
        "shardKeyType":"number",
        "shardKeyValue":"",
        "isSuperShardKey": false
      },
      {
        "shardKeyType":"varchar2",
        "shardKeyValue":"business",
        "isSuperShardKey": true
      }
    ]}];
    let realFetch = window.fetch;
    window.fetch = function (url, opts) {
        return new Promise((resolve, reject) => {
            // authenticate
            if (url.endsWith('/users/authenticate') && opts.method === 'POST') {
                // get parameters from post request
                let params = JSON.parse(opts.body);
                var user;

                if(params.custType === 'individual'){
                    user = users[0];
                }else{
                  user = users[1];
                }

                user.shardKey[0].shardKeyValue = params.username;

                    let responseJson = {
                        username: params.username,
                        firstName: user.firstName,
                        lastName: user.lastName,
                        shardKey: user.shardKey
                    };
                    resolve({ ok: true, text: () => Promise.resolve(JSON.stringify(responseJson)) });

                return;
            }

            // pass through any requests not handled above
            realFetch(url, opts).then(response => resolve(response));
         });
    }
}