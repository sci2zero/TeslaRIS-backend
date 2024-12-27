package rs.teslaris.core.repository.commontypes;

import java.util.Optional;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.model.commontypes.Country;

@Repository
public interface CountryRepository extends JpaRepository<Country, Integer> {

    @Modifying
    @Query("UPDATE Person p SET p.personalInfo.postalAddress.country = null WHERE p.personalInfo.postalAddress.country.id = :countryId")
    void unsetCountryForPersons(Integer countryId);

    @Query("select c from Country c join c.name n where LOWER(n.content) = LOWER(:name)")
    Optional<Country> findCountryByName(String name, Limit limit);

    @Query(value =
        "SELECT c FROM Country c LEFT JOIN c.name name WHERE name.language.languageTag = :languageTag AND " +
            "LOWER(name.content) LIKE LOWER(CONCAT('%', :searchExpression, '%'))",
        countQuery =
            "SELECT count(DISTINCT c) FROM Country c JOIN c.name n WHERE n.language.languageTag = :languageTag AND " +
                "LOWER(n.content) LIKE LOWER(CONCAT('%', :searchExpression, '%'))")
    Page<Country> searchCountries(String searchExpression, String languageTag, Pageable pageable);
}
