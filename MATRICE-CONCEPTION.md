# Matrice de conception — vuln-testapp

> Chaque bug de ce projet cible une **capacité réelle** de la plateforme, confirmée dans le code (rapport Claude Code). Rien n'est planté au hasard.

## Principe

Le projet teste la chaîne complète : **détection → analyse → décision → correction**. Les bugs sont répartis en 4 groupes selon la capacité qu'ils exercent, plus un scénario spécial (EMPTY).

## Vue d'ensemble

| Groupe | Bugs | Capacité testée | Résultat attendu |
|--------|------|-----------------|------------------|
| **A** | MD5, catch vide, Random prévisible, injection SQL | **WF2 — correction par PR** | Findings Sonar CRITICAL/MAJOR non-BLOCKER, concentrés dans `CryptoService.java`/`DataService.java` → **WF2 génère une vraie PR** de correction |
| **B** | Secrets en dur, Log4Shell, Jackson, SnakeYAML | **Détection seule** | Détectés (Sonar BLOCKER + Trivy/OWASP CVE) → **BLOCK du Judge**, mais **PAS auto-corrigés** (BLOCKER exclus, CVE deps = TODO) |
| **C** | Jenkinsfile.demo (pas de timeout/retry/cleanWs, creds en clair) | **WF4 — Jenkinsfile Optimizer** | Soumis à WF4 via bouton → **PR d'optimisation** du Jenkinsfile |
| **D** | Dockerfile (JDK obsolète, tag non pinné, pas de USER/HEALTHCHECK) | **WF5 — Dockerfile Optimizer** | Déclenche les 4 IDs `DF-*` → **PR de correction** du Dockerfile |
| **EMPTY** | (scénario, pas un bug du code) | **Robustesse score INDÉTERMINÉ** | Forcer un scanner à ne rien trouver → vérifier le comportement `EMPTY` vs `COMPLETED` |

## Pourquoi cette répartition (le point QA crucial)

Le rapport d'inventaire a révélé que **WF2 ne corrige PAS ce qu'on croyait** :

- Un **secret en dur** est un finding **BLOCKER** → il est **exclu de l'auto-fix** (revue humaine obligatoire par design de sécurité). Le tester comme « corrigeable » aurait produit un **faux négatif**.
- Les **CVE de dépendances** (Log4Shell...) → **commentaire TODO seulement**, pas de bump de version automatique.
- Ce que WF2 corrige vraiment : findings Sonar **CRITICAL/MAJOR non-BLOCKER** de type BUG/VULNERABILITY/CODE_SMELL, **dans le fichier le plus problématique**.

D'où la stratégie : **concentrer les bugs corrigeables du groupe A dans un même fichier** (`CryptoService.java`) pour garantir qu'il soit le fichier sélectionné par WF2 → PR générée. Les bugs du groupe B sont là pour prouver la **détection** et le **BLOCK**, sans attendre de correction.

## Fichiers du projet et leur rôle

| Fichier | Groupe | Ce qu'il déclenche |
|---------|--------|--------------------|
| `service/CryptoService.java` | A | 3 findings CRITICAL/MAJOR corrigeables (MD5, Random, catch vide) — **cible PR WF2** |
| `service/DataService.java` | A | Injection SQL CRITICAL corrigeable |
| `config/SecretsConfig.java` | B | 3 secrets BLOCKER (détectés, exclus auto-fix) |
| `controller/ApiController.java` | ZAP | Endpoints XSS / SQL / divulgation (DAST) |
| `resources/application.properties` | B | Secret config + H2 console exposée |
| `pom.xml` | B | 4 dépendances CVE (Trivy + OWASP) |
| `Dockerfile` | D | 4 défauts → WF5 |
| `Jenkinsfile.demo` | C | Patterns Groovy manquants → WF4 |
| `Jenkinsfile` | — | Le pipeline réel (scan-only) qui exécute tout |

> ⚠️ `Jenkinsfile.demo` n'est **pas** le pipeline exécuté — c'est le fichier à soumettre manuellement à WF4. Le vrai pipeline est `Jenkinsfile`.
