# syntax=docker/dockerfile:1

################################################################################
# Create a stage for resolving and downloading dependencies.
FROM eclipse-temurin:21-jdk-jammy as deps

# Install the iputils-ping package
RUN apt-get update && apt-get install -y iputils-ping

WORKDIR /build
RUN mkdir -p /build/logs
RUN mkdir -p /build/logstash/config/
RUN mkdir -p /build/logstash/pipeline/
RUN mkdir -p /build/access-refresh-token-keys
COPY src/main/resources/logback-spring.xml /build/logback-spring.xml
COPY logstash/config/logstash.yml /build/logstash/config/logstash.yml
COPY logstash/pipeline/logstash.conf /build/logstash/pipeline/logstash.conf
RUN chmod 644 /build/logstash/config/logstash.yml
RUN chmod 644 /build/logstash/pipeline/logstash.conf

# Copy the mvnw wrapper with executable permissions.
COPY --chmod=0755 mvnw mvnw
COPY .mvn/ .mvn/

# Download dependencies as a separate step to take advantage of Docker's caching.
RUN --mount=type=bind,source=pom.xml,target=pom.xml \
    --mount=type=cache,target=/root/.m2 ./mvnw dependency:go-offline -DskipTests

################################################################################
# Create a stage for building the application based on the stage with downloaded dependencies.
FROM deps as package

WORKDIR /build

COPY ./src src/
RUN --mount=type=bind,source=pom.xml,target=pom.xml \
    --mount=type=cache,target=/root/.m2 \
    ./mvnw package -DskipTests && \
    mv target/$(./mvnw help:evaluate -Dexpression=project.artifactId -q -DforceStdout)-$(./mvnw help:evaluate -Dexpression=project.version -q -DforceStdout).jar target/app.jar

################################################################################
# Create a stage for extracting the application into separate layers.
FROM package as extract

WORKDIR /build

RUN java -Djarmode=layertools -jar target/app.jar extract --destination target/extracted

################################################################################
# Create a new stage for running the application that contains the minimal runtime dependencies.
FROM eclipse-temurin:21-jre-jammy AS final

# Install MySQL client.
RUN apt-get update && \
    apt-get install -y mysql-client && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# Create a non-privileged user that the app will run under.
ARG UID=10001
RUN adduser \
    --disabled-password \
    --gecos "" \
    --home "/nonexistent" \
    --shell "/sbin/nologin" \
    --no-create-home \
    --uid "${UID}" \
    appuser

USER appuser

# Copy the executable from the "extract" stage.
COPY --from=extract build/target/extracted/dependencies/ ./
COPY --from=extract build/target/extracted/spring-boot-loader/ ./
COPY --from=extract build/target/extracted/snapshot-dependencies/ ./
COPY --from=extract build/target/extracted/application/ ./

# Switch to root to copy and set permissions on the script.
USER root

## Copy the `set-env.sh` script into the container.
#COPY set-env.sh /build/set-env.sh
#
## Make the script executable.
#RUN chmod +x /build/set-env.sh

# Switch back to the non-root user if needed.
# USER appuser

EXPOSE 8082

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
