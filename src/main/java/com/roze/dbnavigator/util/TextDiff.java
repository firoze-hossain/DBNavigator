package com.roze.dbnavigator.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Minimal line-level + word-level diff (LCS-based), used by the Local History
 * viewer to show a side-by-side comparison with word highlighting, similar to
 * an IDE's diff tool. This is a real, working diff — not a placeholder.
 */
public final class TextDiff {

    public enum LineKind { EQUAL, CHANGED, ADDED, REMOVED }

    /** One word/whitespace token, with whether it differs from the other side. */
    public record Segment(String text, boolean changed) {}

    /** One aligned row of the diff: either side may be empty for ADDED/REMOVED. */
    public record DiffLine(LineKind kind, List<Segment> left, List<Segment> right) {}

    private static final Pattern TOKEN = Pattern.compile("\\w+|\\W");

    private TextDiff() {}

    public static List<DiffLine> diffLines(String beforeText, String afterText) {
        String[] before = beforeText == null ? new String[0] : beforeText.split("\n", -1);
        String[] after = afterText == null ? new String[0] : afterText.split("\n", -1);

        int n = before.length, m = after.length;
        int[][] lcs = new int[n + 1][m + 1];
        for (int i = n - 1; i >= 0; i--) {
            for (int j = m - 1; j >= 0; j--) {
                lcs[i][j] = before[i].equals(after[j])
                        ? lcs[i + 1][j + 1] + 1
                        : Math.max(lcs[i + 1][j], lcs[i][j + 1]);
            }
        }

        List<DiffLine> raw = new ArrayList<>();
        int i = 0, j = 0;
        while (i < n && j < m) {
            if (before[i].equals(after[j])) {
                raw.add(new DiffLine(LineKind.EQUAL, one(before[i]), one(after[j])));
                i++; j++;
            } else if (lcs[i + 1][j] >= lcs[i][j + 1]) {
                raw.add(new DiffLine(LineKind.REMOVED, one(before[i]), null));
                i++;
            } else {
                raw.add(new DiffLine(LineKind.ADDED, null, one(after[j])));
                j++;
            }
        }
        while (i < n) { raw.add(new DiffLine(LineKind.REMOVED, one(before[i]), null)); i++; }
        while (j < m) { raw.add(new DiffLine(LineKind.ADDED, null, one(after[j]))); j++; }

        return mergeIntoChangedPairs(raw);
    }

    /** Turns an adjacent REMOVED-then-ADDED pair into a single CHANGED row with word-level diff. */
    private static List<DiffLine> mergeIntoChangedPairs(List<DiffLine> lines) {
        List<DiffLine> merged = new ArrayList<>();
        for (int k = 0; k < lines.size(); k++) {
            DiffLine cur = lines.get(k);
            if (cur.kind() == LineKind.REMOVED && k + 1 < lines.size()
                    && lines.get(k + 1).kind() == LineKind.ADDED) {
                String leftText = cur.left().get(0).text();
                String rightText = lines.get(k + 1).right().get(0).text();
                merged.add(wordDiff(leftText, rightText));
                k++;   // consumed the paired ADDED row too
            } else {
                merged.add(cur);
            }
        }
        return merged;
    }

    private static DiffLine wordDiff(String leftText, String rightText) {
        List<String> leftTokens = tokenize(leftText);
        List<String> rightTokens = tokenize(rightText);
        int n = leftTokens.size(), m = rightTokens.size();

        int[][] lcs = new int[n + 1][m + 1];
        for (int i = n - 1; i >= 0; i--) {
            for (int j = m - 1; j >= 0; j--) {
                lcs[i][j] = leftTokens.get(i).equals(rightTokens.get(j))
                        ? lcs[i + 1][j + 1] + 1
                        : Math.max(lcs[i + 1][j], lcs[i][j + 1]);
            }
        }

        List<Segment> leftSeg = new ArrayList<>();
        List<Segment> rightSeg = new ArrayList<>();
        int i = 0, j = 0;
        while (i < n && j < m) {
            if (leftTokens.get(i).equals(rightTokens.get(j))) {
                leftSeg.add(new Segment(leftTokens.get(i), false));
                rightSeg.add(new Segment(rightTokens.get(j), false));
                i++; j++;
            } else if (lcs[i + 1][j] >= lcs[i][j + 1]) {
                leftSeg.add(new Segment(leftTokens.get(i), true));
                i++;
            } else {
                rightSeg.add(new Segment(rightTokens.get(j), true));
                j++;
            }
        }
        while (i < n) { leftSeg.add(new Segment(leftTokens.get(i), true)); i++; }
        while (j < m) { rightSeg.add(new Segment(rightTokens.get(j), true)); j++; }

        return new DiffLine(LineKind.CHANGED, leftSeg, rightSeg);
    }

    private static List<Segment> one(String text) {
        List<Segment> list = new ArrayList<>();
        list.add(new Segment(text, false));
        return list;
    }

    private static List<String> tokenize(String text) {
        List<String> tokens = new ArrayList<>();
        Matcher matcher = TOKEN.matcher(text);
        while (matcher.find()) tokens.add(matcher.group());
        return tokens;
    }

    public static long countDifferences(List<DiffLine> lines) {
        return lines.stream().filter(l -> l.kind() != LineKind.EQUAL).count();
    }
}
