package rs.teslaris.core.util.seeding;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.SKOS;
import org.apache.jena.vocabulary.SKOSXL;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import rs.teslaris.core.model.commontypes.MultiLingualContent;
import rs.teslaris.core.model.commontypes.ResearchArea;
import rs.teslaris.core.service.interfaces.commontypes.LanguageTagService;
import rs.teslaris.core.service.interfaces.commontypes.ResearchAreaService;
import rs.teslaris.core.util.language.LanguageAbbreviations;
import rs.teslaris.core.util.search.StringUtil;

@Component
@Slf4j
@RequiredArgsConstructor
public class SKOSLoader {

    private static final String RDF_FILE_DIRECTORY = "src/main/resources/dbSeedData/";

    private static final Map<String, Integer> loaded = new HashMap<>();

    private final LanguageTagService languageTagService;

    private final ResearchAreaService researchAreaService;


    @Async
    public void loadResearchAreas() {
        log.info("Started loading research areas.");

        var model = ModelFactory.createDefaultModel();
        model.read(RDF_FILE_DIRECTORY + "researchAreas.ttl");

        var iterator = model.listResourcesWithProperty(RDF.type, SKOS.Concept);

        while (iterator.hasNext()) {
            var skosConcept = iterator.nextResource();
            var researchArea = new ResearchArea();

            var uri = skosConcept.getURI();
            if (loaded.containsKey(uri)) {
                continue;
            }

            populateResearchAreaFields(researchArea, skosConcept, model);
            setupSearchField(researchArea);

            var saved = researchAreaService.save(researchArea);
            loaded.put(skosConcept.getURI(), saved.getId());
        }

        log.info("Finished loading research areas.");
    }

    private void populateResearchAreaFields(ResearchArea researchArea, Resource skosConcept,
                                            Model model) {
        var names = new HashSet<MultiLingualContent>();
        var priority = new AtomicInteger(1);
        skosConcept.listProperties(SKOSXL.prefLabel).forEachRemaining(statement -> {
            var object = model.getResource(String.valueOf(statement.getObject()));
            var label = object.getProperty(SKOSXL.literalForm);
            var languageTag = label.getLanguage().toUpperCase();
            var language = languageTagService.findLanguageTagByValue(languageTag);

            if (Objects.nonNull(language.getLanguageTag())) {
                names.add(new MultiLingualContent(language, label.getString(),
                    priority.getAndAdd(1)));
            }

            if (languageTag.equals(LanguageAbbreviations.ENGLISH)) {
                names.add(new MultiLingualContent(
                    languageTagService.findLanguageTagByValue(LanguageAbbreviations.SERBIAN),
                    label.getString(),
                    priority.getAndAdd(1)));
            }
        });
        researchArea.setName(names);

        var broaderConcept = skosConcept.getPropertyResourceValue(SKOS.broader);
        if (broaderConcept != null) {
            ResearchArea broaderResearchArea = getOrCreateBroaderArea(model, broaderConcept);
            researchArea.setSuperResearchArea(broaderResearchArea);
        }
    }

    private ResearchArea getOrCreateBroaderArea(Model model, Resource skosConcept) {
        String uri = skosConcept.getURI();
        if (loaded.containsKey(uri)) {
            return researchAreaService.findOne(loaded.get(uri));
        }

        var broaderArea = new ResearchArea();

        populateResearchAreaFields(broaderArea, skosConcept, model);
        setupSearchField(broaderArea);

        var saved = researchAreaService.save(broaderArea);
        loaded.put(skosConcept.getURI(), saved.getId());
        return saved;
    }

    private void setupSearchField(ResearchArea researchArea) {
        researchArea.setProcessedName("");
        researchArea.getName().forEach(name -> {
            researchArea.setProcessedName(researchArea.getProcessedName() + " " +
                StringUtil.performSimpleLatinPreprocessing(name.getContent()));
        });
    }
}
