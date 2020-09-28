package com.hartwig.hmftools.serve.vicc.signatures;

import java.util.Map;

import com.google.common.collect.Maps;
import com.hartwig.hmftools.vicc.datamodel.Feature;
import com.hartwig.hmftools.vicc.datamodel.ViccEntry;
import com.hartwig.hmftools.vicc.util.EventAnnotation;
import com.hartwig.hmftools.vicc.util.EventAnnotationExtractor;

import org.jetbrains.annotations.NotNull;

public class SignaturesExtractor {

    @NotNull
    public Map<Feature, String> extractSignatures(@NotNull ViccEntry viccEntry) {
        Map<Feature, String> signaturesPerFeature = Maps.newHashMap();
        for (Feature feature : viccEntry.features()) {
            EventAnnotation eventAnnotation = EventAnnotationExtractor.toEventAnnotation(feature);
            if (eventAnnotation == EventAnnotation.SIGNATURE) {
                signaturesPerFeature.put(feature, feature.name());
            }
        }
        return signaturesPerFeature;
    }
}
