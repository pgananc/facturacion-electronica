// package com.wallparisoft.ebillbilling.util;
//
// import jakarta.servlet.Filter;
// import jakarta.servlet.FilterChain;
// import jakarta.servlet.FilterConfig;
// import jakarta.servlet.ServletException;
// import jakarta.servlet.ServletRequest;
// import jakarta.servlet.ServletResponse;
// import jakarta.servlet.http.HttpServletRequest;
// import jakarta.servlet.http.HttpServletResponse;
// import java.io.IOException;
// import org.springframework.core.Ordered;
// import org.springframework.core.annotation.Order;
// import org.springframework.stereotype.Component;
//
// @Component
// @Order(Ordered.HIGHEST_PRECEDENCE)
// public class CORS implements Filter {
//
//  @Override
//  public void init(FilterConfig filterConfig) {}
//
//  @Override
//  public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
//      throws IOException, ServletException {
//    HttpServletResponse response = (HttpServletResponse) res;
//    HttpServletRequest request = (HttpServletRequest) req;
//
//    response.setHeader("Access-Control-Allow-Origin", "*");
//    response.setHeader("Access-Control-Allow-Methods", "DELETE, GET, PATCH, POST, PUT, OPTIONS");
//    response.setHeader("Access-Control-Max-Age", "3600");
//    response.setHeader(
//        "Access-Control-Allow-Headers",
//        "x-requested-with, authorization, Content-Type, Authorization, credential, X-XSRF-TOKEN");
//    response.setHeader(
//        "Access-Control-Expose-Headers",
//        "X-RateLimit-Limit, X-RateLimit-Remaining, X-RateLimit-Reset");
//
//    if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
//      response.setStatus(HttpServletResponse.SC_OK);
//    } else {
//      chain.doFilter(req, res);
//    }
//  }
//
//  @Override
//  public void destroy() {}
// }
