import com.kms.katalon.core.annotation.*
import com.kms.katalon.core.context.TestCaseContext
import groovy.json.JsonSlurper
import groovy.json.JsonOutput

class QaseListener {

    String projectCode = "MKQ"              // project code Qase
    String token = "ea3305ad9c8d11cbd99d1235d2e1da2c42bd0d798f23280c1f0c75d4c9f779ff"         // API token Qase
    Integer runId                           // run_id (akan auto fetch)

    /**
     * Ambil run terbaru sebelum test case dijalankan
     */
    @BeforeTestSuite
    def fetchLatestRunId() {
        def connection = new URL("https://api.qase.io/v1/run/${projectCode}").openConnection()
        connection.setRequestMethod("GET")
        connection.setRequestProperty("Content-Type", "application/json")
        connection.setRequestProperty("Token", token)

        def response = connection.inputStream.getText("UTF-8")
        def json = new JsonSlurper().parseText(response)

        if (json.status && json.result.entities.size() > 0) {
            runId = json.result.entities[0].id
            println "✅ Latest Run ID = ${runId}"
        } else {
            println "⚠️ Gagal ambil Run ID dari Qase"
        }
    }

    /**
     * Kirim result setelah test case selesai
     */
    @AfterTestCase
    def sendResult(TestCaseContext testCaseContext) {
        if (!runId) {
            println "⚠️ Run ID belum ada, skip send result"
            return
        }

        def status = testCaseContext.getTestCaseStatus() == "PASSED" ? "passed" : "failed"
        def testCaseName = testCaseContext.getTestCaseId()

        // TODO: mapping Katalon test case ke Qase case_id
        def payload = [
            case_id: 2,  // ganti sesuai ID case Qase
            status: status,
            comment: "Executed from Katalon - " + testCaseName
        ]

        def connection = new URL("https://api.qase.io/v1/result/${projectCode}/${runId}").openConnection()
        connection.setRequestMethod("POST")
        connection.setRequestProperty("Content-Type", "application/json")
        connection.setRequestProperty("Token", token)
        connection.doOutput = true
        connection.outputStream.write(JsonOutput.toJson(payload).getBytes("UTF-8"))

        def responseCode = connection.responseCode
        println("📡 Qase API response: " + responseCode)
    }
}
