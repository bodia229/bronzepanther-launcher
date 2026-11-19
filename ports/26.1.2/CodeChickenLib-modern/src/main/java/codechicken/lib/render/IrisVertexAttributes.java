package codechicken.lib.render;

import com.mojang.blaze3d.vertex.VertexFormatElement;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

/** Iris derives these attributes in its vertex-consumer hooks, after normal emission. */
final class IrisVertexAttributes {
    private IrisVertexAttributes(){}
    static boolean isManaged(VertexFormatElement element){return Holder.MANAGED.contains(element);}
    private static final class Holder {
        static final Set<VertexFormatElement> MANAGED=load();
        private static Set<VertexFormatElement> load(){
            Set<VertexFormatElement> result=Collections.newSetFromMap(new IdentityHashMap<>());
            try{
                var type=Class.forName("net.irisshaders.iris.vertices.IrisVertexFormats");
                for(String field:new String[]{"ENTITY_ELEMENT","ENTITY_ID_ELEMENT","MID_TEXTURE_ELEMENT","TANGENT_ELEMENT","MID_BLOCK_ELEMENT"})
                    result.add((VertexFormatElement)type.getField(field).get(null));
            }catch(ClassNotFoundException absent){
                // Iris is optional. Unknown attributes from other renderers are still rejected.
            }catch(ReflectiveOperationException incompatible){throw new IllegalStateException("Unable to resolve Iris-managed vertex attributes",incompatible);}
            return Collections.unmodifiableSet(result);
        }
    }
}
