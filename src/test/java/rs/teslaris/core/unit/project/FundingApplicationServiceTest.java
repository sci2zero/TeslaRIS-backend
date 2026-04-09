package rs.teslaris.core.unit.project;

import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import rs.teslaris.project.model.funding.FundingApplication;
import rs.teslaris.project.repository.funding.FundingApplicationRepository;
import rs.teslaris.project.service.impl.funding.FundingApplicationServiceImpl;

import java.util.HashSet;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
public class FundingApplicationServiceTest {
    @Mock
    private FundingApplicationRepository fundingApplicationRepository;

    @InjectMocks
    private FundingApplicationServiceImpl fundingApplicationService;

    @Test
    public void shouldReturnFundingApplicationDTOWhenFundingApplicationExists() {
        // given
        var fundingApplicationId = 1;
        var fundingApplication = new FundingApplication();
        fundingApplication.setId(fundingApplicationId);
        fundingApplication.setOtherFundingSources(new HashSet<>());

        when(fundingApplicationRepository.findById(fundingApplicationId))
                .thenReturn(Optional.of(fundingApplication));

        // when
        var result = fundingApplicationService.readFundingApplication(fundingApplicationId);

        // then
        assertNotNull(result);
        assertEquals(fundingApplicationId, result.getId());
        verify(fundingApplicationRepository).findById(any());
    }

    @Test
    public void shouldThrowExceptionWhenFundingApplicationNotFound() {
        // given
        var fundingApplicationId = 999;

        when(fundingApplicationRepository.findById(fundingApplicationId))
                .thenReturn(Optional.empty());

        // when & then
        assertThrows(Exception.class, () ->
                fundingApplicationService.readFundingApplication(fundingApplicationId));
        verify(fundingApplicationRepository).findById(fundingApplicationId);
    }
}
