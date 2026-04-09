package rs.teslaris.core.unit.project;

import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import rs.teslaris.core.integration.BaseTest;
import rs.teslaris.project.model.funding.Funding;
import rs.teslaris.project.model.project.Project;
import rs.teslaris.project.repository.funding.FundingRepository;
import rs.teslaris.project.service.impl.funding.FundingServiceImpl;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
public class FundingServiceTest extends BaseTest {

    @Mock
    private FundingRepository fundingRepository;

    @InjectMocks
    private FundingServiceImpl fundingService;


    @Test
    public void shouldReturnFundingDTOWhenFundingExists() {
        // given
        var fundingId = 1;
        var funding = new Funding();
        funding.setId(fundingId);
        funding.setProject(new Project());

        when(fundingRepository.findById(fundingId))
                .thenReturn(Optional.of(funding));

        // when
        var result = fundingService.readFunding(fundingId);

        // then
        assertNotNull(result);
        assertEquals(fundingId, result.getId());
        verify(fundingRepository).findById(any());
    }

    @Test
    public void shouldThrowExceptionWhenFundingNotFound() {
        // given
        var fundingId = 999;

        when(fundingRepository.findById(fundingId))
                .thenReturn(Optional.empty());

        // when & then
        assertThrows(Exception.class, () ->
                fundingService.readFunding(fundingId));
        verify(fundingRepository).findById(fundingId);
    }
}
