package rs.teslaris.core.model.person;

public enum EmploymentPosition {
    SCIENTIFIC_ADVISOR("scientificAdvisor"),
    ASSISTANT_TRAINEE("assistantTrainee"),
    EXPERT_ASSOCIATE("expertAssociate"),
    ASSISTANT("assistant"),
    SENIOR_EXPERT_ASSOCIATE("seniorExpertAssociate"),
    INSTRUCTOR("instructor"),
    EXPERT_ADVISOR("expertAdvisor"),
    COLLABORATOR("collaborator"),
    SENIOR_INSTRUCTOR("seniorInstructor"),
    TEACHER("teacher"),
    TEACHER_OF_FOREIGN_LANGUAGES_AND_SKILLS("teacherOfForeignLanguagesAndSkills"),
    RESEARCHER("researcher"),
    PROFESSOR_ENGINEER_HABILITATED("professorEngineerHabilitated"),
    ASSISTANT_WITH_MASTER("assistantWithMaster"),
    ASSISTANT_WITH_MAGISTRATE("assistantWithMagistrate"),
    ASSISTANT_WITH_DOCTORATE("assistantWithDoctorate"),
    LECTURER("lecturer"),
    SENIOR_LECTURER("seniorLecturer"),
    PROFESSOR_OF_VOCATIONAL_STUDIES("professorOfVocationalStudies"),
    ASSISTANT_PROFESSOR("assistantProfessor"),
    ASSOCIATE_PROFESSOR("associateProfessor"),
    TEACHING_ASSISTANT("teachingAssistant"),
    FULL_PROFESSOR("fullProfessor"),
    PROFESSOR_EMERITUS("professorEmeritus"),
    RETIRED_PROFESSOR("retiredProfessor"),
    RESEARCH_TRAINEE("researchTrainee"),
    RESEARCH_ASSOCIATE("researchAssociate"),
    SCIENTIFIC_COLLABORATOR("scientificCollaborator"),
    SENIOR_SCIENTIFIC_COLLABORATOR("seniorScientificCollaborator"),
    TEACHING_ASSOCIATE("teachingAssociate");

    private final String value;

    EmploymentPosition(String value) {
        this.value = value;
    }

    public String getValue() {
        return this.value;
    }
}
