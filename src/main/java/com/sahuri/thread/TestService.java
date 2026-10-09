package com.sahuri.thread;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

@Service
public class TestService {

    /** false = kirim satu kali lalu diam (perilaku lama); true = ulangi setiap jadwal menyala. */
    private static final boolean ULANG_TERUS_MENERUS = false;

    /** Batas tunggu sampai semua request selesai, supaya proses tidak menggantung selamanya. */
    private static final long BATAS_TUNGGU_MENIT = 5;

    /** Di atas jumlah ini pakai ForkJoinPool bersama; di bawahnya pakai thread pool sendiri. */
    private static final int AMBANG_TASK_BANYAK = 10_000;

    /** Sampai jumlah ini, request dikirim berurutan saja (tidak perlu thread pool). */
    private static final int AMBANG_SEQUENTIAL = 5;

    /** Berapa galat pertama yang dicatat lengkap; sisanya cukup dihitung. */
    private static final int GALAT_CONTOH = 5;

    private static final Logger logger = LoggerFactory.getLogger(TestService.class);

    private final AtomicBoolean sedangBerjalan = new AtomicBoolean(false);
    private final AtomicBoolean sudahDijalankan = new AtomicBoolean(false);
    private final AtomicInteger jumlahGagal = new AtomicInteger(0);

    private final RestTemplate restTemplate;

    @Value("${threadtest.api-url:http://localhost:8000/task/submit}")
    private String apiUrl;

    @Value("${threadtest.jumlah-request:1000}")
    private int jumlahRequest;

    public TestService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Scheduled(fixedDelay = 1000)
    public void sendRequests() {
        if (!ULANG_TERUS_MENERUS && sudahDijalankan.get()) {
            return; // mode sekali jalan: sudah selesai
        }
        // Cegah eksekusi tumpang-tindih: jadwal bisa menyala saat kiriman sebelumnya belum selesai.
        if (!sedangBerjalan.compareAndSet(false, true)) {
            logger.debug("Pengiriman sebelumnya belum selesai - jadwal ini dilewati.");
            return;
        }
        try {
            kirimSemuaRequest();
            sudahDijalankan.set(true);
        } finally {
            sedangBerjalan.set(false);
        }
    }

    private void kirimSemuaRequest() {
        int totalRequests = determineRequestCount(); // 1, 5, 40, 150, 1000, 3000, 10000, 100000
        logger.info("Start worker: {} with {} requests", LocalDateTime.now(), totalRequests);

        if (totalRequests <= AMBANG_SEQUENTIAL) {
            // Jumlah kecil: kirim berurutan, tidak perlu thread pool.
            for (int i = 0; i < totalRequests; i++) {
                sendRequest(i);
            }
        } else {
            ExecutorService executor = buatExecutor(totalRequests);
            boolean executorMilikKitaSendiri = executor != ForkJoinPool.commonPool();
            try {
                CountDownLatch latch = new CountDownLatch(totalRequests);
                IntStream.range(0, totalRequests).forEach(i -> executor.submit(() -> {
                    try {
                        sendRequest(i);
                    } finally {
                        latch.countDown();
                    }
                }));

                // Tunggu semua selesai, tapi jangan tanpa batas.
                if (!latch.await(BATAS_TUNGGU_MENIT, TimeUnit.MINUTES)) {
                    logger.warn("Batas tunggu {} menit tercapai; {} request belum selesai",
                            BATAS_TUNGGU_MENIT, latch.getCount());
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.warn("Menunggu dihentikan (interrupt)");
            } finally {
                // ForkJoinPool.commonPool() dipakai bersama seluruh JVM - JANGAN dimatikan.
                if (executorMilikKitaSendiri) {
                    executor.shutdown();
                }
            }
        }

        logger.info("End worker: {}", LocalDateTime.now());
        if (jumlahGagal.get() > 0) {
            logger.info("Semua {} request selesai dikirim! (gagal: {})", totalRequests, jumlahGagal.get());
        } else {
            logger.info("Semua {} request selesai dikirim!", totalRequests);
        }
    }

    /** Jumlah request diambil dari konfigurasi (threadtest.jumlah-request). */
    private int determineRequestCount() {
        return jumlahRequest;
    }
    // Sizing thread pool
    private int getOptimalThreadPoolSize(int totalRequests) {
        if (totalRequests <= 100) return 10;
        if (totalRequests <= 1000) return 50;
        return 100;
    }

    private ExecutorService buatExecutor(int totalRequests) {
        if (totalRequests > AMBANG_TASK_BANYAK) {
            return ForkJoinPool.commonPool();
        }
        return Executors.newFixedThreadPool(getOptimalThreadPoolSize(totalRequests));
    }

    // Request to the API target
    private void sendRequest(int i) {
        try {
            String response = restTemplate.getForObject(apiUrl, String.class);
            logger.debug("Response [{}]: {}", i, response);
        } catch (Exception e) {
            int nomor = jumlahGagal.incrementAndGet();
            if (nomor <= GALAT_CONTOH) {
                logger.warn("Error [{}]: {}", i, e.getMessage());
            } else {
                logger.debug("Error [{}]: {}", i, e.getMessage());
            }
        }
    }

}
