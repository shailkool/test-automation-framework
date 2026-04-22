package com.automation.core.diff;

import lombok.extern.log4j.Log4j2;

import java.io.PrintStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Command-line entry point for the data-diff utility.
 *
 * <pre>{@code
 * diffcli file --old <path> --new <path> --keys <c1[,c2...]> --out <path>
 *              [--ignore-case] [--ignore <c1,c2>] [--title <name>]
 *
 * diffcli dir  --old-dir <dir> --new-dir <dir> --keys <c1[,c2...]> --out-dir <dir>
 *              [--pattern <glob>] [--recursive] [--ignore-case] [--ignore <c1,c2>]
 *              [--title <name>]
 * }</pre>
 *
 * <p>Exit codes: {@code 0} success/no-differences, {@code 1} differences
 * detected, {@code 2} invalid arguments or runtime error.
 */
@Log4j2
public final class DiffCli {

    private DiffCli() {
    }

    public static void main(String[] args) {
        int code = run(args, System.out, System.err);
        System.exit(code);
    }

    public static int run(String[] args, PrintStream out, PrintStream err) {
        if (args == null || args.length == 0 || isHelp(args[0])) {
            printUsage(out);
            return 0;
        }
        String cmd = args[0];
        Map<String, String> opts;
        try {
            opts = parseOpts(Arrays.copyOfRange(args, 1, args.length));
        } catch (IllegalArgumentException e) {
            err.println("Argument error: " + e.getMessage());
            printUsage(err);
            return 2;
        }

        try {
            switch (cmd) {
                case "file": return runFile(opts, out, err);
                case "dir":  return runDir(opts, out, err);
                case "help":
                case "--help":
                case "-h":
                    printUsage(out);
                    return 0;
                default:
                    err.println("Unknown command: " + cmd);
                    printUsage(err);
                    return 2;
            }
        } catch (IllegalArgumentException e) {
            err.println("Argument error: " + e.getMessage());
            return 2;
        } catch (RuntimeException e) {
            err.println("Error: " + e.getMessage());
            log.error("Unexpected failure", e);
            return 2;
        }
    }

    static int runFile(Map<String, String> opts, PrintStream out, PrintStream err) {
        Path oldFile = requirePath(opts, "old");
        Path newFile = requirePath(opts, "new");
        List<String> keys = requireKeys(opts);
        Path outPath = requirePath(opts, "out");
        boolean ignoreCase = opts.containsKey("ignore-case");
        List<String> ignoreFields = splitCsv(opts.get("ignore"));
        String title = opts.getOrDefault("title", defaultTitle(oldFile, newFile));

        List<Map<String, String>> oldData = CsvLoader.load(oldFile);
        List<Map<String, String>> newData = CsvLoader.load(newFile);
        CsvLoader.ensureKeyColumnsPresent(oldFile, keys, oldData);
        CsvLoader.ensureKeyColumnsPresent(newFile, keys, newData);

        DataDiff.Builder db = DataDiff.builder().keyFields(keys).ignoreCase(ignoreCase);
        for (String f : ignoreFields) db.ignoreField(f);
        DiffResult result = db.build().compare(oldData, newData);

        new DiffReportGenerator().saveReport(
            result,
            title,
            oldFile.toString(),
            newFile.toString(),
            outPath.toString()
        );

        out.printf("Compared %s vs %s - %s. Report: %s%n",
            oldFile, newFile, result.getSummary(), outPath.toAbsolutePath());

        return result.isIdentical() ? 0 : 1;
    }

    static int runDir(Map<String, String> opts, PrintStream out, PrintStream err) {
        Path oldDir = requirePath(opts, "old-dir");
        Path newDir = requirePath(opts, "new-dir");
        List<String> keys = requireKeys(opts);
        Path outDir = requirePath(opts, "out-dir");
        String pattern = opts.getOrDefault("pattern", "*.csv");
        boolean recursive = opts.containsKey("recursive");
        boolean ignoreCase = opts.containsKey("ignore-case");
        List<String> ignoreFields = splitCsv(opts.get("ignore"));
        String title = opts.getOrDefault("title", oldDir.getFileName() + " vs " + newDir.getFileName());

        Path perFileDir = outDir.resolve("files");

        DirectoryDiff.Builder b = DirectoryDiff.builder()
            .keyFields(keys)
            .ignoreCase(ignoreCase)
            .glob(pattern)
            .recursive(recursive)
            .reportTitle(title)
            .renderPerFileReports(perFileDir);
        for (String f : ignoreFields) b.ignoreField(f);

        DirectoryDiffResult result = b.build().compare(oldDir, newDir);

        Path indexPath = outDir.resolve("index.html");
        DirectoryDiffReportGenerator.saveIndex(result, title, indexPath);

        out.printf(
            "Compared %d files - identical: %d, different: %d, only-old: %d, only-new: %d. Index: %s%n",
            result.getTotalFiles(),
            result.getIdenticalFiles(),
            result.getFilesWithDifferences(),
            result.getOnlyOldFiles(),
            result.getOnlyNewFiles(),
            indexPath.toAbsolutePath()
        );

        return result.hasDifferences() ? 1 : 0;
    }

    // ---------- argument parsing helpers ----------

    static Map<String, String> parseOpts(String[] args) {
        Map<String, String> opts = new LinkedHashMap<>();
        int i = 0;
        while (i < args.length) {
            String a = args[i];
            if (!a.startsWith("--")) {
                throw new IllegalArgumentException("Expected --option, got '" + a + "'");
            }
            String name = a.substring(2);
            boolean hasValue = i + 1 < args.length && !args[i + 1].startsWith("--");
            if (hasValue) {
                opts.put(name, args[i + 1]);
                i += 2;
            } else {
                opts.put(name, "true"); // flag
                i += 1;
            }
        }
        return opts;
    }

    private static Path requirePath(Map<String, String> opts, String key) {
        String v = opts.get(key);
        if (v == null || v.isBlank()) {
            throw new IllegalArgumentException("Missing required --" + key);
        }
        return Paths.get(v);
    }

    private static List<String> requireKeys(Map<String, String> opts) {
        String raw = opts.get("keys");
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Missing required --keys <col[,col2...]>");
        }
        List<String> keys = splitCsv(raw);
        if (keys.isEmpty()) {
            throw new IllegalArgumentException("--keys must list at least one column");
        }
        return keys;
    }

    private static List<String> splitCsv(String raw) {
        if (raw == null || raw.isBlank()) return List.of();
        List<String> out = new ArrayList<>();
        for (String part : raw.split(",")) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) out.add(trimmed);
        }
        return out;
    }

    private static boolean isHelp(String a) {
        return "--help".equals(a) || "-h".equals(a) || "help".equals(a);
    }

    private static String defaultTitle(Path oldFile, Path newFile) {
        String oldName = oldFile.getFileName().toString();
        String newName = newFile.getFileName().toString();
        return oldName.equals(newName) ? oldName : oldName + " vs " + newName;
    }

    private static void printUsage(PrintStream s) {
        s.println("Usage:");
        s.println("  diffcli file --old <path> --new <path> --keys <c1[,c2...]> --out <path>");
        s.println("              [--ignore-case] [--ignore <c1,c2>] [--title <name>]");
        s.println();
        s.println("  diffcli dir  --old-dir <dir> --new-dir <dir> --keys <c1[,c2...]> --out-dir <dir>");
        s.println("              [--pattern <glob>] [--recursive] [--ignore-case]");
        s.println("              [--ignore <c1,c2>] [--title <name>]");
        s.println();
        s.println("Options:");
        s.println("  --keys         Comma-separated list of key columns used for row matching.");
        s.println("  --ignore       Comma-separated list of columns to ignore when comparing values.");
        s.println("  --ignore-case  Match keys and values case-insensitively.");
        s.println("  --pattern      Glob filter for directory mode (default: *.csv).");
        s.println("  --recursive    Walk subdirectories in directory mode.");
        s.println("  --title        Title shown at the top of the HTML report(s).");
        s.println();
        s.println("Exit codes: 0 = identical, 1 = differences found, 2 = invalid arguments or error.");
    }
}
