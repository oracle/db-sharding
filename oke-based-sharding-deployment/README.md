# Oracle Sharding on OKE
Oracle Sharding is a feature of Oracle Database that lets you distribute and replicate data across a pool of Oracle databases that share no hardware or software. Benefits of sharding include extreme scalability, fault isolation, and geographical distribution of data.

These deployment procedures automate the provisioning of Oracle Sharded Databases on Oracle Kubernetes Engine (OKE) using Oracle Cloud Infrastructure Ansible Modules and Helm/Chart.

# Deployment Overview
To deploy Oracle Sharding on OKE, Oracle Cloud Infrastructure Ansible Modules create compute resources, configure the network, create block storage volumes. It does so by using yaml files passed to ansible playbooks. 

The deployment is divided into the following tasks:

* Task1 - Setting up Ansible control machine
* Task2 - Deploy OKE Cluster on OCI
* Task3 - Creating GSM and Oracle Database Images
* Task4 - Oracle Sharding Deployment using Helm/Charts

# Deployment Steps
You need to execute the following steps in a given order to deploy Oracle Sharding on OKE.
## Task1 - Setting up Ansible control machine  
This section provides steps to setup the Ansible machine to run Oracle Sharding ansible playbooks on OKE. It has following steps:

* Configuration of prerequisites
* Installation and configuration of pip (Python Manager)
* Install Ansible Engine and OCI specific Ansible modules
* Setup OCI configuration file for Ansible for your tenancy

### Prerequisites
The Python SDK requires:
* Python version 2.7.5 or 3.5 or later installed on Ansible machine
* OpenSSL version 1.0.1 or later.
* An Oracle Cloud Infrastructure account
* A user created in that account, in a group with a policy that grants the desired permissions. For an example of how to set up a new user, group, compartment, and policy see [Adding Users](https://docs.us-phoenix-1.oraclecloud.com/Content/GSG/Tasks/addingusers.htm) in the Getting Started Guide.
* A keypair used for signing API requests, with the public key uploaded to Oracle. Only the user calling the API should be in possession of the private key. You can execute the following steps to upload the public key to OCI and make a copy of finger print which will be used later in the playbook. 
  * Create a .oci directory to store the credentials
  ```
  mkdir ~/.oci
  ```
  * Generate the private key with one of the following commands
  ```
  openssl genrsa -out ~/.oci/oci_api_key.pem 2048
  ```
  * Ensure that only you can read the private key file
  ```
  chmod go-rwx ~/.oci/oci_api_key.pem
  ```
  * Generate the public key:
  ```
  openssl rsa -pubout -in ~/.oci/oci_api_key.pem -out ~/.oci/oci_api_key_public.pem
  ```
  * Your API requests will be signed with your private key, and Oracle will use the public key to verify the authenticity of the request. You must upload the public key to IAM. Copy the contents of the public key to the clipboard and you'll need to paste the value into the Console. Refer to How to Upload the Public Key section in [Oracle Cloud Infrastructure Documentation](https://docs.cloud.oracle.com/iaas/Content/API/Concepts/apisigningkey.htm).

**Note**: You can find all the pre-requisites details for OCI ansible machine on [oracle-cloud-infrastructure documentation] (https://oracle-cloud-infrastructure-python-sdk.readthedocs.io/en/latest/installation.html#downloading-and-installing-the-sdk).

### Install and configure pip
You need to install and configure pip on ansible machine. Execute the following steps if the pip is not installed. This step can be executed a non-root user.
```
curl https://bootstrap.pypa.io/get-pip.py -o get-pip.py
python get-pip.py --user
```

### Install Ansible and OCI modules
* Oracle recommends that you run the SDK in a virtual environment with virtualenv. This allows you to isolate the dependencies for the SDK and avoids any potential conflicts with other Python packages that may already be installed (e.g. in your system-wide Python). Change your directory to the location where you want to keep Oracle Sharding on OKE files and we will refer the base location as OCI_ANSIBLE_DIR.

```
pip install virtualenv 
virtualenv oracle-sharding-si-k8s
source oracle-sharding-si-k8s/bin/activate
cd $OCI_ANSIBLE_DIR/oracle-sharding-si-k8s
```

* Install Ansible and OCI module using  pip
```
pip install ansible
pip install oci
pip install paramiko
``` 

* Clone OCI repository from GitHub and run install.py.
```
git clone https://github.com/oracle/oci-ansible-modules.git
cd $OCI_ANSIBLE_DIR/oracle-sharding-si-k8s/oci-ansible-modules
./install.py
cd $OCI_ANSIBLE_DIR/oracle-sharding-si-k8s
```

**Note**: You can find the installation and configuration steps for OCI ansible machine on [Downloading and Installing the SDK documentation] (https://oracle-cloud-infrastructure-python-sdk.readthedocs.io/en/latest/installation.html#downloading-and-installing-the-sdk).

### Setup OCI Configuration File for Ansible
* The default configuration file name and location is ```~/.oci/config```. You need to populate the config file with the following parameters:
```
[DEFAULT]
user=<OCID of the user calling the API. To get the value, [see Required Keys and OCIDs](https://docs.cloud.oracle.com/iaas/Content/API/Concepts/apisigningkey.htm). E.g. ```ocid1.user.oc1..aaaaaaaa65vwl75tewwm32rgqvm6i34unq```>
fingerprint=<Fingerprint for the key pair being used. To get the value, [see Required Keys and OCIDs](https://docs.cloud.oracle.com/iaas/Content/API/Concepts/apisigningkey.htm). Eg. ```20:3b:97:13:55:1c:5b:0d:d3:37:d8:50:4e:c5:3a:34```. This is same fingerprint you have copied uder Prerequisites section>
key_file=<Full path and filename of the private key. This is the same private key you have created under Prerequisites section and uploaded the public key to OCI cloud.>
tenancy=<OCID of your tenancy. To get the value, [see Required Keys and OCIDs] (https://docs.cloud.oracle.com/iaas/Content/API/Concepts/apisigningkey.htm). E.g. ```ocid1.tenancy.oc1..aaaaaaaaba3pv6wuzr4h25vqstifsfdsq```>
region=<An Oracle Cloud Infrastructure region. [See Regions and Availability Domains](https://docs.cloud.oracle.com/iaas/Content/General/Concepts/regions.htm). E.g. ```us-ashburn-1```>
```
**Notes**: You can refer the [SDK and CLI Configuration File documentation](https://docs.cloud.oracle.com/iaas/Content/API/Concepts/sdkconfig.htm) for the details.

## Task2 - Oracle OKE deployment on OCI using ansible playbook
To setup Oracle Sharding on OKE, you need to execute following steps: 

* Prerequisites
* Understand Oracle Sharding on Docker deployment on OCI Ansible playbook
* Execute Ansible playbook

### Prerequisites
* Clone the oracle-sharding-si-k8s playbook on ansible controller machine
```
cd $OCI_ANSIBLE_DIR/oracle-sharding-si-k8s
git clone https://orahub.oraclecorp.com/paramdeep_saini/oracle-sharding-si-k8s.git
cd oracle-sharding-si-k8s
```
* Create a public and private key for compute instance
```
ssh-keygen -t rsa -N "" -b 2048 -C "oracle-sharding-si-k8s_keys"
```
* Create ```oci_export_vars.sh``` executable file.
You need to create ```oci_export_vars.sh``` under ```$OCI_ANSIBLE_DIR/oracle-sharding-si-k8s/``` and set following parameters:

* export SAMPLE_COMPARTMENT_OCID=<Copy OCID compartment ID from OCI console.> To get OCID for
* export SAMPLE_IMAGE_OCID=<You need OCI linux image for OEL 7.x>. E.g. You can use ocid1.image.oc1.phx.aaaaaaaacss7qgb6vhojblgcklnmcbchhei6wgqisqmdciu3l4spmroipghq
* export SAMPLE_AD_NAME=<Provide your AD name>. E.g. JnxQ:PHX-AD-1
* export ANSIBLE_HOST_KEY_CHECKING=False  # Do not change it.

### Understand OKE deployment for Oracle Sharding on OCI using Ansible playbook 
Before executing the playbook, it is important to understand the ansible playbook. This playbook executes following operations

* Create and configure VCN
* Launch OKE cluster. OKE cluster contains 3 worker node
* Open network ports for ssh and your services
* 

### Execute Ansible Playbook
Oracle Kubernetes cluster deployment using Ansible playbook provides an easy way to provision the Kubernetes cluster on OCI.

**Note:** Before proceeding to the next section, you need to meet all the pre-requisites.
#### Getting Started
 * Change directory to oracle-sharding-si-ansible-playbooks
 * Modify parameter file for Oracle Kubernetes Cluster deployment on OCI. You need to refer section `Parameters - OKE Deployment on OCI`

   **Note:** You need to make sure parameters are changed based on your environment. You can also use default values where it is specified.
 * Execute ansible playbook
   `ansible-playbook oci_launch_oke_cluster.yaml`

### Parameters - OKE Deployment on OCI

Edit the file samples/oci-oke-setup-env.yaml and make changes based on your requirement. If it is a new setup, you can keep the default values except following and change them based on your requirement:

* oke_cluster_ssh_public_key: Specify the public SSH key to login to worker nodes. You need to make sure you have a private key available on your ansible control machine to login to worker nodes. You need to provide public ssh key under double quotes. Default set to "<PUBLIC_SSH_KEY>".

### Customize the setup by modifying the parameters in each section based on the user environment.

* quad_zero_route: Default set to "0.0.0.0/0".
* TCP_protocol: Default set to "6". Do not change this value.
* SSH_port: Default set to "22". Do not change this value.
* vcn_name: Specify the VCN name. Default set to "okevcn". 
* vcn_cidr_block: Specify VCN CIDR name "10.0.0.0/16".
* vcn_dns_label: Specify the vcn dns label. Default set to "okevcn".
* ig_name: Set the internetgateway. Default set to "internetgatewayformyokevcn".
* route_table_name: set the route table name. Default set to "okeroutetable".
* route_table_rules: Set the route table rules. Default set to following. It is in yaml form so make sure you follow the exact spacing as shown in sample file.
  *   "- cidr_block: "{{ quad_zero_route }}"
  *  network_entity_id: "{{ ig_id }}"
* subnet_cidr: Specify the CIDR. Default set to "10.0.0.48/28".
* subnet_dns_label: Specify the DNS label name. Default set to "okesubnet".
* subnet1_cidr: Specify the subnet1 CIDR. Default set to "10.0.0.0/24".
* subnet2_cidr: Specify the subnet2 CIDR. Default set to "10.0.1.0/24".
* subnet3_cidr: Specify the subnet3 CIDR. Default set to "10.0.2.0/24".
* subnet1_name: Specify the subnet1 name. Default set to "okesubnet1".
* subnet2_name: Specify the subnet2 name. Default set to "okesubnet2".
* subnet3_name: Specify the subnet3 name. Default set to "okesubnet3".
* lb_subnet1_cidr: Specify the load balance subnet1 cidr. Default set to "10.0.20.0/24".
* lb_subnet2_cidr: Specify the load balance subnet2 cidr. Default set to "10.0.21.0/24".
* lb_subnet1_name: Specify the load balance subnet1 name. Default set to "LB Subnet1".
* lb_subnet2_name: Specify the load balance subnet1 name. Default set to "LB Subnet2".
* cluster_name: Specify the OKE cluster name. Default set to "oke_cluster".
* oracle_gsm_pool_name: Specify the gsm node pool name to contain the shard directors. Default set to "oracle_gsm_node_pool".
* oracle_shards_pool_name: Specify the shard node pool name to contain the shard directors. Default set to "oracle_shards_node_pool".
* node_image_name: Specify the Linux image for worker node. Default set to "Oracle-Linux-7.4".
* node_shape: Specify the Linux Vm Shape for worker node. Default set to "VM.Standard2.4".
* OKE_ip_address1: Specify the OKE IP address range. Default set to "130.35.0.0/16".
* OKE_ip_address2: Specify the OKE IP address range. Default set to "134.70.0.0/17".
* OKE_ip_address3: Specify the OKE IP address range. Default set to "138.1.0.0/16".
* OKE_ip_address4: Specify the OKE IP address range. Default set to "140.91.0.0/17".
* OKE_ip_address5: Specify the OKE IP address range. Default set to "147.154.0.0/16".
* OKE_ip_address6: Specify the OKE IP address range. Default set to "192.29.0.0/16".
* ad1: Specify the ad1 detail. This is read from ENV variable, you need to make sure it is exported at environment level. Default set to "{{ lookup('env', 'SAMPLE_AD1_NAME') }}".
* ad2: Specify the ad2 detail. This is read from ENV variable, you need to make sure it is exported at environment level. Default set to "{{ lookup('env', 'SAMPLE_AD2_NAME') }}"
* ad3: Specify the ad3 detail. This is read from ENV variable, you need to make sure it is exported at environment level. Default set to "{{ lookup('env', 'SAMPLE_AD3_NAME') }}"
* cluster_compartment: Specify the compartment detail. This is read from ENV variable, you need to make sure it is exported at environment level. Default set to "{{ lookup('env', 'SAMPLE_COMPARTMENT_OCID') }}".
* kubeconfig_path:Specify the KUBE_CONFIG PATH location. This is read from ENV variable, you need to make sure it is exported at environment level. Default set to  "{{ lookup('env', 'KUBECONFIG_PATH') }}".
* oke_cluster_ssh_public_key: Specify the public SSH key to login to worker nodes. You need to make sure you have private key available on your ansible control machine to login to worker nodes. You need to provide publich ssh key under double quoates. Default set to "<PUBLIC_SSH_KEY>".

## Task3 - Creating GSM and Oracle Database Images
This steps is optional and you need to execute this when you do not have access to Oracle GSM software and database image.
* To create and setup Oracle GSM and Database Image, please follow [Oracle Sharding on Docker](https://github.com/oracle/db-sharding/tree/master/docker-based-sharding-deployment)

## Task4 - Oracle Sharding Deployment using Helm/Charts 

To setup this, please follow [Oracle Sharding on OKE](./oracle-sharding-si-chart/README.md)

## FAQ
You can refer to following Faqs before filing the ticket.
