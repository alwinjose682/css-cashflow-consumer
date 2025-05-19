package io.alw.css.cashflowconsumer.model.jpa;

import jakarta.persistence.*;

/// To get the latest version of the linkType for any cashflow, query for the linkType with the max cashflow version.
/// - A linkType, once assigned is not allowed to be deleted in the FO system
/// - But the trade itself that the linkType references to can be deleted/cancelled
/// - CSS does not need to be aware of whether the relatedReference trade is deleted or replaced. CSS only needs to receive [io.alw.css.domain.cashflow.FoCashMessage] when there is an update to the Cash
@Entity
@Table(name = "TRADE_LINK", schema = "CSS")
public class TradeLinkEntity {

    @EmbeddedId
    TradeLinkEntityPK tradeLinkEntityPK;

    @Column(name = "LINK_TYPE")
    String linkType;

    @Column(name = "RELATED_REFERENCE")
    String relatedReference;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
            @JoinColumn(referencedColumnName = "CASHFLOW_ID", name = "CF_ID", nullable = false, updatable = false, insertable = false),
            @JoinColumn(referencedColumnName = "CASHFLOW_VERSION", name = "CF_VERSION", nullable = false, updatable = false, insertable = false)})
    CashflowEntity cashflow;
}
