package rs.teslaris.dbinitialization;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.assessment.model.AssessmentMeasure;
import rs.teslaris.assessment.model.AssessmentRulebook;
import rs.teslaris.assessment.model.classification.AssessmentClassification;
import rs.teslaris.assessment.model.indicator.ApplicableEntityType;
import rs.teslaris.assessment.model.indicator.Indicator;
import rs.teslaris.assessment.model.indicator.IndicatorContentType;
import rs.teslaris.assessment.repository.AssessmentMeasureRepository;
import rs.teslaris.assessment.repository.AssessmentRulebookRepository;
import rs.teslaris.assessment.repository.CommissionRelationRepository;
import rs.teslaris.assessment.repository.classification.AssessmentClassificationRepository;
import rs.teslaris.assessment.repository.indicator.IndicatorRepository;
import rs.teslaris.core.model.commontypes.AccessLevel;
import rs.teslaris.core.model.commontypes.LanguageTag;
import rs.teslaris.core.model.commontypes.MultiLingualContent;
import rs.teslaris.core.model.institution.Commission;
import rs.teslaris.core.model.institution.CommissionRelation;
import rs.teslaris.core.model.institution.ResultCalculationMethod;
import rs.teslaris.core.repository.institution.CommissionRepository;
import rs.teslaris.core.util.functional.Pair;

@Component
@RequiredArgsConstructor
@Transactional
public class AssessmentDataInitializer {

    private final AssessmentClassificationRepository assessmentClassificationRepository;

    private final IndicatorRepository indicatorRepository;

    private final CommissionRepository commissionRepository;

    private final CommissionRelationRepository commissionRelationRepository;

    private final AssessmentMeasureRepository assessmentMeasureRepository;

    private final AssessmentRulebookRepository assessmentRulebookRepository;


    public void initializeIndicators(LanguageTag englishTag, LanguageTag serbianTag) {
        var totalViews = new Indicator();
        totalViews.setCode("viewsTotal");
        totalViews.setTitle(Set.of(new MultiLingualContent(englishTag, "Total views", 1),
            new MultiLingualContent(serbianTag, "Ukupno pregleda", 2)));
        totalViews.setDescription(
            Set.of(
                new MultiLingualContent(englishTag, "Total number of views.",
                    1),
                new MultiLingualContent(serbianTag, "Ukupan broj pregleda.",
                    2)));
        totalViews.setAccessLevel(AccessLevel.OPEN);
        totalViews.getApplicableTypes().addAll(
            List.of(ApplicableEntityType.DOCUMENT, ApplicableEntityType.PERSON,
                ApplicableEntityType.ORGANISATION_UNIT, ApplicableEntityType.PUBLICATION_SERIES,
                ApplicableEntityType.EVENT));
        totalViews.setContentType(IndicatorContentType.NUMBER);

        var yearlyViews = new Indicator();
        yearlyViews.setCode("viewsYear");
        yearlyViews.setTitle(Set.of(new MultiLingualContent(englishTag, "This year's views", 1),
            new MultiLingualContent(serbianTag, "Pregleda ove godine", 2)));
        yearlyViews.setDescription(
            Set.of(
                new MultiLingualContent(englishTag, "Total number of views in the last year.",
                    1),
                new MultiLingualContent(serbianTag,
                    "Ukupan broj pregleda u poslednjih godinu dana.",
                    2)));
        yearlyViews.setAccessLevel(AccessLevel.OPEN);
        yearlyViews.getApplicableTypes().addAll(
            List.of(ApplicableEntityType.DOCUMENT, ApplicableEntityType.PERSON,
                ApplicableEntityType.ORGANISATION_UNIT));
        yearlyViews.setContentType(IndicatorContentType.NUMBER);

        var weeklyViews = new Indicator();
        weeklyViews.setCode("viewsWeek");
        weeklyViews.setTitle(Set.of(new MultiLingualContent(englishTag, "Week's views", 1),
            new MultiLingualContent(serbianTag, "Pregleda ove sedmice", 2)));
        weeklyViews.setDescription(
            Set.of(
                new MultiLingualContent(englishTag, "Total number of views in the last 7 days.",
                    1),
                new MultiLingualContent(serbianTag, "Ukupan broj pregleda u poslednjih 7 dana.",
                    2)));
        weeklyViews.setAccessLevel(AccessLevel.OPEN);
        weeklyViews.getApplicableTypes().addAll(
            List.of(ApplicableEntityType.DOCUMENT, ApplicableEntityType.PERSON,
                ApplicableEntityType.ORGANISATION_UNIT));
        weeklyViews.setContentType(IndicatorContentType.NUMBER);

        var monthlyViews = new Indicator();
        monthlyViews.setCode("viewsMonth");
        monthlyViews.setTitle(Set.of(new MultiLingualContent(englishTag, "Month's views", 1),
            new MultiLingualContent(serbianTag, "Pregleda ovog meseca", 2)));
        monthlyViews.setDescription(
            Set.of(
                new MultiLingualContent(englishTag, "Total number of views in the last month.",
                    1),
                new MultiLingualContent(serbianTag, "Ukupan broj pregleda u poslednjih mesec dana.",
                    2)));
        monthlyViews.setAccessLevel(AccessLevel.OPEN);
        monthlyViews.getApplicableTypes().addAll(
            List.of(ApplicableEntityType.DOCUMENT, ApplicableEntityType.PERSON,
                ApplicableEntityType.ORGANISATION_UNIT));
        monthlyViews.setContentType(IndicatorContentType.NUMBER);

        var totalDownloads = new Indicator();
        totalDownloads.setCode("downloadsTotal");
        totalDownloads.setTitle(Set.of(new MultiLingualContent(englishTag, "Total downloads", 1),
            new MultiLingualContent(serbianTag, "Ukupno preuzimanja", 2)));
        totalDownloads.setDescription(
            Set.of(
                new MultiLingualContent(englishTag, "Total number of downloads.",
                    1),
                new MultiLingualContent(serbianTag, "Ukupan broj preuzimanja.",
                    2)));
        totalDownloads.setAccessLevel(AccessLevel.OPEN);
        totalDownloads.getApplicableTypes().addAll(
            List.of(ApplicableEntityType.DOCUMENT, ApplicableEntityType.PERSON,
                ApplicableEntityType.ORGANISATION_UNIT));
        totalDownloads.setContentType(IndicatorContentType.NUMBER);

        var yearlyDownloads = new Indicator();
        yearlyDownloads.setCode("downloadsYear");
        yearlyDownloads.setTitle(
            Set.of(new MultiLingualContent(englishTag, "This year's downloads", 1),
                new MultiLingualContent(serbianTag, "Preuzimanja ove godine", 2)));
        yearlyDownloads.setDescription(
            Set.of(
                new MultiLingualContent(englishTag, "Total number of downloads in the last year.",
                    1),
                new MultiLingualContent(serbianTag,
                    "Ukupan broj preuzimanja u poslednjih godinu dana.",
                    2)));
        yearlyDownloads.setAccessLevel(AccessLevel.OPEN);
        yearlyDownloads.getApplicableTypes().addAll(
            List.of(ApplicableEntityType.DOCUMENT, ApplicableEntityType.PERSON,
                ApplicableEntityType.ORGANISATION_UNIT));
        yearlyDownloads.setContentType(IndicatorContentType.NUMBER);

        var weeklyDownloads = new Indicator();
        weeklyDownloads.setCode("downloadsWeek");
        weeklyDownloads.setTitle(Set.of(new MultiLingualContent(englishTag, "Week's downloads", 1),
            new MultiLingualContent(serbianTag, "Preuzimanja ove sedmice", 2)));
        weeklyDownloads.setDescription(
            Set.of(
                new MultiLingualContent(englishTag, "Total number of downloads in the last 7 days.",
                    1),
                new MultiLingualContent(serbianTag, "Ukupan broj preuzimanja u poslednjih 7 dana.",
                    2)));
        weeklyDownloads.setAccessLevel(AccessLevel.OPEN);
        weeklyDownloads.getApplicableTypes().addAll(
            List.of(ApplicableEntityType.DOCUMENT, ApplicableEntityType.PERSON,
                ApplicableEntityType.ORGANISATION_UNIT));
        weeklyDownloads.setContentType(IndicatorContentType.NUMBER);

        var monthlyDownloads = new Indicator();
        monthlyDownloads.setCode("downloadsMonth");
        monthlyDownloads.setTitle(
            Set.of(new MultiLingualContent(englishTag, "Month's downloads", 1),
                new MultiLingualContent(serbianTag, "Preuzimanja ovog meseca", 2)));
        monthlyDownloads.setDescription(
            Set.of(
                new MultiLingualContent(englishTag, "Total number of downloads in the last month.",
                    1),
                new MultiLingualContent(serbianTag,
                    "Ukupan broj preuzimanja u poslednjih mesec dana.",
                    2)));
        monthlyDownloads.setAccessLevel(AccessLevel.OPEN);
        monthlyDownloads.getApplicableTypes().addAll(
            List.of(ApplicableEntityType.DOCUMENT, ApplicableEntityType.PERSON,
                ApplicableEntityType.ORGANISATION_UNIT));
        monthlyDownloads.setContentType(IndicatorContentType.NUMBER);

        var numberOfPages = new Indicator();
        numberOfPages.setCode("pageNum");
        numberOfPages.setTitle(
            Set.of(new MultiLingualContent(englishTag, "Number of pages", 1),
                new MultiLingualContent(serbianTag, "Broj stranica", 2)));
        numberOfPages.setDescription(
            Set.of(
                new MultiLingualContent(englishTag, "Total number of pages in a document.",
                    1),
                new MultiLingualContent(serbianTag,
                    "Ukupan broj stranica u dokumentu.",
                    2)));
        numberOfPages.setAccessLevel(AccessLevel.CLOSED);
        numberOfPages.getApplicableTypes().add(ApplicableEntityType.MONOGRAPH);
        numberOfPages.setContentType(IndicatorContentType.NUMBER);

        var totalCitations = new Indicator();
        totalCitations.setCode("totalCitations");
        totalCitations.setTitle(
            Set.of(new MultiLingualContent(englishTag, "Total citations", 1),
                new MultiLingualContent(serbianTag, "Broj citata", 2)));
        totalCitations.setDescription(
            Set.of(
                new MultiLingualContent(englishTag, "Total citation count.",
                    1),
                new MultiLingualContent(serbianTag,
                    "Ukupan broj citata.",
                    2)));
        totalCitations.setAccessLevel(AccessLevel.CLOSED);
        totalCitations.getApplicableTypes().addAll(
            List.of(ApplicableEntityType.PUBLICATION_SERIES, ApplicableEntityType.DOCUMENT,
                ApplicableEntityType.PERSON, ApplicableEntityType.ORGANISATION_UNIT));
        totalCitations.setContentType(IndicatorContentType.NUMBER);

        var yearlyCitations = new Indicator();
        yearlyCitations.setCode("yearlyCitations");
        yearlyCitations.setTitle(
            Set.of(new MultiLingualContent(englishTag, "Yearly citations", 1),
                new MultiLingualContent(serbianTag, "Godišnja citiranost", 2)));
        yearlyCitations.setDescription(
            Set.of(
                new MultiLingualContent(englishTag, "Total citation count by year.",
                    1),
                new MultiLingualContent(serbianTag,
                    "Ukupan broj citata po godinama.",
                    2)));
        yearlyCitations.setAccessLevel(AccessLevel.CLOSED);
        yearlyCitations.getApplicableTypes().addAll(
            List.of(ApplicableEntityType.PERSON, ApplicableEntityType.ORGANISATION_UNIT));
        yearlyCitations.setContentType(IndicatorContentType.NUMBER);

        var fiveYearJIF = new Indicator();
        fiveYearJIF.setCode("fiveYearJIF");
        fiveYearJIF.setTitle(
            Set.of(new MultiLingualContent(englishTag, "5 Year JIF", 1),
                new MultiLingualContent(serbianTag, "Petogodišnji IF", 2)));
        fiveYearJIF.setDescription(
            Set.of(
                new MultiLingualContent(englishTag, "JIF in the last 5 years.",
                    1),
                new MultiLingualContent(serbianTag,
                    "IF u poslednjih 5 godina.",
                    2)));
        fiveYearJIF.setAccessLevel(AccessLevel.CLOSED);
        fiveYearJIF.getApplicableTypes().add(ApplicableEntityType.PUBLICATION_SERIES);
        fiveYearJIF.setContentType(IndicatorContentType.NUMBER);

        var currentJIF = new Indicator();
        currentJIF.setCode("currentJIF");
        currentJIF.setTitle(
            Set.of(new MultiLingualContent(englishTag, "Journal Impact Factor", 1),
                new MultiLingualContent(serbianTag, "Impakt Faktor", 2)));
        currentJIF.setDescription(
            Set.of(
                new MultiLingualContent(englishTag, "JIF in the current year.",
                    1),
                new MultiLingualContent(serbianTag,
                    "IF ove godine.",
                    2)));
        currentJIF.setAccessLevel(AccessLevel.CLOSED);
        currentJIF.getApplicableTypes().add(ApplicableEntityType.PUBLICATION_SERIES);
        currentJIF.setContentType(IndicatorContentType.NUMBER);

        var fiveYearJIFRank = new Indicator();
        fiveYearJIFRank.setCode("fiveYearJIFRank");
        fiveYearJIFRank.setTitle(
            Set.of(new MultiLingualContent(englishTag, "5 Year JIF Rank", 1),
                new MultiLingualContent(serbianTag, "Petogodišnji IF Rank", 2)));
        fiveYearJIFRank.setDescription(
            Set.of(
                new MultiLingualContent(englishTag, "JIF Rank in the last 5 years.",
                    1),
                new MultiLingualContent(serbianTag,
                    "IF Rank u poslednjih 5 godina.",
                    2)));
        fiveYearJIFRank.setAccessLevel(AccessLevel.CLOSED);
        fiveYearJIFRank.getApplicableTypes().add(ApplicableEntityType.PUBLICATION_SERIES);
        fiveYearJIFRank.setContentType(IndicatorContentType.TEXT);

        var currentJIFRank = new Indicator();
        currentJIFRank.setCode("currentJIFRank");
        currentJIFRank.setTitle(
            Set.of(new MultiLingualContent(englishTag, "JIF Rank", 1),
                new MultiLingualContent(serbianTag, "IF Rank", 2)));
        currentJIFRank.setDescription(
            Set.of(
                new MultiLingualContent(englishTag, "JIF rank in the current year.",
                    1),
                new MultiLingualContent(serbianTag,
                    "IF rank ove godine.",
                    2)));
        currentJIFRank.setAccessLevel(AccessLevel.CLOSED);
        currentJIFRank.getApplicableTypes().add(ApplicableEntityType.PUBLICATION_SERIES);
        currentJIFRank.setContentType(IndicatorContentType.TEXT);

        var eigenFactorNorm = new Indicator();
        eigenFactorNorm.setCode("eigenFactorNorm");
        eigenFactorNorm.setTitle(
            Set.of(new MultiLingualContent(englishTag, "Normalized Eigenfactor", 1),
                new MultiLingualContent(serbianTag, "Normalizovani Eigenfactor", 2)));
        eigenFactorNorm.setDescription(
            Set.of(
                new MultiLingualContent(englishTag,
                    "A measure of the total influence of a journal over a 5-year period, considering both the quantity and quality of citations.",
                    1),
                new MultiLingualContent(serbianTag,
                    "Mera ukupnog uticaja časopisa u periodu od 5 godina, uzevši u obzir i kvalitet i kvantitet citata.",
                    2)));
        eigenFactorNorm.setAccessLevel(AccessLevel.CLOSED);
        eigenFactorNorm.getApplicableTypes().add(ApplicableEntityType.PUBLICATION_SERIES);
        eigenFactorNorm.setContentType(IndicatorContentType.NUMBER);

        var ais = new Indicator();
        ais.setCode("ais");
        ais.setTitle(
            Set.of(new MultiLingualContent(englishTag, "Article Influence Score", 1),
                new MultiLingualContent(serbianTag, "Article Influence Score", 2)));
        ais.setDescription(
            Set.of(
                new MultiLingualContent(englishTag,
                    "Metric used to measure the average influence of a journal's articles over the first five years after publication.",
                    1),
                new MultiLingualContent(serbianTag,
                    "Metrika koja se koristi da prikaže srednju vrednost uticaja radova u časopisu kroz prvih pet godina nakon publikacije.",
                    2)));
        ais.setAccessLevel(AccessLevel.CLOSED);
        ais.getApplicableTypes().add(ApplicableEntityType.PUBLICATION_SERIES);
        ais.setContentType(IndicatorContentType.NUMBER);

        var citedHL = new Indicator();
        citedHL.setCode("citedHL");
        citedHL.setTitle(
            Set.of(new MultiLingualContent(englishTag, "Cited Half-Life", 1),
                new MultiLingualContent(serbianTag, "Cited Half-Life", 2)));
        citedHL.setDescription(
            Set.of(
                new MultiLingualContent(englishTag,
                    "Calculates the median age of articles cited by a journal during a calendar year. Half of the citations reference articles published before this time, and half reference articles published afterwards.",
                    1),
                new MultiLingualContent(serbianTag,
                    "Izračunava srednju starost članaka citiranih u časopisu tokom jedne kalendarske godine. Polovina citata se odnosi na članke objavljene pre ovog vremena, a polovina na članke objavljene kasnije.",
                    2)));
        citedHL.setAccessLevel(AccessLevel.CLOSED);
        citedHL.getApplicableTypes().add(ApplicableEntityType.PUBLICATION_SERIES);
        citedHL.setContentType(IndicatorContentType.NUMBER);

        var citingHL = new Indicator();
        citingHL.setCode("citingHL");
        citingHL.setTitle(
            Set.of(new MultiLingualContent(englishTag, "Citing Half-Life", 1),
                new MultiLingualContent(serbianTag, "Citing Half-Life", 2)));
        citingHL.setDescription(
            Set.of(
                new MultiLingualContent(englishTag,
                    "Counts all the references made by the journal during one calendar year and calculates the median article publication date—half of the cited references were published before this time, half were published afterwards.",
                    1),
                new MultiLingualContent(serbianTag,
                    "Broji sve reference koje je časopis naveo tokom jedne kalendarske godine i izračunava srednji datum objavljivanja članka – polovina citiranih referenci je objavljena pre ovog vremena, polovina je objavljena kasnije.",
                    2)));
        citingHL.setAccessLevel(AccessLevel.CLOSED);
        citingHL.getApplicableTypes().add(ApplicableEntityType.PUBLICATION_SERIES);
        citingHL.setContentType(IndicatorContentType.NUMBER);


        var sjr = new Indicator();
        sjr.setCode("sjr");
        sjr.setTitle(
            Set.of(new MultiLingualContent(englishTag, "SCImago Journal Rank", 1),
                new MultiLingualContent(serbianTag, "SCImago Journal Rank", 2)));
        sjr.setDescription(
            Set.of(
                new MultiLingualContent(englishTag,
                    "The SCImago Journal Rank (SJR) indicator is a measure of the prestige of scholarly journals that accounts for both the number of citations received by a journal and the prestige of the journals where the citations come from.",
                    1),
                new MultiLingualContent(serbianTag,
                    "SCImago Journal Rank (SJR) je mera prestiža naučnog časopisa koja uzima u obzir i broj citata i prestiž časopisa iz kojeg ti citati dolaze.",
                    2)));
        sjr.setAccessLevel(AccessLevel.CLOSED);
        sjr.getApplicableTypes().add(ApplicableEntityType.PUBLICATION_SERIES);
        sjr.setContentType(IndicatorContentType.TEXT);

        var hIndex = new Indicator();
        hIndex.setCode("hIndex");
        hIndex.setTitle(
            Set.of(new MultiLingualContent(englishTag, "H Index", 1),
                new MultiLingualContent(serbianTag, "H Index", 2)));
        hIndex.setDescription(
            Set.of(
                new MultiLingualContent(englishTag,
                    "The h-index is calculated by counting the number of publications for which an author has been cited by other authors at least that same number of times.",
                    1),
                new MultiLingualContent(serbianTag,
                    "H-indeks se izračunava kao ukupan broj publikacija koje su citirali drugi autori najmanje isti broj puta.",
                    2)));
        hIndex.setAccessLevel(AccessLevel.CLOSED);
        hIndex.getApplicableTypes()
            .addAll(List.of(ApplicableEntityType.PUBLICATION_SERIES, ApplicableEntityType.PERSON));
        hIndex.setContentType(IndicatorContentType.NUMBER);

        var totalOutputCount = new Indicator();
        totalOutputCount.setCode("totalOutputCount");
        totalOutputCount.setTitle(
            Set.of(new MultiLingualContent(englishTag, "Total results", 1),
                new MultiLingualContent(serbianTag, "Ukupno rezultata", 2)));
        totalOutputCount.setDescription(
            Set.of(
                new MultiLingualContent(englishTag,
                    "Total number of all publication types published.",
                    1),
                new MultiLingualContent(serbianTag,
                    "Ukupan broj publikacija svih tipova.",
                    2)));
        totalOutputCount.setAccessLevel(AccessLevel.CLOSED);
        totalOutputCount.getApplicableTypes()
            .addAll(List.of(ApplicableEntityType.ORGANISATION_UNIT, ApplicableEntityType.PERSON));
        totalOutputCount.setContentType(IndicatorContentType.NUMBER);

        var sdg = new Indicator();
        sdg.setCode("sdg");
        sdg.setTitle(
            Set.of(new MultiLingualContent(englishTag, "Sustainable Development Goals (SDG)", 1),
                new MultiLingualContent(serbianTag, "Sustainable Development Goals (SDG)", 2)));
        sdg.setDescription(
            Set.of(
                new MultiLingualContent(englishTag,
                    "SDG represents the alignment of journals with the United Nations Sustainable Development Goals based on their research focus and contribution to global sustainability.",
                    1),
                new MultiLingualContent(serbianTag,
                    "SDG označava povezanost časopisa sa Ciljevima održivog razvoja Ujedinjenih nacija na osnovu njihovog istraživačkog fokusa i doprinosa globalnoj održivosti.",
                    2)));
        sdg.setAccessLevel(AccessLevel.CLOSED);
        sdg.getApplicableTypes().add(ApplicableEntityType.PUBLICATION_SERIES);
        sdg.setContentType(IndicatorContentType.NUMBER);

        var overton = new Indicator();
        overton.setCode("overton");
        overton.setTitle(
            Set.of(new MultiLingualContent(englishTag, "Overton", 1),
                new MultiLingualContent(serbianTag, "Overton", 2)));
        overton.setDescription(
            Set.of(
                new MultiLingualContent(englishTag,
                    "Overton measures the influence of academic journals by tracking citations in policy documents, legal texts, patents, and other non-academic sources, reflecting their impact on public policy and practical applications.",
                    1),
                new MultiLingualContent(serbianTag,
                    "Overton meri uticaj akademskih časopisa praćenjem citata u političkim dokumentima, pravnim tekstovima, patentima i drugim neakademskim izvorima, čime odražava njihov uticaj na javne politike i praktične primene.",
                    2)));
        overton.setAccessLevel(AccessLevel.CLOSED);
        overton.getApplicableTypes().add(ApplicableEntityType.PUBLICATION_SERIES);
        overton.setContentType(IndicatorContentType.NUMBER);

        var erihPlus = new Indicator();
        erihPlus.setCode("erihPlus");
        erihPlus.setTitle(
            Set.of(new MultiLingualContent(englishTag, "ERIH PLUS list", 1),
                new MultiLingualContent(serbianTag, "ERIH PLUS lista", 2)));
        erihPlus.setDescription(
            Set.of(
                new MultiLingualContent(englishTag,
                    "The ERIH PLUS list is a European reference index for scientific journals in the humanities and social sciences, aimed at enhancing their visibility and quality.",
                    1),
                new MultiLingualContent(serbianTag,
                    "ERIH PLUS lista je evropski referentni indeks za naučne časopise iz oblasti humanističkih i društvenih nauka, sa ciljem povećanja njihove vidljivosti i kvaliteta.",
                    2)));
        erihPlus.setAccessLevel(AccessLevel.CLOSED);
        erihPlus.getApplicableTypes().add(ApplicableEntityType.PUBLICATION_SERIES);
        erihPlus.setContentType(IndicatorContentType.BOOL);

        var jci = new Indicator();
        jci.setCode("jci");
        jci.setTitle(
            Set.of(new MultiLingualContent(englishTag, "Journal Citation Indicator (JCI)", 1),
                new MultiLingualContent(serbianTag, "Indikator citiranosti časopisa (JCI)", 2)));
        jci.setDescription(
            Set.of(
                new MultiLingualContent(englishTag,
                    "Normalized metric that measures the citation impact of a journal's publications over a three-year period, allowing for comparison across disciplines.",
                    1),
                new MultiLingualContent(serbianTag,
                    "Normalizovana metrika koja meri uticaj citiranosti publikacija časopisa tokom trogodišnjeg perioda, omogućavajući poređenje među disciplinama.",
                    2)));
        jci.setAccessLevel(AccessLevel.CLOSED);
        jci.getApplicableTypes().add(ApplicableEntityType.PUBLICATION_SERIES);
        jci.setContentType(IndicatorContentType.NUMBER);

        var jciPercentile = new Indicator();
        jciPercentile.setCode("jciPercentile");
        jciPercentile.setTitle(
            Set.of(
                new MultiLingualContent(englishTag, "Journal Citation Indicator (JCI) percentile",
                    1),
                new MultiLingualContent(serbianTag,
                    "Indikator citiranosti časopisa (JCI) percentil", 2)));
        jciPercentile.setDescription(
            Set.of(
                new MultiLingualContent(englishTag,
                    "The Journal Citation Indicator (JCI) percentile shows a journal's relative position within its research field, where a higher percentile indicates stronger performance compared to other journals in the same category.",
                    1),
                new MultiLingualContent(serbianTag,
                    "Percentil Journal Citation Indicator-a (JCI) prikazuje relativnu poziciju časopisa unutar svoje oblasti istraživanja, pri čemu veći procenat označava bolje performanse u odnosu na druge časopise u istoj kategoriji.",
                    2)));
        jciPercentile.setAccessLevel(AccessLevel.CLOSED);
        jciPercentile.getApplicableTypes().add(ApplicableEntityType.PUBLICATION_SERIES);
        jciPercentile.setContentType(IndicatorContentType.NUMBER);

        var jcr = new Indicator();
        jcr.setCode("jcr");
        jcr.setTitle(
            Set.of(new MultiLingualContent(englishTag, "Journal Citation Rank (JCR) list", 1),
                new MultiLingualContent(serbianTag, "Journal Citation Rank (JCR) lista", 2)));
        jcr.setDescription(
            Set.of(
                new MultiLingualContent(englishTag,
                    "The Journal Citation Reports (JCR) list provides metrics like Impact Factor to evaluate and rank scientific journals based on citation data.",
                    1),
                new MultiLingualContent(serbianTag,
                    "Lista Journal Citation Reports (JCR) pruža metrike, poput faktora uticaja, za ocenjivanje i rangiranje naučnih časopisa na osnovu podataka o citatima.",
                    2)));
        jcr.setAccessLevel(AccessLevel.CLOSED);
        jcr.getApplicableTypes().add(ApplicableEntityType.PUBLICATION_SERIES);
        jcr.setContentType(IndicatorContentType.BOOL);

        var scimago = new Indicator();
        scimago.setCode("scimago");
        scimago.setTitle(
            Set.of(new MultiLingualContent(englishTag, "SciMAGO list", 1),
                new MultiLingualContent(serbianTag, "SciMago lista", 2)));
        scimago.setDescription(
            Set.of(
                new MultiLingualContent(englishTag,
                    "The SciMAGO Journal Rank (SJR) list ranks journals using citation data from the Scopus database, emphasizing visibility and prestige.",
                    1),
                new MultiLingualContent(serbianTag,
                    "Lista SciMAGO Journal Rank (SJR) rangira časopise koristeći podatke o citatima iz Scopus baze, naglašavajući vidljivost i prestiž.",
                    2)));
        scimago.setAccessLevel(AccessLevel.CLOSED);
        scimago.getApplicableTypes().add(ApplicableEntityType.PUBLICATION_SERIES);
        scimago.setContentType(IndicatorContentType.BOOL);

        var numParticipants = new Indicator();
        numParticipants.setCode("numParticipants");
        numParticipants.setTitle(
            Set.of(new MultiLingualContent(englishTag, "Number of Participants", 1),
                new MultiLingualContent(serbianTag, "Broj učesnika", 2)));
        numParticipants.setDescription(
            Set.of(
                new MultiLingualContent(englishTag,
                    "Number of conference participants.",
                    1),
                new MultiLingualContent(serbianTag,
                    "Broj učesnika na konferenciji.",
                    2)));
        numParticipants.setAccessLevel(AccessLevel.CLOSED);
        numParticipants.getApplicableTypes().add(ApplicableEntityType.EVENT);
        numParticipants.setContentType(IndicatorContentType.NUMBER);

        var numPresentations = new Indicator();
        numPresentations.setCode("numPresentations");
        numPresentations.setTitle(Set.of(
            new MultiLingualContent(englishTag, "Number of Presentations", 1),
            new MultiLingualContent(serbianTag, "Broj saopštenja na skupu", 2)
        ));
        numPresentations.setDescription(Set.of(
            new MultiLingualContent(englishTag, "Number of presentations at the conference.", 1),
            new MultiLingualContent(serbianTag, "Broj saopštenja na skupu.", 2)
        ));
        numPresentations.setAccessLevel(AccessLevel.CLOSED);
        numPresentations.getApplicableTypes().add(ApplicableEntityType.EVENT);
        numPresentations.setContentType(IndicatorContentType.NUMBER);

        var numParticipantCountries = new Indicator();
        numParticipantCountries.setCode("numParticipantCountries");
        numParticipantCountries.setTitle(Set.of(
            new MultiLingualContent(englishTag, "Number of Participant Countries", 1),
            new MultiLingualContent(serbianTag, "Broj zemalja koji imaju učesnika na skupu", 2)
        ));
        numParticipantCountries.setDescription(Set.of(
            new MultiLingualContent(englishTag,
                "Number of countries represented by participants at the conference.", 1),
            new MultiLingualContent(serbianTag, "Broj zemalja koji imaju učesnika na skupu.", 2)
        ));
        numParticipantCountries.setAccessLevel(AccessLevel.CLOSED);
        numParticipantCountries.getApplicableTypes().add(ApplicableEntityType.EVENT);
        numParticipantCountries.setContentType(IndicatorContentType.NUMBER);

        var numCountriesInScientificCommittee = new Indicator();
        numCountriesInScientificCommittee.setCode("numCountriesInScientificCommittee");
        numCountriesInScientificCommittee.setTitle(Set.of(
            new MultiLingualContent(englishTag,
                "Number of Countries Represented in Scientific Committee", 1),
            new MultiLingualContent(serbianTag,
                "Broj zemalja koji imaju učesnika u naučnom odboru skupa", 2)
        ));
        numCountriesInScientificCommittee.setDescription(Set.of(
            new MultiLingualContent(englishTag,
                "Number of countries represented in the scientific committee.", 1),
            new MultiLingualContent(serbianTag,
                "Broj zemalja koji imaju učesnika u naučnom odboru skupa.", 2)
        ));
        numCountriesInScientificCommittee.setAccessLevel(AccessLevel.CLOSED);
        numCountriesInScientificCommittee.getApplicableTypes().add(ApplicableEntityType.EVENT);
        numCountriesInScientificCommittee.setContentType(IndicatorContentType.NUMBER);

        var organizedByScientificInstitution = new Indicator();
        organizedByScientificInstitution.setCode("organizedByScientificInstitution");
        organizedByScientificInstitution.setTitle(Set.of(
            new MultiLingualContent(englishTag, "Organized by Scientific or National Institution",
                1),
            new MultiLingualContent(serbianTag,
                "Skup organizuje naučno-istraživačka institucija, ili institucija od nacionalnog značaja",
                2)
        ));
        organizedByScientificInstitution.setDescription(Set.of(
            new MultiLingualContent(englishTag,
                "Indicates if the event is organized by a scientific research or national institution.",
                1),
            new MultiLingualContent(serbianTag,
                "Da li skup organizuje naučno-istraživačka institucija, ili institucija od nacionalnog značaja.",
                2)
        ));
        organizedByScientificInstitution.setAccessLevel(AccessLevel.CLOSED);
        organizedByScientificInstitution.getApplicableTypes().add(ApplicableEntityType.EVENT);
        organizedByScientificInstitution.setContentType(IndicatorContentType.BOOL);

        var slavistiCategory = new Indicator();
        slavistiCategory.setCode("slavistiCategory");
        slavistiCategory.setTitle(Set.of(
            new MultiLingualContent(englishTag, "MKS Slavists Category",
                1),
            new MultiLingualContent(serbianTag,
                "MKS Slavisti Kategorija",
                2)
        ));
        slavistiCategory.setDescription(Set.of(
            new MultiLingualContent(englishTag,
                "Reference list of slavistic magazines issued by international committee of Slavists.",
                1),
            new MultiLingualContent(serbianTag,
                "Referentna lista slavističkih časopisa međunarodnog komiteta slavista.",
                2)
        ));
        slavistiCategory.setAccessLevel(AccessLevel.CLOSED);
        slavistiCategory.getApplicableTypes().add(ApplicableEntityType.PUBLICATION_SERIES);
        slavistiCategory.setContentType(IndicatorContentType.TEXT);

        var lectureInvitation = new Indicator();
        lectureInvitation.setCode("lectureInvitation");
        lectureInvitation.setTitle(Set.of(
            new MultiLingualContent(englishTag, "There is an invitation to lecture",
                1),
            new MultiLingualContent(serbianTag,
                "Postoji poziv za predavanje",
                2)
        ));
        lectureInvitation.setDescription(Set.of(
            new MultiLingualContent(englishTag,
                "There is an invitation to lecture.",
                1),
            new MultiLingualContent(serbianTag,
                "Postoji poziv za predavanje.",
                2)
        ));
        lectureInvitation.setAccessLevel(AccessLevel.CLOSED);
        lectureInvitation.getApplicableTypes().add(ApplicableEntityType.DOCUMENT);
        lectureInvitation.setContentType(IndicatorContentType.BOOL);

        var isTheoretical = new Indicator();
        isTheoretical.setCode("isTheoretical");
        isTheoretical.setTitle(Set.of(
            new MultiLingualContent(englishTag, "Paper is thoretical",
                1),
            new MultiLingualContent(serbianTag,
                "Rad je teorijski",
                2)
        ));
        isTheoretical.setDescription(Set.of(
            new MultiLingualContent(englishTag,
                "A paper is theoretical when it focuses on developing models, concepts, or frameworks based on abstract reasoning or mathematics.",
                1),
            new MultiLingualContent(serbianTag,
                "Rad je teorijski kada se fokusira na razvoj modela, koncepata ili okvira zasnovanih na apstraktnom razmišljanju ili matematici.",
                2)
        ));
        isTheoretical.setAccessLevel(AccessLevel.CLOSED);
        isTheoretical.getApplicableTypes().add(ApplicableEntityType.DOCUMENT);
        isTheoretical.setContentType(IndicatorContentType.BOOL);

        var isExperimental = new Indicator();
        isExperimental.setCode("isExperimental");
        isExperimental.setTitle(Set.of(
            new MultiLingualContent(englishTag, "Paper is experimental",
                1),
            new MultiLingualContent(serbianTag,
                "Rad je eksperimentalni",
                2)
        ));
        isExperimental.setDescription(Set.of(
            new MultiLingualContent(englishTag,
                "A paper is experimental when it involves testing or observing phenomena through real-world experiments or practical setups.",
                1),
            new MultiLingualContent(serbianTag,
                "Rad je eksperimentalni kada uključuje testiranje ili posmatranje fenomena kroz eksperimente u stvarnom svetu ili praktične postavke.",
                2)
        ));
        isExperimental.setAccessLevel(AccessLevel.CLOSED);
        isExperimental.getApplicableTypes().add(ApplicableEntityType.DOCUMENT);
        isExperimental.setContentType(IndicatorContentType.BOOL);

        var isSimulation = new Indicator();
        isSimulation.setCode("isSimulation");
        isSimulation.setTitle(Set.of(
            new MultiLingualContent(englishTag, "Paper is a simulation or an analysis",
                1),
            new MultiLingualContent(serbianTag,
                "Rad je simulacija ili analiza",
                2)
        ));
        isSimulation.setDescription(Set.of(
            new MultiLingualContent(englishTag,
                "A paper falls into this category when it uses computational models to mimic real-world processes or systems, or when it examines data, results, or systems to derive insights or conclusions.",
                1),
            new MultiLingualContent(serbianTag,
                "Rad spada u ovu kategoriju kada koristi računarske modele za oponašanje realnih procesa ili sistema, ili kada ispituje podatke, rezultate ili sisteme kako bi se izvukli uvidi ili zaključci.",
                2)
        ));
        isSimulation.setAccessLevel(AccessLevel.CLOSED);
        isSimulation.getApplicableTypes().add(ApplicableEntityType.DOCUMENT);
        isSimulation.setContentType(IndicatorContentType.BOOL);

        var authorCount = new Indicator();
        authorCount.setCode("revisedAuthorCount");
        authorCount.setTitle(Set.of(
            new MultiLingualContent(englishTag, "Revised author count",
                1),
            new MultiLingualContent(serbianTag,
                "Revidiran broj autora",
                2)
        ));
        authorCount.setDescription(Set.of(
            new MultiLingualContent(englishTag,
                "When evaluating the results achieved in large international collaborations with 20 or more authors, where direct normalization would not provide a realistic assessment of the authors' contributions, the competent parent scientific committee verifies and grants full authorship on the published work of the collaboration only to a researcher from the list of authors who meet at least one of the criteria.",
                1),
            new MultiLingualContent(serbianTag,
                "Prilikom vrednovanja rezultata ostvarenih u velikim međunarodnim kolaboracijama, koji imaju 20 ili više autora i čije direktno normiranje ne bi dalo realnu procenu doprinosa autora, nadležni matični naučni odbor verifikuje i priznaje puno autorstvo na objavljenom radu kolaboracije samo istraživaču sa liste autora koji ispunjavaju najmanje jedan od uslova.",
                2)
        ));
        authorCount.setAccessLevel(AccessLevel.CLOSED);
        authorCount.getApplicableTypes().add(ApplicableEntityType.DOCUMENT);
        authorCount.setContentType(IndicatorContentType.NUMBER);

        indicatorRepository.saveAll(
            List.of(totalViews, yearlyViews, weeklyViews, monthlyViews, totalDownloads, fiveYearJIF,
                yearlyDownloads, weeklyDownloads, monthlyDownloads, numberOfPages, totalCitations,
                currentJIF, eigenFactorNorm, ais, citedHL, currentJIFRank, fiveYearJIFRank, sjr,
                hIndex, sdg, overton, citingHL, erihPlus, jci, jcr, scimago, jciPercentile,
                numParticipants, organizedByScientificInstitution, slavistiCategory,
                numCountriesInScientificCommittee, numParticipantCountries, numPresentations,
                lectureInvitation, isTheoretical, isExperimental, isSimulation, authorCount,
                yearlyCitations, totalOutputCount));
    }

    public void initializeAssessmentClassifications(LanguageTag englishTag,
                                                    LanguageTag serbianTag) {
        var journalM21APlus = new AssessmentClassification();
        journalM21APlus.setFormalDescriptionOfRule("handleM21APlus");
        journalM21APlus.setCode("journalM21APlus");
        journalM21APlus.setTitle(
            Set.of(
                new MultiLingualContent(englishTag, "Vodeći međunarodni časopis kategorije M21A+.",
                    1)));
        journalM21APlus.setApplicableTypes(Set.of(ApplicableEntityType.PUBLICATION_SERIES));

        var journalM21A = new AssessmentClassification();
        journalM21A.setFormalDescriptionOfRule("handleM21A");
        journalM21A.setCode("journalM21A");
        journalM21A.setTitle(
            Set.of(
                new MultiLingualContent(englishTag, "Vodeći međunarodni časopis kategorije M21A.",
                    1)));
        journalM21A.setApplicableTypes(Set.of(ApplicableEntityType.PUBLICATION_SERIES));

        var journalM21 = new AssessmentClassification();
        journalM21.setFormalDescriptionOfRule("handleM21");
        journalM21.setCode("journalM21");
        journalM21.setTitle(
            Set.of(
                new MultiLingualContent(englishTag, "Vodeći međunarodni časopis kategorije M21.",
                    1)));
        journalM21.setApplicableTypes(Set.of(ApplicableEntityType.PUBLICATION_SERIES));

        var journalM22 = new AssessmentClassification();
        journalM22.setFormalDescriptionOfRule("handleM22");
        journalM22.setCode("journalM22");
        journalM22.setTitle(
            Set.of(
                new MultiLingualContent(englishTag, "Međunarodni časopis kategorije M22.",
                    1)));
        journalM22.setApplicableTypes(Set.of(ApplicableEntityType.PUBLICATION_SERIES));

        var journalM23 = new AssessmentClassification();
        journalM23.setFormalDescriptionOfRule("handleM23");
        journalM23.setCode("journalM23");
        journalM23.setTitle(
            Set.of(
                new MultiLingualContent(englishTag, "Međunarodni časopis kategorije M23.",
                    1)));
        journalM23.setApplicableTypes(Set.of(ApplicableEntityType.PUBLICATION_SERIES));

        var journalM23e = new AssessmentClassification();
        journalM23e.setFormalDescriptionOfRule("handleM23e");
        journalM23e.setCode("journalM23e");
        journalM23e.setTitle(
            Set.of(
                new MultiLingualContent(englishTag, "Međunarodni časopis kategorije M23e.",
                    1)));
        journalM23e.setApplicableTypes(Set.of(ApplicableEntityType.PUBLICATION_SERIES));

        var journalM24plus = new AssessmentClassification();
        journalM24plus.setFormalDescriptionOfRule("handleM24plus");
        journalM24plus.setCode("journalM24Plus");
        journalM24plus.setTitle(
            Set.of(
                new MultiLingualContent(englishTag, "Međunarodni časopis kategorije M24+.",
                    1)));
        journalM24plus.setApplicableTypes(Set.of(ApplicableEntityType.PUBLICATION_SERIES));

        var journalM24 = new AssessmentClassification();
        journalM24.setFormalDescriptionOfRule("handleM24");
        journalM24.setCode("journalM24");
        journalM24.setTitle(
            Set.of(
                new MultiLingualContent(englishTag,
                    "Vodeći nacionalni časopis kategorije M24.",
                    1)));
        journalM24.setApplicableTypes(Set.of(ApplicableEntityType.PUBLICATION_SERIES));

        var journalM51 = new AssessmentClassification();
        journalM51.setFormalDescriptionOfRule("handleM51");
        journalM51.setCode("journalM51");
        journalM51.setTitle(
            Set.of(
                new MultiLingualContent(englishTag, "Vodeći nacionalni časopis kategorije M51.",
                    1)));
        journalM51.setApplicableTypes(Set.of(ApplicableEntityType.PUBLICATION_SERIES));

        var journalM52 = new AssessmentClassification();
        journalM52.setFormalDescriptionOfRule("handleM52");
        journalM52.setCode("journalM52");
        journalM52.setTitle(
            Set.of(
                new MultiLingualContent(englishTag, "Nacionalni časopis kategorije M52.",
                    1)));
        journalM52.setApplicableTypes(Set.of(ApplicableEntityType.PUBLICATION_SERIES));

        var journalM53 = new AssessmentClassification();
        journalM53.setFormalDescriptionOfRule("handleM53");
        journalM53.setCode("journalM53");
        journalM53.setTitle(
            Set.of(
                new MultiLingualContent(englishTag, "Nacionalni časopis kategorije M53.",
                    1)));
        journalM53.setApplicableTypes(Set.of(ApplicableEntityType.PUBLICATION_SERIES));

        var journalM54 = new AssessmentClassification();
        journalM54.setFormalDescriptionOfRule("handleM54");
        journalM54.setCode("journalM54");
        journalM54.setTitle(
            Set.of(
                new MultiLingualContent(englishTag,
                    "Nacionalni naučni časopis koji se prvi put kategoriše.",
                    1)));
        journalM54.setApplicableTypes(Set.of(ApplicableEntityType.PUBLICATION_SERIES));

        var multinationalConf = new AssessmentClassification();
        multinationalConf.setFormalDescriptionOfRule("multinationalConference");
        multinationalConf.setCode("multinationalConf");
        multinationalConf.setTitle(
            Set.of(
                new MultiLingualContent(serbianTag, "Međunarodna konferencija.", 2),
                new MultiLingualContent(englishTag, "Multinational conference.", 1)));
        multinationalConf.setApplicableTypes(Set.of(ApplicableEntityType.EVENT));

        var nationalConf = new AssessmentClassification();
        nationalConf.setFormalDescriptionOfRule("nationalConference");
        nationalConf.setCode("nationalConf");
        nationalConf.setTitle(
            Set.of(
                new MultiLingualContent(serbianTag, "Domaća konferencija.", 2),
                new MultiLingualContent(englishTag, "National conference.", 1)));
        nationalConf.setApplicableTypes(Set.of(ApplicableEntityType.EVENT));

        var nonAcademicConf = new AssessmentClassification();
        nonAcademicConf.setFormalDescriptionOfRule("nonAcademicConference");
        nonAcademicConf.setCode("nonAcademicConf");
        nonAcademicConf.setTitle(
            Set.of(
                new MultiLingualContent(serbianTag, "Tehnička (ne-naučna) konferencija.", 2),
                new MultiLingualContent(englishTag, "Technical (non-academic) conference.", 1)));
        nonAcademicConf.setApplicableTypes(Set.of(ApplicableEntityType.EVENT));

        var M21APlus = new AssessmentClassification();
        M21APlus.setFormalDescriptionOfRule("handleM21APlus");
        M21APlus.setCode("M21APlus");
        M21APlus.setTitle(
            Set.of(
                new MultiLingualContent(englishTag, "M21a+",
                    1)));
        M21APlus.setApplicableTypes(Set.of(ApplicableEntityType.DOCUMENT));

        var M21A = new AssessmentClassification();
        M21A.setFormalDescriptionOfRule("handleM21A");
        M21A.setCode("M21A");
        M21A.setTitle(
            Set.of(
                new MultiLingualContent(englishTag, "M21a",
                    1)));
        M21A.setApplicableTypes(Set.of(ApplicableEntityType.DOCUMENT));

        var M21 = new AssessmentClassification();
        M21.setFormalDescriptionOfRule("handleM21");
        M21.setCode("M21");
        M21.setTitle(
            Set.of(
                new MultiLingualContent(englishTag, "M21",
                    1)));
        M21.setApplicableTypes(Set.of(ApplicableEntityType.DOCUMENT));

        var M22 = new AssessmentClassification();
        M22.setFormalDescriptionOfRule("handleM22");
        M22.setCode("M22");
        M22.setTitle(
            Set.of(
                new MultiLingualContent(englishTag, "M22",
                    1)));
        M22.setApplicableTypes(Set.of(ApplicableEntityType.DOCUMENT));

        var M23 = new AssessmentClassification();
        M23.setFormalDescriptionOfRule("handleM23");
        M23.setCode("M23");
        M23.setTitle(
            Set.of(
                new MultiLingualContent(englishTag, "M23",
                    1)));
        M23.setApplicableTypes(Set.of(ApplicableEntityType.DOCUMENT));

        var M23e = new AssessmentClassification();
        M23e.setFormalDescriptionOfRule("handleM23e");
        M23e.setCode("M23e");
        M23e.setTitle(
            Set.of(
                new MultiLingualContent(englishTag, "M23e",
                    1)));
        M23e.setApplicableTypes(Set.of(ApplicableEntityType.DOCUMENT));

        var M24plus = new AssessmentClassification();
        M24plus.setFormalDescriptionOfRule("handleM24plus");
        M24plus.setCode("M24Plus");
        M24plus.setTitle(
            Set.of(
                new MultiLingualContent(englishTag, "M24+",
                    1)));
        M24plus.setApplicableTypes(Set.of(ApplicableEntityType.DOCUMENT));

        var M24 = new AssessmentClassification();
        M24.setFormalDescriptionOfRule("handleM24");
        M24.setCode("M24");
        M24.setTitle(
            Set.of(
                new MultiLingualContent(englishTag,
                    "M24",
                    1)));
        M24.setApplicableTypes(Set.of(ApplicableEntityType.DOCUMENT));

        var M26 = new AssessmentClassification();
        M26.setFormalDescriptionOfRule("handleM26");
        M26.setCode("M26");
        M26.setTitle(
            Set.of(
                new MultiLingualContent(englishTag,
                    "M26",
                    1)));
        M26.setApplicableTypes(Set.of(ApplicableEntityType.DOCUMENT));

        var M27 = new AssessmentClassification();
        M27.setFormalDescriptionOfRule("handleM27");
        M27.setCode("M27");
        M27.setTitle(
            Set.of(
                new MultiLingualContent(englishTag,
                    "M27",
                    1)));
        M27.setApplicableTypes(Set.of(ApplicableEntityType.DOCUMENT));

        var M51 = new AssessmentClassification();
        M51.setFormalDescriptionOfRule("handleM51");
        M51.setCode("M51");
        M51.setTitle(
            Set.of(
                new MultiLingualContent(englishTag, "M51",
                    1)));
        M51.setApplicableTypes(Set.of(ApplicableEntityType.DOCUMENT));

        var M52 = new AssessmentClassification();
        M52.setFormalDescriptionOfRule("handleM52");
        M52.setCode("M52");
        M52.setTitle(
            Set.of(
                new MultiLingualContent(englishTag, "M52",
                    1)));
        M52.setApplicableTypes(Set.of(ApplicableEntityType.DOCUMENT));

        var M53 = new AssessmentClassification();
        M53.setFormalDescriptionOfRule("handleM53");
        M53.setCode("M53");
        M53.setTitle(
            Set.of(
                new MultiLingualContent(englishTag, "M53",
                    1)));
        M53.setApplicableTypes(Set.of(ApplicableEntityType.DOCUMENT));

        var M54 = new AssessmentClassification();
        M54.setFormalDescriptionOfRule("handleM54");
        M54.setCode("M54");
        M54.setTitle(
            Set.of(
                new MultiLingualContent(englishTag,
                    "M54",
                    1)));
        M54.setApplicableTypes(Set.of(ApplicableEntityType.DOCUMENT));

        var m31 = new AssessmentClassification();
        m31.setFormalDescriptionOfRule("handleM31");
        m31.setCode("M31");
        m31.setTitle(
            Set.of(
                new MultiLingualContent(englishTag,
                    "M31",
                    1)));
        m31.setApplicableTypes(Set.of(ApplicableEntityType.DOCUMENT));

        var m32 = new AssessmentClassification();
        m32.setFormalDescriptionOfRule("handleM31");
        m32.setCode("M32");
        m32.setTitle(
            Set.of(
                new MultiLingualContent(englishTag,
                    "M32",
                    1)));
        m32.setApplicableTypes(Set.of(ApplicableEntityType.DOCUMENT));

        var m33 = new AssessmentClassification();
        m33.setFormalDescriptionOfRule("handleM31");
        m33.setCode("M33");
        m33.setTitle(
            Set.of(
                new MultiLingualContent(englishTag,
                    "M33",
                    1)));
        m33.setApplicableTypes(Set.of(ApplicableEntityType.DOCUMENT));

        var m34 = new AssessmentClassification();
        m34.setFormalDescriptionOfRule("handleM31");
        m34.setCode("M34");
        m34.setTitle(
            Set.of(
                new MultiLingualContent(englishTag,
                    "M34",
                    1)));
        m34.setApplicableTypes(Set.of(ApplicableEntityType.DOCUMENT));

        var m61 = new AssessmentClassification();
        m61.setFormalDescriptionOfRule("handleM61");
        m61.setCode("M61");
        m61.setTitle(
            Set.of(
                new MultiLingualContent(englishTag,
                    "M61",
                    1)));
        m61.setApplicableTypes(Set.of(ApplicableEntityType.DOCUMENT));

        var m62 = new AssessmentClassification();
        m62.setFormalDescriptionOfRule("handleM62");
        m62.setCode("M62");
        m62.setTitle(
            Set.of(
                new MultiLingualContent(englishTag,
                    "M62",
                    1)));
        m62.setApplicableTypes(Set.of(ApplicableEntityType.DOCUMENT));

        var m63 = new AssessmentClassification();
        m63.setFormalDescriptionOfRule("handleM63");
        m63.setCode("M63");
        m63.setTitle(
            Set.of(
                new MultiLingualContent(englishTag,
                    "M63",
                    1)));
        m63.setApplicableTypes(Set.of(ApplicableEntityType.DOCUMENT));

        var m64 = new AssessmentClassification();
        m64.setFormalDescriptionOfRule("handleM64");
        m64.setCode("M64");
        m64.setTitle(
            Set.of(
                new MultiLingualContent(englishTag,
                    "M64",
                    1)));
        m64.setApplicableTypes(Set.of(ApplicableEntityType.DOCUMENT));

        var m69 = new AssessmentClassification();
        m69.setFormalDescriptionOfRule("handleM69");
        m69.setCode("M69");
        m69.setTitle(
            Set.of(
                new MultiLingualContent(englishTag,
                    "M69",
                    1)));
        m69.setApplicableTypes(Set.of(ApplicableEntityType.DOCUMENT));

        assessmentClassificationRepository.saveAll(
            List.of(journalM21APlus, journalM21A, journalM21, journalM22, journalM23, journalM23e,
                journalM24plus, multinationalConf, nationalConf,
                nonAcademicConf, journalM51, journalM52, journalM53, journalM54, journalM24,
                M21APlus, M21A, M21, M22,
                M23, M23e, M24plus, M24, M51, M52, M53, M54, M26, M27,
                m31, m32, m33,
                m34, m61, m62, m63, m64, m69));
    }

    public Pair<Commission, Commission> initializeCommissions(LanguageTag englishTag,
                                                              LanguageTag serbianTag,
                                                              LanguageTag serbianCyrillicTag) {
        var commission1 = new Commission();
        commission1.setDescription(Set.of(new MultiLingualContent(englishTag, "Web Of Science", 1),
            new MultiLingualContent(serbianTag, "Web Of Science", 2)));
        commission1.setFormalDescriptionOfRule("WOSJournalClassificationRuleEngine");

        var commission2 = new Commission();
        commission2.setDescription(Set.of(new MultiLingualContent(englishTag, "SciMAGO", 1),
            new MultiLingualContent(serbianTag, "SciMAGO", 2)));
        commission2.setFormalDescriptionOfRule("ScimagoJournalClassificationRuleEngine");
        commission2.setAssessmentDateFrom(LocalDate.of(2022, 2, 4));
        commission2.setAssessmentDateTo(LocalDate.of(2022, 5, 4));

        var commission3 = new Commission();
        commission3.setDescription(Set.of(new MultiLingualContent(englishTag, "Erih PLUS", 1),
            new MultiLingualContent(serbianTag, "Erih PLUS", 2)));
        commission3.setFormalDescriptionOfRule("ErihPlusJournalClassificationRuleEngine");

        var commission4 = new Commission();
        commission4.setDescription(Set.of(new MultiLingualContent(englishTag, "MKS Slavists", 1),
            new MultiLingualContent(serbianTag, "MKS Slavisti", 2)));
        commission4.setFormalDescriptionOfRule("MKSJournalClassificationRuleEngine");

        var commission5 = new Commission();
        commission5.setDescription(
            Set.of(new MultiLingualContent(englishTag, "MNO ALL", 1),
                new MultiLingualContent(serbianTag, "MNO SVE", 2),
                new MultiLingualContent(serbianCyrillicTag, "МНО СВЕ", 3)));
        commission5.setFormalDescriptionOfRule("load-mno");
        commission5.setRecognisedResearchAreas(
            Set.of("NATURAL", "SOCIAL", "TECHNICAL", "HUMANITIES"));
        commission5.setIsDefault(true);

        var commission6 = new Commission();
        commission6.setDescription(
            Set.of(new MultiLingualContent(englishTag, "MNO Physics & Chemistry", 1),
                new MultiLingualContent(serbianTag, "MNO Fizika i Hemija", 2),
                new MultiLingualContent(serbianCyrillicTag, "МНО Физика и Хемија", 3)));
        commission6.setFormalDescriptionOfRule("load-mnoPhysChem");

        var commission7 = new Commission();
        commission7.setDescription(
            Set.of(new MultiLingualContent(englishTag, "DMI-PMF", 1),
                new MultiLingualContent(serbianTag, "DMI-PMF", 2),
                new MultiLingualContent(serbianCyrillicTag, "ДМИ-ПМФ", 3)));
        commission7.setFormalDescriptionOfRule("load-mno");

        var commission8 = new Commission();
        commission8.setDescription(
            Set.of(new MultiLingualContent(englishTag, "DH-PMF", 1),
                new MultiLingualContent(serbianTag, "DH-PMF", 2),
                new MultiLingualContent(serbianCyrillicTag, "ДХ-ПМФ", 3)));
        commission8.setFormalDescriptionOfRule("load-mno");

        var commission9 = new Commission();
        commission9.setDescription(
            Set.of(new MultiLingualContent(englishTag, "DGTH-NAT-PMF", 1),
                new MultiLingualContent(serbianTag, "DGTH-NAT-PMF", 2),
                new MultiLingualContent(serbianCyrillicTag, "ДГТХ-НАТ-ПМФ", 3)));
        commission9.setFormalDescriptionOfRule("load-mno");

        var commission10 = new Commission();
        commission10.setDescription(
            Set.of(new MultiLingualContent(englishTag, "DF-PMF", 1),
                new MultiLingualContent(serbianTag, "DF-PMF", 2),
                new MultiLingualContent(serbianCyrillicTag, "ДФ-ПМФ", 3)));
        commission10.setFormalDescriptionOfRule("load-mno");

        var commission11 = new Commission();
        commission11.setDescription(
            Set.of(new MultiLingualContent(englishTag, "DBE-PMF", 1),
                new MultiLingualContent(serbianTag, "DBE-PMF", 2),
                new MultiLingualContent(serbianCyrillicTag, "ДБЕ-ПМФ", 3)));
        commission11.setFormalDescriptionOfRule("load-mno");

        var commission12 = new Commission();
        commission12.setDescription(
            Set.of(new MultiLingualContent(englishTag, "DMI-SOC-PMF", 1),
                new MultiLingualContent(serbianTag, "DMI-SOC-PMF", 2),
                new MultiLingualContent(serbianCyrillicTag, "ДМИ-СОЦ-ПМФ", 3)));
        commission12.setFormalDescriptionOfRule("load-mno");

        var commission13 = new Commission();
        commission13.setDescription(
            Set.of(new MultiLingualContent(englishTag, "DH-SOC-PMF", 1),
                new MultiLingualContent(serbianTag, "DH-SOC-PMF", 2),
                new MultiLingualContent(serbianCyrillicTag, "ДХ-СОЦ-ПМФ", 3)));
        commission13.setFormalDescriptionOfRule("load-mno");

        var commission14 = new Commission();
        commission14.setDescription(
            Set.of(new MultiLingualContent(englishTag, "DGTH-SOC-PMF", 1),
                new MultiLingualContent(serbianTag, "DGTH-SOC-PMF", 2),
                new MultiLingualContent(serbianCyrillicTag, "ДГТХ-СОЦ-ПМФ", 3)));
        commission14.setFormalDescriptionOfRule("load-mno");

        var commission15 = new Commission();
        commission15.setDescription(
            Set.of(new MultiLingualContent(englishTag, "DF-SOC-PMF", 1),
                new MultiLingualContent(serbianTag, "DF-SOC-PMF", 2),
                new MultiLingualContent(serbianCyrillicTag, "ДФ-СОЦ-ПМФ", 3)));
        commission15.setFormalDescriptionOfRule("load-mno");

        var commission16 = new Commission();
        commission16.setDescription(
            Set.of(new MultiLingualContent(englishTag, "DBE-SOC-PMF", 1),
                new MultiLingualContent(serbianTag, "DBE-SOC-PMF", 2),
                new MultiLingualContent(serbianCyrillicTag, "ДБЕ-СОЦ-ПМФ", 3)));
        commission16.setFormalDescriptionOfRule("load-mno");

        commissionRepository.saveAll(
            List.of(commission1, commission2, commission3, commission4, commission5, commission6,
                commission7, commission8, commission9, commission10, commission11, commission12,
                commission13, commission14, commission15, commission16));

        var commissionRelation1 = new CommissionRelation();
        commissionRelation1.setSourceCommission(commission5);
        commissionRelation1.setTargetCommissions(new HashSet<>(List.of(commission2)));
        commissionRelation1.setPriority(2);
        commissionRelation1.setResultCalculationMethod(ResultCalculationMethod.BEST_VALUE);

        var commissionRelation2 = new CommissionRelation();
        commissionRelation2.setSourceCommission(commission5);
        commissionRelation2.setTargetCommissions(new HashSet<>(List.of(commission1, commission4)));
        commissionRelation2.setPriority(1);
        commissionRelation2.setResultCalculationMethod(ResultCalculationMethod.BEST_VALUE);

        commissionRelationRepository.saveAll(List.of(commissionRelation1, commissionRelation2));

        return new Pair<>(commission5, commission6);
    }

    public void initializeRulebooks(LanguageTag englishTag, LanguageTag serbianTag) {
        var serbianRulebook = new AssessmentRulebook();
        serbianRulebook.setName(
            Set.of(new MultiLingualContent(englishTag, "Serbian Rulebook", 1),
                new MultiLingualContent(serbianTag, "Srpski Pravilnik", 2)));
        serbianRulebook.setDescription(
            Set.of(new MultiLingualContent(englishTag, "Description", 1),
                new MultiLingualContent(serbianTag, "Opis", 2)));
        serbianRulebook.setIssueDate(LocalDate.of(2025, 1, 15));
        serbianRulebook.setIsDefault(true);

        var measureM10 = new AssessmentMeasure();
        measureM10.setTitle(Set.of(new MultiLingualContent(englishTag,
                "Monographs, monographic studies, thematic collections, lexicographic and cartographic publications of international importance",
                1),
            new MultiLingualContent(serbianTag,
                "Monografije, monografske studije, tematski zbornici, leksikografske i kartografske publikacije međunarodnog značaja",
                2)));
        measureM10.setCode("M10");
        measureM10.setPointRule("serbianPointsRulebook2025");
        measureM10.setScalingRule("serbianScalingRulebook2025");
        serbianRulebook.addAssessmentMeasure(measureM10);

        var measureM20 = new AssessmentMeasure();
        measureM20.setTitle(Set.of(new MultiLingualContent(englishTag,
                "Papers published in scientific journals of international importance", 1),
            new MultiLingualContent(serbianTag,
                "Radovi objavljeni u naučnim časopisima međunarodnog značaja", 2)));
        measureM20.setCode("M20");
        measureM20.setPointRule("serbianPointsRulebook2025");
        measureM20.setScalingRule("serbianScalingRulebook2025");
        serbianRulebook.addAssessmentMeasure(measureM20);

        var measureM30 = new AssessmentMeasure();
        measureM30.setTitle(Set.of(new MultiLingualContent(englishTag,
                "Proceedings of international scientific conferences", 1),
            new MultiLingualContent(serbianTag, "Zbornici međunarodnih naučnih skupova", 2)));
        measureM30.setCode("M30");
        measureM30.setPointRule("serbianPointsRulebook2025");
        measureM30.setScalingRule("serbianScalingRulebook2025");
        serbianRulebook.addAssessmentMeasure(measureM30);

        var measureM40 = new AssessmentMeasure();
        measureM40.setTitle(
            Set.of(new MultiLingualContent(englishTag, "Monographs of national importance", 1),
                new MultiLingualContent(serbianTag, "Monografije nacionalnog značaja", 2)));
        measureM40.setCode("M40");
        measureM40.setPointRule("serbianPointsRulebook2025");
        measureM40.setScalingRule("serbianScalingRulebook2025");
        serbianRulebook.addAssessmentMeasure(measureM40);

        var measureM50 = new AssessmentMeasure();
        measureM50.setTitle(Set.of(
            new MultiLingualContent(englishTag, "Papers in journals of national importance", 1),
            new MultiLingualContent(serbianTag, "Radovi u časopisima nacionalnog značaja", 2)));
        measureM50.setCode("M50");
        measureM50.setPointRule("serbianPointsRulebook2025");
        measureM50.setScalingRule("serbianScalingRulebook2025");
        serbianRulebook.addAssessmentMeasure(measureM50);

        var measureM60 = new AssessmentMeasure();
        measureM60.setTitle(Set.of(new MultiLingualContent(englishTag,
                "Proceedings of national scientific conferences, critical editing of sources", 1),
            new MultiLingualContent(serbianTag,
                "Zbornici nacionalnih naučnih skupova, kritičko priređivanje izvora", 2)));
        measureM60.setCode("M60");
        measureM60.setPointRule("serbianPointsRulebook2025");
        measureM60.setScalingRule("serbianScalingRulebook2025");
        serbianRulebook.addAssessmentMeasure(measureM60);

        var measureM70 = new AssessmentMeasure();
        measureM70.setTitle(
            Set.of(new MultiLingualContent(englishTag, "Defended doctoral dissertation", 1),
                new MultiLingualContent(serbianTag, "Odbranjena doktorska disertacija", 2)));
        measureM70.setCode("M70");
        measureM70.setPointRule("serbianPointsRulebook2025");
        measureM70.setScalingRule("serbianScalingRulebook2025");
        serbianRulebook.addAssessmentMeasure(measureM70);

        var measureM80 = new AssessmentMeasure();
        measureM80.setTitle(Set.of(new MultiLingualContent(englishTag, "Technical solutions", 1),
            new MultiLingualContent(serbianTag, "Tehnička rešenja", 2)));
        measureM80.setCode("M80");
        measureM80.setPointRule("serbianPointsRulebook2025");
        measureM80.setScalingRule("serbianScalingRulebook2025");
        serbianRulebook.addAssessmentMeasure(measureM80);

        var measureM90 = new AssessmentMeasure();
        measureM90.setTitle(
            Set.of(new MultiLingualContent(englishTag, "Patents, varieties, breeds, or strains", 1),
                new MultiLingualContent(serbianTag, "Patenti, sorte, rase ili sojevi", 2)));
        measureM90.setCode("M90");
        measureM90.setPointRule("serbianPointsRulebook2025");
        measureM90.setScalingRule("serbianScalingRulebook2025");
        serbianRulebook.addAssessmentMeasure(measureM90);

        assessmentMeasureRepository.saveAll(
            List.of(measureM10, measureM20, measureM30, measureM40, measureM50, measureM60,
                measureM70, measureM80, measureM90));
        assessmentRulebookRepository.save(serbianRulebook);
    }
}
