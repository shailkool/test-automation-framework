package com.automation.core.diff;

import lombok.extern.log4j.Log4j2;

import java.io.FileWriter;
import java.io.IOException;
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
     * Render the diff to an HTML string using the supplied page title and
     * old/new labels for the file-metadata strip.
     */
    public static String generateHtml(DataDiff diff, String reportTitle, String oldLabel, String newLabel) {
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

        appendTitle(html, reportTitle, oldCount, newCount, matchPct);
        appendSummaryCards(html, changed, added, removed, unchanged);
        appendMetaStrip(html, oldLabel, newLabel, diff.getKeyFields());
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
                                    int oldCount,
                                    int newCount,
                                    double matchPct) {
        String tone = matchPct >= 99.999 ? "high" : matchPct >= 90.0 ? "mid" : "low";
        html.append("<div class='title-bar'>\n");
        html.append("  <h1 class='title'>Diff <span class='dot'>·</span> ")
            .append(escapeHtml(reportTitle))
            .append("<span class='subtitle'>(old: ")
            .append(oldCount).append(" row").append(oldCount == 1 ? "" : "s")
            .append(" &rarr; new: ")
            .append(newCount).append(" row").append(newCount == 1 ? "" : "s")
            .append(")</span></h1>\n");
        html.append("  <span class='match-badge tone-").append(tone).append("'>")
            .append("<span class='bullet'></span>")
            .append(String.format("%.1f", matchPct)).append("% match")
            .append("</span>\n");
        html.append("</div>\n");
    }

    private static void appendSummaryCards(StringBuilder html, int changed, int added, int removed, int unchanged) {
        html.append("<div class='summary-cards'>\n");
        html.append(card("changed",   "&lt;/&gt;", "Changed",   changed));
        html.append(card("added",     "&plus;",    "Added",     added));
        html.append(card("removed",   "&minus;",   "Removed",   removed));
        html.append(card("unchanged", "&check;",   "Unchanged", unchanged));
        html.append("</div>\n");
    }

    private static String card(String kind, String glyph, String label, int count) {
        return String.format(
            "<div class='card card-%1$s'>"
            + "<span class='icon'>%2$s</span>"
            + "<div class='meta-col'><div class='label'>%3$s</div><div class='count'>%4$d</div></div>"
            + "</div>\n",
            kind, glyph, label, count
        );
    }

    private static void appendMetaStrip(StringBuilder html, String oldLabel, String newLabel, List<String> keyFields) {
        String keys = (keyFields == null || keyFields.isEmpty())
            ? "(none)"
            : String.join(", ", keyFields);

        html.append("<div class='meta'>\n");
        html.append("  <span class='pill'><span class='glyph'>&#x1F4C4;</span><span class='label'>Old</span>")
            .append("<span class='value'>").append(escapeHtml(oldLabel)).append("</span>")
            .append("<button class='copy' data-value='").append(escapeAttr(oldLabel)).append("'>Copy</button></span>\n");
        html.append("  <span class='pill'><span class='glyph'>&#x1F4C4;</span><span class='label'>New</span>")
            .append("<span class='value'>").append(escapeHtml(newLabel)).append("</span>")
            .append("<button class='copy' data-value='").append(escapeAttr(newLabel)).append("'>Copy</button></span>\n");
        html.append("  <span class='pill'><span class='glyph'>&#x1F511;</span><span class='label'>Keys</span>")
            .append("<span class='value'>").append(escapeHtml(keys)).append("</span></span>\n");
        html.append("</div>\n");
    }

    private static void appendToolbar(StringBuilder html,
                                      Set<String> columns,
                                      int changed, int added, int removed, int unchanged) {
        int total = changed + added + removed + unchanged;
        html.append("<div class='toolbar'>\n");
        html.append(filterButton("all",       "&#x25A6;", "All Rows",  total,     true));
        html.append(filterButton("modified",  "&lt;/&gt;", "Changed",  changed,   false));
        html.append(filterButton("added",     "&plus;",    "Added",    added,     false));
        html.append(filterButton("deleted",   "&minus;",   "Removed",  removed,   false));
        html.append(filterButton("unchanged", "&check;",   "Unchanged", unchanged, false));
        html.append("  <div class='spacer'></div>\n");
        html.append("  <label class='hide-unchanged'><input type='checkbox' id='hideUnchanged'>"
                  + "<span>Hide unchanged</span></label>\n");
        html.append("  <div class='columns-wrap'>");
        html.append("    <button class='filter-btn' id='columnsBtn' type='button'>"
                  + "<span class='g'>&#9776;</span>Columns</button>\n");
        html.append("    <div class='columns-panel' id='columnsPanel' hidden>");
        int colIndex = 2; // 1 = row icon column, 2+ = data columns
        for (String col : columns) {
            html.append("<label><input type='checkbox' class='col-toggle' data-col='")
                .append(colIndex++)
                .append("' checked> ")
                .append(escapeHtml(col))
                .append("</label>");
        }
        html.append("    </div>");
        html.append("  </div>\n");
        html.append("</div>\n");
    }

    private static String filterButton(String filter, String glyph, String label, int count, boolean active) {
        return String.format(
            "  <button class='filter-btn%1$s' data-filter='%2$s'>"
            + "<span class='g'>%3$s</span><span class='txt'>%4$s</span>"
            + "<span class='badge'>%5$d</span></button>\n",
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
                return val.isEmpty() ? "" : "<span class='cell-val pill-new'>" + escapeHtml(val) + "</span>";
            }
            case DELETED: {
                String val = left == null ? "" : left.getOrDefault(column, "");
                return val.isEmpty() ? "" : "<span class='cell-val pill-old'>" + escapeHtml(val) + "</span>";
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
            case MODIFIED:  return "&lt;/&gt;";
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
          :root {
            --bg:          #f6f8fa;
            --surface:     #ffffff;
            --border:      #e5e7eb;
            --text:        #1f2937;
            --muted:       #6b7280;
            --primary:     #0f5132;
            --primary-soft:#e6f2ec;

            --changed:      #4338ca;
            --changed-soft: #eef0fb;
            --changed-chip: #dfe3fb;
            --added:        #166534;
            --added-soft:   #e7f5ec;
            --added-chip:   #cbe8d4;
            --removed:      #b42318;
            --removed-soft: #fdecee;
            --removed-chip: #f6cfd3;
            --unchanged:    #15803d;
            --unchanged-soft:#ecf3ef;
            --unchanged-chip:#d3e4d8;

            --old-pill-bg:  #fde2e4;
            --old-pill-fg:  #8a1c23;
            --new-pill-bg:  #d7efdd;
            --new-pill-fg:  #14532d;

            --radius-sm: 6px;
            --radius-md: 10px;
            --radius-lg: 14px;
            --shadow-sm: 0 1px 2px rgba(15, 23, 42, 0.05);
            --shadow-md: 0 4px 10px rgba(15, 23, 42, 0.06), 0 0 0 1px rgba(15, 23, 42, 0.03);
          }

          * { margin: 0; padding: 0; box-sizing: border-box; }
          body {
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', sans-serif;
            color: var(--text);
            background:
              radial-gradient(1200px 400px at 0% -10%, rgba(15, 81, 50, 0.08), transparent 60%),
              radial-gradient(900px 320px at 100% -10%, rgba(67, 56, 202, 0.06), transparent 60%),
              linear-gradient(180deg, #f8fafc 0%, #ffffff 320px);
            padding: 32px 28px 56px;
            font-size: 14px;
            min-height: 100vh;
          }
          .container { max-width: 1400px; margin: 0 auto; }

          .title-bar {
            display: flex;
            align-items: center;
            justify-content: space-between;
            flex-wrap: wrap;
            gap: 14px;
            margin-bottom: 22px;
          }
          h1.title {
            color: var(--primary);
            font-size: 30px;
            font-weight: 800;
            display: flex;
            align-items: baseline;
            flex-wrap: wrap;
            gap: 10px;
            letter-spacing: -0.01em;
          }
          h1.title .dot { color: var(--primary); font-weight: 800; }
          h1.title .subtitle {
            font-size: 14px;
            color: var(--muted);
            font-weight: 500;
          }
          .match-badge {
            display: inline-flex;
            align-items: center;
            gap: 8px;
            padding: 7px 14px;
            border-radius: 999px;
            font-weight: 700;
            font-size: 13px;
            letter-spacing: 0.01em;
            box-shadow: var(--shadow-sm);
          }
          .match-badge .bullet {
            width: 8px;
            height: 8px;
            border-radius: 50%;
            display: inline-block;
          }
          .match-badge.tone-high { background: #dcfce7; color: #14532d; }
          .match-badge.tone-high .bullet { background: #16a34a; }
          .match-badge.tone-mid  { background: #fef3c7; color: #92400e; }
          .match-badge.tone-mid  .bullet { background: #d97706; }
          .match-badge.tone-low  { background: #fee2e2; color: #991b1b; }
          .match-badge.tone-low  .bullet { background: #dc2626; }

          .summary-cards {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
            gap: 14px;
            margin-bottom: 20px;
          }
          .card {
            background: var(--surface);
            border-radius: var(--radius-md);
            box-shadow: var(--shadow-md);
            padding: 16px 18px;
            display: flex;
            align-items: center;
            gap: 14px;
            border-left: 4px solid transparent;
            transition: transform 120ms ease, box-shadow 120ms ease;
          }
          .card:hover {
            transform: translateY(-1px);
            box-shadow: 0 10px 22px rgba(15, 23, 42, 0.08), 0 0 0 1px rgba(15, 23, 42, 0.04);
          }
          .card .icon {
            width: 40px; height: 40px; border-radius: 10px;
            display: flex; align-items: center; justify-content: center;
            font-size: 14px; font-weight: 800;
          }
          .card .label { font-size: 12px; color: var(--muted); font-weight: 600; letter-spacing: 0.04em; text-transform: uppercase; }
          .card .count { font-size: 26px; font-weight: 800; color: #0f172a; line-height: 1.1; margin-top: 2px; }

          .card-changed   { border-left-color: var(--changed); }
          .card-changed   .icon { background: var(--changed-chip); color: var(--changed); }
          .card-added     { border-left-color: var(--added); }
          .card-added     .icon { background: var(--added-chip); color: var(--added); }
          .card-removed   { border-left-color: var(--removed); }
          .card-removed   .icon { background: var(--removed-chip); color: var(--removed); }
          .card-unchanged { border-left-color: var(--unchanged); }
          .card-unchanged .icon { background: var(--unchanged-chip); color: var(--unchanged); }

          .meta {
            background: var(--surface);
            border: 1px solid var(--border);
            border-radius: var(--radius-md);
            padding: 12px 18px;
            display: flex;
            flex-wrap: wrap;
            gap: 28px;
            align-items: center;
            margin-bottom: 18px;
            font-size: 13px;
            color: var(--text);
            box-shadow: var(--shadow-sm);
          }
          .meta .pill { display: inline-flex; align-items: center; gap: 8px; }
          .meta .glyph { font-size: 14px; }
          .meta .label { font-weight: 600; color: var(--muted); text-transform: uppercase; letter-spacing: 0.04em; font-size: 11px; }
          .meta .value { color: #0f172a; font-family: ui-monospace, SFMono-Regular, Menlo, monospace; }
          .meta button.copy {
            padding: 3px 10px;
            border-radius: var(--radius-sm);
            border: 1px solid var(--border);
            background: white;
            cursor: pointer;
            font-size: 12px;
            color: var(--muted);
            transition: background 120ms ease, color 120ms ease, border-color 120ms ease;
          }
          .meta button.copy:hover { background: var(--primary-soft); border-color: var(--primary); color: var(--primary); }

          .toolbar {
            display: flex;
            gap: 10px;
            margin-bottom: 14px;
            flex-wrap: wrap;
            align-items: center;
          }
          .toolbar .spacer { flex: 1; }
          .filter-btn {
            padding: 7px 12px;
            border-radius: 999px;
            border: 1px solid var(--border);
            background: white;
            cursor: pointer;
            font-size: 13px;
            color: var(--text);
            display: inline-flex; align-items: center; gap: 8px;
            font-weight: 500;
            transition: background 120ms ease, color 120ms ease, border-color 120ms ease;
          }
          .filter-btn .g { font-weight: 700; color: var(--muted); }
          .filter-btn .badge {
            background: rgba(15, 23, 42, 0.08);
            padding: 1px 8px;
            border-radius: 999px;
            font-size: 11px;
            font-weight: 700;
            color: var(--muted);
          }
          .filter-btn:hover { background: #f3f4f6; }
          .filter-btn.active {
            background: var(--primary);
            color: white;
            border-color: var(--primary);
          }
          .filter-btn.active .g { color: white; }
          .filter-btn.active .badge {
            background: rgba(255, 255, 255, 0.22);
            color: white;
          }

          .hide-unchanged {
            display: inline-flex;
            align-items: center;
            gap: 8px;
            font-size: 13px;
            color: var(--muted);
            cursor: pointer;
            user-select: none;
          }
          .hide-unchanged input { accent-color: var(--primary); cursor: pointer; }

          .columns-wrap { position: relative; }
          .columns-panel {
            position: absolute;
            top: calc(100% + 6px);
            right: 0;
            background: white;
            border: 1px solid var(--border);
            border-radius: var(--radius-md);
            padding: 10px 14px;
            box-shadow: 0 12px 30px rgba(15, 23, 42, 0.10);
            z-index: 10;
            min-width: 200px;
            max-height: 320px;
            overflow-y: auto;
          }
          .columns-panel label {
            display: flex; align-items: center; gap: 8px;
            padding: 5px 0; font-size: 13px; cursor: pointer;
          }
          .columns-panel input { accent-color: var(--primary); }

          .table-wrap {
            border: 1px solid var(--border);
            border-radius: var(--radius-md);
            overflow: hidden;
            box-shadow: var(--shadow-md);
            background: white;
          }
          table {
            width: 100%;
            border-collapse: collapse;
            background: white;
            font-size: 13px;
          }
          thead th {
            background: #f8fafc;
            font-weight: 700;
            color: #334155;
            padding: 11px 14px;
            text-align: left;
            border-bottom: 1px solid var(--border);
            white-space: nowrap;
            position: sticky;
            top: 0;
            font-size: 12px;
            letter-spacing: 0.04em;
            text-transform: uppercase;
            z-index: 1;
          }
          tbody td {
            padding: 11px 14px;
            border-bottom: 1px solid var(--border);
            vertical-align: middle;
          }
          tbody tr:last-child td { border-bottom: none; }
          tbody tr:hover td { background: #fafcff; }
          tbody tr.row-unchanged:nth-child(even) td { background: #fbfcfd; }

          td.row-icon { width: 44px; text-align: center; color: var(--muted); padding-left: 10px; padding-right: 6px; }
          td.row-icon .dot {
            display: inline-flex; align-items: center; justify-content: center;
            width: 26px; height: 26px;
            border-radius: 50%;
            font-size: 12px; font-weight: 800;
          }

          tr.row-modified  { box-shadow: inset 3px 0 0 var(--changed); }
          tr.row-modified  td.row-icon .dot { background: var(--changed-soft); color: var(--changed); }
          tr.row-added     { box-shadow: inset 3px 0 0 var(--added); }
          tr.row-added     td.row-icon .dot { background: var(--added-soft); color: var(--added); }
          tr.row-deleted   { box-shadow: inset 3px 0 0 var(--removed); }
          tr.row-deleted   td.row-icon .dot { background: var(--removed-soft); color: var(--removed); }
          tr.row-unchanged td.row-icon .dot { background: var(--unchanged-soft); color: var(--unchanged); }

          .pill-old {
            display: inline-block;
            background: var(--old-pill-bg);
            color: var(--old-pill-fg);
            padding: 3px 10px;
            border-radius: 6px;
            margin-bottom: 4px;
            font-weight: 500;
          }
          .pill-new {
            display: inline-block;
            background: var(--new-pill-bg);
            color: var(--new-pill-fg);
            padding: 3px 10px;
            border-radius: 6px;
            font-weight: 500;
          }

          tbody td.empty {
            text-align: center;
            padding: 48px;
            color: var(--muted);
            font-style: italic;
          }

          @media (max-width: 720px) {
            body { padding: 20px 14px; }
            h1.title { font-size: 24px; }
            .summary-cards { grid-template-columns: repeat(2, 1fr); }
            .toolbar .spacer { flex: 0 0 100%; }
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

          document.querySelectorAll('button.copy').forEach(function (btn) {
            btn.addEventListener('click', function () {
              const val = btn.dataset.value || '';
              const done = function () {
                const prev = btn.textContent;
                btn.textContent = 'Copied';
                setTimeout(function () { btn.textContent = prev; }, 1200);
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
            document.querySelectorAll('.col-toggle').forEach(function (chk) {
              chk.addEventListener('change', function () {
                const idx = parseInt(chk.dataset.col, 10);
                const cells = document.querySelectorAll(
                  'table th:nth-child(' + idx + '), table td:nth-child(' + idx + ')'
                );
                cells.forEach(function (c) { c.style.display = chk.checked ? '' : 'none'; });
              });
            });
          }
        })();
        </script>
        """;
}
