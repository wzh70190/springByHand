package com.example.mvcframework.servlet;

import com.example.mvcframework.annotation.GPAutowired;
import com.example.mvcframework.annotation.GPController;
import com.example.mvcframework.annotation.GPRequestMapping;
import com.example.mvcframework.annotation.GPService;

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
import java.net.URL;
import java.util.*;

/**
 * 此类作为启动入口
 */
public class GPDispacherServelet extends HttpServlet {
    /**
     * 初始化
     */
    public GPDispacherServelet() {
        super();
    }

    //与web.xml里面param-name保持一致
    private static final String LOCATION = "contextConfigLocation";

    //保存所有的配置信息
    private Properties properties = new Properties();

    //保存所有被扫描到的相关类名
    private List<String> classNames = new ArrayList<String>();

    //核心IOC容器，保存所有的初始化Bean
    private Map<String, Object> ioc = new HashMap<String, Object>();

    //保存所有的url和方法的映射关系
    private Map<String, Method> handleMapping = new HashMap<String, Method>();


    /**
     * 初始化加载配置文件
     *
     * @param config
     * @throws ServletException
     */
    @Override
    public void init(ServletConfig config) throws ServletException {
        //1、加载配置文件
        doLoadConfig(config.getInitParameter(LOCATION));


        //2.扫描所有相关类
        doScanner(properties.getProperty("scanPackage"));


        //3.初始化所有相关类的实例，并保存到IOC容器中
        doInstance();

        //4.依赖注入
        doAutoWired();


        //5.构造handlerMapping
        initHandlerMapping();


        System.out.println("gupaodu mvcframework is init");


    }

    //    initHandlerMapping()方法，将GPRequestMapping中配置的信息和Method进行关联，并保存这些关系。
    private void initHandlerMapping() {
        if (ioc.isEmpty()) {
            return;
        }
        for (Map.Entry entry : ioc.entrySet()) {
            Class<?> clazz = entry.getValue().getClass();
            if (!clazz.isAnnotationPresent(GPController.class)) {
                continue;
            } else {
                String baseUtrl = "";
                if (clazz.isAnnotationPresent(GPRequestMapping.class)) {
                    GPRequestMapping requestMapping = clazz.getAnnotation(GPRequestMapping.class);
                    baseUtrl = requestMapping.value();
                }
                Method[] methods = clazz.getMethods();
                for (Method method : methods) {
                    //没有RequestMapping注解的直接忽略
                    if (!method.isAnnotationPresent(GPRequestMapping.class)) {
                        continue;
                    } else {
                        //映射url
                        GPRequestMapping requestMapping = method.getAnnotation(GPRequestMapping.class);
                        String url = ("/" + baseUtrl + "/" + requestMapping.value()).replaceAll("/+", "/");
                        handleMapping.put(url, method);
                        System.out.println("mapped" + url + "," + method);
                    }
                }

            }
        }


    }

    private void doAutoWired() {
        if (ioc.isEmpty()) {
            return;
        }

        for (Map.Entry entry : ioc.entrySet()) {
            Field[] fields = entry.getValue().getClass().getDeclaredFields();
            for (Field field : fields) {
                if (!field.isAnnotationPresent(GPAutowired.class)) {
                    continue;
                }
                GPAutowired autowired = field.getAnnotation(GPAutowired.class);
                String beanName = autowired.value().trim();
                if ("".equals(beanName)) {
                    beanName = field.getType().getName();
                }
                field.setAccessible(true);
                try {
                    field.set(entry.getKey(), ioc.get(beanName));
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }


            }

        }

    }

    private void doInstance() {
        if (classNames.size() == 0) {
            return;
        }
        try {
            for (String className : classNames) {
                Class<?> clazz = Class.forName(className);
                if (clazz.isAnnotationPresent(GPController.class)) {
                    //默认首字母小写
                    String beanName = lowerFirstCase(clazz.getSimpleName());
                    ioc.put(beanName, clazz.newInstance());
                } else if (clazz.isAnnotationPresent(GPService.class)) {
                    GPService service = clazz.getAnnotation(GPService.class);
                    String beanName = service.value();
                    //如果用户设置了名字，用户自己设置
                    if (!"".equals(beanName.trim())) {
                        ioc.put(beanName, clazz.newInstance());
                        continue;
                    } else {

                        Class<?>[] interfaces = clazz.getInterfaces();
                        for (Class in :
                                interfaces) {
                            ioc.put(in.getName(), in.newInstance());
                        }

                    }

                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //doScanner()方法，递归扫描出所有的Class文件
    private void doScanner(String packageName) {
        URL url = this.getClass().getClassLoader().getResource("/" + packageName.replaceAll("\\.", "/"));
        File dir = new File(url.getFile());
        for (File file : dir.listFiles()) {
//如果是文件夹，继续递归
            if (file.isDirectory()) {
                doScanner(packageName + "." + file.getName());
            } else {
                classNames.add(packageName + "." + file.getName().replace(".class", "").trim());
            }
        }
    }

    //doLoadConfig()方法的实现，将文件读取到Properties对象中：
    private void doLoadConfig(String location) {
        InputStream fis = null;
        try {
            fis = getClass().getClassLoader().getResourceAsStream(location);
            //1.读取配置文件
            properties.load(fis);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }


    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        super.doGet(req, resp);
    }


    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws  IOException {

        try {
            doDispatch(req, resp);

        } catch (Exception e) {
            resp.getWriter().write("500 Excption,Details:\r\n" + Arrays.toString(e.getStackTrace()));
        }
    }


    private String lowerFirstCase(String str) {
        char[] chars = str.toCharArray();
        chars[0] += 32;
        return String.copyValueOf(chars);
    }

    private void doDispatch(HttpServletRequest req, HttpServletResponse resp) throws IOException, InvocationTargetException, IllegalAccessException {
        if (handleMapping.isEmpty()) {
            return;
        }

        String url = req.getRequestURI();
        String contextPath = req.getContextPath();
        url = url.replaceAll(contextPath, "").replaceAll("/+", "/");
        if (!handleMapping.containsKey(url)) {
            resp.getWriter().write("404 not fond");
            return;
        }
        Map<String, String[]> params = req.getParameterMap();
        Method method = handleMapping.get(url);
        String beanName = lowerFirstCase(method.getDeclaringClass().getSimpleName());
        method.invoke(ioc.get(beanName), req, resp, params.get("name")[0]);
    }

}
