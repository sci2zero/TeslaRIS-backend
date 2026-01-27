package rs.teslaris.core.service.impl.document.cruddelegate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import rs.teslaris.core.model.document.GeneticMaterial;
import rs.teslaris.core.repository.document.GeneticMaterialRepository;
import rs.teslaris.core.service.impl.JPAServiceImpl;

@Component
public class GeneticMaterialJPAServiceImpl extends JPAServiceImpl<GeneticMaterial> {

    private final GeneticMaterialRepository geneticMaterialRepository;

    @Autowired
    public GeneticMaterialJPAServiceImpl(GeneticMaterialRepository geneticMaterialRepository) {
        this.geneticMaterialRepository = geneticMaterialRepository;
    }

    @Override
    protected JpaRepository<GeneticMaterial, Integer> getEntityRepository() {
        return geneticMaterialRepository;
    }
}
