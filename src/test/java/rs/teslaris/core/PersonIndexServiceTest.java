package rs.teslaris.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Query;
import rs.teslaris.core.indexmodel.PersonIndex;
import rs.teslaris.core.indexrepository.PersonIndexRepository;
import rs.teslaris.core.service.impl.PersonIndexServiceImpl;

@SpringBootTest
public class PersonIndexServiceTest {

    @InjectMocks
    PersonIndexServiceImpl personIndexService;
    @Mock
    private PersonIndexRepository personIndexRepository;
    @Mock
    private ElasticsearchOperations template;

    @Test
    void shouldFindPeopleByNameAndEmploymentWhenProperQueryIsGiven() throws Exception {
        // given
        var tokens = Arrays.asList("Ivan", "FTN");
        var pageable = PageRequest.of(0, 10);

        var searchHits = mock(SearchHits.class);
        when(searchHits.getTotalHits()).thenReturn(2L);

        when(template.search((Query) any(), any(), any())).thenReturn(searchHits);

        // when
        var result =
            personIndexService.findPeopleByNameAndEmployment(tokens, pageable);

        // then
        assertEquals(result.getTotalElements(), 2L);
    }

    @Test
    void shouldFindPeopleForOrganisationUnitWhenGivenValidId() {
        // given
        var employmentInstitutionId = 123;
        var pageable = PageRequest.of(0, 10);

        when(personIndexRepository.findByEmploymentInstitutionsIdIn(pageable,
            List.of(employmentInstitutionId))).thenReturn(
            new PageImpl<>(List.of(new PersonIndex())));

        // when
        var result =
            personIndexService.findPeopleForOrganisationUnit(employmentInstitutionId, pageable);

        // then
        assertEquals(result.getTotalElements(), 1L);
    }

}
