package rs.teslaris.core.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import rs.teslaris.core.util.search.StringUtil;

public class StringUtilTest {

    @Test
    void shouldStripSourceSchemeFromHarvestedIdentifier() {
        assertEquals("85123456789", StringUtil.normalizeIdentifier("SCOPUS_ID:85123456789"));
        assertEquals("85123456789", StringUtil.normalizeIdentifier("SCOPUS:85123456789"));
        assertEquals("000123456789012", StringUtil.normalizeIdentifier("WOS:000123456789012"));
        assertEquals("000123456789012",
            StringUtil.normalizeIdentifier("WEB_OF_SCIENCE:000123456789012"));
        assertEquals("W123456789", StringUtil.normalizeIdentifier("openalex:W123456789"));
        assertEquals("W123456789", StringUtil.normalizeIdentifier("OPEN_ALEX:W123456789"));
    }

    @Test
    void shouldStripResolverUrlAndLeaveBareIdentifierIntact() {
        assertEquals("10.35802/110166",
            StringUtil.normalizeIdentifier("https://doi.org/10.35802/110166"));
        assertEquals("85123456789", StringUtil.normalizeIdentifier("85123456789"));
    }

    @Test
    void shouldEncodeUnsafeCharactersInHarvestedUrl() {
        var rawUrl =
            "https://europepmc.org/grantfinder/grantdetails?query=pi:\"%7BProfessor%7D\" gid:\"110166\"";

        var sanitizedUrl = StringUtil.sanitizeUrl(rawUrl);

        assertEquals(
            "https://europepmc.org/grantfinder/grantdetails?query=pi:%22%7BProfessor%7D%22%20gid:%22110166%22",
            sanitizedUrl);
    }

    @Test
    void shouldKeepValidUrlIntactAndTrimIt() {
        assertEquals("https://doi.org/10.35802/110166",
            StringUtil.sanitizeUrl("  https://doi.org/10.35802/110166  "));
    }

    @Test
    void shouldDiscardValuesThatAreNotAbsoluteHttpAddresses() {
        assertNull(StringUtil.sanitizeUrl("not a url at all"));
        assertNull(StringUtil.sanitizeUrl("ftp://example.com/file.txt"));
        assertNull(StringUtil.sanitizeUrl("   "));
        assertNull(StringUtil.sanitizeUrl(null));
    }
}
