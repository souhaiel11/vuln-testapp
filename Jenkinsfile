// ============================================================================
// Jenkinsfile REEL — vuln-testapp (scan-only, calque sur app-test)
// ============================================================================
// Ce pipeline lance les 4 scanners puis envoie le rapport enrichi a n8n
// (declenche WF1 -> analyse multi-agents -> Judge).
//
// >>> A RENSEIGNER AVANT LE PREMIER BUILD (valeurs propres a TA plateforme) :
//     - PROJECT_ID : l'UUID genere quand tu crees le projet "vuln-testapp"
//                    dans la plateforme (comme le 54192eca... de app-test).
//     - Les URLs / tokens : COPIE-LES depuis ton Jenkinsfile app-test qui
//                    fonctionne (SONAR_HOST, N8N_WEBHOOK_URL, BACKEND_URL,
//                    X-API-Key, chemins du volume partage).
//
// Pour ZAP : l'app est lancee dans un conteneur Docker jetable (pas K8s),
// scannee, puis arretee. Adapte si tu preferes deployer sur Minikube.
// ============================================================================

pipeline {
    agent any

    environment {
        // >>> REMPLACE par l'UUID reel de vuln-testapp dans ta plateforme
        PROJECT_ID    = 'A_RENSEIGNER_UUID_VULN_TESTAPP'

        // >>> COPIE ces valeurs depuis ton Jenkinsfile app-test qui marche
        SONAR_HOST    = 'http://sonarqube:9000'
        SONAR_KEY     = 'vuln-testapp'
        N8N_WEBHOOK   = 'A_RENSEIGNER_depuis_app-test'   // ex: http://n8n:5678/webhook/xxxx
        BACKEND_URL   = 'A_RENSEIGNER_depuis_app-test'   // ex: http://backend:3001
        API_KEY       = 'devsecops-secret-2024'          // le X-API-Key de app-test

        IMAGE_NAME    = 'vuln-testapp:latest'
        REPORTS_DIR   = '/shared-reports/vuln-testapp'   // adapte au volume partage reel
    }

    stages {
        stage('Checkout') {
            steps { checkout scm }
        }

        stage('Build') {
            steps {
                echo 'Compilation Maven (doit reussir — les vulns ne cassent pas le build)'
                sh 'mvn -B clean package -DskipTests'
            }
        }

        // ==== SCANNER 1 : SonarQube ====
        stage('SonarQube') {
            steps {
                echo 'Attendu : BLOCKER (secrets, groupe B), CRITICAL/MAJOR corrigeables (groupe A), injection SQL'
                sh '''
                    mvn sonar:sonar \
                      -Dsonar.projectKey=${SONAR_KEY} \
                      -Dsonar.host.url=${SONAR_HOST} || true
                '''
            }
        }

        // ==== SCANNER 2 : OWASP Dependency-Check ====
        stage('OWASP Dependency-Check') {
            steps {
                echo 'Attendu : Log4Shell + Jackson + SnakeYAML + Commons Text (groupe B)'
                sh '''
                    mkdir -p ${REPORTS_DIR}
                    dependency-check.sh --project vuln-testapp --scan . \
                      --format JSON --out ${REPORTS_DIR}/owasp-report.json || true
                '''
            }
        }

        // ==== SCANNER 3 : Trivy ====
        stage('Docker Build & Trivy') {
            steps {
                echo 'Attendu : CVE image de base (openjdk:8) + Log4Shell dans le jar'
                sh '''
                    docker build -t ${IMAGE_NAME} .
                    trivy image --format json --output ${REPORTS_DIR}/trivy-report.json ${IMAGE_NAME} || true
                '''
            }
        }

        // ==== SCANNER 4 : ZAP (app dans un conteneur jetable) ====
        stage('ZAP DAST') {
            steps {
                echo 'Attendu : XSS reflechi, injection SQL, endpoints exposes, headers manquants'
                sh '''
                    docker run -d --name vuln-testapp-run -p 8080:8080 ${IMAGE_NAME}
                    sleep 40   # laisser Spring Boot demarrer (JVM lente sur WSL)
                    zap-baseline.py -t http://vuln-testapp-run:8080 \
                      -J ${REPORTS_DIR}/zap-report.json || true
                    docker stop vuln-testapp-run && docker rm vuln-testapp-run
                '''
            }
        }

        // ==== Envoi vers n8n (declenche WF1 : agents IA + Judge) ====
        stage('Notify Platform') {
            steps {
                echo 'Envoi du rapport enrichi vers n8n + webhook backend'
                sh '''
                    curl -X POST "${N8N_WEBHOOK}" \
                      -H "Content-Type: application/json" \
                      -H "X-API-Key: ${API_KEY}" \
                      -d "{
                        \\"projectId\\": \\"${PROJECT_ID}\\",
                        \\"jenkinsJobName\\": \\"vuln-testapp\\",
                        \\"buildNumber\\": \\"${BUILD_NUMBER}\\",
                        \\"status\\": \\"${currentBuild.currentResult}\\"
                      }" || true

                    curl -X POST "${BACKEND_URL}/api/webhooks/jenkins/${PROJECT_ID}" \
                      -H "Content-Type: application/json" \
                      -H "X-API-Key: ${API_KEY}" \
                      -d "{\\"buildNumber\\": \\"${BUILD_NUMBER}\\"}" || true
                '''
            }
        }
    }

    post {
        always { echo "Pipeline termine — rapports dans ${REPORTS_DIR}" }
    }
}
