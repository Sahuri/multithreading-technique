# Multithreading Technique

A Java-based demo project for handling high-concurrency external API requests using multithreading techniques such as ExecutorService, ForkJoinPool, and CountDownLatch.

This project simulates sending between 1 and 100,000 requests in parallel to an external API using a combination of fixed thread pools and the common fork-join pool, depending on the request load. Built using Spring Boot and RestTemplate, the logic is triggered periodically using @Scheduled.

## Features

- Supports high-load simulations (1 to 100,000 API calls)
- Uses ExecutorService with dynamic thread pool sizing
- Automatically switches to ForkJoinPool.commonPool() for heavy loads (above 10,000 requests)
- Uses CountDownLatch to synchronize request completion
- Avoids overlapping scheduled executions using isRunning flag
- Uses Spring’s RestTemplate for external API invocation
- Logs all responses and errors per request

## Technologies

- Java 17+
- Spring Boot
- Scheduled Tasks (@Scheduled)
- ExecutorService, ForkJoinPool
- RestTemplate
- CountDownLatch
- SLF4J Logger

## How to Run

1. *Clone the repository*
   ```bash
   git clone https://github.com/sahuri/ThreadTest.git
   cd ThreadTest

2. *Update the API endpoint*
   
   Edit TestService.java and update this line to point to your desired API:
   ```java
   private final String apiUrl = "http://localhost:8000/task/submit";

3. *Adjust the number of requests*
   
   Edit the determineRequestCount() method in TestService.java to control the request load:
   ```java
   private int determineRequestCount() {
    return 1000; // Change this number as needed
   }
