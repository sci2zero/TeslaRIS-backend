package rs.teslaris.core.controller;

import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.core.annotation.Idempotent;
import rs.teslaris.core.annotation.PublicationEditCheck;
import rs.teslaris.core.dto.document.DatasetDTO;
import rs.teslaris.core.service.interfaces.document.DatasetService;

@RestController
@RequestMapping("api/dataset")
@RequiredArgsConstructor
public class DatasetController {

    private final DatasetService datasetService;

    @GetMapping("/{publicationId}")
    public DatasetDTO readDataset(
        @PathVariable Integer publicationId) {
        return datasetService.readDatasetById(publicationId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PublicationEditCheck("CREATE")
    @Idempotent
    public DatasetDTO createDataset(@RequestBody @Valid DatasetDTO dataset) {
        var savedDataset = datasetService.createDataset(dataset, true);
        dataset.setId(savedDataset.getId());
        return dataset;
    }

    @PutMapping("/{publicationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PublicationEditCheck
    public void editDataset(@PathVariable Integer publicationId,
                            @RequestBody @Valid DatasetDTO dataset) {
        datasetService.editDataset(publicationId, dataset);
    }

    @DeleteMapping("/{publicationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PublicationEditCheck
    public void deleteDataset(@PathVariable Integer publicationId) {
        datasetService.deleteDataset(publicationId);
    }
}
