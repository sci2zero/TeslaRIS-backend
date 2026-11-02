package rs.teslaris.dbinitialization;


import java.util.Arrays;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import rs.teslaris.core.indexmodel.EntityType;
import rs.teslaris.core.model.user.User;
import rs.teslaris.core.model.user.UserNotificationPeriod;
import rs.teslaris.core.repository.user.UserRepository;
import rs.teslaris.core.service.interfaces.commontypes.ReindexService;
import rs.teslaris.revisioner.model.QualityAssessmentTarget;
import rs.teslaris.revisioner.service.interfaces.QualityAssessmentBackfillService;

/**
 * Orchestrates database seeding. Every step below is a single call, so a step that is not needed
 * for local testing can simply be commented out.
 * <p>
 * The integration testing dataset is the one exception: it is gated on the
 * {@value #INTEGRATION_TEST_PROFILE} profile rather than being commented out, because the
 * integration tests depend on it. That profile is active by default, so to bring up a lean local
 * database run with {@code SPRING_PROFILE=test} and everything but the testing data is seeded.
 */
@Component
@RequiredArgsConstructor
@Profile("test")
public class DbInitializer implements ApplicationRunner {

    private static final String INTEGRATION_TEST_PROFILE = "integration-test";

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    private final Environment environment;

    private final AuthorityDataInitializer authorityDataInitializer;

    private final LanguageDataInitializer languageDataInitializer;

    private final ResearchAreaDataInitializer researchAreaDataInitializer;

    private final CountryDataInitializer countryDataInitializer;

    private final CurrencyDataInitializer currencyDataInitializer;

    private final BrandingDataInitializer brandingDataInitializer;

    private final AssessmentDataInitializer assessmentDataInitializer;

    private final TestingDataInitializer testingDataInitializer;

    private final ReindexService reindexService;

    private final QualityAssessmentBackfillService qualityAssessmentBackfillService;


    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        ///////////////////// NECESSARY DATA /////////////////////

        // PRIVILEGES AND AUTHORITIES
        var authorities = authorityDataInitializer.initializeAuthorities();

        // LANGUAGE TAGS AND LANGUAGES
        var languages = languageDataInitializer.initializeLanguages();

        // ADMIN USER
        initializeAdminUser(languages, authorities);

        // RESEARCH AREAS
        var researchAreaLeaf =
            researchAreaDataInitializer.initializeResearchAreas(languages.serbianTag());

        ///////////////////// OPTIONAL DATA /////////////////////

        // DEFAULT BRANDING
        brandingDataInitializer.initializeDefaultBranding(languages.serbianTag(),
            languages.englishTag());

        // COUNTRIES
        countryDataInitializer.initializeCountries(languages.serbianTag(),
            languages.englishTag());

        // CURRENCIES
        currencyDataInitializer.initializeCurrencies(languages.englishTag());

        ///////////////////// ASSESSMENT DATA /////////////////////
        assessmentDataInitializer.initializeIndicators(languages.englishTag(),
            languages.serbianTag(), languages.serbianCyrillicTag());
        assessmentDataInitializer.initializeAssessmentClassifications(languages.englishTag(),
            languages.serbianTag(), languages.serbianCyrillicTag());
        var commissions = assessmentDataInitializer.initializeCommissions(languages.englishTag(),
            languages.serbianTag(), languages.serbianCyrillicTag());
        assessmentDataInitializer.initializeRulebooks(languages.englishTag(),
            languages.serbianTag());

        ///////////////////// TESTING DATA /////////////////////
        if (environment.acceptsProfiles(Profiles.of(INTEGRATION_TEST_PROFILE))) {
            testingDataInitializer.initializeIntegrationTestingData(languages.serbianTag(),
                languages.serbianLanguage(), languages.englishTag(), languages.germanTag(),
                researchAreaLeaf, authorities.researcher(), authorities.commission(),
                authorities.viceDeanForScience(), authorities.institutionalEditor(),
                authorities.institutionalLibrarian(), authorities.headOfLibrary(),
                authorities.promotionRegistryAdministrator(), commissions.a, commissions.b,
                true);
        }

        ///////////////////// POST-COMMIT WORK /////////////////////
        registerPostCommitWork();
    }

    private void initializeAdminUser(LanguageDataInitializer.SeededLanguages languages,
                                     AuthorityDataInitializer.SeededAuthorities authorities) {
        var adminUser =
            new User("admin@admin.com", passwordEncoder.encode("admin"), "note", "Marko",
                "Markovic", false, false, languages.serbianTag(), languages.englishTag(),
                authorities.admin(), null,
                null, null,
                UserNotificationPeriod.DAILY, true, null);
        userRepository.save(adminUser);
    }

    /**
     * Work that can only run once the seeding transaction has committed.
     */
    private void registerPostCommitWork() {
        TransactionSynchronizationManager
            .registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
//                    researchAreaDataInitializer.loadResearchAreaTaxonomy();
                    reindexService
                        .reindexDatabase(Arrays.asList(EntityType.values()),
                            false, null);

                    // Every entity gets a revision and a first assessment, so the data quality
                    // views have something to show right after a fresh start.
                    qualityAssessmentBackfillService.performBackfill(
                        Arrays.asList(QualityAssessmentTarget.values()),
                        null, null,
                        "ptcris", false
                    );
                }
            });
    }
}
