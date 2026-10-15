package rs.teslaris.migrator.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import rs.teslaris.migrator.converter.hydrator.HydratorMappingRule;

public class HydratorMappingRuleTest {

    @Test
    public void shouldPrefixCurriculumRulesWithTheirCrosswalk() {
        assertEquals("CW-001-MAP-000040", HydratorMappingRule.cv(40));
        assertEquals("CW-001-MAP-000212", HydratorMappingRule.cv(212));
    }

    @Test
    public void shouldPrefixSciProjRulesWithTheirCrosswalk() {
        assertEquals("CW-002-MAP-000028", HydratorMappingRule.sciproj(28));
    }

    @Test
    public void shouldJoinSeveralRulesUnderOnePrefix() {
        assertEquals("CW-002-MAP-000016/000035", HydratorMappingRule.sciproj(16, 35));
    }
}
