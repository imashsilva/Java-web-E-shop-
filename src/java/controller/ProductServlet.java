package controller;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.WebServlet;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.stream.Collectors;
import hibernate.Product;
import hibernate.Category;
import dao.ProductDAO;
import dao.CategoryDAO;
import model.ProductDTO;
import com.google.gson.Gson;

@WebServlet("/products")
public class ProductServlet extends HttpServlet {
    
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
        
        String action = request.getParameter("action");
        String format = request.getParameter("format");
        
        System.out.println("ProductServlet called with format: " + format);
        
        if ("json".equals(format)) {
            // Return JSON data for AJAX calls
            handleJsonRequest(request, response);
        } else {
            // Return HTML page
            handleHtmlRequest(request, response);
        }
    }
    
    private void handleHtmlRequest(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        String categoryId = request.getParameter("category");
        String search = request.getParameter("search");
        String sort = request.getParameter("sort");
        
        try {
            // Get all categories for sidebar
            List<Category> categories = categoryDAO.getAllCategories();
            request.setAttribute("categories", categories);
            
            // Get products based on filters
            List<Product> products;
            if (categoryId != null && !categoryId.isEmpty()) {
                products = productDAO.getProductsByCategory(Long.parseLong(categoryId));
            } else if (search != null && !search.isEmpty()) {
                products = productDAO.searchProducts(search);
            } else {
                products = productDAO.getAllProducts();
            }
            
            System.out.println("Found " + products.size() + " products");
            
            // Apply sorting
            if (sort != null) {
                products = productDAO.sortProducts(products, sort);
            }
            
            request.setAttribute("products", products);
            request.setAttribute("selectedCategory", categoryId);
            request.setAttribute("searchQuery", search);
            request.setAttribute("sortBy", sort);
            
            request.getRequestDispatcher("/products.html").forward(request, response);
            
        } catch (Exception e) {
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error loading products");
        }
    }
    
    private void handleJsonRequest(HttpServletRequest request, HttpServletResponse response) 
            throws IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();
        
        try {
            String categoryId = request.getParameter("category");
            String search = request.getParameter("search");
            String sort = request.getParameter("sort");
            
            System.out.println("JSON request - category: " + categoryId + ", search: " + search);
            
            List<Product> products;
            if (categoryId != null && !categoryId.isEmpty()) {
                products = productDAO.getProductsByCategory(Long.parseLong(categoryId));
            } else if (search != null && !search.isEmpty()) {
                products = productDAO.searchProducts(search);
            } else {
                products = productDAO.getAllProducts();
            }
            
            if (sort != null) {
                products = productDAO.sortProducts(products, sort);
            }
            
            // Convert to DTOs to avoid Hibernate proxy issues
            List<ProductDTO> productDTOs = products.stream()
                .map(ProductDTO::new)
                .collect(Collectors.toList());
            
            System.out.println("Returning " + productDTOs.size() + " products as JSON");
            
            String json = gson.toJson(productDTOs);
            out.write(json);
            
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.write("{\"error\": \"Error fetching products: " + e.getMessage() + "\"}");
        } finally {
            out.close();
        }
    }
}