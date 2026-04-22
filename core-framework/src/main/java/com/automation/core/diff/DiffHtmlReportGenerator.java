package com.automation.core.diff;

import lombok.extern.log4j.Log4j2;

import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Renders a {@link DataDiff} as a standalone, interactive HTML report.
 *
 * <p>The report is modelled on the conventional CSV-diff layout: a match
 * percentage badge, a row of summary counters (Changed / Added / Removed /
 * Unchanged), a file-metadata strip, pill-style filter buttons with live
 * counts, and a single unified table that highlights per-cell differences
 * with stacked red/green pills and a coloured accent stripe on every
 * differing row.
 *
 * <p>The HTML is fully self-contained (inline CSS + JS, no external
 * fetches) so it renders identically whether opened directly from disk or
 * embedded in a masterthought cucumber report.
 */
@Log4j2
public class DiffHtmlReportGenerator {

    private DiffHtmlReportGenerator() {
    }

    /**
     * Legacy entry point retained for backwards compatibility. Treats
     * {@code leftTitle} as the old-side label and {@code rightTitle} as the
     * new-side label, and derives a page title from their common prefix when
     * possible.
     */
    public static String generateHtml(DataDiff diff, String leftTitle, String rightTitle) {
        return generateHtml(diff, deriveTitle(leftTitle, rightTitle), leftTitle, rightTitle);
    }

    /**
     * Render the diff using plain old/new label strings &mdash; used by
     * callers that don't have a real file on disk (e.g. Cucumber steps).
     */
    public static String generateHtml(DataDiff diff, String reportTitle, String oldLabel, String newLabel) {
        return generateHtml(diff, reportTitle,
            DiffSideInfo.label(oldLabel),
            DiffSideInfo.label(newLabel));
    }

    /**
     * Render the diff using rich per-side metadata (path, creation time,
     * byte size, row count).
     */
    public static String generateHtml(DataDiff diff,
                                      String reportTitle,
                                      DiffSideInfo oldSide,
                                      DiffSideInfo newSide) {
        DiffSummary summary = diff.getSummary();
        int changed = summary.getModifiedCount();
        int added = summary.getAddedCount();
        int removed = summary.getDeletedCount();
        int unchanged = summary.getUnchangedCount();
        int oldCount = summary.getLeftRowCount();
        int newCount = summary.getRightRowCount();

        int denominator = Math.max(oldCount, newCount);
        double matchPct = denominator == 0 ? 100.0 : (unchanged * 100.0) / denominator;

        List<DiffRow> rows = diff.getAllRowsInOrder();
        Set<String> columns = collectColumns(rows);

        StringBuilder html = new StringBuilder(12 * 1024);
        html.append("<!DOCTYPE html>\n<html lang='en'>\n<head>\n");
        html.append("<meta charset='UTF-8'>\n");
        html.append("<meta name='viewport' content='width=device-width, initial-scale=1.0'>\n");
        html.append("<title>Diff · ").append(escapeHtml(reportTitle)).append("</title>\n");
        html.append(STYLES);
        html.append("</head>\n<body>\n<div class='container'>\n");

        appendTitle(html, reportTitle, oldSide.getLabel(), newSide.getLabel(), matchPct, oldCount, newCount);
        appendMetaStrip(html, oldSide, newSide, diff.getKeyFields());
        appendToolbar(html, columns, changed, added, removed, unchanged);
        appendTable(html, rows, columns);

        html.append("</div>\n");
        html.append(SCRIPT);
        html.append("</body>\n</html>");

        return html.toString();
    }

    /** Render the diff and write it to {@code outputPath}. */
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

    private static void appendTitle(StringBuilder html,
                                    String reportTitle,
                                    String oldLabel,
                                    String newLabel,
                                    double matchPct,
                                    int oldCount,
                                    int newCount) {
        String tone = matchPct >= 99.999 ? "high" : matchPct >= 90.0 ? "mid" : "low";
        html.append("<div class='header-section'>\n");
        html.append("  <div class='title-group'>\n");
        html.append("    <h1 class='report-title'>Diff: ").append(escapeHtml(reportTitle)).append("</h1>\n");
        html.append("    <div class='comparison-summary'>")
            .append("<span>").append(escapeHtml(oldLabel)).append(" <small>(").append(oldCount).append(" rows)</small></span> ")
            .append("<span class='vs'>VS</span> ")
            .append("<span>").append(escapeHtml(newLabel)).append(" <small>(").append(newCount).append(" rows)</small></span>")
            .append("</div>\n");
        html.append("  </div>\n");
        html.append("  <div class='similarity-card tone-").append(tone).append("'>\n");
        html.append("    <div class='sim-label'>Overall Similarity</div>\n");
        html.append("    <div class='sim-value-row'>\n");
        html.append("      <span class='sim-number'>").append(String.format("%.1f", matchPct)).append("</span>\n");
        html.append("      <span class='sim-unit'>%</span>\n");
        html.append("    </div>\n");
        html.append("    <div class='sim-bar-bg'><div class='sim-bar-fill' style='width:").append(matchPct).append("%'></div></div>\n");
        html.append("  </div>\n");
        html.append("</div>\n");
    }

    // appendSummaryCards removed and integrated into appendToolbar

    private static void appendMetaStrip(StringBuilder html,
                                        DiffSideInfo oldSide,
                                        DiffSideInfo newSide,
                                        List<String> keyFields) {

        String generated = LocalDateTime.now()
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        html.append("<details class='meta-section'>\n");
        html.append("  <summary class='meta-summary'>\n");
        html.append("    <span class='summary-pill'><span class='glyph'>&#x1F511;</span>")
            .append("<span class='label'>Keys:</span>")
            .append("<span class='value'>");
        if (keyFields != null && !keyFields.isEmpty()) {
            for (int i = 0; i < keyFields.size(); i++) {
                html.append("<span class='key-chip'>").append(escapeHtml(keyFields.get(i))).append("</span>");
                if (i < keyFields.size() - 1) html.append(", ");
            }
        } else {
            html.append("(none)");
        }
        html.append("</span></span>\n");
        html.append("    <span class='summary-pill'><span class='glyph'>&#x1F552;</span>")
            .append("<span class='label'>Generated:</span>")
            .append("<span class='value'>").append(escapeHtml(generated)).append("</span></span>\n");
        html.append("    <span class='meta-toggle-hint'>(Click to view file details)</span>\n");
        html.append("  </summary>\n");

        html.append("  <div class='meta-grid'>\n");
        appendSideCard(html, "SOURCE (OLD)", "source", oldSide);
        appendSideCard(html, "TARGET (NEW)", "target", newSide);
        html.append("  </div>\n");
        html.append("</details>\n");
    }

    private static void appendSideCard(StringBuilder html, String sideLabel, String sideClass, DiffSideInfo info) {
        html.append("    <div class='file-card ").append(sideClass).append("'>\n");
        html.append("      <div class='file-card-header'>\n");
        html.append("        <span class='glyph'>").append(sideClass.equals("source") ? "&#x1F4C1;" : "&#x1F4C2;").append("</span>\n");
        html.append("        <span class='tag'>").append(sideLabel).append("</span>\n");
        html.append("      </div>\n");
        html.append("      <div class='file-card-body'>\n");

        String path = (info != null && info.hasPath()) ? info.getPath() : (info != null ? info.getLabel() : "N/A");
        html.append("        <div class='file-row'>\n");
        html.append("          <span class='l'>Path:</span>\n");
        html.append("          <span class='v-wrap'>\n");
        html.append("            <span class='v' title='").append(escapeAttr(path)).append("'>").append(truncatePath(path)).append("</span>\n");
        html.append("            <button class='copy-path' data-value='").append(escapeAttr(path)).append("' title='Copy full path'>&#x2392;</button>\n");
        html.append("          </span>\n");
        html.append("        </div>\n");

        if (info != null) {
            if (info.hasCreated()) {
                html.append("        <div class='file-row'><span class='l'>Created:</span><span class='v'>").append(escapeHtml(info.getCreatedAt())).append("</span></div>\n");
            }
            if (info.hasSize()) {
                html.append("        <div class='file-row'><span class='l'>Size:</span><span class='v'>").append(escapeHtml(info.getHumanSize())).append("</span></div>\n");
            }
            if (info.hasRowCount()) {
                html.append("        <div class='file-row'><span class='l'>Rows:</span><span class='v'>").append(info.getRowCount()).append("</span></div>\n");
            }
        }

        html.append("      </div>\n");
        html.append("    </div>\n");
    }

    private static String truncatePath(String path) {
        if (path == null || path.length() <= 60) return escapeHtml(path);
        int len = path.length();
        return escapeHtml(path.substring(0, 25)) + " ... " + escapeHtml(path.substring(len - 30));
    }

    private static void appendToolbar(StringBuilder html,
                                      Set<String> columns,
                                      int changed, int added, int removed, int unchanged) {
        int total = changed + added + removed + unchanged;
        html.append("<div class='toolbar'>\n");
        html.append(filterButton("all",       "&#x25A6;", "ALL",      total,     true));
        html.append(filterButton("modified",  "&#x270E;", "Changed",   changed,   false));
        html.append(filterButton("added",     "&plus;",    "Added",    added,     false));
        html.append(filterButton("deleted",   "&minus;",   "Deleted",  removed,   false));
        html.append(filterButton("unchanged", "&check;",   "Unchanged", unchanged, false));
        html.append("  <div class='spacer'></div>\n");
        html.append("  <label class='hide-toggle'><input type='checkbox' id='hideUnchanged'>"
                  + "<span>Hide unchanged</span></label>\n");
        html.append("  <div class='columns-dropdown'>\n");
        html.append("    <button class='filter-btn btn-cols' id='columnsBtn' type='button'>\n")
            .append("      <div class='icon-box'>&#9776;</div>\n")
            .append("      <div class='btn-content'>\n")
            .append("        <div class='btn-label'>Settings</div>\n")
            .append("        <div class='btn-count' style='font-size:14px;'>Columns</div>\n")
            .append("      </div>\n")
            .append("    </button>\n");
            html.append("    <div class='columns-panel' id='columnsPanel' hidden>\n");
        html.append("      <div class='panel-header'>Select Columns</div>\n");
        html.append("      <div class='panel-actions'>\n");
        html.append("        <label><input type='checkbox' id='selectAllCols' checked> <b>Select All</b></label>\n");
        html.append("      </div>\n");
        int colIndex = 2; // 1 = row icon column, 2+ = data columns
        for (String col : columns) {
            html.append("<label><input type='checkbox' class='col-toggle' data-col='")
                .append(colIndex++)
                .append("' checked> ")
                .append("<span>").append(escapeHtml(col)).append("</span>")
                .append("</label>");
        }
        html.append("    </div>");
        html.append("  </div>\n");
        html.append("</div>\n");
    }

    private static String filterButton(String filter, String glyph, String label, int count, boolean active) {
        return String.format(
            "  <button class='filter-btn%1$s btn-%2$s' data-filter='%2$s'>\n"
            + "    <div class='icon-box'>%3$s</div>\n"
            + "    <div class='btn-content'>\n"
            + "      <div class='btn-label'>%4$s</div>\n"
            + "      <div class='btn-count'>%5$d</div>\n"
            + "    </div>\n"
            + "  </button>\n",
            active ? " active" : "", filter, glyph, label, count
        );
    }

    private static void appendTable(StringBuilder html, List<DiffRow> rows, Set<String> columns) {
        html.append("<div class='table-wrap'><table>\n<thead><tr>");
        html.append("<th class='row-icon'></th>");
        for (String col : columns) {
            html.append("<th>").append(escapeHtml(col)).append("</th>");
        }
        html.append("</tr></thead>\n<tbody>\n");

        if (rows.isEmpty()) {
            html.append("<tr><td colspan='").append(columns.size() + 1)
                .append("' class='empty'>No rows to display</td></tr>\n");
        }

        for (DiffRow row : rows) {
            appendRow(html, row, columns);
        }

        html.append("</tbody>\n</table></div>\n");
    }

    private static void appendRow(StringBuilder html, DiffRow row, Set<String> columns) {
        String rowKind = classForType(row.getDiffType());
        String glyph = glyphForType(row.getDiffType());

        html.append("<tr class='row row-").append(rowKind)
            .append("' data-type='").append(rowKind).append("'>");
        html.append("<td class='row-icon'><span class='dot'>").append(glyph).append("</span></td>");

        Map<String, String> left = row.getLeftRow();
        Map<String, String> right = row.getRightRow();

        for (String col : columns) {
            html.append("<td>").append(renderCell(row.getDiffType(), col, left, right)).append("</td>");
        }
        html.append("</tr>\n");
    }

    private static String renderCell(DiffType type,
                                     String column,
                                     Map<String, String> left,
                                     Map<String, String> right) {
        switch (type) {
            case ADDED: {
                String val = right == null ? "" : right.getOrDefault(column, "");
                return escapeHtml(val);
            }
            case DELETED: {
                String val = left == null ? "" : left.getOrDefault(column, "");
                return escapeHtml(val);
            }
            case MODIFIED: {
                String l = left == null ? "" : left.getOrDefault(column, "");
                String r = right == null ? "" : right.getOrDefault(column, "");
                if (l.equals(r)) {
                    return escapeHtml(l);
                }
                StringBuilder out = new StringBuilder();
                if (!l.isEmpty()) {
                    out.append("<div class='pill-old'>").append(escapeHtml(l)).append("</div>");
                }
                if (!r.isEmpty()) {
                    out.append("<div class='pill-new'>").append(escapeHtml(r)).append("</div>");
                }
                return out.toString();
            }
            case UNCHANGED:
            default: {
                String val = left != null ? left.getOrDefault(column, "")
                    : right == null ? "" : right.getOrDefault(column, "");
                return escapeHtml(val);
            }
        }
    }

    private static Set<String> collectColumns(List<DiffRow> rows) {
        Set<String> columns = new LinkedHashSet<>();
        for (DiffRow row : rows) {
            if (row.getLeftRow() != null) columns.addAll(row.getLeftRow().keySet());
            if (row.getRightRow() != null) columns.addAll(row.getRightRow().keySet());
        }
        return columns;
    }

    private static String classForType(DiffType type) {
        switch (type) {
            case ADDED:     return "added";
            case DELETED:   return "deleted";
            case MODIFIED:  return "modified";
            case UNCHANGED:
            default:        return "unchanged";
        }
    }

    private static String glyphForType(DiffType type) {
        switch (type) {
            case ADDED:     return "&plus;";
            case DELETED:   return "&minus;";
            case MODIFIED:  return "&#x270E;";
            case UNCHANGED:
            default:        return "&check;";
        }
    }

    private static String deriveTitle(String leftTitle, String rightTitle) {
        if (leftTitle == null) return rightTitle == null ? "" : rightTitle;
        if (rightTitle == null) return leftTitle;
        if (leftTitle.endsWith(" - Expected") && rightTitle.endsWith(" - Actual")) {
            String trimmed = leftTitle.substring(0, leftTitle.length() - " - Expected".length());
            if (!trimmed.isEmpty()) return trimmed;
        }
        int common = 0;
        int max = Math.min(leftTitle.length(), rightTitle.length());
        while (common < max && leftTitle.charAt(common) == rightTitle.charAt(common)) {
            common++;
        }
        String prefix = leftTitle.substring(0, common).trim();
        if (prefix.endsWith("-")) prefix = prefix.substring(0, prefix.length() - 1).trim();
        return prefix.isEmpty() ? leftTitle : prefix;
    }

    private static String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#39;");
    }

    private static String escapeAttr(String text) {
        return escapeHtml(text);
    }

    private static final String STYLES = """
        <style>
          @import url('https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700;800&display=swap');

          :root {
            --bg:          #f8fafc;
            --surface:     #ffffff;
            --border:      #e2e8f0;
            --text-main:   #1e293b;
            --text-muted:  #64748b;
            --primary:     #4f46e5;
            --primary-soft:#eef2ff;

            --modified:     #f97316;
            --modified-soft:#fff7ed;
            --added:        #16a34a;
            --added-soft:   #f0fdf4;
            --deleted:      #dc2626;
            --deleted-soft: #fef2f2;
            --unchanged:    #0ea5e9;
            --unchanged-soft:#f0f9ff;

            --old-pill-bg:  #fee2e2;
            --old-pill-fg:  #991b1b;
            --new-pill-bg:  #d1fae5;
            --new-pill-fg:  #065f46;

            --radius-sm: 6px;
            --radius-md: 12px;
            --radius-lg: 16px;
            --shadow-sm: 0 1px 2px 0 rgb(0 0 0 / 0.05);
            --shadow-md: 0 4px 6px -1px rgb(0 0 0 / 0.1), 0 2px 4px -2px rgb(0 0 0 / 0.1);
            --shadow-lg: 0 10px 15px -3px rgb(0 0 0 / 0.1), 0 4px 6px -4px rgb(0 0 0 / 0.1);
          }

          * { margin: 0; padding: 0; box-sizing: border-box; }
          body {
            font-family: 'Inter', -apple-system, sans-serif;
            color: var(--text-main);
            background: var(--bg);
            padding: 40px 32px 80px;
            font-size: 14px;
            line-height: 1.5;
            min-height: 100vh;
          }
          .container { max-width: 1400px; margin: 0 auto; }

          /* Header Section */
          .header-section {
            display: flex;
            justify-content: space-between;
            align-items: flex-start;
            margin-bottom: 32px;
            gap: 20px;
          }
          .title-group { flex: 1; }
          .report-title {
            font-size: 36px;
            font-weight: 800;
            color: #0f172a;
            letter-spacing: -0.02em;
            margin-bottom: 4px;
          }
          .comparison-summary {
            font-size: 18px;
            color: var(--text-muted);
            font-weight: 500;
            display: flex;
            align-items: center;
            gap: 12px;
          }
          .comparison-summary .vs {
            font-size: 14px;
            font-weight: 700;
            color: #94a3b8;
            background: #f1f5f9;
            padding: 2px 8px;
            border-radius: 4px;
          }

          /* Similarity Card */
          .similarity-card {
            background: white;
            border-radius: var(--radius-md);
            padding: 16px 20px;
            box-shadow: var(--shadow-md);
            border-left: 5px solid var(--primary);
            display: flex;
            flex-direction: column;
            gap: 2px;
            min-width: 180px;
          }
          .sim-label {
            font-size: 10px;
            font-weight: 800;
            color: var(--text-muted);
            letter-spacing: 0.1em;
            text-transform: uppercase;
          }
          .sim-value-row {
            display: flex;
            align-items: baseline;
            gap: 2px;
          }
          .sim-number {
            font-size: 32px;
            font-weight: 800;
            color: var(--text-main);
            line-height: 1;
          }
          .sim-unit {
            font-size: 14px;
            font-weight: 700;
            color: var(--text-muted);
          }
          .sim-bar-bg {
            width: 100%;
            height: 6px;
            background: #f1f5f9;
            border-radius: 4px;
            margin-top: 8px;
            overflow: hidden;
          }
          .sim-bar-fill {
            height: 100%;
            background: var(--primary);
            border-radius: 4px;
          }

          .similarity-card.tone-high { border-left-color: var(--added); }
          .similarity-card.tone-high .sim-bar-fill { background: var(--added); }
          
          .similarity-card.tone-mid { border-left-color: var(--modified); }
          .similarity-card.tone-mid .sim-bar-fill { background: var(--modified); }
          
          .similarity-card.tone-low { border-left-color: var(--primary); }
          .similarity-card.tone-low .sim-bar-fill { background: var(--primary); }

          /* Meta Section - Collapsible */
          .meta-section { margin-bottom: 24px; border: 1px solid var(--border); border-radius: var(--radius-md); background: white; }
          .meta-summary {
            padding: 12px 20px;
            cursor: pointer;
            display: flex;
            align-items: center;
            gap: 24px;
            user-select: none;
            transition: background 0.2s;
          }
          .meta-summary:hover { background: #f8fafc; }
          .meta-section[open] .meta-summary { border-bottom: 1px solid var(--border); }
          .meta-toggle-hint { font-size: 11px; color: var(--text-muted); font-style: italic; margin-left: auto; }

          .summary-pill { display: flex; align-items: center; gap: 8px; font-size: 13px; }
          .summary-pill .label { font-weight: 600; color: var(--text-muted); }
          .summary-pill .value { color: var(--text-main); font-weight: 500; }

          .meta-grid {
            display: grid;
            grid-template-columns: 1fr 1fr;
            gap: 20px;
            padding: 20px;
          }
          .file-card {
            background: #fdfdfd;
            border: 1px solid #f1f5f9;
            border-radius: var(--radius-md);
            padding: 16px 20px;
          }
          .file-card-header {
            display: flex;
            align-items: center;
            gap: 10px;
            margin-bottom: 12px;
            padding-bottom: 10px;
            border-bottom: 1px solid #f1f5f9;
          }
          .file-card-header .glyph { font-size: 16px; color: var(--primary); }
          .file-card-header .tag {
            font-size: 12px;
            font-weight: 700;
            color: var(--text-muted);
            letter-spacing: 0.05em;
          }
          .file-row { display: flex; align-items: center; margin-bottom: 8px; font-size: 13px; }
          .file-row .l { width: 80px; color: var(--text-muted); font-weight: 500; }
          .file-row .v-wrap { flex: 1; display: flex; align-items: center; gap: 8px; min-width: 0; }
          .file-row .v { color: #334155; font-family: 'JetBrains Mono', monospace; word-break: break-all; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
          .copy-path {
            padding: 2px 6px; border-radius: 4px; border: 1px solid var(--border);
            background: white; cursor: pointer; font-size: 14px; color: var(--text-muted);
            transition: all 0.2s;
          }
          .copy-path:hover { background: var(--primary-soft); color: var(--primary); border-color: var(--primary); }

          .key-chip {
            background: #f1f5f9;
            padding: 2px 8px;
            border-radius: 4px;
            font-family: monospace;
            font-size: 12px;
            color: #475569;
          }

          /* Toolbar & Integrated Summary Buttons */
          .toolbar {
            display: flex;
            align-items: center;
            flex-wrap: wrap;
            gap: 16px;
            margin-bottom: 32px;
            padding: 4px;
          }
          .filter-btn {
            background: white;
            border: none;
            border-left: 4px solid transparent;
            border-radius: var(--radius-md);
            padding: 14px 20px;
            display: flex;
            align-items: center;
            gap: 16px;
            cursor: pointer;
            box-shadow: var(--shadow-sm);
            transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
            min-width: 140px;
            flex-shrink: 0;
            text-align: left;
          }
          .filter-btn:hover { transform: translateY(-3px); box-shadow: var(--shadow-md); }
          .filter-btn.active {
            box-shadow: var(--shadow-lg);
            background: #fdfdfd;
          }
          
          .filter-btn .icon-box {
            width: 40px; height: 40px;
            border-radius: 50%;
            display: flex; align-items: center; justify-content: center;
            font-size: 16px;
            flex-shrink: 0;
          }

          .btn-content { display: flex; flex-direction: column; gap: 2px; }
          .btn-label { font-size: 10px; font-weight: 800; color: var(--text-muted); letter-spacing: 0.05em; text-transform: uppercase; }
          .btn-count { font-size: 20px; font-weight: 800; color: var(--text-main); line-height: 1; }

          /* Color styles for icon boxes and borders */
          .btn-all { border-left-color: var(--primary); }
          .btn-all .icon-box { background: var(--primary-soft); color: var(--primary); }
          
          .btn-modified { border-left-color: var(--modified); }
          .btn-modified .icon-box { background: var(--modified-soft); color: var(--modified); }
          
          .btn-added { border-left-color: var(--added); }
          .btn-added .icon-box { background: var(--added-soft); color: var(--added); }
          
          .btn-deleted { border-left-color: var(--deleted); }
          .btn-deleted .icon-box { background: var(--deleted-soft); color: var(--deleted); }
          
          .btn-unchanged { border-left-color: var(--unchanged); }
          .btn-unchanged .icon-box { background: var(--unchanged-soft); color: var(--unchanged); }

          .btn-cols { border-left-color: var(--unchanged); min-width: 130px; }
          .btn-cols .icon-box { background: var(--unchanged-soft); color: var(--unchanged); }

          .filter-btn.active .btn-label { color: var(--text-main); }

          .spacer { flex: 1; }

          .hide-toggle {
            display: inline-flex;
            align-items: center;
            gap: 8px;
            font-size: 13px;
            font-weight: 600;
            color: var(--text-muted);
            cursor: pointer;
            user-select: none;
            padding: 8px 12px;
            border-radius: 8px;
            transition: all 0.2s;
          }
          .hide-toggle:hover { background: #f1f5f9; color: var(--text-main); }
          .hide-toggle input { accent-color: var(--primary); cursor: pointer; scale: 1.1; }

          .columns-dropdown { position: relative; }
          .columns-panel {
            position: absolute;
            top: calc(100% + 12px);
            right: 0;
            background: white;
            border: 1px solid var(--border);
            border-radius: var(--radius-md);
            padding: 16px;
            box-shadow: 0 20px 50px rgba(15, 23, 42, 0.15);
            z-index: 9999;
            min-width: 240px;
            max-height: 450px;
            overflow-y: auto;
          }
          .panel-header {
            font-size: 11px; font-weight: 800; color: var(--text-muted);
            text-transform: uppercase; letter-spacing: 0.1em;
            margin-bottom: 12px; padding-bottom: 8px; border-bottom: 1px solid var(--border);
          }
          .panel-actions {
            margin-bottom: 12px; padding-bottom: 8px; border-bottom: 1px solid #f1f5f9;
          }
          .columns-panel label {
            display: flex; align-items: center; gap: 10px;
            padding: 8px 10px; font-size: 13px; cursor: pointer;
            border-radius: 6px;
            transition: background 0.2s;
          }
          .columns-panel label:hover { background: #f8fafc; }
          .columns-panel input { accent-color: var(--primary); scale: 1.1; }
          .columns-panel label span { font-weight: 500; color: #334155; }

          /* Table Styling */
          .table-wrap {
            background: white;
            border: 1px solid var(--border);
            border-radius: var(--radius-md);
            overflow-x: auto;
            box-shadow: var(--shadow-md);
          }
           table { width: 100%; border-collapse: collapse; min-width: max-content; }
          th {
            background: #f1f5f9;
            text-align: left;
            padding: 16px;
            font-size: 13px; white-space: nowrap;
            font-weight: 700;
            color: #475569;
            border-bottom: 2px solid #cbd5e1;
            border-right: 1px solid #e2e8f0;
          }
          th:last-child { border-right: none; }
          td {
            padding: 12px 16px;
            border-bottom: 1px solid #f1f5f9;
            border-right: 1px solid #f1f5f9;
            font-size: 13px; white-space: nowrap;
            color: #334155;
          }
          td:last-child { border-right: none; }
          tr:last-child td { border-bottom: none; }
          tr:hover td { background: #fdfdfd; }
          .row-added:hover td, .row-deleted:hover td { background: inherit; }

          .row-icon { width: 50px; text-align: center; }
          .dot {
            width: 24px; height: 24px;
            border-radius: 50%;
            display: flex; align-items: center; justify-content: center;
            font-size: 12px; font-weight: 700;
          }

          .row-modified { border-left: 4px solid var(--modified); }
          .row-modified .dot { background: var(--modified-soft); color: var(--modified); }
          
          .row-added { border-left: 4px solid var(--added); background-color: var(--added-soft) !important; }
          .row-added .dot { background: white; color: var(--added); }
          
          .row-deleted { border-left: 4px solid var(--deleted); background-color: var(--deleted-soft) !important; }
          .row-deleted .dot { background: white; color: var(--deleted); }
          .row-deleted td { text-decoration: line-through; opacity: 0.7; }
          .row-deleted .dot { text-decoration: none !important; }

          .row-unchanged .dot { background: var(--unchanged-soft); color: var(--unchanged); }

          .pill-old {
            background: var(--old-pill-bg);
            color: var(--old-pill-fg);
            padding: 2px 8px;
            border-radius: 4px;
            text-decoration: line-through;
            font-size: 12px;
            display: inline-block;
            margin-right: 8px;
          }
          .pill-new {
            background: var(--new-pill-bg);
            color: var(--new-pill-fg);
            padding: 2px 8px;
            border-radius: 4px;
            font-size: 12px;
            font-weight: 600;
            display: inline-block;
          }

          @media (max-width: 1024px) {
            .meta-grid { grid-template-columns: 1fr; }
          }
        </style>
        """;

    private static final String SCRIPT = """
        <script>
        (function () {
          const buttons = document.querySelectorAll('.filter-btn[data-filter]');
          const rows = document.querySelectorAll('tbody tr.row');
          const hideUnchanged = document.getElementById('hideUnchanged');
          let activeFilter = 'all';

          function apply() {
            rows.forEach(function (r) {
              const t = r.dataset.type;
              let show = (activeFilter === 'all') || (activeFilter === t);
              if (hideUnchanged && hideUnchanged.checked && t === 'unchanged') show = false;
              r.style.display = show ? '' : 'none';
            });
          }

          buttons.forEach(function (btn) {
            btn.addEventListener('click', function () {
              buttons.forEach(function (b) { b.classList.remove('active'); });
              btn.classList.add('active');
              activeFilter = btn.dataset.filter;
              apply();
            });
          });

          if (hideUnchanged) hideUnchanged.addEventListener('change', apply);

          document.querySelectorAll('button.copy-path').forEach(function (btn) {
            btn.addEventListener('click', function (e) {
              e.preventDefault();
              const val = btn.dataset.value || '';
              const done = function () {
                const prev = btn.innerHTML;
                btn.innerHTML = '&#x2713;';
                setTimeout(function () { btn.innerHTML = prev; }, 1200);
              };
              if (navigator.clipboard && navigator.clipboard.writeText) {
                navigator.clipboard.writeText(val).then(done, done);
              } else {
                const ta = document.createElement('textarea');
                ta.value = val; document.body.appendChild(ta);
                ta.select();
                try { document.execCommand('copy'); } catch (e) {}
                document.body.removeChild(ta);
                done();
              }
            });
          });

          const colBtn = document.getElementById('columnsBtn');
          const colPanel = document.getElementById('columnsPanel');
          if (colBtn && colPanel) {
            colBtn.addEventListener('click', function (e) {
              e.stopPropagation();
              colPanel.hidden = !colPanel.hidden;
            });
            document.addEventListener('click', function (e) {
              if (!colPanel.hidden && !colPanel.contains(e.target) && e.target !== colBtn) {
                colPanel.hidden = true;
              }
            });
            // Column visibility
            const colToggles = document.querySelectorAll('.col-toggle');
            const selectAllCols = document.getElementById('selectAllCols');

            const updateColVisibility = (toggle) => {
              const colIdx = toggle.getAttribute('data-col');
              const cells = document.querySelectorAll(`td:nth-child(${colIdx}), th:nth-child(${colIdx})`);
              cells.forEach(c => c.style.display = toggle.checked ? '' : 'none');
            };

            colToggles.forEach(toggle => {
              toggle.addEventListener('change', () => {
                updateColVisibility(toggle);
                // Update "Select All" state
                const allChecked = Array.from(colToggles).every(t => t.checked);
                selectAllCols.checked = allChecked;
                selectAllCols.indeterminate = !allChecked && Array.from(colToggles).some(t => t.checked);
              });
            });

            selectAllCols.addEventListener('change', () => {
              colToggles.forEach(toggle => {
                toggle.checked = selectAllCols.checked;
                updateColVisibility(toggle);
              });
            });
          }
        })();
        </script>
        """;
}
