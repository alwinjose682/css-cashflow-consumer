package io.alw.css.cashflowconsumer.service;

import io.alw.css.cashflowconsumer.model.SsiWithCounterpartyData;
import io.alw.css.cashflowconsumer.model.NostroDetails;
import io.alw.css.cashflowconsumer.model.OverridableNostro;
import io.alw.css.cashflowconsumer.model.PrimaryNostro;
import io.alw.css.cashflowconsumer.util.DateUtil;
import io.alw.css.domain.cashflow.TradeType;
import io.alw.css.domain.exception.CategorizedRuntimeException;
import io.alw.css.domain.exception.ExceptionSubCategory;
import io.alw.css.domain.referencedata.Currency;
import io.alw.css.domain.referencedata.Entity;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.apache.ignite.Ignition;
import org.apache.ignite.cache.query.FieldsQueryCursor;
import org.apache.ignite.cache.query.SqlFieldsQuery;
import org.apache.ignite.client.IgniteClient;
import org.apache.ignite.configuration.ClientConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.sql.Time;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static io.alw.css.cashflowconsumer.model.constants.CashflowConsumerExceptionSubCategoryType.UNEXPECTED_REFDATA;

@Service
public class CacheService {
    private final static Logger log = LoggerFactory.getLogger(CacheService.class);

    private final ClientConfiguration clientConfiguration;
    private IgniteClient igniteClient;
    private Map<String, Currency> currencyLocalCache;
    private Map<String, Entity> entityLocalCache;

    public CacheService(ClientConfiguration clientConfiguration) {
        this.clientConfiguration = clientConfiguration;
        this.currencyLocalCache = new HashMap<>();
        this.entityLocalCache = new HashMap<>();
    }

    @PostConstruct
    private void init() {
        try {
            igniteClient = Ignition.startClient(clientConfiguration);
            loadLocalCache();
        } catch (Exception e) {
            log.error("Unable to connect to cache or populate local cache. Ignite cache server may not be started or is unavailable. Exception: {}", e.getMessage());
            throw e;
        }
    }

    @PreDestroy
    public void releaseResources() {
        try {
            igniteClient.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadLocalCache() {
        var currencyLoadSql = "select currCode, countryCode, pmFlag, cutOffTime, activeNostro, entryTime from currency";
        currencyLocalCache = igniteClient.query(new SqlFieldsQuery(currencyLoadSql)).getAll().stream().map(rs -> {
            var currCode = (String) rs.get(0);
            var countryCode = (String) rs.get(1);
            var pmFlag = (boolean) rs.get(2);
            var cutOffTime = DateUtil.toLocalTime((Time) rs.get(3));
            var active = (boolean) rs.get(4);
            var entryTime = DateUtil.toLocalDateTime((Timestamp) rs.get(5));
            return new Currency(currCode, countryCode, pmFlag, cutOffTime, active, entryTime);
        }).collect(Collectors.toMap(Currency::currCode, curr -> curr));

        var entityLoadSql = "select entityCode, entityVersion, entityName, currCode, countryCode, countryName, bicCode, activeNostro, entryTime from entity";
        entityLocalCache = igniteClient.query(new SqlFieldsQuery(entityLoadSql)).getAll().stream().map(rs -> {
            var entityCode = (String) rs.get(0);
            var entityVersion = (int) rs.get(1);
            var entityName = (String) rs.get(2);
            var currCode = (String) rs.get(3);
            var countryCode = (String) rs.get(4);
            var countryName = (String) rs.get(5);
            var bicCode = (String) rs.get(6);
            var active = (boolean) rs.get(7);
            var entryTime = DateUtil.toLocalDateTime((Timestamp) rs.get(8));
            return new Entity(entityCode, entityVersion, entityName, currCode, countryCode, countryName, bicCode, active, entryTime);
        }).collect(Collectors.toMap(Entity::entityCode, ent -> ent));
    }

    public boolean isEntityActive(String entityCode) {
        Entity entity = entityLocalCache.get(entityCode);
        return entity != null && entity.active();
    }

    public boolean isCurrencyActive(String currCode) {
        Currency currency = currencyLocalCache.get(currCode);
        return currency != null && currency.active();
    }

    /// NOTE: Data is fetched only if **both** the Counterparty and SSI records are active
    /// There exists only one active primary SSI for any given counterparty
    ///
    /// @return null if no data in cache matching the criteria
    public SsiWithCounterpartyData getPrimarySsiWithCounterpartyData(String counterpartyCode, String currCode, TradeType tradeType) {
        List<SsiWithCounterpartyData> cpAndPrimarySsiList = getSsiWithCounterpartyData(counterpartyCode, currCode, tradeType, true);
        return cpAndPrimarySsiList.isEmpty() ? null : cpAndPrimarySsiList.get(0);
    }

    /// NOTE: Data is fetched only if **both** the Counterparty and SSI records are active
    /// There exists only one active primary SSI for any given counterparty. But there can be multiple secondary SSIs
    ///
    /// @return empty list no data in cache matching the criteria
    public List<SsiWithCounterpartyData> getSsiWithCounterpartyData(String counterpartyCode, String currCode, TradeType tradeType, boolean primary) {
        // NOTE: The sql JOIN is made on cache-key fields ONLY.
        // This is to make sure that the join will be a **colocated** join. Non-colocated join is very in-efficient.
        var sql = """
                select
                a.counterpartyCode, a.counterpartyVersion, a.internal, a.active,
                b.ssiId, b.ssiVersion, b.currCode, b.product, b.isPrimary, b.active
                from COUNTERPARTY a JOIN SSI b ON a.counterpartyCode = b.counterpartyCode
                where a.counterpartyCode = ? and b.currCode = ? and b.product = ? and b.isPrimary is ?
                and a.active is true and b.active is true
                """;
        return igniteClient.query(new SqlFieldsQuery(sql).setArgs(
                        counterpartyCode, currCode, tradeType.name(), primary
                ))
                .getAll()
                .stream()
                .map(rs -> {
                    var rs_counterpartyCode = (String) rs.get(0);
                    var rs_counterpartyVersion = (int) rs.get(1);
                    var internal = (boolean) rs.get(2);
                    var activeCounterparty = (boolean) rs.get(3);
                    var ssiId = (String) rs.get(4);
                    var ssiVersion = (int) rs.get(5);
                    var rs_currCode = (String) rs.get(6);
                    String product_str = (String) rs.get(7); // all others are mandatory fields and are never null
                    var product = product_str == null ? null : TradeType.valueOf(product_str);
                    var rs_primarySsi = (boolean) rs.get(8);
                    var activeSsi = (boolean) rs.get(9);
                    return new SsiWithCounterpartyData(rs_counterpartyCode, rs_counterpartyVersion, internal, activeCounterparty, ssiId, ssiVersion, rs_currCode, product, rs_primarySsi, activeSsi);
                }).toList();
    }

    public NostroDetails getNostroDetails(String entityCode, String currCode, String counterpartyCode) {
        // TODO: The sql JOIN has to be made on cache-key fields ONLY so that the join will be **Colocated**
        //  Change the affinity-key of 'CounterpartySlaMapping' cache to a composite key with all the relevant fields used to join ?

        var sql = """
                select
                a.nostroId, a.nostroVersion, a.entityCode, a.currCode, a.secondaryLedgerAccount, a.isPrimary, a.active,
                b.mappingId, b.mappingVersion, b.counterpartyCode, b.active
                from NOSTRO a LEFT JOIN CounterpartySlaMapping b
                ON a.entityCode = b.entityCode and a.currCode = b.currCode and a.secondaryLedgerAccount = b.secondaryLedgerAccount
                where a.entityCode = ? and a.currCode = ? and a.active is true
                AND (
                      (a.isPrimary is true and b.mappingId is NULL)
                      OR
                      (a.isPrimary is false and b.active is true and b.counterpartyCode = ?)
                    )
                """;
        SqlFieldsQuery sqlFieldsQuery = new SqlFieldsQuery(sql).setArgs(entityCode, currCode, counterpartyCode);
        try (FieldsQueryCursor<List<?>> qryCur = igniteClient.query(sqlFieldsQuery)) {
            PrimaryNostro primaryNostro = null;
            OverridableNostro overridableNostro = null;
            for (List<?> rs : qryCur) {
                var nostroId = (String) rs.get(0);
                var nostroVersion = (int) rs.get(1);
                var rs_entityCode = (String) rs.get(2);
                var rs_currCode = (String) rs.get(3);
                var secondaryLedgerAccount = (String) rs.get(4);
                var isPrimary = (boolean) rs.get(5);
                var activeNostro = (boolean) rs.get(6);
                var mappingId = (Long) rs.get(7); // maybe null, so casting as java.lang.Long instead of primitive long
                var mappingVersion = (Integer) rs.get(8);
                var rs_counterpartyCode = (String) rs.get(9);
                var activeMapping = (Boolean) rs.get(10);

                if (isPrimary && primaryNostro == null) {
                    primaryNostro = new PrimaryNostro(nostroId, nostroVersion, rs_entityCode, rs_currCode, secondaryLedgerAccount, isPrimary, activeNostro);
                } else if (mappingId != null && overridableNostro == null) {
                    overridableNostro = new OverridableNostro(mappingId, mappingVersion, rs_counterpartyCode, activeMapping, nostroId, nostroVersion, rs_entityCode, rs_currCode, secondaryLedgerAccount, isPrimary, activeNostro);
                } else {
                    final String exMsg;
                    if (isPrimary) {
                        exMsg = "More than ONE primary nostro found in IgniteCache for entityCode: " + entityCode + ", currCode: " + currCode;
                    } else {
                        exMsg = "More than ONE secondary nostro found in IgniteCache for entityCode: " + entityCode + ", currCode: " + currCode + ", counterpartyCode: " + counterpartyCode + ", secondaryLedgerAccount: " + secondaryLedgerAccount;
                    }
                    throw CategorizedRuntimeException.TECHNICAL_RECOVERABLE(exMsg, new ExceptionSubCategory(UNEXPECTED_REFDATA, null));
                }
            }

            return new NostroDetails(primaryNostro, overridableNostro);
        }
//                .collect(Collectors.partitioningBy(NostroDetailRaw::primary));// There is only ONE primary nostro, rest are secondary nostros and a secondary nostros overrides the primary nostro
    }
}
