package controller;

import dao.CartDAO;
import dao.ProductDAO;
import hibernate.CartItem;
import hibernate.User;
import hibernate.HibernateUtil;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Enumeration;
import org.hibernate.Session;

@WebServlet("/cart")
public class CartServlet extends HttpServlet {

    private CartDAO cartDAO;
    private ProductDAO productDAO;
    private Gson gson;

    @Override
    public void init() throws ServletException {
        try {
            cartDAO = new CartDAO();
            productDAO = new ProductDAO();
            gson = new GsonBuilder().setPrettyPrinting().create();
            System.out.println("✅ CartServlet initialized successfully");
        } catch (Exception e) {
            System.err.println("❌ Failed to initialize CartServlet: " + e.getMessage());
            e.printStackTrace();
            throw new ServletException("Failed to initialize CartServlet", e);
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        System.out.println("📥 CartServlet GET request received");

        try {
            HttpSession session = request.getSession(false);
            if (session == null) {
                System.out.println("❌ No session found");
                sendErrorResponse(response, "User not logged in", HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }

            User user = (User) session.getAttribute("user");
            if (user == null) {
                System.out.println("❌ No user in session");
                sendErrorResponse(response, "User not logged in", HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }

            System.out.println("✅ User authenticated: " + user.getUsername() + " (ID: " + user.getId() + ")");

            String action = request.getParameter("action");
            String format = request.getParameter("format");

            System.out.println("🔧 GET Parameters - Action: " + action + ", Format: " + format);

            if ("count".equals(action) && "json".equals(format)) {
                getCartCount(user, response);
            } else if ("json".equals(format)) {
                getCartItems(user, response);
            } else {
                // Redirect to cart page for non-JSON requests
                response.sendRedirect("cart.html");
            }
        } catch (Exception e) {
            System.err.println("❌ Error in CartServlet doGet: " + e.getMessage());
            e.printStackTrace();
            sendErrorResponse(response, "Internal server error: " + e.getMessage(),
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        System.out.println("📥 CartServlet POST request received");

        try {
            HttpSession session = request.getSession(false);
            if (session == null) {
                System.out.println("❌ No session found in POST");
                sendErrorResponse(response, "User not logged in", HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }

            User user = (User) session.getAttribute("user");
            if (user == null) {
                System.out.println("❌ No user in session in POST");
                sendErrorResponse(response, "User not logged in", HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }

            System.out.println("✅ User authenticated: " + user.getUsername() + " (ID: " + user.getId() + ")");

            String action = request.getParameter("action");
            System.out.println("🔧 POST Action parameter: " + action);

            if (action == null || action.trim().isEmpty()) {
                System.err.println("❌ Action parameter is missing or empty");
                sendErrorResponse(response, "Action parameter required", HttpServletResponse.SC_BAD_REQUEST);
                return;
            }

            switch (action) {
                case "add":
                    addToCart(user, request, response);
                    break;
                case "update":
                    updateCartItem(user, request, response);
                    break;
                case "remove":
                    removeFromCart(user, request, response);
                    break;
                case "clear":
                    clearCart(user, response);
                    break;
                default:
                    System.err.println("❌ Invalid action: " + action);
                    sendErrorResponse(response, "Invalid action: " + action, HttpServletResponse.SC_BAD_REQUEST);
            }
        } catch (Exception e) {
            System.err.println("❌ Error in CartServlet doPost: " + e.getMessage());
            e.printStackTrace();
            sendErrorResponse(response, "Internal server error: " + e.getMessage(),
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private void getCartItems(User user, HttpServletResponse response) throws IOException {
        Session hibernateSession = null;
        try {
            System.out.println("🛒 Getting cart items for user ID: " + user.getId());

            // Open a Hibernate session to avoid LazyInitializationException
            hibernateSession = HibernateUtil.getSessionFactory().openSession();

            List<CartItem> cartItems = cartDAO.getCartItemsByUser(user.getId());
            List<Map<String, Object>> cartData = new ArrayList<>();

            if (cartItems != null) {
                System.out.println("📦 Found " + cartItems.size() + " cart items");
                for (CartItem item : cartItems) {
                    // Initialize lazy-loaded objects within the Hibernate session
                    Map<String, Object> itemData = new HashMap<>();
                    itemData.put("id", item.getId());

                    // Access the product within the Hibernate session context
                    if (item.getProduct() != null) {
                        itemData.put("productId", item.getProduct().getId());
                        itemData.put("productName", item.getProduct().getName());
                        itemData.put("price", item.getProduct().getPrice());
                        itemData.put("productImage", item.getProduct().getImageUrl());
                    } else {
                        itemData.put("productId", 0);
                        itemData.put("productName", "Unknown Product");
                        itemData.put("price", 0.0);
                        itemData.put("productImage", "https://via.placeholder.com/100?text=No+Image");
                    }

                    itemData.put("quantity", item.getQuantity());
                    itemData.put("subtotal", item.getSubtotal());
                    cartData.add(itemData);

                    System.out.println("  - " + (item.getProduct() != null ? item.getProduct().getName() : "Unknown") + " x" + item.getQuantity() + " (CartItem ID: " + item.getId() + ")");
                }
            } else {
                System.out.println("📦 No cart items found (cartItems is null)");
            }

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("success", true);
            responseData.put("items", cartData);
            responseData.put("totalItems", cartData.size());
            responseData.put("cartTotal", cartDAO.getCartTotal(user.getId()));

            System.out.println("✅ Returning " + cartData.size() + " cart items to frontend");
            sendJsonResponse(response, responseData);

        } catch (Exception e) {
            System.err.println("❌ Error getting cart items: " + e.getMessage());
            e.printStackTrace();
            sendErrorResponse(response, "Error retrieving cart items: " + e.getMessage(),
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        } finally {
            // Close Hibernate session
            if (hibernateSession != null && hibernateSession.isOpen()) {
                hibernateSession.close();
            }
        }
    }

    private void checkAuthentication(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(false);
        if (session == null) {
            System.out.println("❌ No session found");
            sendErrorResponse(response, "User not logged in", HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        User user = (User) session.getAttribute("user");
        if (user == null) {
            System.out.println("❌ No user in session");
            // Debug session contents
            System.out.println("🔍 Session ID: " + (session != null ? session.getId() : "null"));
            if (session != null) {
                java.util.Enumeration<String> attributeNames = session.getAttributeNames();
                while (attributeNames.hasMoreElements()) {
                    String name = attributeNames.nextElement();
                    System.out.println("🔍 Session attribute: " + name + " = " + session.getAttribute(name));
                }
            }
            sendErrorResponse(response, "User not logged in", HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
    }

    private void getCartCount(User user, HttpServletResponse response) throws IOException {
        Session hibernateSession = null;
        try {
            System.out.println("🔢 Getting cart count for user ID: " + user.getId());
            int count = cartDAO.getCartItemCount(user.getId());

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("success", true);
            responseData.put("count", count);

            System.out.println("✅ Cart count: " + count);
            sendJsonResponse(response, responseData);

        } catch (Exception e) {
            System.err.println("❌ Error getting cart count: " + e.getMessage());
            e.printStackTrace();
            sendErrorResponse(response, "Error getting cart count: " + e.getMessage(),
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        } finally {
            if (hibernateSession != null && hibernateSession.isOpen()) {
                hibernateSession.close();
            }
        }
    }

    private void addToCart(User user, HttpServletRequest request, HttpServletResponse response) throws IOException {
        Session hibernateSession = null;
        try {
            String productIdParam = request.getParameter("productId");
            String quantityParam = request.getParameter("quantity");

            System.out.println("➕ Add to cart - User: " + user.getId()
                    + ", Product ID: " + productIdParam
                    + ", Quantity: " + quantityParam);

            if (productIdParam == null || productIdParam.trim().isEmpty()) {
                System.err.println("❌ Product ID is missing");
                sendErrorResponse(response, "Product ID is required", HttpServletResponse.SC_BAD_REQUEST);
                return;
            }

            if (quantityParam == null || quantityParam.trim().isEmpty()) {
                System.err.println("❌ Quantity is missing");
                sendErrorResponse(response, "Quantity is required", HttpServletResponse.SC_BAD_REQUEST);
                return;
            }

            Long productId;
            Integer quantity;

            try {
                productId = Long.parseLong(productIdParam);
                quantity = Integer.parseInt(quantityParam);
            } catch (NumberFormatException e) {
                System.err.println("❌ Invalid number format - Product ID: " + productIdParam + ", Quantity: " + quantityParam);
                sendErrorResponse(response, "Invalid product ID or quantity format", HttpServletResponse.SC_BAD_REQUEST);
                return;
            }

            // Validate product exists and has sufficient stock
            hibernateSession = HibernateUtil.getSessionFactory().openSession();
            hibernate.Product product = productDAO.getProductById(productId);
            if (product == null) {
                System.err.println("❌ Product not found with ID: " + productId);
                sendErrorResponse(response, "Product not found", HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            System.out.println("📦 Product found: " + product.getName()
                    + ", Stock: " + product.getQuantity()
                    + ", Requested: " + quantity);

            if (!product.hasSufficientStock(quantity)) {
                System.err.println("❌ Insufficient stock - Available: " + product.getQuantity() + ", Requested: " + quantity);
                sendErrorResponse(response, "Insufficient stock. Available: " + product.getQuantity(),
                        HttpServletResponse.SC_BAD_REQUEST);
                return;
            }

            boolean success = cartDAO.addToCart(user.getId(), productId, quantity);

            if (success) {
                Map<String, Object> responseData = new HashMap<>();
                responseData.put("success", true);
                responseData.put("message", "Product added to cart successfully");
                responseData.put("cartCount", cartDAO.getCartItemCount(user.getId()));
                System.out.println("✅ Successfully added product to cart");
                sendJsonResponse(response, responseData);
            } else {
                System.err.println("❌ Failed to add product to cart (DAO returned false)");
                sendErrorResponse(response, "Failed to add product to cart", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }

        } catch (NumberFormatException e) {
            System.err.println("❌ Invalid number format: " + e.getMessage());
            sendErrorResponse(response, "Invalid product ID or quantity format", HttpServletResponse.SC_BAD_REQUEST);
        } catch (Exception e) {
            System.err.println("❌ Error adding to cart: " + e.getMessage());
            e.printStackTrace();
            sendErrorResponse(response, "Failed to add product to cart: " + e.getMessage(),
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        } finally {
            if (hibernateSession != null && hibernateSession.isOpen()) {
                hibernateSession.close();
            }
        }
    }

    private void updateCartItem(User user, HttpServletRequest request, HttpServletResponse response) throws IOException {
        Session hibernateSession = null;
        try {
            String cartItemIdParam = request.getParameter("cartItemId");
            String quantityParam = request.getParameter("quantity");

            System.out.println("✏️ Update cart item - User: " + user.getId()
                    + ", Cart Item ID: " + cartItemIdParam
                    + ", Quantity: " + quantityParam);

            if (cartItemIdParam == null || cartItemIdParam.trim().isEmpty()) {
                sendErrorResponse(response, "Cart item ID is required", HttpServletResponse.SC_BAD_REQUEST);
                return;
            }

            if (quantityParam == null || quantityParam.trim().isEmpty()) {
                sendErrorResponse(response, "Quantity is required", HttpServletResponse.SC_BAD_REQUEST);
                return;
            }

            Long cartItemId = Long.parseLong(cartItemIdParam);
            Integer quantity = Integer.parseInt(quantityParam);

            boolean success = cartDAO.updateCartItemQuantity(cartItemId, quantity);

            if (success) {
                Map<String, Object> responseData = new HashMap<>();
                responseData.put("success", true);
                responseData.put("message", "Cart updated successfully");
                System.out.println("✅ Successfully updated cart item");
                sendJsonResponse(response, responseData);
            } else {
                System.err.println("❌ Failed to update cart item (DAO returned false)");
                sendErrorResponse(response, "Failed to update cart item", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }

        } catch (NumberFormatException e) {
            System.err.println("❌ Invalid number format: " + e.getMessage());
            sendErrorResponse(response, "Invalid cart item ID or quantity format", HttpServletResponse.SC_BAD_REQUEST);
        } catch (Exception e) {
            System.err.println("❌ Error updating cart item: " + e.getMessage());
            e.printStackTrace();
            sendErrorResponse(response, "Failed to update cart item: " + e.getMessage(),
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        } finally {
            if (hibernateSession != null && hibernateSession.isOpen()) {
                hibernateSession.close();
            }
        }
    }

    private void removeFromCart(User user, HttpServletRequest request, HttpServletResponse response) throws IOException {
        Session hibernateSession = null;
        try {
            String cartItemIdParam = request.getParameter("cartItemId");

            System.out.println("🗑️ Remove from cart - User: " + user.getId()
                    + ", Cart Item ID: " + cartItemIdParam);

            if (cartItemIdParam == null || cartItemIdParam.trim().isEmpty()) {
                sendErrorResponse(response, "Cart item ID is required", HttpServletResponse.SC_BAD_REQUEST);
                return;
            }

            Long cartItemId = Long.parseLong(cartItemIdParam);

            boolean success = cartDAO.removeFromCart(cartItemId);

            if (success) {
                Map<String, Object> responseData = new HashMap<>();
                responseData.put("success", true);
                responseData.put("message", "Item removed from cart");
                System.out.println("✅ Successfully removed item from cart");
                sendJsonResponse(response, responseData);
            } else {
                System.err.println("❌ Failed to remove item from cart (DAO returned false)");
                sendErrorResponse(response, "Failed to remove item from cart", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }

        } catch (NumberFormatException e) {
            System.err.println("❌ Invalid number format: " + e.getMessage());
            sendErrorResponse(response, "Invalid cart item ID format", HttpServletResponse.SC_BAD_REQUEST);
        } catch (Exception e) {
            System.err.println("❌ Error removing from cart: " + e.getMessage());
            e.printStackTrace();
            sendErrorResponse(response, "Failed to remove item from cart: " + e.getMessage(),
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        } finally {
            if (hibernateSession != null && hibernateSession.isOpen()) {
                hibernateSession.close();
            }
        }
    }

    private void clearCart(User user, HttpServletResponse response) throws IOException {
        Session hibernateSession = null;
        try {
            System.out.println("🧹 Clear cart - User: " + user.getId());

            boolean success = cartDAO.clearUserCart(user.getId());

            if (success) {
                Map<String, Object> responseData = new HashMap<>();
                responseData.put("success", true);
                responseData.put("message", "Cart cleared successfully");
                System.out.println("✅ Successfully cleared cart");
                sendJsonResponse(response, responseData);
            } else {
                System.err.println("❌ Failed to clear cart (DAO returned false)");
                sendErrorResponse(response, "Failed to clear cart", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }

        } catch (Exception e) {
            System.err.println("❌ Error clearing cart: " + e.getMessage());
            e.printStackTrace();
            sendErrorResponse(response, "Failed to clear cart: " + e.getMessage(),
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        } finally {
            if (hibernateSession != null && hibernateSession.isOpen()) {
                hibernateSession.close();
            }
        }
    }

    private void debugSession(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        System.out.println("🐛 DEBUG SESSION:");
        System.out.println("  - Session exists: " + (session != null));
        if (session != null) {
            System.out.println("  - Session ID: " + session.getId());
            System.out.println("  - Creation time: " + new Date(session.getCreationTime()));
            System.out.println("  - Last accessed: " + new Date(session.getLastAccessedTime()));

            java.util.Enumeration<String> attributeNames = session.getAttributeNames();
            while (attributeNames.hasMoreElements()) {
                String name = attributeNames.nextElement();
                Object value = session.getAttribute(name);
                System.out.println("  - Attribute: " + name + " = "
                        + (value instanceof User ? ((User) value).getUsername() : value));
            }
        }
    }

    private void sendJsonResponse(HttpServletResponse response, Object data) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Access-Control-Allow-Origin", "*");
        PrintWriter out = response.getWriter();
        out.print(gson.toJson(data));
        out.flush();

        System.out.println("📤 Sent JSON response: " + gson.toJson(data));
    }

    private void sendErrorResponse(HttpServletResponse response, String message, int statusCode) throws IOException {
        response.setStatus(statusCode);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Access-Control-Allow-Origin", "*");

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("error", message);

        PrintWriter out = response.getWriter();
        String jsonResponse = gson.toJson(errorResponse);
        out.print(jsonResponse);
        out.flush();

        System.err.println("📤 Sent ERROR response (" + statusCode + "): " + jsonResponse);
    }

    @Override
    public void destroy() {
        System.out.println("🛑 CartServlet destroyed");
        super.destroy();
    }
}
