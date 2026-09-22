package rs.teslaris.migrator.client.hydrator;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import rs.teslaris.migrator.client.RestPage;
import rs.teslaris.migrator.model.hydrator.HydratorCVModel;

@FeignClient(
    name = "hydratorCVClient",
    url = "${migrator.sources.hydrator.base-url:http://localhost:8081}",
    configuration = HydratorFeignConfiguration.class
)
public interface HydratorCVClient {

    /**
     * The sort is pinned rather than relying on the server default, so a traversal is at least
     * consistent between pages of the same run.
     */
    @GetMapping("/api/curricula")
    RestPage<HydratorCVModel.Curriculum> getCurricula(
        @RequestParam(value = "modifiedAfter", required = false) String modifiedAfter,
        @RequestParam("page") int page,
        @RequestParam("size") int size,
        @RequestParam("sort") String sort
    );
}
