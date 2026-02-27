package dao;

import hibernate.*;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.hibernate.criterion.Projections;
import org.hibernate.transform.Transformers;

public class OrderDAO {

    // Add this method to fix the LazyInitializationException
    public Order getOrderWithUserData(Long orderId) {
        Session session = null;
        try {
            session = HibernateUtil.getSessionFactory().openSession();
            
            // Use Criteria to fetch order with user eagerly
            Criteria criteria = session.createCriteria(Order.class);
            criteria.add(Restrictions.eq("id", orderId));
            criteria.setFetchMode("user", org.hibernate.FetchMode.JOIN); // Eagerly fetch user
            
            Order order = (Order) criteria.uniqueResult();
            
            // If using HQL instead (alternative approach)
            // String hql = "FROM Order o LEFT JOIN FETCH o.user WHERE o.id = :orderId";
            // Order order = (Order) session.createQuery(hql)
            //         .setParameter("orderId", orderId)
            //         .uniqueResult();
            
            // Force initialization of user data while session is open
            if (order != null && order.getUser() != null) {
                // Access user properties to force loading
                order.getUser().getUsername();
                order.getUser().getEmail();
                order.getUser().getFullName();
            }
            
            return order;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }

    // NEW METHOD: Get all orders with proper initialization to avoid LazyInitializationException
    public List<Order> getAllOrders() {
        Session session = null;
        try {
            session = HibernateUtil.getSessionFactory().openSession();
            Criteria criteria = session.createCriteria(Order.class);
            criteria.addOrder(org.hibernate.criterion.Order.desc("orderDate"));
            
            List<Order> orders = criteria.list();
            
            // Initialize relationships to avoid LazyInitializationException
            for (Order order : orders) {
                if (order.getUser() != null) {
                    order.getUser().getUsername(); // Initialize user
                }
                if (order.getOrderItems() != null) {
                    order.getOrderItems().size(); // Initialize order items
                }
            }
            
            return orders;
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<Order>();
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }

    // Your existing methods remain the same...
    public Order createOrderFromCart(Long userId, String shippingAddress, BigDecimal totalAmount) {
        Session session = null;
        Transaction transaction = null;
        
        try {
            session = HibernateUtil.getSessionFactory().openSession();
            transaction = session.beginTransaction();
            
            // Get user
            User user = (User) session.get(User.class, userId);
            if (user == null) {
                throw new Exception("User not found");
            }
            
            // Get cart items
            CartDAO cartDAO = new CartDAO();
            List<CartItem> cartItems = cartDAO.getCartItemsByUser(userId);
            
            if (cartItems == null || cartItems.isEmpty()) {
                throw new Exception("Cart is empty");
            }
            
            // Create order
            Order order = new Order();
            order.setUser(user);
            order.setShippingAddress(shippingAddress);
            order.setTotalAmount(totalAmount);
            order.setStatus(OrderStatus.PENDING);
            
            session.save(order);
            
            // Create order items from cart items and update stock
            for (CartItem cartItem : cartItems) {
                Product product = cartItem.getProduct();
                
                // Check stock availability
                if (!product.hasSufficientStock(cartItem.getQuantity())) {
                    throw new Exception("Insufficient stock for product: " + product.getName());
                }
                
                // Create order item
                OrderItem orderItem = new OrderItem();
                orderItem.setOrder(order);
                orderItem.setProduct(product);
                orderItem.setQuantity(cartItem.getQuantity());
                orderItem.setPrice(product.getPrice());
                
                session.save(orderItem);
                
                // Update product stock
                product.decreaseStock(cartItem.getQuantity());
                session.update(product);
            }
            
            // Clear cart after successful order creation
            cartDAO.clearUserCart(userId);
            
            transaction.commit();
            return order;
            
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            e.printStackTrace();
            return null;
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }

    //in
     public Long getTotalOrdersCount() {
        Session session = null;
        try {
            session = HibernateUtil.getSessionFactory().openSession();
            Criteria criteria = session.createCriteria(Order.class);
            criteria.setProjection(Projections.rowCount());
            return (Long) criteria.uniqueResult();
        } catch (Exception e) {
            e.printStackTrace();
            return 0L;
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }
    
    public Double getTotalRevenue() {
        Session session = null;
        try {
            session = HibernateUtil.getSessionFactory().openSession();
            
            // Using HQL for sum calculation
            String hql = "SELECT SUM(o.totalAmount) FROM Order o WHERE o.status = 'COMPLETED'";
            Double total = (Double) session.createQuery(hql).uniqueResult();
            return total != null ? total : 0.0;
        } catch (Exception e) {
            e.printStackTrace();
            return 0.0;
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }
    
    public Double getTodayRevenue() {
        Session session = null;
        try {
            session = HibernateUtil.getSessionFactory().openSession();
            
            // Create today's date range
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            Date todayStart = calendar.getTime();
            
            calendar.set(Calendar.HOUR_OF_DAY, 23);
            calendar.set(Calendar.MINUTE, 59);
            calendar.set(Calendar.SECOND, 59);
            calendar.set(Calendar.MILLISECOND, 999);
            Date todayEnd = calendar.getTime();
            
            // HQL query for today's completed orders
            String hql = "SELECT SUM(o.totalAmount) FROM Order o " +
                        "WHERE o.status = 'COMPLETED' " +
                        "AND o.createdAt BETWEEN :startDate AND :endDate";
            
            Double total = (Double) session.createQuery(hql)
                    .setParameter("startDate", todayStart)
                    .setParameter("endDate", todayEnd)
                    .uniqueResult();
            
            return total != null ? total : 0.0;
        } catch (Exception e) {
            e.printStackTrace();
            return 0.0;
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }
    
    public Long getTodayOrdersCount() {
        Session session = null;
        try {
            session = HibernateUtil.getSessionFactory().openSession();
            
            // Create today's date range
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            Date todayStart = calendar.getTime();
            
            calendar.set(Calendar.HOUR_OF_DAY, 23);
            calendar.set(Calendar.MINUTE, 59);
            calendar.set(Calendar.SECOND, 59);
            calendar.set(Calendar.MILLISECOND, 999);
            Date todayEnd = calendar.getTime();
            
            Criteria criteria = session.createCriteria(Order.class);
            criteria.add(Restrictions.between("createdAt", todayStart, todayEnd));
            criteria.setProjection(Projections.rowCount());
            
            return (Long) criteria.uniqueResult();
        } catch (Exception e) {
            e.printStackTrace();
            return 0L;
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }
    
    public List<Map<String, Object>> getRecentOrders(int limit) {
        Session session = null;
        try {
            session = HibernateUtil.getSessionFactory().openSession();
            
            String hql = "SELECT o.id as id, o.totalAmount as total, o.createdAt as date, " +
                        "u.username as customerName, o.status as status " +
                        "FROM Order o " +
                        "LEFT JOIN o.user u " +
                        "ORDER BY o.createdAt DESC";
            
            List<Map<String, Object>> orders = session.createQuery(hql)
                    .setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP)
                    .setMaxResults(limit)
                    .list();
            
            return orders;
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }
    
    public Map<String, Long> getOrdersByStatus() {
        Session session = null;
        try {
            session = HibernateUtil.getSessionFactory().openSession();
            
            String hql = "SELECT o.status as status, COUNT(o) as count " +
                        "FROM Order o " +
                        "GROUP BY o.status";
            
            List<Object[]> results = session.createQuery(hql).list();
            
            Map<String, Long> statusCount = new HashMap<>();
            for (Object[] result : results) {
                statusCount.put((String) result[0], (Long) result[1]);
            }
            
            return statusCount;
        } catch (Exception e) {
            e.printStackTrace();
            return new HashMap<>();
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }
    
    //done
    public Order getOrderById(Long orderId) {
        Session session = null;
        try {
            session = HibernateUtil.getSessionFactory().openSession();
            
            Criteria criteria = session.createCriteria(Order.class);
            criteria.add(Restrictions.eq("id", orderId));
            
            return (Order) criteria.uniqueResult();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }

    public List<Order> getOrdersByUser(Long userId) {
        Session session = null;
        try {
            session = HibernateUtil.getSessionFactory().openSession();
            
            Criteria criteria = session.createCriteria(Order.class);
            criteria.createAlias("user", "u");
            criteria.add(Restrictions.eq("u.id", userId));
            criteria.addOrder(org.hibernate.criterion.Order.desc("orderDate"));
            
            return criteria.list();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }

    public boolean updateOrderStatus(Long orderId, OrderStatus status) {
        Session session = null;
        Transaction transaction = null;
        try {
            session = HibernateUtil.getSessionFactory().openSession();
            transaction = session.beginTransaction();
            
            Order order = (Order) session.get(Order.class, orderId);
            if (order != null) {
                order.setStatus(status);
                session.update(order);
                transaction.commit();
                return true;
            }
            return false;
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            e.printStackTrace();
            return false;
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }

    public Order createOrder(Order order) {
        Session session = null;
        Transaction transaction = null;
        try {
            session = HibernateUtil.getSessionFactory().openSession();
            transaction = session.beginTransaction();
            
            session.save(order);
            
            transaction.commit();
            return order;
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            e.printStackTrace();
            return null;
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }
}