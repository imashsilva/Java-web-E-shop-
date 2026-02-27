package model;

import hibernate.Product;
import hibernate.Category;
import java.math.BigDecimal;
import java.util.Date;

public class ProductDTO {
    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private Integer Quantity;
    private String imageUrl;
    private Long categoryId;
    private String categoryName;
    private Date createdAt;

    public ProductDTO(Product product) {
        this.id = product.getId();
        this.name = product.getName();
        this.description = product.getDescription();
        this.price = product.getPrice();
        
        // Make sure this field name matches your Product entity
        this.Quantity = product.getQuantity(); 
        
        this.imageUrl = product.getImageUrl();
        this.createdAt = product.getCreatedAt();
        
        // Safely handle category (which might be a Hibernate proxy or null)
        Category category = product.getCategory();
        if (category != null) {
            this.categoryId = category.getId();
            this.categoryName = category.getName();
        } else {
            this.categoryId = null;
            this.categoryName = "Uncategorized";
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Integer getQuantity() {
        return Quantity;
    }

    public void setQuantity(Integer Quantity) {
        this.Quantity = Quantity;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
}