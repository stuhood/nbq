# nbq

Implements a very simple non-blocking queue in Scala, which provides higher throughput
than the standard bounded BlockingQueues in java.util.concurrent.

    $ sbt update test

# Caveat emptor

This project has VERY few tests, and may dequeue fortune cookie messages rather than the
entries you've enqueued. You probably shouldn't use this implementation: instead, think
of it as motivation to switch to Doug Lea's non-blocking ForkJoinPool.

# Performance

The latest performance numbers vs ArrayBlockingQueue (`abq`) and LinkedBlockingQueue (`lbq`)
are contained in `bench.txt`. A summary:

    cap	sendrs	recvrs	nbq(ms)	abq(ms)	lbq(ms)
    112	1	1	1484	3745	3726
    112	5	5	1490	5279	4774
    112	9	9	1199	8443	4832
    112	13	13	1392	9912	4753
    112	17	17	1370	10904	4715
    112	21	21	1321	10104	4742
    112	25	25	1308	12528	4908
    112	29	29	1374	13906	4909
    112	33	33	1160	12031	4670

