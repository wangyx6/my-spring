package com.rachel.spring.Handler;

import java.lang.reflect.Method;
import java.util.*;
import java.util.regex.Pattern;

/**
 * 主要用于请求method对象的封装，其中包含了，要执行的对象，Method方法，
 * @author wangyx
 * @see MethodHandler
 * @since 2020/1/27
 */
public class MethodHandler {

    private Object invokeObject;

    private Method invokeMethod;

    /**
     * 主要用于存储执行方法的参数顺序，key为参数名，value为参数的顺序
     */
    private Map<String, Integer> paramIndexMapping;

    /**
     * 存储请求url
     */
    private Pattern pattern;

    private Set<String> authUsers;


    public MethodHandler(Object invokeObject, Method invokeMethod, Pattern pattern) {
        this.invokeObject = invokeObject;
        this.invokeMethod = invokeMethod;
        this.pattern = pattern;
        this.authUsers = new HashSet<>();
        paramIndexMapping = new HashMap<String, Integer>();
    }

    public Object getInvokeObject() {
        return invokeObject;
    }

    public void setInvokeObject(Object invokeObject) {
        this.invokeObject = invokeObject;
    }

    public Method getInvokeMethod() {
        return invokeMethod;
    }

    public void setInvokeMethod(Method invokeMethod) {
        this.invokeMethod = invokeMethod;
    }

    public Map<String, Integer> getParamIndexMapping() {
        return paramIndexMapping;
    }

    public void setParamIndexMapping(Map<String, Integer> paramIndexMapping) {
        this.paramIndexMapping = paramIndexMapping;
    }

    public Pattern getPattern() {
        return pattern;
    }

    public void setPattern(Pattern pattern) {
        this.pattern = pattern;
    }

    public Set<String> getAuthUsers() {
        return authUsers;
    }

    public void setAuthUsers(Set<String> authUsers) {
        this.authUsers = authUsers;
    }
}
