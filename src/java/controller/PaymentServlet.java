package controller;

import dao.OrderDAO;
import hibernate.Order;
import hibernate.OrderStatus;
import com.google.gson.Gson;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Enumeration;

@WebServlet("/payment/*")
public class PaymentServlet extends HttpServlet {
    private OrderDAO orderDAO;
    private Gson gson;
    
    // Your actual merchant ID
    private final String MERCHANT_ID = "1232299";

    @Override
    public void init() throws ServletException {
        orderDAO = new OrderDAO();
        gson = new Gson();
        System.out.println("✅ PaymentServlet initialized with Merchant ID: " + MERCHANT_ID);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        String pathInfo = request.getPathInfo();
        PrintWriter out = response.getWriter();
        
        System.out.println("📥 PaymentServlet POST request: " + pathInfo);
        
        if (pathInfo == null) {
            System.err.println("❌ No path info in payment request");
            sendErrorResponse(response, "Invalid payment endpoint");
            return;
        }
        
        try {
            if ("/payhere/notify".equals(pathInfo)) {
                handlePayHereNotification(request, response);
            } else if ("/finalize".equals(pathInfo)) {
                finalizePayment(request, response);
            } else {
                System.err.println("❌ Unknown payment endpoint: " + pathInfo);
                sendErrorResponse(response, "Invalid payment endpoint: " + pathInfo);
            }
        } catch (Exception e) {
            System.err.println("❌ Payment processing error: " + e.getMessage());
            e.printStackTrace();
            sendErrorResponse(response, "Payment processing error: " + e.getMessage());
        }
    }

    private void handlePayHereNotification(HttpServletRequest request, HttpServletResponse response) 
            throws IOException {
        PrintWriter out = response.getWriter();
        
        try {
            // Log all parameters for debugging
            System.out.println("🔔 PayHere Notification Received - All Parameters:");
            Enumeration<String> paramNames = request.getParameterNames();
            while (paramNames.hasMoreElements()) {
                String paramName = paramNames.nextElement();
                String paramValue = request.getParameter(paramName);
                System.out.println("   " + paramName + ": " + paramValue);
            }
            
            String merchantId = request.getParameter("merchant_id");
            String orderId = request.getParameter("order_id");
            String paymentStatus = request.getParameter("status_code");
            String amount = request.getParameter("amount");
            String transactionId = request.getParameter("payment_id");
            String method = request.getParameter("method");
            String statusMessage = request.getParameter("status_message");
            
            System.out.println("🔔 PayHere Notification Summary:");
            System.out.println("   Merchant ID: " + merchantId);
            System.out.println("   Order ID: " + orderId);
            System.out.println("   Payment Status: " + paymentStatus);
            System.out.println("   Amount: " + amount);
            System.out.println("   Transaction ID: " + transactionId);
            System.out.println("   Method: " + method);
            System.out.println("   Status Message: " + statusMessage);
            
            // Verify merchant ID
            if (!MERCHANT_ID.equals(merchantId)) {
                System.err.println("❌ Merchant ID mismatch. Expected: " + MERCHANT_ID + ", Got: " + merchantId);
                out.write("ERROR");
                return;
            }
            
            if (orderId == null || orderId.trim().isEmpty()) {
                System.err.println("❌ Order ID is missing in notification");
                out.write("ERROR");
                return;
            }
            
            // Handle different payment statuses
            if ("2".equals(paymentStatus)) { 
                // 2 = success in PayHere
                System.out.println("✅ Payment SUCCESS for order: " + orderId);
                
                Long orderIdLong = Long.parseLong(orderId);
                boolean success = orderDAO.updateOrderStatus(orderIdLong, OrderStatus.PROCESSING);
                
                if (success) {
                    System.out.println("✅ Order " + orderId + " status updated to PROCESSING");
                    out.write("OK");
                } else {
                    System.err.println("❌ Failed to update order status for: " + orderId);
                    out.write("ERROR");
                }
                
            } else if ("0".equals(paymentStatus) || "-1".equals(paymentStatus) || "-2".equals(paymentStatus)) {
                // Payment failed, cancelled, or pending
                System.out.println("❌ Payment FAILED/CANCELLED for order: " + orderId + ", Status: " + paymentStatus);
                
                Long orderIdLong = Long.parseLong(orderId);
                boolean success = orderDAO.updateOrderStatus(orderIdLong, OrderStatus.CANCELLED);
                
                if (success) {
                    System.out.println("✅ Order " + orderId + " status updated to CANCELLED");
                }
                out.write("OK"); // Still return OK to PayHere
                
            } else {
                // Unknown status
                System.out.println("⚠️ Unknown payment status for order: " + orderId + ", Status: " + paymentStatus);
                out.write("OK");
            }
            
        } catch (NumberFormatException e) {
            System.err.println("❌ Invalid order ID format in notification");
            e.printStackTrace();
            out.write("ERROR");
        } catch (Exception e) {
            System.err.println("❌ Error processing PayHere notification: " + e.getMessage());
            e.printStackTrace();
            out.write("ERROR");
        }
    }

    private void finalizePayment(HttpServletRequest request, HttpServletResponse response) 
            throws IOException {
        try {
            String orderId = request.getParameter("orderId");
            String paymentMethod = request.getParameter("paymentMethod");
            String paymentId = request.getParameter("paymentId");
            
            System.out.println("💰 Finalizing payment - Order ID: " + orderId + 
                             ", Method: " + paymentMethod + ", Payment ID: " + paymentId);
            
            if (orderId == null || orderId.trim().isEmpty()) {
                System.err.println("❌ Order ID is required for payment finalization");
                sendErrorResponse(response, "Order ID is required");
                return;
            }
            
            Long orderIdLong = Long.parseLong(orderId);
            
            // Update order status based on payment method
            OrderStatus status;
            if ("cod".equals(paymentMethod)) {
                status = OrderStatus.PENDING; // For Cash on Delivery
                System.out.println("💰 COD order finalized: " + orderId);
            } else {
                status = OrderStatus.PROCESSING; // For online payments
                System.out.println("💰 Online payment finalized: " + orderId);
            }
            
            boolean success = orderDAO.updateOrderStatus(orderIdLong, status);
            
            Map<String, Object> responseData = new HashMap<>();
            if (success) {
                responseData.put("success", true);
                responseData.put("message", "Payment finalized successfully");
                responseData.put("orderId", orderIdLong);
                responseData.put("status", status.toString());
                System.out.println("✅ Payment finalized successfully for order: " + orderId);
            } else {
                responseData.put("success", false);
                responseData.put("error", "Failed to finalize payment");
                System.err.println("❌ Failed to finalize payment for order: " + orderId);
            }
            
            response.getWriter().write(gson.toJson(responseData));
            
        } catch (NumberFormatException e) {
            System.err.println("❌ Invalid order ID format in finalize: " + request.getParameter("orderId"));
            sendErrorResponse(response, "Invalid order ID format");
        } catch (Exception e) {
            System.err.println("❌ Error finalizing payment: " + e.getMessage());
            e.printStackTrace();
            sendErrorResponse(response, "Error finalizing payment: " + e.getMessage());
        }
    }

    private void sendErrorResponse(HttpServletResponse response, String message) throws IOException {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("error", message);
        
        String jsonResponse = gson.toJson(errorResponse);
        response.getWriter().write(jsonResponse);
        
        System.err.println("📤 Sent ERROR response: " + jsonResponse);
    }

    @Override
    public void destroy() {
        System.out.println("🛑 PaymentServlet destroyed");
        super.destroy();
    }
}