package com.android.dx.command.dump;

import com.android.dex.util.FileUtils;
import com.android.dx.cf.iface.ParseException;
import com.android.dx.util.HexParser;
import java.io.UnsupportedEncodingException;

public class Main {
    private final Args parsedArgs = new Args();

    private Main() {
    }

    public static void main(String[] args) {
        new Main().run(args);
    }

    private void run(String[] args) {
        int at = 0;
        while (at < args.length) {
            String arg = args[at];
            if (arg.equals("--") || !arg.startsWith("--")) {
                break;
            }
            if (arg.equals("--bytes")) {
                this.parsedArgs.rawBytes = true;
            } else if (arg.equals("--basic-blocks")) {
                this.parsedArgs.basicBlocks = true;
            } else if (arg.equals("--rop-blocks")) {
                this.parsedArgs.ropBlocks = true;
            } else if (arg.equals("--optimize")) {
                this.parsedArgs.optimize = true;
            } else if (arg.equals("--ssa-blocks")) {
                this.parsedArgs.ssaBlocks = true;
            } else if (arg.startsWith("--ssa-step=")) {
                this.parsedArgs.ssaStep = arg.substring(arg.indexOf(61) + 1);
            } else if (arg.equals("--debug")) {
                this.parsedArgs.debug = true;
            } else if (arg.equals("--dot")) {
                this.parsedArgs.dotDump = true;
            } else if (arg.equals("--strict")) {
                this.parsedArgs.strictParse = true;
            } else if (arg.startsWith("--width=")) {
                arg = arg.substring(arg.indexOf(61) + 1);
                this.parsedArgs.width = Integer.parseInt(arg);
            } else if (arg.startsWith("--method=")) {
                this.parsedArgs.method = arg.substring(arg.indexOf(61) + 1);
            } else {
                System.err.println("unknown option: " + arg);
                throw new RuntimeException("usage");
            }
            at++;
        }
        if (at == args.length) {
            System.err.println("no input files specified");
            throw new RuntimeException("usage");
        }
        while (at < args.length) {
            try {
                String name = args[at];
                System.out.println("reading " + name + "...");
                byte[] bytes = FileUtils.readFile(name);
                if (!name.endsWith(".class")) {
                    bytes = HexParser.parse(new String(bytes, "utf-8"));
                }
                processOne(name, bytes);
            } catch (UnsupportedEncodingException ex) {
                throw new RuntimeException("shouldn't happen", ex);
            } catch (ParseException ex2) {
                System.err.println("\ntrouble parsing:");
                if (this.parsedArgs.debug) {
                    ex2.printStackTrace();
                } else {
                    ex2.printContext(System.err);
                }
            }
            at++;
        }
    }

    private void processOne(String name, byte[] bytes) {
        if (this.parsedArgs.dotDump) {
            DotDumper.dump(bytes, name, this.parsedArgs);
        } else if (this.parsedArgs.basicBlocks) {
            BlockDumper.dump(bytes, System.out, name, false, this.parsedArgs);
        } else if (this.parsedArgs.ropBlocks) {
            BlockDumper.dump(bytes, System.out, name, true, this.parsedArgs);
        } else if (this.parsedArgs.ssaBlocks) {
            this.parsedArgs.optimize = false;
            SsaDumper.dump(bytes, System.out, name, this.parsedArgs);
        } else {
            ClassDumper.dump(bytes, System.out, name, this.parsedArgs);
        }
    }
}
