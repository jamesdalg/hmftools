package com.hartwig.hmftools.patientreporter.cfreport;

import com.hartwig.hmftools.patientreporter.PatientReport;
import com.hartwig.hmftools.patientreporter.cfreport.components.Footer;
import com.hartwig.hmftools.patientreporter.cfreport.components.Header;
import com.hartwig.hmftools.patientreporter.cfreport.components.SidePanel;
import com.itextpdf.kernel.events.Event;
import com.itextpdf.kernel.events.IEventHandler;
import com.itextpdf.kernel.events.PdfDocumentEvent;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfOutline;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.navigation.PdfExplicitRemoteGoToDestination;

import org.jetbrains.annotations.NotNull;

class PageEventHandler implements IEventHandler {

    @NotNull
    private final PatientReport patientReport;

    @NotNull
    private final Footer footer;
    @NotNull
    private final Header header;

    private boolean fullSidebar;
    private boolean fullSidebarContent;

    private String chapterTitle = "Undefined";
    private String pdfTitle = "Undefined";
    private boolean firstPageOfChapter = true;

    private PdfOutline outline = null;

    PageEventHandler(@NotNull final PatientReport patientReport) {
        this.patientReport = patientReport;

        this.header = new Header(patientReport.logoCompanyPath());
        this.footer = new Footer();
    }

    @Override
    public void handleEvent(@NotNull Event event) {
        PdfDocumentEvent documentEvent = (PdfDocumentEvent) event;
        if (documentEvent.getType().equals(PdfDocumentEvent.START_PAGE)) {
            PdfPage page = documentEvent.getPage();

            header.renderHeader(chapterTitle, pdfTitle, firstPageOfChapter, page, patientReport.isWGSreport());
            if (firstPageOfChapter) {
                firstPageOfChapter = false;

                createChapterBookmark(documentEvent.getDocument(), chapterTitle);
            }

            SidePanel.renderSidePatientReport(page, patientReport, fullSidebar, fullSidebarContent);
            footer.renderFooter(page, !fullSidebar);
        }
    }

    void pdfTitle(@NotNull String pdfTitle) {
        this.pdfTitle = pdfTitle;
    }

    void chapterTitle(@NotNull String chapterTitle) {
        this.chapterTitle = chapterTitle;
    }

    void sidebarType(boolean full, boolean fullContent) {
        fullSidebar = full;
        fullSidebarContent = fullSidebar && fullContent;
    }

    void resetChapterPageCounter() {
        firstPageOfChapter = true;
    }

    void writeDynamicTextParts(@NotNull PdfDocument document) {
        header.writeChapterTitles(document);
        footer.writeTotalPageCount(document);
    }

    private void createChapterBookmark(@NotNull PdfDocument pdf, @NotNull String title) {
        if (outline == null) {
            outline = pdf.getOutlines(false);
        }

        PdfOutline chapterItem = outline.addOutline(title);
        chapterItem.addDestination(PdfExplicitRemoteGoToDestination.createFitH(pdf.getNumberOfPages(), 0));
    }
}
