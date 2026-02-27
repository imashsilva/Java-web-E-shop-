package controller;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.WebServlet;
import java.io.IOException;
import java.io.PrintWriter;
import hibernate.User;
import dao.UserDAO;

@WebServlet("/forgot-password")
public class ForgetPasswordServlet extends HttpServlet {
    
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
        
        String action = request.getParameter("action");
        
        try {
            if ("recover".equals(action)) {
                recoverPassword(request, response, out);
            } else if ("update".equals(action)) {
                updatePassword(request, response, out);
            } else {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.write("Invalid action");
            }
        } finally {
            out.close();
        }
    }
    
    private void recoverPassword(HttpServletRequest request, HttpServletResponse response, PrintWriter out) {
        String email = request.getParameter("email");
        
        try {
            if (email == null || email.trim().isEmpty()) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.write("Email is required");
                return;
            }
            
            User user = userDAO.getUserByEmail(email.trim());
            
            if (user == null) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                out.write("No account found with this email");
                return;
            }
            
            // Email found - proceed to password reset
            response.setStatus(HttpServletResponse.SC_OK);
            out.write("Email found");
            
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.write("An error occurred while recovering password");
        }
    }
    
    private void updatePassword(HttpServletRequest request, HttpServletResponse response, PrintWriter out) {
        String email = request.getParameter("email");
        String newPassword = request.getParameter("newPassword");
        String confirmPassword = request.getParameter("confirmPassword");
        
        try {
            if (email == null || email.trim().isEmpty()) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.write("Email is required");
                return;
            }
            
            if (newPassword == null || newPassword.trim().isEmpty()) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.write("New password is required");
                return;
            }
            
            if (!newPassword.equals(confirmPassword)) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.write("Passwords do not match");
                return;
            }
            
            User user = userDAO.getUserByEmail(email.trim());
            
            if (user == null) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                out.write("User not found");
                return;
            }
            
            // Update password
            user.setPassword(newPassword);
            boolean success = userDAO.updateUser(user);
            
            if (success) {
                response.setStatus(HttpServletResponse.SC_OK);
                out.write("Password updated successfully");
            } else {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                out.write("Failed to update password");
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.write("An error occurred while updating password");
        }
    }
}