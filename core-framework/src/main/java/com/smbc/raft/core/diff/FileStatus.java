package com.smbc.raft.core.diff;

/**
 * Outcome of comparing one pair of files inside a directory diff.
 *
 * <ul>
 *   <li>{@link #IDENTICAL} &mdash; files exist on both sides and contain no row-level differences.
 *   <li>{@link #DIFFERENT} &mdash; files exist on both sides but rows were added, deleted, or
 *       modified.
 *   <li>{@link #ONLY_OLD} &mdash; the file exists only in the old directory (i.e. it is missing
 *       from the new side).
 *   <li>{@link #ONLY_NEW} &mdash; the file exists only in the new directory.
 * </ul>
 */
public enum FileStatus {
  IDENTICAL,
  DIFFERENT,
  ONLY_OLD,
  ONLY_NEW
}
