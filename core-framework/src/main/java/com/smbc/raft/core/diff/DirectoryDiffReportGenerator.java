package com.smbc.raft.core.diff;

import lombok.extern.log4j.Log4j2;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Renders a {@link DirectoryDiffResult} as a standalone HTML summary page
 * ({@code index.html}) listing every matched file pair with status badges,
 * per-file counters, aggregate totals, and links out to the per-file detail
 * reports produced by {@link DiffReportGenerator}.
 *
 * <p>The summary uses the same colour palette, pill components, and
 * interaction patterns as {@link DiffHtmlReportGenerator} for visual
 * consistency across the two levels of report.
 */
@Log4j2
public final class DirectoryDiffReportGenerator {

    private DirectoryDiffReportGenerator() {
    }

    public static void saveIndex(DirectoryDiffResult result, String title, Path outputPath) {
        try {
            java.nio.file.Files.createDirectories(outputPath.toAbsolutePath().getParent());
        } catch (IOException e) {
            log.warn("Could not create parent directory for {}: {}", outputPath, e.getMessage());
        }
        String html = renderHtml(result, title, outputPath);
        try (FileWriter w = new FileWriter(outputPath.toFile())) {
            w.write(html);
            log.info("Directory diff summary written to {}", outputPath.toAbsolutePath());
        } catch (IOException e) {
            throw new RuntimeException("Failed to write summary report: " + outputPath, e);
        }
    }

    public static String renderHtml(DirectoryDiffResult result, String title, Path outputPath) {
        int total     = result.getTotalFiles();
        int identical = result.getIdenticalFiles();
        int differ    = result.getFilesWithDifferences();
        int onlyOld   = result.getOnlyOldFiles();
        int onlyNew   = result.getOnlyNewFiles();

        double matchPct = result.getOverallMatchPercentage();
        String tone = matchPct >= 99.999 ? "high" : matchPct >= 90.0 ? "mid" : "low";
        String pageTitle = (title == null || title.isBlank()) ? "Directory Diff" : title;

        StringBuilder html = new StringBuilder(12 * 1024);
        html.append("<!DOCTYPE html>\n<html lang='en'>\n<head>\n");
        html.append("<meta charset='UTF-8'>\n<meta name='viewport' content='width=device-width, initial-scale=1.0'>\n");
        html.append("<title>").append(escape(pageTitle)).append("</title>\n");
        html.append(STYLES);
        html.append("</head>\n<body>\n<div class='container'>\n");

        // title bar
        html.append("<div class='title-bar'>\n");
        html.append("  <h1 class='title'>Directory Diff <span class='dot'>·</span> ")
            .append(escape(pageTitle))
            .append("<span class='subtitle'>(")
            .append(total).append(" file").append(total == 1 ? "" : "s").append(" compared)</span></h1>\n");
        html.append("  <span class='match-badge tone-").append(tone).append("'>")
            .append("<span class='bullet'></span>")
            .append(String.format("%.1f", matchPct)).append("% rows match</span>\n");
        html.append("</div>\n");

        // summary cards (file-level)
        html.append("<div class='summary-cards'>\n");
        html.append(card("identical", "&check;",    "Identical",       identical));
        html.append(card("changed",   "&lt;/&gt;",  "With differences", differ));
        html.append(card("removed",   "&minus;",    "Only in old",      onlyOld));
        html.append(card("added",     "&plus;",     "Only in new",      onlyNew));
        html.append("</div>\n");

        // aggregate row-level counters
        html.append("<div class='row-totals'>\n");
        html.append("  <div class='tot tot-changed'><span class='label'>Changed rows</span><span class='val'>")
            .append(result.getTotalChangedRows()).append("</span></div>\n");
        html.append("  <div class='tot tot-added'><span class='label'>Added rows</span><span class='val'>")
            .append(result.getTotalAddedRows()).append("</span></div>\n");
        html.append("  <div class='tot tot-removed'><span class='label'>Removed rows</span><span class='val'>")
            .append(result.getTotalRemovedRows()).append("</span></div>\n");
        html.append("  <div class='tot tot-unchanged'><span class='label'>Unchanged rows</span><span class='val'>")
            .append(result.getTotalUnchangedRows()).append("</span></div>\n");
        html.append("</div>\n");

        // meta strip
        String keys = result.getKeyFields().isEmpty() ? "(none)" : String.join(", ", result.getKeyFields());
        html.append("<div class='meta'>\n");
        html.append("  <span class='pill'><span class='glyph'>&#x1F4C1;</span><span class='label'>Old dir</span>")
            .append("<span class='value'>").append(escape(result.getOldDir())).append("</span></span>\n");
        html.append("  <span class='pill'><span class='glyph'>&#x1F4C1;</span><span class='label'>New dir</span>")
            .append("<span class='value'>").append(escape(result.getNewDir())).append("</span></span>\n");
        html.append("  <span class='pill'><span class='glyph'>&#x1F50D;</span><span class='label'>Pattern</span>")
            .append("<span class='value'>").append(escape(result.getPattern())).append("</span></span>\n");
        html.append("  <span class='pill'><span class='glyph'>&#x1F511;</span><span class='label'>Keys</span>")
            .append("<span class='value'>").append(escape(keys)).append("</span></span>\n");
        html.append("  <span class='pill'><span class='glyph'>&#x1F552;</span><span class='label'>Generated</span>")
            .append("<span class='value'>")
            .append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
            .append("</span></span>\n");
        html.append("</div>\n");

        // toolbar (filter by file status)
        html.append("<div class='toolbar'>\n");
        html.append(filterBtn("all",       "&#x25A6;",    "All files",        total,     true));
        html.append(filterBtn("different", "&lt;/&gt;",   "With differences", differ,    false));
        html.append(filterBtn("identical", "&check;",     "Identical",        identical, false));
        html.append(filterBtn("only_old",  "&minus;",     "Only in old",      onlyOld,   false));
        html.append(filterBtn("only_new",  "&plus;",      "Only in new",      onlyNew,   false));
        html.append("  <div class='spacer'></div>\n");
        html.append("  <label class='hide-identical'><input type='checkbox' id='hideIdentical'>"
                  + "<span>Hide identical</span></label>\n");
        html.append("</div>\n");

        // table
        html.append("<div class='table-wrap'><table>\n<thead><tr>");
        html.append("<th class='row-icon'></th>");
        html.append("<th>File</th>");
        html.append("<th class='num'>Old rows</th>");
        html.append("<th class='num'>New rows</th>");
        html.append("<th class='num'>Changed</th>");
        html.append("<th class='num'>Added</th>");
        html.append("<th class='num'>Removed</th>");
        html.append("<th class='num'>Unchanged</th>");
        html.append("<th class='num'>Match</th>");
        html.append("<th>Detail</th>");
        html.append("</tr></thead>\n<tbody>\n");

        if (result.getFileResults().isEmpty()) {
            html.append("<tr><td colspan='10' class='empty'>No files to compare</td></tr>\n");
        }
        Path outputParent = outputPath == null ? null : outputPath.toAbsolutePath().getParent();
        for (FileDiffResult f : result.getFileResults()) {
            appendFileRow(html, f, outputParent);
        }
        html.append("</tbody>\n</table></div>\n");

        html.append("</div>\n");
        html.append(SCRIPT);
        html.append("</body>\n</html>");
        return html.toString();
    }

    private static void appendFileRow(StringBuilder html, FileDiffResult f, Path outputParent) {
        String kind = switch (f.getStatus()) {
            case IDENTICAL -> "identical";
            case DIFFERENT -> "different";
            case ONLY_OLD  -> "only_old";
            case ONLY_NEW  -> "only_new";
        };
        String glyph = switch (f.getStatus()) {
            case IDENTICAL -> "&check;";
            case DIFFERENT -> "&lt;/&gt;";
            case ONLY_OLD  -> "&minus;";
            case ONLY_NEW  -> "&plus;";
        };
        String badgeLabel = switch (f.getStatus()) {
            case IDENTICAL -> "Identical";
            case DIFFERENT -> "Different";
            case ONLY_OLD  -> "Only in old";
            case ONLY_NEW  -> "Only in new";
        };

        html.append("<tr class='row row-").append(kind)
            .append("' data-type='").append(kind).append("'>");
        html.append("<td class='row-icon'><span class='dot'>").append(glyph).append("</span></td>");
        html.append("<td class='file-cell'>")
            .append("<span class='fname'>").append(escape(f.getFileName())).append("</span>")
            .append("<span class='status-pill pill-").append(kind).append("'>").append(badgeLabel).append("</span>");
        if (f.getErrorMessage() != null) {
            html.append("<div class='err'>Error: ").append(escape(f.getErrorMessage())).append("</div>");
        }
        html.append("</td>");
        html.append("<td class='num'>").append(f.getOldRowCount()).append("</td>");
        html.append("<td class='num'>").append(f.getNewRowCount()).append("</td>");
        html.append("<td class='num'>").append(f.getChangedRows()).append("</td>");
        html.append("<td class='num'>").append(f.getAddedRows()).append("</td>");
        html.append("<td class='num'>").append(f.getRemovedRows()).append("</td>");
        html.append("<td class='num'>").append(f.getUnchangedRows()).append("</td>");
        html.append("<td class='num'>")
            .append(String.format("%.1f%%", f.getMatchPercentage()))
            .append("</td>");
        html.append("<td>");
        if (f.getReportPath() != null) {
            String href = relativizeHref(outputParent, f.getReportPath());
            html.append("<a class='detail-link' href='").append(escape(href)).append("'>View &rarr;</a>");
        } else {
            html.append("<span class='dim'>&mdash;</span>");
        }
        html.append("</td>");
        html.append("</tr>\n");
    }

    private static String relativizeHref(Path base, String target) {
        if (target == null) return "";
        Path abs = Paths.get(target).toAbsolutePath();
        if (base == null) return abs.toString();
        try {
            return base.relativize(abs).toString().replace('\\', '/');
        } catch (IllegalArgumentException e) {
            return abs.toString();
        }
    }

    private static String card(String kind, String glyph, String label, int count) {
        return String.format(
            "  <div class='card card-%1$s'>"
            + "<span class='icon'>%2$s</span>"
            + "<div class='meta-col'><div class='label'>%3$s</div><div class='count'>%4$d</div></div>"
            + "</div>\n",
            kind, glyph, label, count
        );
    }

    private static String filterBtn(String filter, String glyph, String label, int count, boolean active) {
        return String.format(
            "  <button class='filter-btn%1$s' data-filter='%2$s'>"
            + "<span class='g'>%3$s</span><span class='txt'>%4$s</span>"
            + "<span class='badge'>%5$d</span></button>\n",
            active ? " active" : "", filter, glyph, label, count
        );
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
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
            --identical:      #15803d;
            --identical-soft: #ecf3ef;
            --identical-chip: #d3e4d8;

            --radius-sm: 6px;
            --radius-md: 10px;
            --shadow-sm: 0 1px 2px rgba(15, 23, 42, 0.05);
            --shadow-md: 0 4px 10px rgba(15, 23, 42, 0.06), 0 0 0 1px rgba(15, 23, 42, 0.03);
          }

          * { margin: 0; padding: 0; box-sizing: border-box; }
          body {
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
            color: var(--text);
            background:
              radial-gradient(1200px 400px at 0% -10%, rgba(15, 81, 50, 0.08), transparent 60%),
              radial-gradient(900px 320px at 100% -10%, rgba(67, 56, 202, 0.06), transparent 60%),
              linear-gradient(180deg, #f8fafc 0%, #ffffff 320px);
            padding: 32px 28px 56px;
            font-size: 14px;
            min-height: 100vh;
          }
          .container { max-width: 1500px; margin: 0 auto; }

          .title-bar { display:flex; align-items:center; justify-content:space-between; flex-wrap:wrap; gap:14px; margin-bottom:22px; }
          h1.title { color: var(--primary); font-size: 30px; font-weight: 800; display:flex; align-items:baseline; gap:10px; flex-wrap:wrap; letter-spacing:-0.01em; }
          h1.title .dot { color: var(--primary); }
          h1.title .subtitle { font-size: 14px; color: var(--muted); font-weight: 500; }

          .match-badge {
            display:inline-flex; align-items:center; gap:8px;
            padding: 7px 14px; border-radius: 999px;
            font-weight: 700; font-size: 13px; letter-spacing: 0.01em;
            box-shadow: var(--shadow-sm);
          }
          .match-badge .bullet { width:8px; height:8px; border-radius:50%; display:inline-block; }
          .match-badge.tone-high { background:#dcfce7; color:#14532d; }
          .match-badge.tone-high .bullet { background:#16a34a; }
          .match-badge.tone-mid  { background:#fef3c7; color:#92400e; }
          .match-badge.tone-mid  .bullet { background:#d97706; }
          .match-badge.tone-low  { background:#fee2e2; color:#991b1b; }
          .match-badge.tone-low  .bullet { background:#dc2626; }

          .summary-cards { display:grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap:14px; margin-bottom: 20px; }
          .card {
            background: var(--surface); border-radius: var(--radius-md); box-shadow: var(--shadow-md);
            padding: 16px 18px; display:flex; align-items:center; gap:14px;
            border-left: 4px solid transparent;
            transition: transform 120ms ease, box-shadow 120ms ease;
          }
          .card:hover { transform: translateY(-1px); box-shadow: 0 10px 22px rgba(15, 23, 42, 0.08), 0 0 0 1px rgba(15, 23, 42, 0.04); }
          .card .icon { width:40px; height:40px; border-radius:10px; display:flex; align-items:center; justify-content:center; font-weight:800; font-size:14px; }
          .card .label { font-size:12px; color:var(--muted); font-weight:600; letter-spacing:0.04em; text-transform:uppercase; }
          .card .count { font-size:26px; font-weight:800; color:#0f172a; line-height:1.1; margin-top:2px; }
          .card-identical { border-left-color: var(--identical); }
          .card-identical .icon { background: var(--identical-chip); color: var(--identical); }
          .card-changed   { border-left-color: var(--changed); }
          .card-changed   .icon { background: var(--changed-chip); color: var(--changed); }
          .card-removed   { border-left-color: var(--removed); }
          .card-removed   .icon { background: var(--removed-chip); color: var(--removed); }
          .card-added     { border-left-color: var(--added); }
          .card-added     .icon { background: var(--added-chip); color: var(--added); }

          .row-totals { display:flex; flex-wrap:wrap; gap: 24px; padding: 14px 18px; background: var(--surface); border:1px solid var(--border); border-radius: var(--radius-md); margin-bottom: 18px; box-shadow: var(--shadow-sm); }
          .row-totals .tot { display:flex; flex-direction:column; }
          .row-totals .label { font-size:11px; color:var(--muted); font-weight:600; letter-spacing:0.04em; text-transform:uppercase; }
          .row-totals .val   { font-size:20px; font-weight:800; color:#0f172a; margin-top:2px; }
          .tot-changed   .val { color: var(--changed); }
          .tot-added     .val { color: var(--added); }
          .tot-removed   .val { color: var(--removed); }
          .tot-unchanged .val { color: var(--unchanged); }

          .meta {
            background: var(--surface); border:1px solid var(--border); border-radius: var(--radius-md);
            padding: 12px 18px; display:flex; flex-wrap:wrap; gap:28px; align-items:center;
            margin-bottom: 18px; font-size: 13px; color: var(--text); box-shadow: var(--shadow-sm);
          }
          .meta .pill { display:inline-flex; align-items:center; gap:8px; }
          .meta .glyph { font-size:14px; }
          .meta .label { font-weight:600; color:var(--muted); text-transform:uppercase; letter-spacing:0.04em; font-size:11px; }
          .meta .value { color:#0f172a; font-family: ui-monospace, SFMono-Regular, Menlo, monospace; }

          .toolbar { display:flex; gap:10px; margin-bottom:14px; flex-wrap:wrap; align-items:center; }
          .toolbar .spacer { flex:1; }
          .filter-btn {
            padding: 7px 12px; border-radius: 999px; border:1px solid var(--border); background:white;
            cursor:pointer; font-size:13px; color:var(--text); display:inline-flex; align-items:center; gap:8px;
            font-weight:500; transition: background 120ms ease, color 120ms ease, border-color 120ms ease;
          }
          .filter-btn .g { font-weight:700; color:var(--muted); }
          .filter-btn .badge { background: rgba(15, 23, 42, 0.08); padding: 1px 8px; border-radius: 999px; font-size:11px; font-weight:700; color:var(--muted); }
          .filter-btn:hover { background: #f3f4f6; }
          .filter-btn.active { background: var(--primary); color:white; border-color: var(--primary); }
          .filter-btn.active .g { color:white; }
          .filter-btn.active .badge { background: rgba(255, 255, 255, 0.22); color:white; }

          .hide-identical { display:inline-flex; align-items:center; gap:8px; font-size:13px; color:var(--muted); cursor:pointer; user-select:none; }
          .hide-identical input { accent-color: var(--primary); cursor:pointer; }

          .table-wrap { border:1px solid var(--border); border-radius: var(--radius-md); overflow:hidden; box-shadow: var(--shadow-md); background:white; }
          table { width:100%; border-collapse: collapse; background:white; font-size:13px; }
          thead th {
            background:#f8fafc; font-weight:700; color:#334155; padding: 11px 14px; text-align:left;
            border-bottom: 1px solid var(--border); white-space:nowrap; position: sticky; top:0;
            font-size:12px; letter-spacing: 0.04em; text-transform: uppercase; z-index: 1;
          }
          thead th.num { text-align: right; }
          tbody td { padding: 11px 14px; border-bottom: 1px solid var(--border); vertical-align: middle; }
          tbody td.num { text-align: right; font-variant-numeric: tabular-nums; }
          tbody tr:last-child td { border-bottom: none; }
          tbody tr:hover td { background: #fafcff; }

          td.row-icon { width: 44px; text-align:center; color: var(--muted); padding-left: 10px; padding-right: 6px; }
          td.row-icon .dot { display:inline-flex; align-items:center; justify-content:center; width: 26px; height: 26px; border-radius: 50%; font-size: 12px; font-weight: 800; }

          tr.row-identical { }
          tr.row-identical td.row-icon .dot { background: var(--identical-soft); color: var(--identical); }
          tr.row-different { box-shadow: inset 3px 0 0 var(--changed); }
          tr.row-different td.row-icon .dot { background: var(--changed-soft); color: var(--changed); }
          tr.row-only_old  { box-shadow: inset 3px 0 0 var(--removed); }
          tr.row-only_old  td.row-icon .dot { background: var(--removed-soft); color: var(--removed); }
          tr.row-only_new  { box-shadow: inset 3px 0 0 var(--added); }
          tr.row-only_new  td.row-icon .dot { background: var(--added-soft); color: var(--added); }

          td.file-cell { display:flex; align-items:center; gap:10px; flex-wrap:wrap; }
          td.file-cell .fname { font-weight:600; color:#0f172a; font-family: ui-monospace, SFMono-Regular, Menlo, monospace; }
          td.file-cell .err   { color: var(--removed); font-size: 12px; width: 100%; margin-top: 2px; }
          .status-pill { padding: 2px 10px; border-radius: 999px; font-size: 11px; font-weight: 700; letter-spacing: 0.02em; }
          .pill-identical { background: var(--identical-soft); color: var(--identical); }
          .pill-different { background: var(--changed-soft); color: var(--changed); }
          .pill-only_old  { background: var(--removed-soft); color: var(--removed); }
          .pill-only_new  { background: var(--added-soft); color: var(--added); }

          .detail-link { color: var(--primary); text-decoration: none; font-weight:600; }
          .detail-link:hover { text-decoration: underline; }
          .dim { color: var(--muted); }

          tbody td.empty { text-align:center; padding: 48px; color: var(--muted); font-style: italic; }

          @media (max-width: 820px) {
            body { padding: 20px 14px; }
            h1.title { font-size: 24px; }
            .summary-cards { grid-template-columns: repeat(2, 1fr); }
          }
        </style>
        """;

    private static final String SCRIPT = """
        <script>
        (function () {
          const buttons = document.querySelectorAll('.filter-btn[data-filter]');
          const rows = document.querySelectorAll('tbody tr.row');
          const hideIdentical = document.getElementById('hideIdentical');
          let activeFilter = 'all';

          function apply() {
            rows.forEach(function (r) {
              const t = r.dataset.type;
              let show = (activeFilter === 'all') || (activeFilter === t);
              if (hideIdentical && hideIdentical.checked && t === 'identical') show = false;
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

          if (hideIdentical) hideIdentical.addEventListener('change', apply);
        })();
        </script>
        """;
}
