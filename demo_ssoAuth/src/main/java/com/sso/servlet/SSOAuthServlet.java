package com.sso.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class SSOAuthServlet extends HttpServlet {

	private String domainname;
	private String cookiename;

	private static ConcurrentMap accounts; // 模拟数据库的用户名、密码。
	private static ConcurrentMap SSOIDs; // 存放Cookie对象。
	
	@Override
	public void init(ServletConfig config) throws ServletException {
		domainname = config.getInitParameter("domainname");
		cookiename = config.getInitParameter("cookiename");

		SSOIDs = new ConcurrentHashMap();

		accounts = new ConcurrentHashMap();
		accounts.put("wangyu", "wangyu");
		accounts.put("paul", "paul");
		accounts.put("carol", "carol");
	}

	@Override
	protected void service(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		String action = request.getParameter("action");
		if (action == null) { // 登录时要执行的方法
			handlerFromLogin(request, response);
		} else { // 通过别的工程过滤器过来要执行的方法
			checkAction(request, response, action);
		}
	}
	
	/**
	 * 登录时要执行的方法
	 * 
	 * @param request
	 * @param response
	 * @throws ServletException
	 * @throws IOException
	 */
	private void handlerFromLogin(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		String username = request.getParameter("username");
		String password = request.getParameter("password");
		
		Object pass = accounts.get(username); // 登录时通过用户名查看是否有此对象，如果有则返回用户所对应的密码，如果没有对象，则返回null
		
		if (pass == null || !(pass.toString().equals(password))) {
			request.getServletContext().getRequestDispatcher("/failed.html").forward(request, response);
		} else {
			String gotoURL = request.getParameter("goto");
			
			System.out.println("login success, goto back url:" + gotoURL);
			
			// 登录成功做了两件事，一件是给SSOIDs存放当前用户对象，另一件是生成cookie，cookie的值来自web.xml中cookiename的值。
			String newID = UUID.randomUUID().toString();
			SSOIDs.put(newID, username);
			
			Cookie cookie = new Cookie("cookieName", cookiename);
			cookie.setDomain(this.domainname);
			cookie.setMaxAge(60000);
			cookie.setValue(newID);
			cookie.setPath("/");
			response.addCookie(cookie);
			
			if (gotoURL != null) {
				PrintWriter out = response.getWriter();
				response.sendRedirect(gotoURL);
				out.close();
			}
		}
	}
	
	/**
	 * 通过别的工程过滤器过来要执行的方法
	 * 
	 * @param request
	 * @param response
	 * @param action
	 */
	private void checkAction(HttpServletRequest request, HttpServletResponse response, String action) {
		String result = "failed";
		String myCookie;
		
		try {
			PrintWriter out = response.getWriter();
			
			if (action.equals("authcookie")) {
				myCookie = request.getParameter("cookiename");
				System.out.println("myCookie==================" + myCookie);
				if (myCookie != null) {
					Object username = SSOIDs.get(myCookie);
					if (username == null) {
						result = "failed";
						System.out.println("Authentication failed!");
					} else {
						result = username.toString();
						System.out.println("Authentication success!");
					}
					out.print(result);
					out.close();
				}
			}  else if (action.equals("authuser")) {
				String username = request.getParameter("username");
				String password = request.getParameter("password");

				Object pass = accounts.get(username);
				if (pass == null || !pass.toString().equals(password)) {
					result = "failed";
				} else {
					Date now = new Date();
					long time = now.getTime();
					result = "wangyu" + time;
					SSOIDs.put(result, username);
				}
				out.print(result);
				out.close();
			} else if (action.equals("logout")) {
				myCookie = request.getParameter("cookiename");
				System.out.println("Logout for " + myCookie);
				SSOIDs.remove(myCookie);
				out.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
