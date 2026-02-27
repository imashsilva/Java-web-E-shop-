package controller;

import dao.OrderDAO;
import dao.CartDAO;
import dao.UserDAO;
import hibernate.Order;
import hibernate.OrderStatus;
import hibernate.User;
import hibernate.CartItem;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

@WebServlet("/checkout/*")
public class CheckoutServlet extends HttpServlet {

    private OrderDAO orderDAO;
    private CartDAO cartDAO;
    private UserDAO userDAO;
    private Gson gson;

    @Override
    public void init() throws ServletException {
        orderDAO = new OrderDAO();
        cartDAO = new CartDAO();
        userDAO = new UserDAO();
        gson = new Gson();
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String pathInfo = request.getPathInfo();
        PrintWriter out = response.getWriter();

        try {
            if (pathInfo == null || "/createOrder".equals(pathInfo)) {
                createOrder(request, response);
            } else if ("/payhere/initiate".equals(pathInfo)) {
                initiatePayHerePayment(request, response);
            } else if ("/confirm".equals(pathInfo)) {
                confirmOrder(request, response);
            } else {
                sendErrorResponse(response, "Invalid endpoint");
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendErrorResponse(response, "Server error: " + e.getMessage());
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String pathInfo = request.getPathInfo();
        PrintWriter out = response.getWriter();

        try {
            if (pathInfo == null || "/summary".equals(pathInfo)) {
                getCheckoutSummary(request, response);
            } else {
                sendErrorResponse(response, "Invalid endpoint");
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendErrorResponse(response, "Server error: " + e.getMessage());
        }
    }

    private void createOrder(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            sendErrorResponse(response, "User not authenticated");
            return;
        }

        Long userId = (Long) session.getAttribute("userId");
        PrintWriter out = response.getWriter();

        try {
            String shippingAddress = request.getParameter("shippingAddress");
            String paymentMethod = request.getParameter("paymentMethod");
            String totalAmountStr = request.getParameter("totalAmount");

            if (shippingAddress == null || shippingAddress.trim().isEmpty()) {
                sendErrorResponse(response, "Shipping address is required");
                return;
            }

            BigDecimal totalAmount;
            try {
                totalAmount = new BigDecimal(totalAmountStr);
            } catch (NumberFormatException e) {
                sendErrorResponse(response, "Invalid total amount");
                return;
            }

            // Create order from cart
            Order order = orderDAO.createOrderFromCart(userId, shippingAddress, totalAmount);

            if (order != null) {
                Map<String, Object> responseData = new HashMap<>();
                responseData.put("success", true);
                responseData.put("orderId", order.getId());
                responseData.put("message", "Order created successfully");

                // Store order ID in session for payment processing
                session.setAttribute("currentOrderId", order.getId());

                out.write(gson.toJson(responseData));
            } else {
                sendErrorResponse(response, "Failed to create order");
            }

        } catch (Exception e) {
            e.printStackTrace();
            sendErrorResponse(response, "Error creating order: " + e.getMessage());
        }
    }

    private void initiatePayHerePayment(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            sendErrorResponse(response, "User not authenticated");
            return;
        }

        Long orderId = (Long) session.getAttribute("currentOrderId");
        if (orderId == null) {
            sendErrorResponse(response, "No active order found");
            return;
        }

        try {
            // Use a method that properly initializes the order with user data
            Order order = orderDAO.getOrderWithUserData(orderId);
            if (order == null) {
                sendErrorResponse(response, "Order not found");
                return;
            }

            User user = order.getUser();

            // Prepare PayHere payment data
            Map<String, Object> payHereData = new HashMap<>();
            payHereData.put("merchant_id", "1232299"); // Your PayHere merchant ID
            payHereData.put("return_url", getBaseUrl(request) + "/checkout/payhere/return");
            payHereData.put("cancel_url", getBaseUrl(request) + "/checkout/cancel");
            payHereData.put("notify_url", getBaseUrl(request) + "/payment/payhere/notify");
            payHereData.put("order_id", order.getId().toString());
            payHereData.put("items", "Order #" + order.getId());
            payHereData.put("amount", order.getTotalAmount().toString());
            payHereData.put("currency", "LKR");
            payHereData.put("first_name", getFirstName(user));
            payHereData.put("last_name", getLastName(user));
            payHereData.put("email", user.getEmail() != null ? user.getEmail() : "customer@example.com");
            payHereData.put("phone", user.getPhone() != null ? user.getPhone() : "94771234567");
            payHereData.put("address", order.getShippingAddress());
            payHereData.put("city", "Colombo");
            payHereData.put("country", "Sri Lanka");

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("success", true);
            responseData.put("payhereData", payHereData);
            responseData.put("paymentUrl", "https://sandbox.payhere.lk/pay/checkout"); // Sandbox URL

            response.getWriter().write(gson.toJson(responseData));

        } catch (Exception e) {
            e.printStackTrace();
            sendErrorResponse(response, "Error initiating payment: " + e.getMessage());
        }
    }

    private String getFirstName(User user) {
        if (user.getFullName() != null && !user.getFullName().trim().isEmpty()) {
            String[] names = user.getFullName().split(" ");
            return names[0];
        }
        return "Customer";
    }

    private String getLastName(User user) {
        if (user.getFullName() != null && !user.getFullName().trim().isEmpty()) {
            String[] names = user.getFullName().split(" ");
            return names.length > 1 ? names[1] : "";
        }
        return "";
    }

    private void confirmOrder(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            sendErrorResponse(response, "User not authenticated");
            return;
        }

        try {
            String orderIdStr = request.getParameter("orderId");

            if (orderIdStr == null || orderIdStr.trim().isEmpty()) {
                sendErrorResponse(response, "Order ID is required");
                return;
            }

            Long orderId = Long.parseLong(orderIdStr);
            Long userId = (Long) session.getAttribute("userId");

            // Update order status to PROCESSING
            boolean success = orderDAO.updateOrderStatus(orderId, OrderStatus.PROCESSING);

            Map<String, Object> responseData = new HashMap<>();
            if (success) {
                responseData.put("success", true);
                responseData.put("message", "Order confirmed successfully");
                responseData.put("orderId", orderId);
                // Clear current order from session
                session.removeAttribute("currentOrderId");
            } else {
                responseData.put("success", false);
                responseData.put("error", "Failed to confirm order");
            }

            response.getWriter().write(gson.toJson(responseData));

        } catch (NumberFormatException e) {
            sendErrorResponse(response, "Invalid order ID");
        } catch (Exception e) {
            e.printStackTrace();
            sendErrorResponse(response, "Error confirming order: " + e.getMessage());
        }
    }

    private void getCheckoutSummary(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            sendErrorResponse(response, "User not authenticated");
            return;
        }

        User user = (User) session.getAttribute("user");
        Long userId = user.getId();

        System.out.println("📊 Loading checkout summary for user ID: " + userId);
        PrintWriter out = response.getWriter();

        try {
            List<CartItem> cartItems = cartDAO.getCartItemsByUser(userId);

            if (cartItems == null) {
                cartItems = new ArrayList<>();
            }

            // Convert to simple format for JSON
            List<Map<String, Object>> items = new ArrayList<>();
            BigDecimal subtotal = BigDecimal.ZERO;

            for (CartItem cartItem : cartItems) {
                Map<String, Object> itemData = new HashMap<>();
                itemData.put("id", cartItem.getId());
                itemData.put("cartItemId", cartItem.getId());
                itemData.put("productId", cartItem.getProduct().getId());
                itemData.put("productName", cartItem.getProduct().getName());
                itemData.put("price", cartItem.getProduct().getPrice());
                itemData.put("quantity", cartItem.getQuantity());
                itemData.put("imageUrl", cartItem.getProduct().getImageUrl());

                BigDecimal itemSubtotal = cartItem.getProduct().getPrice()
                        .multiply(BigDecimal.valueOf(cartItem.getQuantity()));
                itemData.put("subtotal", itemSubtotal);

                items.add(itemData);
                subtotal = subtotal.add(itemSubtotal);
            }

            // Calculate totals
            BigDecimal shipping = BigDecimal.valueOf(5.99);
            BigDecimal tax = subtotal.multiply(BigDecimal.valueOf(0.10));
            BigDecimal total = subtotal.add(shipping).add(tax);

            Map<String, Object> totals = new HashMap<>();
            totals.put("subtotal", subtotal);
            totals.put("shipping", shipping);
            totals.put("tax", tax);
            totals.put("total", total);

            Map<String, Object> summary = new HashMap<>();
            summary.put("items", items);
            summary.put("totals", totals);
            summary.put("itemCount", items.size());

            out.write(gson.toJson(summary));

        } catch (Exception e) {
            e.printStackTrace();
            throw new IOException("Error loading checkout summary: " + e.getMessage());
        }
    }

    private String getBaseUrl(HttpServletRequest request) {
        String scheme = request.getScheme();
        String serverName = request.getServerName();
        int serverPort = request.getServerPort();
        String contextPath = request.getContextPath();

        String baseUrl = scheme + "://" + serverName;
        if (("http".equals(scheme) && serverPort != 80) || ("https".equals(scheme) && serverPort != 443)) {
            baseUrl += ":" + serverPort;
        }
        baseUrl += contextPath;

        return baseUrl;
    }

    private void sendErrorResponse(HttpServletResponse response, String message) throws IOException {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("error", message);

        response.getWriter().write(gson.toJson(errorResponse));
    }
}
