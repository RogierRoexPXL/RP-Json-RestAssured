package nl.rroex;

import java.math.BigDecimal;
import java.util.List;

public class Invoice {

    private Long id;

    private final BigDecimal totalAmount;

    private final String companyName;

    private final List<String> comment;

    public Invoice(BigDecimal totalAmount, String companyName, List<String> comment) {
        this.totalAmount = totalAmount;
        this.companyName = companyName;
        this.comment = comment;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public String getCompanyName() {
        return companyName;
    }

    public List<String> getComment() {
        return comment;
    }
}
