#!/bin/bash
#
# LICENSE UPL 1.0
# Since: November, 2020
# Author: paramdeep.saini@oracle.com
# Description: Build script for building RAC container image
#
# DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
#
# Copyright (c) 2020,2021 Oracle and/or its affiliates.
#

if grep -q "Oracle Linux Server release 9" /etc/oracle-release; then \
        curl --noproxy '*' https://ca-artifacts.oraclecorp.com/auto-build/x86_64-build-output-9-dev/oracle-database-preinstall-23ai-1.0-1.4.el9.x86_64.rpm  --output oracle-database-preinstall-23ai-1.0-1.4.el9.x86_64.rpm  && \
        dnf install -y oracle-database-preinstall-23ai-1.0-1.4.el9.x86_64.rpm  && \
        rm -f /etc/systemd/system/oracle-database-preinstall-23ai-firstboot.service && \ 
        dnf clean all; \
else \
        dnf -y install oraclelinux-developer-release-el8 && \
        dnf -y install oracle-database-preinstall-23c && \
        rm -f /etc/rc.d/init.d/oracle-database-preinstall-23c-firstboot && \
        dnf clean all; \
fi && \
dnf -y install net-tools zip unzip tar openssl openssh-server vim-minimal which passwd sudo  python3 hostname fontconfig lsof  && \
dnf clean all && \
chmod ug+x $SCRIPT_DIR/*.sh && \
rm -f /etc/sysctl.conf && \
rm -f /usr/lib/systemd/system/dnf-makecache.service