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

	private static ConcurrentMap accounts; // ģ�����ݿ���û��������롣
	private static ConcurrentMap SSOIDs; // ���Cookie����
	
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
		if (action == null) { // ��¼ʱҪִ�еķ���
			handlerFromLogin(request, response);
		} else { // ͨ����Ĺ��̹���������Ҫִ�еķ���
			checkAction(request, response, action);
		}
	}
	
	/**
	 * ��¼ʱҪִ�еķ���
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
		
		Object pass = accounts.get(username); // ��¼ʱͨ���û����鿴�Ƿ��д˶���������򷵻��û�����Ӧ�����룬���û�ж����򷵻�null
		
		if (pass == null || !(pass.toString().equals(password))) {
			request.getServletContext().getRequestDispatcher("/failed.html").forward(request, response);
		} else {
			String gotoURL = request.getParameter("goto");
			
			System.out.println("login success, goto back url:" + gotoURL);
			
			// ��¼�ɹ����������£�һ���Ǹ�SSOIDs��ŵ�ǰ�û�������һ��������cookie��cookie��ֵ����web.xml��cookiename��ֵ��
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
	 * ͨ����Ĺ��̹���������Ҫִ�еķ���
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
