package com.hartwig.hmftools.linx.visualiser.circos;

import java.io.File;
import java.io.IOException;
import java.util.List;

import com.google.common.collect.Lists;
import com.hartwig.hmftools.common.genome.position.GenomePosition;
import com.hartwig.hmftools.common.genome.position.GenomePositions;
import com.hartwig.hmftools.linx.visualiser.data.FusedExon;
import com.hartwig.hmftools.linx.visualiser.data.FusedExons;
import com.hartwig.hmftools.linx.visualiser.data.FusedProteinDomains;
import com.hartwig.hmftools.linx.visualiser.data.VisProteinDomains;
import com.hartwig.hmftools.linx.visualiser.file.VisFusion;
import com.hartwig.hmftools.linx.visualiser.file.VisGeneExon;
import com.hartwig.hmftools.linx.visualiser.file.VisProteinDomain;

import org.jetbrains.annotations.NotNull;

public class FusionDataWriter
{
    private final List<FusedExon> mFinalExons;
    private final List<VisProteinDomain> mFinalProteinDomains;
    private final ProteinDomainColors mProteinDomainColors;

    public FusionDataWriter(final List<VisFusion> fusions, final List<VisGeneExon> exons, final List<VisProteinDomain> proteinDomains)
    {
        mFinalExons = Lists.newArrayList();
        mFinalProteinDomains = Lists.newArrayList();

        final List<VisProteinDomain> exonicProteinDomains = VisProteinDomains.exonicProteinDomains(proteinDomains, exons);

        for(VisFusion fusion : fusions)
        {
            final List<FusedExon> fusedExons = FusedExons.fusedExons(fusion, exons);
            final List<VisProteinDomain> fusedProteinDomain = FusedProteinDomains.fusedProteinDomains(fusion, fusedExons, exonicProteinDomains);

            final ScaleIntrons scaler = new ScaleIntrons(ScaleIntrons.introns(fusedExons));
            final List<FusedExon> intronScaledExons = scaler.scaleIntronsInExons(fusedExons);
            final List<VisProteinDomain> intronScaledProteinDomains = scaler.scaleIntronsInProteinDomains(fusedProteinDomain);

            final List<GenomePosition> unadjustedPositions = Lists.newArrayList();
            for(FusedExon fusedExon : intronScaledExons)
            {
                unadjustedPositions.add(GenomePositions.create(fusedExon.fusion(), fusedExon.geneStart()));
                unadjustedPositions.add(GenomePositions.create(fusedExon.fusion(), fusedExon.geneEnd()));
                unadjustedPositions.add(GenomePositions.create(fusedExon.fusion(), fusedExon.start()));
                unadjustedPositions.add(GenomePositions.create(fusedExon.fusion(), fusedExon.end()));
            }

            for(VisProteinDomain proteinDomain : intronScaledProteinDomains)
            {
                unadjustedPositions.add(GenomePositions.create(proteinDomain.chromosome(), proteinDomain.start()));
                unadjustedPositions.add(GenomePositions.create(proteinDomain.chromosome(), proteinDomain.end()));
            }

            final ScalePosition scalePosition = new ScalePosition(unadjustedPositions);
            mFinalExons.addAll(scalePosition.scaleFusedExon(intronScaledExons));
            mFinalProteinDomains.addAll(scalePosition.interpolateProteinDomains(intronScaledProteinDomains));
        }

        this.mProteinDomainColors = new ProteinDomainColors(mFinalProteinDomains);
    }

    @NotNull
    public Object write(final String sample, final String outputDir)
            throws IOException
    {
        String filePrefix = outputDir + File.separator + sample;
        FusedExons.write(filePrefix + ".fusions.tsv", mFinalExons);
        FusedProteinDomains.write(filePrefix + ".protein_domains.tsv", mProteinDomainColors, mFinalProteinDomains);
        return this;
    }

    public List<FusedExon> finalExons()
    {
        return mFinalExons;
    }

}
