pipeline {
    agent any

    environment {
        QASE_PROJECT_CODE = "MKQ"
        QASE_API_TOKEN    = credentials('QASE_API_TOKEN')
        KATALON_API_KEY   = credentials('KATALON_API_KEY')
        KATALON_HOME      = "/opt/Katalon_Studio_Engine_Linux_arm64-10.2.4"
    }

    stages {  
        stage('Check Devices & Prepare Katalon') {
            steps {
                sh '''
                  echo ">> Start adb bawaan container"
                  adb start-server
                  adb devices

                  PLATFORM_TOOLS_DIR="$KATALON_HOME/configuration/resources/tools/android/sdk/platform-tools"
                  echo ">> Memastikan direktori $PLATFORM_TOOLS_DIR ada"
                  mkdir -p "$PLATFORM_TOOLS_DIR"

                  echo ">> Paksa Katalon pakai adb bawaan container"
                  ln -sf $(which adb) "$PLATFORM_TOOLS_DIR/adb"

                  echo ">> Cek devices pakai adb yang dipakai Katalon"
                  "$PLATFORM_TOOLS_DIR/adb" devices || true
                '''
            }
        }

        stage('Create Qase Run') {
            steps {
                sh '''
                  echo "Creating new Qase run..."
                  response=$(curl -s -X POST "https://api.qase.io/v1/run/$QASE_PROJECT_CODE" \
                    -H "Token: $QASE_API_TOKEN" \
                    -H "Content-Type: application/json" \
                    -d "{ \\"title\\": \\"Jenkins Run #${BUILD_NUMBER}\\", \\"description\\": \\"Automated test run from Jenkins build: ${BUILD_URL}\\" }")
                  
                  echo "$response" > qase_run.json
                  
                  if [ "$(jq -r '.status' qase_run.json)" != "true" ]; then
                    echo "Gagal membuat Qase Run. Respons:"
                    cat qase_run.json
                    exit 1
                  fi
                  
                  runId=$(jq -r '.result.id' qase_run.json)
                  echo "Created Qase Run ID = $runId"
                  echo $runId > qase_run_id.txt
                '''
            }
        }

        stage('Run Katalon Tests') {
            steps {
                sh '''
                  echo ">> Ambil device dari adb"
                  adb wait-for-device
                  DEVICE_ID=$(adb devices | awk 'NR==2 {print $1}')

                  if [ -z "$DEVICE_ID" ]; then
                    echo "Device tidak ditemukan!"
                    exit 1
                  fi
                  echo "Using device: $DEVICE_ID"

                  SERIAL=$(adb -s $DEVICE_ID shell getprop ro.serialno | tr -d '\\r\\n')
                  echo "Device Serial: $SERIAL"

                  "$KATALON_HOME/katalonc" \\
                    -projectPath="$(pwd)/Android Mobile Tests with Katalon Studio.prj" \\
                    -testSuitePath="Test Suites/Smoke Tests for Mobile Browsers" \\
                    -executionProfile="default" \\
                    -browserType="Android" \\
                    -reportFolder="Reports" \\
                    -apiKey="$KATALON_API_KEY" \\
                    -g_appiumDriverUrl="http://host.docker.internal:4723" \\
                    -g_udid="$SERIAL"
                '''
            }
        }

        stage('Send Results to Qase') {
            steps {
                sh '''
                  runId=$(cat qase_run_id.txt)
                  echo "Sending results to Qase run $runId ..."
                  
                  REPORT_PATH=$(find Reports -name "JUnit_Report.xml" | head -n 1)

                  if [ -z "$REPORT_PATH" ]; then
                    echo "Laporan JUnit tidak ditemukan!"
                    exit 1
                  fi
                  
                  echo "Uploading report: $REPORT_PATH"
                  
                  curl -X POST "https://api.qase.io/v1/result/$QASE_PROJECT_CODE/$runId/bulk" \
                    -H "Token: $QASE_API_TOKEN" \
                    -H "Content-Type: application/json" \
                    -d @<(katalon-qase-reporter -p "$REPORT_PATH")
                  
                  echo "TODO: Implement a proper JUnit XML to Qase JSON converter if needed."
                '''
            }
        }

        stage('Archive Reports') {
            steps {
                archiveArtifacts artifacts: 'Reports/**, qase_run.json, qase_run_id.txt', followSymlinks: false
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

