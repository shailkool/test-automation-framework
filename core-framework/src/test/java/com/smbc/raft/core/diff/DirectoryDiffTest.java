package com.smbc.raft.tests.diff;

import com.smbc.raft.core.diff.DirectoryDiff;
import com.smbc.raft.core.diff.DirectoryDiffReportGenerator;
import com.smbc.raft.core.diff.DirectoryDiffResult;
import com.smbc.raft.core.diff.FileDiffResult;
import com.smbc.raft.core.diff.FileStatus;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Tests for {@link DirectoryDiff} covering:
 *
 * <ul>
 *   <li>single and composite key matching across paired files,
 *   <li>only-old / only-new file detection,
 *   <li>glob filtering,
 *   <li>per-file HTML report generation driven by the builder, and
 *   <li>the aggregated summary index produced by {@link DirectoryDiffReportGenerator}.
 * </ul>
 */
public class DirectoryDiffTest {

  @Test(description = "Single-key diff across a directory; detects modified rows and orphan files")
  public void testSingleKeyDirectoryDiff() throws IOException {
    Path tmp = Files.createTempDirectory("dirdiff-single-");
    Path oldDir = Files.createDirectory(tmp.resolve("old"));
    Path newDir = Files.createDirectory(tmp.resolve("new"));

    writeCsv(
        oldDir.resolve("customers.csv"),
        List.of("id,name,status", "1,Alice,active", "2,Bob,active", "3,Charlie,inactive"));
    writeCsv(
        newDir.resolve("customers.csv"),
        List.of("id,name,status", "1,Alice,active", "2,Bob,suspended", "3,Charlie,inactive"));

    // only-old file
    writeCsv(oldDir.resolve("orphan_old.csv"), List.of("id,value", "1,a", "2,b"));
    // only-new file
    writeCsv(newDir.resolve("orphan_new.csv"), List.of("id,value", "9,z"));

    DirectoryDiffResult result =
        DirectoryDiff.builder().keyField("id").glob("*.csv").build().compare(oldDir, newDir);

    Assert.assertEquals(result.getTotalFiles(), 3, "three files across both sides");
    Assert.assertEquals(result.getFilesWithDifferences(), 1);
    Assert.assertEquals(result.getOnlyOldFiles(), 1);
    Assert.assertEquals(result.getOnlyNewFiles(), 1);
    Assert.assertEquals(result.getIdenticalFiles(), 0);

    FileDiffResult customers = findByName(result, "customers.csv");
    Assert.assertEquals(customers.getStatus(), FileStatus.DIFFERENT);
    Assert.assertEquals(customers.getChangedRows(), 1);
    Assert.assertEquals(customers.getUnchangedRows(), 2);
    Assert.assertEquals(customers.getOldRowCount(), 3);
    Assert.assertEquals(customers.getNewRowCount(), 3);

    FileDiffResult onlyOld = findByName(result, "orphan_old.csv");
    Assert.assertEquals(onlyOld.getStatus(), FileStatus.ONLY_OLD);
    Assert.assertEquals(onlyOld.getOldRowCount(), 2);
    Assert.assertEquals(onlyOld.getNewRowCount(), 0);

    FileDiffResult onlyNew = findByName(result, "orphan_new.csv");
    Assert.assertEquals(onlyNew.getStatus(), FileStatus.ONLY_NEW);
    Assert.assertEquals(onlyNew.getNewRowCount(), 1);
  }

  @Test(description = "Composite key: rows matched only when both key columns agree")
  public void testCompositeKeyDirectoryDiff() throws IOException {
    Path tmp = Files.createTempDirectory("dirdiff-composite-");
    Path oldDir = Files.createDirectory(tmp.resolve("old"));
    Path newDir = Files.createDirectory(tmp.resolve("new"));

    writeCsv(
        oldDir.resolve("people.csv"),
        List.of("first,last,age", "John,Doe,30", "Jane,Smith,25", "Alice,Jones,40"));
    writeCsv(
        newDir.resolve("people.csv"),
        List.of(
            "first,last,age",
            "John,Doe,31", // changed age
            "Jane,Smith,25", // unchanged
            "Bob,Brown,55" // added
            // Alice Jones removed
            ));

    DirectoryDiffResult result =
        DirectoryDiff.builder().keyField("first").keyField("last").build().compare(oldDir, newDir);

    FileDiffResult people = findByName(result, "people.csv");
    Assert.assertEquals(people.getStatus(), FileStatus.DIFFERENT);
    Assert.assertEquals(people.getChangedRows(), 1, "John Doe age changed");
    Assert.assertEquals(people.getUnchangedRows(), 1, "Jane Smith unchanged");
    Assert.assertEquals(people.getAddedRows(), 1, "Bob Brown added");
    Assert.assertEquals(people.getRemovedRows(), 1, "Alice Jones removed");
  }

  @Test(description = "Glob pattern filters which files are compared")
  public void testPatternFiltersNonMatchingFiles() throws IOException {
    Path tmp = Files.createTempDirectory("dirdiff-pattern-");
    Path oldDir = Files.createDirectory(tmp.resolve("old"));
    Path newDir = Files.createDirectory(tmp.resolve("new"));

    writeCsv(oldDir.resolve("COR-15.csv"), List.of("id,status", "1,A"));
    writeCsv(newDir.resolve("COR-15.csv"), List.of("id,status", "1,B"));

    // files that do not match the pattern - should be ignored entirely
    writeCsv(oldDir.resolve("XYZ-1.csv"), List.of("id,status", "1,A"));
    writeCsv(newDir.resolve("XYZ-1.csv"), List.of("id,status", "1,B"));

    DirectoryDiffResult result =
        DirectoryDiff.builder().keyField("id").glob("COR-*.csv").build().compare(oldDir, newDir);

    Assert.assertEquals(result.getTotalFiles(), 1, "only COR-* files should be included");
    Assert.assertEquals(findByName(result, "COR-15.csv").getStatus(), FileStatus.DIFFERENT);
  }

  @Test(description = "Per-file HTML detail reports written and summary index generated")
  public void testSummaryIndexAndPerFileReports() throws IOException {
    Path tmp = Files.createTempDirectory("dirdiff-summary-");
    Path oldDir = Files.createDirectory(tmp.resolve("old"));
    Path newDir = Files.createDirectory(tmp.resolve("new"));
    Path outDir = Files.createDirectory(tmp.resolve("out"));

    writeCsv(oldDir.resolve("a.csv"), List.of("id,v", "1,x", "2,y"));
    writeCsv(newDir.resolve("a.csv"), List.of("id,v", "1,x", "2,z"));
    writeCsv(oldDir.resolve("b.csv"), List.of("id,v", "1,a"));
    writeCsv(newDir.resolve("b.csv"), List.of("id,v", "1,a"));

    DirectoryDiffResult result =
        DirectoryDiff.builder()
            .keyField("id")
            .reportTitle("unit-test-run")
            .renderPerFileReports(outDir.resolve("files"))
            .build()
            .compare(oldDir, newDir);

    Path index = outDir.resolve("index.html");
    DirectoryDiffReportGenerator.saveIndex(result, "unit-test-run", index);

    Assert.assertTrue(Files.exists(index), "index.html should exist");
    String indexHtml = Files.readString(index);
    Assert.assertTrue(indexHtml.contains("Directory Diff"), "page title present");
    Assert.assertTrue(indexHtml.contains("a.csv"), "file rows listed");
    Assert.assertTrue(indexHtml.contains("b.csv"), "identical file listed too");
    Assert.assertTrue(indexHtml.contains("unit-test-run"), "custom title rendered");

    // The differing file should have produced a detail HTML
    FileDiffResult a = findByName(result, "a.csv");
    Assert.assertNotNull(a.getReportPath(), "diff detail generated for a.csv");
    Assert.assertTrue(Files.exists(Path.of(a.getReportPath())));

    // Identical files do not get a detail report
    FileDiffResult b = findByName(result, "b.csv");
    Assert.assertNull(b.getReportPath(), "no detail report for identical b.csv");
  }

  private static FileDiffResult findByName(DirectoryDiffResult r, String name) {
    return r.getFileResults().stream()
        .filter(f -> f.getFileName().equals(name))
        .findFirst()
        .orElseThrow(() -> new AssertionError("file not present in result: " + name));
  }

  private static void writeCsv(Path file, List<String> lines) throws IOException {
    Files.write(file, String.join("\n", lines).getBytes());
  }
}
