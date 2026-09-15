package rs.teslaris.core.unit.revisioner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import rs.teslaris.core.dto.person.PersonSnapshotDTO;
import rs.teslaris.core.dto.person.involvement.EducationDTO;
import rs.teslaris.core.dto.person.involvement.EmploymentDTO;
import rs.teslaris.core.dto.person.involvement.MembershipDTO;
import rs.teslaris.core.service.interfaces.person.InvolvementService;
import rs.teslaris.core.service.interfaces.person.PersonService;
import rs.teslaris.core.util.restoration.RestorationContext;
import rs.teslaris.revisioner.restorer.person.PersonRevisionRestorer;

@SpringBootTest
public class PersonRevisionRestorerTest {

    @Mock
    private PersonService personService;

    @Mock
    private InvolvementService involvementService;

    @InjectMocks
    private PersonRevisionRestorer personRevisionRestorer;


    private static EmploymentDTO employment(Integer id) {
        var employment = new EmploymentDTO();
        employment.setId(id);

        return employment;
    }

    private static EducationDTO education(Integer id) {
        var education = new EducationDTO();
        education.setId(id);

        return education;
    }

    private static MembershipDTO membership(Integer id) {
        var membership = new MembershipDTO();
        membership.setId(id);

        return membership;
    }

    private static PersonSnapshotDTO snapshot(List<EmploymentDTO> employments,
                                              List<EducationDTO> educations,
                                              List<MembershipDTO> memberships) {
        var snapshot = new PersonSnapshotDTO();
        snapshot.setEmployments(employments);
        snapshot.setEducations(educations);
        snapshot.setMemberships(memberships);

        return snapshot;
    }

    @Test
    public void shouldLeaveInvolvementsAloneWhenRevisionPredatesTheirCapture() {
        // given
        var revision = new PersonSnapshotDTO();

        // when
        personRevisionRestorer.restore(1, revision);

        // then
        verifyNoInteractions(involvementService);
    }

    @Test
    public void shouldUpdateInvolvementsThatStillExist() {
        // given
        var target = employment(7);
        when(personService.readPersonSnapshot(1))
            .thenReturn(snapshot(List.of(employment(7)), List.of(), List.of()));

        // when
        personRevisionRestorer.restore(1, snapshot(List.of(target), List.of(), List.of()));

        // then
        verify(involvementService).updateEmployment(7, target);
        verify(involvementService, never()).addEmployment(any(), any());
        verify(involvementService, never()).deleteInvolvement(any());
    }

    @Test
    public void shouldRecreateInvolvementsDeletedSinceTheRevisionWasCaptured() {
        // given
        var target = education(9);
        when(personService.readPersonSnapshot(1))
            .thenReturn(snapshot(List.of(), List.of(), List.of()));

        // when
        var degradedReferences = RestorationContext.collectDuring(() -> {
            personRevisionRestorer.restore(1, snapshot(List.of(), List.of(target), List.of()));
            return null;
        });

        // then
        verify(involvementService).addEducation(1, target);
        verify(involvementService, never()).updateEducation(any(), any());

        assertEquals(1, degradedReferences.size());
        assertEquals("restoreInvolvementRecreatedMessage",
            degradedReferences.getFirst().getMessageKey());
        assertEquals(List.of("9"), degradedReferences.getFirst().getParameters());
    }

    @Test
    public void shouldAddInvolvementsThatNeverHadAnIdWithoutReportingDegradation() {
        // given
        var target = membership(null);
        when(personService.readPersonSnapshot(1))
            .thenReturn(snapshot(List.of(), List.of(), List.of()));

        // when
        var degradedReferences = RestorationContext.collectDuring(() -> {
            personRevisionRestorer.restore(1, snapshot(List.of(), List.of(), List.of(target)));
            return null;
        });

        // then
        verify(involvementService).addMembership(1, target);
        assertTrue(degradedReferences.isEmpty());
    }

    @Test
    public void shouldDeleteInvolvementsAddedAfterTheRevisionWasCaptured() {
        // given
        when(personService.readPersonSnapshot(1)).thenReturn(
            snapshot(List.of(employment(3)), List.of(education(4)), List.of(membership(5))));

        // when
        personRevisionRestorer.restore(1, snapshot(List.of(), List.of(), List.of()));

        // then
        verify(involvementService).deleteInvolvement(3);
        verify(involvementService).deleteInvolvement(4);
        verify(involvementService).deleteInvolvement(5);
    }

    /**
     * The ID is live but now belongs to another kind of involvement, so it cannot be updated in
     * place - the snapshot's record is recreated and the one occupying the ID is removed.
     */
    @Test
    public void shouldRecreateRatherThanUpdateWhenTheIdNowHoldsAnotherKindOfInvolvement() {
        // given
        var target = education(11);
        when(personService.readPersonSnapshot(1))
            .thenReturn(snapshot(List.of(employment(11)), List.of(), List.of()));

        // when
        personRevisionRestorer.restore(1, snapshot(List.of(), List.of(target), List.of()));

        // then
        verify(involvementService).addEducation(1, target);
        verify(involvementService, never()).updateEducation(any(), any());
        verify(involvementService).deleteInvolvement(11);
    }

    @Test
    public void shouldReconcileEveryInvolvementKindInOnePass() {
        // given
        var keptEmployment = employment(1);
        var recreatedEducation = education(2);
        var newMembership = membership(null);

        when(personService.readPersonSnapshot(5)).thenReturn(
            snapshot(List.of(employment(1)), List.of(), List.of(membership(8))));

        // when
        personRevisionRestorer.restore(5, snapshot(List.of(keptEmployment),
            List.of(recreatedEducation), List.of(newMembership)));

        // then
        verify(involvementService).updateEmployment(1, keptEmployment);
        verify(involvementService).addEducation(5, recreatedEducation);
        verify(involvementService).addMembership(5, newMembership);
        verify(involvementService).deleteInvolvement(8);
    }

    @Test
    public void shouldReadCurrentStateAsAPersonSnapshot() {
        // given
        var expected = new PersonSnapshotDTO();
        when(personService.readPersonSnapshot(4)).thenReturn(expected);

        // when
        var currentState = personRevisionRestorer.readCurrentState(4);

        // then
        assertEquals(expected, currentState);
        verify(personService).readPersonSnapshot(eq(4));
    }
}
