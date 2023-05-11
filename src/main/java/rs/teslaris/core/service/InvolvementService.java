package rs.teslaris.core.service;

import org.springframework.stereotype.Service;
import rs.teslaris.core.dto.person.involvement.EducationDTO;
import rs.teslaris.core.dto.person.involvement.EmploymentDTO;
import rs.teslaris.core.dto.person.involvement.MembershipDTO;
import rs.teslaris.core.model.person.Education;
import rs.teslaris.core.model.person.Employment;
import rs.teslaris.core.model.person.Involvement;
import rs.teslaris.core.model.person.Membership;

@Service
public interface InvolvementService {

    Involvement findInvolvementById(Integer involvementId);

    Education addEducation(Integer personId, EducationDTO education);

    Membership addMembership(Integer personId, MembershipDTO membership);

    Employment addEmployment(Integer personId, EmploymentDTO employment);

    void updateEducation(Integer involvementId, EducationDTO education);

    void updateMembership(Integer involvementId, MembershipDTO membership);

    void updateEmployment(Integer involvementId, EmploymentDTO employment);

    void deleteInvolvement(Integer involvementId);
}
