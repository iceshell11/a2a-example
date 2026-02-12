package com.example.a2a.jsonrpc;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.Map;

@Component
public class JsonRpcDispatcher {

    private final Map<String, MethodHandler> methodRegistry = new HashMap<>();

    public void registerService(Object service) {
        Class<?> clazz = service.getClass();
        for (Method method : clazz.getDeclaredMethods()) {
            JsonRpcMethod annotation = method.getAnnotation(JsonRpcMethod.class);
            if (annotation != null) {
                String methodName = annotation.value();
                methodRegistry.put(methodName, new MethodHandler(service, method));
            }
        }
    }

    public Object dispatch(String methodName, JsonNode params) throws Exception {
        MethodHandler handler = methodRegistry.get(methodName);
        if (handler == null) {
            throw new JsonRpcException(-32601, "Method not found: " + methodName);
        }
        return handler.invoke(params);
    }

    public boolean hasMethod(String methodName) {
        return methodRegistry.containsKey(methodName);
    }

    private static class MethodHandler {
        private final Object service;
        private final Method method;
        private final Map<String, Integer> paramIndexMap = new HashMap<>();

        MethodHandler(Object service, Method method) {
            this.service = service;
            this.method = method;
            
            Parameter[] parameters = method.getParameters();
            for (int i = 0; i < parameters.length; i++) {
                JsonRpcParam paramAnnotation = parameters[i].getAnnotation(JsonRpcParam.class);
                if (paramAnnotation != null) {
                    paramIndexMap.put(paramAnnotation.value(), i);
                }
            }
        }

        Object invoke(JsonNode params) throws Exception {
            Object[] args = new Object[method.getParameterCount()];
            
            paramIndexMap.forEach((paramName, index) -> {
                JsonNode paramNode = params.path(paramName);
                args[index] = convertParam(paramNode, method.getParameterTypes()[index]);
            });

            try {
                return method.invoke(service, args);
            } catch (java.lang.reflect.InvocationTargetException e) {
                Throwable cause = e.getCause();
                if (cause instanceof TaskException) {
                    throw (TaskException) cause;
                }
                if (cause instanceof RuntimeException) {
                    throw (RuntimeException) cause;
                }
                throw e;
            }
        }

        private Object convertParam(JsonNode node, Class<?> targetType) {
            if (node.isMissingNode() || node.isNull()) {
                return null;
            }
            if (targetType == String.class) {
                return node.asText();
            }
            if (targetType == int.class || targetType == Integer.class) {
                return node.asInt();
            }
            if (targetType == boolean.class || targetType == Boolean.class) {
                return node.asBoolean();
            }
            if (targetType == JsonNode.class) {
                return node;
            }
            return null;
        }
    }

    public static class JsonRpcException extends Exception {
        private final int code;

        public JsonRpcException(int code, String message) {
            super(message);
            this.code = code;
        }

        public int getCode() { return code; }
    }
}
