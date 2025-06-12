package io.alw.css.cashflowconsumer.model.jpa;

import jakarta.persistence.*;
import org.hibernate.proxy.HibernateProxy;

import java.util.Objects;

/// To get the latest version of the linkType for any cashflow, query for the linkType with the max cashflow version.
/// - A linkType, once assigned is not allowed to be deleted in the FO system
/// - But the trade itself that the linkType references to can be deleted/cancelled
/// - CSS does not need to be aware of whether the relatedReference trade is deleted or replaced. CSS only needs to receive [io.alw.css.domain.cashflow.FoCashMessage] when there is an update to the Cash
@Entity
@Table(name = "TRADE_LINK", schema = "CSS")
public class TradeLinkEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "tradeLinkEntitySeq")
    @SequenceGenerator(sequenceName = "css_common_seq", allocationSize = 1, name = "tradeLinkEntitySeq")
    Long id;

    @Column(name = "CF_ID", nullable = false)
    Long cashflowID;

    @Column(name = "CF_VERSION", nullable = false, length = 10)
    Integer cashflowVersion;

    @Column(name = "LINK_TYPE")
    String linkType;

    @Column(name = "RELATED_REFERENCE")
    String relatedReference;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
            @JoinColumn(referencedColumnName = "CASHFLOW_ID", name = "CF_ID", nullable = false, updatable = false, insertable = false),
            @JoinColumn(referencedColumnName = "CASHFLOW_VERSION", name = "CF_VERSION", nullable = false, updatable = false, insertable = false)})
    CashflowEntity cashflow;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCashflowID() {
        return cashflowID;
    }

    public void setCashflowID(Long cashflowID) {
        this.cashflowID = cashflowID;
    }

    public Integer getCashflowVersion() {
        return cashflowVersion;
    }

    public void setCashflowVersion(Integer cashflowVersion) {
        this.cashflowVersion = cashflowVersion;
    }

    public String getLinkType() {
        return linkType;
    }

    public void setLinkType(String linkType) {
        this.linkType = linkType;
    }

    public String getRelatedReference() {
        return relatedReference;
    }

    public void setRelatedReference(String relatedReference) {
        this.relatedReference = relatedReference;
    }

    public CashflowEntity getCashflow() {
        return cashflow;
    }

    public void setCashflow(CashflowEntity cashflow) {
        this.cashflow = cashflow;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        TradeLinkEntity that = (TradeLinkEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "TradeLinkEntity{" +
                "id=" + id +
                ", cashflowID=" + cashflowID +
                ", cashflowVersion=" + cashflowVersion +
                ", linkType='" + linkType + '\'' +
                ", relatedReference='" + relatedReference + '\'' +
                ", cashflow=" + cashflow +
                '}';
    }
}
