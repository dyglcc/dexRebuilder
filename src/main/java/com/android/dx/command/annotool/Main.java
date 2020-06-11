package com.android.dx.command.annotool;

import java.lang.annotation.ElementType;
import java.util.EnumSet;
import java.util.Locale;

public class Main {

    static class Arguments {
        String aclass;
        EnumSet<ElementType> eTypes = EnumSet.noneOf(ElementType.class);
        String[] files;
        EnumSet<PrintType> printTypes = EnumSet.noneOf(PrintType.class);

        Arguments() {
        }

        void parse(String[] argArray) throws InvalidArgumentException {
            for (int i = 0; i < argArray.length; i++) {
                String arg = argArray[i];
                if (!arg.startsWith("--annotation=")) {
                    if (!arg.startsWith("--element=")) {
                        if (!arg.startsWith("--print=")) {
                            this.files = new String[(argArray.length - i)];
                            System.arraycopy(argArray, i, this.files, 0, this.files.length);
                            break;
                        }
                        try {
                            for (String p : arg.substring(arg.indexOf(61) + 1).split(",")) {
                                this.printTypes.add(PrintType.valueOf(p.toUpperCase(Locale.ROOT)));
                            }
                        } catch (IllegalArgumentException e) {
                            throw new InvalidArgumentException("invalid --print");
                        }
                    }
                    try {
                        for (String p2 : arg.substring(arg.indexOf(61) + 1).split(",")) {
                            this.eTypes.add(ElementType.valueOf(p2.toUpperCase(Locale.ROOT)));
                        }
                    } catch (IllegalArgumentException e2) {
                        throw new InvalidArgumentException("invalid --element");
                    }
                }
                String argParam = arg.substring(arg.indexOf(61) + 1);
                if (this.aclass != null) {
                    throw new InvalidArgumentException("--annotation can only be specified once.");
                }
                this.aclass = argParam.replace('.', ClassPathElement.SEPARATOR_CHAR);
            }
            if (this.aclass == null) {
                throw new InvalidArgumentException("--annotation must be specified");
            }
            if (this.printTypes.isEmpty()) {
                this.printTypes.add(PrintType.CLASS);
            }
            if (this.eTypes.isEmpty()) {
                this.eTypes.add(ElementType.TYPE);
            }
            EnumSet<ElementType> set = this.eTypes.clone();
            set.remove(ElementType.TYPE);
            set.remove(ElementType.PACKAGE);
            if (!set.isEmpty()) {
                throw new InvalidArgumentException("only --element parameters 'type' and 'package' supported");
            }
        }
    }

    private static class InvalidArgumentException extends Exception {
        InvalidArgumentException() {
        }

        InvalidArgumentException(String s) {
            super(s);
        }
    }

    enum PrintType {
        CLASS,
        INNERCLASS,
        METHOD,
        PACKAGE
    }

    private Main() {
    }

    public static void main(String[] argArray) {
        Arguments args = new Arguments();
        try {
            args.parse(argArray);
            new AnnotationLister(args).process();
        } catch (InvalidArgumentException ex) {
            System.err.println(ex.getMessage());
            throw new RuntimeException("usage");
        }
    }
}
