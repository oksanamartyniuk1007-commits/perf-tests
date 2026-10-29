pipeline {
    // All stages run ONLY on the agent with label "perf" (controller has 0 executors)
    agent { label 'perf' }

    options {
        buildDiscarder(logRotator(numToKeepStr: '20'))
        timeout(time: 90, unit: 'MINUTES')
        disableConcurrentBuilds()
    }

    parameters {
        booleanParam(name: 'RUN_JMETER',     defaultValue: true, description: 'Backend: run JMeter test')
        booleanParam(name: 'RUN_GATLING',    defaultValue: true, description: 'Backend: run Gatling test')
        booleanParam(name: 'RUN_LIGHTHOUSE', defaultValue: true, description: 'UI: run Lighthouse user flow')
        string(name: 'APP_HOST',     defaultValue: 'localhost', description: 'Application host (as seen from the agent)')
        string(name: 'APP_PORT',     defaultValue: '80',        description: 'Application port')
        choice(name: 'USERS',        choices: ['3', '4', '5'],  description: 'Backend: number of concurrent users')
        string(name: 'DURATION_SEC', defaultValue: '600',       description: 'Backend: total duration in seconds, ramp-up included (600 = 10 min)')
        string(name: 'RAMPUP_SEC',   defaultValue: '60',        description: 'Backend: ramp-up in seconds')
        choice(name: 'LH_LOOPS',     choices: ['3', '4', '5', '6', '7', '8', '9', '10'], description: 'UI: number of Lighthouse script loops')
    }

    // Defaults protect the very first build, when Jenkins does not know the parameters yet
    environment {
        JMETER_HOME = '/opt/jmeter'
        T_HOST      = "${params.APP_HOST ?: 'localhost'}"
        T_PORT      = "${params.APP_PORT ?: '80'}"
        T_USERS     = "${params.USERS ?: '3'}"
        T_DURATION  = "${params.DURATION_SEC ?: '600'}"
        T_RAMPUP    = "${params.RAMPUP_SEC ?: '60'}"
        T_LOOPS     = "${params.LH_LOOPS ?: '3'}"
        BASE_URL    = "http://${params.APP_HOST ?: 'localhost'}:${params.APP_PORT ?: '80'}"
    }

    stages {
        stage('Validate & Agent info') {
            steps {
                script {
                    if (params.RUN_JMETER == false && params.RUN_GATLING == false && params.RUN_LIGHTHOUSE == false) {
                        error 'Select at least one test to run'
                    }
                }
                sh '''
                    echo "Node: ${NODE_NAME} | host: $(hostname) | user: $(whoami)"
                    echo "Target: ${BASE_URL} | users: ${T_USERS} | duration: ${T_DURATION}s | ramp-up: ${T_RAMPUP}s | LH loops: ${T_LOOPS}"
                    java -version 2>&1 | head -1
                    echo "Node.js: $(node -v)"
                    curl -s -o /dev/null -w "Application HTTP status: %{http_code}\\n" "${BASE_URL}"
                    rm -rf reports && mkdir -p reports
                '''
            }
        }

        stage('JMeter') {
            when { expression { params.RUN_JMETER != false } }
            steps {
                catchError(buildResult: 'FAILURE', stageResult: 'FAILURE') {
                    sh '''
                        mkdir -p reports/jmeter
                        "${JMETER_HOME}/bin/jmeter" -n -t jmeter/test-plan.jmx \
                            -Jthreads="${T_USERS}" -Jrampup="${T_RAMPUP}" -Jduration="${T_DURATION}" \
                            -Jhost="${T_HOST}" -Jport="${T_PORT}" \
                            -l reports/jmeter/results.jtl -j reports/jmeter/jmeter.log \
                            -e -o reports/jmeter/html
                    '''
                }
            }
            post {
                always {
                    publishHTML(target: [allowMissing: true, alwaysLinkToLastBuild: true, keepAll: true,
                        reportDir: 'reports/jmeter/html', reportFiles: 'index.html', reportName: 'JMeter Report'])
                }
            }
        }

        stage('Gatling') {
            when { expression { params.RUN_GATLING != false } }
            steps {
                catchError(buildResult: 'FAILURE', stageResult: 'FAILURE') {
                    dir('gatling') {
                        sh '''
                            chmod +x mvnw
                            ./mvnw -B clean gatling:test \
                                -Dgatling.simulationClass=simulations.PerfTestSimulation \
                                -DUSER_COUNT="${T_USERS}" -DRAMP_UP_PERIOD="${T_RAMPUP}" \
                                -DTEST_DURATION="${T_DURATION}" -DBASE_URL="${BASE_URL}"
                        '''
                    }
                    sh '''
                        mkdir -p reports/gatling
                        LAST=$(ls -td gatling/target/gatling/*/ | head -1)
                        cp -r "${LAST}." reports/gatling/
                    '''
                }
            }
            post {
                always {
                    publishHTML(target: [allowMissing: true, alwaysLinkToLastBuild: true, keepAll: true,
                        reportDir: 'reports/gatling', reportFiles: 'index.html', reportName: 'Gatling Report'])
                }
            }
        }

        stage('Lighthouse') {
            when { expression { params.RUN_LIGHTHOUSE != false } }
            steps {
                catchError(buildResult: 'FAILURE', stageResult: 'FAILURE') {
                    dir('lighthouse') {
                        sh 'npm ci --no-audit --no-fund'
                        sh '''
                            OUT="${WORKSPACE}/reports/lighthouse"
                            mkdir -p "$OUT"
                            for i in $(seq 1 "${T_LOOPS}"); do
                                echo "===== Lighthouse loop $i of ${T_LOOPS} ====="
                                REPORT_NAME="$OUT/run-$i.html" node lighthouse-flow.js
                            done
                            {
                                echo '<!DOCTYPE html><html><head><meta charset="utf-8"><title>Lighthouse runs</title></head><body>'
                                echo "<h1>Lighthouse user flow: ${T_LOOPS} loops</h1><ul>"
                                for i in $(seq 1 "${T_LOOPS}"); do echo "<li><a href=\\"run-$i.html\\">Run $i</a></li>"; done
                                echo '</ul></body></html>'
                            } > "$OUT/index.html"
                        '''
                    }
                }
            }
            post {
                always {
                    publishHTML(target: [allowMissing: true, alwaysLinkToLastBuild: true, keepAll: true,
                        reportDir: 'reports/lighthouse', reportFiles: 'index.html', reportName: 'Lighthouse Report'])
                }
            }
        }
    }

    post {
        always {
            archiveArtifacts artifacts: 'reports/**', allowEmptyArchive: true
        }
    }
}
