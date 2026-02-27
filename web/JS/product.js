// Product Page JavaScript - Separate from session.js
class ProductManager {
    constructor() {
        this.currentPage = 1;
        this.productsPerPage = 8;
        this.allProducts = [];
        this.init();
    }

    init() {
        this.loadCategories();
        this.loadProducts();
        this.setupEventListeners();
    }

    loadCategories() {
        fetch('categories?format=json')
            .then(response => response.json())
            .then(categories => {
                const categoriesList = document.getElementById('categoriesList');
                if (!categoriesList) return;

                categoriesList.innerHTML = `
                    <div class="category-item active" data-category="all">
                        <i class="fas fa-th-large me-2"></i>All Categories
                    </div>
                `;

                categories.forEach(category => {
                    categoriesList.innerHTML += `
                        <div class="category-item" data-category="${category.id}">
                            <i class="fas fa-mobile-alt me-2"></i>${category.name}
                        </div>
                    `;
                });
            })
            .catch(error => {
                console.error('Error loading categories:', error);
            });
    }

    loadProducts(category = 'all', search = '', sort = 'newest') {
        this.showLoading(true);

        let url = `products?format=json&sort=${sort}`;
        if (category !== 'all') {
            url += `&category=${category}`;
        }
        if (search) {
            url += `&search=${encodeURIComponent(search)}`;
        }

        fetch(url)
            .then(response => response.json())
            .then(products => {
                this.allProducts = products;
                this.displayProducts(products);
                this.updatePagination(products.length);
                this.showLoading(false);
            })
            .catch(error => {
                console.error('Error loading products:', error);
                this.showLoading(false);
            });
    }

    displayProducts(products) {
        const productsGrid = document.getElementById('productsGrid');
        const noProducts = document.getElementById('noProducts');

        if (!productsGrid || !noProducts) return;

        if (products.length === 0) {
            productsGrid.innerHTML = '';
            noProducts.style.display = 'block';
            return;
        }

        noProducts.style.display = 'none';

        // Calculate products to show for current page
        const startIndex = (this.currentPage - 1) * this.productsPerPage;
        const endIndex = startIndex + this.productsPerPage;
        const productsToShow = products.slice(startIndex, endIndex);

        let html = '';
        productsToShow.forEach(product => {
            // Convert price if it's an object (BigDecimal)
            const price = typeof product.price === 'object' ? 
                         product.price : 
                         parseFloat(product.price);
            
            const isOutOfStock = product.Quantity === 0;

            html += `
                <div class="col-lg-3 col-md-6">
                    <div class="product-card">
                        <div class="position-relative">
                            <img src="${product.imageUrl || 'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?ixlib=rb-4.0.3&w=500'}" 
                                 class="card-img-top product-image" alt="${product.name}">
                            ${isOutOfStock ? '<div class="product-badge"><span class="badge bg-secondary">Out of Stock</span></div>' : ''}
                            <div class="product-actions">
                                <button class="action-btn btn-warning text-white" title="Add to Wishlist" onclick="productManager.addToWishlist(${product.id})">
                                    <i class="fas fa-heart"></i>
                                </button>
                                <button class="action-btn btn-info text-white" title="Quick View" onclick="productManager.quickView(${product.id})">
                                    <i class="fas fa-eye"></i>
                                </button>
                            </div>
                        </div>
                        <div class="card-body">
                            <h5 class="card-title">${product.name}</h5>
                            <p class="card-text text-muted small">${product.description ? product.description.substring(0, 60) + '...' : 'No description available'}</p>
                            <div class="d-flex justify-content-between align-items-center mb-3">
                                <span class="h5 text-primary mb-0">$${parseFloat(price).toFixed(2)}</span>
                                <small class="text-muted">In stock: ${product.Quantity}</small>
                            </div>
                            <div class="d-grid gap-2">
                                <a href="product-detail.html?id=${product.id}" class="btn btn-outline-primary btn-sm">View Details</a>
                                <button class="btn btn-primary btn-sm add-to-cart-btn" 
                                        data-product-id="${product.id}"
                                        data-product-name="${product.name}"
                                        ${isOutOfStock ? 'disabled' : ''}>
                                    <i class="fas fa-cart-plus me-1"></i> Add to Cart
                                </button>
                            </div>
                        </div>
                    </div>
                </div>
            `;
        });

        productsGrid.innerHTML = html;

        // Add event listeners to Add to Cart buttons
        this.setupAddToCartListeners();
    }

    setupAddToCartListeners() {
        document.querySelectorAll('.add-to-cart-btn').forEach(button => {
            button.addEventListener('click', (event) => {
                const productId = button.getAttribute('data-product-id');
                const productName = button.getAttribute('data-product-name');
                this.addToCart(productId, productName, button);
            });
        });
    }

    // REAL Add to Cart function
    async addToCart(productId, productName, button) {
        // Check if sessionManager exists
        if (!window.sessionManager) {
            console.error('sessionManager not found');
            this.showMessage('Session manager not available. Please refresh the page.', 'error');
            return;
        }

        if (!window.sessionManager.isLoggedIn()) {
            const redirectUrl = encodeURIComponent(window.location.href);
            if (confirm('You need to login to add items to cart. Would you like to login now?')) {
                window.location.href = `login.html?redirect=${redirectUrl}`;
            }
            return;
        }

        try {
            const result = await window.sessionManager.addToCart(productId, 1);
            
            // Show success message
            this.showMessage('Product added to cart!', 'success');
            
            // Update cart count in navigation
            window.sessionManager.updateCartCount();
            
            // Visual feedback on the button
            if (button) {
                this.animateAddToCartButton(button);
            }
            
        } catch (error) {
            console.error('Failed to add to cart:', error);
            this.showMessage(error.message || 'Failed to add product to cart', 'error');
        }
    }

    animateAddToCartButton(button) {
        const originalHTML = button.innerHTML;
        button.innerHTML = '<i class="fas fa-check me-1"></i> Added!';
        button.classList.remove('btn-primary');
        button.classList.add('btn-success');
        button.disabled = true;
        
        // Reset button after 2 seconds
        setTimeout(() => {
            button.innerHTML = originalHTML;
            button.classList.remove('btn-success');
            button.classList.add('btn-primary');
            button.disabled = false;
        }, 2000);
    }

    showMessage(message, type = 'info') {
        // Use ToastManager if available, otherwise use alert
        if (window.ToastManager) {
            window.ToastManager.show(message, type);
        } else {
            // Fallback to simple alert or create basic notification
            const alertClass = type === 'success' ? 'alert-success' : 
                             type === 'error' ? 'alert-danger' : 'alert-info';
            
            // Create a simple notification
            const notification = document.createElement('div');
            notification.className = `alert ${alertClass} alert-dismissible fade show position-fixed top-0 end-0 m-3`;
            notification.style.zIndex = '9999';
            notification.innerHTML = `
                ${message}
                <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
            `;
            document.body.appendChild(notification);
            
            // Remove after 3 seconds
            setTimeout(() => {
                notification.remove();
            }, 3000);
        }
    }

    setupEventListeners() {
        // Category filter
        document.addEventListener('click', (e) => {
            if (e.target.closest('.category-item')) {
                const categoryItem = e.target.closest('.category-item');
                const category = categoryItem.dataset.category;

                // Update active state
                document.querySelectorAll('.category-item').forEach(item => {
                    item.classList.remove('active');
                });
                categoryItem.classList.add('active');

                this.currentPage = 1;
                this.loadProducts(category, document.getElementById('searchInput').value, document.getElementById('sortSelect').value);
            }
        });

        // Search
        const searchBtn = document.getElementById('searchBtn');
        const searchInput = document.getElementById('searchInput');
        const sortSelect = document.getElementById('sortSelect');

        if (searchBtn) {
            searchBtn.addEventListener('click', () => {
                this.currentPage = 1;
                this.loadProducts(this.getSelectedCategory(), searchInput.value, sortSelect.value);
            });
        }

        if (searchInput) {
            searchInput.addEventListener('keypress', (e) => {
                if (e.key === 'Enter') {
                    this.currentPage = 1;
                    this.loadProducts(this.getSelectedCategory(), searchInput.value, sortSelect.value);
                }
            });
        }

        // Sort
        if (sortSelect) {
            sortSelect.addEventListener('change', () => {
                this.currentPage = 1;
                this.loadProducts(this.getSelectedCategory(), searchInput.value, sortSelect.value);
            });
        }
    }

    getSelectedCategory() {
        const activeCategory = document.querySelector('.category-item.active');
        return activeCategory ? activeCategory.dataset.category : 'all';
    }

    updatePagination(totalProducts) {
        const pagination = document.getElementById('pagination');
        if (!pagination) return;

        const totalPages = Math.ceil(totalProducts / this.productsPerPage);

        if (totalPages <= 1) {
            pagination.innerHTML = '';
            return;
        }

        let html = '';

        // Previous button
        html += `
            <li class="page-item ${this.currentPage === 1 ? 'disabled' : ''}">
                <a class="page-link" href="#" onclick="productManager.changePage(${this.currentPage - 1})">Previous</a>
            </li>
        `;

        // Page numbers
        for (let i = 1; i <= totalPages; i++) {
            html += `
                <li class="page-item ${this.currentPage === i ? 'active' : ''}">
                    <a class="page-link" href="#" onclick="productManager.changePage(${i})">${i}</a>
                </li>
            `;
        }

        // Next button
        html += `
            <li class="page-item ${this.currentPage === totalPages ? 'disabled' : ''}">
                <a class="page-link" href="#" onclick="productManager.changePage(${this.currentPage + 1})">Next</a>
            </li>
        `;

        pagination.innerHTML = html;
    }

    changePage(page) {
        this.currentPage = page;
        this.displayProducts(this.allProducts);
        window.scrollTo({top: 0, behavior: 'smooth'});
    }

    showLoading(show) {
        const loadingSpinner = document.getElementById('loadingSpinner');
        if (loadingSpinner) {
            loadingSpinner.style.display = show ? 'block' : 'none';
        }
    }

    clearFilters() {
        const searchInput = document.getElementById('searchInput');
        const sortSelect = document.getElementById('sortSelect');

        if (searchInput) searchInput.value = '';
        if (sortSelect) sortSelect.value = 'newest';

        document.querySelectorAll('.category-item').forEach(item => {
            item.classList.remove('active');
        });
        
        const allCategory = document.querySelector('.category-item[data-category="all"]');
        if (allCategory) allCategory.classList.add('active');

        this.currentPage = 1;
        this.loadProducts();
    }

    // Placeholder functions for wishlist
    addToWishlist(productId) {
        console.log('Add to wishlist:', productId);
        this.showMessage('Added to wishlist!', 'info');
        // Implement wishlist functionality
    }

    quickView(productId) {
        console.log('Quick view:', productId);
        // Implement quick view functionality
        window.location.href = `product-detail.html?id=${productId}`;
    }
}

// Initialize product manager when DOM is loaded
document.addEventListener('DOMContentLoaded', function() {
    window.productManager = new ProductManager();
});

// Make functions globally available for HTML onclick handlers
window.clearFilters = function() {
    if (window.productManager) {
        window.productManager.clearFilters();
    }
};