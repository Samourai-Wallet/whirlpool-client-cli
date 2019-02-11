package com.samourai.whirlpool.cli.config.filters;

import com.samourai.whirlpool.cli.api.protocol.CliApi;
import com.samourai.whirlpool.cli.api.protocol.CliApiEndpoint;
import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletResponse;

@WebFilter(CliApiEndpoint.REST_PREFIX + "*")
public class CliApiWebFilter implements Filter {

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    // add apiVersion header
    HttpServletResponse httpServletResponse = (HttpServletResponse) response;
    httpServletResponse.setHeader(CliApi.HEADER_API_VERSION, CliApi.API_VERSION);
    chain.doFilter(request, response);
  }

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {}

  @Override
  public void destroy() {}
}
