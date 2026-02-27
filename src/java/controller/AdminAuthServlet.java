package controller;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.WebServlet;
import java.io.IOException;
import java.io.PrintWriter;
import com.google.gson.Gson;
import hibernate.User;

@WebServlet("/admin-auth")
public class AdminAuthServlet extends HttpServlet {
    
    private Gson gson;
    
    @Override
    public void init() throws ServletException {
        gson = new Gson();
    }
    
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();
        
        String action = request.getParameter("action");
        
        try {
            HttpSession session = request.getSession(false);
            
            if ("check".equals(action)) {
                // Check if user is admin
                if (session != null) {
                    User user = (User) session.getAttribute("user");
                    boolean isAdmin = (user != null && user.isAdmin());
                    
                    out.write(gson.toJson(new AuthResponse(isAdmin, isAdmin ? user.getUsername() : null)));
                } else {
                    out.write(gson.toJson(new AuthResponse(false, null)));
                }
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.write("{\"error\": \"Authentication check failed\"}");
        } finally {
            out.close();
        }
    }
    
    // Helper class for JSON response
    private static class AuthResponse {
        private boolean isAdmin;
        private String username;
        
        public AuthResponse(boolean isAdmin, String username) {
            this.isAdmin = isAdmin;
            this.username = username;
        }
        
        public boolean getIsAdmin() { return isAdmin; }
        public String getUsername() { return username; }
    }
}