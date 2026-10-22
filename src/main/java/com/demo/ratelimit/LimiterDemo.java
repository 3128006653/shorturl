package com.demo.ratelimit;

public class LimiterDemo {

    private static final int LIMIT = 10;

    private static int burst(RateLimiter limiter, int n) {
        int ok = 0;
        for (int i = 0; i < n; i++) {
            if (limiter.tryAcquire()) {
                ok++;
            }
        }
        return ok;
    }

    public static void main(String[] args) throws Exception {
        System.out.println();
        System.out.println("=== Scenario A: sudden burst, limit " + LIMIT + "/sec ===");
        System.out.println("  fixed   : " + burst(new FixedWindowLimiter(LIMIT, 1000), 50) + " / 50 passed");
        System.out.println("  sliding : " + burst(new SlidingWindowLimiter(LIMIT, 1000), 50) + " / 50 passed");
        System.out.println("  token   : " + burst(new TokenBucketLimiter(LIMIT, LIMIT), 50) + " / 50 passed");
        System.out.println("  leaky   : " + burst(new LeakyBucketLimiter(LIMIT, LIMIT), 50) + " / 50 passed");

        System.out.println();
        System.out.println("=== Scenario B: boundary spike (the 2x problem) ===");
        FixedWindowLimiter fw = new FixedWindowLimiter(LIMIT, 1000);
        SlidingWindowLimiter sw = new SlidingWindowLimiter(LIMIT, 1000);
        Thread.sleep(950);
        int f1 = burst(fw, LIMIT);
        int s1 = burst(sw, LIMIT);
        System.out.println("  at end of window    -> fixed " + f1 + ", sliding " + s1);
        Thread.sleep(100);
        int f2 = burst(fw, LIMIT);
        int s2 = burst(sw, LIMIT);
        System.out.println("  just after boundary -> fixed " + f2 + ", sliding " + s2);
        System.out.println("  total in ~100ms     -> fixed " + (f1 + f2) + "  <== SPIKE (2x limit)");
        System.out.println("  total in ~100ms     -> sliding " + (s1 + s2) + "  <== within limit");

        System.out.println();
        System.out.println("=== Scenario C: token bucket refill (allows burst) ===");
        TokenBucketLimiter tb = new TokenBucketLimiter(5, 5);
        System.out.println("  immediate    : " + burst(tb, 20) + " / 20 passed");
        Thread.sleep(1000);
        System.out.println("  after 1.0s   : " + burst(tb, 20) + " / 20 passed");
        Thread.sleep(300);
        System.out.println("  after 0.3s   : " + burst(tb, 20) + " / 20 passed");

        System.out.println();
        System.out.println("=== Scenario D: leaky bucket drains at constant rate ===");
        LeakyBucketLimiter lb = new LeakyBucketLimiter(5, 5);
        System.out.println("  immediate    : " + burst(lb, 20) + " / 20 passed");
        Thread.sleep(1000);
        System.out.println("  after 1.0s   : " + burst(lb, 20) + " / 20 passed");
        System.out.println();
    }
}