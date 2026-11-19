package com.brandon3055.brandonscore.client.render;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.function.Supplier;

/** Distinct resources within a frame, reused only after the frame has finished. */
public final class FrameResourcePool<K,V> {
    private static final class Bucket<V> {
        final ArrayList<V> values=new ArrayList<>();
        int cursor;
    }
    private final IdentityHashMap<K,Bucket<V>> buckets=new IdentityHashMap<>();
    private final int limit;
    private int retained;
    public FrameResourcePool(int limit){
        if(limit<0)throw new IllegalArgumentException("Negative pool limit");
        this.limit=limit;
    }
    public V acquire(K key,Supplier<V> factory){
        var bucket=buckets.get(key);
        if(bucket!=null&&bucket.cursor<bucket.values.size())return bucket.values.get(bucket.cursor++);
        // Excess resources remain transient, including previously unseen material keys.
        if(retained>=limit)return factory.get();
        var value=factory.get();
        if(bucket==null){bucket=new Bucket<>();buckets.put(key,bucket);}
        bucket.values.add(value);bucket.cursor++;retained++;
        return value;
    }
    public void endFrame(){for(var bucket:buckets.values())bucket.cursor=0;}
    public int retainedCount(){return retained;}
    public void clear(){buckets.clear();retained=0;}
}
