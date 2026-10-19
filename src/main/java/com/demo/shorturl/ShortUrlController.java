package com.demo.shorturl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.Random;

@RestController
public class ShortUrlController {

    @Autowired
    private UrlMapRepository repo;

    private static final String CHARS =
        "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    private static final int MAX_RETRY = 3;

    @PostMapping("/api/shorten")
    public ResponseEntity<String> shorten(@RequestParam String longUrl) {
        if (longUrl == null || longUrl.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("longUrl is empty");
        }
        if (!longUrl.startsWith("http://") && !longUrl.startsWith("https://")) {
            return ResponseEntity.badRequest().body("longUrl must start with http:// or https://");
        }

        String code = generateUniqueCode();

        UrlMap m = new UrlMap();
        m.setLongUrl(longUrl);
        m.setShortCode(code);
        m.setCreateTime(LocalDateTime.now());
        repo.save(m);

        return ResponseEntity.ok(code);
    }

    @GetMapping("/{code}")
    public ResponseEntity<Void> redirect(@PathVariable String code) {
        UrlMap m = repo.findByShortCode(code);
        if (m == null) {
            return ResponseEntity.notFound().build();
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(m.getLongUrl()));
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }

    @GetMapping("/api/expand")
    public String expand(@RequestParam String code) {
        UrlMap m = repo.findByShortCode(code);
        return m == null ? "not found" : m.getLongUrl();
    }

    private String generateUniqueCode() {
        for (int i = 0; i < MAX_RETRY; i++) {
            String code = randomCode(6);
            if (repo.findByShortCode(code) == null) {
                return code;
            }
        }
        throw new RuntimeException("failed to generate unique code");
    }

    private String randomCode(int len) {
        StringBuilder sb = new StringBuilder();
        Random r = new Random();
        for (int i = 0; i < len; i++) {
            sb.append(CHARS.charAt(r.nextInt(CHARS.length())));
        }
        return sb.toString();
    }
}