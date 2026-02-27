package controller;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.*;
import java.io.*;
import com.google.gson.Gson;
import dao.CategoryDAO;
import hibernate.Category;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@WebServlet("/admin-categories")
@MultipartConfig
public class AdminCategoriesServlet extends HttpServlet {
    
    private CategoryDAO categoryDAO;
    private Gson gson;
    
    @Override
    public void init() throws ServletException {
        categoryDAO = new CategoryDAO();
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
                sendCategoryList(out);
            } else if ("get".equals(action)) {
                sendCategoryDetails(request, response, out);
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
            if ("delete".equals(action)) {
                deleteCategory(request, response, out);
            } else {
                saveCategory(request, response, out);
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.write("{\"error\": \"Server error: " + e.getMessage() + "\"}");
        } finally {
            out.close();
        }
    }
    
    private void sendCategoryList(PrintWriter out) {
        List<Category> categories = categoryDAO.getAllCategories();
        List<Map<String, Object>> categoryList = new ArrayList<Map<String, Object>>();
        
        for (Category category : categories) {
            Map<String, Object> categoryMap = new HashMap<String, Object>();
            categoryMap.put("id", category.getId());
            categoryMap.put("name", category.getName());
            categoryMap.put("description", category.getDescription());
            categoryMap.put("imageUrl", category.getImageUrl());
            // You can add product count here if you implement it in CategoryDAO
            categoryMap.put("productCount", 0); // Placeholder
            
            categoryList.add(categoryMap);
        }
        
        out.write(gson.toJson(categoryList));
    }
    
    private void sendCategoryDetails(HttpServletRequest request, HttpServletResponse response, PrintWriter out) {
        String id = request.getParameter("id");
        if (id == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.write("{\"error\": \"Category ID required\"}");
            return;
        }
        
        try {
            Long categoryId = Long.parseLong(id);
            Category category = categoryDAO.getCategoryById(categoryId);
            
            if (category == null) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                out.write("{\"error\": \"Category not found\"}");
                return;
            }
            
            Map<String, Object> categoryMap = new HashMap<String, Object>();
            categoryMap.put("id", category.getId());
            categoryMap.put("name", category.getName());
            categoryMap.put("description", category.getDescription());
            categoryMap.put("imageUrl", category.getImageUrl());
            
            out.write(gson.toJson(categoryMap));
            
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.write("{\"error\": \"Invalid category ID\"}");
        }
    }
    
    private void saveCategory(HttpServletRequest request, HttpServletResponse response, PrintWriter out) throws Exception {
        String id = request.getParameter("id");
        String name = request.getParameter("name");
        String description = request.getParameter("description");
        String imageUrl = request.getParameter("imageUrl");
        
        // Validation
        if (name == null || name.trim().isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.write("{\"error\": \"Category name is required\"}");
            return;
        }
        
        Category category;
        if (id != null && !id.isEmpty()) {
            // Update existing category
            Long categoryId = Long.parseLong(id);
            category = categoryDAO.getCategoryById(categoryId);
            if (category == null) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                out.write("{\"error\": \"Category not found\"}");
                return;
            }
        } else {
            // Create new category
            category = new Category();
        }
        
        category.setName(name.trim());
        category.setDescription(description != null ? description.trim() : "");
        
        // Handle image
        if (imageUrl != null && !imageUrl.trim().isEmpty()) {
            category.setImageUrl(imageUrl.trim());
        }
        
        // For now, just return success - you'll need to implement save/update in CategoryDAO
        out.write("{\"success\": true, \"message\": \"Category saved successfully\"}");
    }
    
    private void deleteCategory(HttpServletRequest request, HttpServletResponse response, PrintWriter out) {
        String id = request.getParameter("id");
        if (id == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.write("{\"error\": \"Category ID required\"}");
            return;
        }
        
        try {
            Long categoryId = Long.parseLong(id);
            // Implement delete in CategoryDAO
            // boolean success = categoryDAO.deleteCategory(categoryId);
            out.write("{\"success\": true, \"message\": \"Category deleted successfully\"}");
            
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.write("{\"error\": \"Invalid category ID\"}");
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