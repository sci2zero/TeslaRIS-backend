package rs.teslaris.core.model.document;

import lombok.Getter;

@Getter
public enum EmploymentTitle {
    FULL_PROFESSOR("ред. проф."),
    ASSISTANT_PROFESSOR("доцент"),
    ASSOCIATE_PROFESSOR("ванр. проф."),
    PROFESSOR_EMERITUS("проф. емеритус"),
    SCIENTIFIC_COLLABORATOR("науч. сар."),
    SENIOR_SCIENTIFIC_COLLABORATOR("виши науч. сар."),
    SCIENTIFIC_ADVISOR("науч. сав."),
    RETIRED_PROFESSOR("проф. у пензији"),
    PROFESSOR_ENGINEER_HABILITATED("проф. инж. хабил.");


    private final String value;

    EmploymentTitle(String value) {
        this.value = value;
    }
}
