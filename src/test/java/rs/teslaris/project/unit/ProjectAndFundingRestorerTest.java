package rs.teslaris.project.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import rs.teslaris.core.indexmodel.EntityType;
import rs.teslaris.project.dto.funding.FundingDTO;
import rs.teslaris.project.dto.project.ProjectDTO;
import rs.teslaris.project.service.interfaces.funding.FundingService;
import rs.teslaris.project.service.interfaces.project.ProjectService;
import rs.teslaris.project.revision.FundingRevisionRestorer;
import rs.teslaris.project.revision.ProjectRevisionRestorer;

@SpringBootTest
public class ProjectAndFundingRestorerTest {

    @Mock
    private ProjectService projectService;

    @Mock
    private FundingService fundingService;

    @InjectMocks
    private ProjectRevisionRestorer projectRevisionRestorer;

    @InjectMocks
    private FundingRevisionRestorer fundingRevisionRestorer;


    @Test
    public void shouldDescribeTheProjectEntityTypeAndItsRevisionDto() {
        assertEquals(EntityType.PROJECT.name(), projectRevisionRestorer.entityType());
        assertEquals(ProjectDTO.class, projectRevisionRestorer.dtoClass());
    }

    @Test
    public void shouldDescribeTheFundingEntityTypeAndItsRevisionDto() {
        assertEquals(EntityType.FUNDING.name(), fundingRevisionRestorer.entityType());
        assertEquals(FundingDTO.class, fundingRevisionRestorer.dtoClass());
    }

    @Test
    public void shouldRestoreAProjectThroughItsUpdateMethod() {
        // given
        var dto = new ProjectDTO();

        // when
        projectRevisionRestorer.restore(7, dto);

        // then (the restore goes through the same validation and indexing as any other edit)
        verify(projectService).updateProject(7, dto);
    }

    @Test
    public void shouldRestoreAFundingThroughItsUpdateMethod() {
        // given
        var dto = new FundingDTO();

        // when
        fundingRevisionRestorer.restore(7, dto);

        // then
        verify(fundingService).updateFunding(7, dto);
    }

    @Test
    public void shouldReadCurrentProjectStateAsTheRevisionDto() {
        // given
        var expected = new ProjectDTO();
        when(projectService.readProject(4)).thenReturn(expected);

        // when
        var currentState = projectRevisionRestorer.readCurrentState(4);

        // then
        assertEquals(expected, currentState);
    }

    @Test
    public void shouldReadCurrentFundingStateAsTheRevisionDto() {
        // given
        var expected = new FundingDTO();
        when(fundingService.readFunding(4)).thenReturn(expected);

        // when
        var currentState = fundingRevisionRestorer.readCurrentState(4);

        // then
        assertEquals(expected, currentState);
    }
}
