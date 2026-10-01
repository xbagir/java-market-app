FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml .
COPY storefront/pom.xml storefront/pom.xml
COPY payment-service/pom.xml payment-service/pom.xml
RUN mvn -B -pl storefront -am dependency:go-offline || for i in 1 2 3 4 5; do sleep 5; mvn -B -pl storefront -am dependency:go-offline && break; done

COPY storefront/src ./storefront/src
RUN mvn -B -pl storefront -am -DskipTests package || for i in 1 2 3 4 5; do sleep 5; mvn -B -pl storefront -am -DskipTests package && break; done

FROM eclipse-temurin:21-jre
RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd -r app && useradd -r -g app app
WORKDIR /app
COPY --from=build --chown=app:app /app/storefront/target/*.jar /app/storefront.jar
USER app
EXPOSE 8080
ENV JAVA_OPTS=""
HEALTHCHECK --interval=10s --timeout=5s --start-period=60s --retries=5 \
  CMD curl -fsS http://localhost:8080/ > /dev/null || exit 1
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/storefront.jar"]
