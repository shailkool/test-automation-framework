# DataDiff

Compare two CSV files — or two directories of CSVs — and produce
self-contained, interactive HTML reports with per-cell highlights, filter
pills, column toggles, and summary counters. Runs as a **CLI**, via the
**Java API**, or from **Cucumber step definitions**.

Features at a glance:

- **Composite keys** (one or many columns) for row matching.
- `file` subcommand renders a single HTML diff.
- `dir` subcommand walks two directories with a glob filter and emits an
  `index.html` summary that links to per-file detail reports.
- Case-insensitive key/value matching and column ignoring.
- UTF-8 BOM tolerant; fails fast with a helpful message when a
  `--keys` column isn't present.
- Rich metadata strip per side (full path, creation time, byte size, row
  count) plus a "Generated" timestamp on every report.
- Exit codes: **0** identical, **1** differences, **2** argument/runtime error.

---

## 1. Quick start (CLI)

Build the shaded uber-jar:

```bash
mvn -pl core-framework -am -DskipTests package
# or just call the wrapper - it builds the jar on first use
./bin/diffcli --help
```

The jar is written to `core-framework/target/core-framework-1.0.0-cli.jar`
(~4 MB; bundles only log4j + opencsv).

### Compare two files

```bash
./bin/diffcli file \
  --old  expected.csv \
  --new  actual.csv \
  --keys id \
  --out  diff.html
```

Open `diff.html` in a browser. The report shows:

- Dark-green title `Diff · <name>` with an `(old: N rows → new: M rows)` subheader.
- Match-percentage badge (high / mid / low tones) in the top-right.
- Four summary cards: **Changed**, **Added**, **Removed**, **Unchanged**.
- Metadata strip listing **OLD** and **NEW** paths on their own lines with
  creation time, byte size, row count, plus a Copy-path button; a footer
  row underneath shows the **Keys** used for matching and the **Generated**
  timestamp.
- Filter toolbar pills: `All Rows · Changed · Added · Removed · Unchanged`,
  plus a **Hide unchanged** toggle and a **Columns** dropdown for per-column
  visibility.
- Unified table: each row carries an icon + accent stripe matching its type.
  Modified cells stack the old value (red pill) above the new value (green
  pill); added/removed rows show every cell as a pill.

### Compare two directories

```bash
./bin/diffcli dir \
  --old-dir expected/ \
  --new-dir actual/ \
  --keys    id \
  --pattern "*.csv" \
  --out-dir reports/
```

Output layout:

```
reports/
├── index.html        ← summary (match %, file cards, counts, table of files)
└── files/
    ├── customers.csv.html
    └── orders.csv.html
```

`index.html` lists every matched file with a status badge (**Identical /
Different / Only in old / Only in new**) and a **View →** link into the
per-file detail.

### Common options

| Flag             | Applies to | Description                                              |
|------------------|------------|----------------------------------------------------------|
| `--keys`         | both       | Comma-separated list of key columns (`id` or `id,version`). |
| `--ignore`       | both       | Comma-separated list of columns to ignore when comparing values. |
| `--ignore-case`  | both       | Match keys and values case-insensitively.                |
| `--pattern`      | `dir`      | Glob filter, default `*.csv` (e.g. `COR-*.csv`).         |
| `--recursive`    | `dir`      | Walk subdirectories as well.                             |
| `--title`        | both       | Title shown at the top of the HTML report(s).            |

### Composite keys

Pass a comma-separated list — matching is by the concatenation of the values:

```bash
./bin/diffcli file --old a.csv --new b.csv --keys "first,last" --out diff.html
./bin/diffcli dir  --old-dir expected --new-dir actual \
  --keys "product_id,region" --out-dir reports/
```

### Exit codes

| Code | Meaning                                                         |
|------|-----------------------------------------------------------------|
| 0    | No differences. Useful in CI (`diffcli ... || exit 1`).         |
| 1    | Differences detected; report(s) still written.                  |
| 2    | Argument error or runtime failure.                              |

### Troubleshooting

- **"Key column(s) [ID] not found"** — the `--keys` column doesn't exist in
  the CSV. The error lists the available columns; check the casing.
- **Everything shows as a single modified row** — your CSV has a UTF-8 BOM
  but an earlier diffcli version didn't strip it. Rebuild the jar
  (`DIFFCLI_REBUILD=1 ./bin/diffcli ...`) or delete
  `core-framework/target/core-framework-1.0.0-cli.jar` to force a rebuild.
- **File in old and new not paired** — both sides must have the same
  filename. In `dir` mode, filenames are matched exactly.

---

## 2. Java API

The CLI is a thin wrapper around `DataDiff`, `DirectoryDiff`, and
`DiffHtmlReportGenerator` in `com.smbc.raft.core.diff`.

### File-level comparison

```java
import com.smbc.raft.core.diff.*;

List<Map<String, String>> expected = /* load from anywhere */;
List<Map<String, String>> actual   = /* load from anywhere */;

DiffResult result = DataDiff.builder()
    .keyFields("first", "last")     // composite key
    .ignoreCase(true)                // case-insensitive keys + values
    .ignoreField("updated_at")       // skip volatile columns
    .build()
    .compare(expected, actual);

if (!result.isIdentical()) {
    new DiffReportGenerator().saveReport(
        result,
        "My diff",
        DiffSideInfo.forFile(Path.of("expected.csv"), expected.size()),
        DiffSideInfo.forFile(Path.of("actual.csv"),   actual.size()),
        "out.html"
    );
}
```

`DiffResult` exposes `getMatchedRows()`, `getAddedRows()`,
`getDeletedRows()`, `getModifiedRows()`, `getMatchPercentage()`, plus a
`getSummary()` one-liner.

### Directory-level comparison

```java
DirectoryDiffResult result = DirectoryDiff.builder()
    .keyField("id")
    .glob("COR-*.csv")
    .recursive(false)
    .renderPerFileReports(Path.of("reports/files"))
    .reportTitle("UAT regression")
    .build()
    .compare(Path.of("expected"), Path.of("actual"));

DirectoryDiffReportGenerator.saveIndex(
    result, "UAT regression", Path.of("reports/index.html")
);

for (FileDiffResult f : result.getFileResults()) {
    System.out.printf("%s - %s%n", f.getFileName(), f.getStatus());
}
```

`DirectoryDiffResult` exposes aggregate counts
(`getIdenticalFiles()`, `getFilesWithDifferences()`,
`getOnlyOldFiles()`, `getOnlyNewFiles()`, `getTotal…Rows()`,
`getOverallMatchPercentage()`) so you can drive custom CI logic.

### From Cucumber

`CsvFilterSteps` already wires DataDiff into the `the resulting data
should be:` step: when a datatable mismatch occurs it renders an HTML
diff, attaches it to the Scenario so the masterthought report embeds
it inline, and fails with a pointer to the file. See
`application-tests/src/test/java/com/smbc/raft/tests/bdd/steps/CsvFilterSteps.java`.

---

## 3. Reports in depth

Every HTML report is a single self-contained file (inline CSS + JS, no
external requests). You can email it, check it in, or open it straight off
a CI artefact store.

### File-level report

- Title: `Diff · <title>` with `(old: N rows → new: M rows)`.
- Match-percentage badge with green/amber/red tone bands
  (≥ 99.9 %, ≥ 90 %, &lt; 90 %).
- Summary cards with counts and coloured accent stripes.
- Metadata block:
  - `OLD` row: path, creation time, size, row count, Copy button.
  - `NEW` row: same.
  - Footer: `Keys` used + `Generated` timestamp.
- Toolbar: pill filters with live counts, Hide-unchanged toggle, Columns
  dropdown to hide/show individual columns at runtime.
- Table: coloured left accent stripe per row type, zebra striping on
  unchanged rows, sticky header; per-cell diff pills for modified rows.
- Column headers show the exact CSV column name (no case transform).

### Directory summary (`index.html`)

- Same palette and layout as the file report.
- Overall match percentage computed across every compared file pair.
- Four file-level cards (Identical / With differences / Only in old /
  Only in new).
- Aggregate row counters (Changed / Added / Removed / Unchanged).
- Metadata strip shows old dir, new dir, pattern, key fields, generated.
- Table of files; `View →` column links to the per-file detail.

---

## 4. Tests

TestNG tests live alongside the framework's BDD suite:

- `DataDiffTest` — core diff semantics, composite keys, builder options.
- `DirectoryDiffTest` — directory pairing, glob filters, summary
  generation.
- `DiffCliTest` — drives `DiffCli.run(...)` end-to-end, including the
  UTF-8 BOM regression and missing-key error path.

Run them directly (bypassing the framework's TestNG suite file):

```bash
mvn -pl application-tests -am test \
    -Dtest='DataDiffTest,DirectoryDiffTest,DiffCliTest' \
    -Dsurefire.suiteXmlFiles= -Dsurefire.failIfNoSpecifiedTests=false
```

---

## 5. Example

```bash
cat > expected.csv <<'EOF'
id,name,status
1,Alice,active
2,Bob,active
3,Charlie,inactive
EOF

cat > actual.csv <<'EOF'
id,name,status
1,Alice,active
2,Bob,suspended
4,Diana,active
EOF

./bin/diffcli file --old expected.csv --new actual.csv \
                   --keys id --out diff.html
# exit 1; diff.html shows:
#   Matched: 1, Added: 1, Deleted: 1, Modified: 1, Match %: 33.33
```
