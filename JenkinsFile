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
                // Gunakan 'script' block untuk mengizinkan penulisan skrip Groovy (try...finally)
                script {
                    def appiumPID // Variabel untuk menyimpan ID proses Appium

                    try {
                        // 1. Mulai server Appium di background (&)
                        // Outputnya disimpan ke file log untuk debugging jika diperlukan
                        echo "Starting Appium server in the background..."
                        sh 'appium > appium-server-log.txt 2>&1 &'
                        
                        // Dapatkan Process ID (PID) dari proses Appium yang baru saja dijalankan
                        appiumPID = sh(script: 'echo $!', returnStdout: true).trim()
                        echo "Appium started with PID: ${appiumPID}"

                        // 2. Beri waktu beberapa detik agar Appium benar-benar siap
                        echo "Waiting for Appium server to initialize..."
                        sleep(20) // Waktu tunggu 20 detik, bisa disesuaikan

                        // 3. Jalankan perintah Katalon Anda seperti biasa
                        // Katalon akan otomatis terhubung ke Appium yang sudah berjalan
                        withCredentials([string(credentialsId: 'KATALON_API_KEY', variable: 'KATALON_API_KEY')]) {
                            sh """
                                echo "Using device: $DEVICE_IP"

                                $KATALON_HOME/katalonc \\
                                    -noSplash \\
                                    -runMode=console \\
                                    -projectPath="$PROJECT_PATH" \\
                                    -retry=0 \\
                                    -testSuitePath="$TEST_SUITE" \\
                                    -browserType="Android" \\
                                    -deviceId="$DEVICE_IP" \\
                                    -executionProfile="default" \\
                                    -apiKey="\$KATALON_API_KEY" \\
                                    --config -g_appiumDriverUrl=$APP_DRIVER_URL
                            """
                        }

                    } finally {
                        // 4. Matikan server Appium setelah tes selesai (baik berhasil maupun gagal)
                        echo "Cleaning up and stopping the Appium server..."
                        if (appiumPID) {
                            sh "kill ${appiumPID}"
                        }
                    }
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
                archiveArtifacts artifacts: 'qase_run*.txt, appium-server-log.txt', followSymlinks: false
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