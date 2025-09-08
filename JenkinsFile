pipeline {
    agent any

    environment {
        KATALON_HOME = '/opt/Katalon_Studio_Engine_Linux_arm64-10.2.4'
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

        stage('Detect Device') {
            steps {
                script {
                    if (isUnix()) {
                        def deviceId = sh(
                            script: "adb devices | awk 'NR>1 && \$2==\"device\" {print \$1; exit}'",
                            returnStdout: true
                        ).trim()
                        if (!deviceId) {
                            error("❌ No Android device is online!")
                        }
                        env.DEVICE_IP = deviceId
                        echo "✅ Found device: ${deviceId}"
                    } else {
                        def deviceId = bat(
                            script: 'for /f "skip=1 tokens=1" %a in (\'adb devices\') do @echo %a & goto :done\n:done',
                            returnStdout: true
                        ).trim()
                        if (!deviceId) {
                            error("❌ No Android device is online!")
                        }
                        env.DEVICE_IP = deviceId
                        echo "✅ Found device: ${deviceId}"
                    }
                }
            }
        }

        stage('Run Katalon Test') {
            steps {
                withCredentials([
                    string(credentialsId: 'KATALON_API_KEY', variable: 'KATALON_API_KEY'),
                    string(credentialsId: 'QASE_API_TOKEN', variable: 'QASE_API_TOKEN')
                ]) {
                    script {
                        if (!params.QASE_RUN_ID?.trim()) {
                            error("❌ QASE_RUN_ID tidak diterima dari Qase!")
                        }
                        echo "ℹ️ Running tests for Qase Run ID: ${params.QASE_RUN_ID}"

                        if (isUnix()) {
                            sh """
                            ${KATALON_HOME}/katalonc -noSplash -runMode=console \
                                -projectPath="${PROJECT_PATH}" \
                                -retry=0 \
                                -testSuitePath="${TEST_SUITE}" \
                                -browserType=Android \
                                -deviceId=${DEVICE_IP} \
                                -executionProfile=default \
                                -apiKey=${KATALON_API_KEY} \
                                --config -g_appiumDriverUrl=${APP_DRIVER_URL} -g_appiumTmpDir="/tmp/Katalon/Appium" \
                                -g_runId=${params.QASE_RUN_ID} \
                                -g_qaseToken=$QASE_API_TOKEN \
                                -g_projectCode=${QASE_PROJECT_CODE}
                            """
                        } else {
                            bat """
                            "%KATALON_HOME%\\katalonc" -noSplash -runMode=console ^
                                -projectPath="%PROJECT_PATH%" ^
                                -retry=0 ^
                                -testSuitePath="%TEST_SUITE%" ^
                                -browserType=Android ^
                                -deviceId=%DEVICE_IP% ^
                                -executionProfile=default ^
                                -apiKey=%KATALON_API_KEY% ^
                                --config -g_appiumDriverUrl=%APP_DRIVER_URL% -g_appiumTmpDir="%TEMP%\\\\Katalon\\\\Appium" ^
                                -g_runId=%QASE_RUN_ID% ^
                                -g_qaseToken=%QASE_API_TOKEN% ^
                                -g_projectCode=%QASE_PROJECT_CODE%
                            """
                        }
                    }
                }
            }
        }

        stage('Close Qase Run') {
            steps {
                withCredentials([string(credentialsId: 'QASE_API_TOKEN', variable: 'QASE_API_TOKEN')]) {
                    script {
                        if (isUnix()) {
                            sh """
                            curl -s -X PATCH https://api.qase.io/v1/run/${QASE_PROJECT_CODE}/${params.QASE_RUN_ID} \
                                -H "Token: $QASE_API_TOKEN" \
                                -H "Content-Type: application/json" \
                                -d '{ "status": "completed" }'
                            """
                        } else {
                            bat """
                            powershell -Command "Invoke-RestMethod -Uri https://api.qase.io/v1/run/${QASE_PROJECT_CODE}/%QASE_RUN_ID% -Method PATCH -Headers @{Token='$QASE_API_TOKEN';'Content-Type'='application/json'} -Body '{\"status\":\"completed\"}'"
                            """
                        }
                    }
                }
            }
        }
    }

    post {
        always { echo "Pipeline finished." }
        success { echo "✅ Build succeeded!" }
        failure { echo "❌ Build failed!" }
    }
}
