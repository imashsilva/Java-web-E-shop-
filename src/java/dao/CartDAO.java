package dao;

import hibernate.CartItem;
import hibernate.User;
import hibernate.Product;
import hibernate.HibernateUtil;
import java.util.ArrayList;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;
import org.hibernate.criterion.Projections;
import java.util.List;

public class CartDAO {

    public CartItem getCartItemByUserAndProduct(Long userId, Long productId) {
        Session session = null;
        try {
            session = HibernateUtil.getSessionFactory().openSession();
            Criteria criteria = session.createCriteria(CartItem.class);
            criteria.createAlias("user", "u");
            criteria.createAlias("product", "p");
            criteria.add(Restrictions.eq("u.id", userId));
            criteria.add(Restrictions.eq("p.id", productId));
            return (CartItem) criteria.uniqueResult();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }

    public List<CartItem> getCartItemsByUser(Long userId) {
        Session session = null;
        try {
            session = HibernateUtil.getSessionFactory().openSession();

            // More robust query with explicit joins
            String hql = "SELECT ci FROM CartItem ci "
                    + "LEFT JOIN FETCH ci.product p "
                    + "LEFT JOIN FETCH ci.user u "
                    + "WHERE u.id = :userId "
                    + "ORDER BY ci.addedDate DESC";

            @SuppressWarnings("unchecked")
            List<CartItem> cartItems = session.createQuery(hql)
                    .setParameter("userId", userId)
                    .setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY) // Avoid duplicates
                    .list();

            System.out.println("✅ Retrieved " + (cartItems != null ? cartItems.size() : 0) + " cart items for user " + userId);
            return cartItems != null ? cartItems : new ArrayList<>();

        } catch (Exception e) {
            System.err.println("❌ Error in getCartItemsByUser: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }

    public boolean addToCart(Long userId, Long productId, Integer quantity) {
        Session session = null;
        Transaction transaction = null;
        try {
            session = HibernateUtil.getSessionFactory().openSession();
            transaction = session.beginTransaction();

            // Check if item already exists in cart
            CartItem existingItem = getCartItemByUserAndProduct(userId, productId);

            if (existingItem != null) {
                // Update quantity if item exists
                existingItem.setQuantity(existingItem.getQuantity() + quantity);
                session.update(existingItem);
            } else {
                // Create new cart item
                User user = (User) session.get(User.class, userId);
                Product product = (Product) session.get(Product.class, productId);

                if (user != null && product != null) {
                    CartItem cartItem = new CartItem(user, product, quantity);
                    session.save(cartItem);
                } else {
                    throw new Exception("User or Product not found");
                }
            }

            transaction.commit();
            return true;
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

    public boolean updateCartItemQuantity(Long cartItemId, Integer quantity) {
        Session session = null;
        Transaction transaction = null;
        try {
            session = HibernateUtil.getSessionFactory().openSession();
            transaction = session.beginTransaction();

            CartItem cartItem = (CartItem) session.get(CartItem.class, cartItemId);
            if (cartItem != null) {
                if (quantity <= 0) {
                    session.delete(cartItem);
                } else {
                    cartItem.setQuantity(quantity);
                    session.update(cartItem);
                }
            }

            transaction.commit();
            return true;
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

    public boolean removeFromCart(Long cartItemId) {
        Session session = null;
        Transaction transaction = null;
        try {
            session = HibernateUtil.getSessionFactory().openSession();
            transaction = session.beginTransaction();

            CartItem cartItem = (CartItem) session.get(CartItem.class, cartItemId);
            if (cartItem != null) {
                session.delete(cartItem);
            }

            transaction.commit();
            return true;
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

    public boolean clearUserCart(Long userId) {
        Session session = null;
        Transaction transaction = null;
        try {
            session = HibernateUtil.getSessionFactory().openSession();
            transaction = session.beginTransaction();

            String hql = "DELETE FROM CartItem ci WHERE ci.user.id = :userId";
            int deletedCount = session.createQuery(hql)
                    .setParameter("userId", userId)
                    .executeUpdate();

            transaction.commit();
            return true;
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

    public int getCartItemCount(Long userId) {
        Session session = null;
        try {
            session = HibernateUtil.getSessionFactory().openSession();
            Criteria criteria = session.createCriteria(CartItem.class);
            criteria.createAlias("user", "u");
            criteria.add(Restrictions.eq("u.id", userId));
            criteria.setProjection(Projections.rowCount());
            Long count = (Long) criteria.uniqueResult();
            return count != null ? count.intValue() : 0;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }

    public double getCartTotal(Long userId) {
        Session session = null;
        try {
            session = HibernateUtil.getSessionFactory().openSession();
            String hql = "SELECT SUM(ci.quantity * p.price) FROM CartItem ci JOIN ci.product p WHERE ci.user.id = :userId";
            Double total = (Double) session.createQuery(hql)
                    .setParameter("userId", userId)
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
}
