package com.automation.tests.diff;

import com.automation.core.diff.DiffCli;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Drives {@link DiffCli#run(String[], PrintStream, PrintStream)} end-to-end
 * and asserts on:
 * <ul>
 *   <li>exit-code contract (0 identical, 1 differences, 2 argument errors);</li>
 *   <li>HTML artefacts produced on disk for {@code file} and {@code dir}
 *       subcommands; and</li>
 *   <li>argument validation behaviour.</li>
 * </ul>
 */
public class DiffCliTest {

    @Test(description = "file subcommand: identical CSVs exit 0 and write the HTML report")
    public void fileSubcommand_identical_returnsZero() throws IOException {
        Path tmp = Files.createTempDirectory("cli-file-ident-");
        Path oldFile = tmp.resolve("a.csv");
        Path newFile = tmp.resolve("b.csv");
        Path out = tmp.resolve("report.html");
        writeCsv(oldFile, List.of("id,name", "1,Alice", "2,Bob"));
        writeCsv(newFile, List.of("id,name", "1,Alice", "2,Bob"));

        int code = DiffCli.run(
            new String[]{"file",
                "--old", oldFile.toString(),
                "--new", newFile.toString(),
                "--keys", "id",
                "--out", out.toString(),
                "--title", "ident-check"},
            quietStream(), quietStream()
        );

        Assert.assertEquals(code, 0, "identical inputs should exit 0");
        Assert.assertTrue(Files.exists(out), "HTML report written");
        String html = Files.readString(out);
        Assert.assertTrue(html.contains("ident-check"), "custom title present");
    }

    @Test(description = "file subcommand: differing CSVs exit 1 and still write the report")
    public void fileSubcommand_differences_returnsOne() throws IOException {
        Path tmp = Files.createTempDirectory("cli-file-diff-");
        Path oldFile = tmp.resolve("a.csv");
        Path newFile = tmp.resolve("b.csv");
        Path out = tmp.resolve("report.html");
        writeCsv(oldFile, List.of("id,name", "1,Alice", "2,Bob"));
        writeCsv(newFile, List.of("id,name", "1,Alice", "2,Robert"));

        int code = DiffCli.run(
            new String[]{"file",
                "--old", oldFile.toString(),
                "--new", newFile.toString(),
                "--keys", "id",
                "--out", out.toString()},
            quietStream(), quietStream()
        );

        Assert.assertEquals(code, 1, "differences should exit 1");
        Assert.assertTrue(Files.exists(out));
    }

    @Test(description = "file subcommand: composite keys supplied via comma list")
    public void fileSubcommand_compositeKeys() throws IOException {
        Path tmp = Files.createTempDirectory("cli-file-composite-");
        Path oldFile = tmp.resolve("a.csv");
        Path newFile = tmp.resolve("b.csv");
        Path out = tmp.resolve("report.html");
        writeCsv(oldFile, List.of("first,last,score", "John,Doe,10", "Jane,Doe,20"));
        // same keys, different scores -> modified rows, not mis-matched keys
        writeCsv(newFile, List.of("first,last,score", "John,Doe,11", "Jane,Doe,20"));

        int code = DiffCli.run(
            new String[]{"file",
                "--old", oldFile.toString(),
                "--new", newFile.toString(),
                "--keys", "first,last",
                "--out", out.toString()},
            quietStream(), quietStream()
        );

        Assert.assertEquals(code, 1);
        String html = Files.readString(out);
        Assert.assertTrue(html.contains("name='value'>first, last<") || html.contains("first, last"),
            "composite key list surfaced in report meta strip");
    }

    @Test(description = "dir subcommand: writes index.html + per-file reports")
    public void dirSubcommand_writesIndexAndDetail() throws IOException {
        Path tmp = Files.createTempDirectory("cli-dir-");
        Path oldDir = Files.createDirectory(tmp.resolve("old"));
        Path newDir = Files.createDirectory(tmp.resolve("new"));
        Path outDir = tmp.resolve("reports");

        writeCsv(oldDir.resolve("customers.csv"), List.of("id,name", "1,Alice", "2,Bob"));
        writeCsv(newDir.resolve("customers.csv"), List.of("id,name", "1,Alice", "2,Robert"));
        writeCsv(oldDir.resolve("identical.csv"), List.of("id,v", "1,x"));
        writeCsv(newDir.resolve("identical.csv"), List.of("id,v", "1,x"));
        writeCsv(oldDir.resolve("only_old.csv"), List.of("id", "9"));

        int code = DiffCli.run(
            new String[]{"dir",
                "--old-dir", oldDir.toString(),
                "--new-dir", newDir.toString(),
                "--keys", "id",
                "--out-dir", outDir.toString(),
                "--pattern", "*.csv"},
            quietStream(), quietStream()
        );

        Assert.assertEquals(code, 1, "at least one file differs");
        Path index = outDir.resolve("index.html");
        Assert.assertTrue(Files.exists(index), "index written");
        Assert.assertTrue(Files.exists(outDir.resolve("files/customers.csv.html")),
            "detail report for differing file");
        Assert.assertFalse(Files.exists(outDir.resolve("files/identical.csv.html")),
            "no detail report for identical file");
        String index_ = Files.readString(index);
        Assert.assertTrue(index_.contains("customers.csv"));
        Assert.assertTrue(index_.contains("only_old.csv"));
    }

    @Test(description = "missing required flag returns exit code 2")
    public void invalidArgs_returnTwo() {
        int code = DiffCli.run(
            new String[]{"file", "--old", "/tmp/a.csv"},
            quietStream(), quietStream()
        );
        Assert.assertEquals(code, 2);
    }

    @Test(description = "CSVs saved with a UTF-8 BOM still match on the first column")
    public void bomStrippedFromFirstHeader() throws IOException {
        Path tmp = Files.createTempDirectory("cli-bom-");
        Path oldFile = tmp.resolve("old.csv");
        Path newFile = tmp.resolve("new.csv");
        Path out = tmp.resolve("report.html");

        String oldBody = "﻿Word,Root or Prefix,Origin Language,Root Meaning,Grammatical Category\n"
            + "benevolent,bene-,Latin,good / well,Adjective\n"
            + "benedict,bene-,Latin,good / well,Noun\n"
            + "benediction,bene-,Latin,good / well,Noun\n"
            + "benign,bene-,Latin,good / well,Adjective\n";
        String newBody = "﻿Word,Root or Prefix,Origin Language,Root Meaning,Grammatical Category\n"
            + "benign,bene-,Latinx,good / well,Noun\n"
            + "benignsd,bene-,Latin,good / well,Adjective\n";

        Files.write(oldFile, oldBody.getBytes(StandardCharsets.UTF_8));
        Files.write(newFile, newBody.getBytes(StandardCharsets.UTF_8));

        int code = DiffCli.run(
            new String[]{"file",
                "--old", oldFile.toString(),
                "--new", newFile.toString(),
                "--keys", "Word",
                "--out", out.toString()},
            quietStream(), quietStream()
        );

        Assert.assertEquals(code, 1, "differences should exit 1");
        String html = Files.readString(out);
        Assert.assertTrue(html.contains("Generated"), "meta strip carries a Generated timestamp");
        // benevolent, benedict, benediction are in old but not new -> three removed rows rendered
        Assert.assertTrue(html.contains("benevolent") && html.contains("benedict")
                          && html.contains("benediction"),
            "all three old-only rows appear in the detail report");
        Assert.assertTrue(html.contains("benignsd"), "added row rendered");
        // benign appears on both sides with two changed fields - old pill carries 'Latin' / new 'Latinx'
        Assert.assertTrue(html.contains("Latinx"), "modified row shows the new value");
    }

    @Test(description = "Misspelt key column returns exit code 2 with a helpful message")
    public void missingKeyColumn_failsWithHelpfulError() throws IOException {
        Path tmp = Files.createTempDirectory("cli-missingkey-");
        Path oldFile = tmp.resolve("a.csv");
        Path newFile = tmp.resolve("b.csv");
        Path out = tmp.resolve("report.html");
        writeCsv(oldFile, List.of("id,name", "1,Alice"));
        writeCsv(newFile, List.of("id,name", "1,Alice"));

        ByteArrayOutputStream errBuf = new ByteArrayOutputStream();
        int code = DiffCli.run(
            new String[]{"file",
                "--old", oldFile.toString(),
                "--new", newFile.toString(),
                "--keys", "ID",   // uppercase, not present in header
                "--out", out.toString()},
            quietStream(), new PrintStream(errBuf)
        );

        Assert.assertEquals(code, 2);
        String err = errBuf.toString();
        Assert.assertTrue(err.contains("[ID]"), "error message names the missing key");
        Assert.assertTrue(err.contains("Available columns"), "error message shows actual headers");
    }

    @Test(description = "no args prints usage and returns 0")
    public void noArgs_printsUsage() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int code = DiffCli.run(new String[]{}, new PrintStream(out), quietStream());
        Assert.assertEquals(code, 0);
        Assert.assertTrue(out.toString().contains("Usage:"), "usage printed");
    }

    private static PrintStream quietStream() {
        return new PrintStream(new ByteArrayOutputStream());
    }

    private static void writeCsv(Path path, List<String> lines) throws IOException {
        Files.write(path, String.join("\n", lines).getBytes());
    }
}
