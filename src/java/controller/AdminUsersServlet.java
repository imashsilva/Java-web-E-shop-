package controller;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.*;
import java.io.*;
import com.google.gson.Gson;
import dao.UserDAO;
import hibernate.User;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@WebServlet("/admin-users")
public class AdminUsersServlet extends HttpServlet {
    
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
        
        // Check admin authentication
        if (!isAdmin(request)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            out.write("{\"error\": \"Admin access required\"}");
            out.close();
            return;
        }
        
        String action = request.getParameter("action");
        
        try {
            if ("list".equals(action)) {
                sendUserList(out);
            } else if ("get".equals(action)) {
                sendUserDetails(request, response, out);
            } else {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.write("{\"error\": \"Invalid action\"}");
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.write("{\"error\": \"Server error: " + e.getMessage() + "\"}");
        } finally {
            out.close();
        }
    }
    
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();
        
        // Check admin authentication
        if (!isAdmin(request)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            out.write("{\"error\": \"Admin access required\"}");
            out.close();
            return;
        }
        
        String action = request.getParameter("action");
        
        try {
            if ("update-role".equals(action)) {
                updateUserRole(request, response, out);
            } else {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.write("{\"error\": \"Invalid action\"}");
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.write("{\"error\": \"Server error: " + e.getMessage() + "\"}");
        } finally {
            out.close();
        }
    }
    
    private void sendUserList(PrintWriter out) {
        List<User> users = userDAO.getRecentUsers(100); // Get recent users with high limit
        List<Map<String, Object>> userList = new ArrayList<Map<String, Object>>();
        
        for (User user : users) {
            Map<String, Object> userMap = new HashMap<String, Object>();
            userMap.put("id", user.getId());
            userMap.put("username", user.getUsername());
            userMap.put("email", user.getEmail());
            userMap.put("fullName", user.getFullName());
            userMap.put("phone", user.getPhone());
            userMap.put("address", user.getAddress());
            userMap.put("role", user.getRole().name());
            userMap.put("createdAt", user.getCreatedAt());
            
            userList.add(userMap);
        }
        
        out.write(gson.toJson(userList));
    }
    
    private void sendUserDetails(HttpServletRequest request, HttpServletResponse response, PrintWriter out) {
        String id = request.getParameter("id");
        if (id == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.write("{\"error\": \"User ID required\"}");
            return;
        }
        
        try {
            Long userId = Long.parseLong(id);
            User user = userDAO.getUserById(userId);
            
            if (user == null) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                out.write("{\"error\": \"User not found\"}");
                return;
            }
            
            Map<String, Object> userMap = new HashMap<String, Object>();
            userMap.put("id", user.getId());
            userMap.put("username", user.getUsername());
            userMap.put("email", user.getEmail());
            userMap.put("fullName", user.getFullName());
            userMap.put("phone", user.getPhone());
            userMap.put("address", user.getAddress());
            userMap.put("role", user.getRole().name());
            userMap.put("createdAt", user.getCreatedAt());
            
            out.write(gson.toJson(userMap));
            
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.write("{\"error\": \"Invalid user ID\"}");
        }
    }
    
    private void updateUserRole(HttpServletRequest request, HttpServletResponse response, PrintWriter out) {
        String id = request.getParameter("id");
        String role = request.getParameter("role");
        
        if (id == null || role == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.write("{\"error\": \"User ID and role are required\"}");
            return;
        }
        
        try {
            Long userId = Long.parseLong(id);
            User user = userDAO.getUserById(userId);
            
            if (user == null) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                out.write("{\"error\": \"User not found\"}");
                return;
            }
            
            // Update user role
            user.setRole(User.UserRole.valueOf(role));
            boolean success = userDAO.updateUser(user);
            
            if (success) {
                out.write("{\"success\": true, \"message\": \"User role updated successfully\"}");
            } else {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                out.write("{\"error\": \"Failed to update user role\"}");
            }
            
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.write("{\"error\": \"Invalid user ID\"}");
        } catch (IllegalArgumentException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.write("{\"error\": \"Invalid role value\"}");
        }
    }
    
    private boolean isAdmin(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            hibernate.User user = (hibernate.User) session.getAttribute("user");
            return user != null && user.isAdmin();
        }
        return false;
    }
}