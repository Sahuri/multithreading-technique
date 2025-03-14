package com.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.concurrent.*;
import java.util.stream.IntStream;

@Service
public class TestService {

    private boolean isRunning = true;
    private static final Logger logger = LoggerFactory.getLogger(TestService.class);
    private final RestTemplate restTemplate = new RestTemplate();
    private final String apiUrl = "http://localhost:8000/task/submit"; // Ganti dengan API tujuan

    @Scheduled(fixedDelay = 1000)
    public void sendRequests() {
        if (!isRunning) {
            return;
        }

        int totalRequests = determineRequestCount(); // Bisa 1, 5, 40, 150, 1000, 3000, 10000, 100000
        logger.info("Start worker: {} with {} requests", LocalDateTime.now(), totalRequests);

        if (totalRequests <= 5) {
            // Eksekusi langsung jika jumlah request kecil
            for (int i = 0; i < totalRequests; i++) {
                sendRequest(i);
            }
        } else {
            // Tentukan ukuran thread pool secara dinamis
            int threadPoolSize = getOptimalThreadPoolSize(totalRequests);

            ExecutorService executor;
            if (totalRequests > 10_000) {
                // Gunakan ForkJoinPool untuk jumlah besar (atau Virtual Threads di Java 21)
                executor = ForkJoinPool.commonPool();
            } else {
                executor = Executors.newFixedThreadPool(threadPoolSize);
            }

            CountDownLatch latch = new CountDownLatch(totalRequests);
            IntStream.range(0, totalRequests).forEach(i -> executor.submit(() -> {
                try {
                    sendRequest(i);
                } finally {
                    latch.countDown();
                }
            }));

            // Tunggu semua request selesai
            try {
                latch.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            executor.shutdown();
        }

        logger.info("End worker: {}", LocalDateTime.now());
        logger.info("Semua {} request selesai dikirim!", totalRequests);
        isRunning = false;
    }

    // Fungsi untuk menentukan jumlah request (bisa diatur dinamis)
    private int determineRequestCount() {
        return 1000; // Simulasi jumlah request acak antara 1 dan 100000
    }

    // Fungsi untuk menentukan ukuran thread pool optimal
    private int getOptimalThreadPoolSize(int totalRequests) {
        if (totalRequests <= 100) return 10;
        if (totalRequests <= 1000) return 50;
        if (totalRequests <= 10000) return 100;
        return Runtime.getRuntime().availableProcessors(); // Gunakan semua core CPU untuk 10000+
    }

    // Fungsi untuk mengirim request
    private void sendRequest(int i) {
        try {
            String response = restTemplate.getForObject(apiUrl, String.class);
            logger.info("Response [{}]: {}", i, response);
        } catch (Exception e) {
            logger.error("Error [{}]: {}", i, e.getMessage());
        }
    }

}
