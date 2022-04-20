package nl.rroex;

import io.restassured.RestAssured;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;

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

public class JsonTest {

    public static final String SOURCE_MOCK_JSON_DATA_SMALL = "src/test/resources/invoices/MockInvoicesSmall.json";
    public static final String SOURCE_MOCK_JSON_DATA_MEDIUM = "src/test/resources/invoices/MockInvoicesMedium.json";
    public static final String SOURCE_MOCK_JSON_DATA_LARGE = "src/test/resources/invoices/MockInvoicesLarge.json";
    private static String PARSER;
    private static String dataSource;

    private static List<String> testData;

    @BeforeClass
    public static void setUp() {
        PARSER = "fastJson";
//        dataSource = SOURCE_MOCK_JSON_DATA_SMALL;
//        dataSource = SOURCE_MOCK_JSON_DATA_MEDIUM;
        dataSource = SOURCE_MOCK_JSON_DATA_LARGE;
        RestAssured.baseURI = "http://localhost:8080/invoice";
        testData = provideTestData();
    }

    @Test
    public void postShouldRespondOk_MoreThan95Percent() {
        // STEP 1: call API with testdata-set
        Map<Integer, Integer> results = startPostApiCalls();

        // STEP 2: report
        printTestReport(results, "POST");

        // STEP 3: assertions
        assertThat(percentileOkResponses(results), is(greaterThan(95.0)));
    }

    @Test
    public void getShouldReturnCorrectData_MoreThan95Percent() {
        // STEP 1: call API with testdata-set
        HashMap<String, Integer> fails = startGetApiCalls();

        // STEP 2: report
        printTestReport(fails, "GET");

        // STEP 3: assertions
        assertThat(fails.get("id"), is(lessThan( 50)));
        assertThat(fails.get("totalAmount"), is(lessThan( 50)));
        assertThat(fails.get("companyName"), is(lessThan( 50)));
        assertThat(fails.get("comment"), is(lessThan( 50)));
    }


    //#region helper methods
    private static List<String> provideTestData() {
        testData = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(Path.of(dataSource))) {
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
        for (String line : testData) {
            int statusCode = given().contentType("application/json")
                    .when()
                    .body(line)
                    .post().getStatusCode();

            // update statuscode data
            if (!map.containsKey(statusCode)) {
                map.put(statusCode, 1);
            } else {
                map.put(statusCode, map.get(statusCode) + 1);
            }
        }
        return map;
    }

    private HashMap<String, Integer> startGetApiCalls() {
        HashMap<String, Integer> fails = new HashMap<>() {{
            put("id", 0);
            put("totalAmount", 0);
            put("companyName", 0);
            put("comment", 0);
        }};
        for (int i = 0; i < testData.size(); i++) {
            int id = i + 1;
            String line = testData.get(i);
            Object[] values = getValues(line);

            String body = given().contentType("application/json")
                    .get("/" + id)
                    .body().asString();

            JSONObject jsonObject = new JSONObject(body);
            if (id != jsonObject.getInt("id")) {
                fails.put("id", fails.get("id") + 1);
            }
            if (0 != jsonObject.getBigDecimal("totalAmount").compareTo((BigDecimal) values[0])) {
                fails.put("totalAmount", fails.get("totalAmount") + 1);
            }
            if (!jsonObject.getString("companyName").equals(values[1])) {
                fails.put("companyName", fails.get("companyName") + 1);
            }
            if (!jsonObject.getString("comment").equals(values[2])) {
                fails.put("comment", fails.get("comment") + 1);
            }
        }
        return fails;
    }

    private Object[] getValues(String line) throws JSONException {
        JSONObject jsonObject = new JSONObject(line);
        return new Object[]{
                jsonObject.getBigDecimal("totalAmount"),
                jsonObject.get("companyName"),
                jsonObject.get("comment")
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

    private void printTestReport(Map<?, ?> map, String caller) {
        System.out.println("Parser: " + PARSER);
        System.out.println("Request: " + caller);
        System.out.println("Datasource: " + dataSource);
        System.out.println("######################");

        for (Map.Entry<?, ?> entry : map.entrySet()) {
            System.out.println(entry.getKey() + " => " + entry.getValue());
        }

        System.out.println("######################");
    }
    //#endregion
}
