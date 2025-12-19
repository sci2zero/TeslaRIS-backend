package rs.teslaris.exporter.model.converter;

import java.util.Objects;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import rs.teslaris.core.model.document.AccessRights;
import rs.teslaris.core.model.document.Conference;
import rs.teslaris.core.model.document.Dataset;
import rs.teslaris.core.model.document.Document;
import rs.teslaris.core.model.document.JournalPublication;
import rs.teslaris.core.model.document.Monograph;
import rs.teslaris.core.model.document.MonographPublication;
import rs.teslaris.core.model.document.Patent;
import rs.teslaris.core.model.document.Proceedings;
import rs.teslaris.core.model.document.ProceedingsPublication;
import rs.teslaris.core.model.document.PublicationSeries;
import rs.teslaris.core.model.document.PublisherPublishable;
import rs.teslaris.core.model.document.ResourceType;
import rs.teslaris.core.model.document.Software;
import rs.teslaris.core.model.document.Thesis;
import rs.teslaris.core.model.institution.OrganisationUnit;
import rs.teslaris.core.model.rocrate.ContextualEntity;
import rs.teslaris.core.model.rocrate.Organization;
import rs.teslaris.core.model.rocrate.Periodical;
import rs.teslaris.core.model.rocrate.RoCrate;
import rs.teslaris.core.model.rocrate.RoCrateDataset;
import rs.teslaris.core.model.rocrate.RoCrateEvent;
import rs.teslaris.core.model.rocrate.RoCrateJournalPublication;
import rs.teslaris.core.model.rocrate.RoCrateMonograph;
import rs.teslaris.core.model.rocrate.RoCrateMonographPublication;
import rs.teslaris.core.model.rocrate.RoCratePatent;
import rs.teslaris.core.model.rocrate.RoCrateProceedings;
import rs.teslaris.core.model.rocrate.RoCrateProceedingsPublication;
import rs.teslaris.core.model.rocrate.RoCratePublicationBase;
import rs.teslaris.core.model.rocrate.RoCratePublishable;
import rs.teslaris.core.model.rocrate.RoCrateSoftware;
import rs.teslaris.core.model.rocrate.RoCrateThesis;
import rs.teslaris.core.repository.document.EventsRelationRepository;
import rs.teslaris.core.util.language.LanguageAbbreviations;
import rs.teslaris.core.util.search.StringUtil;

@Component
public class RoCrateConverter {

    private static final String DEFAULT_RO_CRATE_LANGUAGE = LanguageAbbreviations.ENGLISH;
    private static String identifierPrefix;
    private static String baseUrl;
    private static EventsRelationRepository eventsRelationRepository;

    public static RoCrateDataset toRoCrateModel(Dataset document, String documentIdentifier,
                                                RoCrate metadataInfo) {
        var metadata = new RoCrateDataset();
        setCommonFields(metadata, document, documentIdentifier);
        setPublisherInfo(metadataInfo, metadata, document);
        metadata.setIdentifier(document.getInternalNumber());
        metadata.setDistribution(
            baseUrl + "en/scientific-results/dataset/" + document.getId());

        return metadata;
    }

    public static RoCratePatent toRoCrateModel(Patent document, String documentIdentifier,
                                               RoCrate metadataInfo) {
        var metadata = new RoCratePatent();
        setCommonFields(metadata, document, documentIdentifier);
        setPublisherInfo(metadataInfo, metadata, document);
        metadata.setPatentNumber(document.getNumber());

        return metadata;
    }

    public static RoCrateSoftware toRoCrateModel(Software document, String documentIdentifier,
                                                 RoCrate metadataInfo) {
        var metadata = new RoCrateSoftware();
        setCommonFields(metadata, document, documentIdentifier);
        setPublisherInfo(metadataInfo, metadata, document);
        metadata.setIdentifier(document.getInternalNumber());

        return metadata;
    }

    public static RoCrateMonograph toRoCrateModel(Monograph document, String documentIdentifier,
                                                  RoCrate metadataInfo) {
        var metadata = new RoCrateMonograph();
        setCommonFields(metadata, document, documentIdentifier);
        setPublisherInfo(metadataInfo, metadata, document);
        metadata.setNumberOfPages(document.getNumberOfPages());
        metadata.setBookEdition(document.getNumber());

        if (Objects.nonNull(document.getPublicationSeries())) {
            var issn = getIssn(document.getPublicationSeries());
            if (StringUtil.valueExists(issn)) {
                metadata.setIsPartOf(new ContextualEntity("https://issn.org/issn/"));

                var periodical = new Periodical(
                    StringUtil.getStringContent(document.getPublicationSeries().getTitle(),
                        DEFAULT_RO_CRATE_LANGUAGE),
                    issn,
                    null
                );
                setPublisherInfo(metadataInfo, periodical, document);
                metadataInfo.getGraph().add(periodical);
            }
        }

        return metadata;
    }

    public static RoCrateMonographPublication toRoCrateModel(MonographPublication document,
                                                             String documentIdentifier,
                                                             RoCrate metadataInfo) {
        var metadata = new RoCrateMonographPublication();
        setCommonFields(metadata, document, documentIdentifier);
        setPublisherInfo(metadataInfo, metadata, document.getMonograph());
        metadata.setMonographTitle(StringUtil.getStringContent(document.getMonograph().getTitle(),
            DEFAULT_RO_CRATE_LANGUAGE));
        metadata.setChapterNumber(document.getArticleNumber());
        metadata.setPageStart(document.getStartPage());
        metadata.setPageEnd(document.getEndPage());

        return metadata;
    }

    public static RoCrateProceedings toRoCrateModel(Proceedings document, String documentIdentifier,
                                                    RoCrate metadataInfo) {
        var metadata = new RoCrateProceedings();
        setCommonFields(metadata, document, documentIdentifier);
        setPublisherInfo(metadataInfo, metadata, document);

        if (Objects.nonNull(document.getEvent())) {
            metadata.setConferenceName(StringUtil.getStringContent(document.getEvent().getName(),
                DEFAULT_RO_CRATE_LANGUAGE));

            if (StringUtil.valueExists(((Conference) document.getEvent()).getConfId())) {
                getEventMetadata((Conference) document.getEvent(), metadataInfo);
            }
        }

        return metadata;
    }

    public static RoCrateProceedingsPublication toRoCrateModel(ProceedingsPublication document,
                                                               String documentIdentifier,
                                                               RoCrate metadataInfo) {
        var metadata = new RoCrateProceedingsPublication();
        setCommonFields(metadata, document, documentIdentifier);
        setPublisherInfo(metadataInfo, metadata, document.getProceedings());

        metadata.setInProceedingsTitle(
            StringUtil.getStringContent(document.getProceedings().getTitle(),
                DEFAULT_RO_CRATE_LANGUAGE));

        var isbn = getIsbn(document.getProceedings());
        if (StringUtil.valueExists(isbn)) {
            metadata.setProceedings(new ContextualEntity("https://isbn.org/" + isbn, "Book"));
            toRoCrateModel(document.getProceedings(), isbn, metadataInfo);
        }

        return metadata;
    }

    public static RoCrateJournalPublication toRoCrateModel(JournalPublication document,
                                                           String documentIdentifier,
                                                           RoCrate metadataInfo) {
        var metadata = new RoCrateJournalPublication();
        setCommonFields(metadata, document, documentIdentifier);
        metadata.setVolume(document.getVolume());
        metadata.setIssue(document.getIssue());
        metadata.setPageStart(document.getStartPage());
        metadata.setPageEnd(document.getEndPage());

        metadata.setPeriodicalName(StringUtil.getStringContent(document.getJournal().getTitle(),
            DEFAULT_RO_CRATE_LANGUAGE));

        var issn = getIssn(document.getJournal());
        if (StringUtil.valueExists(issn)) {
            metadata.setIsPartOf(new ContextualEntity("https://issn.org/" + issn, "Periodical"));

            metadataInfo.getGraph().add(new Periodical(
                StringUtil.getStringContent(document.getJournal().getTitle(),
                    DEFAULT_RO_CRATE_LANGUAGE),
                issn,
                null
            ));
        }

        return metadata;
    }

    public static RoCrateThesis toRoCrateModel(Thesis document, String documentIdentifier,
                                               RoCrate metadataInfo) {
        var metadata = new RoCrateThesis();
        setCommonFields(metadata, document, documentIdentifier);
        setPublisherInfo(metadataInfo, metadata, document);

        if (Objects.nonNull(document.getOrganisationUnit())) {
            var ouId = getOrganizationUnitIdentifier(document.getOrganisationUnit());
            metadata.setSourceOrganization(new ContextualEntity(ouId, "Organization"));

            metadataInfo.getGraph().add(new Organization(
                StringUtil.getStringContent(document.getOrganisationUnit().getName(),
                    DEFAULT_RO_CRATE_LANGUAGE),
                document.getOrganisationUnit().getNameAbbreviation(),
                document.getOrganisationUnit().getUris().stream().findFirst().orElse(null),
                Objects.nonNull(document.getOrganisationUnit().getLocation()) ?
                    document.getOrganisationUnit().getLocation().getAddress() : null
            ));
        } else {
            metadata.setSourceOrganization(new ContextualEntity(
                StringUtil.getStringContent(document.getExternalOrganisationUnitName(),
                    DEFAULT_RO_CRATE_LANGUAGE),
                "Organization")
            );
        }

        metadata.setDisplayLocation(
            StringUtil.getStringContent(document.getPlaceOfKeeping(), DEFAULT_RO_CRATE_LANGUAGE));

        document.getFileItems().stream().filter(
                file -> file.getResourceType().equals(ResourceType.OFFICIAL_PUBLICATION) &&
                    file.getAccessRights().equals(AccessRights.OPEN_ACCESS)).findFirst()
            .ifPresent(officialPublication -> {
                metadata.setArchivedAt(
                    baseUrl + "/api/file/" + officialPublication.getServerFilename());

                metadata.setLicense("https://spdx.org/licenses/CC-" +
                    officialPublication.getLicense().name().replace("_", "-") + "-4.0");

                metadata.setIsAccessibleForFree(true);
            });

        metadata.setEducationalLevel(document.getThesisType().name());
        metadata.setInSupportOf(
            StringUtil.getStringContent(document.getTypeOfTitle(), DEFAULT_RO_CRATE_LANGUAGE)
        );

        metadata.setInLanguage(document.getLanguage().getLanguageCode().toLowerCase());

        return metadata;
    }

    private static void setCommonFields(RoCratePublicationBase metadata, Document document,
                                        String primaryIdentifier) {
        metadata.setId(primaryIdentifier);
        metadata.setTitle(StringUtil.getStringContent(document.getTitle(),
            DEFAULT_RO_CRATE_LANGUAGE));
        metadata.setAbstractText(StringUtil.getStringContent(document.getDescription(),
            DEFAULT_RO_CRATE_LANGUAGE));
        metadata.setPublicationYear(document.getDocumentDate());

        metadata.setDoi(document.getDoi());

        if (!primaryIdentifier.startsWith(baseUrl)) {
            metadata.setCiteAs(primaryIdentifier);
        }
    }

    private static void setPublisherInfo(RoCrate metadataInfo,
                                         RoCratePublishable publicationMetadata,
                                         PublisherPublishable document) {
        if (Objects.nonNull(document.getPublisher())) {
            publicationMetadata.setPublisher(
                new ContextualEntity(identifierPrefix + document.getPublisher().getId(),
                    "Publisher"));

            metadataInfo.getGraph().add(new Organization(
                StringUtil.getStringContent(document.getPublisher().getName(),
                    DEFAULT_RO_CRATE_LANGUAGE),
                null,
                null,
                StringUtil.getStringContent(document.getPublisher().getPlace(),
                    DEFAULT_RO_CRATE_LANGUAGE)
            ));
        }
    }

    private static String getIssn(PublicationSeries publicationSeries) {
        return StringUtil.valueExists(publicationSeries.getEISSN()) ?
            publicationSeries.getEISSN() : publicationSeries.getPrintISSN();
    }

    private static String getIsbn(Proceedings proceedings) {
        return StringUtil.valueExists(proceedings.getEISBN()) ?
            proceedings.getEISBN() : proceedings.getPrintISBN();
    }

    private static RoCrateEvent getEventMetadata(Conference conference, RoCrate metadataInfo) {
        var event = new RoCrateEvent();

        event.setId(getEventIdentifier(conference));
        if (StringUtil.valueExists(conference.getConfId())) {
            event.setIdentifier(conference.getConfId());
        }

        event.setName(
            StringUtil.getStringContent(conference.getName(), DEFAULT_RO_CRATE_LANGUAGE));
        event.setStartDate(conference.getDateFrom());
        event.setEndDate(conference.getDateTo());

        if (Objects.nonNull(conference.getCountry())) {
            event.setLocation(
                StringUtil.getStringContent(conference.getCountry().getName(),
                    DEFAULT_RO_CRATE_LANGUAGE));
        }

        if (Objects.nonNull(conference.getKeywords())) {
            event.setKeywords(StringUtil.getStringContent(conference.getKeywords(),
                DEFAULT_RO_CRATE_LANGUAGE));
        }

        if (Objects.nonNull(conference.getUris()) && !conference.getUris().isEmpty()) {
            event.setUrl(conference.getUris().stream().findFirst().get());
        }

        if (!conference.getSerialEvent()) {
            eventsRelationRepository.getSuperEventsForEvent(conference.getId())
                .forEach(superEvent -> {
                    event.getSuperEvents().add(
                        new ContextualEntity(getEventIdentifier((Conference) superEvent), "Event"));
                    metadataInfo.getGraph()
                        .add(getEventMetadata((Conference) superEvent, metadataInfo));
                });

            eventsRelationRepository.getSubEventsForEvent(conference.getId())
                .forEach(subEvent -> {
                    event.getSubEvents().add(
                        new ContextualEntity(getEventIdentifier((Conference) subEvent), "Event"));
                    metadataInfo.getGraph()
                        .add(getEventMetadata((Conference) subEvent, metadataInfo));
                });
        }

        return event;
    }

    private static String getEventIdentifier(Conference conference) {
        if (StringUtil.valueExists(conference.getConfId())) {
            return "https://confid.org/conf/" + conference.getConfId();
        } else {
            return baseUrl + "en/events/conference/" + conference.getId();
        }
    }

    private static String getOrganizationUnitIdentifier(OrganisationUnit organisationUnit) {
        if (StringUtil.valueExists(organisationUnit.getRor())) {
            return "https://ror.org/" + organisationUnit.getRor();
        } else {
            return baseUrl + "en/organisation-units/" + organisationUnit.getId();
        }
    }

    @Value("${export.base.url}")
    public void setBaseUrl(String baseUrl) {
        var hasEndSlash = baseUrl.endsWith("/");
        RoCrateConverter.baseUrl = baseUrl + (hasEndSlash ? "" : "/");
    }

    @Value("${export.internal-identifier.prefix}")
    public void setIdentifierPrefix(String identifierPrefix) {
        RoCrateConverter.identifierPrefix = identifierPrefix;
    }
}
