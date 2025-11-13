package rs.teslaris.importer.utility.skgif;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import rs.teslaris.core.util.session.RestTemplateProvider;

@Component
@RequiredArgsConstructor
@Slf4j
public class SKGIFImportUtility {

    private final RestTemplateProvider restTemplateProvider;


//    public List<ResearchProduct> getPublicationsForAuthors(List<String> internalEntityIds,
//                                                           Boolean institutionLevelHarvest,
//                                                           String dateFrom,
//                                                           String dateTo,
//                                                           String baseUrl) {
//        var allResults = new ArrayList<ResearchProduct>();
//
//
//        for (String entityId : internalEntityIds) {
//            String harvestUrl = baseUrl + "products?page_size=" + PAGE_SIZE +
//                "&filter=" +
//                (institutionLevelHarvest ? "relevant_organisations:" : "contributions.by:") +
//                entityId +
//                "dateFrom=" + dateFrom + "&dateTo=" + dateTo;
//
//            var page = 0;
//
//        }
//
//        return allResults;
//    }
}
