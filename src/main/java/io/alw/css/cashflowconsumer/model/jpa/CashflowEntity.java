package io.alw.css.cashflowconsumer.model.jpa;

import io.alw.css.domain.cashflow.RevisionType;
import io.alw.css.domain.common.InputBy;
import io.alw.css.domain.common.PaymentConstants;
import io.alw.css.domain.common.PaymentSuppressionCategory;
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
    PaymentSuppressionCategory paymentSuppressionCategory;

    // Cashflow Entry Audit
    @Column(name = "INPUT_BY", nullable = false)
    @Enumerated(EnumType.STRING)
    InputBy inputBy;

    @Column(name = "INPUT_BY_USER_ID")
    String inputByUserID;

    @Column(name = "INPUT_DATE_TIME")
    LocalDateTime inputDateTime;

    public CashflowEntityPK getCashflowEntityPK() {
        return cashflowEntityPK;
    }

    public void setCashflowEntityPK(CashflowEntityPK cashflowEntityPK) {
        this.cashflowEntityPK = cashflowEntityPK;
    }

    public YesNo getLatest() {
        return latest;
    }

    public void setLatest(YesNo latest) {
        this.latest = latest;
    }

    public RevisionType getRevisionType() {
        return revisionType;
    }

    public void setRevisionType(RevisionType revisionType) {
        this.revisionType = revisionType;
    }

    public Long getFoCashflowID() {
        return foCashflowID;
    }

    public void setFoCashflowID(Long foCashflowID) {
        this.foCashflowID = foCashflowID;
    }

    public Integer getFoCashflowVersion() {
        return foCashflowVersion;
    }

    public void setFoCashflowVersion(Integer foCashflowVersion) {
        this.foCashflowVersion = foCashflowVersion;
    }

    public Long getTradeID() {
        return tradeID;
    }

    public void setTradeID(Long tradeID) {
        this.tradeID = tradeID;
    }

    public Integer getTradeVersion() {
        return tradeVersion;
    }

    public void setTradeVersion(Integer tradeVersion) {
        this.tradeVersion = tradeVersion;
    }

    public String getTradeType() {
        return tradeType;
    }

    public void setTradeType(String tradeType) {
        this.tradeType = tradeType;
    }

    public String getBookCode() {
        return bookCode;
    }

    public void setBookCode(String bookCode) {
        this.bookCode = bookCode;
    }

    public String getCounterBookCode() {
        return counterBookCode;
    }

    public void setCounterBookCode(String counterBookCode) {
        this.counterBookCode = counterBookCode;
    }

    public String getSecondaryLedgerAccount() {
        return secondaryLedgerAccount;
    }

    public void setSecondaryLedgerAccount(String secondaryLedgerAccount) {
        this.secondaryLedgerAccount = secondaryLedgerAccount;
    }

    public String getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }

    public BigDecimal getRate() {
        return rate;
    }

    public void setRate(BigDecimal rate) {
        this.rate = rate;
    }

    public LocalDate getValueDate() {
        return valueDate;
    }

    public void setValueDate(LocalDate valueDate) {
        this.valueDate = valueDate;
    }

    public List<TradeLinkEntity> getTradeLinks() {
        return tradeLinks;
    }

    public void setTradeLinks(List<TradeLinkEntity> tradeLinks) {
        this.tradeLinks = tradeLinks;
    }

    public void addTradeLinks(List<TradeLinkEntity> tradeLinks) {
        this.tradeLinks = tradeLinks;
        if (tradeLinks != null) {
            tradeLinks.forEach(tle -> tle.setCashflow(this));
        }
    }

    public String getEntityCode() {
        return entityCode;
    }

    public void setEntityCode(String entityCode) {
        this.entityCode = entityCode;
    }

    public String getCounterpartyCode() {
        return counterpartyCode;
    }

    public void setCounterpartyCode(String counterpartyCode) {
        this.counterpartyCode = counterpartyCode;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrCode() {
        return currCode;
    }

    public void setCurrCode(String currCode) {
        this.currCode = currCode;
    }

    public YesNo getInternal() {
        return internal;
    }

    public void setInternal(YesNo internal) {
        this.internal = internal;
    }

    public String getNostroID() {
        return nostroID;
    }

    public void setNostroID(String nostroID) {
        this.nostroID = nostroID;
    }

    public String getSsiID() {
        return ssiID;
    }

    public void setSsiID(String ssiID) {
        this.ssiID = ssiID;
    }

    public PaymentSuppressionCategory getPaymentSuppressionCategory() {
        return paymentSuppressionCategory;
    }

    public void setPaymentSuppressionCategory(PaymentSuppressionCategory paymentSuppressionCategory) {
        this.paymentSuppressionCategory = paymentSuppressionCategory;
    }

    public InputBy getInputBy() {
        return inputBy;
    }

    public void setInputBy(InputBy inputBy) {
        this.inputBy = inputBy;
    }

    public String getInputByUserID() {
        return inputByUserID;
    }

    public void setInputByUserID(String inputByUserID) {
        this.inputByUserID = inputByUserID;
    }

    public LocalDateTime getInputDateTime() {
        return inputDateTime;
    }

    public void setInputDateTime(LocalDateTime inputDateTime) {
        this.inputDateTime = inputDateTime;
    }
}
