package codechicken.lib.gui.render;

import org.joml.Matrix3x2fStack;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class CCCustomRenderStateTests {
    @Test void queuedGeometryKeepsItsTransformAfterTheGuiPoseIsRestored(){
        var pose=new Matrix3x2fStack(4);
        pose.pushMatrix();pose.translate(18,27);
        var first=new CCCustomRenderState(null,null,pose,null,null,(vertices,matrix)->{});
        pose.popMatrix();pose.pushMatrix();pose.translate(64,91);
        var second=new CCCustomRenderState(null,null,pose,null,null,(vertices,matrix)->{});
        pose.popMatrix();
        assertEquals(18,first.pose().m20(),0.0001);
        assertEquals(27,first.pose().m21(),0.0001);
        assertEquals(64,second.pose().m20(),0.0001);
        assertEquals(91,second.pose().m21(),0.0001);
    }
}
