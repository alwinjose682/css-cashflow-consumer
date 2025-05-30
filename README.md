# Cashflow Consumer

## Cashflow Consumer Processing Sequence (CCPS)

| Step | Action                         | Description                                                                                                                            | Technical Aspects                                                                                                                                                                                   |
|------|--------------------------------|----------------------------------------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 1    | **Consume Message**            | Consumes message produced by the upstream system: fo-simulator                                                                         | The message is in avro format and is consumed from a Kafka topic. Messages are continuously produced by fo-simulator. (Several messages per second)                                                 |
| 2    | **DB entry**                   | Creates an initial database(DB) entry                                                                                                  | Uses JPA/Hibernate. Either Oracle DB or H2 DB can be used by changing the configs                                                                                                                   |
| 3    | **Map The Fields**             | Maps the fields of the upstream message to the format used by CSS. Also does verifications of the fields being mapped                  | The avro message is mapped to Cashflow record's CashflowBuilder                                                                                                                                     |
| 4    | **Determine Cashflow Version** | Determines whether the message consumed is: <br/> New or Amendment or Duplicate <br/> Or whether the Previous CF is cancelled          | Performs a check against the DB                                                                                                                                                                     |
| 5    | **Reference Data Validation**  | Validates following reference data values to ensure that they do exist and are still active:<br/> Currency, Entity, Counterparty       | Uses Apache Ignite InMemory Cache. Reference data is held in an in memory cache(Apache Ignite). Currency and Entity are retrieved from Ignite and locally cached                                    |
| 6    | **Cashflow Enrichment**        | Following fields are computed and cashflow is enriched with these values:<br/> nostroID, ssiID, isInternal, paymentSuppressionCategory | Uses Apache Ignite InMemory Cache. These values are computed based on the reference data held in Ignite and based on few other criteria                                                             |
| 7    | **Create Cashflow**            | Create the cashflow and if applicable, create an offsetting cashflow as well. Details are not given here, but documented in the code   | Obtains a new cashflowID from database sequence(if RevisionType is NEW)                                                                                                                             |
| 8    | **Persist Cashflow**           | By synchronizing potential concurrent activities, persists the cashflow to the database(DB)                                            | Uses JPA/Hibernate. In a single Transaction, the previous cashflow version's 'latest' field is updated to 'N' **if it is still 'N'** and the new cashflow version is persisted with 'latest' as 'Y' |
| 9    | **Create Un-Net Event**        | Creates Un-Net event if applicable. **NOTE**: This step is not implemented yet                                                         |

### CCPS#8: Persist Cashflow

    At this step of the processing sequence, the cashflow that is validated and enriched need to be persisted to the database.
     -  OR Persist the rejection if failed due to a concurrent transaction due to Cashflow Confirmation or User's manual action  - 
    But various concurrent actions can happen, like:
        1) sending unconfirmation event when processing a CF amend
        2) sending confirmation event when confirming a CF
        3) concurrent CF amend by CSS user

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
