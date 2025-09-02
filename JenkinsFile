pipeline {
    agent any

    environment {
        KATALON_HOME = '/opt/Katalon_Studio_Engine_Linux_arm64-10.2.4'
        DEVICE_IP = '172.20.10.8:7401'
        APP_DRIVER_URL = 'http://localhost:4723'
        TEST_SUITE = 'Test Suites/Smoke Tests for API Demos App'
        PROJECT_PATH = "${WORKSPACE}/Android Mobile Tests with Katalon Studio.prj"
        QASE_PROJECT_CODE = 'MKQ'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Check Device') {
            steps {
                sh 'adb devices'
            }
        }

stage('Setup Environment') {
    echo 'Installing Appium drivers if not installed...'
    sh '''
    set +e  # jangan exit saat perintah gagal
    DRIVER_INSTALLED=$(appium driver list --installed | grep uiautomator2)
    if [ -z "$DRIVER_INSTALLED" ]; then
        echo "Installing uiautomator2 driver..."
        appium driver install uiautomator2
    else
        echo "Driver uiautomator2 already installed, skipping..."
    fi
    set -e  # kembali exit on error
    appium driver list
    '''
}



        stage('Start Appium Server') {
            steps {
                echo 'Starting Appium server...'
                sh '''
                appium > appium-server-log.txt 2>&1 &
                APP_PID=$!
                echo $APP_PID > appium.pid
                sleep 15
                '''
            }
        }

        stage('Create Qase Run') {
            steps {
                withCredentials([string(credentialsId: 'QASE_API_TOKEN', variable: 'QASE_API_TOKEN')]) {
                    sh '''
                    echo "Creating new Qase run..."
                    curl -s -X POST https://api.qase.io/v1/run/${QASE_PROJECT_CODE} \
                        -H "Token: $QASE_API_TOKEN" \
                        -H "Content-Type: application/json" \
                        -d '{ "title": "Jenkins Run #${BUILD_NUMBER}" }' \
                        > qase_run.json
                    runId=$(jq -r .result.id qase_run.json)
                    echo "Created Qase Run ID = $runId"
                    '''
                }
            }
        }

        stage('Run Katalon Test') {
            steps {
                withCredentials([string(credentialsId: 'KATALON_API_KEY', variable: 'KATALON_API_KEY')]) {
                    sh """
                    ${KATALON_HOME}/katalonc -noSplash -runMode=console \
                        -projectPath="${PROJECT_PATH}" \
                        -retry=0 \
                        -testSuitePath="${TEST_SUITE}" \
                        -browserType=Android \
                        -deviceId=${DEVICE_IP} \
                        -executionProfile=default \
                        -apiKey=${KATALON_API_KEY} \
                        --config -g_appiumDriverUrl=${APP_DRIVER_URL}
                    """
                }
            }
        }

        stage('Stop Appium Server') {
            steps {
                sh '''
                if [ -f appium.pid ]; then
                    kill $(cat appium.pid) || true
                    rm appium.pid
                fi
                '''
            }
        }
    }

    post {
        always {
            echo "Pipeline finished."
        }
        success {
            echo "Build succeeded!"
        }
        failure {
            echo "Build failed!"
        }
    }
}
