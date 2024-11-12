package rs.teslaris.core.service.impl.document;

import jakarta.annotation.Nullable;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.dto.document.PublicationSeriesDTO;
import rs.teslaris.core.model.document.PublicationSeries;
import rs.teslaris.core.repository.document.PublicationSeriesRepository;
import rs.teslaris.core.service.impl.JPAServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.IndexBulkUpdateService;
import rs.teslaris.core.service.interfaces.commontypes.LanguageTagService;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.service.interfaces.document.PublicationSeriesService;
import rs.teslaris.core.service.interfaces.person.PersonContributionService;
import rs.teslaris.core.util.IdentifierUtil;
import rs.teslaris.core.util.email.EmailUtil;

@Service
@Primary
@RequiredArgsConstructor
@Transactional
public class PublicationSeriesServiceImpl extends JPAServiceImpl<PublicationSeries> implements
    PublicationSeriesService {

    protected final PublicationSeriesRepository publicationSeriesRepository;

    protected final MultilingualContentService multilingualContentService;

    protected final LanguageTagService languageTagService;

    protected final PersonContributionService personContributionService;

    protected final EmailUtil emailUtil;

    protected final IndexBulkUpdateService indexBulkUpdateService;


    @Override
    protected JpaRepository<PublicationSeries, Integer> getEntityRepository() {
        return publicationSeriesRepository;
    }

    @Override
    public PublicationSeries findPublicationSeriesById(Integer id) {
        return findOne(id);
    }

    @Override
    @Nullable
    public PublicationSeries findPublicationSeriesByIssn(String eIssn, String printIssn) {
        if (Objects.isNull(eIssn)) { // null will match with other nulls
            eIssn = "";
        }

        if (Objects.isNull(printIssn)) { // null will match with other nulls
            printIssn = "";
        }

        return publicationSeriesRepository.findPublicationSeriesByeISSNOrPrintISSN(eIssn,
            printIssn).orElse(null);
    }

    protected void setPublicationSeriesCommonFields(PublicationSeries publicationSeries,
                                                    PublicationSeriesDTO publicationSeriesDTO) {
        publicationSeries.setTitle(
            multilingualContentService.getMultilingualContent(publicationSeriesDTO.getTitle()));
        publicationSeries.setNameAbbreviation(
            multilingualContentService.getMultilingualContent(
                publicationSeriesDTO.getNameAbbreviation()));

        publicationSeries.setOldId(publicationSeriesDTO.getOldId());

        setCommonIdentifiers(publicationSeries, publicationSeriesDTO);

        publicationSeriesDTO.getLanguageTagIds().forEach(languageTagId -> {
            publicationSeries.getLanguages()
                .add(languageTagService.findLanguageTagById(languageTagId));
        });
    }

    protected void clearPublicationSeriesCommonFields(PublicationSeries publicationSeries) {
        publicationSeries.getContributions().forEach(
            contribution -> personContributionService.deleteContribution(contribution.getId()));
        publicationSeries.getContributions().clear();
    }

    private void setCommonIdentifiers(PublicationSeries publicationSeries,
                                      PublicationSeriesDTO publicationSeriesDTO) {
        IdentifierUtil.validateAndSetIdentifier(
            publicationSeriesDTO.getEissn(),
            publicationSeries.getId(),
            "^(\\d{4}-\\d{4}|\\d{4}-\\d{3}[\\dX]?)$",
            publicationSeriesRepository::existsByeISSN,
            publicationSeries::setEISSN,
            "eissnFormatError",
            "eissnExistsError"
        );

        IdentifierUtil.validateAndSetIdentifier(
            publicationSeriesDTO.getPrintISSN(),
            publicationSeries.getId(),
            "^(\\d{4}-\\d{4}|\\d{4}-\\d{3}[\\dX]?)$",
            publicationSeriesRepository::existsByPrintISSN,
            publicationSeries::setPrintISSN,
            "printIssnFormatError",
            "printIssnExistsError"
        );
    }
}
