# VOLONTAIREMENT NON OPTIMISE — teste WF5 (Dockerfile Optimizer)
# DF-IMG-JDK       : image JDK obsolete
# DF-IMG-PIN       : tag non pinne (:latest)
# DF-SEC-USER      : pas de USER (execution en root)
# DF-SEC-HEALTHCHECK : pas de HEALTHCHECK

FROM openjdk:8-jdk

WORKDIR /app
COPY target/vuln-testapp-1.0.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
