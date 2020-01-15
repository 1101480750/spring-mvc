package com.spring.demo.springframework.web.servlet;

import com.spring.demo.springframework.annotation.GPAutowired;
import com.spring.demo.springframework.annotation.GPController;
import com.spring.demo.springframework.annotation.GPRequestMapping;
import com.spring.demo.springframework.annotation.GPService;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;

public class GPDispatcherServlet extends HttpServlet {


    private Properties configContext = new Properties();

    private List<String> classNames = new ArrayList<>();

    private Map<String, Object> ioc = new HashMap<String, Object>();


    private Map<String, Method> handleMapping = new HashMap<>();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doPut(req, resp);
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // 根据用户的url动态找到method
        try {
            doDispatch(req, resp);

        } catch (Exception e) {
            e.printStackTrace();
            resp.getWriter().write("500 Exception " + Arrays.toString(e.getStackTrace()));
        }
    }

    private void doDispatch(HttpServletRequest req, HttpServletResponse resp) throws Exception {

        if(this.handleMapping.isEmpty()){
            return;
        }
        String url = req.getRequestURL().toString();
        url = req.getPathInfo();

        // 获得上下文绝对路径
//        String contextPath = req.getContextPath();
//
//        url = url.replace(contextPath, "").replace("/+", "/");

        if (!this.handleMapping.containsKey(url)){
            resp.getWriter().write("404 Not Found");
            return;
        }
        Method method = this.handleMapping.get(url);
        // TODO 此处代码需要研究
        String beanName = toFirstLowerCase(method.getDeclaringClass().getSimpleName());
        Map<String, String[]>  params =  req.getParameterMap();
        method.invoke(ioc.get(beanName), new Object[]{req, resp,"zhouyaoming"});
    }

    /**
     * 初始化
     *
     * @param config
     * @throws ServletException
     */
    @Override
    public void init(ServletConfig config) throws ServletException {

        // 1.加载配置文件,解析文件

        doLoadConfig(config.getInitParameter("contextConfigLocation"));

        // 2.扫描所有相关的类

        doScanner(configContext.getProperty("scanPackage"));

        // 3.初始化bean,放到ioc容器中
        doInstance();

        //4.自动完成依赖注入
        doAutowired();

        // 5.初始化hanlerMapping容器
        initHandlerMapping();

        System.out.println("初始化完成");
    }

    private void initHandlerMapping() {
        if (ioc.isEmpty()) {
            return;
        }

        for (Map.Entry<String, Object> entry : ioc.entrySet()) {
            Class<?> clazz = entry.getValue().getClass();
            if (!clazz.isAnnotationPresent(GPController.class)) {
                continue;
            }
            String baseUrl = "";
            if (clazz.isAnnotationPresent(GPRequestMapping.class)) {
                GPRequestMapping requestMapping = clazz.getAnnotation(GPRequestMapping.class);
                baseUrl = requestMapping.value();
            }

            Method[] methods = clazz.getMethods();
            for (Method method : methods) {
                if (!method.isAnnotationPresent(GPRequestMapping.class)) {
                    continue;
                }
                GPRequestMapping requestMapping = method.getAnnotation(GPRequestMapping.class);
                // /+ 代表多个/
                String url = (baseUrl + "/" + requestMapping.value()).replaceAll("/+", "/");
                handleMapping.put(url, method);

                System.out.println("mapped" + url + "method" + method);

            }

        }
    }

    private void doAutowired() {

        if (ioc.isEmpty()) {
            return;
        }
        for (Map.Entry<String, Object> entry : ioc.entrySet()) {
            // 所有的字段。不管是public protected private default
            Field[] fields = entry.getValue().getClass().getDeclaredFields();

            for (Field field : fields) {
                if (!field.isAnnotationPresent(GPAutowired.class)) {
                    continue;
                }
                GPAutowired autowired = field.getAnnotation(GPAutowired.class);
                String beanName = autowired.value();
                if ("".equals(beanName)) {
                    beanName = field.getType().getName();
                }

                // 强制赋值, 暴力访问，
                field.setAccessible(true);

                try {
                    // 用反射
                    field.set(entry.getValue(), ioc.get(beanName));
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void doInstance() {
        if (classNames.isEmpty()) {
            return;
        }
        try {
            for (String className : classNames) {
                Class<?> clazz = Class.forName(className);
                if (clazz.isAnnotationPresent(GPController.class)) {
                    Object object = clazz.newInstance();
                    // getSimpleName得到类的简写名称
                    String beanName = toFirstLowerCase(clazz.getSimpleName());
                    // key默认为首字母消息
                    ioc.put(beanName, object);
                } else if (clazz.isAnnotationPresent(GPService.class)) {
                    // 1.默认首字母小写

                    // 2.自定义的bean类
                    GPService service = clazz.getAnnotation(GPService.class);
                    String beanName = service.value();

                    if ("".equals(beanName)) {
                        beanName = toFirstLowerCase(clazz.getSimpleName());
                    }
                    Object instance = clazz.newInstance();
                    ioc.put(beanName, instance);
                    // 3.注入接口的实现类
                    for (Class<?> i : clazz.getInterfaces()) {
                        if (ioc.containsKey(i.getName())) {
                            throw new Exception("The beanName " + i.getName() + " is exist");
                        }
                        ioc.put(i.getName(), instance);
                    }
                } else {
                    continue;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String toFirstLowerCase(String simpleName) {
        char[] chars = simpleName.toCharArray();
        chars[0] += 32;
        return String.valueOf(chars);
    }

    private void doScanner(String scanPackage) {
        URL url = this.getClass().getClassLoader().getResource("/" + scanPackage.replaceAll("\\.", "/"));
        File calssDir = new File(url.getFile());
        for (File file : calssDir.listFiles()) {

            if (file.isDirectory()) {
                doScanner(scanPackage + "." + file.getName());
            } else {
                if (!file.getName().endsWith(".class")) {
                    continue;
                }
                String clazzName = (scanPackage + "." + file.getName().replaceAll(".class", ""));
                classNames.add(clazzName);
            }
        }

    }

    private void doLoadConfig(String contextConfigLocation) {

        InputStream is = this.getClass().getClassLoader().getResourceAsStream(contextConfigLocation);
        try {
            configContext.load(is);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
