package com.example.a2a.jsonrpc;

import java.lang.annotation.*;

@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface JsonRpcParam {
    String value();
}
