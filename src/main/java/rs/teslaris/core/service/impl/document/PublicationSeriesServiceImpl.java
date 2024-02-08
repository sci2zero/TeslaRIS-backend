package rs.teslaris.core.service.impl.document;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.dto.document.PublicationSeriesDTO;
import rs.teslaris.core.model.document.PublicationSeries;
import rs.teslaris.core.repository.document.PublicationSeriesRepository;
import rs.teslaris.core.service.impl.JPAServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.LanguageTagService;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.service.interfaces.document.PublicationSeriesService;
import rs.teslaris.core.service.interfaces.person.PersonContributionService;
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


    @Override
    protected JpaRepository<PublicationSeries, Integer> getEntityRepository() {
        return publicationSeriesRepository;
    }

    @Override
    public PublicationSeries findPublicationSeriesById(Integer id) {
        return findOne(id);
    }

    @Override
    public void setPublicationSeriesCommonFields(PublicationSeries publicationSeries,
                                                 PublicationSeriesDTO publicationSeriesDTO) {
        publicationSeries.setTitle(
            multilingualContentService.getMultilingualContent(publicationSeriesDTO.getTitle()));
        publicationSeries.setNameAbbreviation(
            multilingualContentService.getMultilingualContent(
                publicationSeriesDTO.getNameAbbreviation()));

        publicationSeries.setEISSN(publicationSeriesDTO.getEissn());
        publicationSeries.setPrintISSN(publicationSeriesDTO.getPrintISSN());

        publicationSeriesDTO.getLanguageTagIds().forEach(languageTagId -> {
            publicationSeries.getLanguages()
                .add(languageTagService.findLanguageTagById(languageTagId));
        });
    }
}
