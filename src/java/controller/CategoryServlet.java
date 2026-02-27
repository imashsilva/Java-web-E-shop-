package controller;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.WebServlet;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.stream.Collectors;
import hibernate.Category;
import dao.CategoryDAO;
import model.CategoryDTO;
import com.google.gson.Gson;

@WebServlet("/categories")
public class CategoryServlet extends HttpServlet {
    
    private CategoryDAO categoryDAO;
    private Gson gson;
    
    @Override
    public void init() throws ServletException {
        categoryDAO = new CategoryDAO();
        gson = new Gson();
    }
    
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        String format = request.getParameter("format");
        
        if ("json".equals(format)) {
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            PrintWriter out = response.getWriter();
            
            try {
                List<Category> categories = categoryDAO.getAllCategories();
                
                // Convert to DTOs to avoid any Hibernate issues
                List<CategoryDTO> categoryDTOs = categories.stream()
                    .map(CategoryDTO::new)
                    .collect(Collectors.toList());
                
                String json = gson.toJson(categoryDTOs);
                out.write(json);
                System.out.println("Returning " + categoryDTOs.size() + " categories as JSON");
                
            } catch (Exception e) {
                e.printStackTrace();
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                out.write("{\"error\": \"Error fetching categories: " + e.getMessage() + "\"}");
            } finally {
                out.close();
            }
        }
    }
}