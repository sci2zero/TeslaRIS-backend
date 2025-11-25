package rs.teslaris.core.util.language;

import com.ibm.icu.text.Transliterator;
import java.util.Objects;

public class SerbianTransliteration {
    private static final String SERBIAN_RULES = """
            Dž > Џ;
            Lj > Љ;
            Nj > Њ;
            dž > џ;
            lj > љ;
            nj > њ;
        
            Ö > О;
            Ü > У;
            Ä > А;
            ß > С;
            É > Е;
            È > Е;
            Ç > Ц;
        
            D > Д;
            L > Л;
            N > Н;
            d > д;
            l > л;
            n > н;
            Ђ > Ђ;
            đ > ђ;
        
            A > А; B > Б; C > Ц; Č > Ч; Ć > Ћ; E > Е; F > Ф; G > Г; H > Х;
            I > И; J > Ј; K > К; M > М; O > О; P > П; R > Р; S > С; Š > Ш; T > Т;
            U > У; V > В; Z > З; Ž > Ж;
        
            a > а; b > б; c > ц; č > ч; ć > ћ; e > е; f > ф; g > г; h > х;
            i > и; j > ј; k > к; m > м; o > о; p > п; r > р; s > с; š > ш; t > т;
            u > у; v > в; z > з; ž > ж;
        """;

    private static final Transliterator CUSTOM_LATIN_TO_CYRILLIC =
        Transliterator.createFromRules("Latin-Serbian-Custom", SERBIAN_RULES,
            Transliterator.FORWARD);

    public static String toCyrillic(String latinText) {
        if (Objects.isNull(latinText)) {
            return "";
        }

        return CUSTOM_LATIN_TO_CYRILLIC.transliterate(latinText);
    }
}
