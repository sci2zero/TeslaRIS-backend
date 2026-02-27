package rs.teslaris.core.repository.project;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.model.project.FundingCall;

@Repository
public interface FundingCallRepository extends JpaRepository<FundingCall, Integer> {

    @Query("SELECT COUNT(f) > 0 FROM Funding f " +
        "WHERE f.fundingCall.id = :fundingCallId")
    boolean hasFunding(Integer fundingCallId);

    @Query("SELECT COUNT(fp) > 0 FROM FundingProposal fp " +
        "WHERE fp.fundingCall.id = :fundingCallId")
    boolean hasFundingProposals(Integer fundingCallId);
}
