package com.fanjia.spring.mvcframework.v2.servlet;

import com.fanjia.spring.mvcframework.annotation.*;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;

/**
 * description: FJDispatcherServlet <br>
 * date: 2020/4/18 17:02 <br>
 * author: Administrator <br>
 * version: 1.0 <br>
 */
public class FJDispatcherServlet extends HttpServlet{
    //存储aplication.properties的配置内容
    private Properties contextConfig = new Properties();
    //存储所有扫描到的类
    private List<String> classNames = new ArrayList<String>();
    //IOC容器，保存所有实例化对象
    //注册式单例模式
    private Map<String,Object> ioc = new HashMap<String,Object>();
    //保存Contrller中所有Mapping的对应关系

    private Map<String, Method> handlerMapping = new HashMap<String,Method>();
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doPost(req,resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            doDispatch(req,resp);
        }catch (Exception e){
            e.printStackTrace();
            resp.getWriter().write("500 Exception,Detail : "+Arrays.toString(e.getStackTrace()));
        }
    }

    private void doDispatch(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        String url=req.getRequestURI();
        String contextPath=req.getContextPath();
        url.replace(contextPath," ").replaceAll("/+","/");
        if (!this.handlerMapping.containsKey(url)){
            resp.getWriter().write("404 not found!!");
            return;
        }
        Method method=this.handlerMapping.get(url);
        Map<String,String[]> params=req.getParameterMap();
        Class<?>[] parameterTypes=method.getParameterTypes();
        Map<String,String[]> parameterMap=req.getParameterMap();
        Object[] paramerValues=new Object[parameterTypes.length];
        for (int i = 0; i < parameterTypes.length; i++) {
            Class parameterType=parameterTypes[i];
            if (parameterType == HttpServletRequest.class){
                paramerValues[i]=req;
                continue;
            }else if (parameterType == HttpServletResponse.class){
                paramerValues[i]=resp;
                continue;
            }else if (parameterType == String.class){
                //提取方法中的加了注解的参数
                Annotation[] [] pa=method.getParameterAnnotations();
                for (int j = 0; j <pa.length ; j++) {
                    for (Annotation a :pa[j]){
                        if (a instanceof FJRequestParam){
                            String paramName=((FJRequestParam) a).value();
                            if ("".equals(paramName.trim())){
                                String value=Arrays.toString(parameterMap.get(paramName))
                                        .replaceAll("\\[|\\]","")
                                        .replaceAll("\\s",",");
                                paramerValues[i]=value;





                            }
                        }
                    }
                }
            }
        }
        String beanName=toLowerFirstCase(method.getDeclaringClass().getSimpleName());
        method.invoke(ioc.get(beanName),new Object[]{req,resp,params.get("name")[0]});
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        //1.加载配置文件
        doLoadConfig(config.getInitParameter("contextConfigLocation"));
        //2.扫描相关的类
        doScanner(contextConfig.getProperty("scanPackage"));
        //3.初始化扫描到的类，加载到ioc容器中去
        doInstance();
        //4.完成依赖注入
        doAotowired();
        //5.初始化handlermapping
        initHandlerMapping();
        System.out.println("FJ Spring Framework is init !");
    }

    /**
     * <h2>初始化handlerMapping</h2>
     */
    private void initHandlerMapping() {
        if (ioc.isEmpty()){return;}
        for (Map.Entry<String,Object> entry:ioc.entrySet()){
            Class<?> clazz=entry.getValue().getClass();
            if (!clazz.isAnnotationPresent(FJController.class)){continue;}
            //保存写在类上的注解@FJRequestMapping
            String baseUrl="";
            if (clazz.isAnnotationPresent(FJRequestMapping.class)){
                FJRequestMapping requestMapping=clazz.getAnnotation(FJRequestMapping.class);
                baseUrl=requestMapping.value();
            }
            for (Method method:clazz.getMethods()){
                if (!method.isAnnotationPresent(FJRequestMapping.class)){continue;}
                FJRequestMapping requestMapping=method.getAnnotation(FJRequestMapping.class);
                String url=("/"+baseUrl+"/"+requestMapping.value());
                handlerMapping.put(url,method);
                System.out.println("Mapper :"+url+","+method);
            }
        }
    }

    /**
     * <h2>自动进行依赖注入</h2>
     */
    private void doAotowired() {
        if (ioc.isEmpty()){return;}

        for (Map.Entry<String,Object> entry:ioc.entrySet()
             ) {
            //获取所以的字段 包括private protected default
            //正常来说普通的oop编程只能获取到public类型的字段
            Field[] fields=entry.getValue().getClass().getDeclaredFields();
            for (Field field:fields
                 ) {
                if (!field.isAnnotationPresent(FJAutowired.class)){continue;}
                FJAutowired autowired=field.getAnnotation(FJAutowired.class);
                String beanName=autowired.value().trim();
                if ("".equals(beanName)){
                    //获取接口的类型作为key然后用这个key从ioc中取值
                    beanName=field.getType().getName();
                }
                //如果是public以外的类型 只要加了@Autowired注解就要强制赋值
                //反射中叫暴力访问
                field.setAccessible(true);
                try {
                    field.set(entry.getValue(),ioc.get(beanName));
                }catch (IllegalAccessException e){
                    e.printStackTrace();
                }

                
            }

        }
    }

    /**
     * <h2>初始化扫描到的类，加载到ioc容器中去</h2>
     */
    private void doInstance() {
        //初始化过程 为di做准备
        if (classNames.isEmpty()){return;}
        try{
            for (String className:classNames
                 ) {
                //反射调用获取class对象
                Class<?> clazz=Class.forName(className);
                if (clazz.isAnnotationPresent(FJController.class)){
                    Object instance=clazz.getAnnotation(FJController.class);
                    String beanName=toLowerFirstCase(clazz.getSimpleName());
                    ioc.put(beanName,instance);
                }else  if (clazz.isAnnotationPresent(FJService.class)){
                    FJService service=clazz.getAnnotation(FJService.class);
                    String beanName=service.value();
                    if ("".equals(beanName.trim())){
                        beanName=toLowerFirstCase(clazz.getSimpleName());
                    }
                    Object instance=clazz.newInstance();
                    ioc.put(beanName,instance);
                    for (Class<?> i:clazz.getInterfaces()){
                        if (ioc.containsKey(i.getName())){
                            throw new Exception("the "+i.getName()+"is exist!!");
                        }
                    }
                }else {continue;}
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    /**
     * <h2>首字母小写的beanName获取算法实现</h2>
     * @param simpleName
     * @return
     */
    private String toLowerFirstCase(String simpleName) {
        char[] chars=simpleName.toCharArray();
        chars[0]+=32;
        return String.valueOf(chars);
    }

    /**
     * <h2>扫描路径下所有的类</h2>
     * @param scanPackage
     */
    private void doScanner(String scanPackage) {
        //转换文件路径将文件中的.转换为/
        URL url=this.getClass().getClassLoader().getResource("/"+scanPackage.replaceAll("\\.","/"));
        File classPath=new File(url.getFile());
        for (File file:classPath.listFiles()
             ) {
            if (file.isDirectory()) {
                doScanner(scanPackage + "." + file.getName());
            }else{
                if (!file.getName().endsWith(".class")){continue;}
                String className=(scanPackage+"."+file.getName().replace(".class",""));
                classNames.add(className);
            }
        }
    }

    /**
     * <h2>加载配置文件</h2>
     * @param contextConfigLocation
     */
    private void doLoadConfig(String contextConfigLocation) {
        InputStream is=this.getClass().getClassLoader().getResourceAsStream(contextConfigLocation);
        try {
            contextConfig.load(is);
        }catch (IOException e){
            e.printStackTrace();
        }finally {
            if (null != is){
                try{
                    is.close();
                }catch (IOException e){
                    e.printStackTrace();
                }
            }
        }
    }
}
