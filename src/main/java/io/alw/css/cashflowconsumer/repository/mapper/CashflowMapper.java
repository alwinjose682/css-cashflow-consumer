package io.alw.css.cashflowconsumer.repository.mapper;

import io.alw.css.cashflowconsumer.model.jpa.CashflowEntity;
import io.alw.css.cashflowconsumer.model.jpa.TradeLinkEntity;
import io.alw.css.domain.cashflow.Cashflow;
import io.alw.css.domain.cashflow.TradeLink;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(uses = MapperUtil.class)
public interface CashflowMapper {
    static CashflowMapper instance() {
        return Mappers.getMapper(CashflowMapper.class);
    }

    @Mapping(source = ".", target = "cashflowEntityPK")
    @Mapping(target = "tradeLinks", ignore = true)
    CashflowEntity mapToEntity_excludingTradeLinks(Cashflow cashflow);

    @Mapping(target = ".", source = "cashflowEntityPK")
    @Mapping(target = "tradeLinks", ignore = true)
    Cashflow mapToDomain_excludingAssociations(CashflowEntity cashflowEntity);

    static CashflowEntity mapToEntity(Cashflow cashflow) {
        CashflowEntity cashflowEntity = instance().mapToEntity_excludingTradeLinks(cashflow);
        cashflowEntity.setTradeLinks(mapTradeLinkToTradeLinkEntity(cashflow, cashflowEntity));
        return cashflowEntity;
    }

    static List<TradeLinkEntity> mapTradeLinkToTradeLinkEntity(Cashflow cashflow, CashflowEntity cashflowEntity) {
        List<TradeLink> tradeLinks = cashflow.tradeLinks();
        long cashflowID = cashflow.cashflowID();
        int cashflowVersion = cashflow.cashflowVersion();

        return tradeLinks == null
                ? null
                : tradeLinks.stream().map(tl -> {
            TradeLinkEntity tle = new TradeLinkEntity();
            tle.setCashflowID(cashflowID);
            tle.setCashflowVersion(cashflowVersion);
            tle.setLinkType(tl.linkType());
            tle.setRelatedReference(tl.relatedReference());
            tle.setCashflow(cashflowEntity);
            return tle;
        }).toList();
    }
}
