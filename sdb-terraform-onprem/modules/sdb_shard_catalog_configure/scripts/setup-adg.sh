# Copyright 2017, 2019, Oracle Corporation and/or affiliates.  All rights reserved.
# Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl

#!/bin/bash

#    DESCRIPTION
#      Setup an [auxiliary] clone or a physical Active Data Guard standby
#      database(s) from a primary [target] database. (+broker w/adg).
#      All databases are OMF and ACTIVE (archivelog enabled).
# 
# 
#    FUNCTIONS:
#      dup_fghelp - get help to show input args
#      dup_fheadr - print calling function name
#      dup_fgtime - get time and day
#      dup_felaps - get elapsed time
#      dup_fatalf - fatal error (format errno [line no] message)
#      dup_fechos - display comments
#      dup_fisymb - search string for a symbol
#      ddlSQL - ddl sql
#      rsSQL - Result sql
#      PreProcessing  - pre-processing
#      ConnectParse   - tokenize target connect string
#      SanityCheck    - perform sanity checks
#      TargetDB       - Enable target for data guard
#      StaticListener - static listener
#      AuxiliaryDB    - instantiate auxiliary
#      RmanDuplicate  - rman duplicate
#      ResetListener  - local listener 
#      ClusterDB      - add auxiliary Database resource
#      dgBroker       - data guard broker
#

PN=${0##*/}
VER='V20.0'

# GLOBAL 
AS_SYSDBA="as sysdba"

#
# function dup_fghelp - input args
#
dup_fghelp ()
{
   echo 
   echo "OPTIONS:"
   echo "-----------------------------------------------------------------------"
   echo "   -v verbose                                                          "
   echo "   -h Show this help message                                           "
   echo "   -l target EZConnect login                                        (+)"
   echo "   -o FSFO (w/g mode)                                                  "
   echo "   -c auxiliary db clone option                                        "
   echo "   -g auxiliary db standby option                               [g]uard"
   echo "   -d auxiliary db db_unique_name                        [{ORACLE_SID}]"
   echo "   -a auxiliary db admin managed db (RAC)                      [policy]"
   echo "   -n auxiliary db nodes                                   [local host]"
   echo "   -s auxiliary db data storage                 [{ORACLE_BASE}/oradata]"
   echo "   -r auxiliary db recovery storage  [{ORACLE_BASE}/fast_recovery_area]"
   echo "-----------------------------------------------------------------------"
   echo "   (+) - requires input argument, [*] - denote default option"

   exit 0
}

# HELPER FUNCTIONS

#
# function dup_fheadr - print calling function name
#
dup_fheadr () 
{ 
   echo "$( date +"%y-%m-%d %T") ${1}() ..."| tee -a $LOGF
}

#
# function dup_fgtime - get time and day
#
dup_fgtime () 
{ 
   [ "$OS" = "SunOS" ] && TM=$SECONDS || TM="$(date +%s)" 
}

#
# function dup_felaps - get elapsed time
#
dup_felaps ()
{
   tstart=$1
   tend=$2

   et=$(( $tend - $tstart ))
   if [[ $et -lt 60 ]]; then
      unit="s"
   elif [[ $et -ge 60 && $et -lt 3600 ]]; then
      et=$(echo "scale=2; $et / 60" | bc -l)
      unit="m"
   else
      et=$(echo "scale=2; $et / 3600" | bc -l)
      unit="h"
   fi
   ELT="${et}${unit}"

   return 0
}

#
# function dup_fatalf $? [$LINENO] - fatal error 
#
dup_fatalf () 
{ 
   echo "Error: $@"
   echo 
   echo "Log: $LOGF"
   exit 1
}

#
# function dup_fechos - display comments
#
dup_fechos () 
{ 
   echo "$( date +"%y-%m-%d %T") ... [OK] $@" | tee -a $LOGF 
}

#
# function dup_fisymb - search string for a symbol
#
dup_fisymb () { [[ ! "${2##*$1*}" ]]; }

#
# function ddlSQL - auxiliary instance
#
ddlSQL () 
{
   CON="$1"
   SQL="$2"

   sqlplus "$CON" <<EOF >> $LOGF
   set echo on
   ${SQL};
   exit sql.sqlcode;
EOF
   return $?
}

#
# function rsSQL - target database
#
rsSQL ()
{
   CON="$1"
   SQL="$2"
   RS=

   RS=`sqlplus -s "$CON" <<EOF 2>>$LOGF
   set pagesize 0 feedback off verify off heading off echo off
   ${SQL};
   exit sql.sqlcode;
EOF`
   status=$?

   echo $SQL >> $LOGF
   echo $RS >> $LOGF

   return $status
}

# CORE FUNCTIONS

#------------------------------------------------------------------------------
#
# function PreProcessing - pre-processing
#
#------------------------------------------------------------------------------
PreProcessing () 
{
   dup_fheadr "$FUNCNAME"
   dup_fgtime;TS=$TM

   LSNRN="LISTENER_${XDBUN}"
   XLSORA="${TNS_ADMIN}/listener.ora"
   XLSBKP="${TNS_ADMIN}/listener.dupe"

   msg="Warning! aux $LSNRN is already running!"
   lsnrctl status $LSNRN  >/dev/null && dup_fechos $? [$LINENO] $msg

   # Validate connection to target
   msg="target Database Connection test: $TEZDBA" 
   SQL="SELECT 1 FROM DUAL"
   ddlSQL "$TEZDBA" "$SQL" || dup_fatalf $? [$LINENO] $msg
   [ $DEBUG ] && dup_fechos $msg

   msg="ORACLE_HOME not found"
   [ -d $ORACLE_HOME ] || dup_fatalf $? [$LINENO] $msg
   [ $DEBUG ] && dup_fechos "auxiliary ORACLE_HOME [$ORACLE_HOME]"

   msg="TNS_ADMIN not found"
   [ -d $TNS_ADMIN ] || dup_fatalf $? [$LINENO] $msg
   [ $DEBUG ] && dup_fechos "auxiliary TNS_ADMIN   [$TNS_ADMIN]"

   # ORACLE_BASE or not
   msg="target database oracle release version"
   SQL="select SUBSTR(VERSION,1,2) from v\$instance"
   rsSQL "$TEZDBA" "$SQL" && OVERS=$RS || dup_fatalf $? [$LINENO] $msg
   [ $DEBUG ] && dup_fechos "$msg [$OVERS]"

  # if [ $OVERS -lt 20 ]; then
      # is ROOH enabled by user?
  #    cd $ORACLE_HOME/bin >/dev/null
  #    ob=$(orabasehome)
  #    [ $ob = $ORACLE_HOME ] && AUX_BASE=$ORACLE_HOME || AUX_BASE=$ORACLE_BASE
  # else
  #    AUX_BASE=$ORACLE_BASE
  # fi
  AUX_BASE=$ORACLE_HOME  

   [ $DEBUG ] && dup_fechos "auxiliary ORACLE_BASE [$AUX_BASE]"
  
   msg="auxiliary check for ORACLE_BASE [AUX_BASE]"
   [ -d ${AUX_BASE} ] || dup_fatalf $? [$LINENO] $msg
   [ $DEBUG ] && dup_fechos $msg

   # nodes
   XNODEC=$(echo "${XNODE}" | tr -d ' ')   # NODES list comma separated
   XNODEB=$(echo "${XNODEC}" | tr ',' ' ') # NODES list blank separated
   NARRAY=($XNODEB)
   XHOST=`uname -n`
   XMAX_INST=${#NARRAY[@]}

   # target db instance count
   msg="target database instance count"
   SQL="SELECT COUNT(*) FROM GV\$INSTANCE"
   rsSQL "$TEZDBA" "$SQL" && TMAX_INST=$RS || dup_fatalf $? [$LINENO] $msg

   msg="target database instance count [$RS]"
   [ $DEBUG ] && dup_fechos $msg

   msg="auxiliary database instance count [$XMAX_INST]"
   [ $DEBUG ] && dup_fechos $msg

   # Restriction
   msg1="auxiliary db instances [${XMAX_INST}] > target's [${TMAX_INST}]!"
   msg2="auxiliary db instances must be less or equal to target's"
   [ $XMAX_INST -le $TMAX_INST ] || dup_fatalf $? [$LINENO] $msg1
   [ $DEBUG ] && dup_fechos $msg2

   #Validate nodes
   msg="auxiliary host validation"
   for n in $XNODEB
   do
       host $n >/dev/null 2>&1 || dup_fatalf $? [$LINENO] "$msg [${n}]"
   done
   [ $DEBUG ] && dup_fechos $msg

   # target ORACLE_HOME
   msg="target database oracle home"
   SQL="SELECT sys_context('USERENV', 'ORACLE_HOME') FROM dual" 
   rsSQL "$TEZDBA" "$SQL" && TARGET_OH="$RS" || dup_fatalf $? [$LINENO] $msg

   # db_name
   msg="common database name"
   SQL="SELECT SYS_CONTEXT ('USERENV', 'DB_NAME') FROM DUAL"
   rsSQL "$TEZDBA" "$SQL" && dbname=$RS || dup_fatalf $? [$LINENO] $msg
   [[ "$dbname" != "" ]] || dup_fatalf $? [$LINENO] "$msg is null"
   [ $DEBUG ] && dup_fechos "$msg [ $dbname ]"

   # target db unique_name
   msg="target Database db_unique_name"
   SQL="SELECT SYS_CONTEXT ('USERENV', 'DB_UNIQUE_NAME') from dual"
   rsSQL "$TEZDBA" "$SQL" && TDBUN=$RS || dup_fatalf $? [$LINENO] $msg
   msg="$msg [ $TDBUN ]"
   [ $DEBUG ] && dup_fechos $msg

   PRI="$(echo $TDBUN| tr '[:lower:]' '[:upper:]')"
   AUX="$(echo $XDBUN| tr '[:lower:]' '[:upper:]')"

   msg="database DB_UNIQUE_NAME"
   [[ "$PRI" != "" ]] || dup_fatalf $? [$LINENO] "target $msg is null"
   [[ "$AUX" != "" ]] || dup_fatalf $? [$LINENO] "auxiliary $msg is null"

   # if clone/standby on same host db_unique_name must be different
   msg="auxiliary db_unique_name[ $AUX ] is same as the target's [PRI]"
   if [[ "$XHOST" == "$THOST" ]]; then 
      [[ "$PRI" != "$AUX" ]] || dup_fatalf $? [$LINENO] $msg
   fi

   msg="auxiliary db_unique_name [ $XDBUN ]"
   [ $DEBUG ] && dup_fechos $msg

   if [[ "$DBOP" == "g" ]]; then
      msg1="existing aux in DGCFG"
      SQL="SELECT count(*) FROM V\$DATAGUARD_CONFIG 
           WHERE DEST_ROLE like '%STANDBY%'"
      rsSQL "$TEZDBA" "$SQL" || dup_fatalf $? [$LINENO] $msg1

      [ $RS -eq 0 ] && DGEXIST=0 || DGEXIST=1
      msg2="existing aux in DGCFG [ $DGEXIST ]"
      [ $DEBUG ] && dup_fechos $msg2

      if [ $DGEXIST -ne 0 ]; then
         msg="existing aux unique_name check in DGCFG"
         SQL="SELECT 1 FROM V\$DATAGUARD_CONFIG 
              WHERE DEST_ROLE='PHYSICAL STANDBY' 
                AND UPPER(DB_UNIQUE_NAME)='${AUX}'"
         rsSQL "$TEZDBA" "$SQL" || dup_fatalf $? [$LINENO] $msg

         msg1="found existing auxiliary db_unique_name in DGCFG [ $XDBUN ]"
         msg2="no existing auxiliary db_unique_name in DGCFG [ $XDBUN ]"
         [ "$RS" ] && dup_fatalf $? [$LINENO] $msg1
         [ $DEBUG ] && dup_fechos $msg2
      fi
   fi

   # Local listener
   netstat -lntpv 2>/dev/null | grep 1521 >/dev/null
   [ $? -eq 0 ] && PORT_1521=true || PORT_1521=false

   # static listener: SPORT
   # 
   SPORT=1521
   # while [ 1 ];
   # do
   #   netstat -lntpv 2>/dev/null | grep $SPORT >/dev/null
   #   [ $? -eq 0 ] && (( ++SPORT )) || break
   # done

   msg="auxiliary designated $LSNRN port: $SPORT"
   [ $DEBUG ] && dup_fechos $msg
   
   if [ ! -d ${ORACLE_BASE}/diag ]; then 
      msg="auxiliary Database ORACLE_BASE/diag"
      mkdir -p ${ORACLE_BASE}/diag >> $LOGF || dup_fatalf $? [$LINENO] $msg
      [ $DEBUG ] && dup_fechos $msg
   fi

   # is RAC
   srvctl status nodeapps > /dev/null && XRAC=TRUE || XRAC=FALSE
   # is auxiliary rac? 
   msg="auxiliary Database rac=$XRAC"

   # Shared storage is required in RAC
   if [[ "$XRAC" == "TRUE" ]]; then
       msg="auxiliary Database shared data storage check"
       [[ $XDATA ]] || dup_fatalf $? [$LINENO] $msg
       [ $DEBUG ] && dup_fechos $msg

       [[ $XFRA ]] || XFRA=$XDATA

       # is auxiliary storage ASM?
       #
       [ "${XDATA:0:1}" = '+' ] && XASM=true
       if [ $XASM ]; then
           XDGD=`echo $XDATA | cut -b 2-`
           XDGR=`echo $XFRA | cut -b 2-`
       fi

       [ $XASM ] && msg="auxiliary Database data storage [${XASM}]" || \
       msg="auxiliary Database storage [CFS]"
       [ $DEBUG ] && dup_fechos $msg
   else
       XDATA="${ORACLE_BASE}/oradata"
       msg="auxiliary Database shared data storage [${XDATA##*/}]"
       [ $DEBUG ] && dup_fechos $msg

       XFRA="${ORACLE_BASE}/fast_recovery_area"
       msg="auxiliary Database shared recovery storage [${XFRA##*/}]"
       [ $DEBUG ] && dup_fechos $msg
   fi

   AUXINI="${AUX_BASE}/dbs/init${XDBUN}.ora"
   [[ "$XRAC" == "TRUE" ]] && \
       AUXSPF="${XDATA}/spfile${XDBUN}.ora" || \
       AUXSPF="${AUX_BASE}/dbs/spfile${XDBUN}.ora"

   AUXOPW="${AUX_BASE}/dbs/orapw${XDBUN}"

   dup_fgtime;TE=$TM
   dup_felaps $TS $TE
   echo "$( date +"%y-%m-%d %T") ... Completed ($ELT)"
}

#------------------------------------------------------------------------------
#
# function ConnectParse - tokenize login string
# get target's password, host, port, svc, dbname and domain
#
#  var     description
#  ------  ---------------------------------------
#  sysuser dba sys user
#  syspass password
#  THOST   hostname/scan alias of the target database
#  tport   listener tport on the target (default 1521)
#  TSVCQ    target local database service name fully qualified
#  TSVC  target sevice_name absolute
#  domain  database domain name (if exists)
#------------------------------------------------------------------------------
#
ConnectParse () 
{
   dup_fheadr "$FUNCNAME"
   dup_fgtime;TS=$TM

   # Tokenize
   #
   msg="target Database input Connect string parsing" 
   [ $DEBUG ] && dup_fechos $msg
   dup_fisymb '@' "$TEZCON"
   [ $? -eq 0 ] && constr=( $(echo "$TEZCON" | tr '@' '\n') ) || \
      dup_fatalf $? [$LINENO]  $msg
   logstr=${constr[0]}                 
   cidstr=${constr[1]}                 # 
   TEZCID=$cidstr                      # for dgmgrl

   msg="$logstr"
   dup_fisymb '/' "$logstr"
   [ $? -eq 0 ] && xtoken=( $(echo "$logstr" | tr '/' '\n') ) || \
      dup_fatalf $? [$LINENO]  $msg
   sysuser=${xtoken[0]}                
   syspass=${xtoken[1]}                

   msg="$cidstr"
   dup_fisymb '/' "$cidstr"
   [ $? -eq 0 ] && cid=( $(echo "$cidstr" | tr '/' '\n') ) || \
      dup_fatalf $? [$LINENO] $msg
   cid1=${cid[0]}                      # 
   cid2=${cid[1]}                      # 

   dup_fisymb ':' "$cid1"
   if [ $? -eq 0 ]; then 
      ytoken=( $(echo "$cid1" | tr ':' '\n') ) 
      THOST=${ytoken[0]}               # 
      tport=${ytoken[1]}               # 1521
   else
      THOST=$cid1                      # 
      tport=$tport                     # 1521
   fi

   [[ $tport ]] || TPORT=1521 && TPORT=$tport 

   TSVCQ="$cid2"                        # 
   msg5="target Databse Service_name [ $TSVCQ ]"
   [[ $TSVCQ ]] || dup_fatalf $? [$LINENO] $msg5

   dup_fisymb '.' "$cid2"
   if [ $? -eq 0 ]; then 
      ztoken=( $(echo "${cid2[0]}" | tr '.' '\n') )
      TSVC=${ztoken[0]}            # orcl
      domain="${TSVCQ#*.}"     # us.oracle.com
   else
      TSVC=$cid2
      domain=
   fi
   [ $domain ] && DOM=".${domain}" || DOM=

   # verify not null
   msg1="Common sys user [ $sysuser ]"
   msg2="Common sys password check"
   msg3="target Database host name [ $THOST ]"
   msg4="target Database listener port [ $TPORT ]"
   msg6="target Database db_unique_name [ $TSVC ]"
   msg7="common db_domain [ $domain ]"

   [[ $sysuser ]] || dup_fatalf $? [$LINENO] $msg1
   [[ $syspass ]] || dup_fatalf $? [$LINENO] $msg2
   [[ $THOST ]] || dup_fatalf $? [$LINENO] $msg3
   [[ $TPORT ]] || dup_fatalf $? [$LINENO] $msg4
   [[ $TSVCQ ]]  || dup_fatalf $? [$LINENO] $msg5
   [[ $TSVC ]]  || dup_fatalf $? [$LINENO] $msg6

   if [ $DEBUG ]; then 
       dup_fechos $msg1; dup_fechos $msg2; dup_fechos $msg3; dup_fechos $msg4
       dup_fechos $msg5; dup_fechos $msg6; dup_fechos $msg7
   fi


   dup_fgtime;TE=$TM
   dup_felaps $TS $TE
   echo "$( date +"%y-%m-%d %T") ... Completed ($ELT)"

}

#------------------------------------------------------------------------------
#
# function SanityCheck - perform sanity checks
#
#------------------------------------------------------------------------------
SanityCheck () 
{
   dup_fheadr "$FUNCNAME"
   dup_fgtime;TS=$TM

   # TDE
   if [[ "$FSFO" == "true" ]]; then
      msg1="target Database TDE"
      msg2="copy the wallet to aux and to update its sqlnet.ora"
      SQL="SELECT STATUS from V\$ENCRYPTION_WALLET"
      rsSQL "$TEZDBA" "$SQL" || dup_fatalf $? [$LINENO] $msg1
      [ "$RS" != "NOT_AVAILABLE" ] && dup_fatalf $? [$LINENO] $msg2
      [ $DEBUG ] && dup_fechos "$msg1 [${RS}]"
   fi

   # spfile
   msg="target Database spfile"
   [ $DEBUG ] && dup_fechos $msg
   SQL="SELECT VALUE FROM V\$PARAMETER WHERE NAME='spfile'"
   rsSQL "$TEZDBA" "$SQL" || dup_fatalf $? [$LINENO] $msg
   [ "$RS" = "" ] && dup_fatalf $? [$LINENO] $msg

   # is archivelog
   msg="target Database log_mode"
   SQL="SELECT LOG_MODE FROM V\$DATABASE"
   rsSQL "$TEZDBA" "$SQL" || dup_fatalf $? [$LINENO] $msg
   msg="target Database log_mode [${RS}]"
   [ "$RS" = "ARCHIVELOG" ] || dup_fatalf $? [$LINENO] $msg
   [ $DEBUG ] && dup_fechos $msg

   # OMF - DB_CREATE_FILE_DEST
   msg="target database DATA "
   SQL="select value from v\$parameter where name='db_create_file_dest'"
   rsSQL "$TEZDBA" "$SQL" || dup_fatalf $? [$LINENO] $msg
   TDATA=$RS
   msg="target database DATA [ $TDATA ]"
   [ $DEBUG ] && dup_fechos $msg

   # OMF - DB_RECOVERY_FILE_DEST must exist
   msg="target database FRA"
   SQL="select value from v\$parameter where name='db_recovery_file_dest'"
   rsSQL "$TEZDBA" "$SQL" || dup_fatalf $? [$LINENO] $msg
   TFRA=$RS
   msg="target database FRA [ $TFRA ]"
   [ $DEBUG ] && dup_fechos $msg

   # db_recovery_file_dest_SIZE must be set
   msg="target Database db_recovery_file_dest_size"
   SQL="SELECT ROUND((VALUE)/1024/1024/1024,0) FROM V\$PARAMETER 
        WHERE NAME='db_recovery_file_dest_size'"
   rsSQL "$TEZDBA" "$SQL" || dup_fatalf $? [$LINENO] $msg
   msg="$msg [${RS} GB]"
   [ "$RS" = "" ] && dup_fatalf $? [$LINENO] $msg
   [ $RS -eq 0 ] && dup_fatalf $? [$LINENO] $msg
   [ $DEBUG ] && dup_fechos $msg

   # OMF checks
   msg="target database must be OMF "
   [[ "$TDATA" != "" ]] || dup_fatalf $? [$LINENO] $msg

   msg="target database must be OMF "
   [[ "$TFRA" != "" ]] || dup_fatalf $? [$LINENO] $msg

   msg="target Database flashback check"
   SQL="SELECT FLASHBACK_ON FROM V\$DATABASE"
   rsSQL "$TEZDBA" "$SQL" || dup_fatalf $? [$LINENO] $msg
   msg="target Database flashback [$RS]"
   TFLASH="$RS"
   [ $DEBUG ] && dup_fechos $msg

   if [[ "$FSFO" == "true" ]]; then
      msg="target Database FSFO requires FLASHBACK on"
      [[ "$TFLASH" == "YES" ]] || dup_fatalf $? [$LINENO] $msg
      [ $DEBUG ] && dup_fechos $msg

      msg="target Database restore point check"
      SQL="SELECT count(*) FROM V\$RESTORE_POINT"
      rsSQL "$TEZDBA" "$SQL" || dup_fatalf $? [$LINENO] $msg
      RP="$RS"
      msg="target Database restore point found (not supported)"
      [ $RP -eq 0 ] || dup_fatalf $? [$LINENO] $msg
      msg="target Database restore point none (ok)"
      [ $DEBUG ] && dup_fechos $msg
   
      [[ "$DBOP" == "g" && "$FSFO" == "true" ]] \
          && FAILOVER=true || FAILOVER=false
   fi

   msg="target Database recovery status"
   SQL="SELECT COUNT(*) FROM V\$RECOVERY_STATUS"
   rsSQL "$TEZDBA" "$SQL" || dup_fatalf $? [$LINENO] $msg
   [ $RS -eq 0 ] || dup_fatalf $? [$LINENO] $msg
   [ $DEBUG ] && dup_fechos $msg

   msg="target Database header files status"
   [ $DEBUG ] && dup_fechos $msg
   SQL="SELECT COUNT(file#) FROM V\$DATAFILE_HEADER WHERE RECOVER ='YES'"
   rsSQL "$TEZDBA" "$SQL" || dup_fatalf $? [$LINENO] $msg
   [ $RS -eq 0 ] || dup_fatalf $? [$LINENO] $msg

   msg="target database RAC"
   SQL="select value from v\$parameter where name='cluster_database'"
   rsSQL "$TEZDBA" "$SQL" || dup_fatalf $? [$LINENO] $msg
   TRAC=$RS 
   msg="target database RAC [ $TRAC ]"
   [ $DEBUG ] && dup_fechos $msg

   if [[ "$TRAC" == TRUE ]]; then
          [ "${TDATA:0:1}" = '+' ] && TASM=true

      if [[ "$DBOP" == "g" ]]; then
          # Broker data files must be on a sharable disk in RAC
          msg="target database DG_BROKER_CONFIG_FILES"
          SQL="select value from v\$parameter where 
               name='dg_broker_config_file1' and upper(value) like '%${TDATA}%'"
          rsSQL "$TEZDBA" "$SQL" || dup_fatalf $? [$LINENO] $msg
          dgbcf=$RS
          if [[ "$dgbcf" == "" ]]; then 
             dup_fatalf $? [$LINENO] "$msg $dgbcf !shared" || \
             dup_fechos "$msg $dgbcf"
          fi
      fi
   fi

   #
   # AUX checks
   #
   if [[ "$XRAC" == "TRUE" ]]; then
       # olsnodes is not available in database home, get its equivalent
       racnl=$(srvctl status listener | grep running)
       racnodes=${racnl#*:}
       OLSNODES=$(echo "${racnodes}" | tr ',' ' ') # NODES list blank separated
       msg="auxiliary Database node(s) verification"
       for n in $XNODEB
       do
           for i in $OLSNODES
           do
               found=1
               [[ "$i" == "$n" ]] && { (( j++ )); found=0; break; }
           done
       done
       [[ $j -eq $XMAX_INST && $found ]] || dup_fatalf $? [$LINENO] $msg
       [ $DEBUG ] && dup_fechos $msg

       # DB policy management & Scan aliase
       [ $XPOLICY ] && XCRSPOOL="${XDBUN}pool"

       SCANNM=$(srvctl config scan | grep 'SCAN name')
       dup_fisymb ',' "$SCANNM"
       [ $? -eq 0 ] && scantok=( $(echo "$SCANNM" | tr ',' '\n') ) 
       XSCAN=${scantok[2]}                 # scan alias
       [[ $XSCAN ]] || msg="auxiliary Database scan not available" && \
           msg="auxiliary Database scan [$XSCAN]"
       [ $DEBUG ] && dup_fechos $msg

       msg="auxiliary Database RAC [$XRAC]"
       [ $DEBUG ] && dup_fechos $msg

       msg="auxiliary RAC listener status"
       srvctl status listener >> $LOGF || dup_fatalf $? [$LINENO] $msg
       [ $DEBUG ] && dup_fechos $msg

       msg="Warning! auxiliary config database [$XDBUN] found"
       srvctl config database -d $XDBUN >> $LOGF && dup_fechos $msg

       if [ $XPOLICY ]; then
           msg="Warning! auxiliary config srvpool [$XCRSPOOL]"
           srvctl config srvpool -g $XCRSPOOL >> $LOGF && \
              { spfoud=true; dup_fechos $msg; }

           if [[ ! $spfoud ]]; then
              for n in $XNODEC
              do
                 msg="auxiliary check srvpool for host [$n]"
                 srvctl config srvpool | grep $n >> $LOGF && \
                 dup_fatalf $? [$LINENO] $msg
              done
          fi
       fi
   else
       msg="auxiliary Database is a non-RAC SI"
       [ $DEBUG ] && dup_fechos $msg
   fi
       
   if [[ "$XRAC" != "TRUE" ]]; then 
       msg="Multiple hosts given for NON-RAC SI configuration"
       [[ $XMAX_INST -gt 1 ]] && dup_fatalf $? [$LINENO] $msg

       msg="auxiliary database data storage [${XDATA##*/}]"
       test -d $XDATA || \
           { mkdir -p $XDATA || dup_fatalf $? [$LINENO] $msg; }
       [ $DEBUG ] && dup_fechos $msg

       msg="auxiliary database recovery storage [${XFRA##*/}]"
       test -d $XFRA || \
           { mkdir -p $XFRA || dup_fatalf $? [$LINENO] $msg; }
       [ $DEBUG ] && dup_fechos $msg
   fi

   # Home directories (not exists w/ S/W only install)
   #
   OLD_UMASK=`umask`
   umask 0027

   msg="auxiliary database ORACLE_BASE/admin"
   if [ ! -d ${ORACLE_BASE}/admin ]; then
      mkdir -p ${ORACLE_BASE}/admin || dup_fatalf $? [$LINENO] $msg
      [ $DEBUG ] && dup_fechos $msg
   fi

   msg="auxiliary database ORACLE_BASE/audit"
   if [ ! -d ${ORACLE_BASE}/audit ]; then
      mkdir -p ${ORACLE_BASE}/audit || dup_fatalf $? [$LINENO] $msg
      [ $DEBUG ] && dup_fechos $msg
   fi

   msg="auxiliary database $AUX_BASE/dbs"
   if [ ! -d ${AUX_BASE}/dbs ]; then
      mkdir -p ${AUX_BASE}/dbs || dup_fatalf $? [$LINENO] $msg
      [ $DEBUG ] && dup_fechos $msg
   fi
   umask ${OLD_UMASK}

   if [[ "$DBOP" == "g" ]]; then
      # Broker EZConnect
      DTCID="${THOST}:${TPORT}/${TDBUN}${DOM}:dedicated"
      if [[ "$XRAC" == "TRUE" ]]; then
         DXCID="${XSCAN}:${TPORT}/${XDBUN}${DOM}:dedicated"
      else
         DXCID="${XHOST}:${SPORT}/${XDBUN}${DOM}:dedicated"
      fi
   
      # broker connections
      DPEZLOG="sysdg/${syspass}@${DTCID}"
      DXEZLOG="sysdg/${syspass}@${DXCID}"

      msg="target Database as sysdg connection check"
      msg1="$msg failed connection [$DPEZLOG]"
      SQL="select 1 from dual"
      rsSQL "$DPEZLOG as sysdg" "$SQL" || dup_fatalf $? [$LINENO] $msg1
      TOPWD="$RS"
      msg2="$msg sql return[$TOPWD], expecting 1"
      [ $TOPWD -eq 1 ] || dup_fatalf $? [$LINENO] "$msg $msg2"
      [ $DEBUG ] && dup_fechos "$msg [ok]"
   fi

   dup_fgtime;TE=$TM
   dup_felaps $TS $TE
   echo "$( date +"%y-%m-%d %T") ... Completed ($ELT)"

}

#------------------------------------------------------------------------------
#
# function TargetDB - Enable data guard
#
#------------------------------------------------------------------------------
TargetDB () 
{
   
   [[ "$DBOP" == "g" ]] || return

   dup_fheadr "$FUNCNAME"
   dup_fgtime;TS=$TM

   # is target force_logging
   msg="target Database force_logging"
   SQL="SELECT FORCE_LOGGING FROM V\$DATABASE"
   rsSQL "$TEZDBA" "$SQL" || dup_fatalf $? [$LINENO] $msg
   [ $DEBUG ] && dup_fechos $msg

   msg="target Databse force_logging [ $RS ]"
   [ $DEBUG ] && dup_fechos $msg
   force_logging=$RS 

   if [ "$force_logging" = "NO" ]; then
       msg="target Database enable force_logging"
       [ $DEBUG ] && dup_fechos $msg
       SQL="ALTER DATABASE FORCE LOGGING"
       ddlSQL "$TEZDBA" "$SQL" || dup_fatalf $? [$LINENO] $msg
   fi

   # is target standby_file_management set
   msg="target Database standby_file_management"
   SQL="SELECT VALUE FROM V\$PARAMETER WHERE NAME='standby_file_management'"
   rsSQL "$TEZDBA" "$SQL" || dup_fatalf $? [$LINENO] $msg
   [ $DEBUG ] && dup_fechos $msg

    msg="target Databse standby_file_management [ $RS ]"
   [ $DEBUG ] && dup_fechos $msg
   standby_file_management=$RS 

   if [ "$standby_file_management" != "AUTO" ]; then
       msg="target Database set standby_file_management auto"
       [ $DEBUG ] && dup_fechos $msg
       SQL="ALTER SYSTEM SET STANDBY_FILE_MANAGEMENT=AUTO SCOPE=BOTH SID='*'"
       ddlSQL "$TEZDBA" "$SQL" || dup_fatalf $? [$LINENO] $msg
   fi

   # is DG_BROKER_START=TRUE
   msg="target Database DG_BROKER_START"
   SQL="SELECT VALUE FROM V\$PARAMETER WHERE NAME='dg_broker_start'"
   rsSQL "$TEZDBA" "$SQL" || dup_fatalf $? [$LINENO] $msg
   [ $DEBUG ] && dup_fechos $msg
   DGBS=$RS 

   if [ "$DGBS" != "TRUE" ]; then
      msg="target Database enable DG_BROKER_START"
      SQL="ALTER SYSTEM SET DG_BROKER_START=TRUE SCOPE=BOTH SID='*'"
      ddlSQL "$TEZDBA" "$SQL" || dup_fatalf $? [$LINENO] $msg
      [ $DEBUG ] && dup_fechos $msg
   fi

   # Add standby logfile (SRL) on target
   msg="target database SRL check"
   SQLSRL="select count(*) from v\$logfile where type='STANDBY'"
   rsSQL "$TEZDBA" "$SQLSRL" && SRL=$RS || dup_fatalf $? [$LINENO] $msg

   if [ $SRL -eq 0 ]; then
      msg="target database SRL add"
      [ $DEBUG ] && dup_fechos $msg

      SQL="SELECT MAX (THREAD#) FROM V\$LOG"
      rsSQL "$TEZDBA" "$SQL" && threads=$RS || dup_fatalf $? [$LINENO] $msg

      SQL="SELECT max(bytes)/1024/1024 FROM V\$LOG"
      rsSQL "$TEZDBA" "$SQL" && logsz=$RS || dup_fatalf $? [$LINENO] $SQL

      for ((i = 1 ; i <= $threads ; i++))
      do
         SQL="SELECT COUNT(*) + 1 FROM V\$LOG WHERE THREAD#=$i"
         rsSQL "$TEZDBA" "$SQL" && maxlogs=$RS || dup_fatalf $? [$LINENO] $SQL

         for ((j = 1 ; j <= $maxlogs ; j++))
         do
            SQL="ALTER DATABASE ADD STANDBY LOGFILE thread $i SIZE ${logsz}M"
            ddlSQL "$TEZDBA" "$SQL"  || dup_fatalf $? [$LINENO] $SQL
         done
      done
   else
      msg="target database SRL exist"
      [ $DEBUG ] && dup_fechos $msg
   fi

   dup_fgtime;TE=$TM
   dup_felaps $TS $TE
   echo "$( date +"%y-%m-%d %T") ... Completed ($ELT)"
   
}

#------------------------------------------------------------------------------
#
# function StaticListener - static listener
#
#------------------------------------------------------------------------------
StaticListener () 
{
   dup_fheadr "$FUNCNAME"
   dup_fgtime;TS=$TM

   lsnrctl status $LSNRN >/dev/null && lsnrctl stop $LSNRN >/dev/null

   # Backup listener file
   [ -f $XLSBKP ] || { [ -f $XLSORA ] && cp $XLSORA $XLSBKP; }

   msg="auxiliary Database create/append static listener file"
   [ $DEBUG ] && dup_fechos $msg

   cat >> $XLSORA << EOF

# Generated by $PN configuration tools. (static)
${LSNRN} =
  (DESCRIPTION_LIST =
    (DESCRIPTION =
      (ADDRESS = (PROTOCOL = TCP)(HOST = ${XHOST})(PORT = ${SPORT}))
    )
  )

SID_LIST_${LSNRN} =
  (SID_LIST =
    (SID_DESC =
      (GLOBAL_DBNAME = ${XDBUN}${DOM})
      (ORACLE_HOME = ${ORACLE_HOME})
      (SID_NAME = ${XDBUN})
    )
  )
EOF

   msg="auxiliary Database start static listener"
   lsnrctl start ${LSNRN} >> $LOGF
   lsnrctl status ${LSNRN} >> $LOGF || dup_fatalf $? [$LINENO] $msg
   [ $DEBUG ] && dup_fechos $msg

   msg="auxiliary Database use of static listener port=$SPORT"
   [ $DEBUG ] && dup_fechos $msg

   dup_fgtime;TE=$TM
   dup_felaps $TS $TE
   echo "$( date +"%y-%m-%d %T") ... Completed ($ELT)"

}


#------------------------------------------------------------------------------
#
# function AuxiliaryDB - instantiate auxiliary
#
#------------------------------------------------------------------------------
AuxiliaryDB () 
{
   dup_fheadr "$FUNCNAME"
   dup_fgtime;TS=$TM

   LOC_LIST="(ADDRESS=(PROTOCOL=TCP)(HOST=${XHOST})(PORT=${SPORT}))"

   msg="auxiliary Database create init.ora"
   [ $DEBUG ] && dup_fechos $msg
   cat > $AUXINI << EOF
db_name='${XDBUN}'
sga_target=2000M
cluster_database=FALSE
local_listener='${LOC_LIST}'
EOF
   [[ "${DOM}" != "" ]] && cat >> $AUXINI << EOF
db_domain='${domain}'
EOF
  # [ $? -eq 0 ] || dup_fatalf $? [$LINENO] $msg

   msg="auxiliary Database temp orapwd"
   [ -f $AUXOPW ] && rm -f $AUXOPW
   orapwd file="${AUXOPW}" password="${syspass}"
   [ $? -eq 0 ] || dup_fatalf $? [$LINENO] $msg
   [ $DEBUG ] && dup_fechos $msg

   if [ -f $AUXSPF ]; then
      msg="auxiliary removed existing spfile"
      rm -f $AUXSPF
      [ $DEBUG ] && dup_fechos $msg
   fi

   msg="auxiliary Database startup nomount"
   [ $DEBUG ] && dup_fechos $msg
   SQL="STARTUP FORCE NOMOUNT"
   ddlSQL "$XBQDBA" "$SQL" || dup_fatalf $? [$LINENO] $msg

   msg="auxiliary Database register the static listener"
   [ $DEBUG ] && dup_fechos $msg
   SQL="ALTER SYSTEM REGISTER"
   ddlSQL "$XBQDBA" "$SQL" || dup_fatalf $? [$LINENO] $msg

   msg="auxiliary Database ini->spfile"
   echo "spfile='$AUXSPF'" > $AUXINI

   dup_fgtime;TE=$TM
   dup_felaps $TS $TE
   echo "$( date +"%y-%m-%d %T") ... Completed ($ELT)"
}

#------------------------------------------------------------------------------
#
# function RmanDuplicate - rman duplicate
#
#------------------------------------------------------------------------------
RmanDuplicate () 
{
   dup_fheadr "$FUNCNAME"
   dup_fgtime;TS=$TM

   RTCID=$TEZCID
   RXCID="${XHOST}:${SPORT}/${XDBUN}${DOM}"
   
   RPEZLOG="${sysuser}/${syspass}@${RTCID}:dedicated"
   RXEZLOG="${sysuser}/${syspass}@${RXCID}:dedicated"

   msg1="auxiliary Database Connection test"
   msg2="${msg1} [ $RXEZLOG ]"
   [ $DEBUG ] && dup_fechos $msg1
   SQL="SELECT 1 FROM DUAL"
   ddlSQL "$RXEZLOG $AS_SYSDBA" "$SQL" || dup_fatalf $? [$LINENO] $msg2

   case $DBOP in
               c) RMANCMD1="to '$XDBUN' from active database" 
               ;;
               g) RMANCMD1="for standby from active database dorecover"
               ;;
               ?) dup_fatalf $? [$LINENO] "Unknown database option \"$DBOP\""
               ;;
   esac

   msg="rman duplicate target Database ${RMANCMD1}"
   [ $DEBUG ] && dup_fechos $msg

   RMANCMD2="PARAMETER_VALUE_CONVERT '$TDBUN','$XDBUN','$PRI','$AUX','${TDATA}'\
            ,'${XDATA}','${TFRA}','${XFRA}'"

   SET="set db_create_file_dest '${XDATA}' 
        set db_recovery_file_dest '${XFRA}'
        set diagnostic_dest '${ORACLE_BASE}'
        set cluster_database 'FALSE'
        set audit_file_dest '${ORACLE_BASE}/audit'"
   [[ "$DBOP" != "g" ]] && \
      RMANCMD3="${SET}" || \
      RMANCMD3="${SET} set db_name '${dbname}' set db_unique_name '${XDBUN}'" 

   # RESET
   v=2
   lad=""
   while (( $v <= 31 ))
   do
      lad="$lad reset log_archive_dest_${v}"
      (( v++ ))
   done

   RESET="reset db_file_name_convert
          reset log_file_name_convert
          reset log_archive_config
          reset fal_server
          ${lad}"
   RMANCMD4=$RESET

   rman TARGET $RPEZLOG AUXILIARY $RXEZLOG trace=rman${XDBUN}.log<< EOF >> $LOGF
   run {
           debug on;
           allocate channel pux1 device type disk;
           allocate channel pux2 device type disk;
           allocate channel pux3 device type disk;
           allocate channel pux4 device type disk;
           allocate auxiliary channel aux1 device type disk;
           allocate auxiliary channel aux2 device type disk;
           allocate auxiliary channel aux3 device type disk;
           allocate auxiliary channel aux4 device type disk;
      duplicate target database
           $RMANCMD1 
      SPFILE 
           $RMANCMD2
           $RMANCMD3
           $RMANCMD4
      ;
      debug off;
      }
   quit
EOF
   [ $? -eq 0 ] || dup_fatalf $? [$LINENO] $msg

   msg="auxiliary check datafiles"
   SQL="ALTER SYSTEM CHECK DATAFILES"
   ddlSQL "$XBQDBA" "$SQL" || dup_fatalf $? [$LINENO] $msg
   [ $DEBUG ] && dup_fechos $msg

   if [[ "$TFLASH" == "YES" ]]; then
      msg="auxiliary enable flashback"
      SQL="alter database FLASHBACK ON"
      ddlSQL "$XBQDBA" "$SQL" || dup_fechos $msg
      [ $DEBUG ] && dup_fechos $msg
   fi

   if [[ "$DBOP" == "g" ]]; then
      #dg_broker_config_file
      msg="set dg_broker_config_file"
      [ $DEBUG ] && dup_fechos $msg
      cfg1="'${XDATA}/${AUX}/dr1${XDBUN}.dat'"
      cfg2="'${XDATA}/${AUX}/dr2${XDBUN}.dat'"
      DL1="alter system set dg_broker_config_file1="$cfg1" scope=spfile sid='*'"
      ddlSQL "$XBQDBA" "$DL1"
      DL2="alter system set dg_broker_config_file2="$cfg2" scope=spfile sid='*'"
      ddlSQL "$XBQDBA" "$DL2"
   fi

   msg="auxiliary restart"
   [ $DEBUG ] && dup_fechos $msg
   SQL="startup force"
   ddlSQL "$XBQDBA" "$SQL" || dup_fatalf $? [$LINENO] $msg

   SQL="SELECT decode(VALUE,null,0,1) FROM V\$PARAMETER 
        WHERE NAME='db_file_name_convert'"
   rsSQL "$TEZDBA" "$SQL" || tdbfnc=0
   [[ $RS != "" ]] && tdbfnc=$RS || tdbfnc=0
   msg="target db_file_name_convert [$tdbfnc]"
   [ $DEBUG ] && dup_fechos $msg

   if [ $tdbfnc -eq 1 ]; then
      msg="Get aux datafile path"
      SQL="SELECT DISTINCT SUBSTR (FILE_NAME,1,INSTR (FILE_NAME,'/',-1,1))
           FROM dba_data_files WHERE ROWNUM=1"
      rsSQL "$XBQDBA" "$SQL" || dup_fatalf $? [$LINENO] "$msg [$RS]"
      conv="$RS" 
      [ $DEBUG ] && dup_fechos "$msg [$conv]"

      if [[ "$conv" != "" ]]; then
         msg="set db_file_name_convert ['*','${conv}']"
         SQL="alter system set db_file_name_convert='*','${conv}' scope=spfile"
         ddlSQL "$XBQDBA" "$SQL" || dup_fatalf $? [$LINENO] $msg
         [ $DEBUG ] && dup_fechos $msg

         msg="auxiliary restart (set db_file_name_convert)"
         [ $DEBUG ] && dup_fechos $msg
         SQL="startup force"
         ddlSQL "$XBQDBA" "$SQL" || dup_fatalf $? [$LINENO] $msg
      fi
   fi

   # Summary
   if [ $DEBUG ]; then
      SQL="select dbid, name, db_unique_name, database_role, open_mode
           from v\$database"
      # target
      rsSQL "$TEZDBA" "$SQL" && tdb="$RS"
      # aux
      rsSQL "$XBQDBA" "$SQL" && xdb="$RS"

      echo "-------------------------------------"
      echo "DBID       DBN DBQ DB_ROLE OPEN_MODE"
      echo "-------------------------------------"
      echo $tdb
      echo $xdb
      echo "-------------------------------------"
   fi
   # version, platform and bittness compatibility
   # target & auxiliary oracle version and o/s must be the same
   # if bit differ, then must compile auxiliary pl/sql
   # @${ORACLE_HOME}/rdbms/admin/utlirp

   dup_fgtime;TE=$TM
   dup_felaps $TS $TE
   echo "$( date +"%y-%m-%d %T") ... Completed ($ELT)"
}

#------------------------------------------------------------------------------
#
# function ResetListener - reset to local listener 
#
#------------------------------------------------------------------------------
ResetListener () 
{
   dup_fheadr "$FUNCNAME"
   dup_fgtime;TS=$TM

   msg="auxiliary Database stop [${LSNRN}]" 
   lsnrctl stop $LSNRN >> $LOGF
   [ $DEBUG ] && dup_fechos $msg

   msg="auxiliary Database restore listener file"
   if [ -f $XLSBKP ]; then
       cp $XLSBKP $XLSORA 
       [ $DEBUG ] && dup_fechos $msg
   else
       rm -f $XLSORA
   fi

   if [[ "$XRAC" == "TRUE" ]]; then
         msg="auxiliary reset local_listener in RAC"
         SQL="ALTER SYSTEM reset local_listener scope=both sid='*'"
         ddlSQL "$XBQDBA" "$SQL"
         [ $DEBUG ] && dup_fechos $msg
   elif [[ "$DBOP" == "g" ]]; then
         cat >> $XLSORA << EOF
# Generated by $PN configuration tools. (re-set)
${LSNRN} =
  (DESCRIPTION_LIST =
    (DESCRIPTION =
      (ADDRESS = (PROTOCOL = TCP)(HOST = ${XHOST})(PORT = ${SPORT}))
    )
  )
SID_LIST_${LSNRN} =
  (SID_LIST =
    (SID_DESC =
      (GLOBAL_DBNAME = ${XDBUN}_DGMGRL${DOM})
      (ORACLE_HOME = ${ORACLE_HOME})
      (SID_NAME = ${XDBUN})
    )
  )
EOF
         msg="auxiliary Database start [${LSNRN}]" 
         echo "start ${LSNRN}" >> $LOGF
         lsnrctl start ${LSNRN}>> $LOGF
         [ $? -eq 0 ] || dup_fatalf $? [$LINENO] $msg

         # Warn user to set dgmgrl static listener on the primary site 
         if [[ "$XHOST" != "$THOST" ]]; then 
            if [[ $DGEXIST -eq 0 && "$TRAC" != "TRUE" ]]; then
               cat >> $LOGF << EOF
Warning! To complete the setup,

1) append/modify the static listener entry below to the Target database 
   "$TDBUN" listener, 
2) and restart the listener.

SID_LIST_LISTENER =
  (SID_LIST =
    (SID_DESC =
      (GLOBAL_DBNAME = ${TDBUN}_DGMGRL${DOM})
      (ORACLE_HOME = ${TARGET_OH})
      (SID_NAME = ${TDBUN})
    )
  )
EOF
               tail -13 $LOGF
            fi
         fi 
 
         msg="auxiliary database set local_listener"
         LOC_LISTX="(ADDRESS=(PROTOCOL=TCP)(HOST=${XHOST})(PORT=${SPORT}))"
         SQL="ALTER SYSTEM SET local_listener='${LOC_LISTX}' SCOPE=BOTH"
         ddlSQL "$XBQDBA" "$SQL" || dup_fatalf $? [$LINENO] $msg
   elif [ $PORT_1521 != true ]; then
         if [ ! -f $XLSORA ]; then
            cat >> $XLSORA << EOF
# Generated by $PN configuration tools. (re-set)
LISTENER =
  (DESCRIPTION_LIST =
    (DESCRIPTION =
      (ADDRESS = (PROTOCOL = TCP)(HOST = ${XHOST})(PORT = 1521))
    )
  )
EOF
            msg="auxiliary Database start listener" 
            echo "start listener" >> $LOGF
            lsnrctl start >> $LOGF
            [ $? -eq 0 ] || dup_fatalf $? [$LINENO] $msg
            [ $DEBUG ] && dup_fechos $msg

            SQL="ALTER SYSTEM RESET local_listener SCOPE=BOTH"
            ddlSQL "$XBQDBA" "$SQL" || dup_fatalf $? [$LINENO] $msg
            [ $DEBUG ] && dup_fechos $SQL
         fi
   else
         SQL="ALTER SYSTEM RESET local_listener SCOPE=BOTH"
         ddlSQL "$XBQDBA" "$SQL" || dup_fatalf $? [$LINENO] $msg
         [ $DEBUG ] && dup_fechos $SQL
   fi

   msg="auxiliary Database Register the listener"
   SQL="ALTER SYSTEM REGISTER"
   ddlSQL "$XBQDBA" "$SQL" || dup_fatalf $? [$LINENO] $msg
   [ $DEBUG ] && dup_fechos $msg

   dup_fgtime;TE=$TM
   dup_felaps $TS $TE
   echo "$( date +"%y-%m-%d %T") ... Completed ($ELT)"
}

#------------------------------------------------------------------------------
#
# function ClusterDB - add auxiliary Database resource 
#
#------------------------------------------------------------------------------
ClusterDB () 
{
   [[ "$XRAC" == "TRUE" ]] || return

   dup_fheadr "$FUNCNAME"
   dup_fgtime;TS=$TM

   if [ $XPOLICY ]; then
      msg="auxiliary add srvpool [$XCRSPOOL]"
      srvctl add srvpool -g $XCRSPOOL -l $XMAX_INST \
       -u $XMAX_INST -n "$XNODEC" >> $LOGF || \
       dup_fatalf $? [$LINENO] $msg
       [ $DEBUG ] && dup_fechos $msg
   fi

   # Policy managed database (created in checks)
   [ $XPOLICY ] && FLAG1="-serverpool $XCRSPOOL" || FLAG1=""

   # ASM
   [ $XASM ] && FLAG2="-diskgroup ${XDGD},${XDGR}" || FLAG2=""
   
   # Enable cluster_database
   msg="auxiliary Database Enable cluster_database"
   SQL="ALTER SYSTEM SET CLUSTER_DATABASE=TRUE SCOPE=SPFILE SID='*'"
   ddlSQL "$XBQDBA" "$SQL" || dup_fatalf $? [$LINENO] $msg
   [ $DEBUG ] && dup_fechos $msg

   # shutdown the database
   msg="auxiliary Database shutdown manual"
   SQL="SHUTDOWN IMMEDIATE"
   ddlSQL "$XBQDBA" "$SQL" || dup_fatalf $? [$LINENO] $msg
   [ $DEBUG ] && dup_fechos $msg

   # create instance's init.ora
   rm ${AUX_BASE}/dbs/init${XDBUN}.ora >/dev/null 2>&1

   msg="auxiliary add database [$XDBUN]"
   if [[ "$DBOP" == "g" ]]; then
      srvctl add database -db $XDBUN -dbname $dbname -oraclehome $ORACLE_HOME \
      -spfile $AUXSPF -domain $domain $FLAG1 $FLAG2 \
      -dbtype RAC -role PHYSICAL_STANDBY -startoption "READ ONLY" \
      >> $LOGF ||  dup_fatalf $? [$LINENO] $msg
   else
       srvctl add database -d $XDBUN -n $dbname -o $ORACLE_HOME \
       -p $AUXSPF -m $domain $FLAG1 $FLAG2 \
       >> $LOGF ||  dup_fatalf $? [$LINENO] $msg
   fi
   [ $DEBUG ] && dup_fechos $msg

   if [[ ! $XCRSPOOL ]]; then
      msg="auxiliary Database Admin Managed add instances"
      j=1
      for k in $XNODEB
      do
         inst="${XDBUN}_${j}"
         srvctl add instance -db $XDBUN -instance $inst -node $k >> $LOGF || \
             dup_fatalf $? [$LINENO] $msg
        (( j++ ))
      done
      [ $DEBUG ] && dup_fechos $msg
   fi

   ctns=$(srvctl getenv listener | grep TNS_ADMIN)
   RTNS=${ctns#*=}

   msg="server control setenv database [$XDBUN] TNS_ADMIN is [$TNS_ADMIN]"
   srvctl setenv database -d $XDBUN -envs \"TNS_ADMIN=$TNS_ADMIN=\" >> $LOGF
   [ $DEBUG ] && dup_fechos $msg

   msg="server control setenv database [$XDBUN] TZ is [$TZ]"
   srvctl setenv database -d $XDBUN -t \"TZ=$TZ\" >> $LOGF 
   [ $DEBUG ] && dup_fechos $msg

   msg1="warning! auxiliary remote pasword failed"
   msg2="auxiliary remote pasword successfull"
   orapwd file=$XDATA dbuniquename=$XDBUN input_file=$AUXOPW ||\
   dup_fechos $msg1
   [ $DEBUG ] && dup_fechos $msg2

   msg="auxiliary start auxiliary Database"
   [ $DEBUG ] && dup_fechos $msg
   srvctl start database -d $XDBUN >> $LOGF || dup_fatalf $? [$LINENO] $msg 

   msg="auxiliary status database [$XDBUN]"
   srvctl status database -d $XDBUN >> $LOGF || dup_fatalf $? [$LINENO] $msg
   [ $DEBUG ] && dup_fechos $msg

   msg="auxiliary config database [$XDBUN]"
   srvctl config database -d $XDBUN >> $LOGF || dup_fatalf $? [$LINENO] $msg
   [ $DEBUG ] && dup_fechos $msg

   dup_fgtime;TE=$TM
   dup_felaps $TS $TE
   echo "$( date +"%y-%m-%d %T") ... Completed ($ELT)"
}

#------------------------------------------------------------------------------
#
# function dgBroker - data guard broker
#
#------------------------------------------------------------------------------
dgBroker ()
{
   [[ "$DBOP" == "g" ]] || return

   dup_fheadr "$FUNCNAME"
   dup_fgtime;TS=$TM

   # Data Guard configuration name
   MGCFG="${dbname}cfg"

   msg="target Database as sysdg remote connection test [$DPEZLOG]"
   [ $DEBUG ] && dup_fechos $msg
   SQL="SELECT 1 FROM DUAL"
   ddlSQL "$DPEZLOG as sysdg" "$SQL" || dup_fatalf $? [$LINENO] $msg

   DXEZCON="sys/${syspass}@${DXCID} $AS_SYSDBA"
   msg="aux Database remote connection test [$DXEZCON]"
   [ $DEBUG ] && dup_fechos $msg
   SQL="SELECT 1 FROM DUAL"
   ddlSQL "$DXEZCON" "$SQL" || dup_fatalf $? [$LINENO] $msg

   # Test remote connection to auxiliary
   msg="auxiliary Database remote connection test [$DXEZLOG as sysdg]"
   [ $DEBUG ] && dup_fechos $msg
   SQL="SELECT 1 FROM DUAL"
   for (( i=1; i< 3; i=i+1 ))
   do
      xflag=1
      ddlSQL "$DXEZLOG as sysdg" "$SQL" && { xflag=0; break; }
      sleep 30
   done
   [ $xflag -eq 0 ] || dup_fatalf $? [$LINENO] $msg

   # Test remote connection to target w/dgmgrl
   msg="dgmgrl target Database remote connection test"
   DGCMD="EXIT"
   echo $DGCMD >> $LOGF
   dgmgrl "$DPEZLOG" "$DGCMD" >> $LOGF
   [ $? -eq 0 ] || dup_fatalf $? [$LINENO] $msg
   [ $DEBUG ] && dup_fechos $msg

   #
   # check for existing cfg
   #
   msg1="The Oracle Data Guard broker is not available yet."
   msg2="The Oracle Data Guard broker is available."
   DGCMD="SHOW CONFIGURATION"
   echo $DGCMD >> $LOGF
   dgmgrl "$DPEZLOG" "$DGCMD" >> $LOGF
   if [[ $? -eq 0 ]]; then
       DGCFG=true
       [ $DEBUG ] && dup_fechos $msg2
   else
       [ $DEBUG ] && dup_fechos $msg1
   fi

   if  [ ! $DGCFG ]; then
       # CREATE broker CONFIGURATION
       DGCMD="CREATE CONFIGURATION ${MGCFG} AS PRIMARY DATABASE IS ${TDBUN} 
              CONNECT IDENTIFIER IS \"$DTCID\""
       msg="dgmgrl [$DGCMD]"
       [ $DEBUG ] && dup_fechos $msg
       dgmgrl "$DPEZLOG" "$DGCMD" >> $LOGF
       [ $? -eq 0 ] || dup_fatalf $? [$LINENO] $msg

       # Tracing manually enabled
       if [ $DEBUG ]; then
           msg="dgmgrl TRACELEVEL=SUPPORT"
           DGCMD="EDIT CONFIGURATION SET PROPERTY TRACELEVEL=SUPPORT"
           echo $DGCMD >> $LOGF
           dgmgrl "$DPEZLOG" "$DGCMD" >> $LOGF
           [ $? -eq 0 ] || dup_fatalf $? [$LINENO] $msg
           dup_fechos $msg
       fi
   fi
 
   #
   # Add standby
   #
   msg="dgmgrl the standby database [ $XDBUN ] already in cfg"
   DGCMD="SHOW DATABASE ${XDBUN}"
   echo $DGCMD >> $LOGF
   dgmgrl "$DPEZLOG" "$DGCMD" >> $LOGF

   if [ $? -ne 0 ]; then
       DGCMD="ADD DATABASE ${XDBUN} AS CONNECT IDENTIFIER IS \"${DXCID}\""
       msg="dgmgrl [$DGCMD]"
       dgmgrl "$DPEZLOG" "$DGCMD" >> $LOGF || dup_fatalf $? [$LINENO] $msg
       [ $DEBUG ] && dup_fechos $msg
   fi
 
   #
   #  managed standby recovery
   #
   msg="dgmgrl enable configuration"
   DGCMD="ENABLE CONFIGURATION"
   echo $DGCMD >> $LOGF
   dgmgrl "$DPEZLOG" "$DGCMD" >> $LOGF || dup_fatalf $? [$LINENO] $msg
   [ $DEBUG ] && dup_fechos $msg

   DGCMD="SHOW CONFIGURATION"
   for (( i=0; i<30; i=i+1 ))
   do
      status=$(dgmgrl "$DPEZLOG" "$DGCMD" | grep status | awk '{print $1}')
      case $status in
           SUCCESS) break ;;
                 *) sleep 10 ;;
      esac
   done

   # FSFO - Setup observer if not available
   if [[ "$FAILOVER" == "true" ]]; then
      DGCMD="SHOW CONFIGURATION"
      OBSRV=$(dgmgrl "$DPEZLOG" "$DGCMD" | \
              grep "Fast-Start Failover" | awk '{print $3}')
      [ $DEBUG ] && dup_fechos "Fast-Start Failover: $OBSRV"

      if [[ "$OBSRV" != "Enabled" ]]; then
         # Observer name
         ON="${XDBUN}_obsrv"
         # Wallet directory
         WD="${AUX_BASE}/admin/wallets"
         # expect script
         WS="${WD}/mkw.sh"

         msg="Setup wallet and observer on FSFO "
         [ $DEBUG ] && dup_fechos $msg
      
         # Create wallet if not exists already
         [ -d $WD ] && rm -rf $WD >/dev/null
         # create dir
         msg="Aux create wallet dir"
         mkdir -p $WD >/dev/null
         chmod 700 $WD
         [ $? -eq 0 ] || dup_fatalf $? [$LINENO] $msg
         [ $DEBUG ] && dup_fechos $msg

         # Append sqlnet.ora
         cat >> $TNS_ADMIN/sqlnet.ora << EOF 
WALLET_LOCATION = (SOURCE = (METHOD = FILE) (METHOD_DATA = (DIRECTORY = $WD))) 
SQLNET.WALLET_OVERRIDE = TRUE
EOF

         msg="Aux create wallet"
         cat > $WS <<EOF
set timeout 20
spawn mkstore -wrl $WD -create
expect "Enter password:   " { send "${syspass}\r" }
expect "Enter password again:   " { send "${syspass}\r" }
expect EOF
EOF
         expect ${WS} > /dev/null 2>&1
         [ $? -eq 0 ] || dup_fatalf $? [$LINENO] $msg
         [ $DEBUG ] && dup_fechos $msg

         msg="Target create credentials"
         cat > $WS <<EOF
set timeout 20
spawn mkstore -wrl $WD -createCredential $DTCID sys ${syspass}
expect "Enter wallet password:   " { send "${syspass}\r" }
expect EOF
EOF
         expect ${WS} > /dev/null 2>&1
         [ $? -eq 0 ] || dup_fatalf $? [$LINENO] $msg
         [ $DEBUG ] && dup_fechos $msg

         msg="Aux create credentials"
         cat > $WS <<EOF
set timeout 20
spawn mkstore -wrl $WD -createCredential $DXCID sys ${syspass}
expect "Enter wallet password:   " { send "${syspass}\r" }
expect EOF
EOF
         expect ${WS} > /dev/null 2>&1
         [ $? -eq 0 ] || dup_fatalf $? [$LINENO] $msg
         [ $DEBUG ] && dup_fechos $msg

         msg="Aux list wallets"
         cat > $WS <<EOF
set timeout 20
spawn mkstore -wrl $WD -listCredential
expect "Enter wallet password:   " { send "${syspass}\r" }
expect EOF
EOF
         expect ${WS} > /dev/null 2>&1
         [ $? -eq 0 ] || dup_fatalf $? [$LINENO] $msg
         [ $DEBUG ] && dup_fechos $msg
         rm -f ${WS} >/dev/null 2>&1

         msg="target sqlplus test connection w/wallet [/@${DTCID} $AS_SYSDBA]"
         SQL="SELECT 1 FROM DUAL"
         ddlSQL "/@${DTCID} $AS_SYSDBA" "$SQL" || dup_fatalf $? [$LINENO] $msg
         [ $DEBUG ] && dup_fechos $msg

         msg="Aux sqlplus test connection w/wallet [/@${DXCID} $AS_SYSDBA]"
         SQL="SELECT 1 FROM DUAL"
         ddlSQL "/@${DXCID} $AS_SYSDBA" "$SQL" || dup_fatalf $? [$LINENO] $msg
         [ $DEBUG ] && dup_fechos $msg

         DGCMD="connect /@${DTCID}"
         msg="target dgmgrl connection test w/wallet [$DGCMD]"
         dgmgrl "/" "$DGCMD" >> $LOGF
         [ $? -eq 0 ] || dup_fatalf $? [$LINENO] $msg
         [ $DEBUG ] && dup_fechos $msg

         DGCMD="connect /@${DXCID}"
         msg="aux dgmgrl connection test w/wallet [$DGCMD]"
         dgmgrl "/" "$DGCMD" >> $LOGF
         [ $? -eq 0 ] || dup_fatalf $? [$LINENO] $msg
         [ $DEBUG ] && dup_fechos $msg

         cd $WD >/dev/null
         msg="start observer on aux in B/G"
         DGCMD="start observer $ON in background file is ${ON}.dat 
                logfile is ${ON}.log connect identifier is '${DTCID}'"
         dgmgrl "/@${DTCID}" "$DGCMD" 
         [ $? -eq 0 ] || dup_fatalf $? [$LINENO] $msg
         [ $DEBUG ] && dup_fechos $msg

         DGCMD="ENABLE FAST_START FAILOVER"
         dgmgrl "/@${DTCID}" "$DGCMD" >> $LOGF
         [ $? -eq 0 ] || dup_fatalf $? [$LINENO] $DGCMD
         [ $DEBUG ] && dup_fechos $DGCMD
      fi
   fi

   # Final status
   DGCMD="SHOW CONFIGURATION"
   [ $DEBUG ] && dgmgrl "$DPEZLOG" "$DGCMD" | awk '/^Configuration/,EOF'

   bst=$(dgmgrl "$DPEZLOG" "$DGCMD" | grep status | awk '{print $1}')
   msg="Broker configuration is ${bst}"
   [[ "$status" == "ERROR" ]] && dup_fatalf $? [$LINENO] $msg
   [ $DEBUG ] && dup_fechos $msg 

   dup_fgtime;TE=$TM
   dup_felaps $TS $TE
   echo "$( date +"%y-%m-%d %T") ... Completed ($ELT)"

}


#------------------------------------------------------------------------------
#
# main() 
#
#------------------------------------------------------------------------------
# {

   # trap keyboard interrupt (control-c)
   trap dup_fatalf SIGINT

   dup_fgtime
   prog_start=$TM


   # Process user command line arg input
   XDBUN=
   DBOP=g
   XNODE=`hostname`
   TEZCON=
   XPOLICY=true
   FSFO=false
   
   while getopts "hvcgaod:n:s:r:l:" OPTION
   do
     case $OPTION in
           v) DEBUG=$OPTION ;;
         c|g) DBOP=$OPTION ;;
           d) XDBUN=$OPTARG ;;
           n) XNODE="$OPTARG" ;;
           s) XDATA=$OPTARG ;;
           r) XFRA=$OPTARG ;;
           l) TEZCON=$OPTARG ;;
           a) XPOLICY= ;;
           o) FSFO=true ;;
         h|?) dup_fghelp ;;
     esac
   done
   shift $((OPTIND-1))

   [[ $OPTIND -eq 1 ]] && dup_fghelp
   [[ $TEZCON ]] || dup_fatalf $? [$LINENO] "source EZConnect login is null" 
   [[ "$DBOP" == "c" && "$FSFO" == "true" ]] && \
       dup_fatalf $? [$LINENO] "fsfo is data guard feature"

   # Aux Bequeth
   XBQDBA='/ AS SYSDBA' 
   TEZDBA="$TEZCON $AS_SYSDBA" 

   # Aux
   # Derive the default name for the auxilliary database
   if [[ ! $XDBUN ]]; then
      ORACLE_SID=${ORACLE_SID:?"Please set env parameter ORACLE_SID first... "}
      XDBUN="$ORACLE_SID"
   fi
   export ORACLE_SID=$XDBUN

   # Post inp arg
   # env settings
   ORACLE_BASE=${ORACLE_BASE:?"Please set env parameter ORACLE_BASE first... "}
   ORACLE_HOME=${ORACLE_HOME:?"Please set env parameter ORACLE_HOME first... "}
   TNS_ADMIN=${TNS_ADMIN:?"Please set env parameter TNS_ADMIN first... "}

   # program & log identifier
   LOGF="${PN}.${XDBUN}.log"
   [ -f $LOGF ] && mv $LOGF ${LOGF}_$$

   # header output
   [[ "$DBOP" != "g" ]] && xmode="Database Clone"  || \
      xmode="Broker managed Active Physical standby database"
   echo "${PN} $VER   [ $xmode ]" | tee -a $LOGF
   echo "$( date +"%y-%m-%d %T") main() ..." | tee -a $LOGF

   # Core functions calls
   #
   PreProcessing
   ConnectParse 
   SanityCheck
   TargetDB
   StaticListener
   AuxiliaryDB   
   RmanDuplicate
   ResetListener
   ClusterDB 
   dgBroker 

   dup_fgtime;prog_stop=$TM
   dup_felaps $prog_start $prog_stop
   echo "$( date +"%y-%m-%d %T") Successfully completed (Total Elapsed: $ELT)"
   echo "Log: $LOGF"

# } end of main()
