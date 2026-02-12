package com.example.a2a.jsonrpc;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface JsonRpcMethod {
    String value();
}
