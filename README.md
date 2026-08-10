# vuln-testapp — Projet de validation sur mesure

> ⚠️ **Application volontairement vulnérable.** Conçue pour valider la plateforme DevSecOps de bout en bout. **Ne jamais déployer en production.**

## But

Tester que la plateforme fait **réellement** ce qu'elle annonce : détecter, analyser, décider, **et corriger**. Chaque bug cible une capacité confirmée dans le code de la plateforme (pas une supposition).

## Documents à lire dans l'ordre

1. **MATRICE-CONCEPTION.md** — quel bug teste quoi (vue d'ensemble).
2. **CHECKLIST-DETAILLEE.md** — le document de test à cocher, capacité par capacité.

## Ce que teste chaque groupe

- **Groupe A** (`CryptoService.java`, `DataService.java`) → bugs que **WF2 corrige par PR** (MD5, Random, catch vide, injection SQL). Concentrés dans un fichier pour garantir la sélection par WF2.
- **Groupe B** (`SecretsConfig.java`, `pom.xml`) → bugs **détectés seulement** : secrets BLOCKER (exclus de l'auto-fix) + CVE de dépendances (TODO). Testent la détection et le BLOCK du Judge.
- **Groupe C** (`Jenkinsfile.demo`) → à soumettre à **WF4** (Jenkinsfile Optimizer).
- **Groupe D** (`Dockerfile`) → déclenche les 4 IDs de **WF5** (Dockerfile Optimizer).
- **Scénario EMPTY** → robustesse du score INDÉTERMINÉ.

## Mode d'emploi (ordre QA rigoureux)

1. **Nettoyer les fins de ligne** (après passage par Windows) :
   ```bash
   sudo apt install -y dos2unix
   find . -type f \( -name "*.java" -o -name "*.xml" -o -name "*.properties" -o -name "Dockerfile" -o -name "*.md" \) -exec dos2unix {} \;
   ```
2. **Pousser sur GitHub** (repo **privé** — contient des faux secrets qui peuvent bloquer le push).
3. **Créer le projet `vuln-testapp`** dans la plateforme (avec `githubRepo: souhaiel11/vuln-testapp`) → récupérer son **PROJECT_ID** (UUID).
4. **Renseigner le `Jenkinsfile`** : remplacer `PROJECT_ID` et les URLs/tokens marqués `A_RENSEIGNER` par les vraies valeurs (copiées de ton `app-test` qui marche).
5. **Créer le job Jenkins** et lancer le build.
6. **Cocher `CHECKLIST-DETAILLEE.md`** selon ce que la plateforme affiche.
7. **Tester WF4** en soumettant `Jenkinsfile.demo`, et **WF5** via le Dockerfile.

## Vérifier que l'appli tourne (pré-requis ZAP)

```bash
# une fois l'appli lancée sur :8080
curl http://localhost:8080/api/health          # -> OK
curl "http://localhost:8080/api/greet?name=<script>alert(1)</script>"   # XSS
curl "http://localhost:8080/api/user?username=x' OR '1'='1"             # SQLi
```

## Le point QA important

Ce projet a été taillé **après** avoir vérifié la spec réelle de la plateforme (rapport d'inventaire du code). Conséquence : les bugs du **groupe B ne doivent PAS être auto-corrigés** — c'est le comportement voulu (un secret BLOCKER exige une revue humaine). Un test qui attendrait leur correction serait un faux négatif. La checklist distingue clairement « détecté » de « corrigé ».
