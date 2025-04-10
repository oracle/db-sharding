#!/bin/bash
#
# Since: November, 2018
# Author: paramdeep.saini@oracle.com
# Description: Build script for building RAC container image
#
# DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
#
# Copyright (c) 2014,2024 Oracle and/or its affiliates.
#

usage() {
  cat << EOF

Usage: buildContainerImage.sh -v [version] -t [image_name:tag] [-e | -s] [-i] [-o] [container build option]
It builds a container image for a DNS server

Parameters:
   -v: version to build
   -i: ignores the MD5 checksums
   -t: user defined image name and tag (e.g., image_name:ta
   -o: passes on container build option (e.g., --build-arg SLIMMIMG=true for slim)

LICENSE UPL 1.0

Copyright (c) 2014,2024 Oracle and/or its affiliates.

EOF
  exit 0
}

# Validate packages
checksumPackages() {
  if hash md5sum 2>/dev/null; then
    echo "Checking if required packages are present and valid..."
    md5sum -c Checksum
    # shellcheck disable=SC2181
    if [ "$?" -ne 0 ]; then
      echo "MD5 for required packages to build this image did not match!"
      echo "Make sure to download missing files in folder $VERSION."
      # shellcheck disable=SC2320
      exit $?
    fi
  else
    echo "Ignored MD5 sum, 'md5sum' command not available.";
  fi
}

##############
#### MAIN ####
##############

if [ "$#" -eq 0 ]; then
  usage;
fi

# Parameters
VERSION="12.2.0.1"
SKIPMD5=0
DOCKEROPS=""
IMAGE_NAME=""
SLIM="false"
DOCKEROPS=" --build-arg SLIMMING=false"
while getopts "hiv:o:t:" optname; do
  case "$optname" in
    "h")
      usage
      ;;
    "i")
      SKIPMD5=1
      ;;
    "v")
      VERSION="$OPTARG"
      ;;
    "o")
      DOCKEROPS="$OPTARG"
      if [[ "$DOCKEROPS" != *"--build-arg SLIMMING="* ]]; then
         DOCKEROPS+=" --build-arg SLIMMING=false"
         SLIM="false"
      fi
      if [[ "$OPTARG" == *"--build-arg SLIMMING=true"* ]]; then
        SLIM="true"
      fi
     ;;
    "t")
      IMAGE_NAME="$OPTARG"
     ;;

    "?")
      usage;
      ;;
    *)
    # Should not occur
      echo "Unknown error while processing options inside buildContainerImage.sh"
      ;;
  esac
done

# Oracle Database Image Name
if [ "${IMAGE_NAME}"x = "x" ] && [ "${SLIM}" == "true" ]; then
   IMAGE_NAME="oracle/gsm:${VERSION}-slim"
elif [ "${IMAGE_NAME}"x = "x" ] && [ "${SLIM}" == "false" ]; then
   IMAGE_NAME="oracle/gsm:${VERSION}"
else
   echo "Image name is passed as an variable"
fi

 echo "Container Image set to : ${IMAGE_NAME}"

# Go into version folder
# cd "$VERSION" || exit

if [ ! "$SKIPMD5" -eq 1 ]; then
  checksumPackages
else
  echo "Ignored MD5 checksum."
fi
echo "=========================="
echo "DOCKER info:"
docker info
echo "=========================="

# Proxy settings
PROXY_SETTINGS=""
# shellcheck disable=SC2154
if [ "${http_proxy}" != "" ]; then
  PROXY_SETTINGS="$PROXY_SETTINGS --build-arg http_proxy=${http_proxy}"
fi
# shellcheck disable=SC2154
if [ "${https_proxy}" != "" ]; then
  PROXY_SETTINGS="$PROXY_SETTINGS --build-arg https_proxy=${https_proxy}"
fi
# shellcheck disable=SC2154
if [ "${ftp_proxy}" != "" ]; then
  PROXY_SETTINGS="$PROXY_SETTINGS --build-arg ftp_proxy=${ftp_proxy}"
fi
# shellcheck disable=SC2154
if [ "${no_proxy}" != "" ]; then
  PROXY_SETTINGS="$PROXY_SETTINGS --build-arg no_proxy=${no_proxy}"
fi
# shellcheck disable=SC2154
if [ "$PROXY_SETTINGS" != "" ]; then
  echo "Proxy settings were found and will be used during the build."
fi

# ################## #
# BUILDING THE IMAGE #
# ################## #
echo "Building image '$IMAGE_NAME' ..."

# BUILD THE IMAGE (replace all environment variables)
BUILD_START=$(date '+%s')
# shellcheck disable=SC2086
docker build --force-rm=true --no-cache=true ${DOCKEROPS} ${PROXY_SETTINGS} --build-arg VERSION="${VERSION}" -t ${IMAGE_NAME} -f "${VERSION}"/Containerfile . || {
  echo "There was an error building the image."
  exit 1
}
BUILD_END=$(date '+%s')
# shellcheck disable=SC2154,SC2003
BUILD_ELAPSED=$((BUILD_END - BUILD_START))

echo ""
# shellcheck disable=SC2181,SC2320
if [ $? -eq 0 ]; then
cat << EOF
  Oracle GSM Container Image for version $VERSION is ready to be extended:

    --> $IMAGE_NAME

  Build completed in $BUILD_ELAPSED seconds.

EOF

else
  echo "Oracle GSM Container Image was NOT successfully created. Check the output and correct any reported problems with the docker build operation."
fi
