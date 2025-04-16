package rs.teslaris.thesislibrary.model;

import lombok.Getter;

@Getter
public enum AcademicTitle {
    BASIC_ACADEMIC_STUDIES("Основне академске студије - ОАС"),
    MASTER_ACADEMIC_STUDIES("Мастер академске студије - МАС"),
    INTEGRATED_ACADEMIC_STUDIES("Интегрисане академске студије - ИАС"),
    SPECIALIZED_ACADEMIC_STUDIES("Специјалистичке академске студије - САС"),
    MAGISTER_STUDIES("Магистарске студије - МС");

    private final String value;

    AcademicTitle(String value) {
        this.value = value;
    }
}
