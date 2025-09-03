import com.kms.katalon.core.annotation.*
import com.kms.katalon.core.configuration.RunConfiguration
import com.kms.katalon.core.context.TestCaseContext

import groovy.json.JsonOutput
import internal.GlobalVariable

class QaseListener {
    static String runId
    static String projectCode
    static String token

    @BeforeTestSuite
    def beforeSuite() {
        runId = GlobalVariable.runId
        projectCode = GlobalVariable.projectCode
        token = GlobalVariable.qaseToken

        if (!runId || !projectCode || !token) {
            println "⚠️ Qase config missing: runId=$runId, projectCode=$projectCode, token=$token"
        } else {
            println "✅ Using Qase runId=$runId, project=$projectCode"
        }
    }

    @AfterTestCase
    def afterTestCase(TestCaseContext testCaseContext) {
        if (!runId || !projectCode || !token) {
            println "⚠️ Skip sending result to Qase, missing config"
            return
        }

        def status = testCaseContext.getTestCaseStatus() == "PASSED" ? "passed" : "failed"
        def testCaseName = testCaseContext.getTestCaseId()

        def caseId = extractCaseId(testCaseName)
        if (!caseId) {
            println "⚠️ No Qase case_id found in $testCaseName"
            return
        }

        def payload = [
            case_id: caseId,
            status : status,
            comment: "Executed from Jenkins build " + System.getenv("BUILD_NUMBER")
        ]

        try {
            def url = new URL("https://api.qase.io/v1/result/${projectCode}/${runId}")
            def connection = url.openConnection()
            connection.setRequestMethod("POST")
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Token", token)
            connection.doOutput = true
            connection.outputStream.write(JsonOutput.toJson(payload).getBytes("UTF-8"))

            println "📡 Sent result for case ${caseId} = ${status}"
        } catch (Exception e) {
            println "❌ Failed to send result to Qase: " + e.message
        }
    }

    private Integer extractCaseId(String name) {
        // Format test case di Katalon: "TC01 [QASE-123]"
        if (name.contains("[QASE-")) {
            try {
                return Integer.parseInt(name.split("\\[QASE-")[1].split("]")[0])
            } catch (Exception ignored) { }
        }
        return null
    }
}
