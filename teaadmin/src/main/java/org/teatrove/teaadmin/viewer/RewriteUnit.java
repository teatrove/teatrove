package org.teatrove.teaadmin.viewer;

import java.util.ArrayList;
import java.util.List;

import org.teatrove.tea.compiler.SourceInfo;

public class RewriteUnit {
    private StringBuilder data;
    private List<RewriteState> rewrites = new ArrayList<RewriteState>(100);

    public RewriteUnit(StringBuilder data) {
        this.data = data;
    }

    public StringBuilder getSource() {
        return this.data;
    }
    
    public boolean hasRewrites() {
        return !rewrites.isEmpty();
    }

    public boolean hasRead(SourceInfo source) {
        int start = source.getStartPosition();
        int end = source.getEndPosition();
        if (start > end) {
            throw new IllegalStateException("invalid start/end: " + start + '/' + end);
        }

        for (RewriteState existing : this.rewrites) {
            if (start == existing.start && end == existing.end) {
                return true;
            }
        }

        return false;
    }

    public String read(SourceInfo source) {
        int start = source.getStartPosition();
        int end = source.getEndPosition();
        if (start > end) {
            throw new IllegalStateException("invalid start/end: " + start + '/' + end);
        }

        int soffset = getOffset(start);
        int eoffset = getOffset(end);
        return this.data.substring(start + soffset, end + eoffset + 1);
    }

    public void rewrite(Rewrite rewrite) {
        // ignore if existing rewrite
        SourceInfo source = rewrite.getSourceInfo();
        int start = source.getStartPosition();
        int end = source.getEndPosition();
        if (start > end) {
            throw new IllegalStateException("invalid start/end: " + start + '/' + end);
        }

        for (RewriteState existing : this.rewrites) {
            if (start == existing.start && end == existing.end) {
                return;
            }
        }

        String result = rewrite.getValue();
        int soffset = getOffset(start);
        int eoffset = getOffset(end);
        this.data.replace(start + soffset, end + eoffset + 1, result);

        // remove indexes fully overwrittem
        overwriteOffsets(start, end);

        // add to list
        this.rewrites.add(new RewriteState(start, end, result.length()));
    }

    protected void overwriteOffsets(int start, int end) {
        for (RewriteState rewrite : this.rewrites) {
            if (start <= rewrite.start && end >= rewrite.end) {
                rewrite.valid = false;
            }
        }
    }

    protected int getOffset(int index) {
        int offset = 0;
        for (RewriteState rewrite : this.rewrites) {
            if (rewrite.valid && index > rewrite.end) {
                offset += (rewrite.count - (rewrite.end - rewrite.start + 1));
            }
        }

        return offset;
    }

    public static class RewriteState {
        int start;
        int end;
        int count;
        boolean valid;

        public RewriteState(int start, int end, int count) {
            this.start = start;
            this.end = end;
            this.count = count;
            this.valid = true;
        }
    }
}
