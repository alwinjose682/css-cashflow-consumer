package io.alw.css.cashflowconsumer.model.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@ConfigurationProperties("app.cfc.suppress")
//@ConfigurationPropertiesScan("io.alw.css.configprops")
public class SuppressionConfig {

    //    @Value("#{${app.cfc.suppress.uptoAmount}}")
    private final Map<String, BigDecimal> suppressibleCurrToAmountMap;
    private final boolean suppressInterbookTX;
    private final BigDecimal highestSuppressibleAmount;

    @ConstructorBinding
    public SuppressionConfig(Map<String, String> uptoAmount, Boolean interbookTx) {
        this.suppressibleCurrToAmountMap = new HashMap<>();
        this.suppressInterbookTX = interbookTx;

        BigDecimal[] highestAmount = new BigDecimal[]{new BigDecimal("0.0")};
        uptoAmount.forEach((curr, amountStr) -> {
            BigDecimal amount = new BigDecimal(amountStr); // BigDecimal is created by passing a String, which is correct
            if (amount.compareTo(highestAmount[0]) > 0) {
                highestAmount[0] = amount;
            }
            this.suppressibleCurrToAmountMap.put(curr.toUpperCase(Locale.ROOT), amount);
        });

        this.highestSuppressibleAmount = highestAmount[0];
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
