package rs.teslaris.core.service.interfaces.person;

import java.util.List;
import org.springframework.stereotype.Service;
import rs.teslaris.core.dto.document.DocumentFileDTO;
import rs.teslaris.core.dto.document.DocumentFileResponseDTO;
import rs.teslaris.core.dto.person.involvement.EducationDTO;
import rs.teslaris.core.dto.person.involvement.EmploymentDTO;
import rs.teslaris.core.dto.person.involvement.MembershipDTO;
import rs.teslaris.core.model.person.Education;
import rs.teslaris.core.model.person.Employment;
import rs.teslaris.core.model.person.Involvement;
import rs.teslaris.core.model.person.Membership;
import rs.teslaris.core.service.interfaces.JPAService;

@Service
public interface InvolvementService extends JPAService<Involvement> {

    Involvement findInvolvementById(Integer involvementId);

    <T extends Involvement, R> R getInvolvement(Integer involvementId, Class<T> involvementClass);

    Education addEducation(Integer personId, EducationDTO education);

    Membership addMembership(Integer personId, MembershipDTO membership);

    Employment addEmployment(Integer personId, EmploymentDTO employment);

    List<EmploymentDTO> getEmploymentsForPerson(Integer personId);

    DocumentFileResponseDTO addInvolvementProof(DocumentFileDTO proof, Integer involvementId);

    void deleteProof(Integer proofId, Integer involvementId);

    DocumentFileResponseDTO updateProof(Integer proofId, Integer involvementId,
                                        DocumentFileDTO updatedProof);

    void updateEducation(Integer involvementId, EducationDTO education);

    void updateMembership(Integer involvementId, MembershipDTO membership);

    void updateEmployment(Integer involvementId, EmploymentDTO employment);

    void deleteInvolvement(Integer involvementId);
}
