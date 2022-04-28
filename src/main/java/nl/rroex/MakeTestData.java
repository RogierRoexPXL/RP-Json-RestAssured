package nl.rroex;

import com.google.gson.Gson;
import org.apache.commons.lang3.RandomStringUtils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;


public class MakeTestData {
    private final static Random random = new Random();
    private final static long AMOUNT_OF_LINES = 10000;
    private final static String DESTINATION_FILE = "./src/test/resources/invoices/MockInvoicesArrays.json";

    public static void main(String[] args) {
        Gson gson = new Gson();

        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(DESTINATION_FILE))) {
            for (int i = 0; i < AMOUNT_OF_LINES; i++) {
                writer.write(gson.toJson(getRandomInvoice()) + ",");
                writer.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Invoice getRandomInvoice() {
        //TotalAmount - BigDecimal
        BigDecimal totalAmount = BigDecimal.valueOf(random.nextFloat(999999.99f)).setScale(2, RoundingMode.HALF_EVEN);
        //CompanyName - String
        String companyName = generateString(random.nextInt(5, 15));
        //Comment - ArrayList<String>
        List<String> list = new ArrayList<>();
        int rnd = random.nextInt(10);
        for (int j = 0; j < rnd; j++) {
            int length = random.nextInt(20000);
            String comment = generateString(length);
            list.add(comment);
        }

        return new Invoice(totalAmount, companyName, list);
    }

    private static String generateString(int length) {
        return RandomStringUtils.randomAlphanumeric(length);
    }
}
