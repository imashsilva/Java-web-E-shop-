package dao;

import hibernate.Product;
import hibernate.Category;
import hibernate.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.MatchMode;
import java.util.List;
import java.util.ArrayList;
import org.hibernate.Transaction;
import org.hibernate.criterion.Projections;

public class ProductDAO {
    
    public List<Product> getAllProducts() {
    Session session = null;
    try {
        session = HibernateUtil.getSessionFactory().openSession();
        
        // Use HQL with JOIN FETCH to load categories eagerly
        String hql = "FROM Product p LEFT JOIN FETCH p.category ORDER BY p.createdAt DESC";
        List<Product> products = session.createQuery(hql).list();
        
        return products;
    } catch (Exception e) {
        e.printStackTrace();
        return new ArrayList<>();
    } finally {
        if (session != null && session.isOpen()) {
            session.close();
        }
    }
}
    
public Product getProductById(Long id) {
    Session session = null;
    try {
        session = HibernateUtil.getSessionFactory().openSession();
        Product product = (Product) session.get(Product.class, id);
        
        // Initialize the category to avoid LazyInitializationException
        if (product != null && product.getCategory() != null) {
            // This forces Hibernate to load the category while session is open
            product.getCategory().getName(); // Just accessing it loads it
        }
        
        return product;
    } catch (Exception e) {
        e.printStackTrace();
        return null;
    } finally {
        if (session != null && session.isOpen()) {
            session.close();
        }
    }
}
    
    // METHOD 1: Using direct foreign key (if no JPA relationship)
    public List<Product> getProductsByCategory(Long categoryId) {
        Session session = null;
        try {
            session = HibernateUtil.getSessionFactory().openSession();
            Criteria criteria = session.createCriteria(Product.class);
            criteria.add(Restrictions.eq("category.id", categoryId));
            criteria.addOrder(Order.desc("createdAt"));
            return criteria.list();
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }
    
    // METHOD 2: Alternative using HQL (if Criteria doesn't work)
    public List<Product> getProductsByCategoryHQL(Long categoryId) {
        Session session = null;
        try {
            session = HibernateUtil.getSessionFactory().openSession();
            String hql = "FROM Product p WHERE p.category.id = :categoryId ORDER BY p.createdAt DESC";
            return session.createQuery(hql)
                         .setParameter("categoryId", categoryId)
                         .list();
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }
    
    // METHOD 3: Using native SQL query (most reliable)
    public List<Product> getProductsByCategorySQL(Long categoryId) {
        Session session = null;
        try {
            session = HibernateUtil.getSessionFactory().openSession();
            String sql = "SELECT * FROM products WHERE category_id = :categoryId ORDER BY created_at DESC";
            return session.createSQLQuery(sql)
                         .addEntity(Product.class)
                         .setParameter("categoryId", categoryId)
                         .list();
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }
    
    public List<Product> searchProducts(String query) {
        Session session = null;
        try {
            session = HibernateUtil.getSessionFactory().openSession();
            Criteria criteria = session.createCriteria(Product.class);
            criteria.add(Restrictions.or(
                Restrictions.ilike("name", query, MatchMode.ANYWHERE),
                Restrictions.ilike("description", query, MatchMode.ANYWHERE)
            ));
            criteria.addOrder(Order.desc("createdAt"));
            return criteria.list();
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }
    
    public List<Product> sortProducts(List<Product> products, String sortBy) {
        if (products == null) return new ArrayList<>();
        
        List<Product> sortedProducts = new ArrayList<>(products);
        
        switch (sortBy) {
            case "price_low":
                sortedProducts.sort((p1, p2) -> p1.getPrice().compareTo(p2.getPrice()));
                break;
            case "price_high":
                sortedProducts.sort((p1, p2) -> p2.getPrice().compareTo(p1.getPrice()));
                break;
            case "name":
                sortedProducts.sort((p1, p2) -> p1.getName().compareToIgnoreCase(p2.getName()));
                break;
            case "newest":
            default:
                // Already sorted by newest by default
                break;
        }
        
        return sortedProducts;
    }
    
    public List<Product> getLowStockProducts(int threshold) {
        Session session = null;
        try {
            session = HibernateUtil.getSessionFactory().openSession();
            Criteria criteria = session.createCriteria(Product.class);
            criteria.add(Restrictions.le("quantity", threshold));
            criteria.addOrder(Order.asc("quantity"));
            return criteria.list();
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }
    
    public Long getOutOfStockCount() {
        Session session = null;
        try {
            session = HibernateUtil.getSessionFactory().openSession();
            Criteria criteria = session.createCriteria(Product.class);
            criteria.add(Restrictions.eq("quantity", 0));
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
    
    public Long getLowStockCount(int threshold) {
        Session session = null;
        try {
            session = HibernateUtil.getSessionFactory().openSession();
            Criteria criteria = session.createCriteria(Product.class);
            criteria.add(Restrictions.le("quantity", threshold));
            criteria.add(Restrictions.gt("quantity", 0));
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
    
    public boolean saveProduct(Product product) {
        Session session = null;
        Transaction transaction = null;
        try {
            session = HibernateUtil.getSessionFactory().openSession();
            transaction = session.beginTransaction();
            session.saveOrUpdate(product);
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
    
    public boolean deleteProduct(Long productId) {
        Session session = null;
        Transaction transaction = null;
        try {
            session = HibernateUtil.getSessionFactory().openSession();
            transaction = session.beginTransaction();
            
            Product product = (Product) session.get(Product.class, productId);
            if (product != null) {
                session.delete(product);
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
    
    public boolean updateProduct(Product product) {
        Session session = null;
        Transaction transaction = null;
        try {
            session = HibernateUtil.getSessionFactory().openSession();
            transaction = session.beginTransaction();
            session.update(product);
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
    
    public List<Product> getRelatedProducts(Product product) {
        Session session = null;
        try {
            session = HibernateUtil.getSessionFactory().openSession();
            Criteria criteria = session.createCriteria(Product.class);
            
            if (product.getCategory() != null) {
                criteria.add(Restrictions.eq("category.id", product.getCategory().getId()));
            }
            
            criteria.add(Restrictions.ne("id", product.getId()));
            criteria.setMaxResults(4);
            criteria.addOrder(Order.desc("createdAt"));
            return criteria.list();
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }
}