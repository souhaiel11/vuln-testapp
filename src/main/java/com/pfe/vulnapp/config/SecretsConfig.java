package com.pfe.vulnapp.config;

/**
 * ============================================================================
 * GROUPE B — BUGS DETECTES MAIS PAS AUTO-CORRIGES (revue humaine)
 * ============================================================================
 * Les secrets en dur sont des findings SonarQube de severite BLOCKER.
 *
 * Point cle valide dans le code de WF2 : un BLOCKER SELECTIONNE le fichier
 * (il compte comme finding BLOCKER/CRITICAL) MAIS est EXPLICITEMENT EXCLU de
 * l'auto-fix (excludeAutoFixSeverities: ['BLOCKER']) -> "revue humaine
 * obligatoire". La plateforme doit donc DETECTER ces secrets (carte Sonar,
 * BLOCKER visible) mais NE PAS les corriger automatiquement dans la PR.
 *
 * C'est le comportement de securite ATTENDU : on ne laisse pas une IA patcher
 * un secret sans revue humaine. Test = verifier qu'ils apparaissent en
 * BLOCKER mais ne sont PAS dans le diff de la PR.
 * ============================================================================
 */
public class SecretsConfig {

    // BUG B5 — BLOCKER : mot de passe en dur (S2068).
    public static final String DB_PASSWORD = "SuperSecret123!";

    // BUG B6 — BLOCKER : cle API en dur.
    public static final String API_KEY = "AKIAIOSFODNN7EXAMPLE";

    // BUG B7 — BLOCKER : token en dur.
    public static final String ADMIN_TOKEN = "admin-token-do-not-share-42";

    private SecretsConfig() {
    }
}
