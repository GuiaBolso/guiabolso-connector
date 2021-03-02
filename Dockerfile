# Builder
ARG GUIABOLSO_CONNECTOR_VERSION
ARG SHORT_COMMIT
ARG DATADOG_VERSION="0.49.0"

FROM gradle:5.4-jdk11 AS builder
ARG GUIABOLSO_CONNECTOR_VERSION
ARG DATADOG_VERSION

COPY ./ /home/gradle/project

WORKDIR /home/gradle/project

RUN gradle -Dorg.gradle.daemon=false build --stacktrace --info

RUN mkdir -p /home/gradle/project/build/distributions/app/

RUN wget -O /home/gradle/dd-java-agent.jar  "https://repository.sonatype.org/service/local/artifact/maven/redirect?r=central-proxy&g=com.datadoghq&a=dd-java-agent&v=$DATADOG_VERSION"

RUN unzip /home/gradle/project/application/build/distributions/application-${GUIABOLSO_CONNECTOR_VERSION}.zip -d /home/gradle/project/build/distributions/

RUN mv /home/gradle/project/build/distributions/application-${GUIABOLSO_CONNECTOR_VERSION}/*  /home/gradle/project/build/distributions/app

# Application
FROM openjdk:11.0.7-jre
ARG GUIABOLSO_CONNECTOR_VERSION
ARG SHORT_COMMIT

LABEL ref_commit=${SHORT_COMMIT} version="${GUIABOLSO_CONNECTOR_VERSION}" description="Guiabolso Connector" maintainer="Guiabolso Connect<suporteconnect@guiabolso.com.br>"

COPY --from=builder /home/gradle/project/build/distributions/app/ /opt/app/

COPY --from=builder /home/gradle/dd-java-agent.jar /opt/datadog/dd-java-agent.jar

RUN rm -rf /var/cache/*

EXPOSE 9000
CMD "/opt/app/bin/application"
