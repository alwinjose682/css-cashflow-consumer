package io.alw.css.cashflowconsumer.model.jpa;

import io.alw.css.domain.cashflow.RevisionType;
import io.alw.css.domain.common.PaymentConstants;
import io.alw.css.domain.common.YesNo;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "CASHFLOW", schema = "CSS")
public class CashflowEntity {

    // CSS Cashflow Version Data
    @EmbeddedId
    CashflowEntityPK cashflowEntityPK;

    @Column(name = "LATEST", nullable = false, length = 1)
    @Enumerated(EnumType.STRING)
    YesNo latest; // The latest field is intended to be used only by CSS Services that synchronizes Cashflow processing by acquiring a lock

    @Column(name = "REVISION_TYPE", nullable = false)
    @Enumerated(EnumType.STRING)
    RevisionType revisionType;

    // Fo Cashflow Version Data
    @Column(name = "FO_CASHFLOW_ID", nullable = false)
    Long foCashflowID;

    @Column(name = "FO_CASHFLOW_VERSION", nullable = false)
    Integer foCashflowVersion;

    @Column(name = "TRADE_ID")
    Long tradeID;

    @Column(name = "trade_version")
    Integer tradeVersion;

    // Trade and Cashflow Data
    @Column(name = "TRADE_TYPE")
    String tradeType;

    @Column(name = "BOOK_CODE")
    String bookCode;

    @Column(name = "COUNTER_BOOK_CODE")
    String counterBookCode;

    @Column(name = "SECONDARY_LEDGER_ACCOUNT")
    String secondaryLedgerAccount;

    @Column(name = "TRANSACTION_TYPE")
    String transactionType;

    @Column(name = "RATE", scale = PaymentConstants.RATE_SCALE)
    BigDecimal rate;

    @Column(name = "VALUE_DATE")
    LocalDate valueDate;

    @OneToMany(mappedBy = "cashflow", fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    List<TradeLinkEntity> tradeLinks;

    // ObligationData
    @Column(name = "ENTITY_CODE")
    String entityCode;

    @Column(name = "COUNTERPARTY_CODE")
    String counterpartyCode;

    @Column(name = "AMOUNT", scale = PaymentConstants.AMOUNT_SCALE)
    BigDecimal amount;

    @Column(name = "CURR_CODE")
    String currCode;

    // EnrichmentData
    @Column(name = "INTERNAL", nullable = false, length = 1)
    @Enumerated(EnumType.STRING)
    YesNo internal;

    @Column(name = "NOSTRO_ID")
    String nostroID;

    @Column(name = "SSI_ID")
    String ssiID;

    @Column(name = "PAYMENT_SUPPRESSION_CATEGORY")
    @Enumerated(EnumType.STRING)
    String paymentSuppressionCategory;

    // Cashflow Entry Audit
    @Column(name = "INPUT_BY", nullable = false)
    String inputBy;

    @Column(name = "INPUT_BY_USER_ID")
    String inputByUserID;

    @Column(name = "INPUT_DATE_TIME")
    LocalDateTime inputDateTime;
}
