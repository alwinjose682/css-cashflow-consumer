package io.alw.css.cashflowconsumer.service;

import io.alw.css.cashflowconsumer.model.CounterpartyAndSsiDetails;
import io.alw.css.cashflowconsumer.model.NostroDetails;
import io.alw.css.cashflowconsumer.util.DateUtil;
import io.alw.css.domain.cashflow.TradeType;
import io.alw.css.domain.referencedata.Currency;
import io.alw.css.domain.referencedata.Entity;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.apache.ignite.Ignition;
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
        var currencyLoadSql = "select currCode, countryCode, pmFlag, cutOffTime, active, entryTime from currency";
        currencyLocalCache = igniteClient.query(new SqlFieldsQuery(currencyLoadSql)).getAll().stream().map(rs -> {
            var currCode = (String) rs.get(0);
            var countryCode = (String) rs.get(1);
            var pmFlag = (boolean) rs.get(2);
            var cutOffTime = DateUtil.toLocalTime((Time) rs.get(3));
            var active = (boolean) rs.get(4);
            var entryTime = DateUtil.toLocalDateTime((Timestamp) rs.get(5));
            return new Currency(currCode, countryCode, pmFlag, cutOffTime, active, entryTime);
        }).collect(Collectors.toMap(Currency::currCode, curr -> curr));

        var entityLoadSql = "select entityCode, entityVersion, entityName, currCode, countryCode, countryName, bicCode, active, entryTime from entity";
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

    /// NOTE: Counterparty and SSI records are fetched even if they are inactive.
    /// This is so because, it is required write a rejection(so that business users will be aware) when Counterparty and/or SSI are inactive
    ///
    /// NOTE: Ssi#Product is considered same as TradeType. There exist only one primary SSI. Rest of them, if any, are secondary SSIs.
    ///
    /// @return null if data matching the criteria is not present in the cache
    public CounterpartyAndSsiDetails getCounterpartyAndSsiDetails(String counterpartyCode, String currCode, TradeType tradeType, boolean primarySsi) {
        // NOTE: The sql JOIN is made on cache-key fields ONLY.
        // This is to make sure that the join will be a **colocated** join. Non-colocated join is very in-efficient.
        var sql = """
                select
                a.counterpartyCode, a.counterpartyVersion, a.internal, a.active,
                b.ssiId, b.ssiVersion, b.currCode, b.product, b.isPrimary, b.active
                from COUNTERPARTY a JOIN SSI b ON a.counterpartyCode = b.counterpartyCode
                where a.counterpartyCode = ? and b.currCode = ? and b.product = ? and b.isPrimary = ?
                """;
        List<CounterpartyAndSsiDetails> counterpartyAndSsiDetailsList = igniteClient.query(new SqlFieldsQuery(sql).setArgs(
                        counterpartyCode, currCode, tradeType.name(), primarySsi
                )).getAll()
                .stream()
                .map(rs -> {
                    var rs_counterpartyCode = (String) rs.get(0);
                    var rs_counterpartyVersion = (int) rs.get(1);
                    var internal = (boolean) rs.get(2);
                    var activeCounterparty = (boolean) rs.get(3);
                    var ssiId = (String) rs.get(4);
                    var ssiVersion = (int) rs.get(5);
                    var rs_currCode = (String) rs.get(6);
                    var product = TradeType.valueOf((String) rs.get(7));
                    var rs_primarySsi = (boolean) rs.get(8);
                    var activeSsi = (boolean) rs.get(9);
                    return new CounterpartyAndSsiDetails(rs_counterpartyCode, rs_counterpartyVersion, internal, activeCounterparty, ssiId, ssiVersion, rs_currCode, product, rs_primarySsi, activeSsi);
                }).toList();

        return counterpartyAndSsiDetailsList.isEmpty() ? null : counterpartyAndSsiDetailsList.get(0);
    }

    public NostroDetails getNostroDetails(String entityCode, String currCode, String counterpartyCode) {
        var sql = """
                select
                a.nostroId, a.nostroVersion, a.entityCode, a.currCode, a.secondaryLedgerAccount, a.isPrimary, a.active
                from NOSTRO a, 
                
                

              
                where a.entityCode = ? and a.currCode = ? and b.counterpartyCode = ?
                """;


    }
}
