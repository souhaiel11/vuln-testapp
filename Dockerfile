# ============================================================================
# GROUPE D — DOCKERFILE POUR WF5 (Dockerfile Optimizer)
# ============================================================================
# Concu pour declencher EXACTEMENT les 4 IDs deterministes que WF5 corrige
# (confirmes dans le code : DF-IMG-JDK, DF-IMG-PIN, DF-SEC-USER,
# DF-SEC-HEALTHCHECK).
# ============================================================================

# DF-IMG-JDK — image JDK obsolete/ancienne (WF5 doit proposer un bump).
# DF-IMG-PIN — tag non pinne / version glissante (WF5 doit pinner).
FROM openjdk:8-jdk

WORKDIR /app

COPY target/vuln-testapp-1.0.0.jar app.jar

EXPOSE 8080

# DF-SEC-USER — pas d'instruction USER (execution en root : WF5 doit ajouter
#               un utilisateur non-root).
# DF-SEC-HEALTHCHECK — pas de HEALTHCHECK (WF5 doit en ajouter un).

ENTRYPOINT ["java", "-jar", "app.jar"]
