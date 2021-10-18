# LICENSE UPL 1.0
#
# Copyright (c) 2018,2021 Oracle and/or its affiliates.
#
# ORACLE DOCKERFILES PROJECT
# --------------------------
# This is the Dockerfile for Oracle GSM 21c Release 3 to build the container image
# MAINTAINER <paramdeep.saini@oracle.com>
#
# This is the Dockerfile for Oracle GSM  21c
#
# REQUIRED FILES TO BUILD THIS IMAGE
# ----------------------------------
# (1) LINUX.X64_213000_gsm.zip
#     Download Oracle Database 21c GSM Software
#     from http://www.oracle.com/technetwork/database/enterprise-edition/downloads/index.html
#
# HOW TO BUILD THIS IMAGE
# -----------------------
# Put all downloaded files in the same directory as this Dockerfile
# Run:
#      $ docker build -t oracle/gsm:21.3.0 .
#
# Pull base image
# ---------------
FROM oraclelinux:7-slim as base

# Maintainer
# ----------
MAINTAINER Paramdeep Saini <paramdeep.saini@oracle.com>

# Environment variables required for this build (do NOT change)
# -------------------------------------------------------------
ENV GSM_BASE="/u01/app/oracle" \
    GSM_HOME="/u01/app/oracle/product/21c/gsmhome_1" \
    INVENTORY="/u01/app/oracle/oraInventory" \
    INSTALL_DIR="/opt/oracle/scripts" \
    INSTALL_FILE_1="LINUX.X64_213000_gsm.zip" \
    INSTALL_RSP="21c_gsm_install.rsp" \
    RUN_FILE="runOracle.sh" \
    SETUP_LINUX_FILE="setupLinuxEnv.sh" \
    CHECK_SPACE_FILE="checkSpace.sh" \
    USER_SCRIPTS_FILE="runUserScripts.sh" \
    INSTALL_GSM_BINARIES_FILE="installGSMBinaries.sh" \
    GSM_SETUP_FILE="setupOshardEnv.sh"  \
    GSM_ENV_SETUP_FILE="setupGSM.sh" \
    GSM_SCRIPTS="scripts" \
    MAINPY="main.py" \
    CHECKLIVENESS="checkLiveness.sh" 
# Use second ENV so that variable get substituted
ENV  INSTALL_SCRIPTS=$INSTALL_DIR/install \
     ORACLE_HOME=$GSM_HOME \
     ORACLE_BASE=$GSM_BASE \
     SCRIPT_DIR=$INSTALL_DIR/sharding \
     PATH=/bin:/usr/bin:/sbin:/usr/sbin \
     GSM_PATH=$GSM_HOME/bin:$PATH \
     GSM_LD_LIBRARY_PATH=$GSM_HOME/lib:/usr/lib:/lib 
    

# Copy files needed during both installation and runtime
# ------------
COPY $INSTALL_FILE_1 $SETUP_LINUX_FILE $CHECK_SPACE_FILE $INSTALL_RSP $GSM_ENV_SETUP_FILE $INSTALL_GSM_BINARIES_FILE $GSM_SETUP_FILE $INSTALL_DIR/install/
COPY $RUN_FILE $GSM_SETUP_FILE $CHECKLIVENESS $USER_SCRIPTS_FILE $SCRIPT_DIR/
COPY $GSM_SCRIPTS $SCRIPT_DIR/scripts/

RUN chmod 755 $INSTALL_DIR/install/*.sh && \
    sync && \
    $INSTALL_DIR/install/$CHECK_SPACE_FILE && \
    $INSTALL_DIR/install/$SETUP_LINUX_FILE && \
    $INSTALL_DIR/install/$GSM_ENV_SETUP_FILE  && \
    sed -e '/hard *memlock/s/^/#/g' -i /etc/security/limits.d/oracle-database-preinstall-21c.conf && \
    su oracle -c "$INSTALL_DIR/install/$INSTALL_GSM_BINARIES_FILE" && \
    $INVENTORY/orainstRoot.sh && \
    $GSM_HOME/root.sh && \
    rm -rf $INSTALL_DIR/install && \
    rm -f /etc/sysctl.d/99-oracle-database-preinstall-21c-sysctl.conf && \
    rm -f /etc/sysctl.d/99-sysctl.conf && \
    rm -f /etc/rc.d/init.d/oracle-database-preinstall-21c-firstboot && \
    rm -f /etc/security/limits.d/oracle-database-preinstall-21c.conf && \
    rm -f $INSTALL_DIR/install/*  && \
    chown -R oracle:oinstall $SCRIPT_DIR && \
    chmod 755 $SCRIPT_DIR/*.sh && \
    chmod 755 $SCRIPT_DIR/scripts/*.py && \
    sync

USER  oracle
WORKDIR /home/oracle
EXPOSE 1521

VOLUME ["$GSM_BASE/oradata"]

HEALTHCHECK --interval=2m --start-period=25m \
   CMD "$SCRIPT_DIR/$CHECKLIVENESS" >/dev/null || exit 1

# Define default command to start Oracle Database.
CMD exec $SCRIPT_DIR/$RUN_FILE
