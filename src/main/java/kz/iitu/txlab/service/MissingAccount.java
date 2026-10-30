package kz.iitu.txlab.service;
public class MissingAccount extends RuntimeException { public MissingAccount(long id){super("Account "+id+" was not found");} }
