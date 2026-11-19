package codechicken.lib.gui.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import org.joml.Matrix3x2f;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GuiRectangleTests {
    @Test void legacyRgbLabelsRemainVisibleAndExplicitAlphaIsPreserved(){
        assertEquals(0xFFFF0000,GuiGraphicsExtension.legacyTextColour(0xFF0000));
        assertEquals(0xFF000000,GuiGraphicsExtension.legacyTextColour(0));
        assertEquals(0x8000C0FF,GuiGraphicsExtension.legacyTextColour(0x8000C0FF));
    }
    @Test void shadedButtonVerticesStayWithinItsOffsetRectangle(){
        var vertices=new ArrayList<double[]>();
        VertexConsumer recorder=(VertexConsumer)Proxy.newProxyInstance(VertexConsumer.class.getClassLoader(),new Class<?>[]{VertexConsumer.class},(proxy,method,args)->{
            if(method.getName().equals("addVertex")&&args.length==3){
                vertices.add(new double[]{((Number)args[0]).doubleValue(),((Number)args[1]).doubleValue()});return proxy;
            }
            if(method.isDefault())return InvocationHandler.invokeDefault(proxy,method,args);
            if(method.getReturnType()==VertexConsumer.class)return proxy;
            throw new UnsupportedOperationException(method.toString());
        });
        RenderStateUtils.shadedFill(recorder,new Matrix3x2f(),120,170,80,100,2,0xffeeeeee,0xff111111,0xff555555,0xff777777);
        assertEquals(28,vertices.size());
        for(var point:vertices){
            assertTrue(point[0]>=120&&point[0]<=170,"Button vertex outside horizontal bounds");
            assertTrue(point[1]>=80&&point[1]<=100,"Button vertex outside vertical bounds: "+point[1]);
        }
    }
    @Test void verticalAndHorizontalGradientHelpersSelectDifferentAxes(){
        var axes=new ArrayList<Boolean>();
        GuiGraphicsExtension extension=(GuiGraphicsExtension)Proxy.newProxyInstance(GuiGraphicsExtension.class.getClassLoader(),new Class<?>[]{GuiGraphicsExtension.class},(proxy,method,args)->{
            if(method.getName().equals("cc$fillGradient")&&args.length==7){axes.add((Boolean)args[6]);return null;}
            if(method.isDefault())return InvocationHandler.invokeDefault(proxy,method,args);
            throw new UnsupportedOperationException(method.toString());
        });
        extension.cc$fillGradientV(10,20,30,40,0xff000000,0xffffffff);
        extension.cc$fillGradientH(10,20,30,40,0xff000000,0xffffffff);
        assertEquals(java.util.List.of(false,true),axes);
    }
}
