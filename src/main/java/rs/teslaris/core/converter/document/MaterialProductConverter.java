package rs.teslaris.core.converter.document;

import java.util.Objects;
import java.util.stream.Collectors;
import rs.teslaris.core.converter.commontypes.MultilingualContentConverter;
import rs.teslaris.core.dto.document.MaterialProductDTO;
import rs.teslaris.core.model.commontypes.BaseEntity;
import rs.teslaris.core.model.document.MaterialProduct;

public class MaterialProductConverter extends DocumentPublicationConverter {

    public static MaterialProductDTO toDTO(MaterialProduct materialProduct) {
        var materialProductDTO = new MaterialProductDTO();

        setCommonFields(materialProduct, materialProductDTO);

        materialProductDTO.setInternalNumber(materialProduct.getInternalNumber());
        materialProductDTO.setNumberProduced(materialProduct.getNumberProduced());
        materialProductDTO.setMaterialProductType(materialProduct.getMaterialProductType());

        materialProductDTO.setProductUsers(MultilingualContentConverter.getMultilingualContentDTO(
            materialProduct.getProductUsers()));
        materialProductDTO.setResearchAreasId(materialProduct.getResearchAreas().stream().map(
            BaseEntity::getId).collect(Collectors.toSet()));

        if (Objects.nonNull(materialProduct.getPublisher())) {
            materialProductDTO.setPublisherId(materialProduct.getPublisher().getId());
        } else {
            materialProductDTO.setAuthorReprint(materialProduct.getAuthorReprint());
        }

        return materialProductDTO;
    }
}
