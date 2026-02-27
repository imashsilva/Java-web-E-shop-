package controller;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.WebServlet;
import java.io.IOException;
import java.io.PrintWriter;
import hibernate.Product;
import dao.ProductDAO;
import model.ProductDTO;
import com.google.gson.Gson;

@WebServlet("/product-details")
public class ProductDetailServlet extends HttpServlet {
    
    private ProductDAO productDAO;
    private Gson gson;
    
    @Override
    public void init() throws ServletException {
        productDAO = new ProductDAO();
        gson = new Gson();
    }
    
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        String productId = request.getParameter("id");
        String format = request.getParameter("format");
        
        System.out.println("ProductDetailServlet called with ID: " + productId);
        
        if (productId == null || productId.isEmpty()) {
            System.out.println("Error: Product ID is required");
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Product ID is required");
            return;
        }
        
        try {
            Long id = Long.parseLong(productId);
            System.out.println("Looking for product with ID: " + id);
            
            Product product = productDAO.getProductById(id);
            
            if (product == null) {
                System.out.println("Product not found with ID: " + id);
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "Product not found");
                return;
            }
            
            System.out.println("Product found: " + product.getName());
            
            // Debug: Print product details
            System.out.println("Product details - Name: " + product.getName() + 
                             ", Price: " + product.getPrice() + 
                             ", Quantity: " + product.getQuantity());
            
            if (product.getCategory() != null) {
                System.out.println("Category: " + product.getCategory().getName());
            } else {
                System.out.println("Category is null");
            }
            
            if ("json".equals(format)) {
                // Return JSON data using DTO
                response.setContentType("application/json");
                response.setCharacterEncoding("UTF-8");
                PrintWriter out = response.getWriter();
                
                ProductDTO productDTO = new ProductDTO(product);
                String json = gson.toJson(productDTO);
                System.out.println("Returning JSON: " + json);
                out.write(json);
                out.close();
            } else {
                // Return HTML page
                request.setAttribute("product", product);
                request.setAttribute("relatedProducts", productDAO.getRelatedProducts(product));
                request.getRequestDispatcher("/product-detail.html").forward(request, response);
            }
            
        } catch (NumberFormatException e) {
            System.out.println("Invalid product ID format: " + productId);
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid product ID");
        } catch (Exception e) {
            System.out.println("Error loading product: " + e.getMessage());
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error loading product: " + e.getMessage());
        }
    }
}