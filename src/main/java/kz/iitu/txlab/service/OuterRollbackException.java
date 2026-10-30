package kz.iitu.txlab.service;
public class OuterRollbackException extends RuntimeException {
    public final long outerTransaction,auditTransaction;
    public OuterRollbackException(long outer,long audit){
        super("Outer transaction deliberately rolled back after audit returned");
        outerTransaction=outer;auditTransaction=audit;
    }
}
