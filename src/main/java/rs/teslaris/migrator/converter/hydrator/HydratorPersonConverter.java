package rs.teslaris.migrator.converter.hydrator;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import rs.teslaris.core.dto.person.ImportPersonDTO;
import rs.teslaris.core.dto.person.PersonNameDTO;
import rs.teslaris.core.model.person.PersonNameType;
import rs.teslaris.core.model.person.Sex;
import rs.teslaris.migrator.model.hydrator.HydratorCVModel;
import rs.teslaris.migrator.pipeline.RecordExtractor;
import rs.teslaris.migrator.util.MigrationEntityType;
import rs.teslaris.migrator.util.MigrationLog;

/**
 * One curriculum yields exactly one person, so this is a 1:1 converter wrapped with
 * {@link RecordExtractor#of}.
 * <p>
 * Applies the CIÊNCIA VITAE person crosswalk ({@code CW-001-MAP-000019…037}). Loss policy:
 * <ul>
 *     <li>a source value with no target in the crosswalk is logged as dropped;</li>
 *     <li>several source values for one target slot are ranked by the PTCRIS scalar precedence
 *     (authoritative source, valid over invalid, more complete, most recently updated), the rest
 *     are logged as dropped;</li>
 *     <li>an identifier with no valid value of its type is passed on unchanged, so the import
 *     rejects it and the whole person fails, to be retried once the source is fixed.</li>
 * </ul>
 * Relations (degrees, employments, distinctions, fundings) are migrated separately.
 */
@Component
@RequiredArgsConstructor
public class HydratorPersonConverter
    implements RecordExtractor.RecordConverter<HydratorCVModel.Curriculum, ImportPersonDTO> {

    private static final String PUBLIC_PRIVACY_LEVEL = "publico";

    private static final String PERSONAL_CODE = "R";

    private static final String MOBILE_PHONE_CODE = "2";

    private static final String FAX_CODE = "3";

    private static final String PREFERRED = "true";

    private static final Map<String, Sex> SEX_BY_CODE = Map.of("F", Sex.FEMALE, "M", Sex.MALE);

    private static final String CIENCIA_ID_CODE = "CIENCIAID";

    private static final String ORCID_CODE = "ORCID";

    private static final String SCOPUS_CODE = "SCOPUS";

    private static final String GOOGLE_SCHOLAR_CODE = "GOOGLE";

    private static final String WEB_OF_SCIENCE_CODE = "WOS";

    // Same formats PersonServiceImpl enforces, used to prefer a valid value over an invalid one
    private static final Pattern ORCID_PATTERN =
        Pattern.compile("^\\d{4}-\\d{4}-\\d{4}-[\\dX]{4}$");

    private static final Pattern SCOPUS_PATTERN = Pattern.compile("^\\d+$");

    private static final Pattern SCHOLAR_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]{12,}$");

    private static final Pattern WEB_OF_SCIENCE_PATTERN =
        Pattern.compile("^[A-Z]{1,3}-\\d{4}-\\d{4}$");

    private static final Pattern SCHOLAR_USER_PARAMETER = Pattern.compile("[?&]user=([^&#]+)");

    private static final Pattern URL_SCHEME = Pattern.compile("^[a-zA-Z][a-zA-Z0-9+.-]*://.*");

    private static final Pattern DIGIT = Pattern.compile("\\d");

    // ISO-8601 timestamps sort lexicographically; a missing one ranks last
    private static final Comparator<String> MOST_RECENT_FIRST =
        Comparator.nullsLast(Comparator.reverseOrder());

    private static final Comparator<HydratorCVModel.Email> EMAIL_PRECEDENCE =
        Comparator.<HydratorCVModel.Email, Boolean>comparing(
                email -> !PREFERRED.equals(email.preferredEmail()))
            .thenComparing(email -> !email.emailAddress().contains("@"))
            .thenComparing(HydratorCVModel.Email::lastModifiedDate, MOST_RECENT_FIRST);

    private static final Comparator<HydratorCVModel.PhoneNumber> PHONE_NUMBER_PRECEDENCE =
        Comparator.<HydratorCVModel.PhoneNumber, Boolean>comparing(
                phoneNumber -> !PREFERRED.equals(phoneNumber.preferredPhoneNumber()))
            .thenComparing(phoneNumber -> !DIGIT.matcher(phoneNumber.localNumber()).find())
            .thenComparing(phoneNumber -> isBlankValue(phoneNumber.countryCode()))
            .thenComparing(HydratorCVModel.PhoneNumber::lastModifiedDate, MOST_RECENT_FIRST);

    private static final Comparator<HydratorCVModel.MailingAddress> MAILING_ADDRESS_PRECEDENCE =
        Comparator.<HydratorCVModel.MailingAddress, Boolean>comparing(
                address -> !PREFERRED.equals(address.preferredMailingAddress()))
            .thenComparing(HydratorPersonConverter::completeness, Comparator.reverseOrder())
            .thenComparing(HydratorCVModel.MailingAddress::lastModifiedDate, MOST_RECENT_FIRST);

    private final HydratorConversionUtil conversionUtil;

    private final MigrationLog migrationLog;


    @Override
    public ImportPersonDTO toDTO(HydratorCVModel.Curriculum record) {
        var info = identifyingInfoOf(record);

        if (Objects.isNull(info) || Objects.isNull(info.personInfo())) {
            return null;
        }

        var personInfo = info.personInfo();
        var personName = personName(personInfo, record.fullName());

        if (Objects.isNull(personName)) {
            return null;
        }

        var language = record.curriculum().language();
        var dto = new ImportPersonDTO();
        dto.setImportSource(HydratorSource.IMPORT_SOURCE);

        // MAP-000023…028
        dto.setPersonName(personName);
        dto.setOtherNames(otherNames(personInfo, info.citationNames(), personName));

        // MAP-000020…022
        dto.setLocalBirthDate(birthDate(personInfo.dateOfBirth()));
        dto.setSex(sex(record, personInfo.gender()));
        logPhotoDropped(record, personInfo.photography());

        // MAP-000029, MAP-000036
        dto.setBiography(conversionUtil.multilingualContent(
            Objects.isNull(info.resume()) ? null : info.resume().text(), language));
        dto.setKeywords(conversionUtil.multilingualContent(keywords(info), language));

        // MAP-000030…033
        setEmails(record, dto, info.emails());
        setPhoneNumbers(record, dto, info.phoneNumbers());
        setMailingAddresses(record, dto, info.mailingAddresses(), language);
        dto.setUris(uris(record, info.webAddresses()));

        // MAP-000034, MAP-000035
        logLanguageCompetenciesDropped(record, info.languageCompetencies());
        logResearchClassificationsDropped(record, info.domainActivities());

        // MAP-000037
        setIdentifiers(record, dto);

        return dto;
    }

    /**
     * MAP-000024 + MAP-000025 build one instance: {@code names} and {@code surnames} are the
     * default name. Falls back to splitting the full name when either half is missing.
     */
    private PersonNameDTO personName(HydratorCVModel.PersonInfo personInfo,
                                     String fullNameFallback) {
        if (!isBlank(personInfo.names()) && !isBlank(personInfo.surnames())) {
            return name(personInfo.names().trim(), personInfo.surnames().trim(), null);
        }

        var source = isBlank(personInfo.fullName()) ? fullNameFallback : personInfo.fullName();

        return splitName(source, null);
    }

    private List<PersonNameDTO> otherNames(HydratorCVModel.PersonInfo personInfo,
                                           HydratorCVModel.CitationNames citationNames,
                                           PersonNameDTO personName) {
        var candidates = new ArrayList<PersonNameDTO>();
        candidates.add(splitName(personInfo.fullName(), PersonNameType.FULL_NAME));
        candidates.add(splitName(personInfo.displayName(), PersonNameType.DISPLAY_NAME));

        listOf(citationNames, HydratorCVModel.CitationNames::citationName).stream()
            .filter(citationName -> isPublic(citationName.privacyLevel()))
            .sorted(Comparator.comparing(
                citationName -> !PREFERRED.equals(citationName.preferredCitationName())))
            .map(citationName -> splitName(citationName.value(), PersonNameType.CITATION_NAME))
            .forEach(candidates::add);

        // A form identical to one already kept adds nothing, whatever its type
        var seen = new HashSet<String>();
        seen.add(nameKey(personName));

        return candidates.stream()
            .filter(Objects::nonNull)
            .filter(name -> seen.add(nameKey(name)))
            .toList();
    }

    /**
     * Citation names come as {@code "Surname, Given"}; other single-string forms put the surname
     * last.
     */
    private PersonNameDTO splitName(String value, PersonNameType type) {
        if (isBlank(value)) {
            return null;
        }

        var trimmed = value.trim().replaceAll("\\s+", " ");
        var comma = trimmed.indexOf(',');

        if (comma > 0 && comma < trimmed.length() - 1) {
            return name(trimmed.substring(comma + 1).trim(), trimmed.substring(0, comma).trim(),
                type);
        }

        var lastSpace = trimmed.lastIndexOf(' ');

        if (lastSpace < 0) {
            return name(trimmed, trimmed, type);
        }

        return name(trimmed.substring(0, lastSpace), trimmed.substring(lastSpace + 1), type);
    }

    private PersonNameDTO name(String firstname, String lastname, PersonNameType type) {
        var name = new PersonNameDTO();
        name.setFirstname(firstname);
        name.setLastname(lastname);
        name.setPersonNameType(type);

        return name;
    }

    private String nameKey(PersonNameDTO name) {
        return conversionUtil.normalise(name.getFirstname() + " " + name.getLastname());
    }

    // MAP-000020: the source keeps the date only when it is public
    private LocalDate birthDate(HydratorCVModel.DateOfBirth dateOfBirth) {
        if (Objects.isNull(dateOfBirth) || !isPublic(dateOfBirth.privacyLevel())) {
            return null;
        }

        var year = conversionUtil.parseInteger(dateOfBirth.year());
        var month = conversionUtil.parseInteger(dateOfBirth.month());
        var day = conversionUtil.parseInteger(dateOfBirth.day());

        if (Objects.isNull(year) || Objects.isNull(month) || Objects.isNull(day)) {
            return null;
        }

        try {
            return LocalDate.of(year, month, day);
        } catch (DateTimeException e) {
            return null;
        }
    }

    // MAP-000021
    private Sex sex(HydratorCVModel.Curriculum record, HydratorCVModel.Gender gender) {
        if (Objects.isNull(gender) || isBlank(gender.code())) {
            return null;
        }

        var sex = SEX_BY_CODE.get(gender.code().trim().toUpperCase(Locale.ROOT));

        if (Objects.isNull(sex)) {
            dropped(record, "MAP-000021", "unmapped gender code '" + gender.code() + "'");
        }

        return sex;
    }

    // MAP-000022: the source carries only a file name, never the image itself
    private void logPhotoDropped(HydratorCVModel.Curriculum record,
                                 HydratorCVModel.Photography photography) {
        if (Objects.nonNull(photography) && isPublic(photography.privacyLevel()) &&
            !isBlank(photography.fileName())) {
            dropped(record, "MAP-000022",
                "photo '" + photography.fileName() + "' has no retrievable content");
        }
    }

    // MAP-000036: every domain's keywords, one per line
    private String keywords(HydratorCVModel.IdentifyingInfo info) {
        var keywords = new LinkedHashSet<String>();

        listOf(info.domainActivities(), HydratorCVModel.DomainActivities::domainActivity).stream()
            .map(HydratorCVModel.DomainActivity::keywords)
            .filter(Objects::nonNull)
            .map(HydratorCVModel.Keywords::keyword)
            .filter(Objects::nonNull)
            .flatMap(List::stream)
            .filter(keyword -> !isBlank(keyword))
            .map(String::trim)
            .forEach(keywords::add);

        return String.join("\n", keywords);
    }

    // MAP-000030: professional unless typed personal; EMAIL_PRECEDENCE picks the slot winner
    private void setEmails(HydratorCVModel.Curriculum record, ImportPersonDTO dto,
                           HydratorCVModel.Emails emails) {
        listOf(emails, HydratorCVModel.Emails::email).stream()
            .filter(email -> !isBlank(email.emailAddress()))
            .sorted(EMAIL_PRECEDENCE)
            .forEach(email -> {
                var address = email.emailAddress().trim();

                if (isPersonal(email.emailType())) {
                    fill(record, "MAP-000030", address, dto::getPrivateContactEmail,
                        dto::setPrivateContactEmail);
                } else {
                    fill(record, "MAP-000030", address, dto::getContactEmail,
                        dto::setContactEmail);
                }
            });
    }

    // MAP-000031: phone type picks the field, usage type picks professional vs private contact
    private void setPhoneNumbers(HydratorCVModel.Curriculum record, ImportPersonDTO dto,
                                 HydratorCVModel.PhoneNumbers phoneNumbers) {
        listOf(phoneNumbers, HydratorCVModel.PhoneNumbers::phoneNumber).stream()
            .filter(phoneNumber -> !isBlank(phoneNumber.localNumber()))
            .sorted(PHONE_NUMBER_PRECEDENCE)
            .forEach(phoneNumber -> {
                var number = phoneNumber(phoneNumber);
                var personal = isPersonal(phoneNumber.usageType());
                var phoneType = Objects.isNull(phoneNumber.phoneType()) ? null :
                    phoneNumber.phoneType().code();

                if (MOBILE_PHONE_CODE.equals(phoneType)) {
                    fill(record, "MAP-000031", number,
                        personal ? dto::getPrivateMobilePhoneNumber :
                            dto::getMobilePhoneNumber,
                        personal ? dto::setPrivateMobilePhoneNumber :
                            dto::setMobilePhoneNumber);
                } else if (FAX_CODE.equals(phoneType)) {
                    fill(record, "MAP-000031", number,
                        personal ? dto::getPrivateFaxNumber : dto::getFaxNumber,
                        personal ? dto::setPrivateFaxNumber : dto::setFaxNumber);
                } else {
                    fill(record, "MAP-000031", number,
                        personal ? dto::getPrivatePhoneNumber : dto::getPhoneNumber,
                        personal ? dto::setPrivatePhoneNumber : dto::setPhoneNumber);
                }
            });
    }

    private String phoneNumber(HydratorCVModel.PhoneNumber phoneNumber) {
        var number = phoneNumber.localNumber().trim();

        if (!isBlank(phoneNumber.countryCode())) {
            var countryCode = phoneNumber.countryCode().trim();
            number = (countryCode.startsWith("+") ? countryCode : "+" + countryCode) + " " + number;
        }

        if (!isBlank(phoneNumber.extension())) {
            number = number + " ext. " + phoneNumber.extension().trim();
        }

        return number;
    }

    // MAP-000032: professional unless typed personal; MAILING_ADDRESS_PRECEDENCE picks the winner
    private void setMailingAddresses(HydratorCVModel.Curriculum record, ImportPersonDTO dto,
                                     HydratorCVModel.MailingAddresses mailingAddresses,
                                     String language) {
        listOf(mailingAddresses, HydratorCVModel.MailingAddresses::mailingAddress).stream()
            .sorted(MAILING_ADDRESS_PRECEDENCE)
            .forEach(address -> {
                var personal = isPersonal(address.addressType());
                var slotTaken = personal ?
                    !dto.getPrivateAddressLine().isEmpty() ||
                        !dto.getPrivateAddressCity().isEmpty() :
                    !dto.getAddressLine().isEmpty() || !dto.getAddressCity().isEmpty();

                if (slotTaken) {
                    dropped(record, "MAP-000032", "additional " +
                        (personal ? "personal" : "professional") + " address " + address.id() +
                        " ranked below the kept one");
                    return;
                }

                var line = conversionUtil.multilingualContent(address.streetAddress(), language);
                var city = conversionUtil.multilingualContent(address.city(), language);
                var state = conversionUtil.multilingualContent(address.provinceState(), language);
                var postalCode =
                    isBlank(address.postalCode()) ? null : address.postalCode().trim();
                var countryCode = Objects.isNull(address.country()) ||
                    isBlank(address.country().code()) ? null : address.country().code().trim();

                if (personal) {
                    dto.setPrivateAddressLine(line);
                    dto.setPrivateAddressCity(city);
                    dto.setPrivateAddressState(state);
                    dto.setPrivatePostalNumber(postalCode);
                    dto.setPrivateCountryCode(countryCode);
                } else {
                    dto.setAddressLine(line);
                    dto.setAddressCity(city);
                    dto.setAddressState(state);
                    dto.setPostalNumber(postalCode);
                    dto.setCountryCode(countryCode);
                }
            });
    }

    private static int completeness(HydratorCVModel.MailingAddress address) {
        var parts = new ArrayList<String>();
        parts.add(address.streetAddress());
        parts.add(address.city());
        parts.add(address.postalCode());
        parts.add(Objects.isNull(address.country()) ? null : address.country().code());

        return (int) parts.stream().filter(part -> !isBlankValue(part)).count();
    }

    // MAP-000033: the target has no field for the site type, so that distinction is lost
    private Set<String> uris(HydratorCVModel.Curriculum record,
                             HydratorCVModel.WebAddresses webAddresses) {
        var uris = new LinkedHashSet<String>();

        listOf(webAddresses, HydratorCVModel.WebAddresses::webAddress).stream()
            .filter(webAddress -> !isBlank(webAddress.url()))
            .forEach(webAddress -> {
                var url = webAddress.url().trim();
                uris.add(URL_SCHEME.matcher(url).matches() ? url : "https://" + url);

                if (Objects.nonNull(webAddress.siteType())) {
                    dropped(record, "MAP-000033",
                        "site type '" + webAddress.siteType().value() + "' of " + url);
                }
            });

        return uris;
    }

    // MAP-000034: the person import carries no expertises and skills
    private void logLanguageCompetenciesDropped(
        HydratorCVModel.Curriculum record,
        HydratorCVModel.LanguageCompetencies languageCompetencies) {
        listOf(languageCompetencies, HydratorCVModel.LanguageCompetencies::languageCompetency)
            .stream()
            .map(HydratorCVModel.LanguageCompetency::language)
            .filter(Objects::nonNull)
            .forEach(language -> dropped(record, "MAP-000034",
                "language competency '" + language.code() + "'"));
    }

    // MAP-000035: no vocabulary maps FOS codes onto TeslaRIS research areas yet
    private void logResearchClassificationsDropped(
        HydratorCVModel.Curriculum record,
        HydratorCVModel.DomainActivities domainActivities) {
        listOf(domainActivities, HydratorCVModel.DomainActivities::domainActivity).stream()
            .map(HydratorCVModel.DomainActivity::researchClassification)
            .filter(Objects::nonNull)
            .forEach(classification -> dropped(record, "MAP-000035",
                "research classification '" + classification.code() + "'"));
    }

    // MAP-000037: the identifier type picks the target field
    private void setIdentifiers(HydratorCVModel.Curriculum record, ImportPersonDTO dto) {
        dto.setNationalScienceId(
            identifier(record, CIENCIA_ID_CODE, value -> value, null));
        dto.setOrcid(identifier(record, ORCID_CODE,
            value -> value.replaceFirst("(?i)^https?://(www\\.)?orcid\\.org/", "")
                .toUpperCase(Locale.ROOT), ORCID_PATTERN));
        dto.setScopusAuthorId(identifier(record, SCOPUS_CODE, value -> value, SCOPUS_PATTERN));
        dto.setScholarId(
            identifier(record, GOOGLE_SCHOLAR_CODE, this::scholarId, SCHOLAR_PATTERN));
        dto.setWebOfScienceResearcherId(identifier(record, WEB_OF_SCIENCE_CODE,
            value -> value.toUpperCase(Locale.ROOT), WEB_OF_SCIENCE_PATTERN));
    }

    /**
     * First valid value of the given type (valid over invalid); other values of that type are
     * logged. With no valid value at all, the first invalid one is returned on purpose: the import
     * rejects its format, so the person fails and is retried instead of losing the identifier.
     */
    private String identifier(HydratorCVModel.Curriculum record, String code,
                              Function<String, String> canonicalise, Pattern format) {
        var values = identifiersOf(record).stream()
            .filter(identifier -> Objects.nonNull(identifier.identifierType()) &&
                code.equalsIgnoreCase(identifier.identifierType().code()) &&
                !isBlank(identifier.identifier()))
            .map(identifier -> canonicalise.apply(identifier.identifier().trim()))
            .distinct()
            .toList();

        if (values.isEmpty()) {
            return null;
        }

        var accepted = values.stream()
            .filter(value -> Objects.isNull(format) || format.matcher(value).matches())
            .findFirst()
            .orElse(values.getFirst());

        values.stream()
            .filter(value -> !value.equals(accepted))
            .forEach(value -> dropped(record, "MAP-000037",
                "additional " + code + " '" + value + "', kept '" + accepted + "'"));

        return accepted;
    }

    // Scholar ids are often entered as the profile URL or with trailing query parameters
    private String scholarId(String value) {
        var matcher = SCHOLAR_USER_PARAMETER.matcher(value);

        if (matcher.find()) {
            return matcher.group(1);
        }

        var parameters = value.indexOf('&');

        return parameters < 0 ? value : value.substring(0, parameters);
    }

    private void fill(HydratorCVModel.Curriculum record, String rule, String value,
                      Supplier<String> current, Consumer<String> setter) {
        if (Objects.isNull(current.get())) {
            setter.accept(value);
        } else {
            dropped(record, rule,
                "additional value '" + value + "' ranked below kept '" + current.get() + "'");
        }
    }

    private List<HydratorCVModel.AuthorIdentifier> identifiersOf(
        HydratorCVModel.Curriculum record) {
        return listOf(identifyingInfoOf(record).authorIdentifiers(),
            HydratorCVModel.AuthorIdentifiers::authorIdentifier);
    }

    private HydratorCVModel.IdentifyingInfo identifyingInfoOf(HydratorCVModel.Curriculum record) {
        if (Objects.isNull(record.curriculum())) {
            return null;
        }

        return record.curriculum().identifyingInfo();
    }

    private <C, T> List<T> listOf(C container, Function<C, List<T>> items) {
        if (Objects.isNull(container) || Objects.isNull(items.apply(container))) {
            return List.of();
        }

        return items.apply(container).stream().filter(Objects::nonNull).toList();
    }

    private void dropped(HydratorCVModel.Curriculum record, String rule, String reason) {
        migrationLog.valueDropped(HydratorSource.NAME, MigrationEntityType.PERSON.name(),
            record.id(), rule, reason);
    }

    private boolean isPersonal(HydratorCVModel.CodeValue type) {
        return Objects.nonNull(type) && PERSONAL_CODE.equals(type.code());
    }

    private boolean isPublic(String privacyLevel) {
        return Objects.isNull(privacyLevel) || PUBLIC_PRIVACY_LEVEL.equals(privacyLevel);
    }

    private boolean isBlank(String value) {
        return isBlankValue(value);
    }

    private static boolean isBlankValue(String value) {
        return Objects.isNull(value) || value.isBlank();
    }
}
