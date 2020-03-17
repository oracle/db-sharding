## Sharded database Mid-Tier routing - Demo app backend 

### Pre-Requisites

- Download and install node js and npm if you don't already have it installed on the target machine.

### Installation & Configuration

1.  Install dependencies: `npm install` at the root of the project to pull and install all the dependencies. If the download fails make sure to check your http_proxy settings as your target machine might be behind a firewall.

2. To run different multiple instances of backends to simulate multiple app backends (swim lanes),  clone this repository 3 more times and replace the **com.oracle.sdb.config.Configuration.js** with **backends.backend-2.Configuration.js**,  **backends.backend-3.Configuration.js** and **backends.backend-4.Configuration.js**

Note :  Configuration of the backend is available at com.oracle.sdb.config.Configuration  (If you need to make config changes and you have already started the SDB mid-tier app backend server, then don't forget to restart the node server)

3. Start the SDB MTR Invoice Demo app backend server: `npm start`
4. To restart the SDB MTR Invoice Demo app backend server : `npm restart`

### Using the SDB mid-tier-routing demo backend app 

**Backend app REST APIs** 

-  _API Collection_ : https://documenter.getpostman.com/view/754319/RztsnRCC

**Fetch invoice for a specific customer**   

-  `https://backendHost:backendPort/api/invoice?custId=X`  

**Description** : Retrieves all the invoices from different vendors for a specific customer. Each customer (for example a company that needs to host all its vendor data including invoices in a specific shard) is on a separate app backend hosted at backendHost:backendPort. 

**Backend API CURLs for various backends** 

      curl --location --request GET "http://localhost:10000/api/invoice?custId=999" --data ""
      
      curl --location --request GET "http://localhost:20000/api/invoice?custId=250251" --data ""

      curl --location --request GET "http://localhost:30000/api/invoice?custId=550551" --data ""

      curl --location --request GET "http://localhost:40000/api/invoice?custId=650651" --data ""
