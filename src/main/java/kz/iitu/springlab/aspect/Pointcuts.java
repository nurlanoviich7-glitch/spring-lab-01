package kz.iitu.springlab.aspect;

import kz.iitu.springlab.audit.Audited;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

/** The single location for all AspectJ selection expressions. */
@Aspect
@Component
public class Pointcuts {
    @Pointcut("within(kz.iitu.springlab.service..*)")
    public void serviceLayer() { }

    @Pointcut("execution(public * *(..))")
    public void publicMethod() { }

    @Pointcut("serviceLayer() && publicMethod()")
    public void serviceOperation() { }

    @Pointcut(value = "@annotation(audited)", argNames = "audited")
    public void auditedOperation(Audited audited) { }

    @Pointcut("execution(public String kz.iitu.springlab.service.CatalogService.removeTwice*(long))")
    public void selfInvocationDemo() { }
}
