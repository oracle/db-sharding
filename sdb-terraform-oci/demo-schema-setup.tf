# Copyright 2020, Oracle Corporation and/or affiliates.  All rights reserved.
# Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl
resource "null_resource" "sdb_schema_setup" {
  depends_on = ["null_resource.sdb_add_service"]
  count = "${var.demo_setup=="false"?0:var.sharding_method==local.system_sharding?1:0}"

  #creates ssh connection
  connection {
    type = "ssh"
    user = "${var.os_user}"
    private_key = "${file(var.ssh_private_key_path)}"
    host = "${oci_database_db_system.catalog_db[0].hostname}.${oci_database_db_system.catalog_db[0].domain}"
    agent = false
    timeout = "${var.ssh_timeout}"
  }

  provisioner "file" {
    content  = "${data.template_file.system_sharding_schema_setup_template.rendered}"
    destination = "${local.db_home_path}/sharding-schema.sql"
  }

  provisioner "file" {
    content  = <<-EOF
      #! /bin/bash
      source ${local.db_home_path}/shardcat.sh
      sqlplus / as sysdba @${local.db_home_path}/sharding-schema.sql
      EOF
    destination = "${local.db_home_path}/sharding-schema-setup.sh"
  }

  #Catalog config
  provisioner "remote-exec" {
    inline = [
    "chmod 700 ${local.db_home_path}/sharding-schema-setup.sh",
    "${local.db_home_path}/sharding-schema-setup.sh"
    ]
  }


  provisioner "file" {
    when    = "destroy"
    content  = <<-EOF
      #! /bin/bash
      source ${local.db_home_path}/shardcat.sh
      rm -f ${local.db_home_path}/sharding-schema.sql
      EOF
    destination = "${local.db_home_path}/sharding-schema-teardown.sh"
  }

  # destroying
  provisioner "remote-exec" {
    when    = "destroy"
    inline = [
    "chmod 700 ${local.db_home_path}/sharding-schema-teardown.sh",
    "${local.db_home_path}/sharding-schema-teardown.sh",
    "rm -f ${local.db_home_path}/sharding-schema-teardown.sh",
    "rm -f ${local.db_home_path}/sharding-schema-setup.sh",
    "rm -f ${local.db_home_path}/create_app_schema.lst"
    ]
   }
}
