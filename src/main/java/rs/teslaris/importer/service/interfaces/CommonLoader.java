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

    void skipRecord(Integer userId, Integer institutionId);

    void markRecordAsLoaded(Integer userId, Integer institutionId);

    Integer countRemainingDocumentsForLoading(Integer userId, Integer institutionId);

    OrganisationUnitDTO createInstitution(String scopusAfid, Integer userId, Integer institutionId);

    PersonResponseDTO createPerson(String scopusAuthorId, Integer userId, Integer institutionId);

    PublicationSeriesDTO createJournal(String eIssn, String printIssn, Integer userId,
                                       Integer institutionId);

    ProceedingsDTO createProceedings(Integer userId, Integer institutionId);
}
