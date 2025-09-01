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
                KATALON_HOME="/opt/Katalon_Studio_Engine_Linux_64-9.0.0"

                # Check if the file is already executable.
                if [ ! -x "$KATALON_HOME/katalonc" ]; then
                    echo "File is not executable, attempting to change permissions."

                    # Option 1: Try to change ownership to the Jenkins user.
                    # This is the more secure approach.
                    if [ -w "$KATALON_HOME" ]; then
                        sudo chown -R jenkins "$KATALON_HOME"
                        sudo chmod +x "$KATALON_HOME/katalonc"
                    else
                        # Option 2: Use sudo to grant execution permissions directly.
                        # This should work if chown fails.
                        echo "Changing permissions with sudo..."
                        sudo chmod +x "$KATALON_HOME/katalonc"
                    fi
                fi

                # Run the tests.
                "$KATALON_HOME/katalonc" \\
                    -projectPath="$(pwd)/Android Mobile Tests with Katalon Studio.prj" \\
                    -testSuitePath="Test Suites/Smoke Tests for Mobile Testing" \\
                    -executionProfile="default" \\
                    -executionPlatform="Android" \\
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