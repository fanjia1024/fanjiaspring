package com.fanjia.spring.mvcframework.v3.servlet;

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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    //保存所有的Url和方法的映射关系
    private List<Handler> handlerMapping = new ArrayList<Handler>();

    public FJDispatcherServlet(){ super(); }
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
        try{
            Handler handler = getHandler(req);

            if(handler == null){
                //如果没有匹配上，返回404错误
                resp.getWriter().write("404 Not Found");
                return;
            }


            //获取方法的参数列表
            Class<?> [] paramTypes = handler.method.getParameterTypes();

            //保存所有需要自动赋值的参数值
            Object [] paramValues = new Object[paramTypes.length];


            Map<String,String[]> params = req.getParameterMap();
            for (Map.Entry<String, String[]> param : params.entrySet()) {
                String value = Arrays.toString(param.getValue()).replaceAll("\\[|\\]", "").replaceAll(",\\s", ",");

                //如果找到匹配的对象，则开始填充参数值
                if(!handler.paramIndexMapping.containsKey(param.getKey())){continue;}
                int index = handler.paramIndexMapping.get(param.getKey());
                paramValues[index] = convert(paramTypes[index],value);
            }


            //设置方法中的request和response对象
            int reqIndex = handler.paramIndexMapping.get(HttpServletRequest.class.getName());
            paramValues[reqIndex] = req;
            int respIndex = handler.paramIndexMapping.get(HttpServletResponse.class.getName());
            paramValues[respIndex] = resp;

            handler.method.invoke(handler.controller, paramValues);

        }catch(Exception e){
            throw e;
        }
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
                String regex=("/"+baseUrl+requestMapping.value()).replaceAll("/+","/");
                Pattern pattern=Pattern.compile(regex);
                handlerMapping.add(new Handler(pattern,entry.getValue(),method));
                System.out.println("Mapper :"+regex+","+method);
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
    private Handler getHandler(HttpServletRequest req) throws Exception{
        if(handlerMapping.isEmpty()){ return null; }

        String url = req.getRequestURI();
        String contextPath = req.getContextPath();
        url = url.replace(contextPath, "").replaceAll("/+", "/");

        for (Handler handler : handlerMapping) {
            try{
                Matcher matcher = handler.pattern.matcher(url);
                //如果没有匹配上继续下一个匹配
                if(!matcher.matches()){ continue; }

                return handler;
            }catch(Exception e){
                throw e;
            }
        }
        return null;
    }

    //url传过来的参数都是String类型的，HTTP是基于字符串协议
    //只需要把String转换为任意类型就好
    private Object convert(Class<?> type,String value){
        if(Integer.class == type){
            return Integer.valueOf(value);
        }
        //如果还有double或者其他类型，继续加if
        //这时候，我们应该想到策略模式了
        //在这里暂时不实现，希望小伙伴自己来实现
        return value;
    }
    /**
     * Handler记录Controller中的RequestMapping和Method的对应关系
     * @author Tom
     * 内部类
     */
    private class Handler{

        protected Object controller;	//保存方法对应的实例
        protected Method method;		//保存映射的方法
        protected Pattern pattern;
        protected Map<String,Integer> paramIndexMapping;	//参数顺序

        /**
         * 构造一个Handler基本的参数
         * @param controller
         * @param method
         */
        protected Handler(Pattern pattern,Object controller,Method method){
            this.controller = controller;
            this.method = method;
            this.pattern = pattern;

            paramIndexMapping = new HashMap<String,Integer>();
            putParamIndexMapping(method);
        }

        private void putParamIndexMapping(Method method){

            //提取方法中加了注解的参数
            Annotation [] [] pa = method.getParameterAnnotations();
            for (int i = 0; i < pa.length ; i ++) {
                for(Annotation a : pa[i]){
                    if(a instanceof FJRequestParam){
                        String paramName = ((FJRequestParam) a).value();
                        if(!"".equals(paramName.trim())){
                            paramIndexMapping.put(paramName, i);
                        }
                    }
                }
            }

            //提取方法中的request和response参数
            Class<?> [] paramsTypes = method.getParameterTypes();
            for (int i = 0; i < paramsTypes.length ; i ++) {
                Class<?> type = paramsTypes[i];
                if(type == HttpServletRequest.class ||
                        type == HttpServletResponse.class){
                    paramIndexMapping.put(type.getName(),i);
                }
            }
        }
    }
}
