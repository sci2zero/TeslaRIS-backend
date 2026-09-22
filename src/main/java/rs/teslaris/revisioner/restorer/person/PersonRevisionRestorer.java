package rs.teslaris.revisioner.restorer.person;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import rs.teslaris.core.dto.person.PersonSnapshotDTO;
import rs.teslaris.core.dto.person.involvement.InvolvementDTO;
import rs.teslaris.core.indexmodel.EntityType;
import rs.teslaris.core.service.interfaces.person.InvolvementService;
import rs.teslaris.core.service.interfaces.person.PersonService;
import rs.teslaris.core.util.restoration.DegradationOutcome;
import rs.teslaris.core.util.restoration.RestorationContext;
import rs.teslaris.revisioner.restorer.RevisionRestorer;

/**
 * Persons have no single edit method, so the restore replays the individual updates that produce a
 * person revision, in the order the fields depend on each other.
 * <p>
 * Involvements are separate entities with their own lifecycle, so they are not replayed but
 * reconciled: the snapshot is the target state and the live collections are moved onto it.
 * Prizes and expertises/skills still have no capture of their own and are left untouched.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PersonRevisionRestorer implements RevisionRestorer<PersonSnapshotDTO> {

    private static final String EMPLOYMENT = "employment";

    private static final String EDUCATION = "education";

    private static final String MEMBERSHIP = "membership";

    private final PersonService personService;

    private final InvolvementService involvementService;


    @Override
    public String entityType() {
        return EntityType.PERSON.name();
    }

    @Override
    public Class<PersonSnapshotDTO> dtoClass() {
        return PersonSnapshotDTO.class;
    }

    @Override
    public void restore(Integer entityId, PersonSnapshotDTO dto) {
        if (Objects.nonNull(dto.getPersonalInfo())) {
            personService.updatePersonalInfo(entityId, dto.getPersonalInfo());
        }

        if (Objects.nonNull(dto.getPersonName())) {
            personService.updatePersonMainName(entityId, dto.getPersonName());
        }

        if (Objects.nonNull(dto.getPersonOtherNames())) {
            personService.setPersonOtherNames(dto.getPersonOtherNames(), entityId);
        }

        if (Objects.nonNull(dto.getBiography())) {
            personService.setPersonBiography(dto.getBiography(), entityId);
        }

        if (Objects.nonNull(dto.getKeyword())) {
            personService.setPersonKeyword(dto.getKeyword(), entityId);
        }

        reconcileInvolvements(entityId, dto);
    }

    @Override
    public Object readCurrentState(Integer entityId) {
        return personService.readPersonSnapshot(entityId);
    }

    /**
     * Moves the person's involvements onto the ones the snapshot holds: matching records are
     * updated in place, records the snapshot has and the person no longer does are recreated, and
     * records the person has and the snapshot does not are deleted.
     * <p>
     * All three lists being null means the revision predates involvement capture, and the safe
     * reading of that is "this revision says nothing about involvements" rather than "this person
     * had none" - so they are left alone. An empty list is a genuine empty.
     */
    private void reconcileInvolvements(Integer entityId, PersonSnapshotDTO dto) {
        if (Objects.isNull(dto.getEmployments()) && Objects.isNull(dto.getEducations()) &&
            Objects.isNull(dto.getMemberships())) {
            return;
        }

        var current = personService.readPersonSnapshot(entityId);

        var liveKinds = new HashMap<Integer, String>();
        indexKinds(liveKinds, current.getEmployments(), EMPLOYMENT);
        indexKinds(liveKinds, current.getEducations(), EDUCATION);
        indexKinds(liveKinds, current.getMemberships(), MEMBERSHIP);

        var claimed = new HashSet<Integer>();

        reconcile(dto.getEmployments(), EMPLOYMENT, liveKinds, claimed,
            involvementService::updateEmployment,
            employment -> involvementService.addEmployment(entityId, employment));

        reconcile(dto.getEducations(), EDUCATION, liveKinds, claimed,
            involvementService::updateEducation,
            education -> involvementService.addEducation(entityId, education));

        reconcile(dto.getMemberships(), MEMBERSHIP, liveKinds, claimed,
            involvementService::updateMembership,
            membership -> involvementService.addMembership(entityId, membership));

        removeInvolvementsAddedSinceCapture(entityId, liveKinds, claimed);
    }

    private <T extends InvolvementDTO> void reconcile(List<T> target, String kind,
                                                      Map<Integer, String> liveKinds,
                                                      Set<Integer> claimed,
                                                      BiConsumer<Integer, T> update,
                                                      Consumer<T> add) {
        if (Objects.isNull(target)) {
            return;
        }

        target.forEach(involvement -> {
            var involvementId = involvement.getId();

            if (Objects.nonNull(involvementId) && kind.equals(liveKinds.get(involvementId))) {
                claimed.add(involvementId);
                update.accept(involvementId, involvement);
                return;
            }

            // Deleted since the snapshot was taken, or that ID now holds a different kind of
            // involvement. Either way it can only come back as a new record with a new ID, which
            // is a restore that did not fully reproduce what was asked for.
            if (Objects.nonNull(involvementId)) {
                RestorationContext.report("restoreInvolvementRecreatedMessage", kind,
                    DegradationOutcome.DEGRADED, List.of(String.valueOf(involvementId)));

                log.info("Restoration: involvement '{}' (ID={}) is gone, recreating it.",
                    kind, involvementId);
            }

            add.accept(involvement);
        });
    }

    private void removeInvolvementsAddedSinceCapture(Integer entityId,
                                                     Map<Integer, String> liveKinds,
                                                     Set<Integer> claimed) {
        var obsolete = new ArrayList<>(liveKinds.keySet());
        obsolete.removeAll(claimed);
        obsolete.sort(Integer::compareTo);

        obsolete.forEach(involvementId -> {
            log.info("Restoration: deleting involvement (ID={}) of PERSON with ID {}, it did not " +
                "exist when the revision was captured.", involvementId, entityId);

            involvementService.deleteInvolvement(involvementId);
        });
    }

    private void indexKinds(Map<Integer, String> kinds, List<? extends InvolvementDTO> involvements,
                            String kind) {
        if (Objects.isNull(involvements)) {
            return;
        }

        involvements.stream()
            .map(InvolvementDTO::getId)
            .filter(Objects::nonNull)
            .forEach(involvementId -> kinds.put(involvementId, kind));
    }
}
