package com.brandon3055.brandonscore.lib;

import com.mojang.serialization.Dynamic;
import net.minecraft.SharedConstants;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.*;
import net.minecraft.resources.RegistryOps;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import javax.annotation.Nullable;

/** String item references; reads legacy tag/ForgeCaps input and writes lossless native components. */
@Deprecated
public class StringyStacks {
    public static final Logger LOGGER=LogManager.getLogger("StackConverter");
    private static final String COMPONENTS="brandonscore:components";
    private static final String LEGACY_CAPS="brandonscore:legacy_forge_caps";

    public static ItemStack fromString(String value,HolderLookup.Provider provider){return fromString(value,ItemStack.EMPTY,provider);}
    public static ItemStack fromString(String value,@Nullable ItemStack fallback,HolderLookup.Provider provider){
        if(value.isEmpty())return ItemStack.EMPTY;
        try{
            int brace=value.indexOf('{'),comma=value.indexOf(',');
            // Pre-flattening syntax: id,count,metadata,{tag}. Metadata remains damage as in the original API.
            if(comma>=0&&(brace<0||comma<brace)&&value.substring(comma+1).contains(",")){
                String[] parts=value.split(",",4);
                int count=Integer.parseInt(parts[1]),damage=Integer.parseInt(parts[2]);
                CompoundTag tag=parts.length==4?TagParser.parseCompoundFully(parts[3]):new CompoundTag();
                if(damage!=0)tag.putInt("Damage",damage);
                return decodeLegacy(parts[0],count,tag,null,provider);
            }
            int idEnd=brace>=0?brace:comma>=0?comma:value.length();
            String id=value.substring(0,idEnd),suffix=value.substring(idEnd);
            CompoundTag payload=null;
            if(suffix.startsWith("{")){
                int end=suffix.lastIndexOf('}');if(end<0)throw new IllegalArgumentException("Unclosed item tag");
                payload=TagParser.parseCompoundFully(suffix.substring(0,end+1));suffix=suffix.substring(end+1);
            }
            int count=1;
            if(!suffix.isEmpty()){
                if(!suffix.startsWith(","))throw new IllegalArgumentException("Unexpected item suffix");
                count=Integer.parseInt(suffix.substring(1));
            }
            if(count<=0)throw new IllegalArgumentException("Non-positive item count");
            if(payload!=null&&payload.contains(COMPONENTS)){
                CompoundTag encoded=new CompoundTag();encoded.putString("id",id);encoded.putInt("count",1);
                encoded.put("components",payload.getCompoundOrEmpty(COMPONENTS).copy());
                return decodeNative(encoded,count,provider);
            }
            CompoundTag tag=payload,caps=null;
            if(payload!=null&&payload.contains("ForgeCaps")){
                tag=payload.getCompoundOrEmpty("tag");caps=payload.getCompoundOrEmpty("ForgeCaps");
            }
            return decodeLegacy(id,count,tag,caps,provider);
        }catch(Exception error){LOGGER.warn("Invalid string item reference: {}",value,error);return fallback;}
    }
    private static ItemStack decodeNative(CompoundTag encoded,int count,HolderLookup.Provider provider){
        if(count<=0)throw new IllegalArgumentException("Non-positive item count");
        ItemStack stack=ItemStack.CODEC.parse(RegistryOps.create(NbtOps.INSTANCE,provider),encoded).getOrThrow();
        if(stack.isEmpty())throw new IllegalArgumentException("Unknown or empty item");
        stack.setCount(count);return stack;
    }
    private static ItemStack decodeLegacy(String id,int count,@Nullable CompoundTag tag,@Nullable CompoundTag caps,HolderLookup.Provider provider){
        CompoundTag encoded=new CompoundTag();encoded.putString("id",id);encoded.putByte("Count",(byte)1);
        if(tag!=null&&!tag.isEmpty())encoded.put("tag",tag.copy());
        // The native ItemStackComponentizationFix is registered at schema 3818.5; start immediately before it.
        Tag fixed;
        if(tag==null||tag.isEmpty()){
            encoded.remove("Count");encoded.putInt("count",1);fixed=encoded;
        }else fixed=DataFixers.getDataFixer().update(References.ITEM_STACK,new Dynamic<>(NbtOps.INSTANCE,encoded),
                3817,SharedConstants.getCurrentVersion().dataVersion().version()).getValue();
        ItemStack stack=decodeNative((CompoundTag)fixed,count,provider);
        if(caps!=null&&!caps.isEmpty())CustomData.update(DataComponents.CUSTOM_DATA,stack,data->data.put(LEGACY_CAPS,caps.copy()));
        return stack;
    }
    public static String toString(ItemStack source,boolean withNBT,boolean withCount,boolean withForgeCaps,HolderLookup.Provider provider){
        if(source.isEmpty())return "";
        String result=BuiltInRegistries.ITEM.getKey(source.getItem()).toString();
        ItemStack copy=source.copyWithCount(1);
        CompoundTag custom=copy.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
        CompoundTag caps=custom.getCompoundOrEmpty(LEGACY_CAPS).copy();
        if(!withForgeCaps&&custom.contains(LEGACY_CAPS)){
            custom.remove(LEGACY_CAPS);
            if(custom.isEmpty())copy.remove(DataComponents.CUSTOM_DATA);else copy.set(DataComponents.CUSTOM_DATA,CustomData.of(custom));
        }
        if(withNBT){
            CompoundTag encoded=(CompoundTag)ItemStack.CODEC.encodeStart(RegistryOps.create(NbtOps.INSTANCE,provider),copy).getOrThrow();
            CompoundTag components=encoded.getCompoundOrEmpty("components");
            if(!components.isEmpty()){CompoundTag payload=new CompoundTag();payload.put(COMPONENTS,components.copy());result+=payload;}
        }else if(withForgeCaps&&!caps.isEmpty()){
            CompoundTag payload=new CompoundTag();payload.put("tag",new CompoundTag());payload.put("ForgeCaps",caps);result+=payload;
        }
        if(withCount&&source.getCount()>1)result+=","+source.getCount();
        return result;
    }
    public static String toStringNoCaps(ItemStack stack,HolderLookup.Provider provider){return toString(stack,true,true,false,provider);}
    public static String toString(ItemStack stack,HolderLookup.Provider provider){return toString(stack,true,true,true,provider);}
    @Deprecated
    public static ItemStack legacyStackConverter(String id,int count,int damage,@Nullable CompoundTag nbt){
        try{
            CompoundTag tag=nbt==null?new CompoundTag():nbt.copy();if(damage!=0)tag.putInt("Damage",damage);
            return decodeLegacy(id,count,tag,null,RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY));
        }catch(Exception error){LOGGER.warn("Invalid legacy item reference: {}",id,error);return ItemStack.EMPTY;}
    }
}
