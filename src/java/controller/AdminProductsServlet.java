package controller;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.*;
import java.io.*;
import com.google.gson.Gson;
import dao.ProductDAO;
import dao.CategoryDAO;
import hibernate.Product;
import hibernate.Category;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@WebServlet("/admin-products")
@MultipartConfig
public class AdminProductsServlet extends HttpServlet {
    
    private ProductDAO productDAO;
    private CategoryDAO categoryDAO;
    private Gson gson;
    
    @Override
    public void init() throws ServletException {
        productDAO = new ProductDAO();
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
                sendProductList(out);
            } else if ("get".equals(action)) {
                sendProductDetails(request, response, out);
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
                deleteProduct(request, response, out);
            } else {
                saveProduct(request, response, out);
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.write("{\"error\": \"Server error: " + e.getMessage() + "\"}");
        } finally {
            out.close();
        }
    }
    
    private void sendProductList(PrintWriter out) {
        List<Product> products = productDAO.getAllProducts();
        List<Map<String, Object>> productList = new ArrayList<Map<String, Object>>();
        
        for (Product product : products) {
            Map<String, Object> productMap = new HashMap<String, Object>();
            productMap.put("id", product.getId());
            productMap.put("name", product.getName());
            productMap.put("description", product.getDescription());
            productMap.put("price", product.getPrice());
            productMap.put("Quantity", product.getQuantity());
            productMap.put("imageUrl", product.getImageUrl());
            productMap.put("createdAt", product.getCreatedAt());
            
            if (product.getCategory() != null) {
                productMap.put("categoryId", product.getCategory().getId());
                productMap.put("categoryName", product.getCategory().getName());
            } else {
                productMap.put("categoryId", null);
                productMap.put("categoryName", "Uncategorized");
            }
            
            productList.add(productMap);
        }
        
        out.write(gson.toJson(productList));
    }
    
    private void sendProductDetails(HttpServletRequest request, HttpServletResponse response, PrintWriter out) {
        String id = request.getParameter("id");
        if (id == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.write("{\"error\": \"Product ID required\"}");
            return;
        }
        
        try {
            Long productId = Long.parseLong(id);
            Product product = productDAO.getProductById(productId);
            
            if (product == null) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                out.write("{\"error\": \"Product not found\"}");
                return;
            }
            
            Map<String, Object> productMap = new HashMap<String, Object>();
            productMap.put("id", product.getId());
            productMap.put("name", product.getName());
            productMap.put("description", product.getDescription());
            productMap.put("price", product.getPrice());
            productMap.put("Quantity", product.getQuantity());
            productMap.put("imageUrl", product.getImageUrl());
            
            if (product.getCategory() != null) {
                productMap.put("categoryId", product.getCategory().getId());
            } else {
                productMap.put("categoryId", null);
            }
            
            out.write(gson.toJson(productMap));
            
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.write("{\"error\": \"Invalid product ID\"}");
        }
    }
    
    private void saveProduct(HttpServletRequest request, HttpServletResponse response, PrintWriter out) throws Exception {
        String id = request.getParameter("id");
        String name = request.getParameter("name");
        String description = request.getParameter("description");
        String priceStr = request.getParameter("price");
        String quantityStr = request.getParameter("Quantity");
        String categoryIdStr = request.getParameter("categoryId");
        String imageUrl = request.getParameter("imageUrl");
        
        // Validation
        if (name == null || name.trim().isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.write("{\"error\": \"Product name is required\"}");
            return;
        }
        
        if (priceStr == null || priceStr.trim().isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.write("{\"error\": \"Price is required\"}");
            return;
        }
        
        if (quantityStr == null || quantityStr.trim().isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.write("{\"error\": \"Quantity is required\"}");
            return;
        }
        
        Product product;
        if (id != null && !id.isEmpty()) {
            // Update existing product
            Long productId = Long.parseLong(id);
            product = productDAO.getProductById(productId);
            if (product == null) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                out.write("{\"error\": \"Product not found\"}");
                return;
            }
        } else {
            // Create new product
            product = new Product();
        }
        
        product.setName(name.trim());
        product.setDescription(description != null ? description.trim() : "");
        product.setPrice(new BigDecimal(priceStr));
        product.setQuantity(Integer.parseInt(quantityStr));
        
        // Handle category
        if (categoryIdStr != null && !categoryIdStr.isEmpty()) {
            Long categoryId = Long.parseLong(categoryIdStr);
            Category category = categoryDAO.getCategoryById(categoryId);
            product.setCategory(category);
        } else {
            product.setCategory(null);
        }
        
        // Handle image
        if (imageUrl != null && !imageUrl.trim().isEmpty()) {
            product.setImageUrl(imageUrl.trim());
        }
        
        // Save product using ProductDAO
        boolean success = productDAO.saveProduct(product);
        if (success) {
            out.write("{\"success\": true, \"message\": \"Product saved successfully\"}");
        } else {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.write("{\"error\": \"Failed to save product\"}");
        }
    }
    
    private void deleteProduct(HttpServletRequest request, HttpServletResponse response, PrintWriter out) {
        String id = request.getParameter("id");
        if (id == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.write("{\"error\": \"Product ID required\"}");
            return;
        }
        
        try {
            Long productId = Long.parseLong(id);
            // Delete product using ProductDAO
            boolean success = productDAO.deleteProduct(productId);
            if (success) {
                out.write("{\"success\": true, \"message\": \"Product deleted successfully\"}");
            } else {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                out.write("{\"error\": \"Failed to delete product\"}");
            }
            
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.write("{\"error\": \"Invalid product ID\"}");
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