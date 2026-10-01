# Laboratory work 4 individual variant 1

This project provisionally implements **variant 1, Call counter**. The supplied laboratory instructions define the variant as your position in the group list. The actual position has not been provided, so confirm it before submission. The modulo formula from laboratory work 3 does not apply to laboratory work 4.

The report authors are **Akhmet Didar and Magazhan Nurdaulet**, group **IS-2405**.

`CallCounterAspect` is a separate `@Aspect` and `@Component` with `@Order(4)`. Its `@Before` advice refers to the existing named `Pointcuts.serviceOperation()` pointcut. It increments a `ConcurrentHashMap<String, LongAdder>` entry for the method name before each intercepted call. This counts attempted calls, including calls that subsequently throw an exception. Methods with the same name share an entry, as required by the per-method-name assignment.

The pointcut selects public methods of proxied Spring beans in `kz.iitu.springlab.service` and its subpackages. It does not count controller calls, aspect methods, or calls in other packages. A call through `this` bypasses the proxy and does not increment the called method's count. The fixed demonstration crosses into a separate removal bean in the service package, so its two proxied `remove` calls are counted. Both removal beans contribute to the same `remove` entry because the assignment groups counts by method name.

The counter emits no log lines, so it does not add records to the required audit, logging, and timing sequence. `GET /api/lab4/statistics` returns a sorted JSON object of the current counts. For example, after one proxied call to `findById` and one to `remove`:

```json
{
  "findById": 1,
  "remove": 1
}
```

Use the endpoint on the application's actual port, for example:

```powershell
Invoke-RestMethod http://localhost:8080/api/lab4/item/1
Invoke-RestMethod -Method Delete http://localhost:8080/api/lab4/item/5
Invoke-RestMethod http://localhost:8080/api/lab4/statistics
```

Counts accumulate for the lifetime of the application process. Restart the application to begin a fresh demonstration. Reading the statistics does not increment service counts.

The returned map is an immutable copy with sorted keys. During simultaneous requests, the snapshot is **weakly consistent**: it may observe different keys at slightly different instants, and it is not an atomic snapshot of every call. Once the concurrent calls have completed, their increments are preserved and the final counts can be checked exactly.

`CallCounterAspectTest` exercises the real named pointcut through `AspectJProxyFactory` and a fast test target in the service package. It checks successful and failing direct calls, the two uncounted `this.remove` calls, 800 calls from eight concurrent workers, sorted immutable snapshots, and the statistics controller's lack of side effects. Run the tests with the project's Gradle wrapper:

```powershell
.\gradlew.bat test
```
