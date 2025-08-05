package rs.teslaris.importer.service.interfaces;

import org.springframework.stereotype.Service;
import rs.teslaris.core.dto.document.ProceedingsDTO;
import rs.teslaris.core.dto.document.PublicationSeriesDTO;
import rs.teslaris.core.dto.institution.OrganisationUnitDTO;
import rs.teslaris.core.dto.person.PersonResponseDTO;

@Service
public interface CommonLoader {

    <R> R loadRecordsWizard(Integer userId, Integer institutionId);

    <R> R loadSkippedRecordsWizard(Integer userId, Integer institutionId);

    void skipRecord(Integer userId, Integer institutionId, Boolean removeFromRecord);

    void markRecordAsLoaded(Integer userId, Integer institutionId, Integer oldDocumentId,
                            Boolean deleteOldDocument);

    Integer countRemainingDocumentsForLoading(Integer userId, Integer institutionId);

    OrganisationUnitDTO createInstitution(String importId, Integer userId, Integer institutionId);

    PersonResponseDTO createPerson(String importId, Integer userId, Integer institutionId);

    PublicationSeriesDTO createJournal(String eIssn, String printIssn, Integer userId,
                                       Integer institutionId);

    ProceedingsDTO createProceedings(Integer userId, Integer institutionId);

    void updateManuallySelectedPersonIdentifiers(String importId, Integer selectedPersonId,
                                                 Integer userId, Integer institutionId);

    void updateManuallySelectedInstitutionIdentifiers(String importId,
                                                      Integer selectedInstitutionId, Integer userId,
                                                      Integer institutionId);

    void updateManuallySelectedPublicationSeriesIdentifiers(String eIssn, String printIssn,
                                                            Integer selectedPubSeriesId,
                                                            Integer userId,
                                                            Integer institutionId);

    void updateManuallySelectedConferenceIdentifiers(Integer selectedConferenceId, Integer userId,
                                                     Integer institutionId);

    void prepareOldDocumentForOverwriting(Integer userId, Integer institutionId,
                                          Integer oldDocumentId);
}
