package rs.teslaris.assessment.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import rs.teslaris.core.service.interfaces.document.FileService;
import rs.teslaris.core.util.files.ResourceMultipartFile;
import rs.teslaris.core.util.functional.Pair;

@Component
public class ReportTemplateEngine {

    private static FileService fileService;

    public ReportTemplateEngine(FileService fileService) {
        ReportTemplateEngine.fileService = fileService;
    }

    public static synchronized XWPFDocument loadDocumentTemplate(String templateName)
        throws IOException {
        FileInputStream fis =
            new FileInputStream("src/main/resources/reportTemplates/" + templateName);
        var document = new XWPFDocument(fis);
        fis.close();
        return document;
    }

    public static synchronized void addColumnsToFirstRow(XWPFDocument document,
                                                         List<String> columnsData,
                                                         int tableIndex, String locale) {
        var table = document.getTables().get(tableIndex);
        var firstRow = table.getRow(0);

        for (String columnValue : columnsData) {
            var cell = firstRow.createCell();
            cell.setText(columnValue + " (" + LocalizationUtil.getMessage(
                "reporting.table63.NumberOfResults", new Object[] {}, locale) + ")");
        }
    }

    public static synchronized void dynamicallyGenerateTableRows(XWPFDocument document,
                                                                 List<List<String>> rowsData,
                                                                 Integer tableIndex) {
        var table = document.getTables().get(tableIndex);
        setTableBorders(table);

        if (table.getRows().size() > 1) {
            table.removeRow(1);
        }

        for (List<String> rowData : rowsData) {
            var row = table.createRow();

            for (int i = 0; i < rowData.size(); i++) {
                var cell = row.getCell(i) != null ? row.getCell(i) : row.createCell();
                var text = rowData.get(i);

                if (!text.contains("§")) {
                    var entries = text.split("\n");
                    for (var entry : entries) {
                        if (entry.isEmpty()) {
                            continue;
                        }
                        setText(cell, entry);
                    }
                } else {
                    var entries = text.split("\n");
                    for (var entry : entries) {
                        if (entry.isEmpty()) {
                            continue;
                        }
                        var entryTokens = entry.split("§");
                        setColoredText(cell, entryTokens[0], entryTokens[1]);
                    }
                }

                var firstParagraph = cell.getParagraphs().getFirst();
                if (firstParagraph.isEmpty()) {
                    cell.removeParagraph(0);
                }
            }
        }
    }

    private static void setTableBorders(XWPFTable table) {
        var ctTbl = table.getCTTbl();

        var tblPr = ctTbl.getTblPr();
        if (tblPr == null) {
            tblPr = ctTbl.addNewTblPr();
        }

        var tblBorders = tblPr.isSetTblBorders()
            ? tblPr.getTblBorders()
            : tblPr.addNewTblBorders();

        var borderSize = java.math.BigInteger.valueOf(8);

        // Top
        var top = tblBorders.isSetTop()
            ? tblBorders.getTop()
            : tblBorders.addNewTop();
        top.setVal(
            org.openxmlformats.schemas.wordprocessingml.x2006.main.STBorder.SINGLE
        );
        top.setSz(borderSize);

        // Bottom
        var bottom = tblBorders.isSetBottom()
            ? tblBorders.getBottom()
            : tblBorders.addNewBottom();
        bottom.setVal(
            org.openxmlformats.schemas.wordprocessingml.x2006.main.STBorder.SINGLE
        );
        bottom.setSz(borderSize);

        // Left
        var left = tblBorders.isSetLeft()
            ? tblBorders.getLeft()
            : tblBorders.addNewLeft();
        left.setVal(
            org.openxmlformats.schemas.wordprocessingml.x2006.main.STBorder.SINGLE
        );
        left.setSz(borderSize);

        // Right
        var right = tblBorders.isSetRight()
            ? tblBorders.getRight()
            : tblBorders.addNewRight();
        right.setVal(
            org.openxmlformats.schemas.wordprocessingml.x2006.main.STBorder.SINGLE
        );
        right.setSz(borderSize);

        // Internal horizontal lines
        var insideH = tblBorders.isSetInsideH()
            ? tblBorders.getInsideH()
            : tblBorders.addNewInsideH();
        insideH.setVal(
            org.openxmlformats.schemas.wordprocessingml.x2006.main.STBorder.SINGLE
        );
        insideH.setSz(borderSize);

        // Internal vertical lines
        var insideV = tblBorders.isSetInsideV()
            ? tblBorders.getInsideV()
            : tblBorders.addNewInsideV();
        insideV.setVal(
            org.openxmlformats.schemas.wordprocessingml.x2006.main.STBorder.SINGLE
        );
        insideV.setSz(borderSize);
    }

    private static synchronized void setText(XWPFTableCell cell, String text) {
        var paragraph = cell.addParagraph();
        var run = paragraph.createRun();
        run.setText(text);
    }

    private static synchronized void setColoredText(XWPFTableCell cell, String text, String color) {
        var paragraph = cell.addParagraph();
        var run = paragraph.createRun();
        run.setTextHighlightColor(color);
        run.setText(text);
    }

    public static synchronized void insertFields(XWPFDocument document,
                                                 Map<String, String> replacements) {
        document.getParagraphs().forEach(paragraph -> processParagraph(paragraph, replacements));

        document.getTables().forEach(table ->
            table.getRows().forEach(row ->
                row.getTableCells().forEach(cell ->
                    cell.getParagraphs().forEach(paragraph ->
                        processParagraph(paragraph, replacements)
                    )
                )
            )
        );
    }

    private static synchronized void processParagraph(XWPFParagraph paragraph,
                                                      Map<String, String> replacements) {
        StringBuilder fullText = new StringBuilder();
        for (var run : paragraph.getRuns()) {
            if (run.getText(0) != null) {
                fullText.append(run.getText(0));
            }
        }

        String updatedText = replacePlaceholders(fullText.toString(), replacements);

        if (!updatedText.contentEquals(fullText)) {
            paragraph.getRuns().forEach(run -> run.setText("", 0));
            var newRun = paragraph.createRun();
            newRun.setText(updatedText);
        }
    }

    private static synchronized String replacePlaceholders(String text,
                                                           Map<String, String> replacements) {
        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            text = text.replace(entry.getKey(), entry.getValue());
        }
        return text;
    }

    public static synchronized Pair<InputStreamResource, Integer> getReportAsResource(
        XWPFDocument document)
        throws IOException {
        try (var baos = new ByteArrayOutputStream()) {
            document.write(baos);
            document.close();

            byte[] bytes = baos.toByteArray();
            var resource = new InputStreamResource(new ByteArrayInputStream(bytes));

            return new Pair<>(resource, bytes.length);
        }
    }

    public static synchronized void saveReport(XWPFDocument document, String reportName)
        throws IOException {
        fileService.store(convertToMultipartFile(document, reportName), reportName.split("\\.")[0]);
        document.close();
    }

    private static synchronized MultipartFile convertToMultipartFile(XWPFDocument document,
                                                                     String fileName)
        throws IOException {
        var byteArrayOutputStream = new ByteArrayOutputStream();
        document.write(byteArrayOutputStream);
        return new ResourceMultipartFile(fileName, fileName,
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            new ByteArrayResource(byteArrayOutputStream.toByteArray()));
    }
}
