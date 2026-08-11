# VOLONTAIREMENT NON OPTIMISE — teste WF5 (Dockerfile Optimizer)
# DF-IMG-JDK       : image JDK obsolete
# DF-IMG-PIN       : tag non pinne (:latest)
# DF-SEC-USER      : pas de USER (execution en root)
# DF-SEC-HEALTHCHECK : pas de HEALTHCHECK

FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src/ src/
RUN mvn package -DskipTests

FROM eclipse-temurin:21-jre-jammy

WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
RUN addgroup --system appgroup && adduser --system --ingroup appgroup appuser
USER appuser
HEALTHCHECK --interval=30s --timeout=5s --retries=3 CMD curl -f http://localhost:8080/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]