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