package com.hartwig.hmftools.orange.algo.selection;

import java.util.List;

import com.hartwig.hmftools.common.protect.ProtectEventGenerator;
import com.hartwig.hmftools.common.protect.ProtectEvidence;
import com.hartwig.hmftools.common.sv.linx.LinxFusion;

import org.apache.commons.compress.utils.Lists;
import org.jetbrains.annotations.NotNull;

public final class FusionSelector {

    private FusionSelector() {
    }

    @NotNull
    public static List<LinxFusion> selectNonDriverFusions(@NotNull List<LinxFusion> fusions, @NotNull List<ProtectEvidence> evidences) {
        List<LinxFusion> filtered = Lists.newArrayList();
        for (LinxFusion fusion : fusions) {
            String event = ProtectEventGenerator.fusionEvent(fusion);
            if (!fusion.reportedType().equals("NONE") || EvidenceSelector.hasEvidence(evidences, null, event)) {
                filtered.add(fusion);
            }
        }
        return filtered;
    }
}
