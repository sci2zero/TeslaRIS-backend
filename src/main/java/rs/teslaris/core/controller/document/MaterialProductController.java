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
import rs.teslaris.core.dto.document.MaterialProductDTO;
import rs.teslaris.core.service.interfaces.document.MaterialProductService;
import rs.teslaris.core.util.signposting.FairSignpostingL1Utility;

@RestController
@RequestMapping("api/material-product")
@RequiredArgsConstructor
@Traceable
public class MaterialProductController {

    private final MaterialProductService materialProductService;


    @GetMapping("/{documentId}")
    public ResponseEntity<MaterialProductDTO> readMaterialProduct(
        @PathVariable Integer documentId) {
        var dto = materialProductService.readMaterialProductById(documentId);

        return ResponseEntity.ok()
            .headers(FairSignpostingL1Utility.constructHeaders(dto, "/api/materialProduct"))
            .body(dto);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PublicationEditCheck("CREATE")
    @Idempotent
    public MaterialProductDTO createMaterialProduct(
        @RequestBody @Valid MaterialProductDTO materialProduct) {
        var savedMaterialProduct =
            materialProductService.createMaterialProduct(materialProduct, true);
        materialProduct.setId(savedMaterialProduct.getId());
        return materialProduct;
    }

    @PutMapping("/{documentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PublicationEditCheck
    public void editMaterialProduct(@PathVariable Integer documentId,
                                    @RequestBody @Valid MaterialProductDTO materialProduct) {
        materialProductService.editMaterialProduct(documentId, materialProduct);
    }

    @DeleteMapping("/{documentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PublicationEditCheck
    public void deleteMaterialProduct(@PathVariable Integer documentId) {
        materialProductService.deleteMaterialProduct(documentId);
    }
}

