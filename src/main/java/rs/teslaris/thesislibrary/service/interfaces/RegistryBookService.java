package rs.teslaris.thesislibrary.service.interfaces;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import rs.teslaris.core.service.interfaces.JPAService;
import rs.teslaris.thesislibrary.dto.InstitutionCountsReportDTO;
import rs.teslaris.thesislibrary.dto.PhdThesisPrePopulatedDataDTO;
import rs.teslaris.thesislibrary.dto.RegistryBookEntryDTO;
import rs.teslaris.thesislibrary.model.RegistryBookEntry;

@Service
public interface RegistryBookService extends JPAService<RegistryBookEntry> {

    RegistryBookEntryDTO readRegistryBookEntry(Integer registryBookEntryId);

    Page<RegistryBookEntryDTO> getNonPromotedRegistryBookEntries(Integer userId, Pageable pageable);

    Page<RegistryBookEntryDTO> getRegistryBookEntriesForPromotion(Integer promotionId,
                                                                  Pageable pageable);

    RegistryBookEntry createRegistryBookEntry(RegistryBookEntryDTO registryBookEntryDTO,
                                              Integer thesisId);

    RegistryBookEntry migrateRegistryBookEntry(RegistryBookEntryDTO dto, Integer thesisId);

    void updateRegistryBookEntry(Integer registryBookEntryId,
                                 RegistryBookEntryDTO registryBookEntryDTO,
                                 boolean editedByLibrarian);

    void deleteRegistryBookEntry(Integer registryBookEntryId);

    PhdThesisPrePopulatedDataDTO getPrePopulatedPHDThesisInformation(Integer thesisId);

    void addToPromotion(Integer registryBookEntryId, Integer promotionId);

    void removeFromPromotion(Integer registryBookEntryId);

    void removeFromPromotion(String attendanceIdentifier);

    boolean isAttendanceNotCancelled(String attendanceIdentifier);

    void promoteAll(Integer promotionId);

    List<List<String>> previewPromotedEntries(Integer promotionId, String lang);

    Integer hasThesisRegistryBookEntry(Integer thesisId);

    List<String> getPromoteesList(Integer promotionId);

    List<String> getAddressesList(Integer promotionId);

    Page<RegistryBookEntryDTO> getRegistryBookForInstitutionAndPeriod(Integer userId,
                                                                      Integer institutionId,
                                                                      LocalDate from, LocalDate to,
                                                                      String authorName,
                                                                      String authorTitle,
                                                                      Pageable pageable);

    List<InstitutionCountsReportDTO> institutionCountsReport(Integer userId,
                                                             LocalDate from,
                                                             LocalDate to);

    void allowSingleUpdate(Integer registryBookEntryId);

    boolean canEdit(Integer registryBookEntryId, boolean librarianCheck);
}
