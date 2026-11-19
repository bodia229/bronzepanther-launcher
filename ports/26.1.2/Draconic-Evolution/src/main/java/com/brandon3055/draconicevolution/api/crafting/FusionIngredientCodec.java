package com.brandon3055.draconicevolution.api.crafting;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import net.minecraft.world.item.crafting.Ingredient;
import java.util.stream.Stream;

/** Accepts authored 1.21 item/tag objects while encoding the native 26.1 ingredient representation. */
public final class FusionIngredientCodec implements Codec<Ingredient> {
    public static final Codec<Ingredient> INSTANCE=new FusionIngredientCodec();
    private FusionIngredientCodec(){}
    @Override public <T> DataResult<Pair<Ingredient,T>> decode(DynamicOps<T> ops,T input){
        var nativeResult=Ingredient.CODEC.decode(ops,input);if(nativeResult.result().isPresent())return nativeResult;
        try{return Ingredient.CODEC.decode(ops,normalize(ops,input,0));}
        catch(RuntimeException error){return DataResult.error(error::getMessage);}
    }
    private static <T> T normalize(DynamicOps<T> ops,T input,int depth){
        if(depth>16)throw new IllegalArgumentException("Fusion ingredient nesting is too deep");
        var map=ops.getMap(input).result();
        if(map.isPresent()&&map.get().get("type")==null){
            T item=map.get().get("item"),tag=map.get().get("tag");
            if(item!=null&&tag!=null)throw new IllegalArgumentException("Fusion ingredient cannot define item and tag together");
            if(item!=null)return ops.createString(ops.getStringValue(item).getOrThrow());
            if(tag!=null)return ops.createString("#"+ops.getStringValue(tag).getOrThrow());
        }
        if(map.isPresent()){
            return ops.createMap(map.get().entries().map(entry->{
                String name=ops.getStringValue(entry.getFirst()).result().orElse("");
                T key=name.equals("type")?ops.createString("neoforge:ingredient_type"):entry.getFirst();
                T value=entry.getSecond();
                if(name.equals("ingredient"))value=normalize(ops,value,depth+1);
                else if(name.equals("children")||name.equals("ingredients")){
                    var children=ops.getStream(value).result();
                    if(children.isPresent())value=ops.createList(children.get().map(child->normalize(ops,child,depth+1)));
                }
                return Pair.of(key,value);
            }));
        }
        var list=ops.getStream(input).result();
        if(list.isPresent()){
            T children=ops.createList(list.get().map(child->normalize(ops,child,depth+1)));
            return ops.createMap(Stream.of(Pair.of(ops.createString("neoforge:ingredient_type"),ops.createString("neoforge:compound")),Pair.of(ops.createString("children"),children)));
        }
        return input;
    }
    @Override public <T> DataResult<T> encode(Ingredient ingredient,DynamicOps<T> ops,T prefix){return Ingredient.CODEC.encode(ingredient,ops,prefix);}
}
