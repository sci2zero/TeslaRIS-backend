package rs.teslaris.core.unit.project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import rs.teslaris.core.dto.commontypes.MonetaryAmountDTO;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;
import rs.teslaris.project.dto.funding.FundingPartDTO;
import rs.teslaris.project.model.funding.FundingPart;
import rs.teslaris.project.repository.funding.FundingPartRepository;
import rs.teslaris.project.service.impl.funding.FundingPartServiceImpl;
import rs.teslaris.project.util.FundingPartFactory;

@SpringBootTest
public class FundingPartServiceTest {

    @Mock
    private FundingPartRepository fundingPartRepository;

    @Mock
    private FundingPartFactory fundingPartFactory;

    @InjectMocks
    private FundingPartServiceImpl fundingPartService;

    private FundingPartDTO fundingPartDTO;


    @BeforeEach
    void setUp() {
        var monetaryAmountDTO = new MonetaryAmountDTO();
        monetaryAmountDTO.setCurrencyId(1);
        monetaryAmountDTO.setAmount(100);

        fundingPartDTO = new FundingPartDTO();
        fundingPartDTO.setFundingId(1);
        fundingPartDTO.setDescription(List.of());
        fundingPartDTO.setAmount(monetaryAmountDTO);
        fundingPartDTO.setProjectEventId(1);
    }

    @Test
    public void shouldCreateFundingPartSuccessfully() {
        // given
        var builtFundingPart = new FundingPart();

        when(fundingPartFactory.buildFundingPart(fundingPartDTO)).thenReturn(builtFundingPart);
        when(fundingPartRepository.save(any(FundingPart.class))).thenAnswer(
            i -> i.getArguments()[0]);

        // when
        var result = fundingPartService.createFundingPart(fundingPartDTO);

        // then
        verify(fundingPartFactory).buildFundingPart(fundingPartDTO);
        verify(fundingPartRepository).save(builtFundingPart);

        assertThat(result).isEqualTo(builtFundingPart);
    }

    @Test
    public void shouldUpdateFundingPartSuccessfully() {
        // given
        var fundingPartId = 1;
        var existingFundingPart = new FundingPart();
        existingFundingPart.setId(fundingPartId);

        when(fundingPartRepository.findById(fundingPartId)).thenReturn(
            Optional.of(existingFundingPart));

        // when
        fundingPartService.updateFundingPart(fundingPartId, fundingPartDTO);

        // then
        verify(fundingPartRepository).findById(fundingPartId);
        verify(fundingPartFactory).setCommonFields(existingFundingPart, fundingPartDTO);
        verify(fundingPartRepository).save(existingFundingPart);
    }

    @Test
    public void shouldDeleteFundingPartSuccessfully() {
        // given
        var fundingPartId = 1;
        var fundingPart = new FundingPart();
        fundingPart.setId(fundingPartId);

        when(fundingPartRepository.findById(fundingPartId)).thenReturn(Optional.of(fundingPart));
        doNothing().when(fundingPartRepository).delete(fundingPart);

        // when
        fundingPartService.deleteFundingPart(fundingPartId);

        // then
        verify(fundingPartRepository).findById(fundingPartId);
        verify(fundingPartRepository).save(fundingPart);
    }

    @Test
    public void shouldThrowExceptionWhenDeletingNonExistentFundingPart() {
        // given
        var fundingPartId = 999;

        when(fundingPartRepository.findById(fundingPartId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> fundingPartService.deleteFundingPart(fundingPartId))
            .isInstanceOf(NotFoundException.class);

        verify(fundingPartRepository).findById(fundingPartId);
        verify(fundingPartRepository, never()).delete(any(FundingPart.class));
    }

    @Test
    public void shouldThrowExceptionWhenUpdatingNonExistentFundingPart() {
        // given
        var fundingPartId = 999;

        when(fundingPartRepository.findById(fundingPartId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(
            () -> fundingPartService.updateFundingPart(fundingPartId, fundingPartDTO))
            .isInstanceOf(NotFoundException.class);

        verify(fundingPartRepository).findById(fundingPartId);
        verify(fundingPartRepository, never()).save(any(FundingPart.class));
    }
}
