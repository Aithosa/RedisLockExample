# RedisLockExample

This project provides implementations and demonstrations for some common Redis lock usage scenarios.
If it serves as an inspiration for your work and study, we will be honored.
For detailed project structure information, please continue reading this document or directly inspect the code.

## Prerequisites

Before you begin, ensure you have the following installed on your machine:

- JDK 17 or higher
- Maven 3.8.1 or higher
- Redis server

## Project Structure and Usage

Overall, this project demonstrates two ways to use Redis locks:
one based on annotations and the other by manually controlling the scope of the Redis lock.
Whether you are learning or reading the source code,
it is recommended to start with the following scenarios to clearly understand the implementation details.

### Annotation-Based Usage

For annotation-based usage, this project presents two scenarios.

The first scenario is for the order submission interface.
For more details, see the `OrderController.java` file in the `com.example.redislock.controller` directory.
To prevent multiple submissions of the same order,
the interface will ignore subsequent duplicate requests after receiving the first one.
How to identify the same request can be distinguished based on the field values in the request,
which can be customized according to your business needs.
Simply extend the request to implement `ILockable` and define the rules in the `getLockKey()` method.
Then, in the `Controller`, add the `@RedisLockCheck` annotation to the interface's request that needs to be locked,
and you can filter out duplicate requests.
Subsequent duplicate requests will be identified and return an error response.

The second scenario targets situations similar to scheduled tasks.
No matter the reason (possibly manual triggering), when a scheduled task is retriggered before it is completed,
adding the `@RedisLock` annotation
and specifying the unique identifier of the task can automatically ignore subsequent duplicate trigger requests.

### Manual Control of Redis Locks

This section provides two implementations:

The first is a simple implementation,
see [LockService.java](src%2Fmain%2Fjava%2Fcom%2Fexample%2Fredislock%2Fservice%2Flock%2Fbase%2FLockService.java).
You can achieve locking by calling `lock()` and unlocking by calling `unlock()`.

The second implementation is slightly more complex,
see [ComplexLockService.java](src%2Fmain%2Fjava%2Fcom%2Fexample%2Fredislock%2Fservice%2Flock%2Fbase%2FComplexLockService.java).
This class is unique
in that it provides a `Map<String, LocalDateTime> locks = new ConcurrentHashMap<>();`
to store the added locks and their maximum expiration time after successfully acquiring the lock.
It also requires removing the record when unlocking.
The reason for adding records is mainly considering
that if the program runs for too long and exceeds the lock expiration time,
it will cause the lock acquisition to fail.
Therefore, a scheduled task `refreshLockXXX()` is added,
which automatically extends the lock time if it is found that the lock has not expired
(an appropriate scheduled task execution plan needs to be set).

- `lockOrder()` and `unlockOrder()` can lock and unlock by passing in the order number (a unique identifier).
- `lock()` and `unlock()` are the simple implementation version.
- `lockWithRetry()` and `unlockWithRetry()` add a retry mechanism for locking and unlocking, but I think if locking and unlocking fail, it is likely due to network or other issues, and retrying in a short time may not increase the success rate.

Scheduled Tasks:

- `refreshLock()` regularly checks the lock records, and if the lock has not expired, it automatically extends the lock's expiration time, but this method can only extend once.
- `refreshLockWithRetry()` regularly checks the lock records and adds a retry mechanism. If the lock has not expired, it automatically extends the lock's expiration time, but this method can only extend once.
- `refreshLockWithoutLimit()` regularly checks the lock records, adds a retry mechanism, and if the lock has not expired, it automatically extends the lock's expiration time indefinitely as long as the task is still running.

## Usage

This project contains examples of how to implement and use Redis for distributed locking in a Java application. The examples include:

- Lock acquisition and release
- Timeout handling
- Error handling

## Built With

- [Spring Boot](https://spring.io/projects/spring-boot) - Framework for building Spring applications.
- [Redis](https://redis.io/) - In-memory data structure store, used as a database, cache, and message broker.
- [Lombok](https://projectlombok.org/) - Java library that automatically plugs into your editor and build tools.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
