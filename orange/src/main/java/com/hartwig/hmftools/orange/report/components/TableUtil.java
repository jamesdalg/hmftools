package com.hartwig.hmftools.orange.report.components;

import com.hartwig.hmftools.orange.report.ReportResources;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.IBlockElement;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.property.UnitValue;
import com.itextpdf.layout.property.VerticalAlignment;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TableUtil {

    private static final float TABLE_BOTTOM_MARGIN = 20;

    private TableUtil() {
    }

    @NotNull
    public static Table createReportContentTable(@NotNull float[] columnPercentageWidths, @NotNull Cell[] headerCells) {
        Table table = new Table(UnitValue.createPercentArray(columnPercentageWidths)).setWidth(ReportResources.CONTENT_WIDTH_WIDE);
        table.setFixedLayout();

        for (Cell headerCell : headerCells) {
            table.addHeaderCell(headerCell);
        }

        return table;
    }

    @NotNull
    public static Table createWrappingReportTable(@NotNull Table contentTable) {
        return createWrappingReportTable(contentTable, null);
    }

    @NotNull
    public static Table createWrappingReportTable(@NotNull Table contentTable, @Nullable String tableTitle) {
        contentTable.addFooterCell(new Cell(1, contentTable.getNumberOfColumns()).setBorder(Border.NO_BORDER)
                .setPaddingTop(5)
                .setPaddingBottom(5)
                .add(new Paragraph("The table continues on the next page".toUpperCase()).addStyle(ReportResources.subTextStyle())))
                .setSkipLastFooter(true);

        Table continuedWrapTable = new Table(1).setMinWidth(contentTable.getWidth())
                .addHeaderCell(new Cell().setBorder(Border.NO_BORDER)
                        .add(new Paragraph("Continued from the previous page".toUpperCase()).addStyle(ReportResources.subTextStyle())))
                .setSkipFirstHeader(true)
                .addCell(new Cell().add(contentTable).setPadding(0).setBorder(Border.NO_BORDER));

        Table table = new Table(1).setMinWidth(contentTable.getWidth()).setMarginBottom(TABLE_BOTTOM_MARGIN);
        if (tableTitle != null) {
            table.addHeaderCell(new Cell().setBorder(Border.NO_BORDER)
                    .setPadding(0)
                    .add(new Paragraph(tableTitle).addStyle(ReportResources.tableTitleStyle())));
        }

        table.addCell(new Cell().add(continuedWrapTable).setPadding(0).setBorder(Border.NO_BORDER));
        return table;
    }

    @NotNull
    public static Cell createHeaderCell(@NotNull String text) {
        return createHeaderCell(text, 1);
    }

    @NotNull
    public static Cell createHeaderCell(@NotNull String text, int colSpan) {
        return createHeaderCell(colSpan).add(new Paragraph(text.toUpperCase()));
    }

    @NotNull
    private static Cell createHeaderCell(int colSpan) {
        Cell c = new Cell(1, colSpan);
        c.setHeight(23); // Set fixed height to create consistent spacing between table title and header
        c.setBorder(Border.NO_BORDER);
        c.setVerticalAlignment(VerticalAlignment.BOTTOM);
        c.addStyle(ReportResources.tableHeaderStyle());
        return c;
    }

    @NotNull
    public static Cell createContentCell(@NotNull String text) {
        return createContentCell(new Paragraph(text));
    }

    @NotNull
    public static Cell createContentCell(@NotNull IBlockElement element) {
        Cell c = new Cell();
        c.setBorder(Border.NO_BORDER);
        c.setBorderBottom(new SolidBorder(ReportResources.PALETTE_MID_GREY, 0.25F));
        c.addStyle(ReportResources.tableContentStyle());
        c.setKeepTogether(true);
        c.add(element);
        return c;
    }
}
