package hibernate;

import java.io.Serializable;
import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "cart_items")
public class CartItem implements Serializable{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;
    
    @Column(name = "quantity", nullable = false)
    private Integer quantity = 1;
    
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "added_date")
    private Date addedDate;
    
    // Constructors
    public CartItem() {
        this.addedDate = new Date();
    }
    
    public CartItem(User user, Product product, Integer quantity) {
        this();
        this.user = user;
        this.product = product;
        this.quantity = quantity;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public User getUser() {
        return user;
    }
    
    public void setUser(User user) {
        this.user = user;
    }
    
    public Product getProduct() {
        return product;
    }
    
    public void setProduct(Product product) {
        this.product = product;
    }
    
    public Integer getQuantity() {
        return quantity;
    }
    
    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
    
    public Date getAddedDate() {
        return addedDate;
    }
    
    public void setAddedDate(Date addedDate) {
        this.addedDate = addedDate;
    }
    
    // Business methods
    public Double getSubtotal() {
        if (product != null && product.getPrice() != null) {
            return product.getPrice().doubleValue() * quantity;
        }
        return 0.0;
    }
    
    @Override
    public String toString() {
        return "CartItem{" +
                "id=" + id +
                ", user=" + (user != null ? user.getUsername() : "null") +
                ", product=" + (product != null ? product.getName() : "null") +
                ", quantity=" + quantity +
                ", addedDate=" + addedDate +
                '}';
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CartItem)) return false;
        CartItem cartItem = (CartItem) o;
        return id != null && id.equals(cartItem.getId());
    }
    
    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}