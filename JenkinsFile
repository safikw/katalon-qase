pipeline {
    agent any

    environment {
        QASE_PROJECT_CODE = "MKQ"
        QASE_API_TOKEN    = credentials('QASE_API_TOKEN')
        KATALON_API_KEY   = credentials('KATALON_API_KEY')
    }

    stages {  
        stage('Create Qase Run') {
            steps {
                script {
                    sh '''
                    echo "Creating new Qase run..."
                    response=$(curl -s -X POST "https://api.qase.io/v1/run/$QASE_PROJECT_CODE" \
                      -H "Token: $QASE_API_TOKEN" \
                      -H "Content-Type: application/json" \
                      -d "{
                            \\"title\\": \\"Jenkins Run #$BUILD_NUMBER\\"
                          }")
                    echo "$response" > qase_run.json
                    runId=$(jq -r '.result.id' qase_run.json)
                    echo "Created Qase Run ID = $runId"
                    echo $runId > qase_run_id.txt
                    '''
                }
            }
        }

        stage('Run Katalon Tests') {
            steps {
                script {
                    sh '''
                    ./katalonc.sh -projectPath="/katalon/project" \
                    -testSuitePath="Test Suites/Smoke Tests for Mobile Testing" \
                    -executionProfile="default" \
                    -deviceId="emulator-5554" \
                    -executionPlatform="Android" \
                    -apiKey=$KATALON_API_KEY
                    '''
                }
            }
        }

        stage('Send Results to Qase') {
            steps {
                script {
                    sh '''
                    runId=$(cat qase_run_id.txt)
                    echo "Sending results to Qase run $runId ..."
                    # Bisa pakai Katalon listener atau curl upload report
                    '''
                }
            }
        }

        stage('Archive Reports') {
            steps {
                archiveArtifacts artifacts: 'qase_run*.txt', followSymlinks: false
                junit '**/Reports/**/JUnit_Report.xml'
            }
        }
    }

    post {
        always {
            echo "Build finished: ${currentBuild.currentResult}"
        }
        success {
            echo "Build successful!"
        }
        failure {
            echo "Build failed!"
        }
    }
}
