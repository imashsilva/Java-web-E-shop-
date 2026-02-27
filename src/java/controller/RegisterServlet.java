package controller;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.WebServlet;
import java.io.IOException;
import java.io.PrintWriter;
import hibernate.User;
import dao.UserDAO;

@WebServlet("/register")
public class RegisterServlet extends HttpServlet {
    
    private UserDAO userDAO;
    
    @Override
    public void init() throws ServletException {
        userDAO = new UserDAO();
    }
    
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        response.setContentType("text/plain");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();
        
        String username = request.getParameter("username");
        String email = request.getParameter("email");
        String password = request.getParameter("password");
        String confirmPassword = request.getParameter("confirmPassword");
        String fullName = request.getParameter("fullName");
        String phone = request.getParameter("phone");
        String address = request.getParameter("address");
        
        try {
            // Validation
            if (username == null || username.trim().isEmpty()) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.write("Username is required");
                return;
            }
            
            if (email == null || email.trim().isEmpty()) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.write("Email is required");
                return;
            }
            
            if (password == null || password.trim().isEmpty()) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.write("Password is required");
                return;
            }
            
            if (!password.equals(confirmPassword)) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.write("Passwords do not match");
                return;
            }
            
            // Check if username already exists
            if (userDAO.isUsernameExists(username.trim())) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.write("Username already exists");
                return;
            }
            
            // Check if email already exists
            if (userDAO.isEmailExists(email.trim())) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.write("Email already exists");
                return;
            }
            
            // Create new user
            User user = new User();
            user.setUsername(username.trim());
            user.setEmail(email.trim());
            user.setPassword(password); // No hashing
            user.setFullName(fullName != null ? fullName.trim() : "");
            user.setPhone(phone != null ? phone.trim() : "");
            user.setAddress(address != null ? address.trim() : "");
            
            // Role is automatically set to CUSTOMER in User constructor
            // No need to manually set it
            
            // Save user
            boolean success = userDAO.saveUser(user);
            
            if (success) {
                response.setStatus(HttpServletResponse.SC_OK);
                out.write("Registration successful");
            } else {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                out.write("Registration failed. Please try again.");
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.write("An error occurred during registration");
        } finally {
            out.close();
        }
    }
}