import com.kms.katalon.core.annotation.*
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

        println "🔍 [Qase Debug] Loaded GlobalVariables:"
        println "   runId      = $runId"
        println "   projectCode= $projectCode"
        println "   token      = ${token ? '***MASKED***' : 'NULL'}"

        if (!runId || !projectCode || !token) {
            println "⚠️ Qase config missing, results will NOT be sent"
        } else {
            println "✅ Using Qase runId=$runId, project=$projectCode"
        }
    }

    @AfterTestCase
    def afterTestCase(TestCaseContext testCaseContext) {
        println "🔍 [Qase Debug] AfterTestCase triggered for: ${testCaseContext.getTestCaseId()}"
        println "   Status     = ${testCaseContext.getTestCaseStatus()}"

        if (!runId || !projectCode || !token) {
            println "⚠️ Skip sending result to Qase, missing config"
            return
        }

        def status = testCaseContext.getTestCaseStatus() == "PASSED" ? "passed" : "failed"
        def testCaseName = testCaseContext.getTestCaseId()

        def caseId = extractCaseId(testCaseName)
        println "🔍 [Qase Debug] Extracted caseId = $caseId from testCaseName = $testCaseName"

        if (!caseId) {
            println "⚠️ No Qase case_id found in $testCaseName"
            return
        }

        def payload = [
            case_id: caseId,
            status : status,
            comment: "Executed from Jenkins build " + System.getenv("BUILD_NUMBER")
        ]

        println "📡 [Qase Debug] Sending payload to Qase: " + JsonOutput.prettyPrint(JsonOutput.toJson(payload))

        try {
            def url = new URL("https://api.qase.io/v1/result/${projectCode}/${runId}")
            def connection = url.openConnection()
            connection.setRequestMethod("POST")
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Token", token)
            connection.doOutput = true
            connection.outputStream.write(JsonOutput.toJson(payload).getBytes("UTF-8"))

            def responseCode = connection.responseCode
            def responseText = connection.inputStream.withReader("UTF-8") { it.text }

            println "✅ [Qase Debug] Response Code = $responseCode"
            println "✅ [Qase Debug] Response Body = $responseText"

            println "📡 Sent result for case ${caseId} = ${status}"
        } catch (Exception e) {
            println "❌ Failed to send result to Qase: " + e.message
            e.printStackTrace()
        }
    }

    private Integer extractCaseId(String name) {
        // Format test case di Katalon: "TC01 [QASE-123]"
        if (name.contains("[QASE-")) {
            try {
                return Integer.parseInt(name.split("\\[QASE-")[1].split("]")[0])
            } catch (Exception ignored) {
                println "⚠️ [Qase Debug] Failed to parse caseId from name=$name"
            }
        }
        return null
    }
}
