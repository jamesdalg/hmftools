package com.hartwig.hmftools.patientreporter.cfreport.chapters.panel;

import com.hartwig.hmftools.patientreporter.cfreport.ReportResources;
import com.hartwig.hmftools.patientreporter.cfreport.chapters.ReportChapter;
import com.hartwig.hmftools.patientreporter.cfreport.components.TableUtil;
import com.itextpdf.io.IOException;
import com.itextpdf.kernel.pdf.action.PdfAction;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Div;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.property.UnitValue;

import org.apache.logging.log4j.util.Strings;
import org.jetbrains.annotations.NotNull;

public class PanelExplanationChapter implements ReportChapter {

    public PanelExplanationChapter() {
    }

    @NotNull
    @Override
    public String pdfTitle() {
        return Strings.EMPTY;
    }

    @NotNull
    @Override
    public String name() {
        return "Result explanation";
    }

    @Override
    public void render(@NotNull Document reportDocument) throws IOException {
        Table table = new Table(UnitValue.createPercentArray(new float[] { 1 }));
        table.setWidth(contentWidth());
        table.addCell(TableUtil.createLayoutCell().add(createExplanationDiv()));
        reportDocument.add(table);
    }

    @NotNull
    private static Div createExplanationDiv() {
        Div div = new Div();

        div.add(new Paragraph("Details on the report general ").addStyle(ReportResources.smallBodyHeadingStyle()));
        div.add(createContentParagraph("The variant calling of the sequencing data is based on reference genome version GRCh38."));
        div.add(createContentDivWithLinkThree("The transcript list can be found at ",
                "https://resources.hartwigmedicalfoundation.nl",
                " in the directory " + "'Patient-Reporting.",
                "https://resources.hartwigmedicalfoundation.nl"));

        div.add(new Paragraph("").addStyle(ReportResources.smallBodyHeadingStyle()));
        div.add(new Paragraph("Details on the VCF file").addStyle(ReportResources.smallBodyHeadingStyle()));
        div.add(createContentDivWithLinkThree("A short description of the headers present in the VCF file can be found at ",
                "https://resources.hartwigmedicalfoundation.nl",
                " in the directory 'Patient-Reporting.",
                "https://resources.hartwigmedicalfoundation.nl"));
        return div;
    }

    @NotNull
    private static Paragraph createContentParagraph(@NotNull String text) {
        return new Paragraph(text).addStyle(ReportResources.smallBodyTextStyle()).setFixedLeading(ReportResources.BODY_TEXT_LEADING);
    }

    @NotNull
    private static Div createContentDivWithLinkThree(@NotNull String string1, @NotNull String string2, @NotNull String string3,
            @NotNull String link) {
        Div div = new Div();

        div.add(createParaGraphWithLinkThree(string1, string2, string3, link));
        return div;
    }

    @NotNull
    private static Paragraph createParaGraphWithLinkThree(@NotNull String string1, @NotNull String string2, @NotNull String string3,
            @NotNull String link) {
        return new Paragraph(string1).addStyle(ReportResources.subTextStyle())
                .setFixedLeading(ReportResources.BODY_TEXT_LEADING)
                .add(new Text(string2).addStyle(ReportResources.urlStyle()).setAction(PdfAction.createURI(link)))
                .setFixedLeading(ReportResources.BODY_TEXT_LEADING)
                .add(new Text(string3).addStyle(ReportResources.subTextStyle()))
                .setFixedLeading(ReportResources.BODY_TEXT_LEADING);
    }
}