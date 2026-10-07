package rs.teslaris.migrator.client.hydrator;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import rs.teslaris.migrator.client.RestPage;
import rs.teslaris.migrator.model.hydrator.HydratorProjectModel;

@FeignClient(
    name = "hydratorProjectClient",
    url = "${migrator.sources.hydrator.base-url:http://localhost:8081}",
    configuration = HydratorFeignConfiguration.class
)
public interface HydratorProjectClient {

    @GetMapping("/api/projects")
    RestPage<HydratorProjectModel.ProjectDocument> getProjects(
        @RequestParam(value = "modifiedAfter", required = false) String modifiedAfter,
        @RequestParam("page") int page,
        @RequestParam("size") int size,
        @RequestParam("sort") String sort
    );
}
