package rs.teslaris.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * The root packages under {@code rs.teslaris} are modules in waiting: each is meant to become its
 * own Maven artifact, and an artifact cannot be extracted while something it depends on depends
 * back. Every module may use {@code core}; nothing else is open to it.
 * <p>
 * {@code KNOWN_EXCEPTIONS} was the migration backlog and is now empty: no module depends on
 * anything but {@code core}. It is not a permission list, meaning a new entry means a module just became
 * un-extractable, so the design decision has to be made deliberately, which is the point of this
 * test.
 */
public class ModuleBoundaryTest {

    private static final String ROOT = "rs.teslaris";

    private static final String SHARED_MODULE = "core";

    private static final List<String> MODULES = List.of(
        "assessment", "exporter", "importer", "migrator", "project", "reporting", "revisioner",
        "thesislibrary"
    );

    private static final Map<String, Set<String>> KNOWN_EXCEPTIONS = Map.of();

    /**
     * {@code core} reaching back into a module is the worse direction: it makes that module
     * un-extractable and drags it into every other module through core. This set is empty, and
     * must stay empty - a new entry here is a module that can no longer be extracted.
     */
    private static final Set<String> MODULES_CORE_STILL_REACHES_INTO = Set.of();

    private static JavaClasses classes;


    @BeforeAll
    public static void importProductionClasses() {
        classes = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages(ROOT);
    }

    private static Stream<Arguments> moduleAndForbiddenModule() {
        return MODULES.stream().flatMap(module -> MODULES.stream()
            .filter(other -> !other.equals(module))
            .filter(other -> !KNOWN_EXCEPTIONS.getOrDefault(module, Set.of()).contains(other))
            .map(other -> Arguments.of(module, other)));
    }

    private static Stream<String> modulesCoreMustNotReachInto() {
        return MODULES.stream().filter(module -> !MODULES_CORE_STILL_REACHES_INTO.contains(module));
    }

    @ParameterizedTest(name = "{0} must not depend on {1}")
    @MethodSource("moduleAndForbiddenModule")
    public void moduleMustNotDependOnAnotherModule(String module, String forbiddenModule) {
        noClasses()
            .that().resideInAPackage(ROOT + "." + module + "..")
            .should().dependOnClassesThat()
            .resideInAPackage(ROOT + "." + forbiddenModule + "..")
            .because(module + " may only depend on " + SHARED_MODULE +
                ", so that it can be extracted into its own module")
            .check(classes);
    }

    @ParameterizedTest(name = SHARED_MODULE + " must not depend on {0}")
    @MethodSource("modulesCoreMustNotReachInto")
    public void sharedModuleMustNotDependOnAModule(String module) {
        noClasses()
            .that().resideInAPackage(ROOT + "." + SHARED_MODULE + "..")
            .should().dependOnClassesThat().resideInAPackage(ROOT + "." + module + "..")
            .because(SHARED_MODULE + " is what every module builds on, so depending on " + module +
                " would make it un-extractable and pull it into every other module")
            .check(classes);
    }
}
