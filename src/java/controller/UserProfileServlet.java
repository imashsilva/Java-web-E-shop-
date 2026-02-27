package controller;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.WebServlet;
import java.io.IOException;
import java.io.PrintWriter;
import hibernate.User;
import dao.UserDAO;
import com.google.gson.Gson;

@WebServlet("/user-profile")
public class UserProfileServlet extends HttpServlet {
    
    private UserDAO userDAO;
    private Gson gson;
    
    @Override
    public void init() throws ServletException {
        userDAO = new UserDAO();
        gson = new Gson();
    }
    
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();
        
        HttpSession session = request.getSession(false);
        if (session == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            out.write("{\"error\": \"Not logged in\"}");
            return;
        }
        
        try {
            String username = (String) session.getAttribute("username");
            if (username == null) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                out.write("{\"error\": \"Not logged in\"}");
                return;
            }
            
            User user = userDAO.getUserByUsername(username);
            if (user == null) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                out.write("{\"error\": \"User not found\"}");
                return;
            }
            
            // Create a safe user object without password
            UserProfile userProfile = new UserProfile(user);
            String userJson = gson.toJson(userProfile);
            out.write(userJson);
            
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.write("{\"error\": \"Internal server error\"}");
        } finally {
            out.close();
        }
    }
    
    // Inner class for safe user data (without password)
    private static class UserProfile {
        private Long id;
        private String username;
        private String email;
        private String fullName;
        private String address;
        private String phone;
        private String role;
        private String createdAt;
        
        public UserProfile(User user) {
            this.id = user.getId();
            this.username = user.getUsername();
            this.email = user.getEmail();
            this.fullName = user.getFullName();
            this.address = user.getAddress();
            this.phone = user.getPhone();
            this.role = user.getRole().toString();
            this.createdAt = user.getCreatedAt().toString();
        }
        
        // Getters
        public Long getId() { return id; }
        public String getUsername() { return username; }
        public String getEmail() { return email; }
        public String getFullName() { return fullName; }
        public String getAddress() { return address; }
        public String getPhone() { return phone; }
        public String getRole() { return role; }
        public String getCreatedAt() { return createdAt; }
    }
}