pipeline {
  agent any
  tools { maven 'M3' }

  options {
    timestamps()
    timeout(time: 60, unit: 'MINUTES')
    buildDiscarder(logRotator(numToKeepStr: '10'))
  }

  environment {
    PROJECT_ID        = '49bca390-3661-40b1-8f7e-c5dfa3a5cca6'
    SONAR_PROJECT_KEY = 'vuln-testapp'
    SONAR_HOST_URL    = 'http://sonarqube:9000'
    // URL publique du dashboard SonarQube (IP externe intentionnelle pour acces navigateur)
    SONAR_DASHBOARD_URL = 'http://172.31.172.61:9000'
    DOCKER_IMAGE      = 'vuln-testapp'
    N8N_WEBHOOK       = 'http://n8n:5678/webhook/jenkins-event'
    BACKEND_URL       = 'http://172.31.172.61:3001'
    DOCKER_NETWORK    = 'pfe-network'
    COMPILE_STATUS        = 'UNKNOWN'
    SONAR_STATUS          = 'UNKNOWN'
    QUALITY_GATE_STATUS   = 'UNKNOWN'
    SONAR_BUGS            = '0'
    SONAR_VULNS           = '0'
    SONAR_SMELLS          = '0'
    SONAR_COVERAGE        = '0'
    SONAR_DUPLICATIONS    = '0'
    OWASP_STATUS          = 'UNKNOWN'
    OWASP_CRITICAL        = '0'
    OWASP_HIGH            = '0'
    OWASP_ERROR           = ''
    TRIVY_STATUS          = 'UNKNOWN'
    TRIVY_CRITICAL        = '0'
    TRIVY_HIGH            = '0'
    TRIVY_ERROR           = ''
    ZAP_STATUS            = 'UNKNOWN'
    ZAP_ALERTS_HIGH       = '0'
    ZAP_ALERTS_MEDIUM     = '0'
    ZAP_ALERTS_LOW        = '0'
    ZAP_ERROR             = ''
    DOCKER_BUILD_STATUS   = 'UNKNOWN'
  }

  stages {

    stage('Build Info') {
      steps {
        script {
          sh 'chmod +x mvnw'
          env.BUILD_VERSION = "${env.BUILD_NUMBER}"
        }
      }
    }

    stage('Build') {
      steps { sh './mvnw clean package -DskipTests -q -Dmaven.repo.local=/var/jenkins_home/.m2/repository' }
      post {
        success {
          script { env.COMPILE_STATUS = 'SUCCESS' }
          archiveArtifacts artifacts: 'target/vuln-testapp-1.0.0.jar', allowEmptyArchive: true
        }
        failure { script { env.COMPILE_STATUS = 'FAILED' } }
      }
    }

    stage('SAST - SonarQube') {
      options { catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') }
      steps {
        withCredentials([string(credentialsId: 'sonar-token', variable: 'SONAR_TOKEN')]) {
          withSonarQubeEnv('sq1') {
            retry(2) {
              sh '''
                ./mvnw org.sonarsource.scanner.maven:sonar-maven-plugin:4.0.0.4121:sonar \
                -Dsonar.projectKey=$SONAR_PROJECT_KEY \
                -Dsonar.projectVersion=$BUILD_VERSION \
                -Dmaven.repo.local=/var/jenkins_home/.m2/repository
              '''
            }
          }
        }
        script { env.SONAR_STATUS = 'SUCCESS' }
      }
      post { failure { script { env.SONAR_STATUS = 'FAILED' } } }
    }

    stage('Quality Gate') {
      options {
        catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE')
        timeout(time: 5, unit: 'MINUTES')
      }
      steps {
        script {
          withCredentials([string(credentialsId: 'sonar-token', variable: 'SONAR_TOKEN')]) {
            try {
              // Recuperation du statut du Quality Gate via l'API SonarQube (sans dependance au webhook)
              def qgResponse = retry(2) {
                sh(script: '''
                  curl -s --max-time 30 -u "$SONAR_TOKEN": \
                  "$SONAR_HOST_URL/api/qualitygates/project_status?projectKey=$SONAR_PROJECT_KEY" \
                  2>/dev/null || echo '{}'
                ''', returnStdout: true).trim()
              }
              def qgJson = new groovy.json.JsonSlurper().parseText(qgResponse)
              env.QUALITY_GATE_STATUS = qgJson?.projectStatus?.status ?: 'UNKNOWN'

              def sonarMetrics = retry(2) {
                sh(script: '''
                  curl -s --max-time 30 -u "$SONAR_TOKEN": \
                  "$SONAR_HOST_URL/api/measures/component?component=$SONAR_PROJECT_KEY&metricKeys=bugs,vulnerabilities,code_smells,coverage,duplicated_lines_density" \
                  2>/dev/null || echo '{}'
                ''', returnStdout: true).trim()
              }
              def metrics = new groovy.json.JsonSlurper().parseText(sonarMetrics)
              metrics.component?.measures?.each { m ->
                switch(m.metric) {
                  case 'bugs':                     env.SONAR_BUGS         = m.value ?: '0'; break
                  case 'vulnerabilities':          env.SONAR_VULNS        = m.value ?: '0'; break
                  case 'code_smells':              env.SONAR_SMELLS       = m.value ?: '0'; break
                  case 'coverage':                 env.SONAR_COVERAGE     = m.value ?: '0'; break
                  case 'duplicated_lines_density': env.SONAR_DUPLICATIONS = m.value ?: '0'; break
                }
              }
            } catch(e) { echo "SonarQube quality gate/metrics failed: ${e.message}" }
          }
        }
      }
    }

    stage('Security Scans') {
      parallel {

        stage('SCA - OWASP') {
          options {
            catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE')
            timeout(time: 30, unit: 'MINUTES')
          }
          steps {
            withCredentials([string(credentialsId: 'nvd-api-key', variable: 'NVD_API_KEY')]) {
              script {
                def result = retry(2) {
                  sh(script: '''
                    ./mvnw org.owasp:dependency-check-maven:check \
                      -DfailBuildOnCVSS=7 \
                      "-DnvdApiKey=$NVD_API_KEY" \
                      -Dformats=HTML,JSON \
                      -Dmaven.repo.local=/var/jenkins_home/.m2/repository 2>&1 || true
                  ''', returnStdout: true).trim()
                }
                if (result.contains('BUILD SUCCESS')) {
                  env.OWASP_STATUS = 'SUCCESS'
                  try {
                    def report = new groovy.json.JsonSlurper().parseText(readFile('target/dependency-check-report.json'))
                    def critical = 0; def high = 0
                    report.dependencies?.each { dep -> dep.vulnerabilities?.each { v ->
                      if (v.severity == 'CRITICAL') critical++
                      if (v.severity == 'HIGH') high++
                    }}
                    env.OWASP_CRITICAL = "${critical}"; env.OWASP_HIGH = "${high}"
                  } catch(e) {}
                } else {
                  env.OWASP_STATUS = 'FAILED'; env.OWASP_ERROR = 'Dependency check failed'
                }
              }
            }
          }
          post { always { archiveArtifacts artifacts: 'target/dependency-check-report.json, target/dependency-check-report.html', allowEmptyArchive: true } }
        }

        stage('Docker Build & Trivy') {
          options {
            catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE')
            timeout(time: 15, unit: 'MINUTES')
          }
          steps {
            script {
              sh 'docker build -t $DOCKER_IMAGE:$BUILD_NUMBER .'
              env.DOCKER_BUILD_STATUS = 'SUCCESS'
              retry(2) {
                sh '''
                  trivy image --exit-code 0 --severity CRITICAL,HIGH \
                    --format json --output $WORKSPACE/trivy-report.json \
                    $DOCKER_IMAGE:$BUILD_NUMBER 2>&1 || true
                '''
              }
              try {
                def report = new groovy.json.JsonSlurper().parseText(readFile("${env.WORKSPACE}/trivy-report.json"))
                def critical = 0; def high = 0
                report.Results?.each { r -> r.Vulnerabilities?.each { v ->
                  if (v.Severity == 'CRITICAL') critical++
                  if (v.Severity == 'HIGH') high++
                }}
                env.TRIVY_CRITICAL = "${critical}"; env.TRIVY_HIGH = "${high}"; env.TRIVY_STATUS = 'SUCCESS'
              } catch(e) { env.TRIVY_STATUS = 'FAILED'; env.TRIVY_ERROR = "Parse error: ${e.message}" }
            }
          }
          post {
            always {
              archiveArtifacts artifacts: 'trivy-report.json', allowEmptyArchive: true
              sh 'docker rmi $DOCKER_IMAGE:$BUILD_NUMBER || true'
            }
          }
        }

      }
    }

    stage('DAST - ZAP') {
      options {
        catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE')
        timeout(time: 20, unit: 'MINUTES')
      }
      steps {
        script {
          sh '''
            mkdir -p $WORKSPACE/security/zap
            docker rm -f vuln-testapp-zap || true
            docker run -d --name vuln-testapp-zap --network $DOCKER_NETWORK $DOCKER_IMAGE:$BUILD_NUMBER
          '''
          sh '''
            echo "Attente du demarrage du conteneur applicatif..."
            for i in $(seq 1 12); do
              if curl -sf http://vuln-testapp-zap:8080 > /dev/null 2>&1; then
                echo "Application disponible."
                exit 0
              fi
              echo "Tentative $i/12 - attente 5s..."
              sleep 5
            done
            echo "Timeout: l application n a pas repondu dans les 60s."
            exit 1
          '''
          retry(2) {
            sh '''
              docker run --rm \
                --network $DOCKER_NETWORK \
                -v $WORKSPACE/security/zap:/zap/wrk:rw \
                ghcr.io/zaproxy/zaproxy:2.15.0 \
                zap-baseline.py \
                -t http://vuln-testapp-zap:8080 \
                -r zap-report.html \
                -J zap-report.json \
                -I 2>&1 || true
            '''
          }
          try {
            def zapReport = new groovy.json.JsonSlurper().parseText(readFile("${WORKSPACE}/security/zap/zap-report.json"))
            def high = 0; def medium = 0; def low = 0
            zapReport.site?.each { site -> site.alerts?.each { alert ->
              switch(alert.riskdesc?.split(' ')[0]) {
                case 'High':   high++;   break
                case 'Medium': medium++; break
                case 'Low':    low++;    break
              }
            }}
            env.ZAP_ALERTS_HIGH = "${high}"; env.ZAP_ALERTS_MEDIUM = "${medium}"
            env.ZAP_ALERTS_LOW = "${low}"; env.ZAP_STATUS = 'SUCCESS'
          } catch(e) { env.ZAP_STATUS = 'FAILED'; env.ZAP_ERROR = "Parse error: ${e.message}" }
        }
      }
      post {
        always {
          sh 'docker rm -f vuln-testapp-zap || true'
          archiveArtifacts artifacts: 'security/zap/zap-report.json, security/zap/zap-report.html', allowEmptyArchive: true
        }
      }
    }
  }

  post {
    always {
      script {
        def buildStatus = currentBuild.currentResult
        def event = buildStatus == 'SUCCESS' ? 'pipeline_success' : buildStatus == 'UNSTABLE' ? 'pipeline_unstable' : 'pipeline_failed'
        def severity = buildStatus == 'FAILURE' ? 'HIGH' : buildStatus == 'UNSTABLE' ? 'MEDIUM' : 'LOW'
        withCredentials([string(credentialsId: 'N8N_API_KEY', variable: 'N8N_API_KEY')]) {
          try {
            def payloadMap = [
              event: event,
              project_id: env.PROJECT_ID,
              job: env.JOB_NAME,
              build_number: env.BUILD_NUMBER,
              build_url: env.BUILD_URL,
              status: buildStatus,
              severity: severity,
              duration_ms: currentBuild.duration,
              sonar: [
                status: env.SONAR_STATUS,
                quality_gate: env.QUALITY_GATE_STATUS,
                project_key: env.SONAR_PROJECT_KEY,
                bugs: env.SONAR_BUGS,
                vulnerabilities: env.SONAR_VULNS,
                code_smells: env.SONAR_SMELLS,
                coverage: env.SONAR_COVERAGE,
                duplications: env.SONAR_DUPLICATIONS,
                dashboard_url: "${env.SONAR_DASHBOARD_URL}/dashboard?id=${env.SONAR_PROJECT_KEY}",
                api_url: "${env.SONAR_HOST_URL}/api/measures/component?component=${env.SONAR_PROJECT_KEY}&metricKeys=bugs,vulnerabilities,code_smells,coverage,duplicated_lines_density"
              ],
              trivy: [
                status: env.TRIVY_STATUS,
                critical: env.TRIVY_CRITICAL,
                high: env.TRIVY_HIGH,
                error: env.TRIVY_ERROR,
                image_tag: env.BUILD_NUMBER,
                report_url: "${env.BUILD_URL}artifact/trivy-report.json"
              ],
              owasp: [
                status: env.OWASP_STATUS,
                error: env.OWASP_ERROR,
                critical: env.OWASP_CRITICAL,
                high: env.OWASP_HIGH,
                report_url: "${env.BUILD_URL}artifact/target/dependency-check-report.json"
              ],
              zap: [
                status: env.ZAP_STATUS,
                error: env.ZAP_ERROR,
                alerts_high: env.ZAP_ALERTS_HIGH,
                alerts_medium: env.ZAP_ALERTS_MEDIUM,
                alerts_low: env.ZAP_ALERTS_LOW,
                report_url: "${env.BUILD_URL}artifact/security/zap/zap-report.json"
              ],
              tests: [status: 'SKIPPED', total: 0, failures: 0],
              docker: [build_status: env.DOCKER_BUILD_STATUS, push_status: 'SKIPPED'],
              deploy: [status: 'SKIPPED'],
              nexus: [status: 'SKIPPED']
            ]
            def payload = groovy.json.JsonOutput.toJson(payloadMap)
            httpRequest(url: env.N8N_WEBHOOK, httpMode: 'POST',
              contentType: 'APPLICATION_JSON', requestBody: payload,
              customHeaders: [[name: 'X-API-Key', value: N8N_API_KEY]],
              ignoreSslErrors: true, validResponseCodes: '100:599', timeout: 30)
            echo "n8n notifie : ${event}"
          } catch(Exception e) { echo "n8n failed: ${e.message}" }
        }
        try {
          def backendPayloadMap = [
            name: env.JOB_NAME,
            build: [
              phase: 'FINALIZED',
              status: buildStatus,
              number: env.BUILD_NUMBER,
              url: env.BUILD_URL
            ]
          ]
          def backendPayload = groovy.json.JsonOutput.toJson(backendPayloadMap)
          httpRequest(
            url: "${env.BACKEND_URL}/api/webhooks/jenkins/${env.PROJECT_ID}",
            httpMode: 'POST', contentType: 'APPLICATION_JSON',
            requestBody: backendPayload,
            ignoreSslErrors: true, validResponseCodes: '100:599')
          echo "Backend notifie"
        } catch(Exception e) { echo "Backend failed: ${e.message}" }
      }
      cleanWs()
    }
  }
}