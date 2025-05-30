package io.alw.css.cashflowconsumer.repository.mapper;

import io.alw.css.cashflowconsumer.model.jpa.CashflowEntity;
import io.alw.css.domain.cashflow.Cashflow;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper
public interface CashflowEntityMapper {
    static CashflowEntityMapper instance() {
        return Mappers.getMapper(CashflowEntityMapper.class);
    }

    @Mapping(source = ".", target = "cashflowEntityPK")
    CashflowEntity mapToEntity(Cashflow cashflow);

    @Mapping(target = ".", source = "cashflowEntityPK")
    @Mapping(target = "tradeLinks", ignore = true)
    Cashflow mapToDomain(CashflowEntity cashflowEntity);
}
