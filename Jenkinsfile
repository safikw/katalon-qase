pipeline {
    agent any

    parameters {
        string(name: 'RUN_ID', defaultValue: '', description: 'Qase Run ID (kosongkan kalau mau bikin baru)')
    }

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

        stage('Setup Appium Environment') {
            when { expression { isUnix() } }
            steps {
                sh '''
                mkdir -p /tmp/Katalon/Appium

                DRIVER_INSTALLED=$(appium driver list --installed | grep uiautomator2)
                if [ -z "$DRIVER_INSTALLED" ]; then
                    appium driver install uiautomator2
                fi

                appium driver list
                '''
            }
        }

        stage('Setup Appium Environment (Windows)') {
            when { expression { !isUnix() } }
            steps {
                echo 'Skipping Appium setup on Windows. Make sure Appium is installed manually.'
            }
        }

        stage('Create or Use Qase Run') {
            steps {
                withCredentials([string(credentialsId: 'QASE_API_TOKEN', variable: 'QASE_API_TOKEN')]) {
                    script {
                        if (params.RUN_ID?.trim()) {
                            echo "ℹ️ Using existing Qase run: ${params.RUN_ID}"
                            writeFile file: 'run_id.txt', text: params.RUN_ID
                        } else {
                            echo "📌 No run ID provided. Creating new run in Qase..."
                            if (isUnix()) {
                                sh """
                                curl -s -X POST https://api.qase.io/v1/run/${QASE_PROJECT_CODE} \
                                    -H "Token: $QASE_API_TOKEN" \
                                    -H "Content-Type: application/json" \
                                    -d '{ "title": "Jenkins Run #${BUILD_NUMBER}" }' \
                                    > qase_run.json
                                runId=\$(jq -r .result.id qase_run.json)
                                echo \$runId > run_id.txt
                                """
                            } else {
                                bat """
                                powershell -Command "\$r=Invoke-RestMethod -Uri https://api.qase.io/v1/run/${QASE_PROJECT_CODE} -Method POST -Headers @{Token='$QASE_API_TOKEN';'Content-Type'='application/json'} -Body '{\"title\":\"Jenkins Run #${BUILD_NUMBER}\"}'; \$r.result.id | Out-File run_id.txt -Encoding ascii"
                                """
                            }
                        }
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
                        if (isUnix()) {
                            sh """
                            runId=\$(cat run_id.txt)
                            ${KATALON_HOME}/katalonc -noSplash -runMode=console \
                                -projectPath="${PROJECT_PATH}" \
                                -retry=0 \
                                -testSuitePath="${TEST_SUITE}" \
                                -browserType=Android \
                                -deviceId=${DEVICE_IP} \
                                -executionProfile=default \
                                -apiKey=${KATALON_API_KEY} \
                                --config -g_appiumDriverUrl=${APP_DRIVER_URL} -g_appiumTmpDir="/tmp/Katalon/Appium" \
                                -g_runId=\$runId \
                                -g_qaseToken=$QASE_API_TOKEN \
                                -g_projectCode=${QASE_PROJECT_CODE}
                            """
                        } else {
                            bat """
                            set /p runId=<run_id.txt
                            "%KATALON_HOME%\\katalonc" -noSplash -runMode=console ^
                                -projectPath="%PROJECT_PATH%" ^
                                -retry=0 ^
                                -testSuitePath="%TEST_SUITE%" ^
                                -browserType=Android ^
                                -deviceId=%DEVICE_IP% ^
                                -executionProfile=default ^
                                -apiKey=%KATALON_API_KEY% ^
                                --config -g_appiumDriverUrl=%APP_DRIVER_URL% -g_appiumTmpDir="%TEMP%\\\\Katalon\\\\Appium" ^
                                -g_runId=%runId% ^
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
                            sh '''
                            runId=$(cat run_id.txt)
                            curl -s -X PATCH https://api.qase.io/v1/run/${QASE_PROJECT_CODE}/$runId \
                                -H "Token: $QASE_API_TOKEN" \
                                -H "Content-Type: application/json" \
                                -d '{ "status": "completed" }'
                            '''
                        } else {
                            bat """
                            set /p runId=<run_id.txt
                            powershell -Command "Invoke-RestMethod -Uri https://api.qase.io/v1/run/${QASE_PROJECT_CODE}/$runId -Method PATCH -Headers @{Token='$QASE_API_TOKEN';'Content-Type'='application/json'} -Body '{\"status\":\"completed\"}'"
                            """
                        }
                    }
                }
            }
        }
    }

    post {
        always {
            echo "Pipeline finished."
        }
        success {
            echo "✅ Build succeeded!"
        }
        failure {
            echo "❌ Build failed!"
        }
    }
}
