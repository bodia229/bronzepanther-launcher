package com.brandon3055.brandonscore.client.gui;

/** ARGB helpers independent of the removed immediate GUI renderer. */
public final class GuiColourMath {
    private GuiColourMath(){}
    public static int midColour(int first,int second){
        int result=0;
        for(int shift=0;shift<32;shift+=8){int a=first>>>shift&255,b=second>>>shift&255;result|=(b+(a-b)/2)<<shift;}
        return result;
    }
    public static int mixColours(int first,int second){return mixColours(first,second,false);}
    public static int mixColours(int first,int second,boolean subtract){
        int result=0;
        for(int shift=0;shift<32;shift+=8){int a=first>>>shift&255,b=second>>>shift&255;result|=Math.max(0,Math.min(255,a+(subtract?-b:b)))<<shift;}
        return result;
    }
}
