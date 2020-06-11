package com.android.dx.command.dump;

class Args {
    boolean basicBlocks = false;
    boolean debug = false;
    boolean dotDump = false;
    String method;
    boolean optimize = false;
    boolean rawBytes = false;
    boolean ropBlocks = false;
    boolean ssaBlocks = false;
    String ssaStep = null;
    boolean strictParse = false;
    int width = 0;

    Args() {
    }
}
