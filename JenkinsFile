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
                sh '''
                # Use the exact, correct path to the Katalon Runtime Engine folder.
                # This path matches what you found in the container at /opt/.
                KATALON_HOME="/opt/Katalon_Studio_Engine_Linux_64-9.0.0"

                echo "Running Katalon Tests with Katalon Runtime Engine from $KATALON_HOME"

                # Give execution permissions to the Katalon executable.
                # This is a good practice to ensure it's runnable.
                chmod +x "$KATALON_HOME/katalonc"

                # Run the tests using the corrected path and project details.
                # The $(pwd) variable points to the Jenkins workspace where your Git project is cloned.
                "$KATALON_HOME/katalonc" \\
                    -projectPath="$(pwd)/Android Mobile Tests with Katalon Studio.prj" \\
                    -testSuitePath="Test Suites/Smoke Tests for Mobile Testing" \\
                    -executionProfile="default" \\
                    -executionPlatform="Android" \\
                    -apiKey="$KATALON_API_KEY" \\
                    -browserType="Mobile"
                '''
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
