pipeline {
  agent any
  tools { maven 'M3' }

  environment {
    PROJECT_ID        = '49bca390-3661-40b1-8f7e-c5dfa3a5cca6'
    SONAR_PROJECT_KEY = 'vuln-testapp'
    SONAR_HOST_URL    = 'http://sonarqube:9000'
    DOCKER_IMAGE      = 'vuln-testapp'
    N8N_WEBHOOK       = 'http://n8n:5678/webhook/jenkins-event'
    N8N_API_KEY       = 'devsecops-secret-2024'
    BACKEND_URL       = 'http://172.31.172.61:3001'
    DOCKER_NETWORK    = 'pfe-network'
    SONAR_TOKEN       = 'squ_c33503b3657c5760a027c1fcd5eb5405cd1fbd36'
    DOCKER_PASSWORD   = 'dckr_pat_fake_password_for_wf4_test'
  }

  stages {

    stage('Build Info') {
      steps {
        script {
          env.BUILD_VERSION = "${env.BUILD_NUMBER}"
          env.COMPILE_STATUS = 'UNKNOWN'
          env.SONAR_STATUS = 'UNKNOWN'
          env.QUALITY_GATE_STATUS = 'UNKNOWN'
          env.SONAR_BUGS = '0'; env.SONAR_VULNS = '0'
          env.SONAR_SMELLS = '0'; env.SONAR_COVERAGE = '0'
          env.SONAR_DUPLICATIONS = '0'
          env.OWASP_STATUS = 'UNKNOWN'; env.OWASP_CRITICAL = '0'
          env.OWASP_HIGH = '0'; env.OWASP_ERROR = ''
          env.TRIVY_STATUS = 'UNKNOWN'; env.TRIVY_CRITICAL = '0'
          env.TRIVY_HIGH = '0'; env.TRIVY_ERROR = ''
          env.ZAP_STATUS = 'UNKNOWN'; env.ZAP_ALERTS_HIGH = '0'
          env.ZAP_ALERTS_MEDIUM = '0'; env.ZAP_ALERTS_LOW = '0'
          env.ZAP_ERROR = ''; env.DOCKER_BUILD_STATUS = 'UNKNOWN'
        }
      }
    }

    stage('Build') {
      steps { sh 'mvn clean package -DskipTests -q' }
      post {
        success { script { env.COMPILE_STATUS = 'SUCCESS' } }
        failure { script { env.COMPILE_STATUS = 'FAILED' } }
      }
    }

    stage('SAST - SonarQube') {
      options { catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') }
      steps {
        withSonarQubeEnv('sq1') {
          sh """
            /var/jenkins_home/tools/hudson.tasks.Maven_MavenInstallation/M3/bin/mvn \
            org.sonarsource.scanner.maven:sonar-maven-plugin:4.0.0.4121:sonar \
            -Dsonar.projectKey=${env.SONAR_PROJECT_KEY} \
            -Dsonar.projectVersion=${env.BUILD_VERSION}
          """
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
          def qg = waitForQualityGate()
          env.QUALITY_GATE_STATUS = qg.status
          try {
            def sonarMetrics = sh(script: """
              curl -s -u ${env.SONAR_TOKEN}: \
              "${env.SONAR_HOST_URL}/api/measures/component?component=${env.SONAR_PROJECT_KEY}&metricKeys=bugs,vulnerabilities,code_smells,coverage,duplicated_lines_density" \
              2>/dev/null || echo '{}'
            """, returnStdout: true).trim()
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
          } catch(e) { echo "SonarQube metrics failed: ${e.message}" }
        }
      }
    }

    stage('SCA - OWASP') {
      options { catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') }
      steps {
        script {
          def result = sh(script: """
            mvn org.owasp:dependency-check-maven:check \
              -DfailBuildOnCVSS=7 \
              -DnvdApiKey=14EB33B7-AB3A-F111-836A-129478FCB64D \
              -Dformats=HTML,JSON 2>&1 || true
          """, returnStdout: true).trim()
          if (result.contains('BUILD SUCCESS')) {
            env.OWASP_STATUS = 'SUCCESS'
            try {
              def report = new groovy.json.JsonSlurper().parseText(readFile("target/dependency-check-report.json"))
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
      post { always { archiveArtifacts artifacts: 'target/dependency-check-report.json', allowEmptyArchive: true } }
    }

    stage('Docker Build & Trivy') {
      options { catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') }
      steps {
        script {
          sh "docker build -t ${DOCKER_IMAGE}:latest ."
          env.DOCKER_BUILD_STATUS = 'SUCCESS'
          sh """
            trivy image --exit-code 0 --severity CRITICAL,HIGH \
              --format json --output ${WORKSPACE}/trivy-report.json \
              ${DOCKER_IMAGE}:latest 2>&1 || true
          """
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
      post { always { archiveArtifacts artifacts: 'trivy-report.json', allowEmptyArchive: true } }
    }

    stage('DAST - ZAP') {
      options { catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') }
      steps {
        script {
          sh "mkdir -p ${WORKSPACE}/security/zap"
          sh "docker run -d --name vuln-testapp-zap --network ${DOCKER_NETWORK} ${DOCKER_IMAGE}:latest"
          sleep(40)
          sh """
            docker run --rm \
              --network ${DOCKER_NETWORK} \
              -v ${WORKSPACE}/security/zap:/zap/wrk:rw \
              --user root \
              ghcr.io/zaproxy/zaproxy:stable \
              zap-baseline.py \
              -t http://vuln-testapp-zap:8080 \
              -r zap-report.html \
              -J zap-report.json \
              -I 2>&1 || true
          """
          sh "docker stop vuln-testapp-zap && docker rm vuln-testapp-zap || true"
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
      post { always { archiveArtifacts artifacts: 'security/zap/zap-report.json', allowEmptyArchive: true } }
    }
  }

  post {
    always {
      script {
        def buildStatus = currentBuild.currentResult
        def event = buildStatus == 'SUCCESS' ? 'pipeline_success' : buildStatus == 'UNSTABLE' ? 'pipeline_unstable' : 'pipeline_failed'
        def severity = buildStatus == 'FAILURE' ? 'HIGH' : buildStatus == 'UNSTABLE' ? 'MEDIUM' : 'LOW'
        try {
          def payload = """{
            "event":"${event}","project_id":"${env.PROJECT_ID}",
            "job":"${env.JOB_NAME}","build_number":"${env.BUILD_NUMBER}",
            "build_url":"${env.BUILD_URL}","status":"${buildStatus}",
            "severity":"${severity}","duration_ms":${currentBuild.duration},
            "sonar":{"status":"${env.SONAR_STATUS}","quality_gate":"${env.QUALITY_GATE_STATUS}",
              "project_key":"${env.SONAR_PROJECT_KEY}","bugs":${env.SONAR_BUGS},
              "vulnerabilities":${env.SONAR_VULNS},"code_smells":${env.SONAR_SMELLS},
              "coverage":"${env.SONAR_COVERAGE}","duplications":"${env.SONAR_DUPLICATIONS}",
              "dashboard_url":"http://172.31.172.61:9000/dashboard?id=${env.SONAR_PROJECT_KEY}",
              "api_url":"http://sonarqube:9000/api/measures/component?component=${env.SONAR_PROJECT_KEY}&metricKeys=bugs,vulnerabilities,code_smells,coverage,duplicated_lines_density"},
            "trivy":{"status":"${env.TRIVY_STATUS}","critical":${env.TRIVY_CRITICAL},
              "high":${env.TRIVY_HIGH},"error":"${env.TRIVY_ERROR}","image_tag":"latest",
              "report_url":"${env.BUILD_URL}artifact/trivy-report.json"},
            "owasp":{"status":"${env.OWASP_STATUS}","error":"${env.OWASP_ERROR}",
              "critical":${env.OWASP_CRITICAL},"high":${env.OWASP_HIGH},
              "report_url":"${env.BUILD_URL}artifact/target/dependency-check-report.json"},
            "zap":{"status":"${env.ZAP_STATUS}","error":"${env.ZAP_ERROR}",
              "alerts_high":${env.ZAP_ALERTS_HIGH},"alerts_medium":${env.ZAP_ALERTS_MEDIUM},
              "alerts_low":${env.ZAP_ALERTS_LOW},
              "report_url":"${env.BUILD_URL}artifact/security/zap/zap-report.json"},
            "tests":{"status":"SKIPPED","total":0,"failures":0},
            "docker":{"build_status":"${env.DOCKER_BUILD_STATUS}","push_status":"SKIPPED"},
            "deploy":{"status":"SKIPPED"},"nexus":{"status":"SKIPPED"}
          }"""
          httpRequest(url: env.N8N_WEBHOOK, httpMode: 'POST',
            contentType: 'APPLICATION_JSON', requestBody: payload,
            customHeaders: [[name: 'X-API-Key', value: env.N8N_API_KEY]],
            ignoreSslErrors: true, validResponseCodes: '100:599', timeout: 30)
          echo "n8n notifie : ${event}"
        } catch(Exception e) { echo "n8n failed: ${e.message}" }
        try {
          httpRequest(
            url: "${env.BACKEND_URL}/api/webhooks/jenkins/${env.PROJECT_ID}",
            httpMode: 'POST', contentType: 'APPLICATION_JSON',
            requestBody: """{"name":"${env.JOB_NAME}","build":{"phase":"FINALIZED","status":"${buildStatus}","number":"${env.BUILD_NUMBER}","url":"${env.BUILD_URL}"}}""",
            ignoreSslErrors: true, validResponseCodes: '100:599')
          echo "Backend notifie"
        } catch(Exception e) { echo "Backend failed: ${e.message}" }
      }
    }
  }
}
