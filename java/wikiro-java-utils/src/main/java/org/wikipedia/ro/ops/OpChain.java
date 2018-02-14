package org.wikipedia.ro.ops;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

/**
 * An {@link Op} that chains together multiple ops and then executes them all in one edit.
 * 
 * @author acstroe
 *
 */
public class OpChain extends Op {

    public OpChain() {
        super(null);
    }

    private List<Op> ops = new ArrayList<>();
    
    public OpChain addOp(Op op) {
        if (null != page && page != op.page) {
            throw new IllegalArgumentException("Only ops on the same article can be added");
        }
        page = op.page;
        ops.add(op);
        return this;
    }
    
    public void changePageStructure() {
        if (0 == ops.size()) {
            return;
        }
        StringBuilder sbuild = new StringBuilder();
        boolean appendSummary = null == this.summary;
        if (appendSummary) {
            this.summary = "";
        }
        for (Op eachOp: ops) {
            String eachSummary = eachOp.getSummary();
            try {
                if (appendSummary && 255 < summary.getBytes("UTF-8").length + 2 + eachSummary.getBytes("UTF-8").length) {
                    sbuild.append("...");
                    appendSummary = false;
                }
            } catch (UnsupportedEncodingException e) {
                System.err.println("UTF-8 unsupported?? wtf???");
                e.printStackTrace();
            }
            if (appendSummary) {
                if (0 < sbuild.length()) {
                    sbuild.append("; ");
                }
                sbuild.append(eachSummary);
            }
            summary = sbuild.toString();
            eachOp.changePageStructure();
        }
    }

    
}
