# Cashflow Consumer

## Cashflow Consumer Processing Sequence (CCPS)

| Step | Action                                     | Description                                                                                                                            | Technical Aspects                                                                                                                                                                                   |
|------|--------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 1    | **Consume Message**                        | Consumes message produced by the upstream system: fo-simulator                                                                         | The message is in avro format and is consumed from a Kafka topic. Messages are continuously produced by fo-simulator. (Several messages per second)                                                 |
| 2    | **Map The Fields**                         | Maps the fields of the upstream message to Cashflow. Also does verifications of the fields being mapped                                | The avro message is mapped to Cashflow record's CashflowBuilder                                                                                                                                     |
| 3    | **Determine Cashflow Version**             | Determines whether the message consumed is: <br/> New or Amendment or Duplicate <br/> Or whether the Previous CF is cancelled          | Performs a check against the DB                                                                                                                                                                     |
| 4    | **Reference Data Validation**              | Validates following reference data values to ensure that they do exist and are still active:<br/> Currency, Entity, Counterparty       | Uses Apache Ignite InMemory Cache. Reference data is held in an in memory cache(Apache Ignite). Currency and Entity are retrieved from Ignite and locally cached                                    |
| 5    | **Cashflow Enrichment**                    | Following fields are computed and cashflow is enriched with these values:<br/> nostroID, ssiID, isInternal, paymentSuppressionCategory | Uses Apache Ignite InMemory Cache. These values are computed based on the reference data held in Ignite and based on few other criteria                                                             |
| 6    | **Create Cashflow**                        | Create the cashflow and if applicable, create an offsetting cashflow as well. Details are not given here, but documented in the code   | Obtains a new cashflowID from database sequence(if RevisionType is NEW)                                                                                                                             |
| 7    | **Persist Cashflow**                       | By synchronizing potential concurrent activities, persists the cashflow to the database(DB)                                            | Uses JPA/Hibernate. In a single Transaction, the previous cashflow version's 'latest' field is updated to 'N' **if it is still 'N'** and the new cashflow version is persisted with 'latest' as 'Y' |
| 8    | **Create Confirmation Cancellation Event** | Creates and publishes a Confirmation Cancellation Event if applicable. **NOTE**: This step is not implemented yet                      |

**NOTE:**
At any step if an exception occurs or the cashflow cannot be processed, a rejection entry is written to database.

### CCPS#8: Persist Cashflow

    The cashflow that is validated and enriched is persisted in the database.

    Concurrent actions to be accounted for when processing a cashflow amendment:
        When processing a cashflow amendment, various concurrent actions, although less frequent, can happen:
            1) Confirmation of the previous version of the cashflow and publishing the confirmation event to netting-service
            2) Concurrent amendment of the previous version of the Cashflow by a CSS user
    
            **NOTE:** 
                The point 1 above is also a concurrent action that needs to be accounted for, becuase, the cashflow-consumer has to generate a Confirmation Cancellation Event if the previous cashflow is in confirmed state.
                ie; The cashflow-consumer has to generate confirmation cancellation event and confirmation-consumer has to generate confirmation event. These are potential concurrent actions that needs to be synchronized.
        
        Ideally, these potential concurrent actions and the cashflow amendment processing should be serialized one after the other to ensure correctness of payment shape generation.
        But, it is not feasible to pessimistically serialize these potential concurrent actions. The main reason is that the probability of concurrent actions will be low at any point in time and full serialization is costly.
        Therefore, cashflow-consumer, does the synchronization optimistically, when persisting the cashflow, via the traditional approach of relying on the atomic transaction guarantee of databases.
        
        Persisting the cashflow amendment in an optimistic database transaction:
            In a single transaction, the previous cashflow version's 'latest' field is updated to 'N' **if it is still 'N'** and the new cashflow version is persisted with 'latest' as 'Y'
            Since this is done optimistically, it is very well possible that a concurrent action has taken place and the cashflow considered as previous cashflow is no longer the real previous cashflow.
            In such a case the optimistic transaction will not complete and the cashflow amendment will be rejected which can be replayed later to re-process the cashflow amendment.

### CCPS#6: Cashflow Enrichment

    Cashflow Enrichment involves determining the appropriate value for each field that needs to computed by CSS(Cashflow Settlement System).

    1) nostroID enrichment
        Details on Nostro Account:
            - A nostro account, owned by the bank, is uniquely identified by a nostroID
            - Each Entity + Currency combination has one or more nostro accounts.
              ONE of the nostro account is specified as a PRIMARY nostro account and rest of them, if any, are SECONDARY nostro accounts
              ie; There exists only one PRIMARY nostro for any given combination of entity + currency

        Determining the Nostro Account / nostroID:
            PRIMARY nostro:
                A nostro is determined based on the combination of entityCode + currCode and by default the PRIMARY nostro is selected.
            SECONDARY nostro:
                - SECONDARY nostro is selected when the counterparty profile is setup explicitely, by the reference data business team, to override the default selection of PRIMARY nostro
                If the Counterparty profile has a nostro and 'sla' mapping specified for the same entityCode + currCode combination, then the nostroID specified by this mapping, which is always secondary, is selected
                - Although not part of the normal 'Cashflow Processing Sequence', another possible route by which Cashflow can have a SECONDARY SSI is when the CSS buisness user manually chooses one
 
        More on sla(Secondary Ledger Account):
			- 'sla' is Secondary Ledger Account and belongs to an entity. A single entity can have multiple sla. (sla<-entity is many-to-one relationship)
			- sla is unique for each nostro. ie; nostro and sla has a one-to-one relationship

    2) ssiID enrichment
        Details on SSI Account: 
            - An SSI account, owned by the Counterparty, is uniquely identified by an ssiID
            - Each Counterparty + Currency + TradeType combination has one or more SSI accounts.
              ONE of the ssi account is specified as a PRIMARY SSI account and rest of them, if any, are secondary SSI accounts
              ie; There exists only one PRIMARY SSI for any given combination of Counterparty + Currency + TradeType

        Determining the SSI Account / ssiID:
            PRIMARY SSI:
                An SSI is determined based on the combination of Counterparty + Currency + TradeType and by default the PRIMARY SSI is selected.
            SECONDARY SSI:
                SECONDARY SSI is selected when the CSS buisness user manually chooses one.

    3) 'isInternal' enrichment
        'isInternal' flag is set to true if:
            TransactionType is INTER_BOOK, INTER_BRANCH or INTER_COMPANY
            AND
            Counterparty is internal(Internal Counterparties can be considered as virtual counterparty accounts created to denote one of the bank's entity that needs to be represented on the counter side of the transaction)

    4) 'paymentSuppressionCategory' enrichment
        'paymentSuppressionCategory' denotes whether the Payment for the Cashflow needs to be suppressed due to:
			1) being INTERBOOK
			2) amount being too small
			3) missing or invalid nostro and/or client SSI
        A suppressed Cashflow will not be netted/aggregated and hence a Payment will not be generated
