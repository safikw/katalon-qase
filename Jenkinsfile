pipeline {
    agent any

    environment {
        QASE_PROJECT_CODE = "MKQ"
        DEVICE_IP         = "192.168.1.10:7405"
        KATALON_HOME      = "/opt/Katalon_Studio_Engine_Linux_arm64-10.2.4"
        PROJECT_PATH      = "/var/jenkins_home/workspace/jenkins-qase/Android Mobile Tests with Katalon Studio.prj"
        TEST_SUITE        = "Test Suites/Smoke Tests for API Demos App"
        APP_DRIVER_URL    = "http://localhost:4723"
    }

    stages {

        stage('Check Device') {
            steps {
                sh 'adb devices'
            }
        }

        stage('Create Qase Run') {
            steps {
                withCredentials([string(credentialsId: 'QASE_API_TOKEN', variable: 'QASE_API_TOKEN')]) {
                    sh '''
                        echo "Creating new Qase run..."
                        response=$(curl -s -X POST "https://api.qase.io/v1/run/$QASE_PROJECT_CODE" \
                            -H "Token: $QASE_API_TOKEN" \
                            -H "Content-Type: application/json" \
                            -d "{ \\"title\\": \\"Jenkins Run #$BUILD_NUMBER\\" }")
                        echo "$response" > qase_run.json
                        runId=$(jq -r '.result.id' qase_run.json)
                        echo "Created Qase Run ID = $runId"
                        echo $runId > qase_run_id.txt
                    '''
                }
            }
        }

        stage('Run Katalon Test') {
            steps {
                withCredentials([string(credentialsId: 'KATALON_API_KEY', variable: 'KATALON_API_KEY')]) {
                    sh """
                        adb start-server
                        echo "Using device: $DEVICE_IP"

                        $KATALON_HOME/katalonc \
                            -noSplash \
                            -runMode=console \
                            -projectPath="$PROJECT_PATH" \
                            -retry=0 \
                            -testSuitePath="$TEST_SUITE" \
                            -browserType="Android" \
                            -deviceId="$DEVICE_IP" \
                            -executionProfile="default" \
                            -apiKey="$KATALON_API_KEY" \
                            --config -proxy.auth.option=NO_PROXY \
                            -proxy.system.option=NO_PROXY \
                            -proxy.system.applyToDesiredCapabilities=true \
                            -webui.autoUpdateDrivers=true \
                            -g_appiumDriverUrl=http://localhost:4723
                    """
                }
            }
        }

        stage('Send Results to Qase') {
            steps {
                sh '''
                    runId=$(cat qase_run_id.txt)
                    echo "Sending results to Qase run $runId ..."
                    # TODO: Upload report via Qase CLI or API
                '''
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
