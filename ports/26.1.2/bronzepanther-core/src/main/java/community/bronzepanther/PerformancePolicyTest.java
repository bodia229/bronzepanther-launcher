package community.bronzepanther;
public final class PerformancePolicyTest {
    public static void main(String[] args) {
        PerformancePolicy p = new PerformancePolicy();
        if (p.sample(10,10,10,0)!=10 || p.sample(80,10,10,5)!=10) throw new AssertionError("one transient frame");
        int distance=10;
        for(int s=10;s<=20;s+=5)distance=p.sample(20,distance,10,s);
        if(distance!=8)throw new AssertionError("sustained low FPS");
        for(int s=25;s<65;s+=5)if(p.sample(10,distance,10,s)!=8)throw new AssertionError("cooldown");
        for(int s=65;s<600;s+=5)distance=p.sample(10,distance,10,s);
        if(distance!=4)throw new AssertionError("safe minimum");
        for(int s=600;s<1800;s+=5)distance=p.sample(100,distance,10,s);
        if(distance!=10)throw new AssertionError("recovery stays at player ceiling");
        System.out.println("BRONZEPANTHER_PERFORMANCE_POLICY_PASS");
    }
}
