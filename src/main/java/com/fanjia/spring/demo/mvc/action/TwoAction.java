package com.fanjia.spring.demo.mvc.action;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fanjia.spring.demo.service.IDemoSerivce;



public class TwoAction {
	
	private IDemoSerivce demoService;

	public void edit(HttpServletRequest req,HttpServletResponse resp,
					 String name){
		String result = demoService.get(name);
		try {
			resp.getWriter().write(result);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
}
