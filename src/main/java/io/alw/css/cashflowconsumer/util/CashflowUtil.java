package io.alw.css.cashflowconsumer.util;

import io.alw.css.domain.cashflow.CashflowConstants;

public final class CashflowUtil {
    public static boolean isFirstFoCashflowVersion(int foCashflowVersion) {
        return foCashflowVersion == CashflowConstants.FO_CASHFLOW_FIRST_VERSION;
    }
}
