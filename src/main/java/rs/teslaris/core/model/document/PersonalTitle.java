package rs.teslaris.core.model.document;

import lombok.Getter;

@Getter
public enum PersonalTitle {
    PHD("др"),
    ACADEMIC("академик"),
    PHD_ART("др ум."),
    MR("мр"),
    NONE("");


    private final String value;

    PersonalTitle(String value) {
        this.value = value;
    }
}
