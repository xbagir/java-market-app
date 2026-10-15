FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml .
COPY auth-server/pom.xml auth-server/pom.xml
COPY storefront/pom.xml storefront/pom.xml
COPY payment-service/pom.xml payment-service/pom.xml
COPY specs ./specs
RUN mvn -B -pl auth-server -am dependency:go-offline || for i in 1 2 3 4 5; do sleep 5; mvn -B -pl auth-server -am dependency:go-offline && break; done

COPY auth-server/src ./auth-server/src
RUN mvn -B -pl auth-server -am -DskipTests package || for i in 1 2 3 4 5; do sleep 5; mvn -B -pl auth-server -am -DskipTests package && break; done

FROM eclipse-temurin:21-jre
RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd -r app && useradd -r -g app app
WORKDIR /app
COPY --from=build --chown=app:app /app/auth-server/target/*.jar /app/auth-server.jar
USER app
EXPOSE 9000
ENV JAVA_OPTS=""
HEALTHCHECK --interval=10s --timeout=5s --start-period=60s --retries=5 \
  CMD curl -fsS http://localhost:9000/health > /dev/null || exit 1
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/auth-server.jar"]
