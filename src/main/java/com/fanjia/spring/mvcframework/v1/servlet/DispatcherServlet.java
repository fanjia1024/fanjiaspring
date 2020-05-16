package com.fanjia.spring.mvcframework.v1.servlet;

import com.fanjia.spring.mvcframework.annotation.FJAutowired;
import com.fanjia.spring.mvcframework.annotation.FJController;
import com.fanjia.spring.mvcframework.annotation.FJRequestMapping;
import com.fanjia.spring.mvcframework.annotation.FJService;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletResponse;
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

/**
 * description: DispatcherServlet <br>
 * date: 2020/4/18 9:37 <br>
 * author: Administrator <br>
 * version: 1.0 <br>
 */
public class DispatcherServlet extends HttpServlet {

    private Map<String,Object> mapping=new HashMap<String, Object>();

    /**
     * Called by the server (via the <code>service</code> method) to
     * allow a servlet to handle a GET request.
     *
     * <p>Overriding this method to support a GET request also
     * automatically supports an HTTP HEAD request. A HEAD
     * request is a GET request that returns no body in the
     * response, only the request header fields.
     *
     * <p>When overriding this method, read the request data,
     * write the response headers, get the response's writer or
     * output stream object, and finally, write the response data.
     * It's best to include content type and encoding. When using
     * a <code>PrintWriter</code> object to return the response,
     * set the content type before accessing the
     * <code>PrintWriter</code> object.
     *
     * <p>The servlet container must write the headers before
     * committing the response, because in HTTP the headers must be sent
     * before the response body.
     *
     * <p>Where possible, set the Content-Length header (with the
     * {@link ServletResponse#setContentLength} method),
     * to allow the servlet container to use a persistent connection
     * to return its response to the client, improving performance.
     * The content length is automatically set if the entire response fits
     * inside the response buffer.
     *
     * <p>When using HTTP 1.1 chunked encoding (which means that the response
     * has a Transfer-Encoding header), do not set the Content-Length header.
     *
     * <p>The GET method should be safe, that is, without
     * any side effects for which users are held responsible.
     * For example, most form queries have no side effects.
     * If a client request is intended to change stored data,
     * the request should use some other HTTP method.
     *
     * <p>The GET method should also be idempotent, meaning
     * that it can be safely repeated. Sometimes making a
     * method safe also makes it idempotent. For example,
     * repeating queries is both safe and idempotent, but
     * buying a product online or modifying data is neither
     * safe nor idempotent.
     *
     * <p>If the request is incorrectly formatted, <code>doGet</code>
     * returns an HTTP "Bad Request" message.
     *
     * @param req  an {@link HttpServletRequest} object that
     *             contains the request the client has made of the servlet
     * @param resp an {@link HttpServletResponse} object that
     *             contains the response the servlet sends to the client
     * @throws IOException      if an input or output error is
     *                          detected when the servlet handles the GET request
     * @throws ServletException if the request for the GET
     *                          could not be handled
     * @see ServletResponse#setContentType
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doPost(req, resp);
    }

    /**
     * Called by the server (via the <code>service</code> method)
     * to allow a servlet to handle a POST request.
     * <p>
     * The HTTP POST method allows the client to send
     * data of unlimited length to the Web server a single time
     * and is useful when posting information such as
     * credit card numbers.
     *
     * <p>When overriding this method, read the request data,
     * write the response headers, get the response's writer or output
     * stream object, and finally, write the response data. It's best
     * to include content type and encoding. When using a
     * <code>PrintWriter</code> object to return the response, set the
     * content type before accessing the <code>PrintWriter</code> object.
     *
     * <p>The servlet container must write the headers before committing the
     * response, because in HTTP the headers must be sent before the
     * response body.
     *
     * <p>Where possible, set the Content-Length header (with the
     * {@link ServletResponse#setContentLength} method),
     * to allow the servlet container to use a persistent connection
     * to return its response to the client, improving performance.
     * The content length is automatically set if the entire response fits
     * inside the response buffer.
     *
     * <p>When using HTTP 1.1 chunked encoding (which means that the response
     * has a Transfer-Encoding header), do not set the Content-Length header.
     *
     * <p>This method does not need to be either safe or idempotent.
     * Operations requested through POST can have side effects for
     * which the user can be held accountable, for example,
     * updating stored data or buying items online.
     *
     * <p>If the HTTP POST request is incorrectly formatted,
     * <code>doPost</code> returns an HTTP "Bad Request" message.
     *
     * @param req  an {@link HttpServletRequest} object that
     *             contains the request the client has made of the servlet
     * @param resp an {@link HttpServletResponse} object that
     *             contains the response the servlet sends to the client
     * @throws IOException      if an input or output error is
     *                          detected when the servlet handles the request
     * @throws ServletException if the request for the POST
     *                          could not be handled
     * @see ServletOutputStream
     * @see ServletResponse#setContentType
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            doDispatch(req, resp);
        }catch (Exception e){
            resp.getWriter().write("500 Exception"+ Arrays.toString(e.getStackTrace()));
        }
    }

    private void doDispatch(HttpServletRequest req, HttpServletResponse resp) throws Exception{
        String url=req.getRequestURI();
        String contextPath=req.getContextPath();
        url.replace(contextPath," ").replaceAll("/+","/");
        if (!this.mapping.containsKey(url)){
            resp.getWriter().write("404 not found!!");
            return;
        }
        Method method = (Method) this.mapping.get(url);
        Map<String,String[]> params = req.getParameterMap();
        method.invoke(this.mapping.get(method.getDeclaringClass().getName())
                ,new Object[]{req,resp,params.get("name")[0]});
    }

    /**
     * Called by the servlet container to indicate to a servlet that the
     * servlet is being placed into service.  See {@link javax.servlet.Servlet#init}.
     *
     * <p>This implementation stores the {@link ServletConfig}
     * object it receives from the servlet container for later use.
     * When overriding this form of the method, call
     * <code>super.init(config)</code>.
     *
     * @param config the <code>ServletConfig</code> object
     *               that contains configutation information for this servlet
     * @throws ServletException if an exception occurs that
     *                          interrupts the servlet's normal operation
     * @see ServletException
     */
    @Override
    public void init(ServletConfig config) throws ServletException {
        InputStream is=null;
        try{
            Properties configContext=new Properties();
            is=this.getClass().getClassLoader().getResourceAsStream(config.getInitParameter("contextConfigLocation"));
            configContext.load(is);
            String scanPackage=configContext.getProperty("scanPackage");
            doScanner(scanPackage);
            for (String clazzName : mapping.keySet()) {
                if (!clazzName.contains(".")){continue;}
                Class<?> clazz=Class.forName(clazzName);
                if (clazz.isAnnotationPresent(FJController.class)){
                    mapping.put(clazzName,clazz.newInstance());
                    String baseUrl="";
                    if (clazz.isAnnotationPresent(FJRequestMapping.class)){
                        FJRequestMapping requestMapping=clazz.getAnnotation(FJRequestMapping.class);
                        baseUrl=requestMapping.value();
                    }
                    Method[] methods=clazz.getMethods();
                    for (Method method:methods){
                        if (!method.isAnnotationPresent(FJRequestMapping.class)){continue;}
                        FJRequestMapping requestMapping=method.getAnnotation(FJRequestMapping.class);
                        String url=(baseUrl+"/"+requestMapping.value().replaceAll("/+","/"));
                        mapping.put(url,method);
                        System.out.println("Mapper" + url+","+method);
                    }
                }else if (clazz.isAnnotationPresent(FJService.class)){
                    FJService service=clazz.getAnnotation(FJService.class);
                    String beanName=service.value();
                    if ("".equals(beanName)){
                        beanName=clazz.getName();
                    }
                    Object instance=clazz.newInstance();
                    mapping.put(beanName,instance);
                    for (Class<?> i:clazz.getInterfaces()){
                        mapping.put(i.getName(),instance);
                    }
                }else {
                    continue;
                }
            }
            for (Object object:mapping.values()){
                if (object==null){continue;}
                Class clazz=object.getClass();
                if (clazz.isAnnotationPresent(FJController.class)){
                    Field[] fields=clazz.getFields();
                    for (Field field:fields){
                        if (!field.isAnnotationPresent(FJAutowired.class)){
                            continue;
                        }
                        FJAutowired fjAutowired=field.getAnnotation(FJAutowired.class);
                        String beanName=fjAutowired.value();
                        if ("".equals(beanName)){beanName=field.getType().getName();}
                        field.setAccessible(true);
                        try {
                            field.set(mapping.get(clazz.getName()),mapping.get(beanName));
                        }catch (IllegalAccessException e){
                            e.printStackTrace();
                        }
                    }
                }
            }
        }catch (Exception ex){

        }finally {
            if (is != null) {
                try {
                    is.close();
                }catch (IOException e){
                    e.printStackTrace();
                }
            }
        }
        System.out.println("FJ MVC Framework is init");
    }

    /**
     * <h2>扫描目录下所有的class</h2>
     * @param scanPackage
     */
    private void doScanner(String scanPackage) {
        URL url=this.getClass().getClassLoader().getResource("/"+scanPackage.replaceAll("\\.","/"));
        File classDir=new File(url.getFile());
        for (File file: classDir.listFiles()){
            if (file.isDirectory()){
                doScanner(scanPackage+"."+file.getName());
            }else{
                if (!file.getName().endsWith(".class")){
                    continue;
                }
                String clazzName=(scanPackage+"."+file.getName().replace(".class",""));
                mapping.put(clazzName,null);
            }
        }
    }
}
