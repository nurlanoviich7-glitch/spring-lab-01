package kz.iitu.txlab.web;
import java.util.*;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import kz.iitu.txlab.service.*;

@RestControllerAdvice
public class ApiErrors {
    @ExceptionHandler(MissingAccount.class) public ProblemDetail missing(MissingAccount e){return problem(404,e.getMessage());}
    @ExceptionHandler({BusinessConflict.class,OptimisticLockingFailureException.class,jakarta.persistence.OptimisticLockException.class})
    public ProblemDetail conflict(Exception e){return problem(409,e instanceof BusinessConflict?e.getMessage():"Concurrent update rejected; reload the account");}
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail constraint(Exception e){return problem(409,"The operation conflicts with a database constraint");}
    @ExceptionHandler(IllegalArgumentException.class) public ProblemDetail bad(IllegalArgumentException e){return problem(400,e.getMessage());}
    @ExceptionHandler(MethodArgumentNotValidException.class) public ProblemDetail invalid(MethodArgumentNotValidException e){
        ProblemDetail p=problem(400,"Request validation failed");
        p.setProperty("errors",e.getBindingResult().getFieldErrors().stream().map(v->Map.of("field",v.getField(),"message",Objects.requireNonNullElse(v.getDefaultMessage(),"Invalid value"))).toList());return p;
    }
    @ExceptionHandler(HttpMessageNotReadableException.class) public ProblemDetail json(Exception e){return problem(400,"Malformed JSON request");}
    private ProblemDetail problem(int status,String detail){return ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(status),detail);}
}
