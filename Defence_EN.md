# Laboratory work 4 defence guide

Cross-cutting concerns with Spring AOP: logging, execution timing and auditing.

Authors: Akhmet Didar and Magazhan Nurdaulet. Group: IS-2405.

The project uses Gradle, Spring Boot 4.1.1 and Java 25. The individual variant is the student's position in the group list; the Lab 3 remainder formula does not apply. Confirm the assigned variant before presenting its aspect.

## Answers to all 16 defence questions

### 1. What is a cross-cutting concern? Which concerns are cross-cutting here?

A cross-cutting concern affects several operations rather than belonging to one business method. Here, logging arguments and results, measuring execution time and recording audit outcomes apply across service methods. Aspects keep this common behaviour separate from the catalog business logic.

### 2. Where are the pointcut and advice in the code?

The `@Pointcut` methods in `Pointcuts` describe which service executions match. They answer **where**. The annotated methods in `LoggingAspect`, `TimingAspect` and `AuditAspect` contain the behaviour to run. They answer **what to do**. Pointcuts can be combined and referenced by name.

### 3. Read within(kz.iitu.springlab.service..*). How do .. and * differ?

It selects executions within types in `kz.iitu.springlab.service` and its subpackages. In this type pattern, `..` covers zero or more package levels; `*` is a wildcard for a name component. It includes the service package itself, not only deeper packages. [Spring pointcuts](https://docs.spring.io/spring-framework/reference/core/aop/ataspectj/pointcuts.html)

### 4. Why are both @Aspect and @Component used?

`@Aspect` identifies advice and pointcuts. `@Component` makes the class a Spring bean through component scanning. `@Aspect` alone is not a component-scanning stereotype. Explicit registration with `@Bean` can replace `@Component`. [Declaring an aspect](https://docs.spring.io/spring-framework/reference/core/aop/ataspectj/at-aspectj.html)

### 5. In what order does one aspect's advice run?

With one advice of each type: around entry, before, target execution, after returning or after throwing, after finally, around exit. Around exit on failure requires appropriate `catch` or `finally` code. Ordering between multiple advice methods of the same type in one aspect is undefined. [Advice ordering](https://docs.spring.io/spring-framework/reference/core/aop/ataspectj/advice.html#advice-ordering)

### 6. What happens if proceed() is removed from @Around?

The remaining interceptor chain and target method are skipped. The caller receives whatever the around advice returns or throws. This is intentional for a cache hit, but would break our timing advice: it must call `proceed()` and return the result.

### 7. Why does @AfterReturning not fire for DELETE /api/lab4/item/0?

`remove(0)` throws `IllegalArgumentException` instead of returning normally. The matching `@AfterThrowing` advice receives the exception. A controller exception handler may later convert it into an HTTP error response; that does not turn the service execution into a normal return.

### 8. What is the throwing attribute for? What if its name is wrong?

`throwing = "ex"` binds the thrown exception to the advice parameter named `ex`. The parameter type also restricts matching. A mismatched name makes binding invalid, normally causing an advice configuration error rather than a working exception logger.

### 9. Why must the custom annotation have RUNTIME retention?

Spring must inspect the method annotation while the application is running. `RUNTIME` preserves that metadata for reflection. Default `CLASS` retention keeps it in the class file but does not expose it through ordinary runtime annotation reflection.

### 10. How does the Audited annotation instance reach the advice?

The binding expression `@annotation(audited)` matches an annotated method and supplies its annotation instance to the parameter named `audited`. The advice reads `action()` and `logArguments()` from it. Explicit `argNames` or compiler parameter metadata make the binding unambiguous. [Advice parameters](https://docs.spring.io/spring-framework/reference/core/aop/ataspectj/advice.html#advice-parameters)

### 11. Which aspect enters first and exits last?

Audit has order 1, logging order 2 and timing order 3. Audit enters first and closes last. On a successful audited call, the core log sequence is audit start, logging before, timing result, logging return, audit success. A lower order value means higher precedence.

### 12. Explain the proxy endpoint's generated class name.

`CatalogService$$SpringCGLIB$$...` identifies a generated subclass used as a class-based proxy. It intercepts eligible calls before delegating to the service target. The exact numeric suffix is an implementation detail, not an application version. Verify the `AopUtils` flags. [Proxying mechanisms](https://docs.spring.io/spring-framework/reference/core/aop/proxying.html)

### 13. Why does Boot use CGLIB even if an interface could be added?

Spring Boot defaults to class-based AOP proxies. With a suitable service interface, `spring.aop.proxy-target-class=false` allows JDK interface proxies. Merely adding an interface does not override Boot's default. Without a suitable interface, class-based proxying is still needed. [Spring Boot AOP](https://docs.spring.io/spring-boot/reference/features/aop.html)

### 14. Why is self-invocation not advised? Which fix is used?

An internal `this.remove(...)` call stays inside the target and bypasses the proxy. The fixed method delegates to an injected separate removal bean, so both removals cross that bean's proxy. This avoids self-injection and circular dependencies. [Self-invocation and refactoring](https://docs.spring.io/spring-framework/reference/core/aop/proxying.html#understanding-aop-proxies)

### 15. Can the aspect advise private or static methods?

Spring's proxy-based AOP cannot advise private methods because a subclass cannot override them. Static calls are class calls rather than intercepted instance dispatch. The service pointcut also explicitly selects public methods. AspectJ bytecode weaving has different capabilities. [Proxy limitations](https://docs.spring.io/spring-framework/reference/core/aop/proxying.html)

### 16. Name three Spring annotations using the same mechanism.

`@Transactional`, `@Cacheable` and `@Async` use interceptors on Spring-managed proxies in their usual proxy modes, when their support is enabled. Their self-invocations have the same limitation. [Transactions](https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/annotations.html), [caching](https://docs.spring.io/spring-framework/reference/integration/cache/annotations.html), [async execution](https://docs.spring.io/spring-framework/reference/integration/scheduling.html)

## Preparation before the demonstration

1. Start the application before the timed demonstration. Use the existing Gradle wrapper, for example `./gradlew.bat bootRun` in PowerShell.
2. Open `Pointcuts`, the three core aspects, `Audited`, `CatalogService`, the separate removal bean and the selected variant aspect in the IDE.
3. Keep the runtime log visible in another window. Begin from a clearly marked request boundary so unrelated startup messages do not confuse the counts.
4. Save the endpoint requests below. Keep the two self-invocation observation tables and the numbered successful-call screenshot ready.
5. Have the real `lab04` branch history and branch URL ready. Enter a remote URL only after a push has actually succeeded.

Spring Boot 4 renamed the old `spring-boot-starter-aop` starter to `spring-boot-starter-aspectj`. For this Gradle project the dependency is `implementation 'org.springframework.boot:spring-boot-starter-aspectj'`. The starter supports these `org.aspectj.lang.annotation` aspects; it does not mean that this project uses AspectJ bytecode weaving. [Official Boot migration guide](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide#aop-starter-pom)

## Five to six minute demonstration

The URLs below assume port 8080. Adjust the port if a profile changes it. Use the before and fixed endpoints separately; both remain available to make the comparison clear.

| Time | Show | Explain |
|---|---|---|
| 0:00–0:40 | Pointcuts and LoggingAspect | `within` selects the service package, `execution(public * *(..))` selects public executions; three logging advice methods handle arguments, normal result and exception. |
| 0:40–1:10 | GET item/5 | Logging before and after returning fire. Timing fires. Audit does not match this unannotated method. |
| 1:10–1:45 | GET items?limit=5 | Read the numbered core sequence: audit start, log entry, timing warning, log return, audit success. Explain orders 1, 2 and 3. |
| 1:45–2:15 | DELETE item/0 | Show the exception type/message and error response. After throwing replaces after returning; audit records failure and rethrows. |
| 2:15–2:50 | TimingAspect and Audited | `nanoTime` measures elapsed time; `finally` records timing even on failure; `proceed` calls the target. RUNTIME and annotation binding supply audit attributes. |
| 2:50–3:20 | GET proxy | Explain the generated subclass and actual `isAopProxy`/`isCglib` values. |
| 3:20–4:30 | Direct remove, then before and fixed remove-twice calls | Compare audit counts; point at internal `this` calls and separate-bean delegation. |
| 4:30–5:05 | Selected individual variant | Show the separate aspect, matching pointcut and endpoint/log demonstrating its effect. |
| 5:05–6:00 | Commit and questions | Show the real branch/history; answer questions with the relevant method or log line open. |

### Request sequence

PowerShell's `curl` can be an alias for another command, so use `curl.exe` explicitly:

```powershell
curl.exe -s http://localhost:8080/api/lab4/item/5
curl.exe -s "http://localhost:8080/api/lab4/items?limit=5"
curl.exe -i -X DELETE http://localhost:8080/api/lab4/item/0
curl.exe -s http://localhost:8080/api/lab4/proxy
curl.exe -s -X DELETE http://localhost:8080/api/lab4/item/5
curl.exe -s http://localhost:8080/api/lab4/remove-twice/5
curl.exe -s http://localhost:8080/api/lab4/remove-twice-fixed/5
```

For the invalid identifier, use the observed status and actual response in the report. The requirement is that the service exception reaches the caller as an error and remains visible to after-throwing advice.

### What to say about the successful slow call

“`findAll` is annotated with CATALOG_LIST, so audit starts first. Logging prints the arguments next. The target simulates a 300 millisecond operation. Timing reports the elapsed duration before logging prints the return value. Audit records success last. A warning is expected when the measured duration exceeds 200 milliseconds; the exact duration varies.”

### What to say about failure

“The invalid identifier throws an exception. After returning does not apply. After throwing records the exception type and message, timing still finishes, and audit records failure. The audit aspect rethrows the original exception rather than converting the call into a false success.”

### How to count the self-invocation experiment

Count an **audited operation** separately from individual **audit log entries**. With one start entry and one outcome entry per operation:

| Call | Audited operations expected | Audit entries expected | Actual count to enter from the runtime log |
|---|---:|---:|---|
| External DELETE item/5 | 1 | 2 | Count the start and success pair. |
| Original remove-twice/5 | 0 for the internal remove calls | 0 | Confirm no CATALOG_REMOVE audit pair appears. |
| Fixed remove-twice-fixed/5 | 2 | 4 | Confirm two CATALOG_REMOVE start/success pairs. |

For the original successful `removeTwice`, the core LoggingAspect invokes **two advice methods**: before and after returning for the outer method. Each internal `remove` invokes **zero** logging advice methods. Do not report “two interceptions of remove”: there was one intercepted outer execution and two direct inner calls.

For the fixed method, both inner removals cross the separate CatalogRemovalService bean's proxy. Both beans are in the service package and match the service pointcut, so the three successful executions produce six core logging advice invocations in total. Extra finally or variant messages must be counted separately.

Suggested explanation: “The original wrapper is intercepted because the controller calls its proxy. Once execution reaches the target, implicit `this.remove` calls bypass it. We preserve this endpoint as the before example. The fixed endpoint calls an injected removal bean twice, so its two proxy boundaries make audit, logging and timing run for each removal.”

## Final checklist

- The application starts and all catalog endpoints respond.
- Pointcut expressions are centralized and the service-operation pointcut combines the two named pointcuts.
- Logging prints method and arguments before a call, method and result after normal return, and method plus exception type/message after failure.
- Timing returns the target result, measures with `nanoTime`, logs in `finally` and warns above 200 milliseconds.
- `Audited` has METHOD target, RUNTIME retention, an action and `logArguments` defaulting to false.
- `findAll` uses CATALOG_LIST with arguments enabled; `remove` uses CATALOG_REMOVE; `findById` is not audited.
- Orders are audit 1, logging 2 and timing 3, confirmed by actual log sequence.
- Proxy data is captured from the endpoint, including the actual generated class name.
- The original self-invocation and separate-bean fix both have real before/after evidence.
- The assigned variant is a separate aspect and its effect is demonstrated.
- The report includes source listings, numbered success-log screenshot, failure-log screenshot, proxy response, completed observation table, variant result and a three or four sentence conclusion.
- The real branch is `lab04`; commit history and the branch link are accurate.

SelfInvocationTraceAspect adds `@After` finally advice to the two removeTwice demonstration methods. It runs after both normal and exceptional completion, providing the fifth advice type. Its TRACE message is separate from the five core records produced by findAll.
