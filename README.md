# java-virtual-threads-benchmark

This project is to experiment with virtual threads on Java 21 and Spring Boot 3. In particular, I wanted to see how virtual threads compare to traditional threads in terms of performance and resource usage in a web application. 

Also, given that Virtual Threads work very similarly to how the event loop works in Node.js, a comparison between a mono-threaded Spring Boot application and a Node.js application is analyzed. 

You will find two modules in the project:

### Module `./spring-boot-demo`

This is the Spring Boot service. It consists of a simple REST API that exposes two endpoints:
* `/demo/cpu-light`: This uses two `Threads.sleep()` to simulate a blocking IO operation, so to make most use of Virtual Threads (if available).
*  `/demo/cpu-intensive`: This adds a CPU intensive operation to the mix simulated with an empty while loop.

We make use of spring profiles to be able to launch the same application in 3 different scenarios: 
* `non-virtual`: This is the default profile, and it uses traditional threads.
* `virtual`: This profile uses virtual threads.
* `mono-virtual`: This profile starts a mono-threaded Spring Boot application with virtual threads (for comparison with a Node.js service).

To build the application.
```shell
cd spring-boot-demo
./gradlew bootJar
```

### Module `./nodejs-demo`

This is the Node.js service. It consists of a simple REST API that exposes two endpoints:
* `/demo/cpu-light`: This uses a `setTimeout()` to simulate a blocking IO operation, so to make most use of Virtual Threads (if available).
*  `/demo/cpu-intensive`: This adds a CPU intensive operation to the mix simulated with an empty while loop.

To install all packages
```shell
cd nodejs-demo
npm install
```

## Docker Compose

A `docker-compose` file is provided to launch the 4 services all together, in order to run them on a reproducible environment. 

`docker-compose` needs the ready-to-run jar to be found in `./spring-boot/build/libs`, so:

* Pack the Spring Boot application
```sh
cd spring-boot-demo
./gradlew bootJar
```
* Launch `docker-compose`
```sh
docker-compose up -d
```

4 containers will run with the same config: 4 cores and 4GB of RAM. The services will be available at:

* `spring-boot-app-non-virtual`. Spring Boot service running on normal threads. Responds at [localhost:8081](http://localhost:8081/demo/ping).
* `spring-boot-app-virtual`. Spring Boot service running on virtual threads. Responds at [localhost:8082](http://localhost:8082/demo/ping).
* `spring-boot-app-mono-virtual`. Spring Boot mono threaded service running on virtual threads. Responds at [localhost:8083](http://localhost:8083/demo/ping).
* `nodejs-app`. Node.js service. Responds at [localhost:8084](http://localhost:8084/demo/ping).

## Load Test


We make use of [Apache Benchmark](https://httpd.apache.org/docs/2.4/programs/ab.html) `ab` to run load tests.