package com.android.dx.ssa;

import com.android.dx.ssa.DomFront.DomInfo;
import com.android.dx.ssa.SsaBasicBlock.Visitor;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashSet;

public final class Dominators {
    private final ArrayList<SsaBasicBlock> blocks;
    private final DomInfo[] domInfos;
    private final DFSInfo[] info = new DFSInfo[(this.blocks.size() + 2)];
    private final SsaMethod meth;
    private final boolean postdom;
    private final ArrayList<SsaBasicBlock> vertex = new ArrayList();

    private static final class DFSInfo {
        public SsaBasicBlock ancestor;
        public ArrayList<SsaBasicBlock> bucket = new ArrayList();
        public SsaBasicBlock parent;
        public SsaBasicBlock rep;
        public int semidom;
    }

    private class DfsWalker implements Visitor {
        private int dfsNum;

        private DfsWalker() {
            this.dfsNum = 0;
        }

        public void visitBlock(SsaBasicBlock v, SsaBasicBlock parent) {
            DFSInfo bbInfo = new DFSInfo();
            int i = this.dfsNum + 1;
            this.dfsNum = i;
            bbInfo.semidom = i;
            bbInfo.rep = v;
            bbInfo.parent = parent;
            Dominators.this.vertex.add(v);
            Dominators.this.info[v.getIndex()] = bbInfo;
        }
    }

    private Dominators(SsaMethod meth, DomInfo[] domInfos, boolean postdom) {
        this.meth = meth;
        this.domInfos = domInfos;
        this.postdom = postdom;
        this.blocks = meth.getBlocks();
    }

    public static Dominators make(SsaMethod meth, DomInfo[] domInfos, boolean postdom) {
        Dominators result = new Dominators(meth, domInfos, postdom);
        result.run();
        return result;
    }

    private BitSet getSuccs(SsaBasicBlock block) {
        if (this.postdom) {
            return block.getPredecessors();
        }
        return block.getSuccessors();
    }

    private BitSet getPreds(SsaBasicBlock block) {
        if (this.postdom) {
            return block.getSuccessors();
        }
        return block.getPredecessors();
    }

    private void compress(SsaBasicBlock in) {
        if (this.info[this.info[in.getIndex()].ancestor.getIndex()].ancestor != null) {
            ArrayList<SsaBasicBlock> worklist = new ArrayList();
            HashSet<SsaBasicBlock> visited = new HashSet();
            worklist.add(in);
            while (!worklist.isEmpty()) {
                int wsize = worklist.size();
                DFSInfo vbbInfo = this.info[((SsaBasicBlock) worklist.get(wsize - 1)).getIndex()];
                SsaBasicBlock vAncestor = vbbInfo.ancestor;
                DFSInfo vabbInfo = this.info[vAncestor.getIndex()];
                if (!visited.add(vAncestor) || vabbInfo.ancestor == null) {
                    worklist.remove(wsize - 1);
                    if (vabbInfo.ancestor != null) {
                        SsaBasicBlock vAncestorRep = vabbInfo.rep;
                        if (this.info[vAncestorRep.getIndex()].semidom < this.info[vbbInfo.rep.getIndex()].semidom) {
                            vbbInfo.rep = vAncestorRep;
                        }
                        vbbInfo.ancestor = vabbInfo.ancestor;
                    }
                } else {
                    worklist.add(vAncestor);
                }
            }
        }
    }

    private SsaBasicBlock eval(SsaBasicBlock v) {
        DFSInfo bbInfo = this.info[v.getIndex()];
        if (bbInfo.ancestor == null) {
            return v;
        }
        compress(v);
        return bbInfo.rep;
    }

    private void run() {
        SsaBasicBlock root;
        int i;
        if (this.postdom) {
            root = this.meth.getExitBlock();
        } else {
            root = this.meth.getEntryBlock();
        }
        if (root != null) {
            this.vertex.add(root);
            this.domInfos[root.getIndex()].idom = root.getIndex();
        }
        DfsWalker dfsWalker = new DfsWalker();
        this.meth.forEachBlockDepthFirst(this.postdom, dfsWalker);
        int dfsMax = this.vertex.size() - 1;
        for (i = dfsMax; i >= 2; i--) {
            SsaBasicBlock w = (SsaBasicBlock) this.vertex.get(i);
            DFSInfo wInfo = this.info[w.getIndex()];
            BitSet preds = getPreds(w);
            for (int j = preds.nextSetBit(0); j >= 0; j = preds.nextSetBit(j + 1)) {
                SsaBasicBlock predBlock = (SsaBasicBlock) this.blocks.get(j);
                if (this.info[predBlock.getIndex()] != null) {
                    int predSemidom = this.info[eval(predBlock).getIndex()].semidom;
                    if (predSemidom < wInfo.semidom) {
                        wInfo.semidom = predSemidom;
                    }
                }
            }
            this.info[((SsaBasicBlock) this.vertex.get(wInfo.semidom)).getIndex()].bucket.add(w);
            wInfo.ancestor = wInfo.parent;
            ArrayList<SsaBasicBlock> wParentBucket = this.info[wInfo.parent.getIndex()].bucket;
            while (!wParentBucket.isEmpty()) {
                SsaBasicBlock last = (SsaBasicBlock) wParentBucket.remove(wParentBucket.size() - 1);
                SsaBasicBlock U = eval(last);
                if (this.info[U.getIndex()].semidom < this.info[last.getIndex()].semidom) {
                    this.domInfos[last.getIndex()].idom = U.getIndex();
                } else {
                    this.domInfos[last.getIndex()].idom = wInfo.parent.getIndex();
                }
            }
        }
        for (i = 2; i <= dfsMax; i++) {
            w = (SsaBasicBlock) this.vertex.get(i);
            if (this.domInfos[w.getIndex()].idom != ((SsaBasicBlock) this.vertex.get(this.info[w.getIndex()].semidom)).getIndex()) {
                this.domInfos[w.getIndex()].idom = this.domInfos[this.domInfos[w.getIndex()].idom].idom;
            }
        }
    }
}
