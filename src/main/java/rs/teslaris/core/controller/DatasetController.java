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

    @GetMapping("/{documentId}")
    public DatasetDTO readDataset(
        @PathVariable Integer documentId) {
        return datasetService.readDatasetById(documentId);
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

    @PutMapping("/{documentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PublicationEditCheck
    public void editDataset(@PathVariable Integer documentId,
                            @RequestBody @Valid DatasetDTO dataset) {
        datasetService.editDataset(documentId, dataset);
    }

    @DeleteMapping("/{documentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PublicationEditCheck
    public void deleteDataset(@PathVariable Integer documentId) {
        datasetService.deleteDataset(documentId);
    }
}
