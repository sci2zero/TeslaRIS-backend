package rs.teslaris.core.indexrepository;


import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.annotations.CountQuery;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;

@Repository
public interface DocumentPublicationIndexRepository extends
    ElasticsearchRepository<DocumentPublicationIndex, String> {

    Optional<DocumentPublicationIndex> findDocumentPublicationIndexByDatabaseId(Integer databaseId);

    Page<DocumentPublicationIndex> findDocumentPublicationIndexByDatabaseIdIn(
        List<Integer> databaseIds, Pageable pageable);

    long count();

    Page<DocumentPublicationIndex> findByTypeAndEventId(String type, Integer eventId,
                                                        Pageable pageable);

    Page<DocumentPublicationIndex> findByTypeAndProceedingsId(String type, Integer proceedingsId,
                                                              Pageable pageable);

    Page<DocumentPublicationIndex> findByTypeAndProceedingsIdAndIsApprovedTrue(String type,
                                                                               Integer proceedingsId,
                                                                               Pageable pageable);

    List<DocumentPublicationIndex> findByTypeAndJournalIdAndAuthorIds(String type,
                                                                      Integer journalId,
                                                                      Integer authorId);

    List<DocumentPublicationIndex> findByTypeAndMonographIdAndAuthorIds(String type,
                                                                        Integer monographId,
                                                                        Integer authorId);

    Page<DocumentPublicationIndex> findByAuthorIds(Integer authorId, Pageable pageable);

    Page<DocumentPublicationIndex> findByAuthorIdsAndYearBetween(Integer authorId,
                                                                 Integer startYear, Integer endYear,
                                                                 Pageable pageable);

    @Query("""
        {
          "bool": {
            "must": [
              { "term": { "author_ids": "?0" } }
            ],
            "filter": {
              "bool": {
                "should": [
                  { "range": { "year": { "gte": "?1", "lte": "?2" } } },
                  { "term": { "year": -1 } }
                ]
              }
            }
          }
        }
        """)
    Page<DocumentPublicationIndex> findByAuthorIdAndYearRangeOrUnknown(Integer authorId,
                                                                       Integer startYear,
                                                                       Integer endYear,
                                                                       Pageable pageable);

    Page<DocumentPublicationIndex> findByOrganisationUnitIdsIn(List<Integer> organisationUnitIds,
                                                               Pageable pageable);

    Page<DocumentPublicationIndex> findByTypeAndJournalId(String type, Integer journalId,
                                                          Pageable pageable);

    Page<DocumentPublicationIndex> findByTypeAndJournalIdAndIsApprovedTrue(String type,
                                                                           Integer journalId,
                                                                           Pageable pageable);

    Page<DocumentPublicationIndex> findByTypeInAndPublicationSeriesId(List<String> types,
                                                                      Integer publicationSeriesId,
                                                                      Pageable pageable);

    Page<DocumentPublicationIndex> findByTypeAndMonographId(String type, Integer monographId,
                                                            Pageable pageable);

    Page<DocumentPublicationIndex> findByTypeAndMonographIdAndIsApprovedTrue(String type,
                                                                             Integer monographId,
                                                                             Pageable pageable);

    Page<DocumentPublicationIndex> findByPublisherId(Integer publisherId, Pageable pageable);

    Page<DocumentPublicationIndex> findByTypeIn(List<String> types, Pageable pageable);

    void deleteByJournalIdAndType(Integer journalId, String type);

    void deleteByEventIdAndType(Integer eventId, String type);

    void deleteByMonographId(Integer monographId);

    void deleteByProceedingsId(Integer monographId);

    void deleteByAuthorIdsAndType(Integer authorId, String type);

    Page<DocumentPublicationIndex> findByClaimerIds(Integer claimerId, Pageable pageable);

    @CountQuery("""
        {
          "bool": {
            "must": [
              { "range": { "year": { "gt": -1 } } }
            ],
            "must_not": [
              { "term": { "type": "PROCEEDINGS" } }
            ]
          }
        }
        """)
    Long countAssessable();

    @CountQuery("""
        {
          "bool": {
            "must": [
              { "terms": { "organisationUnitIds": [?0] } },
              { "range": { "year": { "gt": -1 } } }
            ],
            "must_not": [
              { "term": { "type": "PROCEEDINGS" } }
            ]
          }
        }
        """)
    Long countAssessableByOrganisationUnitIds(Integer organisationUnitId);

    @CountQuery("""
        {
          "bool": {
            "must": [
              { "terms": { "assessedBy": [?0] } },
              { "range": { "year": { "gt": -1 } } }
            ],
            "must_not": [
              { "term": { "type": "PROCEEDINGS" } }
            ]
          }
        }
        """)
    Long countByAssessedBy(Integer assessedBy);

    @CountQuery("""
        {
          "bool": {
            "must": [
              { "terms": { "organisationUnitIds": [?0] } },
              { "terms": { "assessedBy": [?1] } },
              { "range": { "year": { "gt": -1 } } }
            ],
            "must_not": [
              { "term": { "type": "PROCEEDINGS" } }
            ]
          }
        }
        """)
    Long countByOrganisationUnitIdsAndAssessedBy(Integer organisationUnitId, Integer assessedBy);

    @Query("""
        {
          "bool": {
            "must": [
              { "term": { "type": "THESIS" } },
              { "range": {
                  "thesis_defence_date": {
                    "gte": "?0",
                    "lte": "?1"
                  }
                }
              },
              { "terms": { "organisation_unit_ids": ?2 } },
              { "term": { "publication_type": "?3" } }
            ]
          }
        }
        """)
    Page<DocumentPublicationIndex> fetchDefendedThesesInPeriod(LocalDate startDate,
                                                               LocalDate endDate,
                                                               List<Integer> institutionIds,
                                                               String thesisType,
                                                               Pageable pageable);

    @Query("""
        {
          "bool": {
            "must": [
              { "term": { "type": "THESIS" } },
              { "range": {
                  "topic_acceptance_date": {
                    "gte": "?0",
                    "lte": "?1"
                  }
                }
              },
              { "terms": { "organisation_unit_ids": ?2 } },
              { "term": { "publication_type": "?3" } }
            ]
          }
        }
        """)
    Page<DocumentPublicationIndex> fetchAcceptedThesesInPeriod(LocalDate startDate,
                                                               LocalDate endDate,
                                                               List<Integer> institutionIds,
                                                               String thesisType,
                                                               Pageable pageable);

    @Query("""
        {
          "bool": {
            "must": [
              { "term": { "type": "THESIS" } },
              { "range": {
                  "public_review_start_dates": {
                    "gte": "?0",
                    "lte": "?1"
                  }
                }
              },
              { "terms": { "organisation_unit_ids": ?2 } },
              { "term": { "publication_type": "?3" } }
            ]
          }
        }
        """)
    Page<DocumentPublicationIndex> fetchThesesWithPublicReviewInPeriod(LocalDate startDate,
                                                                       LocalDate endDate,
                                                                       List<Integer> institutionIds,
                                                                       String thesisType,
                                                                       Pageable pageable);

    @Query("""
        {
          "bool": {
            "must": [
              { "term": { "type": "THESIS" } },
              { "range": {
                  "thesis_defence_date": {
                    "gte": "?0",
                    "lte": "?1"
                  }
                }
              },
              { "terms": { "organisation_unit_ids": ?2 } },
              { "term": { "publication_type": "?3" } }
            ],
            "filter": [
              { "term": { "is_open_access": true } }
            ]
          }
        }
        """)
    Page<DocumentPublicationIndex> fetchPubliclyAvailableDefendedThesesInPeriod(LocalDate startDate,
                                                                                LocalDate endDate,
                                                                                List<Integer> institutionIds,
                                                                                String thesisType,
                                                                                Pageable pageable);

    @CountQuery("""
        {
          "bool": {
            "must_not": {
              "term": {
                "type": "PROCEEDINGS"
              }
            }
          }
        }
        """)
    long countPublications();

    @CountQuery("""
        {
          "bool": {
            "must": [
              { "term": { "is_approved": "true" } }
            ],
            "must_not": [
              { "term": { "type": "PROCEEDINGS" } }
            ]
          }
        }
        """)
    long countApprovedPublications();


}
