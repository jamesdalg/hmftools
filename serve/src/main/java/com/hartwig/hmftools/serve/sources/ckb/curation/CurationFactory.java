package com.hartwig.hmftools.serve.sources.ckb.curation;

import java.util.Map;

import com.google.common.collect.Maps;
import com.hartwig.hmftools.ckb.classification.CkbConstants;

public final class CurationFactory {

    static final Map<CurationEntry, CurationEntry> VARIANT_MAPPINGS = Maps.newHashMap();

    private CurationFactory() {
    }

    static {
        // CKB uses "genes" to model evidence on characteristics. We map this away from genes.
        VARIANT_MAPPINGS.put(new CurationEntry("HRD", "positive"), new CurationEntry(CkbConstants.NO_GENE, CkbConstants.HRD_POSITIVE));
        VARIANT_MAPPINGS.put(new CurationEntry("HRD", "negative"), new CurationEntry(CkbConstants.NO_GENE, CkbConstants.HRD_NEGATIVE));
        VARIANT_MAPPINGS.put(new CurationEntry("MSI", "high"), new CurationEntry(CkbConstants.NO_GENE, CkbConstants.MSI_HIGH));
        VARIANT_MAPPINGS.put(new CurationEntry("MSI", "low"), new CurationEntry(CkbConstants.NO_GENE, CkbConstants.MSI_LOW));
        VARIANT_MAPPINGS.put(new CurationEntry("MSI", "negative"), new CurationEntry(CkbConstants.NO_GENE, CkbConstants.MSI_NEGATIVE));
        VARIANT_MAPPINGS.put(new CurationEntry("TMB", "high"), new CurationEntry(CkbConstants.NO_GENE, CkbConstants.TMB_HIGH));
        VARIANT_MAPPINGS.put(new CurationEntry("TMB", "low"), new CurationEntry(CkbConstants.NO_GENE, CkbConstants.TMB_LOW));
    }
}
