pipeline {
    agent any

    environment {
        QASE_PROJECT_CODE = "MKQ"
        QASE_API_TOKEN    = credentials('QASE_API_TOKEN')
        KATALON_API_KEY   = credentials('KATALON_API_KEY')
        STF_DEVICE        = "stf:7401"
    }

    stages {  
        stage('Check Devices') {
            steps {
                sh '''
                  adb devices
                '''
            }
        }

        stage('Create Qase Run') {
            steps {
                sh '''
                  echo "Creating new Qase run..."
                  response=$(curl -s -X POST "https://api.qase.io/v1/run/$QASE_PROJECT_CODE" \\
                    -H "Token: $QASE_API_TOKEN" \\
                    -H "Content-Type: application/json" \\
                    -d "{ \\"title\\": \\"Jenkins Run #$BUILD_NUMBER\\" }")
                  echo "$response" > qase_run.json
                  runId=$(jq -r '.result.id' qase_run.json)
                  echo "Created Qase Run ID = $runId"
                  echo $runId > qase_run_id.txt
                '''
            }
        }

        stage('Run Katalon Tests') {
            steps {
                sh '''
                adb start-server
                  KATALON_HOME="/opt/Katalon_Studio_Engine_Linux_arm64-10.2.4"
                  DEVICE_ID=$(adb devices | awk 'NR==2 {print $1}')
                  echo "Using device: $DEVICE_ID"
                  SERIAL=$(adb -s $DEVICE_ID shell getprop ro.serialno | tr -d '\r')
                  echo "📱 Cek devices dengan Katalon:"
                DEVICE=$(/opt/Katalon_Studio_Engine_Linux_arm64-10.2.4/katalonc -listDevices | grep -oP "id:\s*\K\S+")
                  echo "✅ Device terdeteksi: $DEVICE"


                  "$KATALON_HOME/katalonc" \\
                    -projectPath="$(pwd)/Android Mobile Tests with Katalon Studio.prj" \\
                    -testSuitePath="Test Suites/Smoke Tests for Mobile Browsers" \\
                    -executionProfile="default" \\
                    -browserType="Android" \\
                    -deviceId="192.168.1.10:7401" \\
                    -reportFolder=Reports \\
                    -apiKey="$KATALON_API_KEY" \\
                    -g_appiumDriverUrl="http://host.docker.internal:4723" \\

                '''
            }
        }

        stage('Send Results to Qase') {
            steps {
                sh '''
                  runId=$(cat qase_run_id.txt)
                  echo "Sending results to Qase run $runId ..."
                  # Bisa pakai Katalon listener atau curl upload report
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
