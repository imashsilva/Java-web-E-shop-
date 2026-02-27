package controller;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.*;
import java.io.*;
import com.google.gson.Gson;
import dao.OrderDAO;
import hibernate.Order;
import hibernate.OrderItem;
import hibernate.OrderStatus; // Import the separate OrderStatus enum
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@WebServlet("/admin-orders")
public class AdminOrdersServlet extends HttpServlet {
    
    private OrderDAO orderDAO;
    private Gson gson;
    
    @Override
    public void init() throws ServletException {
        orderDAO = new OrderDAO();
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
                sendOrderList(request, out);
            } else if ("get".equals(action)) {
                sendOrderDetails(request, response, out);
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
            if ("update-status".equals(action)) {
                updateOrderStatus(request, response, out);
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
    
    private void sendOrderList(HttpServletRequest request, PrintWriter out) {
        String statusFilter = request.getParameter("status");
        
        // Use getAllOrders instead of getRecentOrders since getRecentOrders returns Map
        List<Order> orders = orderDAO.getAllOrders();
        List<Map<String, Object>> orderList = new ArrayList<Map<String, Object>>();
        
        for (Order order : orders) {
            // Apply status filter if provided
            if (statusFilter != null && !statusFilter.isEmpty() && !statusFilter.equals(order.getStatus().name())) {
                continue;
            }
            
            Map<String, Object> orderMap = new HashMap<String, Object>();
            orderMap.put("id", order.getId());
            orderMap.put("orderDate", order.getOrderDate());
            orderMap.put("totalAmount", order.getTotalAmount());
            orderMap.put("status", order.getStatus().name());
            orderMap.put("shippingAddress", order.getShippingAddress());
            orderMap.put("itemCount", order.getOrderItems() != null ? order.getOrderItems().size() : 0);
            
            if (order.getUser() != null) {
                orderMap.put("customerName", order.getUser().getFullName());
                orderMap.put("customerEmail", order.getUser().getEmail());
            } else {
                orderMap.put("customerName", "Guest");
                orderMap.put("customerEmail", "");
            }
            
            orderList.add(orderMap);
        }
        
        out.write(gson.toJson(orderList));
    }
    
    private void sendOrderDetails(HttpServletRequest request, HttpServletResponse response, PrintWriter out) {
        String id = request.getParameter("id");
        if (id == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.write("{\"error\": \"Order ID required\"}");
            return;
        }
        
        try {
            Long orderId = Long.parseLong(id);
            // Use your method that handles LazyInitializationException
            Order order = orderDAO.getOrderWithUserData(orderId);
            
            if (order == null) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                out.write("{\"error\": \"Order not found\"}");
                return;
            }
            
            Map<String, Object> orderMap = new HashMap<String, Object>();
            orderMap.put("id", order.getId());
            orderMap.put("orderDate", order.getOrderDate());
            orderMap.put("totalAmount", order.getTotalAmount());
            orderMap.put("status", order.getStatus().name());
            orderMap.put("shippingAddress", order.getShippingAddress());
            
            if (order.getUser() != null) {
                orderMap.put("customerName", order.getUser().getFullName());
                orderMap.put("customerEmail", order.getUser().getEmail());
            } else {
                orderMap.put("customerName", "Guest");
                orderMap.put("customerEmail", "");
            }
            
            // Order items
            List<Map<String, Object>> orderItems = new ArrayList<Map<String, Object>>();
            if (order.getOrderItems() != null) {
                for (OrderItem item : order.getOrderItems()) {
                    Map<String, Object> itemMap = new HashMap<String, Object>();
                    itemMap.put("productName", item.getProduct() != null ? item.getProduct().getName() : "Unknown Product");
                    itemMap.put("quantity", item.getQuantity());
                    itemMap.put("price", item.getPrice());
                    itemMap.put("subtotal", item.getSubtotal());
                    orderItems.add(itemMap);
                }
            }
            orderMap.put("orderItems", orderItems);
            
            out.write(gson.toJson(orderMap));
            
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.write("{\"error\": \"Invalid order ID\"}");
        }
    }
    
    private void updateOrderStatus(HttpServletRequest request, HttpServletResponse response, PrintWriter out) {
        String id = request.getParameter("id");
        String status = request.getParameter("status");
        
        if (id == null || status == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.write("{\"error\": \"Order ID and status are required\"}");
            return;
        }
        
        try {
            Long orderId = Long.parseLong(id);
            
            // Convert string status to OrderStatus enum (using the separate enum class)
            OrderStatus orderStatus;
            try {
                orderStatus = OrderStatus.valueOf(status);
            } catch (IllegalArgumentException e) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.write("{\"error\": \"Invalid status value\"}");
                return;
            }
            
            // Use your existing updateOrderStatus method
            boolean success = orderDAO.updateOrderStatus(orderId, orderStatus);
            
            if (success) {
                out.write("{\"success\": true, \"message\": \"Order status updated successfully\"}");
            } else {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                out.write("{\"error\": \"Failed to update order status\"}");
            }
            
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.write("{\"error\": \"Invalid order ID\"}");
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