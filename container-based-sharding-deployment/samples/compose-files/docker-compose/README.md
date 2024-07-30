# Create Containers using docker compose

Below steps provide an example to use `docker-compose` to create the docker network and to create the containers for a Sharded Database Deployment on a single Oracle Linux 7 host using Docker Containers.

**IMPORTANT:** This example uses 21c RDBMS and 21c GSM Docker Images.

- [Step 1: Install Docker Compose](#install-docker-compose)
- [Step 2: Complete the prerequisite steps](#complete-the-prerequisite-steps)
- [Step 3: Create Docker Compose file](#create-docker-compose-file)
- [Step 4: Create services using "docker compose" command](#create-services-using-docker-compose-command)
- [Step 5: Check the logs](#check-the-logs)
- [Step 6: Remove the deployment](#remove-the-deployment)
- [Copyright](#copyright)


## Install Docker Compose
```bash
DOCKER_CONFIG=${DOCKER_CONFIG:-$HOME/.docker}
mkdir -p $DOCKER_CONFIG/cli-plugins
ls -lrt $DOCKER_CONFIG/cli-plugins
curl -SL https://github.com/docker/compose/releases/download/v2.23.1/docker-compose-linux-x86_64 -o $DOCKER_CONFIG/cli-plugins/docker-compose
ls -lrt $DOCKER_CONFIG/cli-plugins
chmod +x $DOCKER_CONFIG/cli-plugins/docker-compose
```

## Complete the prerequisite steps

Use the file [docker-compose-prerequisites.sh](./docker-compose-prerequisites.sh) to export the environment variables, create the network host file and create required directories before running next steps:

**NOTE:** You will need to change the values for `SIDB_IMAGE` and `GSM_IMAGE` to use the images you want to use for the deployment.

```bash
source docker-compose-env-variables
```

Use below steps to create an encrypted password file:

```bash
rm -rf /opt/.secrets/
mkdir /opt/.secrets/
openssl genrsa -out /opt/.secrets/key.pem
openssl rsa -in /opt/.secrets/key.pem -out /opt/.secrets/key.pub -pubout

# Edit the file /opt/.secrets/pwdfile.txt to add the password string
vi /opt/.secrets/pwdfile.txt

# Encrypt the file having the password
openssl pkeyutl -in /opt/.secrets/pwdfile.txt -out /opt/.secrets/pwdfile.enc -pubin -inkey /opt/.secrets/key.pub -encrypt

rm -f /opt/.secrets/pwdfile.txt
chown 54321:54321 /opt/.secrets/pwdfile.enc
chown 54321:54321 /opt/.secrets/key.pem
chown 54321:54321 /opt/.secrets/key.pub
chmod 400 /opt/.secrets/pwdfile.enc
chmod 400 /opt/.secrets/key.pem
chmod 400 /opt/.secrets/key.pub
```

## Create Docker Compose file 

In this step, copy a Docker Compose file named [docker-compose.yaml](./docker-compose.yml) in your working directory.


## Create services using "docker compose" command
Once you have completed the prerequisties, run below commands to create the services:
```bash
# Switch to location with the `docker-compose.yaml` file and run:
 
docker compose up -d
``` 

## Check the logs

```bash
# You can monitor the logs for all the containers using below command:
 
docker compose logs -f
```

Wait for all setup to be ready:
```
# docker compose up -d
[+] Running 6/6
 ✔ Container pcatalog  Healthy                                                                                                                                                                                                                                           0.0s
 ✔ Container shard1    Healthy                                                                                                                                                                                                                                           0.0s
 ✔ Container shard2    Healthy                                                                                                                                                                                                                                           0.0s
 ✔ Container shard3    Healthy                                                                                                                                                                                                                                           0.0s
 ✔ Container gsm1      Healthy                                                                                                                                                                                                                                           0.0s
 ✔ Container gsm2      Started       
```

## Remove the deployment

You can also use the `docker-compose` command to remove the deployment. To remove the deployment:

- First export all the variables from the Prerequisites Sesion.
- Use the below command to remove the deployment:

```bash
docker-compose down
```

## Copyright

Copyright (c) 2022, 2023 Oracle and/or its affiliates.
Released under the Universal Permissive License v1.0 as shown at https://oss.oracle.com/licenses/upl/