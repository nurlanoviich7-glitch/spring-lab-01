# Laboratory Work 4

This project demonstrates logging, execution timing and user action auditing with Spring AOP. It includes a runtime proxy inspection, the self invocation experiment, a fix using a separate bean, and the variant 1 call counter.

Authors: Akhmet Didar and Magazhan Nurdaulet, IS-2405.

## Run in IntelliJ IDEA

1. Reload the Gradle project after opening `build.gradle`.
2. Select JDK 25 as the project SDK and Gradle JVM.
3. Run `SpringLab01Application.main()`.
4. Open `http://localhost:8080/api/lab4/proxy` to confirm that the application is running.

The project uses Spring Boot 4.1.1 and the compatible `spring-boot-starter-aspectj` dependency. The laboratory sheet uses the earlier Maven starter name; `build.gradle` provides the equivalent dependency for this project.

## Build and test in PowerShell

```powershell
$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-25.0.4.101-hotspot'
.\gradlew.bat test bootJar
.\gradlew.bat bootRun
```

## Demonstration

```powershell
Invoke-RestMethod 'http://localhost:8080/api/lab4/items?limit=5'
Invoke-RestMethod 'http://localhost:8080/api/lab4/item/5'
Invoke-RestMethod -Method Delete 'http://localhost:8080/api/lab4/item/5'
Invoke-RestMethod -Method Delete 'http://localhost:8080/api/lab4/item/0'
Invoke-RestMethod 'http://localhost:8080/api/lab4/proxy'
Invoke-RestMethod 'http://localhost:8080/api/lab4/remove-twice/5'
Invoke-RestMethod 'http://localhost:8080/api/lab4/remove-twice-fixed/5'
Invoke-RestMethod 'http://localhost:8080/api/lab4/statistics'
```

The invalid DELETE intentionally returns HTTP 400, so PowerShell displays an error for that one request. Its exception reaches the logging and audit advice before the controller converts it into an HTTP response.

For `findAll`, read the log in this order: audit start, logging before, timing warning, logging return, audit success. `findById` has logging and timing, without audit.

`removeTwice` uses `this.remove` and therefore produces no inner audit records. `removeTwiceFixed` calls the injected `CatalogRemovalService` bean, crossing its proxy twice and producing two audit starts and two success records. A small `@After` trace on the two demonstration methods also shows finally advice, including on failure.

The statistics endpoint counts service calls intercepted by the named service layer pointcut. Internal calls through `this` do not increment the count for `remove`. Counts start at zero after an application restart. A snapshot taken during concurrent requests can reflect counts from slightly different instants.

The lab 1 and lab 2 endpoints remain available. See `Defence_EN.md` for the explanation and the complete set of defence answers.
