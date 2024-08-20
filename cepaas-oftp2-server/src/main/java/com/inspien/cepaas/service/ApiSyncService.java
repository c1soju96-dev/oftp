// package com.inspien.cepaas.service;

// import org.springframework.cache.CacheManager;
// import org.springframework.cache.annotation.Cacheable;
// import org.springframework.scheduling.annotation.Scheduled;
// import org.springframework.stereotype.Service;
// import org.springframework.web.client.RestTemplate;

// @Service
// public class ApiSyncService {

//     private final RestTemplate restTemplate;
//     private final CacheManager cacheManager;

//     public ApiSyncService(RestTemplate restTemplate, CacheManager cacheManager) {
//         this.restTemplate = restTemplate;
//         this.cacheManager = cacheManager;
//     }

//     @Scheduled(fixedRate = 600000) // 10분마다 실행
//     public void syncApiData() {
//         String apiUrl = "https://example.com/api/data";
//         String responseData = restTemplate.getForObject(apiUrl, String.class);
        
//         // 캐시에 데이터 저장
//         cacheManager.getCache("apiCache").put("cachedDataKey", responseData);
//     }
// }

