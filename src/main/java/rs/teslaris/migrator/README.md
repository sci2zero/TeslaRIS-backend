# `rs.teslaris.migrator`

Harvests entities from an external HTTP source (currently the **hydrator** service), converts them
to core DTOs, and creates them through the existing core services.

Full design: `migrator-design.md`.

## Pipeline

```
Feign client -> SourceRecordFetcher -> ItemRouter -> EntityCreator (core service)
                                            |
                                            +-> FailureHandler (merge / retry / skip)
```

`MigrationPipelineRunner` is the only place that holds the algorithm. Everything source-specific
lives in `client/<source>`, `model/<source>`, `converter/<source>` and one `@Configuration` of
pipeline beans.

## Running a migration

Passes must run in order - documents reference persons, employments reference both persons and
organisation units:

```
POST /api/migrator/run?source=hydrator&entityType=ORGANISATION_UNIT
POST /api/migrator/run?source=hydrator&entityType=PERSON
POST /api/migrator/run?source=hydrator&entityType=PERSON_EMPLOYMENT
POST /api/migrator/run?source=hydrator&entityType=DOCUMENT
```

Each call returns a run id immediately and executes on the `migrationExecutor`. Progress:

```
GET  /api/migrator/runs/{runId}
GET  /api/migrator/runs/{runId}/failures
POST /api/migrator/runs/{runId}/retry-failed
GET  /api/migrator/pipelines
```

All endpoints require the `PERFORM_MIGRATION` authority. `performIndex=false` is the default -
migrate with indexing off and reindex afterwards.

## Failure behaviour

Nothing aborts a run:

| Level | Behaviour |
|---|---|
| Item creation | `FailureHandler` decides RETRY / RESOLVED / SKIP; failures are logged and the run continues |
| Record routing | Logged as `RECORD_UNCONVERTIBLE`, next record |
| Batch fetch | Retried per `RetryPolicy`, then logged as `BATCH_FAILED` and the run moves to the next page |

Everything lands in `application-logs/migration.log` (dedicated `MIGRATION` logger) and in the
`migration_record_log` Mongo collection.

## Resume, dedup and id mapping

`migration_record_log` stores one row per item: `(source, entity_type, source_key)` unique, with
status and the created TeslaRIS id. No payloads - this is bookkeeping, not a staging store. It gives:

- **resume**: items already `CREATED`/`RESOLVED` are skipped on a re-run;
- **dedup**: the same institution appears in many curricula and is created once;
- **id map**: employments resolve their person and organisation unit through
  `MigrationIdResolver`, because hydrator ids are strings while core `oldId` columns are numeric.

## Adding a source

1. `model/<source>/` - record classes (partial is fine, everything ignores unknown properties).
2. `converter/<source>/` - `RecordExtractor` implementations, or `RecordConverter` + `RecordExtractor.of`
   for 1:1 sources.
3. `client/<source>/` - `@FeignClient` per endpoint plus a fetcher (`HttpPagedFetcher`,
   `SingleShotFetcher`, or your own).
4. `configuration/<source>/` - one `MigrationPipeline` bean per entity type.

## Overriding a single entity type

Register a pipeline for the specific type and it wins over the generic `DOCUMENT` pipeline:

```java
@Bean
MigrationPipeline<ProceedingsRecord> hydratorProceedingsPipeline() {
    return new MigrationPipeline<>(HydratorSource.NAME, PROCEEDINGS_PUBLICATION, ...);
}
```

Unregistered subtypes keep falling back to `DOCUMENT`, whose router output is filtered down to the
requested type by `ResolvedPipeline#accepts`.

## PoC scope

Implemented: organisation units, persons, employments, journal articles, dissertations.
Not yet mapped: books, book chapters, conference papers/posters, scripts, manuals, newspaper
articles, other outputs - they are logged as unsupported and skipped.

Known simplifications, because this is a Poc:

- Institutions carry no identifier in the payload, so their identity is a normalised name.
- Journals are created as stubs from the article's journal name.
- Contributions are name-only; co-authors link when their own curriculum is migrated.
- Paging is offset-based over a mutable sort key, so a live traversal can skip records. Keyset
  pagination has been requested from hydrator.
