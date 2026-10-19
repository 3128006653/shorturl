package com.demo.shorturl;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UrlMapRepository extends JpaRepository<UrlMap, Long> {
    UrlMap findByShortCode(String shortCode);
}