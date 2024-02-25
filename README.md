# java-virtual-threads-benchmark

This project is to experiment with virtual threads on Java 21 and Spring Boot 3. In particular, I wanted to see how virtual threads compare to traditional threads in terms of performance and resource usage in a web application and validate my understanding of the concept.

Also, given that Virtual Threads work very similarly to how the event loop works in Node.js, a comparison between a mono-threaded Spring Boot application and a Node.js application is analyzed. 

There are two modules in the project:

### Module `./spring-boot-demo`

This is the Spring Boot service. It consists of a simple REST API that exposes two endpoints:
* `/demo/cpu-light`: This uses two `Threads.sleep()` to simulate some non-blocking IO operations, so to make most use of Virtual Threads (when enabled).
*  `/demo/cpu-intensive`: This adds a CPU intensive operation to the mix simulated with an empty while loop.

We make use of spring profiles to be able to launch the same application in 3 different scenarios: 
* [non-virtual](./spring-boot-demo/src/main/resources/application-non-virtual.yml): This loads the default configurations, and uses traditional threads.
* [virtual](./spring-boot-demo/src/main/resources/application-virtual.yml): This profile uses virtual threads.
* [mono-virtual](./spring-boot-demo/src/main/resources/application-mono-virtual.yml): This profile starts a mono-threaded Spring Boot application with virtual threads (for comparison with the Node.js service).

By default, the Tomcat server uses a pool of maximum 200 threads. [Here](./spring-boot-demo/src/main/resources/application.yml) we limit them to 50. The reason is that the Docker containers used for this experiment don't accept more than 100 concurrent requests at a time and this would have not allowed us to test scenarios where the number of concurrent requests is greater than the accepted concurrent request by the server.

To build the application:
```sh
cd spring-boot-demo && ./gradlew build
```

To run the application locally:
```sh
cd spring-boot-demo && ./gradlew bootRun --args='--spring.profiles.active=non-virtual'
```

### Module `./nodejs-demo`

This is the Node.js service. It consists of a simple REST API that exposes two endpoints:
* `/demo/cpu-light`: This uses a `setTimeout()` to simulate some IO non-blocking operations.
* `/demo/cpu-intensive`: This adds a CPU intensive operation to the mix simulated with an empty while loop.

To install all packages:
```sh
cd nodejs-demo && npm install
```

To run the application locally:
```sh
node ./nodejs-demo/src/app.js
```

## Docker Compose

A [docker-compose.yml](./docker-compose.yml) is provided to launch the 4 services all together. This guarantees that the tests are performed in a reproducible environment. 

[docker-compose.yml](./docker-compose.yml) needs the ready-to-run spring boot jar to be found in `./spring-boot/build/libs`, so to start up the docker containers:

1. Pack the Spring Boot application:
```sh
cd spring-boot-demo && ./gradlew bootJar
```

2. Launch `docker-compose`
```sh
docker-compose up -d
```

4 containers will run with the same config: 4 cores and 4GB of RAM. The services will be available as:

* `spring-boot-app-non-virtual`. Spring Boot service running on normal threads. Responds at [localhost:8081/demo](http://localhost:8081/demo).
* `spring-boot-app-virtual`. Spring Boot service running on virtual threads. Responds at [localhost:8082/demo](http://localhost:8082/demo).
* `spring-boot-app-mono-virtual`. Spring Boot mono threaded service running on virtual threads. Responds at [localhost:8083/demo](http://localhost:8083/demo).
* `nodejs-app`. Node.js service. Responds at [localhost:8084/demo](http://localhost:8084/demo).

## Load Test


The actual load tests are performed with [Apache Benchmark](https://httpd.apache.org/docs/2.4/programs/ab.html) `ab`.
Results in [RESULTS.md](./RESULTS.md).