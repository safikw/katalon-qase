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

        stage('Setup Appium Environment') {
            steps {
                echo 'Preparing Appium temp folder and drivers...'
                sh '''
                mkdir -p /tmp/Katalon/Appium

                set +e
                DRIVER_INSTALLED=$(appium driver list --installed | grep uiautomator2)
                if [ -z "$DRIVER_INSTALLED" ]; then
                    echo "Installing uiautomator2 driver..."
                    appium driver install uiautomator2
                else
                    echo "Driver uiautomator2 already installed, skipping..."
                fi
                set -e

                appium driver list
                '''
            }
        }

        stage('Create Qase Run') {
            steps {
                withCredentials([string(credentialsId: 'QASE_API_TOKEN', variable: 'QASE_API_TOKEN')]) {
                    sh """
                    echo "Creating new Qase run..."
                    curl -s -X POST https://api.qase.io/v1/run/${QASE_PROJECT_CODE} \
                        -H "Token: $QASE_API_TOKEN" \
                        -H "Content-Type: application/json" \
                        -d '{ "title": "Jenkins Run #${BUILD_NUMBER}" }' \
                        > qase_run.json
                    runId=\$(jq -r .result.id qase_run.json)
                    echo "Created Qase Run ID = \$runId"
                    """
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
                        --config -g_appiumDriverUrl=${APP_DRIVER_URL} -g_appiumTmpDir="/tmp/Katalon/Appium"
                    """
                }
            }
        }

        stage('Upload Results to Qase') {
            steps {
                withCredentials([string(credentialsId: 'QASE_API_TOKEN', variable: 'QASE_API_TOKEN')]) {
                    sh """
                    runId=\$(jq -r .result.id qase_run.json)
                    echo "Parsing JUnit and uploading to Qase (runId=\$runId)..."

                    python3 << 'EOF'
import xml.etree.ElementTree as ET
import os, sys, glob

QASE_TOKEN = os.getenv("QASE_API_TOKEN")
PROJECT = "${QASE_PROJECT_CODE}"
run_id = os.popen("jq -r .result.id qase_run.json").read().strip()

files = glob.glob("Reports/*/JUnit_Report.xml")
if not files:
    print("JUnit report not found")
    sys.exit(1)

for report in files:
    print(f"Processing report: {report}")
    tree = ET.parse(report)
    root = tree.getroot()

    for testcase in root.iter("testcase"):
        name = testcase.attrib.get("name", "")
        if "[QASE-" in name:
            try:
                case_id = int(name.split("[QASE-")[1].split("]")[0])
            except Exception:
                continue

            status = "passed"
            if testcase.find("failure") is not None:
                status = "failed"

            cmd = f'''curl -s -X POST "https://api.qase.io/v1/result/{PROJECT}/{run_id}" \
                -H "Content-Type: application/json" \
                -H "Token: {QASE_TOKEN}" \
                -d '{{"case_id": {case_id}, "status": "{status}", "comment": "Executed by Jenkins build ${BUILD_NUMBER}"}}' '''
            os.system(cmd)
            print(f"Uploaded case {case_id} with status {status}")
EOF
                    """
                }
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
