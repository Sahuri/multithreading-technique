# Multithreading Technique

A Java-based demo project for handling high-concurrency external API requests using multithreading techniques such as ExecutorService, ForkJoinPool, and CountDownLatch.

This project simulates sending between 1 and 100,000 requests in parallel to an external API using a combination of fixed thread pools and the common fork-join pool, depending on the request load. Built using Spring Boot and RestTemplate, the logic is triggered periodically using @Scheduled.

## Features

- Supports high-load simulations (1 to 100,000 API calls)
- Uses ExecutorService with dynamic thread pool sizing
- Automatically switches to ForkJoinPool.commonPool() for heavy loads (above 10,000 requests)
- Uses CountDownLatch to synchronize request completion
- Avoids overlapping scheduled executions using an AtomicBoolean guard
- Uses Spring’s RestTemplate for external API invocation (provided as a bean)
- Logs each response at DEBUG, the first 5 errors at WARN, and a failure count in the summary
- Target API and request load come from application.properties

## Technologies

- Java 20+
- Spring Boot
- Scheduled Tasks (@Scheduled)
- ExecutorService, ForkJoinPool
- RestTemplate
- CountDownLatch
- SLF4J Logger

## How to Run

1. *Clone the repository*
   ```bash
   git clone https://github.com/sahuri/multithreading-technique.git
   cd multithreading-technique

2. *Configure the target API and the number of requests*

   Both live in `src/main/resources/application.properties` — no code change needed:

   ```properties
   threadtest.api-url=http://localhost:8000/task/submit
   threadtest.jumlah-request=1000
   ```

   Loads used for testing: 1, 5, 40, 150, 1000, 3000, 10000, 100000.

3. *Run*

   ```bash
   mvn spring-boot:run
   ```

   The job runs **once** by default. To let it repeat on every schedule tick, set
   `ULANG_TERUS_MENERUS = true` in `TestService`.
