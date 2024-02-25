To run the following tests, first launch the container as described [here](./README.md#docker-compose).

### Comparing Spring Boot running with and without virtual threads

We have our normal Java Spring Boot application running with traditional threads responding on port 8081 and the same application running with virtual threads responding on port 8082. Both expose two apis: `/demo/cpu-light` and `/demo/cpu-intensive`, where the first one simulates two non-blocking IO operations of 200ms each and the second adds a CPU intensive operation of 100ms. The Tomcat server has been configured to run on a thread pool of 50 threads.

#### Testing the CPU light scenario

First some normal flow verification. We will use `ab` to send 1000 requests with concurrency 50 to the `/demo/cpu-light` endpoint of both services. Given that Tomcat can run the requests on 50 threads in parallel (on both services) we expect the requests to be served in 20 batches of 50 requests each. Every request should take around 400ms to be served, given the IO non-blocking operations simulated in the controller. SO we expect every request to be served in around 400ms and a total of 1000/50 * 400ms = 8000ms = 8s to serve the whole test. This should be the same in both services given that we have enough threads to serve the requests in parallel.

###### Normal Threads
```sh
ab -n 1000 -c 50 http://localhost:8081/demo/cpu-light  
```

```
Server Software:        
Server Hostname:        localhost
Server Port:            8081

Document Path:          /demo/cpu-light
Document Length:        13 bytes

Concurrency Level:      50
Time taken for tests:   8.902 seconds
Complete requests:      1000
Failed requests:        0
Total transferred:      146000 bytes
HTML transferred:       13000 bytes
Requests per second:    112.33 [#/sec] (mean)
Time per request:       445.119 [ms] (mean)
Time per request:       8.902 [ms] (mean, across all concurrent requests)
Transfer rate:          16.02 [Kbytes/sec] received

Connection Times (ms)
              min  mean[+/-sd] median   max
Connect:        0    1   1.1      1      12
Processing:   407  423   6.1    422     441
Waiting:      406  422   5.9    422     441
Total:        409  424   5.8    424     441

Percentage of the requests served within a certain time (ms)
  50%    424
  66%    426
  75%    428
  80%    430
  90%    432
  95%    435
  98%    437
  99%    438
 100%    441 (longest request)

```

###### Virtual Threads
```sh
ab -n 1000 -c 50 http://localhost:8082/demo/cpu-light  
```

```
Server Software:        
Server Hostname:        localhost
Server Port:            8082

Document Path:          /demo/cpu-light
Document Length:        13 bytes

Concurrency Level:      50
Time taken for tests:   8.948 seconds
Complete requests:      1000
Failed requests:        0
Total transferred:      146000 bytes
HTML transferred:       13000 bytes
Requests per second:    111.75 [#/sec] (mean)
Time per request:       447.424 [ms] (mean)
Time per request:       8.948 [ms] (mean, across all concurrent requests)
Transfer rate:          15.93 [Kbytes/sec] received

Connection Times (ms)
              min  mean[+/-sd] median   max
Connect:        0    1   1.1      1       4
Processing:   412  425   6.0    424     444
Waiting:      409  425   5.8    424     442
Total:        412  427   5.9    426     445

Percentage of the requests served within a certain time (ms)
  50%    426
  66%    428
  75%    430
  80%    431
  90%    434
  95%    439
  98%    443
  99%    444
 100%    445 (longest request)

```

The data confirm the hypothesis. The requests are served in around 400ms and the total time is around 8.9s in both services.

Now, Virtual Threads use threads more efficiently by not non-blocking them on IO operations. While a non-blocking IO operation is performed (as the one simulated in our controller with the `Threads.sleep()`), the thread is released to serve other requests. This should allow the server running with Virtual Threads to serve more requests, but this would be noticeable only if we have more than 50 requests in parallel.

###### Normal Threads
```sh
ab -n 1000 -c 100 http://localhost:8081/demo/cpu-light  
```

```
Server Software:        
Server Hostname:        localhost
Server Port:            8081

Document Path:          /demo/cpu-light
Document Length:        13 bytes

Concurrency Level:      100
Time taken for tests:   8.537 seconds
Complete requests:      1000
Failed requests:        0
Total transferred:      146000 bytes
HTML transferred:       13000 bytes
Requests per second:    117.14 [#/sec] (mean)
Time per request:       853.684 [ms] (mean)
Time per request:       8.537 [ms] (mean, across all concurrent requests)
Transfer rate:          16.70 [Kbytes/sec] received

Connection Times (ms)
              min  mean[+/-sd] median   max
Connect:        0    1   1.5      1      17
Processing:   409  790  85.2    809     838
Waiting:      406  790  85.1    808     838
Total:        409  792  84.4    810     842

Percentage of the requests served within a certain time (ms)
  50%    810
  66%    812
  75%    813
  80%    814
  90%    818
  95%    826
  98%    835
  99%    839
 100%    842 (longest request)

```

The total execution time is about the same as the previous test. In fact, we are running batches of 100 requests concurrently, but since Tomcat can only parallelize 50 of them and each takes 400ms, we have every batch executed in around 800ms. The total execution time is still 8s, but the average time per request is now 800ms.

###### Virtual Threads
```sh
ab -n 1000 -c 100 http://localhost:8082/demo/cpu-light  
```

```
Server Software:        
Server Hostname:        localhost
Server Port:            8082

Document Path:          /demo/cpu-light
Document Length:        13 bytes

Concurrency Level:      100
Time taken for tests:   4.701 seconds
Complete requests:      1000
Failed requests:        0
Total transferred:      146000 bytes
HTML transferred:       13000 bytes
Requests per second:    212.74 [#/sec] (mean)
Time per request:       470.057 [ms] (mean)
Time per request:       4.701 [ms] (mean, across all concurrent requests)
Transfer rate:          30.33 [Kbytes/sec] received

Connection Times (ms)
              min  mean[+/-sd] median   max
Connect:        0    4   2.1      4       8
Processing:   411  424   6.5    422     446
Waiting:      411  423   6.4    422     446
Total:        414  428   6.2    427     448

Percentage of the requests served within a certain time (ms)
  50%    427
  66%    429
  75%    432
  80%    433
  90%    436
  95%    438
  98%    444
  99%    446
 100%    448 (longest request)

```

With Virtual Threads enabled, instead, Java optimizes the usage of the Thread waiting for the IO operation to be completed, to serve another request. Essentially on Thread is serving two requests in the same time it takes for the normal threads to serve one. This is why the total execution time is now 4.7s, half of the time of the previous test. The average time per request is now 470ms, half of the time of the previous test.

So we can see that Virtual Threads are more efficient in serving requests that are IO bound.

#### Testing the CPU intensive scenario

Now we will test the `/demo/cpu-intensive` endpoint. This endpoint simulates a CPU intensive operation with an empty while loop of 100ms. This operation can't be optimised by Virtual Threads, since the thread is doing actual work here and not just waiting for an external callback.


###### Normal Threads
```sh
ab -n 1000 -c 100 http://localhost:8081/demo/cpu-intensive
```

```
Server Software:        
Server Hostname:        localhost
Server Port:            8081

Document Path:          /demo/cpu-intensive
Document Length:        13 bytes

Concurrency Level:      100
Time taken for tests:   11.028 seconds
Complete requests:      1000
Failed requests:        0
Total transferred:      146000 bytes
HTML transferred:       13000 bytes
Requests per second:    90.68 [#/sec] (mean)
Time per request:       1102.814 [ms] (mean)
Time per request:       11.028 [ms] (mean, across all concurrent requests)
Transfer rate:          12.93 [Kbytes/sec] received

Connection Times (ms)
              min  mean[+/-sd] median   max
Connect:        0    1   1.2      0       6
Processing:   511 1013 115.1   1016    1113
Waiting:      507 1012 115.1   1016    1112
Total:        514 1013 114.4   1017    1118

Percentage of the requests served within a certain time (ms)
  50%   1017
  66%   1066
  75%   1073
  80%   1075
  90%   1081
  95%   1096
  98%   1114
  99%   1114
 100%   1118 (longest request)
```

The total execution time is about 11s, and the average time per request is 1100ms. This is expected since the single api request has a total execution time of 500ms (200ms IO + 100ms CPU + 200ms IO) and we are running 100 requests in parallel with a thread pool of 50. Let's see how the Virtual Threads perform.

###### Virtual Threads
```sh
ab -n 1000 -c 100 http://localhost:8082/demo/cpu-intensive
```

```
Server Software:        
Server Hostname:        localhost
Server Port:            8082

Document Path:          /demo/cpu-intensive
Document Length:        13 bytes

Concurrency Level:      100
Time taken for tests:   27.576 seconds
Complete requests:      1000
Failed requests:        0
Total transferred:      146000 bytes
HTML transferred:       13000 bytes
Requests per second:    36.26 [#/sec] (mean)
Time per request:       2757.570 [ms] (mean)
Time per request:       27.576 [ms] (mean, across all concurrent requests)
Transfer rate:          5.17 [Kbytes/sec] received

Connection Times (ms)
              min  mean[+/-sd] median   max
Connect:        0    2   1.3      2       6
Processing:   503 2636 373.1   2605    4894
Waiting:      503 2636 373.1   2605    4894
Total:        505 2638 373.1   2606    4895

Percentage of the requests served within a certain time (ms)
  50%   2606
  66%   2702
  75%   2705
  80%   2707
  90%   2714
  95%   2717
  98%   2913
  99%   4796
 100%   4895 (longest request)
```

The application running on Virtual Threads performs poorly compared to the one running on normal threads. In fact, the total execution time is about 27s, and the average time per request is 2700ms.

### Comparing a mono-threaded Spring Boot with Virtual Threads and Node.js

Let's now compare the mono-threaded Spring Boot application running with Virtual Threads with a Node.js application. The reason for this comparison is that Java Virtual Threads work conceptually very similarly to how the event loop works in Node.js and Node.js being mono-threaded, we can compare the performance of the two in a similar scenario.

#### Testing the CPU light scenario

###### Spring Boot

```sh
ab -n 1000 -c 100 http://localhost:8083/demo/cpu-light
```
```
Server Software:        
Server Hostname:        localhost
Server Port:            8083

Document Path:          /demo/cpu-light
Document Length:        13 bytes

Concurrency Level:      100
Time taken for tests:   4.829 seconds
Complete requests:      1000
Failed requests:        0
Total transferred:      146000 bytes
HTML transferred:       13000 bytes
Requests per second:    207.10 [#/sec] (mean)
Time per request:       482.851 [ms] (mean)
Time per request:       4.829 [ms] (mean, across all concurrent requests)
Transfer rate:          29.53 [Kbytes/sec] received

Connection Times (ms)
              min  mean[+/-sd] median   max
Connect:        0    2   1.8      2       8
Processing:   409  430  11.6    429     501
Waiting:      409  429  11.4    427     498
Total:        412  432  11.2    431     501

Percentage of the requests served within a certain time (ms)
  50%    431
  66%    437
  75%    441
  80%    441
  90%    447
  95%    453
  98%    457
  99%    460
 100%    501 (longest request)
```
###### Node.js

```sh
ab -n 1000 -c 100 http://localhost:8084/demo/cpu-light
```
```
Server Software:        
Server Hostname:        localhost
Server Port:            8084

Document Path:          /demo/cpu-light
Document Length:        12 bytes

Concurrency Level:      100
Time taken for tests:   5.007 seconds
Complete requests:      1000
Failed requests:        0
Total transferred:      211000 bytes
HTML transferred:       12000 bytes
Requests per second:    199.72 [#/sec] (mean)
Time per request:       500.699 [ms] (mean)
Time per request:       5.007 [ms] (mean, across all concurrent requests)
Transfer rate:          41.15 [Kbytes/sec] received

Connection Times (ms)
              min  mean[+/-sd] median   max
Connect:        0    4   2.5      5       9
Processing:   411  449  14.4    447     483
Waiting:      407  433  12.7    433     474
Total:        414  454  13.6    454     485

Percentage of the requests served within a certain time (ms)
  50%    454
  66%    455
  75%    456
  80%    457
  90%    481
  95%    482
  98%    484
  99%    484
 100%    485 (longest request)
```

In a scenario where only IO non-blocking operations are involved, the two services perform similarly. The total execution time is about 5s, and the average time per request is 500ms, which is expected given that the IO non-blocking operations take 400ms in total and we are running 100 requests in parallel.


#### Testing the CPU intensive scenario

Let's check out now how the two applications perform when a CPU intensive operation is involved.

###### Spring Boot

```sh
ab -n 1000 -c 100 http://localhost:8083/demo/cpu-intensive
```
```
Server Software:        
Server Hostname:        localhost
Server Port:            8083

Document Path:          /demo/cpu-intensive
Document Length:        13 bytes

Concurrency Level:      100
Time taken for tests:   27.926 seconds
Complete requests:      1000
Failed requests:        0
Total transferred:      146000 bytes
HTML transferred:       13000 bytes
Requests per second:    35.81 [#/sec] (mean)
Time per request:       2792.639 [ms] (mean)
Time per request:       27.926 [ms] (mean, across all concurrent requests)
Transfer rate:          5.11 [Kbytes/sec] received

Connection Times (ms)
              min  mean[+/-sd] median   max
Connect:        0    1   0.8      0       4
Processing:   608 2702  72.7   2710    2906
Waiting:      604 2702  72.8   2710    2906
Total:        609 2702  72.7   2711    2908
WARNING: The median and mean for the initial connection time are not within a normal deviation
        These results are probably not that reliable.

Percentage of the requests served within a certain time (ms)
  50%   2711
  66%   2712
  75%   2713
  80%   2714
  90%   2716
  95%   2717
  98%   2720
  99%   2721
 100%   2908 (longest request)
```

###### Node.js

```sh
ab -n 1000 -c 100 http://localhost:8084/demo/cpu-intensive
```
```
Server Software:        
Server Hostname:        localhost
Server Port:            8084

Document Path:          /demo/cpu-intensive
Document Length:        12 bytes

Concurrency Level:      100
Time taken for tests:   102.977 seconds
Complete requests:      1000
Failed requests:        0
Total transferred:      211000 bytes
HTML transferred:       12000 bytes
Requests per second:    9.71 [#/sec] (mean)
Time per request:       10297.737 [ms] (mean)
Time per request:       102.977 [ms] (mean, across all concurrent requests)
Transfer rate:          2.00 [Kbytes/sec] received

Connection Times (ms)
              min  mean[+/-sd] median   max
Connect:        0    1   1.2      1       5
Processing:   560 10214 307.3  10234   10425
Waiting:      550 10205 307.2  10223   10425
Total:        561 10215 307.3  10235   10425

Percentage of the requests served within a certain time (ms)
  50%  10235
  66%  10237
  75%  10238
  80%  10239
  90%  10246
  95%  10259
  98%  10260
  99%  10260
 100%  10425 (longest request)

```

In a scenario where a CPU intensive operation is involved, the Spring Boot application running on Virtual Threads performs much better than the Node.js application. The total execution time is about 28s, and the average time per request is 2800ms, while the Node.js application takes about 103s to serve the same requests, with an average time per request of 10300ms. 
What's interesting is that in a CPU intensive scenario the two Spring Boot applications running with Virtual (multi and mono threaded) perform similarly.