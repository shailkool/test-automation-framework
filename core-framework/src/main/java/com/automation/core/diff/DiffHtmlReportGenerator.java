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
 * <p>The report mirrors the conventional CSV-diff layout: a summary strip of
 * counters (Changed / Added / Removed / Unchanged), a file-metadata bar with
 * old and new source labels plus the key fields, pill-style filter buttons
 * ({@code All Rows}, {@code Changed}, {@code Added}, {@code Removed},
 * {@code Unchanged}, and {@code Columns}), and a single unified table that
 * highlights per-cell differences with stacked red/green pills.
 *
 * <p>The HTML is fully self-contained (inline CSS + JS with no external
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

        List<DiffRow> rows = diff.getAllRowsInOrder();
        Set<String> columns = collectColumns(rows);

        StringBuilder html = new StringBuilder(8 * 1024);
        html.append("<!DOCTYPE html>\n<html lang='en'>\n<head>\n");
        html.append("<meta charset='UTF-8'>\n");
        html.append("<meta name='viewport' content='width=device-width, initial-scale=1.0'>\n");
        html.append("<title>Diff · ").append(escapeHtml(reportTitle)).append("</title>\n");
        html.append(STYLES);
        html.append("</head>\n<body>\n<div class='container'>\n");

        appendTitle(html, reportTitle, oldCount, newCount);
        appendSummaryCards(html, changed, added, removed, unchanged);
        appendMetaStrip(html, oldLabel, newLabel, diff.getKeyFields());
        appendToolbar(html, columns);
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

    private static void appendTitle(StringBuilder html, String reportTitle, int oldCount, int newCount) {
        html.append("<h1 class='title'>Diff <span class='dot'>·</span> ")
            .append(escapeHtml(reportTitle))
            .append("<span class='subtitle'>(old: ")
            .append(oldCount).append(" row").append(oldCount == 1 ? "" : "s")
            .append(" &rarr; new: ")
            .append(newCount).append(" row").append(newCount == 1 ? "" : "s")
            .append(")</span></h1>\n");
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
            "<div class='card card-%1$s'><span class='icon'>%2$s</span>"
            + "<div><div class='label'>%3$s</div><div class='count'>%4$d</div></div></div>\n",
            kind, glyph, label, count
        );
    }

    private static void appendMetaStrip(StringBuilder html, String oldLabel, String newLabel, List<String> keyFields) {
        String keys = (keyFields == null || keyFields.isEmpty())
            ? "(none)"
            : String.join(", ", keyFields);

        html.append("<div class='meta'>\n");
        html.append("<span class='pill'><span class='glyph'>&#x1F4C4;</span><span class='label'>Old</span>")
            .append("<span class='value'>").append(escapeHtml(oldLabel)).append("</span>")
            .append("<button class='copy' data-value='").append(escapeAttr(oldLabel)).append("'>Copy</button></span>\n");
        html.append("<span class='pill'><span class='glyph'>&#x1F4C4;</span><span class='label'>New</span>")
            .append("<span class='value'>").append(escapeHtml(newLabel)).append("</span>")
            .append("<button class='copy' data-value='").append(escapeAttr(newLabel)).append("'>Copy</button></span>\n");
        html.append("<span class='pill'><span class='glyph'>&#x1F511;</span><span class='label'>Keys</span>")
            .append("<span class='value'>").append(escapeHtml(keys)).append("</span></span>\n");
        html.append("</div>\n");
    }

    private static void appendToolbar(StringBuilder html, Set<String> columns) {
        html.append("<div class='toolbar'>\n");
        html.append("<button class='filter-btn active' data-filter='all'><span class='g'>&#x25A6;</span>All Rows</button>\n");
        html.append("<button class='filter-btn' data-filter='modified'><span class='g'>&lt;/&gt;</span>Changed</button>\n");
        html.append("<button class='filter-btn' data-filter='added'><span class='g'>&plus;</span>Added</button>\n");
        html.append("<button class='filter-btn' data-filter='deleted'><span class='g'>&minus;</span>Removed</button>\n");
        html.append("<button class='filter-btn' data-filter='unchanged'><span class='g'>&check;</span>Unchanged</button>\n");
        html.append("<div class='columns-wrap'>");
        html.append("<button class='filter-btn' id='columnsBtn' type='button'><span class='g'>&#9776;</span>Columns</button>\n");
        html.append("<div class='columns-panel' id='columnsPanel' hidden>");
        int colIndex = 3; // 1 = icon, 2 = key, 3+ = data columns
        for (String col : columns) {
            html.append("<label><input type='checkbox' class='col-toggle' data-col='")
                .append(colIndex++)
                .append("' checked> ")
                .append(escapeHtml(col))
                .append("</label>");
        }
        html.append("</div></div>\n");
        html.append("</div>\n");
        html.append("<label class='hide-unchanged'><input type='checkbox' id='hideUnchanged'> Hide unchanged</label>\n");
    }

    private static void appendTable(StringBuilder html, List<DiffRow> rows, Set<String> columns) {
        html.append("<div class='table-wrap'><table>\n<thead><tr>");
        html.append("<th class='row-icon'></th>");
        html.append("<th>Key</th>");
        for (String col : columns) {
            html.append("<th>").append(escapeHtml(col)).append("</th>");
        }
        html.append("</tr></thead>\n<tbody>\n");

        if (rows.isEmpty()) {
            html.append("<tr><td colspan='").append(columns.size() + 2)
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
        html.append("<td class='key-cell'>").append(escapeHtml(row.getKey())).append("</td>");

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
          * { margin: 0; padding: 0; box-sizing: border-box; }
          body {
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
            color: #1f2937;
            background: #ffffff;
            padding: 28px;
            font-size: 14px;
          }
          .container { max-width: 1400px; margin: 0 auto; }

          h1.title {
            color: #0f5132;
            font-size: 30px;
            font-weight: 800;
            margin-bottom: 18px;
            display: flex;
            align-items: baseline;
            flex-wrap: wrap;
            gap: 8px;
          }
          h1.title .dot { color: #0f5132; font-weight: 800; }
          h1.title .subtitle {
            font-size: 14px;
            color: #6b7280;
            font-weight: 400;
            margin-left: 6px;
          }

          .summary-cards {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
            gap: 14px;
            margin-bottom: 18px;
          }
          .card {
            display: flex;
            align-items: center;
            gap: 14px;
            padding: 14px 18px;
            border-radius: 10px;
          }
          .card .icon {
            width: 34px; height: 34px; border-radius: 50%;
            display: flex; align-items: center; justify-content: center;
            font-size: 14px; font-weight: 700;
          }
          .card .label { font-size: 12px; color: #6b7280; text-transform: none; }
          .card .count { font-size: 24px; font-weight: 800; color: #111827; line-height: 1; margin-top: 4px; }

          .card-changed   { background: #eef0fb; }
          .card-changed   .icon { background: #dfe3fb; color: #3730a3; }
          .card-added     { background: #e7f5ec; }
          .card-added     .icon { background: #cbe8d4; color: #166534; }
          .card-removed   { background: #fce9eb; }
          .card-removed   .icon { background: #f6cfd3; color: #991b1b; }
          .card-unchanged { background: #ecf3ef; }
          .card-unchanged .icon { background: #d3e4d8; color: #15803d; }

          .meta {
            background: #f4f6f8;
            border-radius: 10px;
            padding: 12px 18px;
            display: flex;
            flex-wrap: wrap;
            gap: 28px;
            align-items: center;
            margin-bottom: 18px;
            font-size: 13px;
            color: #374151;
          }
          .meta .pill { display: inline-flex; align-items: center; gap: 8px; }
          .meta .glyph { font-size: 14px; }
          .meta .label { font-weight: 600; color: #4b5563; }
          .meta .value { color: #111827; font-family: ui-monospace, SFMono-Regular, Menlo, monospace; }
          .meta button.copy {
            padding: 3px 10px;
            border-radius: 6px;
            border: 1px solid #d1d5db;
            background: white;
            cursor: pointer;
            font-size: 12px;
            color: #374151;
          }
          .meta button.copy:hover { background: #f9fafb; }

          .toolbar {
            display: flex;
            gap: 10px;
            margin-bottom: 10px;
            flex-wrap: wrap;
            align-items: center;
          }
          .filter-btn {
            padding: 8px 14px;
            border-radius: 8px;
            border: 1px solid #d1d5db;
            background: white;
            cursor: pointer;
            font-size: 13px;
            color: #1f2937;
            display: inline-flex; align-items: center; gap: 8px;
            font-weight: 500;
          }
          .filter-btn .g { font-weight: 700; color: #6b7280; }
          .filter-btn:hover { background: #f9fafb; }
          .filter-btn.active { background: #0f5132; color: white; border-color: #0f5132; }
          .filter-btn.active .g { color: white; }

          .columns-wrap { position: relative; }
          .columns-panel {
            position: absolute;
            top: calc(100% + 6px);
            right: 0;
            background: white;
            border: 1px solid #e5e7eb;
            border-radius: 8px;
            padding: 10px 14px;
            box-shadow: 0 4px 14px rgba(0,0,0,0.08);
            z-index: 10;
            min-width: 180px;
            max-height: 280px;
            overflow-y: auto;
          }
          .columns-panel label {
            display: flex;
            align-items: center;
            gap: 8px;
            padding: 4px 0;
            font-size: 13px;
            cursor: pointer;
          }

          .hide-unchanged {
            display: inline-flex;
            align-items: center;
            gap: 8px;
            margin-bottom: 14px;
            font-size: 13px;
            color: #374151;
            cursor: pointer;
          }
          .hide-unchanged input { cursor: pointer; }

          .table-wrap {
            border: 1px solid #e5e7eb;
            border-radius: 10px;
            overflow: hidden;
          }
          table {
            width: 100%;
            border-collapse: collapse;
            background: white;
            font-size: 13px;
          }
          thead th {
            background: #f8fafc;
            font-weight: 600;
            color: #0f172a;
            padding: 10px 12px;
            text-align: left;
            border-bottom: 1px solid #e5e7eb;
            white-space: nowrap;
          }
          tbody td {
            padding: 10px 12px;
            border-bottom: 1px solid #e5e7eb;
            vertical-align: middle;
          }
          tbody tr:last-child td { border-bottom: none; }
          tbody tr:hover td { background: #fcfdff; }

          td.row-icon { width: 36px; text-align: center; color: #6b7280; }
          td.row-icon .dot {
            display: inline-flex;
            align-items: center;
            justify-content: center;
            font-size: 13px;
            font-weight: 700;
            line-height: 1;
          }
          tr.row-modified  td.row-icon .dot { color: #3730a3; }
          tr.row-added     td.row-icon .dot { color: #166534; }
          tr.row-deleted   td.row-icon .dot { color: #991b1b; }
          tr.row-unchanged td.row-icon .dot { color: #15803d; }

          td.key-cell { font-weight: 600; color: #0f172a; }

          .pill-old {
            display: inline-block;
            background: #fce3e6;
            color: #8b1c23;
            padding: 2px 10px;
            border-radius: 5px;
            margin-bottom: 4px;
          }
          .pill-new {
            display: inline-block;
            background: #d6efdb;
            color: #14532d;
            padding: 2px 10px;
            border-radius: 5px;
          }

          tr.row-deleted .cell-val.pill-old,
          tr.row-added   .cell-val.pill-new {
            padding: 2px 10px;
            border-radius: 5px;
          }

          tbody td.empty {
            text-align: center;
            padding: 40px;
            color: #6b7280;
            font-style: italic;
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
