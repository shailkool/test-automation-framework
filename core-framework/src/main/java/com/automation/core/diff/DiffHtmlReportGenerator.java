package com.automation.core.diff;

import lombok.extern.log4j.Log4j2;

import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Generates HTML report for data differences
 * Similar to csv-diff-report functionality
 */
@Log4j2
public class DiffHtmlReportGenerator {
    
    private static final String CSS_STYLES = """
        <style>
            * { margin: 0; padding: 0; box-sizing: border-box; }
            body {
                font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                background: #f5f7fa;
                padding: 20px;
            }
            .container { max-width: 1400px; margin: 0 auto; }
            .header {
                background: white;
                padding: 30px;
                border-radius: 8px;
                box-shadow: 0 2px 4px rgba(0,0,0,0.1);
                margin-bottom: 20px;
            }
            h1 {
                color: #2c3e50;
                margin-bottom: 10px;
                font-size: 28px;
            }
            .timestamp {
                color: #7f8c8d;
                font-size: 14px;
            }
            .summary {
                background: white;
                padding: 25px;
                border-radius: 8px;
                box-shadow: 0 2px 4px rgba(0,0,0,0.1);
                margin-bottom: 20px;
            }
            .summary-grid {
                display: grid;
                grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
                gap: 15px;
                margin-top: 15px;
            }
            .summary-card {
                padding: 15px;
                border-radius: 6px;
                border-left: 4px solid;
            }
            .summary-card h3 {
                font-size: 14px;
                color: #7f8c8d;
                margin-bottom: 5px;
                text-transform: uppercase;
                letter-spacing: 0.5px;
            }
            .summary-card .value {
                font-size: 32px;
                font-weight: bold;
            }
            .added-card { background: #d4edda; border-color: #28a745; }
            .added-card .value { color: #28a745; }
            .deleted-card { background: #f8d7da; border-color: #dc3545; }
            .deleted-card .value { color: #dc3545; }
            .modified-card { background: #fff3cd; border-color: #ffc107; }
            .modified-card .value { color: #e09900; }
            .unchanged-card { background: #d1ecf1; border-color: #17a2b8; }
            .unchanged-card .value { color: #17a2b8; }
            .total-card { background: #e2e3e5; border-color: #6c757d; }
            .total-card .value { color: #6c757d; }
            .section {
                background: white;
                padding: 25px;
                border-radius: 8px;
                box-shadow: 0 2px 4px rgba(0,0,0,0.1);
                margin-bottom: 20px;
            }
            .section h2 {
                color: #2c3e50;
                margin-bottom: 15px;
                font-size: 20px;
                padding-bottom: 10px;
                border-bottom: 2px solid #ecf0f1;
            }
            .badge {
                display: inline-block;
                padding: 4px 12px;
                border-radius: 12px;
                font-size: 12px;
                font-weight: 600;
                margin-left: 10px;
            }
            .badge-added { background: #28a745; color: white; }
            .badge-deleted { background: #dc3545; color: white; }
            .badge-modified { background: #ffc107; color: #000; }
            table {
                width: 100%;
                border-collapse: collapse;
                margin-top: 15px;
            }
            th {
                background: #f8f9fa;
                padding: 12px;
                text-align: left;
                font-weight: 600;
                color: #495057;
                border-bottom: 2px solid #dee2e6;
                position: sticky;
                top: 0;
            }
            td {
                padding: 12px;
                border-bottom: 1px solid #dee2e6;
            }
            tr:hover {
                background: #f8f9fa;
            }
            .key-cell {
                font-weight: 600;
                color: #495057;
            }
            .added-row {
                background: #d4edda !important;
            }
            .deleted-row {
                background: #f8d7da !important;
            }
            .modified-row {
                background: #fff3cd !important;
            }
            .old-value {
                text-decoration: line-through;
                color: #dc3545;
                opacity: 0.7;
            }
            .new-value {
                color: #28a745;
                font-weight: 600;
            }
            .diff-indicator {
                font-size: 10px;
                padding: 2px 6px;
                border-radius: 3px;
                margin-left: 5px;
            }
            .no-data {
                text-align: center;
                padding: 40px;
                color: #6c757d;
                font-style: italic;
            }
            .footer {
                text-align: center;
                margin-top: 30px;
                color: #7f8c8d;
                font-size: 14px;
            }
        </style>
        """;
    
    /**
     * Generate HTML report and save to file
     */
    public static void generateReport(DataDiff diff, String outputPath, 
                                     String leftTitle, String rightTitle) {
        try (FileWriter writer = new FileWriter(outputPath)) {
            writer.write(generateHtml(diff, leftTitle, rightTitle));
            log.info("HTML diff report generated: {}", outputPath);
        } catch (IOException e) {
            log.error("Error generating HTML report", e);
            throw new RuntimeException("Failed to generate HTML report", e);
        }
    }
    
    /**
     * Generate HTML report content
     */
    public static String generateHtml(DataDiff diff, String leftTitle, String rightTitle) {
        StringBuilder html = new StringBuilder();
        
        html.append("<!DOCTYPE html>\n");
        html.append("<html lang='en'>\n<head>\n");
        html.append("<meta charset='UTF-8'>\n");
        html.append("<meta name='viewport' content='width=device-width, initial-scale=1.0'>\n");
        html.append("<title>Data Diff Report</title>\n");
        html.append(CSS_STYLES);
        html.append("</head>\n<body>\n");
        
        html.append("<div class='container'>\n");
        
        // Header
        html.append(generateHeader(leftTitle, rightTitle));
        
        // Summary
        html.append(generateSummary(diff));
        
        // Added rows
        if (!diff.getAddedRows().isEmpty()) {
            html.append(generateAddedSection(diff));
        }
        
        // Deleted rows
        if (!diff.getDeletedRows().isEmpty()) {
            html.append(generateDeletedSection(diff));
        }
        
        // Modified rows
        if (!diff.getModifiedRows().isEmpty()) {
            html.append(generateModifiedSection(diff));
        }
        
        // Footer
        html.append(generateFooter());
        
        html.append("</div>\n");
        html.append("</body>\n</html>");
        
        return html.toString();
    }
    
    private static String generateHeader(String leftTitle, String rightTitle) {
        return String.format("""
            <div class='header'>
                <h1>📊 Data Comparison Report</h1>
                <div class='timestamp'>Generated: %s</div>
                <div style='margin-top: 15px;'>
                    <strong>Left:</strong> %s &nbsp;&nbsp; | &nbsp;&nbsp; <strong>Right:</strong> %s
                </div>
            </div>
            """,
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
            leftTitle, rightTitle);
    }
    
    private static String generateSummary(DataDiff diff) {
        DiffSummary summary = diff.getSummary();
        
        return String.format("""
            <div class='summary'>
                <h2>Summary</h2>
                <div class='summary-grid'>
                    <div class='summary-card total-card'>
                        <h3>Left Rows</h3>
                        <div class='value'>%d</div>
                    </div>
                    <div class='summary-card total-card'>
                        <h3>Right Rows</h3>
                        <div class='value'>%d</div>
                    </div>
                    <div class='summary-card added-card'>
                        <h3>Added</h3>
                        <div class='value'>%d</div>
                    </div>
                    <div class='summary-card deleted-card'>
                        <h3>Deleted</h3>
                        <div class='value'>%d</div>
                    </div>
                    <div class='summary-card modified-card'>
                        <h3>Modified</h3>
                        <div class='value'>%d</div>
                    </div>
                    <div class='summary-card unchanged-card'>
                        <h3>Unchanged</h3>
                        <div class='value'>%d</div>
                    </div>
                </div>
            </div>
            """,
            summary.getLeftRowCount(),
            summary.getRightRowCount(),
            summary.getAddedCount(),
            summary.getDeletedCount(),
            summary.getModifiedCount(),
            summary.getUnchangedCount());
    }
    
    private static String generateAddedSection(DataDiff diff) {
        StringBuilder html = new StringBuilder();
        html.append("<div class='section'>\n");
        html.append("<h2>➕ Added Rows <span class='badge badge-added'>").append(diff.getAddedRows().size()).append("</span></h2>\n");
        
        if (diff.getAddedRows().isEmpty()) {
            html.append("<div class='no-data'>No rows added</div>\n");
        } else {
            html.append(generateTableForRows(diff.getAddedRows(), "added-row"));
        }
        
        html.append("</div>\n");
        return html.toString();
    }
    
    private static String generateDeletedSection(DataDiff diff) {
        StringBuilder html = new StringBuilder();
        html.append("<div class='section'>\n");
        html.append("<h2>➖ Deleted Rows <span class='badge badge-deleted'>").append(diff.getDeletedRows().size()).append("</span></h2>\n");
        
        if (diff.getDeletedRows().isEmpty()) {
            html.append("<div class='no-data'>No rows deleted</div>\n");
        } else {
            html.append(generateTableForRows(diff.getDeletedRows(), "deleted-row"));
        }
        
        html.append("</div>\n");
        return html.toString();
    }
    
    private static String generateModifiedSection(DataDiff diff) {
        StringBuilder html = new StringBuilder();
        html.append("<div class='section'>\n");
        html.append("<h2>✏️ Modified Rows <span class='badge badge-modified'>").append(diff.getModifiedRows().size()).append("</span></h2>\n");
        
        if (diff.getModifiedRows().isEmpty()) {
            html.append("<div class='no-data'>No rows modified</div>\n");
        } else {
            html.append(generateModifiedTable(diff.getModifiedRows()));
        }
        
        html.append("</div>\n");
        return html.toString();
    }
    
    private static String generateTableForRows(List<DiffRow> rows, String rowClass) {
        if (rows.isEmpty()) {
            return "";
        }
        
        StringBuilder html = new StringBuilder();
        html.append("<table>\n");
        
        // Get all column names
        Set<String> columns = new LinkedHashSet<>();
        for (DiffRow row : rows) {
            Map<String, String> data = row.getLeftRow() != null ? row.getLeftRow() : row.getRightRow();
            if (data != null) {
                columns.addAll(data.keySet());
            }
        }
        
        // Header
        html.append("<thead><tr>\n");
        for (String column : columns) {
            html.append("<th>").append(escapeHtml(column)).append("</th>\n");
        }
        html.append("</tr></thead>\n");
        
        // Rows
        html.append("<tbody>\n");
        for (DiffRow row : rows) {
            Map<String, String> data = row.getLeftRow() != null ? row.getLeftRow() : row.getRightRow();
            html.append("<tr class='").append(rowClass).append("'>\n");
            
            for (String column : columns) {
                String value = data.getOrDefault(column, "");
                html.append("<td>").append(escapeHtml(value)).append("</td>\n");
            }
            
            html.append("</tr>\n");
        }
        html.append("</tbody>\n");
        html.append("</table>\n");
        
        return html.toString();
    }
    
    private static String generateModifiedTable(List<DiffRow> rows) {
        if (rows.isEmpty()) {
            return "";
        }
        
        StringBuilder html = new StringBuilder();
        html.append("<table>\n");
        
        // Get all column names
        Set<String> columns = new LinkedHashSet<>();
        for (DiffRow row : rows) {
            if (row.getLeftRow() != null) {
                columns.addAll(row.getLeftRow().keySet());
            }
            if (row.getRightRow() != null) {
                columns.addAll(row.getRightRow().keySet());
            }
        }
        
        // Header
        html.append("<thead><tr>\n");
        html.append("<th>Key</th>\n");
        for (String column : columns) {
            html.append("<th>").append(escapeHtml(column)).append("</th>\n");
        }
        html.append("</tr></thead>\n");
        
        // Rows
        html.append("<tbody>\n");
        for (DiffRow row : rows) {
            html.append("<tr class='modified-row'>\n");
            html.append("<td class='key-cell'>").append(escapeHtml(row.getKey())).append("</td>\n");
            
            for (String column : columns) {
                String leftValue = row.getLeftRow().getOrDefault(column, "");
                String rightValue = row.getRightRow().getOrDefault(column, "");
                
                if (!leftValue.equals(rightValue)) {
                    html.append("<td>");
                    html.append("<div class='old-value'>").append(escapeHtml(leftValue)).append("</div>");
                    html.append("<div class='new-value'>").append(escapeHtml(rightValue)).append("</div>");
                    html.append("</td>\n");
                } else {
                    html.append("<td>").append(escapeHtml(leftValue)).append("</td>\n");
                }
            }
            
            html.append("</tr>\n");
        }
        html.append("</tbody>\n");
        html.append("</table>\n");
        
        return html.toString();
    }
    
    private static String generateFooter() {
        return """
            <div class='footer'>
                Generated by Test Automation Framework | Data Diff Report
            </div>
            """;
    }
    
    private static String escapeHtml(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
                  .replace("<", "&lt;")
                  .replace(">", "&gt;")
                  .replace("\"", "&quot;")
                  .replace("'", "&#39;");
    }
}
