package rs.teslaris.core.controller.document;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.core.dto.document.GeneticMaterialDTO;
import rs.teslaris.core.service.interfaces.document.GeneticMaterialService;
import rs.teslaris.core.util.signposting.FairSignpostingL1Utility;

@RestController
@RequestMapping("api/genetic-material")
@RequiredArgsConstructor
@Traceable
public class GeneticMaterialController {

    private final GeneticMaterialService geneticMaterialService;


    @GetMapping("/{documentId}")
    public ResponseEntity<GeneticMaterialDTO> readGeneticMaterial(
        @PathVariable Integer documentId) {
        var dto = geneticMaterialService.readGeneticMaterialById(documentId);

        return ResponseEntity.ok()
            .headers(FairSignpostingL1Utility.constructHeaders(dto, "/api/genetic-material"))
            .body(dto);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PublicationEditCheck("CREATE")
    @Idempotent
    public GeneticMaterialDTO createGeneticMaterial(
        @RequestBody @Valid GeneticMaterialDTO geneticMaterial) {
        var savedGeneticMaterial =
            geneticMaterialService.createGeneticMaterial(geneticMaterial, true);
        geneticMaterial.setId(savedGeneticMaterial.getId());
        return geneticMaterial;
    }

    @PutMapping("/{documentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PublicationEditCheck
    public void editGeneticMaterial(@PathVariable Integer documentId,
                                    @RequestBody @Valid GeneticMaterialDTO geneticMaterial) {
        geneticMaterialService.editGeneticMaterial(documentId, geneticMaterial);
    }

    @DeleteMapping("/{documentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PublicationEditCheck
    public void deleteGeneticMaterial(@PathVariable Integer documentId) {
        geneticMaterialService.deleteGeneticMaterial(documentId);
    }
}
