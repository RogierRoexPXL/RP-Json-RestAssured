package nl.rroex;

import io.restassured.RestAssured;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class JsonTest {

    private static final String PARSER = "json-java";
    private static final int DATA_AMOUNT = 1001;
    private Path invoicesJson;

    @Before
    public void setUp() {
        RestAssured.baseURI = "http://localhost:8080/invoice";
//        RestAssured.port = 443;
        invoicesJson = Paths.get("src/test/java/resources/MOCK_DATA.json");
    }

    @Test
    public void shouldRespondStatusOkMoreThan95Percent() {
        // STEP 1: call API with testdata-set
        Map<Integer, Integer> results = startTestApiCalls();

        // STEP 2: report
        printTestReport(results);

        // STEP 3: assertions
        assertThat(percentileOkResponses(results), is(greaterThan(95.0)));
    }


    //#region helper methods
    private Map<Integer, Integer> startTestApiCalls() {
        Map<Integer, Integer> map = new HashMap<>();

        try (BufferedReader reader = Files.newBufferedReader(invoicesJson)) {
            int counter = 0;
            String line;

            while ((line = reader.readLine()) != null) {
                line = removeCommaAfterJsonFile(counter, line);
                counter++;

                int statusCode = given().contentType("application/json")
                        .body(line)
                        .post().getStatusCode();

                // update statuscode data
                if (!map.containsKey(statusCode)) {
                    map.put(statusCode, 1);
                } else {
                    map.put(statusCode, map.get(statusCode) + 1);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return map;
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

    private String removeCommaAfterJsonFile(int amount, String line) {
        if (amount != DATA_AMOUNT - 1) {
            line = line.substring(0, line.length() - 1);
        }
        return line;
    }

    private void printTestReport(Map<Integer, Integer> map) {
        String title = "####### " + PARSER + " #######";
        System.out.println(title);
        System.out.println("Status codes report:");

        for (Map.Entry<Integer, Integer> entry : map.entrySet()) {
            System.out.println(entry.getKey() + " => " + entry.getValue());
        }

        System.out.println("#".repeat(title.length()));
    }
    //#endregion
}