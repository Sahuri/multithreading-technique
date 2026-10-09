package com.sahuri.thread;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Mengunci perilaku TestService tanpa benar-benar memanggil jaringan.
 *
 * RestTemplate diganti stub yang menghitung panggilan (dan bisa diberi jeda),
 * jumlah request diisi langsung ke field @Value lewat ReflectionTestUtils.
 */
class TestServiceTest {

    /** RestTemplate palsu: menghitung panggilan, opsional melambat. */
    static class RestTemplatePalsu extends RestTemplate {
        final AtomicInteger jumlahPanggilan = new AtomicInteger();
        private final long jedaMs;

        RestTemplatePalsu(long jedaMs) {
            this.jedaMs = jedaMs;
        }

        @Override
        public <T> T getForObject(String url, Class<T> tipe, Object... uriVariables) {
            jumlahPanggilan.incrementAndGet();
            if (jedaMs > 0) {
                try {
                    Thread.sleep(jedaMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            return tipe.cast("ok");
        }
    }

    @Test
    @DisplayName("jumlah request mengikuti konfigurasi (jalur berurutan untuk <= 5)")
    void mengikutiJumlahRequestDariKonfigurasi() {
        RestTemplatePalsu restTemplate = new RestTemplatePalsu(0);
        TestService service = new TestService(restTemplate);
        ReflectionTestUtils.setField(service, "jumlahRequest", 3);

        service.sendRequests();

        assertEquals(3, restTemplate.jumlahPanggilan.get());
    }

    @Test
    @DisplayName("dua eksekusi bersamaan tidak tumpang-tindih")
    void tidakTumpangTindihSaatDipanggilBersamaan() throws InterruptedException {
        RestTemplatePalsu restTemplate = new RestTemplatePalsu(80);
        TestService service = new TestService(restTemplate);
        ReflectionTestUtils.setField(service, "jumlahRequest", 40);

        CountDownLatch mulai = new CountDownLatch(1);
        Runnable tugas = () -> {
            try {
                mulai.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            service.sendRequests();
        };

        Thread pertama = new Thread(tugas, "ujian-1");
        Thread kedua = new Thread(tugas, "ujian-2");
        pertama.start();
        kedua.start();
        mulai.countDown(); // lepaskan keduanya hampir bersamaan
        pertama.join();
        kedua.join();

        assertEquals(40, restTemplate.jumlahPanggilan.get(),
                "pengiriman tidak boleh bertumpuk - 40 request tetap 40 panggilan, bukan 80");
    }

    @Test
    @DisplayName("setelah selesai, jadwal berikutnya tidak mengirim ulang")
    void tidakMengulangSetelahSelesai() {
        RestTemplatePalsu restTemplate = new RestTemplatePalsu(0);
        TestService service = new TestService(restTemplate);
        ReflectionTestUtils.setField(service, "jumlahRequest", 5);

        service.sendRequests(); // jalan pertama
        service.sendRequests(); // jadwal kedua - harus dilewati

        assertEquals(5, restTemplate.jumlahPanggilan.get());
    }
}
