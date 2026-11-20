package com.easybytes.accounts.exception;

public class ResourceNotFoundException extends RuntimeException{
    public ResourceNotFoundException(String ressourceName, String fieldName, String fieldValue) {

        super(String.format("%s not found with the given input data %s: %s", ressourceName, fieldName, fieldValue));
    }
}
