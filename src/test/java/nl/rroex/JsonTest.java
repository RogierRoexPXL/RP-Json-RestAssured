package nl.rroex;

import io.restassured.RestAssured;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import java.io.BufferedReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class JsonTest {
    private static final Logger LOGGER = LogManager.getLogger(JsonTest.class);
    private static final String MOCK_DATA = "src/test/resources/invoices/MockInvoicesArrays.json";
    private static String PARSER;
    private static int CYCLES;

    private static List<String> testData;

    @BeforeClass
    public static void setUp() {
        CYCLES = 1;
        PARSER = "java json";
        RestAssured.baseURI = "http://localhost:8080/invoice";
        testData = provideTestData();
    }

    @Test
    public void testA_POST_ShouldRespondOk_MoreThan95Percent() {
        System.out.println("Parser: " + PARSER);
        for (int i = 0; i < CYCLES; i++) {
            // STEP 1: call API with testdata-set
            Map<Integer, Integer> results = startPostApiCalls();

            // STEP 2: report
            printTestReport(results, i + 1);

            // STEP 3: assertions
            assertThat(percentileOkResponses(results), is(greaterThan(95.0)));
        }
    }

    @Test
    public void testB_GET_ShouldReturnCorrectData_MoreThan95Percent() {
        System.out.println("Parser: " + PARSER);
        int fiftyPercent = (int) (testData.size() * 0.95);
        for (int i = 0; i < CYCLES; i++) {
            // STEP 1: call API with testdata-set
            HashMap<String, Integer> results = startGetApiCalls(i);

            // STEP 2: report
            printTestReport(results, i + 1);

            // STEP 3: assertions
            assertThat(results.get("id"), is(lessThan(fiftyPercent)));
            assertThat(results.get("totalAmount"), is(lessThan(fiftyPercent)));
            assertThat(results.get("companyName"), is(lessThan(fiftyPercent)));
            assertThat(results.get("comment"), is(lessThan(fiftyPercent)));
        }
    }


    //#region helper methods
    private static List<String> provideTestData() {
        testData = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(Path.of(MOCK_DATA))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.replace("},", "}");
                testData.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return testData;
    }

    private Map<Integer, Integer> startPostApiCalls() {
        Map<Integer, Integer> map = new HashMap<>();
        for (int i = 0; i < testData.size(); i++) {
            int statusCode = given().contentType("application/json")
                    .when()
                    .body(testData.get(i))
                    .post().getStatusCode();
            LOGGER.info("POST "+ (i + 1) + ": statuscode " + statusCode);
            // update statuscode data
            if (!map.containsKey(statusCode)) {
                map.put(statusCode, 1);
            } else {
                map.put(statusCode, map.get(statusCode) + 1);
            }
        }
        return map;
    }

    private HashMap<String, Integer> startGetApiCalls(int iteration) {
        HashMap<String, Integer> fails = new HashMap<>() {{
            put("id", 0);
            put("totalAmount", 0);
            put("companyName", 0);
            put("comment", 0);
        }};
        int startIndex = 1 + (iteration * testData.size());

        for (int i = 0; i < testData.size(); i++) {
            int id = startIndex + i;
            //test data (LOCAL)
            String line = testData.get(i);
            Object[] testObject = getTestLineValues(line);
            JSONArray testArray = (JSONArray) testObject[2];
            //json data (from server)
            String body = given().contentType("application/json")
                    .get("/" + id)
                    .body().asString();
            JSONObject jsonObject = new JSONObject(body);
            JSONArray jsonArray = jsonObject.getJSONArray("comment");

            boolean commentDataIsCorrect = true;
            for (int j = 0; j < testArray.length(); j++) {
                commentDataIsCorrect = testArray.get(j).equals(jsonArray.get(j));
            }
            LOGGER.info("GET " + id + ": isDataCorrect returns " + commentDataIsCorrect);

            if (id != jsonObject.getInt("id")) {
                fails.put("id", fails.get("id") + 1);
            }
            if (0 != jsonObject.getBigDecimal("totalAmount").compareTo((BigDecimal) testObject[0])) {
                fails.put("totalAmount", fails.get("totalAmount") + 1);
            }
            if (!jsonObject.getString("companyName").equals(testObject[1])) {
                fails.put("companyName", fails.get("companyName") + 1);
            }
            if (!commentDataIsCorrect) {
                fails.put("comment", fails.get("comment") + 1);
            }
        }
        return fails;
    }

    private Object[] getTestLineValues(String line) throws JSONException {
        JSONObject jsonObject = new JSONObject(line);
        return new Object[]{
                jsonObject.getBigDecimal("totalAmount"),
                jsonObject.get("companyName"),
                jsonObject.getJSONArray("comment")
        };
    }

    private Double percentileOkResponses(Map<Integer, Integer> map) {
        int ok = 0, total = 0;
        for (Map.Entry<Integer, Integer> entry : map.entrySet()) {
            if (entry.getKey() == 200) {
                ok += entry.getValue();
            }
            total += entry.getValue();
        }
        return (double) ok / total * 100;
    }

    private void printTestReport(Map<?, ?> map, int iteration) {
        System.out.println("######################");
        System.out.println("**Iteration: " + iteration + "**");
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            System.out.println(entry.getKey() + " => " + entry.getValue());
        }
        System.out.println("######################");
    }
    //#endregion
}
