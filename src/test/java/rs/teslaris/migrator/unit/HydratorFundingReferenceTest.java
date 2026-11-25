package rs.teslaris.migrator.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import rs.teslaris.migrator.converter.hydrator.HydratorFundingReference;
import rs.teslaris.migrator.converter.hydrator.HydratorFundingReference.Kind;

public class HydratorFundingReferenceTest {

    static Stream<Arguments> strongReferences() {
        return Stream.of(
            Arguments.of("PTDC/ABC-XYZ/1234/2020", Kind.FCT, "PTDC/ABC-XYZ/1234/2020"),
            Arguments.of("UIDB/00123/2020", Kind.FCT, "UIDB/00123/2020"),
            Arguments.of("SFRH/BD/123456/2019", Kind.FCT, "SFRH/BD/123456/2019"),
            Arguments.of("PTDC/Multi/12345/2014", Kind.FCT, "PTDC/Multi/12345/2014"),
            Arguments.of("ptdc/abc/1234/2020", Kind.FCT, "ptdc/abc/1234/2020"),
            Arguments.of("PTDC/ABC-XYZ-QWE/1234/2020", Kind.FCT, "PTDC/ABC-XYZ-QWE/1234/2020"),
            Arguments.of("  PTDC/ABC/1234/2020  ", Kind.FCT, "PTDC/ABC/1234/2020"),
            Arguments.of("FCT - PTDC/ABC-XYZ/1234/2020 (Test Project)", Kind.FCT,
                "PTDC/ABC-XYZ/1234/2020"),
            Arguments.of("info:eu-repo/grantAgreement/FCT/5876/PTDC%2FABC-XYZ%2F1234%2F2020/PT",
                Kind.FCT, "PTDC/ABC-XYZ/1234/2020"),
            Arguments.of("info:eu-repo/grantAgreement/FCT/1234 - TEST ID/UIDB%2F00123%2F2020/PT",
                Kind.FCT, "UIDB/00123/2020"),
            Arguments.of("POCI-01-0145-FEDER-000001", Kind.QREN, "POCI-01-0145-FEDER-000001"),
            Arguments.of("ABC12-01-0145-FEDER-123456", Kind.QREN, "ABC12-01-0145-FEDER-123456"),
            Arguments.of("POCI-01-0145-FEDER-000001 and more", Kind.QREN,
                "POCI-01-0145-FEDER-000001"),
            Arguments.of("Projeto POCI-01-0145-FEDER-000001 (Test Project)", Kind.QREN,
                "POCI-01-0145-FEDER-000001")
        );
    }

    @ParameterizedTest
    @MethodSource("strongReferences")
    void shouldRecognizeStrongReference(String reference, Kind kind, String lookupValue) {
        var result = HydratorFundingReference.of(reference);

        assertEquals(kind, result.kind());
        assertEquals(lookupValue, result.lookupValue());
        assertEquals(reference.trim(), result.original());
        assertTrue(result.isStrong());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "-",
        ".",
        "123456",
        "Test Project Grant",
        "info:eu-repo/grantAgreement/FCT/5876/123456/PT",
        "info:eu-repo/grantAgreement/EC/H2020/123456/EU",
        "info:eu-repo/grantAgreement/EC/FP7/PTDC%2FABC%2F1234%2F2020/EU",
        "info:eu-repo/grantAgreement/FCT",
        "PTDC/ABC/1234/20201",
        "POCI-01-0145-FEDER-0000011"
    })
    void shouldTreatOtherReferencesAsWeak(String reference) {
        var result = HydratorFundingReference.of(reference);

        assertEquals(Kind.WEAK, result.kind());
        assertNull(result.lookupValue());
        assertEquals(reference, result.original());
        assertFalse(result.isStrong());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void shouldTreatBlankReferenceAsWeak(String reference) {
        var result = HydratorFundingReference.of(reference);

        assertEquals(Kind.WEAK, result.kind());
        assertNull(result.original());
        assertNull(result.lookupValue());
    }

    @Test
    void shouldKeepMalformedEuRepoUriAsIs() {
        var result = HydratorFundingReference.of("info:eu-repo/grantAgreement/FCT/%ZZ/PT");

        assertEquals(Kind.WEAK, result.kind());
        assertEquals("info:eu-repo/grantAgreement/FCT/%ZZ/PT", result.original());
    }
}
