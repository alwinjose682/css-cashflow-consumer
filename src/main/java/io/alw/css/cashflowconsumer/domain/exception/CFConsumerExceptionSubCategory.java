package io.alw.css.cashflowconsumer.domain.exception;

import io.alw.css.domain.exception.ExceptionConstants;

public interface CFConsumerExceptionSubCategory extends ExceptionConstants.ExceptionSubCategory {
    String EXP_SUB_CATEGORY__CF_COR_AFTER_CAN = "CASHFLOW_AMEND_AFTER_CASHFLOW_OUTRIGHT_CANCELLATION";
}
