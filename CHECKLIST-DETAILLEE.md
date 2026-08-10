# Checklist de validation détaillée — vuln-testapp

> Test QA de la plateforme DevSecOps, capacité par capacité.
> Chaque ligne = une capacité **confirmée dans le code**. Coche selon ce que la plateforme affiche réellement.
> Une ligne ❌ = un vrai problème de plateforme (pas un problème de config, on a validé la spec avant).

---

## CAPACITÉ 1 — DÉTECTION (les 4 scanners)

### 1.1 SonarQube (code source)

| # | Finding attendu | Fichier | Sévérité | Détecté ? |
|---|-----------------|---------|----------|-----------|
| S1 | Hash MD5 (S4790) | CryptoService | CRITICAL | ☐ |
| S2 | Random prévisible (S2245) | CryptoService | CRITICAL | ☐ |
| S3 | Catch vide (S2486) | CryptoService | MAJOR | ☐ |
| S4 | Injection SQL (S3649) | DataService | CRITICAL | ☐ |
| S5 | Secret DB_PASSWORD (S2068) | SecretsConfig | **BLOCKER** | ☐ |
| S6 | Secret API_KEY | SecretsConfig | **BLOCKER** | ☐ |
| S7 | Secret ADMIN_TOKEN | SecretsConfig | **BLOCKER** | ☐ |
| S8 | Secret dans properties | application.properties | BLOCKER | ☐ |

→ **Carte SonarQube affiche des chiffres réels (pas « non exécuté ») : ☐**

### 1.2 OWASP Dependency-Check (dépendances)

| # | Dépendance | CVE | Détecté ? |
|---|-----------|-----|-----------|
| O1 | log4j-core 2.14.1 | CVE-2021-44228 (CRITIQUE) | ☐ |
| O2 | jackson-databind 2.9.8 | Désérialisation | ☐ |
| O3 | snakeyaml 1.29 | CVE-2022-1471 | ☐ |
| O4 | commons-text 1.9 | CVE-2022-42889 | ☐ |

→ **Carte OWASP affiche des chiffres réels : ☐**

### 1.3 Trivy (image Docker)

| # | Cible | Attendu | Détecté ? |
|---|-------|---------|-----------|
| T1 | Image openjdk:8 | CVE système (OpenSSL, glibc...) | ☐ |
| T2 | Log4Shell dans le jar | CVE-2021-44228 | ☐ |

→ **Carte Trivy affiche des chiffres réels : ☐**

### 1.4 ZAP (application en marche)

> ⚠️ L'appli doit **démarrer** (H2 en mémoire). Vérifie `GET /api/health` → `OK` avant de compter sur ZAP.

| # | Faille | Endpoint | Détecté ? |
|---|--------|----------|-----------|
| Z1 | XSS réfléchi | `/api/greet?name=` | ☐ |
| Z2 | Injection SQL | `/api/user?username=` | ☐ |
| Z3 | Divulgation d'info | `/api/debug/info` | ☐ |
| Z4 | Headers de sécurité manquants | toutes réponses | ☐ |
| Z5 | Console H2 exposée | `/h2-console` | ☐ |

→ **Carte ZAP affiche des chiffres réels : ☐**

---

## CAPACITÉ 2 — ANALYSE (agents IA / WF1)

| # | Attendu | Vérifié ? |
|---|---------|-----------|
| A1 | Un incident est créé après le build | ☐ |
| A2 | L'agent Root Cause produit une analyse non vide | ☐ |
| A3 | L'agent Security Risk produit une analyse | ☐ |
| A4 | L'agent Remediation produit une proposition | ☐ |
| A5 | L'agent Developer Guidance produit un guide | ☐ |
| A6 | Le Judge produit une décision (`judgeDecision` persisté) | ☐ |
| A7 | Les cartes agents s'affichent dans l'onglet « Rapport IA » | ☐ |

---

## CAPACITÉ 3 — DÉCISION (score + Judge fail-closed)

| # | Attendu | Vérifié ? |
|---|---------|-----------|
| C1 | Un score de sécurité réel est calculé | ☐ |
| C2 | Vu la CVE critique (Log4Shell) → niveau **CRITICAL** (pas « healthy ») | ☐ |
| C3 | Le Judge décide **BLOCK** (CVE critique) | ☐ |
| C4 | Le score du header (anneau) = vrai `Report.securityScore` | ☐ |
| C5 | KPI « Déploiement » affiche **BLOQUÉ** (rouge) | ☐ |
| C6 | Bloc « Déployer sur Azure » refuse (fail-closed) avec raisons | ☐ |
| C7 | KPI et bloc Azure disent la même chose (cohérence) | ☐ |

---

## CAPACITÉ 4 — CORRECTION (les 3 workflows)

### 4.1 WF2 — Correction applicative par PR ⭐ (le test clé)

| # | Attendu | Vérifié ? |
|---|---------|-----------|
| W1 | WF2 sélectionne `CryptoService.java` (le fichier le plus problématique) | ☐ |
| W2 | Une **vraie PR GitHub** est créée (vérifiable dans le repo) | ☐ |
| W3 | La PR corrige MD5 → SHA-256 | ☐ |
| W4 | La PR corrige Random → SecureRandom | ☐ |
| W5 | La PR corrige le catch vide (log de l'exception) | ☐ |
| W6 | Le lien « Voir la PR de correction générée » apparaît dans l'incident | ☐ |
| W7 | ⭐ Les **secrets BLOCKER** (SecretsConfig) **NE sont PAS** dans le diff de la PR (revue humaine) | ☐ |
| W8 | Les CVE de dépendances (Log4Shell) → **commentaire TODO**, pas de bump | ☐ |

> W7 est le test le plus subtil : il prouve que la plateforme **détecte** le secret mais **refuse de le patcher automatiquement** — comportement de sécurité voulu.

### 4.2 WF4 — Jenkinsfile Optimizer

> Soumettre `Jenkinsfile.demo` via le bouton « Optimiser le Jenkinsfile ».

| # | Attendu | Vérifié ? |
|---|---------|-----------|
| J1 | WF4 détecte l'absence de `timeout()` | ☐ |
| J2 | WF4 détecte l'absence de `retry()` / `cleanWs()` | ☐ |
| J3 | WF4 détecte les credentials en clair | ☐ |
| J4 | WF4 génère une PR d'optimisation du Jenkinsfile | ☐ |

### 4.3 WF5 — Dockerfile Optimizer

| # | Attendu (ID déterministe) | Vérifié ? |
|---|---------------------------|-----------|
| K1 | DF-IMG-JDK : image JDK obsolète détectée | ☐ |
| K2 | DF-IMG-PIN : tag non pinné détecté | ☐ |
| K3 | DF-SEC-USER : absence de USER détectée | ☐ |
| K4 | DF-SEC-HEALTHCHECK : absence de HEALTHCHECK détectée | ☐ |
| K5 | WF5 génère une PR de correction du Dockerfile | ☐ |

---

## CAPACITÉ 5 — RESTITUTION (dashboard)

| # | Attendu | Vérifié ? |
|---|---------|-----------|
| D1 | Le projet apparaît dans les cartes du dashboard, vrai statut | ☐ |
| D2 | Le résumé sécurité intègre ses CVE | ☐ |
| D3 | La page Incidents le liste, filtre Statut OK, colonnes ID+Sévérité remplies | ☐ |
| D4 | Taux de réussite des builds affiché (ou « Non disponible ») | ☐ |

---

## SCÉNARIO SPÉCIAL — EMPTY (robustesse INDÉTERMINÉ)

> Teste le bug latent identifié : un scanner qui tourne mais ne trouve RIEN renvoie `status:'EMPTY'`, traité comme « incomplet ».

| # | Manœuvre | Attendu | Vérifié ? |
|---|----------|---------|-----------|
| E1 | Lancer avec un scanner qui ne trouve rien (ou couper ZAP) | Le scanner apparaît « non exécuté / INDÉTERMINÉ » | ☐ |
| E2 | Vérifier que le score global passe INDÉTERMINÉ, jamais « healthy » | ☐ |
| E3 | ⭐ Question : est-ce le comportement voulu, ou faut-il distinguer EMPTY (a tourné, rien trouvé) de UNKNOWN (n'a pas tourné) ? | Décision à noter | ☐ |

> E3 est un point d'amélioration potentiel à discuter/noter pour la soutenance : aujourd'hui « a scanné et rien trouvé » et « n'a pas scanné » sont confondus. C'est un bug latent honnête à mentionner.

---

## SYNTHÈSE

- **Détection :** ______ / 4 scanners OK
- **Analyse :** ______ / 7 points
- **Décision :** ______ / 7 points
- **Correction :** WF2 ______ / 8 · WF4 ______ / 4 · WF5 ______ / 5
- **Restitution :** ______ / 4

**Bugs ❌ trouvés dans la plateforme (à corriger avant soutenance) :**
1. _______________________________________________
2. _______________________________________________
3. _______________________________________________

> **Si tout passe :** tu démontres au jury la chaîne complète — 4 scanners détectent, 5 agents analysent, le Judge décide en fail-closed, et 3 workflows corrigent (applicatif, Jenkinsfile, Dockerfile) — avec un comportement honnête (secrets non auto-patchés, score INDÉTERMINÉ si scan incomplet). C'est la preuve que ta plateforme fait ce qu'elle annonce.
