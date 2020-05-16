package com.fanjia.spring.demo.mvc.action;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fanjia.spring.demo.service.IDemoSerivce;
import com.fanjia.spring.mvcframework.annotation.FJAutowired;
import com.fanjia.spring.mvcframework.annotation.FJController;
import com.fanjia.spring.mvcframework.annotation.FJRequestMapping;
import com.fanjia.spring.mvcframework.annotation.FJRequestParam;


@FJController
@FJRequestMapping("/demo")
public class DemoAction {

  	@FJAutowired private IDemoSerivce demoService;

	@FJRequestMapping("/query")
	public void query(HttpServletRequest req, HttpServletResponse resp,
					  @FJRequestParam("name") String name){
		String result = demoService.get(name);
//		String result = "My name is " + name;
		try {
			resp.getWriter().write(result);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@FJRequestMapping("/add")
	public void add(HttpServletRequest req, HttpServletResponse resp,
					@FJRequestParam("a") Integer a, @FJRequestParam("b") Integer b){
		try {
			resp.getWriter().write(a + "+" + b + "=" + (a + b));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@FJRequestMapping("/remove")
	public void remove(HttpServletRequest req,HttpServletResponse resp,
					   @FJRequestParam("id") Integer id){
	}

}
