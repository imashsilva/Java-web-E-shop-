package controller;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.*;
import java.io.*;
import com.google.gson.Gson;
import dao.OrderDAO;
import dao.ProductDAO;
import dao.UserDAO;
import hibernate.Order;
import hibernate.Product;
import hibernate.User;
import hibernate.OrderStatus;
import java.util.*;
import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.math.BigDecimal;

@WebServlet("/admin-reports")
public class AdminReportsServlet extends HttpServlet {
    
    private OrderDAO orderDAO;
    private ProductDAO productDAO;
    private UserDAO userDAO;
    private Gson gson;
    
    @Override
    public void init() throws ServletException {
        orderDAO = new OrderDAO();
        productDAO = new ProductDAO();
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
            if ("quick-stats".equals(action)) {
                sendQuickStats(out);
            } else if ("sales-report".equals(action)) {
                sendSalesReport(request, out);
            } else if ("inventory-report".equals(action)) {
                sendInventoryReport(out);
            } else if ("customer-report".equals(action)) {
                sendCustomerReport(request, out);
            } else if ("payment-receipts".equals(action)) {
                sendPaymentReceipts(out);
            } else if ("export-sales".equals(action)) {
                exportSalesReport(request, response);
            } else if ("export-inventory".equals(action)) {
                exportInventoryReport(response);
            } else if ("export-customers".equals(action)) {
                exportCustomerReport(request, response);
            } else if ("print-receipt".equals(action)) {
                printReceipt(request, response);
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
    
    private void sendQuickStats(PrintWriter out) {
        Map<String, Object> stats = new HashMap<String, Object>();
        
        // Get real data from DAOs
        Long totalOrders = orderDAO.getTotalOrdersCount();
        Double totalRevenue = orderDAO.getTotalRevenue();
        Long totalProducts = (long) productDAO.getAllProducts().size();
        Long totalUsers = userDAO.getTotalUsersCount();
        
        stats.put("totalOrders", totalOrders);
        stats.put("totalRevenue", String.format("%.2f", totalRevenue != null ? totalRevenue : 0.0));
        stats.put("totalProducts", totalProducts);
        stats.put("totalUsers", totalUsers);
        
        out.write(gson.toJson(stats));
    }
    
    private void sendSalesReport(HttpServletRequest request, PrintWriter out) throws Exception {
        String month = request.getParameter("month");
        Map<String, Object> report = new HashMap<String, Object>();
        
        // Get all orders for calculations
        List<Order> allOrders = orderDAO.getAllOrders();
        
        double totalSales = 0.0;
        int completedOrders = 0;
        int totalOrderCount = 0;
        Map<String, Integer> categorySales = new HashMap<String, Integer>();
        Map<String, Double> dailySales = new HashMap<String, Double>();
        
        SimpleDateFormat monthFormat = new SimpleDateFormat("yyyy-MM");
        SimpleDateFormat dayFormat = new SimpleDateFormat("yyyy-MM-dd");
        
        for (Order order : allOrders) {
            // Filter by month if specified
            if (month != null && !month.isEmpty()) {
                String orderMonth = monthFormat.format(order.getOrderDate());
                if (!orderMonth.equals(month)) {
                    continue;
                }
            }
            
            totalOrderCount++;
            
            if (order.getStatus() == OrderStatus.DELIVERED) {
                completedOrders++;
                if (order.getTotalAmount() != null) {
                    totalSales += order.getTotalAmount().doubleValue();
                }
            }
            
            // Track daily sales
            String orderDay = dayFormat.format(order.getOrderDate());
            double dayTotal = dailySales.getOrDefault(orderDay, 0.0);
            if (order.getTotalAmount() != null) {
                dayTotal += order.getTotalAmount().doubleValue();
            }
            dailySales.put(orderDay, dayTotal);
            
            // Track category sales (simplified - you might need to enhance this)
            if (order.getOrderItems() != null) {
                for (hibernate.OrderItem item : order.getOrderItems()) {
                    if (item.getProduct() != null && item.getProduct().getCategory() != null) {
                        String categoryName = item.getProduct().getCategory().getName();
                        int currentCount = categorySales.getOrDefault(categoryName, 0);
                        categorySales.put(categoryName, currentCount + item.getQuantity());
                    }
                }
            }
        }
        
        // Find best selling category
        String bestCategory = "None";
        int maxSales = 0;
        for (Map.Entry<String, Integer> entry : categorySales.entrySet()) {
            if (entry.getValue() > maxSales) {
                maxSales = entry.getValue();
                bestCategory = entry.getKey();
            }
        }
        
        double avgOrderValue = totalOrderCount > 0 ? totalSales / totalOrderCount : 0;
        
        report.put("totalSales", String.format("%.2f", totalSales));
        report.put("completedOrders", completedOrders);
        report.put("avgOrderValue", String.format("%.2f", avgOrderValue));
        report.put("bestCategory", bestCategory);
        report.put("dailySales", dailySales);
        
        out.write(gson.toJson(report));
    }
    
    private void sendInventoryReport(PrintWriter out) {
        Map<String, Object> report = new HashMap<String, Object>();
        
        List<Product> allProducts = productDAO.getAllProducts();
        
        int inStockCount = 0;
        int lowStockCount = 0;
        int outOfStockCount = 0;
        double inStockValue = 0.0;
        double lowStockValue = 0.0;
        double outOfStockValue = 0.0;
        
        // Track top products by potential revenue
        List<Map<String, Object>> topProducts = new ArrayList<Map<String, Object>>();
        
        for (Product product : allProducts) {
            double productValue = 0.0;
            if (product.getPrice() != null && product.getQuantity() != null) {
                productValue = product.getPrice().doubleValue() * product.getQuantity();
            }
            
            if (product.getQuantity() == 0) {
                outOfStockCount++;
                outOfStockValue += productValue;
            } else if (product.getQuantity() <= 5) {
                lowStockCount++;
                lowStockValue += productValue;
            } else {
                inStockCount++;
                inStockValue += productValue;
            }
            
            // For top products, we'll use a simple metric (price * quantity)
            // In a real scenario, you'd use actual sales data
            Map<String, Object> productInfo = new HashMap<String, Object>();
            productInfo.put("name", product.getName());
            productInfo.put("sold", 0); // You'd need to track actual sales
            productInfo.put("revenue", String.format("%.2f", productValue));
            productInfo.put("stock", product.getQuantity());
            topProducts.add(productInfo);
        }
        
        // Sort by revenue (product value)
        topProducts.sort((a, b) -> {
            double revenueA = Double.parseDouble((String) a.get("revenue"));
            double revenueB = Double.parseDouble((String) b.get("revenue"));
            return Double.compare(revenueB, revenueA);
        });
        
        // Take top 5
        if (topProducts.size() > 5) {
            topProducts = topProducts.subList(0, 5);
        }
        
        report.put("inStockCount", inStockCount);
        report.put("inStockValue", String.format("%.2f", inStockValue));
        report.put("lowStockCount", lowStockCount);
        report.put("lowStockValue", String.format("%.2f", lowStockValue));
        report.put("outOfStockCount", outOfStockCount);
        report.put("outOfStockValue", String.format("%.2f", outOfStockValue));
        report.put("topProducts", topProducts);
        
        out.write(gson.toJson(report));
    }
    
    private void sendCustomerReport(HttpServletRequest request, PrintWriter out) throws Exception {
        String month = request.getParameter("month");
        Map<String, Object> report = new HashMap<String, Object>();
        
        List<User> allUsers = userDAO.getRecentUsers(1000); // Get all users
        List<Order> allOrders = orderDAO.getAllOrders();
        
        SimpleDateFormat monthFormat = new SimpleDateFormat("yyyy-MM");
        Calendar cal = Calendar.getInstance();
        
        // Count new customers for the selected month
        int newCustomers = 0;
        int totalCustomers = allUsers.size();
        
        if (month != null && !month.isEmpty()) {
            for (User user : allUsers) {
                String userMonth = monthFormat.format(user.getCreatedAt());
                if (userMonth.equals(month)) {
                    newCustomers++;
                }
            }
        } else {
            // If no month specified, count current month
            String currentMonth = monthFormat.format(new Date());
            for (User user : allUsers) {
                String userMonth = monthFormat.format(user.getCreatedAt());
                if (userMonth.equals(currentMonth)) {
                    newCustomers++;
                }
            }
        }
        
        // Calculate customer metrics
        Map<Long, Integer> customerOrderCount = new HashMap<Long, Integer>();
        Map<Long, Double> customerSpending = new HashMap<Long, Double>();
        
        for (Order order : allOrders) {
            if (order.getUser() != null) {
                Long userId = order.getUser().getId();
                int orderCount = customerOrderCount.getOrDefault(userId, 0);
                customerOrderCount.put(userId, orderCount + 1);
                
                double spent = customerSpending.getOrDefault(userId, 0.0);
                if (order.getTotalAmount() != null) {
                    spent += order.getTotalAmount().doubleValue();
                }
                customerSpending.put(userId, spent);
            }
        }
        
        // Calculate repeat customer rate
        int repeatCustomers = 0;
        for (int orderCount : customerOrderCount.values()) {
            if (orderCount > 1) {
                repeatCustomers++;
            }
        }
        
        double repeatRate = totalCustomers > 0 ? (double) repeatCustomers / totalCustomers * 100 : 0;
        double totalRevenue = orderDAO.getTotalRevenue() != null ? orderDAO.getTotalRevenue() : 0;
        double avgCustomerValue = totalCustomers > 0 ? totalRevenue / totalCustomers : 0;
        
        // Customer growth (last 6 months)
        Map<String, Integer> customerGrowth = new HashMap<String, Integer>();
        for (int i = 5; i >= 0; i--) {
            cal.setTime(new Date());
            cal.add(Calendar.MONTH, -i);
            String growthMonth = monthFormat.format(cal.getTime());
            customerGrowth.put(growthMonth, 0);
        }
        
        for (User user : allUsers) {
            String userMonth = monthFormat.format(user.getCreatedAt());
            if (customerGrowth.containsKey(userMonth)) {
                customerGrowth.put(userMonth, customerGrowth.get(userMonth) + 1);
            }
        }
        
        // Top customers by spending
        List<Map<String, Object>> topCustomers = new ArrayList<Map<String, Object>>();
        for (Map.Entry<Long, Double> entry : customerSpending.entrySet()) {
            User user = userDAO.getUserById(entry.getKey());
            if (user != null) {
                Map<String, Object> customer = new HashMap<String, Object>();
                customer.put("name", user.getFullName() != null ? user.getFullName() : user.getUsername());
                customer.put("email", user.getEmail());
                customer.put("totalSpent", String.format("%.2f", entry.getValue()));
                customer.put("orderCount", customerOrderCount.get(entry.getKey()));
                topCustomers.add(customer);
            }
        }
        
        // Sort by total spent and take top 5
        topCustomers.sort((a, b) -> {
            double spentA = Double.parseDouble((String) a.get("totalSpent"));
            double spentB = Double.parseDouble((String) b.get("totalSpent"));
            return Double.compare(spentB, spentA);
        });
        
        if (topCustomers.size() > 5) {
            topCustomers = topCustomers.subList(0, 5);
        }
        
        report.put("newCustomers", newCustomers);
        report.put("totalCustomers", totalCustomers);
        report.put("repeatRate", String.format("%.1f", repeatRate));
        report.put("avgCustomerValue", String.format("%.2f", avgCustomerValue));
        report.put("customerGrowth", customerGrowth);
        report.put("topCustomers", topCustomers);
        
        out.write(gson.toJson(report));
    }
    
    private void sendPaymentReceipts(PrintWriter out) {
        List<Map<String, Object>> receipts = new ArrayList<Map<String, Object>>();
        
        // Get completed orders as payment receipts
        List<Order> allOrders = orderDAO.getAllOrders();
        int receiptCount = 1;
        
        for (Order order : allOrders) {
            if (order.getStatus() == OrderStatus.DELIVERED || order.getStatus() == OrderStatus.SHIPPED) {
                Map<String, Object> receipt = new HashMap<String, Object>();
                receipt.put("id", receiptCount++);
                receipt.put("orderId", order.getId());
                
                if (order.getUser() != null) {
                    receipt.put("customerName", order.getUser().getFullName() != null ? 
                        order.getUser().getFullName() : order.getUser().getUsername());
                } else {
                    receipt.put("customerName", "Guest Customer");
                }
                
                receipt.put("amount", order.getTotalAmount() != null ? 
                    String.format("%.2f", order.getTotalAmount().doubleValue()) : "0.00");
                receipt.put("date", order.getOrderDate());
                receipt.put("status", order.getStatus().name());
                receipts.add(receipt);
            }
        }
        
        out.write(gson.toJson(receipts));
    }
    
    private void exportSalesReport(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String month = request.getParameter("month");
        
        // Generate CSV content
        StringBuilder csv = new StringBuilder();
        csv.append("Sales Report");
        if (month != null && !month.isEmpty()) {
            csv.append(" - ").append(month).append("\n");
        }
        csv.append("\nDate,Revenue\n");
        
        // Get sales data
        List<Order> allOrders = orderDAO.getAllOrders();
        SimpleDateFormat dayFormat = new SimpleDateFormat("yyyy-MM-dd");
        Map<String, Double> dailySales = new HashMap<String, Double>();
        
        for (Order order : allOrders) {
            if (month != null && !month.isEmpty()) {
                SimpleDateFormat monthFormat = new SimpleDateFormat("yyyy-MM");
                String orderMonth = monthFormat.format(order.getOrderDate());
                if (!orderMonth.equals(month)) {
                    continue;
                }
            }
            
            String orderDay = dayFormat.format(order.getOrderDate());
            double dayTotal = dailySales.getOrDefault(orderDay, 0.0);
            if (order.getTotalAmount() != null) {
                dayTotal += order.getTotalAmount().doubleValue();
            }
            dailySales.put(orderDay, dayTotal);
        }
        
        // Add daily sales to CSV
        for (Map.Entry<String, Double> entry : dailySales.entrySet()) {
            csv.append(entry.getKey()).append(",").append(entry.getValue()).append("\n");
        }
        
        // Set response headers for download
        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=\"sales_report.csv\"");
        response.getWriter().write(csv.toString());
    }
    
    private void exportInventoryReport(HttpServletResponse response) throws Exception {
        // Generate CSV content
        StringBuilder csv = new StringBuilder();
        csv.append("Inventory Report\n\n");
        csv.append("Product Name,Category,Stock,Price,Total Value\n");
        
        List<Product> allProducts = productDAO.getAllProducts();
        
        for (Product product : allProducts) {
            csv.append("\"").append(product.getName()).append("\",");
            csv.append(product.getCategory() != null ? product.getCategory().getName() : "Uncategorized").append(",");
            csv.append(product.getQuantity()).append(",");
            csv.append(product.getPrice() != null ? product.getPrice().toString() : "0.00").append(",");
            
            double totalValue = 0.0;
            if (product.getPrice() != null && product.getQuantity() != null) {
                totalValue = product.getPrice().doubleValue() * product.getQuantity();
            }
            csv.append(totalValue).append("\n");
        }
        
        // Set response headers for download
        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=\"inventory_report.csv\"");
        response.getWriter().write(csv.toString());
    }
    
    private void exportCustomerReport(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String month = request.getParameter("month");
        
        // Generate CSV content
        StringBuilder csv = new StringBuilder();
        csv.append("Customer Report");
        if (month != null && !month.isEmpty()) {
            csv.append(" - ").append(month).append("\n");
        }
        csv.append("\nCustomer Name,Email,Total Orders,Total Spent,Join Date\n");
        
        List<User> allUsers = userDAO.getRecentUsers(1000);
        List<Order> allOrders = orderDAO.getAllOrders();
        
        // Calculate customer metrics
        Map<Long, Integer> customerOrderCount = new HashMap<Long, Integer>();
        Map<Long, Double> customerSpending = new HashMap<Long, Double>();
        
        for (Order order : allOrders) {
            if (order.getUser() != null) {
                Long userId = order.getUser().getId();
                customerOrderCount.put(userId, customerOrderCount.getOrDefault(userId, 0) + 1);
                
                double spent = customerSpending.getOrDefault(userId, 0.0);
                if (order.getTotalAmount() != null) {
                    spent += order.getTotalAmount().doubleValue();
                }
                customerSpending.put(userId, spent);
            }
        }
        
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        
        for (User user : allUsers) {
            csv.append("\"").append(user.getFullName() != null ? user.getFullName() : user.getUsername()).append("\",");
            csv.append(user.getEmail()).append(",");
            csv.append(customerOrderCount.getOrDefault(user.getId(), 0)).append(",");
            csv.append(customerSpending.getOrDefault(user.getId(), 0.0)).append(",");
            csv.append(dateFormat.format(user.getCreatedAt())).append("\n");
        }
        
        // Set response headers for download
        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=\"customer_report.csv\"");
        response.getWriter().write(csv.toString());
    }
    
    private void printReceipt(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String receiptId = request.getParameter("id");
        
        if (receiptId == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("Receipt ID required");
            return;
        }
        
        // Generate HTML receipt for printing
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>");
        html.append("<html>");
        html.append("<head>");
        html.append("<title>Payment Receipt</title>");
        html.append("<style>");
        html.append("body { font-family: Arial, sans-serif; margin: 20px; }");
        html.append(".receipt { border: 1px solid #000; padding: 20px; max-width: 400px; }");
        html.append(".header { text-align: center; margin-bottom: 20px; }");
        html.append(".details { margin-bottom: 20px; }");
        html.append(".footer { text-align: center; margin-top: 20px; font-size: 12px; }");
        html.append("</style>");
        html.append("</head>");
        html.append("<body>");
        html.append("<div class=\"receipt\">");
        html.append("<div class=\"header\">");
        html.append("<h2>SMART TECH</h2>");
        html.append("<p>Payment Receipt</p>");
        html.append("</div>");
        html.append("<div class=\"details\">");
        html.append("<p><strong>Receipt ID:</strong> RC-").append(receiptId).append("</p>");
        html.append("<p><strong>Date:</strong> ").append(new Date()).append("</p>");
        html.append("<p><strong>Status:</strong> PAID</p>");
        html.append("</div>");
        html.append("<div class=\"footer\">");
        html.append("<p>Thank you for your business!</p>");
        html.append("</div>");
        html.append("</div>");
        html.append("<script>window.print();</script>");
        html.append("</body>");
        html.append("</html>");
        
        response.setContentType("text/html");
        response.getWriter().write(html.toString());
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