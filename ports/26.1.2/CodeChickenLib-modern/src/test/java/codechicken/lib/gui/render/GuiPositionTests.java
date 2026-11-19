package codechicken.lib.gui.render;

import codechicken.lib.gui.modular.elements.GuiElement;
import codechicken.lib.gui.modular.lib.geometry.GuiParent;
import java.lang.reflect.Proxy;
import java.lang.reflect.InvocationHandler;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class GuiPositionTests {
    @Test void settingYDoesNotOverwriteXAndBothAxesFollowTheirParent(){
        double[] origin={17,41};
        GuiParent<?> parent=(GuiParent<?>)Proxy.newProxyInstance(GuiParent.class.getClassLoader(),new Class<?>[]{GuiParent.class},(proxy,method,args)->{
            if(method.isDefault())return InvocationHandler.invokeDefault(proxy,method,args);
            return switch(method.getName()){
                case "xMin" -> origin[0];
                case "yMin" -> origin[1];
                case "addChild" -> null;
                default -> throw new UnsupportedOperationException(method.getName());
            };
        });
        var element=new GuiElement<>(parent).setPos(100,150).setSize(20,30);
        assertEquals(100,element.xMin());assertEquals(150,element.yMin());
        element.setYPos(175);
        assertEquals(100,element.xMin());assertEquals(175,element.yMin());
        origin[0]+=9;origin[1]+=13;
        element.clearGeometryCache(); // The GUI clears this once per frame.
        assertEquals(109,element.xMin());assertEquals(188,element.yMin());
    }
}
