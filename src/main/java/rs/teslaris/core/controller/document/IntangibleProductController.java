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
import rs.teslaris.core.dto.document.IntangibleProductDTO;
import rs.teslaris.core.service.interfaces.document.IntangibleProductService;
import rs.teslaris.core.util.signposting.FairSignpostingL1Utility;

@RestController
@RequestMapping("api/intangible-product")
@RequiredArgsConstructor
@Traceable
public class IntangibleProductController {

    private final IntangibleProductService intangibleProductService;


    @GetMapping("/{documentId}")
    public ResponseEntity<IntangibleProductDTO> readIntangibleProduct(
        @PathVariable Integer documentId) {
        var dto = intangibleProductService.readIntangibleProductById(documentId);

        return ResponseEntity.ok()
            .headers(FairSignpostingL1Utility.constructHeaders(dto, "/api/intangibleProduct"))
            .body(dto);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PublicationEditCheck("CREATE")
    @Idempotent
    public IntangibleProductDTO createIntangibleProduct(
        @RequestBody @Valid IntangibleProductDTO intangibleProduct) {
        var savedIntangibleProduct =
            intangibleProductService.createIntangibleProduct(intangibleProduct, true);
        intangibleProduct.setId(savedIntangibleProduct.getId());
        return intangibleProduct;
    }

    @PutMapping("/{documentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PublicationEditCheck
    public void editIntangibleProduct(@PathVariable Integer documentId,
                                      @RequestBody @Valid IntangibleProductDTO intangibleProduct) {
        intangibleProductService.editIntangibleProduct(documentId, intangibleProduct);
    }

    @DeleteMapping("/{documentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PublicationEditCheck
    public void deleteIntangibleProduct(@PathVariable Integer documentId) {
        intangibleProductService.deleteIntangibleProduct(documentId);
    }
}
