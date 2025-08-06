package io.alw.css.cashflowconsumer.model.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@ConfigurationProperties("app.cfc.suppress")
public class SuppressionConfig {

    //    @Value("#{${app.cfc.suppress.uptoAmount}}")
    private final Map<String, BigDecimal> suppressibleCurrToAmountMap;
    private final boolean suppressInterbookTX;
    private final BigDecimal highestSuppressibleAmount;

    @ConstructorBinding
    public SuppressionConfig(String uptoAmount, Boolean interbookTx) {
        this.suppressibleCurrToAmountMap = new HashMap<>();
        this.suppressInterbookTX = interbookTx;

        BigDecimal[] highestAmount = new BigDecimal[]{new BigDecimal("0.0")};

        getUptoAmountAsMap(uptoAmount).forEach((curr, amountStr) -> {
            BigDecimal amount = new BigDecimal(amountStr).abs(); // BigDecimal is created by passing a String, which is correct
            if (amount.compareTo(highestAmount[0]) > 0) {
                highestAmount[0] = amount;
            }
            this.suppressibleCurrToAmountMap.put(curr.toUpperCase(Locale.ROOT), amount);
        });

        this.highestSuppressibleAmount = highestAmount[0];
    }

    private Map<String, String> getUptoAmountAsMap(String uptoAmount) {
        try {
            String prop = uptoAmount.replace("{", "").replace("}", "").replace("'", "").replace("\"", "");
            String[] indProps = prop.split(",");
            var propMap = new HashMap<String, String>();
            for (String indProp : indProps) {
                String[] kv = indProp.trim().split(":");
                propMap.put(kv[0], kv[1]);
            }
            return propMap;
        } catch (Exception e) {
            throw new RuntimeException("Property configuration format of property: 'uptoAmount' is invalid. Exception: " + e.getMessage() + System.lineSeparator()
                    + " Formatting rules are:" + System.lineSeparator()
                    + " 1) key(curr) and value(amount) should be delimited with ':'" + System.lineSeparator()
                    + " 2) multiple key-value pairs should be delimeted by ','" + System.lineSeparator()
                    + " 3) Any occurrence of following will be ignored: '{' '}' ''' and '\"'" + System.lineSeparator());
        }
    }

    public Map<String, BigDecimal> suppressibleCurrToAmountMap() {
        return suppressibleCurrToAmountMap;
    }

    public boolean suppressInterbookTX() {
        return suppressInterbookTX;
    }

    /// highest suppressible amount irrespective of currCode
    public BigDecimal highestSuppressibleAmount() {
        return highestSuppressibleAmount;
    }
}
