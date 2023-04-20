package rs.teslaris.core.service;

import org.springframework.stereotype.Service;
import rs.teslaris.core.model.institution.OrganisationUnit;

@Service
public interface OrganisationalUnitService {

    OrganisationUnit findOrganisationalUnitById(Integer id);
}
