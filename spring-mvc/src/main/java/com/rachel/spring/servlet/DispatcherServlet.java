package com.rachel.spring.servlet;

import com.rachel.spring.Handler.MethodHandler;
import com.rachel.spring.annocation.*;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.regex.Pattern;

public class DispatcherServlet extends HttpServlet {

    private Properties scanPackageProperties = new Properties();

    private Map<String, Object> beans = new HashMap<String, Object>();

    private List<String> scanBeanClassNames = new ArrayList<String>();

    private List<MethodHandler> methodsHandlers = new ArrayList<MethodHandler>();

    private static String authUserParam = "username";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // 处理具体的请求逻辑

        // 根据请求的url地址，获取到具体执行的方法
        String requestURI = req.getRequestURI();
        MethodHandler methodHandler = getMethodByUrl(requestURI);
        // 是否有映射的请求地址
        if(methodHandler == null) {
            resp.getWriter().write("404 not found");
            return;
        }
        // 判断权限集合
        if(!auth(req, methodHandler)){
            resp.getWriter().write("auth failer...");
            return;
        }

        // 获取到request中的参数
        Map<String, String[]> reqParameterMap = req.getParameterMap();

        // method执行的参数数组
        Object args[] = new Object[methodHandler.getParamIndexMapping().size()];
        // 自动处理请求参数赋值
        for (Map.Entry<String, String[]> entity : reqParameterMap.entrySet()) {
            // 前端可能存在的请求方式为，同一个参数，传递多个值 name=1,name=2
            String value = StringUtils.join(entity.getValue(), ",");

            // 判断请求参数中，是否包含该参数名称的请求参数
            if(!methodHandler.getParamIndexMapping().containsKey(entity.getKey())){
                continue;
            }
            // 处理请求参数
            Integer index = methodHandler.getParamIndexMapping().get(entity.getKey());
            args[index] = value;
        }

        // 处理request 和 response
        int requestIndex = methodHandler.getParamIndexMapping().get(HttpServletRequest.class.getSimpleName());
        args[requestIndex] = req;
        int responseIndex = methodHandler.getParamIndexMapping().get(HttpServletResponse.class.getSimpleName());
        args[responseIndex] = resp;
        // 执行具体的方法
        try {
            Object invoke = methodHandler.getInvokeMethod().invoke(methodHandler.getInvokeObject(), args);
            resp.getWriter().write(invoke.toString());
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }


    }

    public boolean auth(HttpServletRequest request, MethodHandler methodHandler){
        // 获取到请求的用户名
        String userName = request.getParameter(authUserParam);
        if(null == userName || "".equals(userName.trim())){
            return false;
        }
        // 获取到用户名不为空，则根据handler进行校验
        return methodHandler.getAuthUsers().contains(userName);
    }

    public MethodHandler getMethodByUrl(String requestURI){
        if(methodsHandlers.isEmpty()){
            return null;
        }
        for (MethodHandler methodsHandler : methodsHandlers) {
            if(methodsHandler.getPattern().matcher(requestURI).matches()){
                return methodsHandler;
            }
        }
        return null;
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        try {
            // 1,读取配置文件
            String location = config.getInitParameter("contextLocation");
            doReadProperties(location);

            // 2，根据注解，扫描并加载bean对象
            doScan(scanPackageProperties.getProperty("scanPackage"));

            // 3.完成对象的实例化
            doInstance();

            // 4，根据依赖注入，完成对象之间属性的注入
            doSetProperties();

            // 4，完成请求uri的扫描配置
            initHandlerMapping();
            System.out.println("mvc 框架初始化完成....");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initHandlerMapping() {
        // 循环遍历已经扫描的bean对象
        if(beans.size() == 0) {return;}
        for (Map.Entry<String, Object> entity : beans.entrySet()) {
            //获取到当前对象是否标记了 MyRequestMapping
            Object object = entity.getValue();

            // 访问控制权限集合
            List<String> classAuthUsers = new ArrayList<>();

            // 请求地址
            String requestUri = null;
            // 获取到类上标记的注解信息
            if(object.getClass().isAnnotationPresent(MyRequestMapping.class)){
                MyRequestMapping myRequestMapping = object.getClass().getAnnotation(MyRequestMapping.class);
                if(!"".equals(myRequestMapping.value().trim())){
                    requestUri = myRequestMapping.value();
                }
            }
            // 获取类上是否有标记 用户集合,并将获取到的权限集合添加到该类上的权限集合
            classAuthUsers.addAll(getAuthInfos(object.getClass().getAnnotation(Security.class)));

            // 获取具体方法上标记的请求url信息
            Method[] methods = entity.getValue().getClass().getMethods();
            for (int i = 0;  i < methods.length; i++) {
                Method method = methods[i];
                // 判断方法上是否标记了 RequestMapping注解
                if(method.isAnnotationPresent(MyRequestMapping.class)){
                    MyRequestMapping myRequestMapping = method.getAnnotation(MyRequestMapping.class);
                    String methodUri = myRequestMapping.value();
                    MethodHandler methodHandler = new MethodHandler(object, method, Pattern.compile(requestUri + methodUri));
                    // 处理请求参数
                    Parameter[] parameters = method.getParameters();
                    for (int j = 0; j < parameters.length; j++) {
                        Parameter parameter = parameters[j];
                        if(parameter.getType() == HttpServletRequest.class || parameter.getType() == HttpServletResponse.class){
                            methodHandler.getParamIndexMapping().put(parameter.getType().getSimpleName(), j);
                        } else {
                            methodHandler.getParamIndexMapping().put(parameter.getName(), j);
                        }
                    }

                    // 将类请求上配置的用户添加到该方法用户权限集合中
                    methodHandler.getAuthUsers().addAll(classAuthUsers);
                    // 将方法上配置的权限集合添加
                    methodHandler.getAuthUsers().addAll(getAuthInfos(method.getAnnotation(Security.class)));
                    methodsHandlers.add(methodHandler);
                }
                // 获取方法上的权限集合
            }
        }
    }

    public List<String> getAuthInfos(Security securityAnno){
        List<String> authUsers = new ArrayList<>();
        if(null == securityAnno){
            return authUsers;
        }
        String[] value = securityAnno.value();
        if("".equals(value)){
            return authUsers;
        }
        for (String authUser : value) {
            authUsers.add(authUser);
        }
        return authUsers;
    }

    private void doSetProperties() throws IllegalAccessException {
        // 是否已经扫描的标记注解的类
        if(beans.size() == 0) return;

        // 遍历bean对象
        for (Map.Entry<String, Object> entity : beans.entrySet()) {
            // 获取到当前对象声明的字段信息
            Field[] declaredFields = entity.getValue().getClass().getDeclaredFields();
            for (Field declaredField : declaredFields) {
                // 判断当前字段是否有 MyAutowired注解
                if(declaredField.isAnnotationPresent(MyAutowired.class)){
                    declaredField.setAccessible(true);
                    // 根据当前字段的名称，从bean中获取对象，完成对象的依赖注入
                    // 获取到当前对象的名称，private IdemoService demoService;
                    String fieldName = declaredField.getName();
                    String fieldClass = declaredField.getType().getName();
                    if(null != beans.get(fieldName)){
                        declaredField.set(entity.getValue(), beans.get(fieldName));
                    } else if(null != beans.get(fieldClass)){
                        declaredField.set(entity.getValue(), beans.get(fieldClass));
                    }
                }
            }
        }
    }

    private void doInstance() throws Exception {

        // 根据扫描到的bean 全限定名称，完成对象的实例化
        if(scanBeanClassNames.size() == 0){
            return;
        }
        // 遍历扫描的对象名称，完成初始化动作
        for (String className : scanBeanClassNames) {
            // 根据全限定名称，实例化当前对象 com.rachel.demo.controller.DemoController

            // 获取到当前对象的类型
            Class<?> objectClass = Class.forName(className);
            // 判断当前对象是否标注了MyController or MyService注解
            MyController myController = objectClass.getAnnotation(MyController.class);
            MyService myService = objectClass.getAnnotation(MyService.class);
            if(null != myController || null != myService){
                // 判断该对象是否继承了接口，如果继承接口，需要根据接口的名称，往beans中存放一份
                Class<?>[] interfaces = objectClass.getInterfaces();
                // service层一般都是有接口的，面向接口开发，此时再以接口名为id，放入一份对象到bean集合中，便于后期根据接口类型注入
                for (Class<?> anInterface : interfaces) {
                    beans.put(anInterface.getName(),objectClass.newInstance());
                }
                //根据是否配置id,如果配置id则根据id将对象保存，否则根据类的名称小写首字母
                String beanId = null != myController ? myController.value() : myService.value();
                if(null == beanId || "".equals(beanId.trim())){
                    String simpleName = toFirstLowerCase(objectClass.getSimpleName());
                    beans.put(simpleName, objectClass.newInstance());
                } else {
                    beans.put(beanId, objectClass.newInstance());
                }
            }
        }
    }

    private String toFirstLowerCase(String simpleName) {
        char[] chars = simpleName.toCharArray();
        if(chars[0] >= 'A' || chars[0] <= 'Z'){
            chars[0] +=32;
        }
        return String.valueOf(chars);
    }

    public void doReadProperties(String location) throws IOException {
        // 将配置文件读取成流信息
        InputStream stream = this.getClass().getClassLoader().getResourceAsStream(location);

        // 解析配置文件
        scanPackageProperties.load(stream);
    }

    public void doScan(String scanPackage){
        if(null == scanPackage){
            return;
        }
        // 获取到当前项目的绝对路径
        String realPath = (Thread.currentThread().getContextClassLoader().getResource("").getPath() + scanPackage.replace(".","/")).substring(1);
        // 迭代获取当前file下是否还有file
        File scanFile = new File(realPath);
        for(File file : scanFile.listFiles()) {
            if (file.isDirectory()) {
                doScan(scanPackage + "." + file.getName());
            } else if(file.getName().endsWith(".class")){
                String beanName = scanPackage + "." + file.getName().replaceAll(".class","");
                scanBeanClassNames.add(beanName);
            }
        }
    }
}
