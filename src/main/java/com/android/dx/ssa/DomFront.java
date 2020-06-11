package com.android.dx.ssa;

import com.android.dx.util.IntSet;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Iterator;

public class DomFront {
    private static final boolean DEBUG = false;
    private final DomInfo[] domInfos;
    private final SsaMethod meth;
    private final ArrayList<SsaBasicBlock> nodes;

    public static class DomInfo {
        public IntSet dominanceFrontiers;
        public int idom = -1;
    }

    public DomFront(SsaMethod meth) {
        this.meth = meth;
        this.nodes = meth.getBlocks();
        int szNodes = this.nodes.size();
        this.domInfos = new DomInfo[szNodes];
        for (int i = 0; i < szNodes; i++) {
            this.domInfos[i] = new DomInfo();
        }
    }

    public DomInfo[] run() {
        int szNodes = this.nodes.size();
        Dominators methDom = Dominators.make(this.meth, this.domInfos, false);
        buildDomTree();
        for (int i = 0; i < szNodes; i++) {
            this.domInfos[i].dominanceFrontiers = SetFactory.makeDomFrontSet(szNodes);
        }
        calcDomFronts();
        return this.domInfos;
    }

    private void debugPrintDomChildren() {
        int szNodes = this.nodes.size();
        for (int i = 0; i < szNodes; i++) {
            SsaBasicBlock node = (SsaBasicBlock) this.nodes.get(i);
            StringBuffer sb = new StringBuffer();
            sb.append('{');
            boolean comma = false;
            Iterator it = node.getDomChildren().iterator();
            while (it.hasNext()) {
                SsaBasicBlock child = (SsaBasicBlock) it.next();
                if (comma) {
                    sb.append(',');
                }
                sb.append(child);
                comma = true;
            }
            sb.append('}');
            System.out.println("domChildren[" + node + "]: " + sb);
        }
    }

    private void buildDomTree() {
        int szNodes = this.nodes.size();
        for (int i = 0; i < szNodes; i++) {
            DomInfo info = this.domInfos[i];
            if (info.idom != -1) {
                ((SsaBasicBlock) this.nodes.get(info.idom)).addDomChild((SsaBasicBlock) this.nodes.get(i));
            }
        }
    }

    private void calcDomFronts() {
        int szNodes = this.nodes.size();
        for (int b = 0; b < szNodes; b++) {
            SsaBasicBlock nb = (SsaBasicBlock) this.nodes.get(b);
            DomInfo nbInfo = this.domInfos[b];
            BitSet pred = nb.getPredecessors();
            if (pred.cardinality() > 1) {
                for (int i = pred.nextSetBit(0); i >= 0; i = pred.nextSetBit(i + 1)) {
                    int runnerIndex = i;
                    while (runnerIndex != nbInfo.idom && runnerIndex != -1) {
                        DomInfo runnerInfo = this.domInfos[runnerIndex];
                        if (runnerInfo.dominanceFrontiers.has(b)) {
                            break;
                        }
                        runnerInfo.dominanceFrontiers.add(b);
                        runnerIndex = runnerInfo.idom;
                    }
                }
            }
        }
    }
}
