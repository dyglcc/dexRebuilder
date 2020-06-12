package com.builder;

import com.android.dex.ClassDef;
import com.android.dex.Dex;
import com.android.dex.MethodId;
import com.android.dx.merge.CollisionPolicy;
import com.android.dx.merge.DexMerger;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

public class DexUtils {
    public static void main(String[] args) throws IOException {
        System.out.println("hello world");
        File file  = new File("/Users/dongyuangui/Desktop/liepin/ddd/classes.dex");
        Dex dex = new Dex(file);
//        File file2  = new File("/Users/dongyuangui/Desktop/liepin/ddd/classes2.dex");
//        Dex dex2= new Dex(file);
//        File file3  = new File("/Users/dongyuangui/Desktop/liepin/ddd/classes3.dex");
//        Dex dex3 = new Dex(file);
//        File file4  = new File("/Users/dongyuangui/Desktop/liepin/ddd/classes4.dex");
//        Dex dex4 = new Dex(file);
        File file5  = new File("/Users/dongyuangui/Desktop/liepin/ddd/classes5.dex");
        Dex dex5 = new Dex(file5);
        System.out.println("dex5 " + dex5.methodIds().size());
        Iterable<ClassDef> iterable =  dex.classDefs();
        List<MethodId> list  = dex.methodIds();
        System.out.println("dex0 "+list.size());
        for(Iterator<ClassDef> iterator = iterable.iterator(); iterator.hasNext();){
            ClassDef def = iterator.next();
            System.out.println(def.toString());
        }
        Dex dexNew = new DexMerger(new Dex[]{dex,dex5}, CollisionPolicy.KEEP_FIRST).merge();
        dexNew.writeTo(new File("/Users/dongyuangui/Desktop/liepin/ddd/classes-aoligei.dex"));
        System.out.println(dexNew == dex5);
    }

}
