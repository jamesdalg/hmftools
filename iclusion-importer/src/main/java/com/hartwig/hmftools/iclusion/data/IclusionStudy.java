package com.hartwig.hmftools.iclusion.data;

import java.util.List;

import com.squareup.moshi.Json;

public class IclusionStudy {
    @Json(name = "id") public String id;
    @Json(name = "title") public String title;
    @Json(name = "acronym") public String acronym;
    @Json(name = "eudra") public String eudra;
    @Json(name = "nct") public String nct;
    @Json(name = "ipn") public String ipn;
    @Json(name = "ccmo") public String ccmo;
    @Json(name = "indications_id") public List<String> indication_ids;
    @Json(name = "mutations") public List<IclusionMutation> mutations;
}
