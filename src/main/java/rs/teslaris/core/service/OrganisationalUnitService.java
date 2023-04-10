package rs.teslaris.core.service;

import org.springframework.stereotype.Service;
import rs.teslaris.core.model.OrganisationalUnit;

@Service
public interface OrganisationalUnitService {

    OrganisationalUnit findOrganisationalUnitById(Integer id);
}
