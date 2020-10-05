package com.hartwig.hmftools.serve.actionability.range;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.List;

import com.hartwig.hmftools.serve.actionability.ActionabilityTestUtil;

import org.junit.Test;

public class RangeEvidenceAnalyzerFactoryTest {

    @Test
    public void canLoadActionableRanges() throws IOException {
        String actionableRangeTsv =
                RangeEvidenceAnalyzerFactory.actionableRangeTsvFilePath(ActionabilityTestUtil.SERVE_ACTIONABILITY_DIR);
        List<ActionableRange> actionableRanges = RangeEvidenceAnalyzerFactory.loadFromActionableRangeTsv(actionableRangeTsv);

        assertEquals(2, actionableRanges.size());
    }
}