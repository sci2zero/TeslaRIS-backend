package rs.teslaris.core.util.search;

import com.ibm.icu.text.Normalizer2;
import com.ibm.icu.text.Transliterator;
import jakarta.annotation.Nonnull;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceTokenizer;
import org.apache.lucene.analysis.miscellaneous.ASCIIFoldingFilter;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.queryparser.classic.QueryParserBase;
import org.jbibtex.BibTeXDatabase;
import org.jbibtex.BibTeXEntry;
import org.jbibtex.BibTeXFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import rs.teslaris.core.model.commontypes.MultiLingualContent;

@Component
@Slf4j
public class StringUtil {

    private static final Pattern CLEAN_PATTERN = Pattern.compile("(\\b\\d+\\b)|[\\p{Punct}]+");

    private static final Pattern COMBINING_MARKS = Pattern.compile("\\p{M}+");

    private static final Pattern BAD_CHARS = Pattern.compile("[^\\p{L}\\p{Nd} ]+");

    private static final Pattern MULTI_SPACE = Pattern.compile("\\s{2,}");

    private static List<String> stopwords;

    private static Analyzer analyzer;


    @Autowired
    public StringUtil() throws IOException {
        StringUtil.stopwords =
            Files.readAllLines(
                Paths.get("src/main/resources/configuration/notable_stopwords.txt")
            );
        log.info("Loaded notable stop words for manual pre-processing.");

        StringUtil.analyzer = new Analyzer() {
            @Override
            protected TokenStreamComponents createComponents(String fieldName) {
                return new TokenStreamComponents(new WhitespaceTokenizer());
            }
        };
    }

    public static void removeTrailingDelimiters(StringBuilder contentSr,
                                                StringBuilder contentOther) {
        removeTrailingDelimitersHelper(contentSr);
        removeTrailingDelimitersHelper(contentOther);
    }

    private static void removeTrailingDelimitersHelper(StringBuilder content) {
        String[] delimiters = {" | ", " | ;", " | ; ", "; "};

        for (String delimiter : delimiters) {
            if (content.toString().endsWith(delimiter)) {
                content.delete(content.length() - delimiter.length(), content.length());
                break;
            }
        }
    }

    public static String removeLeadingColonSpace(String content) {
        StringBuilder stringBuilder = new StringBuilder(content);
        if (stringBuilder.toString().startsWith("; ")) {
            stringBuilder.delete(0, 2);
        }
        return stringBuilder.toString();
    }

    public static void sanitizeTokens(List<String> tokens) {
        tokens.replaceAll(
            token -> (token.equals("*") || token.equals(".")) ? "*" :
                QueryParserBase.escape(token));
    }

    @Nonnull
    public static String performSimpleLatinPreprocessing(String input) {
        return performSimpleLatinPreprocessing(input, true);
    }

    @Nonnull
    public static String performSimpleLatinPreprocessing(String input, boolean performFolding) {
        if (input == null) {
            return "";
        }

        // Handle Serbian-specific digraph mappings
        input = input
            .replace("ђ", "dj")
            .replace("Ђ", "Dj")
            .replace("њ", "nj")
            .replace("Њ", "Nj")
            .replace("љ", "lj")
            .replace("Љ", "Lj")
            .replace("đ", "dj")
            .replace("Đ", "Dj")
            .replace("џ", "dz")
            .replace("Џ", "Dz");

        var transliterator =
            Transliterator.getInstance("Any-Latin; NFD; [:Nonspacing Mark:] Remove; NFC");

        var result = transliterator.transliterate(input.toLowerCase());

        if (performFolding) {
            result = applyASCIIFolding(result);
        }

        var normalizer = Normalizer2.getNFDInstance();
        result = normalizer.normalize(result);

        result = result.toLowerCase();

        result = result.replaceAll("[^a-z0-9 ]", "");

        return result;
    }

    private static String applyASCIIFolding(String input) {
        try (var tokenizer = new WhitespaceTokenizer()) {
            tokenizer.setReader(new StringReader(input));
            var tokenStream = new ASCIIFoldingFilter(tokenizer);
            tokenStream.reset();

            var folded = new StringBuilder();
            var attr = tokenStream.getAttribute(CharTermAttribute.class);

            while (tokenStream.incrementToken()) {
                folded.append(attr.toString()).append(" ");
            }

            tokenStream.end();
            tokenStream.close();

            return folded.toString().trim();
        } catch (IOException e) {
            throw new RuntimeException("Failed to apply ASCII folding",
                e); // Should never happen with StringReader
        }
    }

    public static String formatIssn(String issn) {
        if (issn == null || issn.isEmpty()) {
            return "";
        }

        if (issn.length() < 8) {
            return "";
        }

        if (issn.contains("-")) {
            return issn;
        }

        return issn.substring(0, 4) + "-" + issn.substring(4, 8);
    }

    public static String getStringContent(Set<MultiLingualContent> multilingualContent,
                                          String lang) {
        MultiLingualContent fallback = null;
        for (var content : multilingualContent) {
            if (lang.equalsIgnoreCase(content.getLanguage().getLanguageTag())) {
                return content.getContent();
            }

            fallback = content;
        }

        return Objects.nonNull(fallback) ? fallback.getContent() : "";
    }

    public static boolean isInteger(String s, int radix) {
        if (s.isEmpty()) {
            return false;
        }
        for (int i = 0; i < s.length(); i++) {
            if (i == 0 && s.charAt(i) == '-') {
                if (s.length() == 1) {
                    return false;
                } else {
                    continue;
                }
            }
            if (Character.digit(s.charAt(i), radix) < 0) {
                return false;
            }
        }
        return true;
    }

    public static void removeNotableStopwords(List<String> tokens) {
        tokens.removeAll(stopwords);
    }

    public static List<String> extractKeywords(String... texts) {
        List<String> keywords = new ArrayList<>();
        for (String text : texts) {
            if (Objects.isNull(text) || text.isBlank()) {
                continue;
            }
            keywords.addAll(tokenize(text));
        }
        return new ArrayList<>(keywords);
    }

    private static List<String> tokenize(String text) {
        List<String> tokens = new ArrayList<>();
        try (var tokenStream = analyzer.tokenStream("field", new StringReader(text))) {
            var attr = tokenStream.addAttribute(CharTermAttribute.class);
            tokenStream.reset();
            while (tokenStream.incrementToken()) {
                String rawToken = attr.toString().toLowerCase(Locale.ROOT);
                String cleaned = CLEAN_PATTERN.matcher(rawToken).replaceAll("");
                if (!cleaned.isBlank() && !stopwords.contains(cleaned)) {
                    tokens.add(cleaned);
                }
            }
            tokenStream.end();
        } catch (IOException e) {
            throw new RuntimeException("Error during tokenization", e); // should never happen
        }
        return tokens;
    }

    public static Optional<Integer> romanToInt(String s) {
        s = s.toUpperCase();
        Map<Character, Integer> map = new HashMap<>();
        map.put('I', 1);
        map.put('V', 5);
        map.put('X', 10);
        map.put('L', 50);
        map.put('C', 100);
        map.put('D', 500);
        map.put('M', 1000);

        try {
            int result = map.get(s.charAt(s.length() - 1));
            for (int i = s.length() - 2; i >= 0; i--) {
                if (map.get(s.charAt(i)) < map.get(s.charAt(i + 1))) {
                    result -= map.get(s.charAt(i));
                } else {
                    result += map.get(s.charAt(i));

                }
            }
            return Optional.of(result);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public static String bibTexEntryToString(BibTeXEntry entry) {
        var writer = new StringWriter();
        var formatter = new BibTeXFormatter();
        var database = new BibTeXDatabase();
        database.addObject(entry);
        try {
            formatter.format(database, writer);
        } catch (IOException e) {
            throw new IllegalStateException(
                "Error while creating BibTex representation"); // should never happen
        }

        return writer + "\n";
    }

    public static String performDOIPreprocessing(String token) {
        return token.replace("\\-", "-").replace("\\/", "/").replace("\\:", ":")
            .replace("https://doi.org/", "");
    }

    public static boolean valueExists(String value) {
        return Objects.nonNull(value) && !value.isBlank();
    }

    public static String sanitizeForKeywordFieldFast(String input) {
        if (Objects.isNull(input) || input.isEmpty()) {
            return input;
        }

        var normalized = Normalizer.normalize(input, Normalizer.Form.NFKD);

        normalized = COMBINING_MARKS.matcher(normalized).replaceAll("");
        normalized = BAD_CHARS.matcher(normalized).replaceAll(" ");

        return MULTI_SPACE.matcher(normalized.trim()).replaceAll(" ");
    }

}
