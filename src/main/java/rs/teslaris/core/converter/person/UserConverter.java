package rs.teslaris.core.converter.person;

import java.util.HashSet;
import java.util.Objects;
import rs.teslaris.core.converter.commontypes.MultilingualContentConverter;
import rs.teslaris.core.dto.user.UserResponseDTO;
import rs.teslaris.core.model.commontypes.MultiLingualContent;
import rs.teslaris.core.model.user.User;

public class UserConverter {

    public static UserResponseDTO toUserResponseDTO(User user) {
        var organisationUnitId =
            Objects.nonNull(user.getOrganisationUnit()) ? user.getOrganisationUnit().getId() : -1;

        var personId =
            Objects.nonNull(user.getPerson()) ? user.getPerson().getId() : -1;

        var organisationUnitName =
            Objects.nonNull(user.getOrganisationUnit()) ? user.getOrganisationUnit().getName() :
                new HashSet<MultiLingualContent>();

        return new UserResponseDTO(user.getId(), user.getEmail(),
            user.getFirstname(),
            user.getLastName(), user.getLocked(), user.getCanTakeRole(),
            user.getPreferredLanguage().getLanguageCode(),
            organisationUnitId,
            personId,
            MultilingualContentConverter.getMultilingualContentDTO(organisationUnitName),
            user.getUserNotificationPeriod());
    }
}
