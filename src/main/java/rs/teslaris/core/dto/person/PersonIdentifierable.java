package rs.teslaris.core.dto.person;

public interface PersonIdentifierable {

    String getApvnt();

    String getECrisId();

    String getENaukaId();

    String getOrcid();

    void setOrcid(String orcid);

    String getScopusAuthorId();

    String getOpenAlexId();

    String getWebOfScienceId();
}
