package controller;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.WebServlet;
import java.io.IOException;
import java.io.PrintWriter;
import com.google.gson.Gson;
import dao.ProductDAO;
import dao.UserDAO;
import dao.OrderDAO;
import hibernate.User;
import hibernate.Product;
import java.util.*;

@WebServlet("/admin-dashboard")
public class AdminDashboardServlet extends HttpServlet {
    
    private ProductDAO productDAO;
    private UserDAO userDAO;
    private OrderDAO orderDAO;
    private Gson gson;
    
    @Override
    public void init() throws ServletException {
        productDAO = new ProductDAO();
        userDAO = new UserDAO();
        orderDAO = new OrderDAO();
        gson = new Gson();
    }
    
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();
        
        // Check admin authentication
        HttpSession session = request.getSession(false);
        if (session == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            out.write("{\"error\": \"Not authenticated\"}");
            return;
        }
        
        String action = request.getParameter("action");
        
        try {
            switch (action) {
                case "stats":
                    sendDashboardStats(out);
                    break;
                case "recent-orders":
                    sendRecentOrders(out);
                    break;
                case "low-stock":
                    sendLowStockAlerts(out);
                    break;
                case "status-counts":
                    sendOrderStatusCounts(out);
                    break;
                default:
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
    
    private void sendDashboardStats(PrintWriter out) {
        Map<String, Object> stats = new HashMap<>();
        
        // Get real data from database
        Long totalOrders = orderDAO.getTotalOrdersCount();
        Double totalRevenue = orderDAO.getTotalRevenue();
        Long totalProducts = (long) productDAO.getAllProducts().size();
        Long totalUsers = userDAO.getTotalUsersCount();
        
        // Today's statistics
        Long todayOrders = orderDAO.getTodayOrdersCount();
        Double todayRevenue = orderDAO.getTodayRevenue();
        Long todayUsers = userDAO.getTodayRegisteredUsersCount();
        
        // Low stock alerts
        Long lowStockCount = productDAO.getLowStockCount(10); // Threshold of 10
        Long outOfStockCount = productDAO.getOutOfStockCount();
        
        stats.put("totalOrders", totalOrders);
        stats.put("totalRevenue", String.format("%.2f", totalRevenue));
        stats.put("totalProducts", totalProducts);
        stats.put("totalUsers", totalUsers);
        stats.put("todayOrders", todayOrders);
        stats.put("todayRevenue", String.format("%.2f", todayRevenue));
        stats.put("todayUsers", todayUsers);
        stats.put("lowStockCount", lowStockCount);
        stats.put("outOfStockCount", outOfStockCount);
        
        out.write(gson.toJson(stats));
    }
    
    private void sendRecentOrders(PrintWriter out) {
        List<Map<String, Object>> recentOrders = orderDAO.getRecentOrders(5);
        
        // Format the data for frontend
        List<Map<String, Object>> formattedOrders = new ArrayList<>();
        for (Map<String, Object> order : recentOrders) {
            Map<String, Object> formattedOrder = new HashMap<>();
            formattedOrder.put("id", order.get("id"));
            formattedOrder.put("customerName", order.get("customerName") != null ? 
                order.get("customerName") : "Guest");
            formattedOrder.put("total", order.get("total"));
            formattedOrder.put("date", formatDate(order.get("date")));
            formattedOrder.put("status", order.get("status"));
            formattedOrders.add(formattedOrder);
        }
        
        out.write(gson.toJson(formattedOrders));
    }
    
    private void sendLowStockAlerts(PrintWriter out) {
        // Get products with stock <= 5
        List<Product> lowStockProducts = productDAO.getLowStockProducts(5);
        
        List<Map<String, Object>> alerts = new ArrayList<>();
        for (Product product : lowStockProducts) {
            Map<String, Object> alert = new HashMap<>();
            alert.put("id", product.getId());
            alert.put("name", product.getName());
            alert.put("stock", product.getQuantity());
            alert.put("price", product.getPrice());
            alerts.add(alert);
        }
        
        out.write(gson.toJson(alerts));
    }
    
    private void sendOrderStatusCounts(PrintWriter out) {
        Map<String, Long> statusCounts = orderDAO.getOrdersByStatus();
        out.write(gson.toJson(statusCounts));
    }
    
    private String formatDate(Object dateObj) {
        if (dateObj instanceof Date) {
            Date date = (Date) dateObj;
            return String.format("%tF", date); // Returns YYYY-MM-DD format
        }
        return String.valueOf(dateObj);
    }
}