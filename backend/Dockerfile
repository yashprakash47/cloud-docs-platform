FROM eclipse-temurin:21-jdk-jammy AS build

WORKDIR /workspace

COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw
RUN ./mvnw -B -ntp dependency:go-offline

COPY src src
RUN ./mvnw -B -ntp -DskipTests package

FROM eclipse-temurin:21-jre-jammy AS runtime

ENV JAVA_OPTS="" \
    SERVER_PORT=8080 \
    CLOUDDOCS_STORAGE_PROVIDER=local \
    CLOUDDOCS_PROCESSING_MESSAGE_SOURCE=local

RUN groupadd --system --gid 10001 clouddocs \
    && useradd --system --uid 10001 --gid 10001 --create-home clouddocs \
    && mkdir -p /opt/clouddocs /tmp/clouddocs-files \
    && chown -R clouddocs:clouddocs /opt/clouddocs /tmp/clouddocs-files

RUN apt-get update \
    && apt-get install --no-install-recommends --yes curl \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /opt/clouddocs
COPY --from=build --chown=clouddocs:clouddocs /workspace/target/clouddocs-backend-*.jar /opt/clouddocs/app.jar

USER clouddocs
EXPOSE 8080

HEALTHCHECK --interval=10s --timeout=5s --start-period=30s --retries=6 \
    CMD curl --fail --silent http://127.0.0.1:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /opt/clouddocs/app.jar"]
