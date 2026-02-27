package filter;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.*;
import java.io.IOException;
import hibernate.User;

@WebFilter("/admin/*")
public class AdminFilter implements Filter {
    
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        HttpSession session = httpRequest.getSession(false);
        
        String loginURI = httpRequest.getContextPath() + "/admin-login.html";
        boolean isLoginRequest = httpRequest.getRequestURI().equals(loginURI);
        
        // Allow access to admin login page
        if (isLoginRequest) {
            chain.doFilter(request, response);
            return;
        }
        
        // Check if user is authenticated and is admin
        if (session != null) {
            User user = (User) session.getAttribute("user");
            if (user != null && user.isAdmin()) {
                chain.doFilter(request, response);
                return;
            }
        }
        
        // Redirect to admin login if not authorized
        httpResponse.sendRedirect(loginURI);
    }
}