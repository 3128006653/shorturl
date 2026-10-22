package com.demo.ratelimit;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DemoController {

    @GetMapping("/api/demo/sliding")
    @RateLimit(key = "demo-sliding", limit = 5, algorithm = "sliding")
    public String sliding() {
        return "ok";
    }

    @GetMapping("/api/demo/fixed")
    @RateLimit(key = "demo-fixed", limit = 5, algorithm = "fixed")
    public String fixed() {
        return "ok";
    }
}