# /*
# ** Oracle Sharding Tools Library
# **
# ** Copyright © 2019 Oracle and/or its affiliates. All rights reserved.
# ** Licensed under the Universal Permissive License v 1.0 as shown at 
# **   http://oss.oracle.com/licenses/upl 
# */

FROM tomcat:9.0-slim
VOLUME /tmp
ARG WAR_FILE
COPY ${WAR_FILE} /usr/local/tomcat/webapps/sdb-mid-tier-routing-services.war
RUN sh -c 'touch /usr/local/tomcat/webapps/sdb-mid-tier-routing-services.war'


# Define environment variables

ENV SPRING_CONFIG_LOCATION /usr/local/tomcat/conf/
ENV SPRING_CONFIG_NAME sdb-mid-tier-routing-services

ADD src/main/resources/sdb-mid-tier-routing-services.properties /usr/local/tomcat/conf/

ENTRYPOINT ["sh", "-c", "java -Djava.security.egd=file:/dev/./urandom -jar /usr/local/tomcat/webapps/sdb-mid-tier-routing-services.war"]
