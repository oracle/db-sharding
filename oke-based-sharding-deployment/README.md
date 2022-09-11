# Oracle Sharding on OKE
Oracle Sharding is a feature of Oracle Database that lets you distribute and replicate data across a pool of Oracle databases that share no hardware or software. Benefits of sharding include extreme scalability, fault isolation, and geographical distribution of data.

Oracle Cloud Infrastructure Container Engine for Kubernetes is a fully-managed, scalable, and highly available service that you can use to deploy your containerized applications to the cloud. Use Container Engine for Kubernetes (sometimes abbreviated to just OKE) when your development team wants to reliably build, deploy, and manage cloud-native applications. You specify the compute resources that your applications require, and Container Engine for Kubernetes provisions them on Oracle Cloud Infrastructure in an existing OCI tenancy.

Helm is the package manager (analogous to yum and apt) and Charts are packages (analogous to debs and rpms). The home for these Charts is the Kubernetes Charts repository which provides continuous integration for pull requests, as well as automated releases of Charts in the master branch. For details, refer [Helm Charts: making it simple to package and deploy common applications on Kubernetes](https://kubernetes.io/blog/2016/10/helm-charts-making-it-simple-to-package-and-deploy-apps-on-kubernetes/).

To deploy Oracle Sharding on OKE using Helm and Chart, execute following steps in a given order:
* [Create Container Engine for Kubernetes](https://docs.oracle.com/en-us/iaas/Content/ContEng/Concepts/contengoverview.htm)
   * You must have 1 node in each AD to distribute the shards across ADs
   * If you want to automate OKE deployment, you can refer following:
     * [Ansible Collection](https://docs.oracle.com/en-us/iaas/Content/API/SDKDocs/ansible.htm)
     * [Terraform Provider](https://docs.oracle.com/en-us/iaas/Content/API/SDKDocs/terraform.htm)
* [Deploy Oracle Sharding using Helm and Chart](./oracle-sharding-si-chart/README.md)
