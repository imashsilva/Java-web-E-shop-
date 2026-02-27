// Session Management and Dynamic Navigation
class SessionManager {
    constructor() {
        this.currentUser = null;
        this.cartManager = new CartManager();
        this.checkoutProcess = new CheckoutProcess();
        this.init();
    }

    init() {
        this.loadUserFromStorage();
        this.updateNavigation();
        this.checkAuthentication();
        this.updateCartCount();
    }

    loadUserFromStorage() {
        const userData = localStorage.getItem('currentUser');
        if (userData) {
            this.currentUser = JSON.parse(userData);
            console.log('User loaded from storage:', this.currentUser);
        }
    }

    saveUserToStorage(user) {
        this.currentUser = user;
        localStorage.setItem('currentUser', JSON.stringify(user));
        console.log('User saved to storage:', user);
        this.updateNavigation();
        this.updateCartCount();
    }

    clearStorage() {
        this.currentUser = null;
        localStorage.removeItem('currentUser');
        this.updateNavigation();
        this.updateCartCount();

        if (this.isProtectedPage()) {
            window.location.href = 'login.html';
        }
    }

    isProtectedPage() {
        const protectedPages = ['profile.html', 'orders.html', 'checkout.html', 'cart.html'];
        const currentPage = window.location.pathname.split('/').pop();
        return protectedPages.includes(currentPage);
    }

    updateNavigation() {
        const userMenu = document.getElementById('userMenu');
        if (!userMenu) {
            console.log('User menu element not found');
            return;
        }

        if (this.currentUser) {
            userMenu.innerHTML = `
                <a class="nav-link dropdown-toggle" href="#" id="userDropdown" role="button" data-bs-toggle="dropdown" aria-expanded="false">
                    <i class="fas fa-user-circle fa-lg me-1"></i>
                    ${this.currentUser.username || this.currentUser.name}
                </a>
                <ul class="dropdown-menu dropdown-menu-end">
                    <li><a class="dropdown-item" href="profile.html"><i class="fas fa-user me-2"></i>My Profile</a></li>
                    <li><a class="dropdown-item" href="orders.html"><i class="fas fa-shopping-bag me-2"></i>My Orders</a></li>
                    <li><a class="dropdown-item" href="wishlist.html"><i class="fas fa-heart me-2"></i>Wishlist</a></li>
                    <li><hr class="dropdown-divider"></li>
                    <li><a class="dropdown-item" href="#" id="logoutBtn"><i class="fas fa-sign-out-alt me-2"></i>Logout</a></li>
                </ul>
            `;

            setTimeout(() => {
                const logoutBtn = document.getElementById('logoutBtn');
                if (logoutBtn) {
                    logoutBtn.addEventListener('click', (e) => {
                        e.preventDefault();
                        this.logout();
                    });
                }
            }, 100);

        } else {
            userMenu.innerHTML = `
                <a class="nav-link dropdown-toggle" href="#" id="userDropdown" role="button" data-bs-toggle="dropdown" aria-expanded="false">
                    <i class="fas fa-user fa-lg"></i>
                </a>
                <ul class="dropdown-menu dropdown-menu-end">
                    <li><a class="dropdown-item" href="login.html"><i class="fas fa-sign-in-alt me-2"></i>Login</a></li>
                    <li><a class="dropdown-item" href="register.html"><i class="fas fa-user-plus me-2"></i>Register</a></li>
                </ul>
            `;
        }

        console.log('Navigation updated. User logged in:', this.isLoggedIn());
    }

    // Update cart count in navigation
    async updateCartCount() {
        if (!this.isLoggedIn()) {
            const cartCountElements = document.querySelectorAll('#cartCount');
            cartCountElements.forEach(element => {
                element.textContent = '0';
                element.style.display = 'none';
            });
            return;
        }

        try {
            const count = await this.cartManager.getCartCount();
            const cartCountElements = document.querySelectorAll('#cartCount');
            cartCountElements.forEach(element => {
                element.textContent = count;
                element.style.display = count > 0 ? 'inline' : 'none';
            });
            console.log('🛒 Cart count updated to:', count);
        } catch (error) {
            console.error('Error updating cart count:', error);
        }
    }

    checkAuthentication() {
        if (this.isProtectedPage() && !this.currentUser) {
            window.location.href = 'login.html?redirect=' + encodeURIComponent(window.location.href);
        }
    }

    // REAL LOGIN METHOD THAT CONNECTS TO SERVLET
    login(loginData) {
        return new Promise((resolve, reject) => {
            console.log('🔐 Attempting login to: login');
            console.log('📧 Login data:', loginData);

            $.ajax({
                url: 'login',
                type: 'POST',
                data: loginData,
                success: (response) => {
                    console.log('✅ Login success response:', response);

                    // For text response, create user object
                    if (typeof response === 'string' && response === "Login successful") {
                        const user = {
                            id: Date.now(), // Temporary ID
                            username: loginData.username,
                            name: loginData.username.charAt(0).toUpperCase() + loginData.username.slice(1),
                            email: loginData.username + '@example.com',
                            loginTime: new Date().toISOString(),
                            role: 'CUSTOMER'
                        };
                        this.saveUserToStorage(user);
                        resolve(user);
                    } else {
                        // Handle JSON response
                        let user;
                        try {
                            user = typeof response === 'string' ? JSON.parse(response) : response;
                            this.saveUserToStorage(user);
                            resolve(user);
                        } catch (e) {
                            console.error('❌ Failed to parse login response:', e);
                            reject(new Error('Invalid login response'));
                        }
                    }
                },
                error: (xhr, status, error) => {
                    console.error('❌ Login failed - Status:', xhr.status);
                    console.error('Response:', xhr.responseText);
                    reject(new Error(xhr.responseText || 'Login failed. Check server connection.'));
                }
            });
        });
    }

    // REAL REGISTRATION METHOD THAT CONNECTS TO SERVLET
    registerUser(userData) {
        return new Promise((resolve, reject) => {
            console.log('📝 Attempting registration to: register');
            console.log('📋 Registration data:', userData);

            $.ajax({
                url: 'register',
                type: 'POST',
                data: userData,
                success: (response) => {
                    console.log('✅ Registration success:', response);
                    resolve(response);
                },
                error: (xhr, status, error) => {
                    console.error('❌ Registration failed - Status:', xhr.status);
                    console.error('Response:', xhr.responseText);
                    reject(new Error(xhr.responseText || 'Registration failed. Check server connection.'));
                }
            });
        });
    }

    // ADD TO CART METHOD
    async addToCart(productId, quantity = 1) {
        if (!this.isLoggedIn()) {
            const redirectUrl = encodeURIComponent(window.location.href);
            window.location.href = `login.html?redirect=${redirectUrl}`;
            return false;
        }

        try {
            console.log('🛒 SessionManager.addToCart called with:', {productId, quantity});
            const result = await this.cartManager.addToCart(productId, quantity);
            await this.updateCartCount();
            return result;
        } catch (error) {
            console.error('❌ Error in SessionManager.addToCart:', error);
            throw error;
    }
    }

    // Get cart items
    async getCartItems() {
        if (!this.isLoggedIn()) {
            return {items: []};
        }

        try {
            const data = await this.cartManager.getCartItems();
            console.log('📦 SessionManager.getCartItems received:', data);
            return data;
        } catch (error) {
            console.error('Error getting cart items:', error);
            return {items: []};
        }
    }

    // CHECKOUT METHODS
    async createOrder(orderData) {
        if (!this.isLoggedIn()) {
            throw new Error('User must be logged in to create order');
        }

        try {
            const result = await this.checkoutProcess.createOrder(orderData);
            return result;
        } catch (error) {
            console.error('Error creating order:', error);
            throw error;
        }
    }

    async initiatePayment(orderId) {
        try {
            const result = await this.checkoutProcess.initiatePayHerePayment(orderId);
            return result;
        } catch (error) {
            console.error('Error initiating payment:', error);
            throw error;
        }
    }

    async confirmOrder(orderId) {
        try {
            const result = await this.checkoutProcess.confirmOrder(orderId);
            return result;
        } catch (error) {
            console.error('Error confirming order:', error);
            throw error;
        }
    }

    async getOrderSummary() {
        try {
            const result = await this.checkoutProcess.getOrderSummary();
            return result;
        } catch (error) {
            console.error('Error getting order summary:', error);
            throw error;
        }
    }

    logout() {
        this.clearStorage();
        window.location.href = 'index.html';
    }

    getCurrentUser() {
        return this.currentUser;
    }

    isLoggedIn() {
        return this.currentUser !== null;
    }
}

// Cart Manager Class
class CartManager {
    constructor() {
        this.baseUrl = this.getBaseUrl() + '/cart';
        console.log('🛒 CartManager initialized with URL:', this.baseUrl);
    }

    getBaseUrl() {
        const currentPath = window.location.pathname;
        const appPath = currentPath.substring(0, currentPath.lastIndexOf('/'));
        return window.location.origin + appPath;
    }

    // Add product to cart
    async addToCart(productId, quantity = 1) {
        try {
            const params = new URLSearchParams();
            params.append('action', 'add');
            params.append('productId', productId.toString());
            params.append('quantity', quantity.toString());

            console.log('🛒 Sending add to cart to:', this.baseUrl);
            console.log('📦 Product ID:', productId, 'Quantity:', quantity);

            const response = await fetch(this.baseUrl, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded',
                },
                body: params
            });

            console.log('📡 Response status:', response.status, response.statusText);

            const responseText = await response.text();
            console.log('📄 Raw response:', responseText);

            let result;
            try {
                result = JSON.parse(responseText);
            } catch (parseError) {
                console.error('❌ Failed to parse JSON:', parseError);
                throw new Error('Server returned invalid JSON: ' + responseText.substring(0, 100));
            }

            if (!response.ok) {
                throw new Error(result.error || 'Failed to add to cart');
            }

            return result;
        } catch (error) {
            console.error('❌ Error adding to cart:', error);
            throw error;
    }
    }

    // Add this method to CartManager class in session.js
    async makeRequest(url, options = {}) {
        try {
            console.log('🌐 Making request to:', url, 'Options:', options);

            const response = await fetch(url, {
                credentials: 'include', // Important for sessions
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded',
                    ...options.headers
                },
                ...options
            });

            console.log('📡 Response status:', response.status, response.statusText);

            const responseText = await response.text();
            console.log('📄 Raw response:', responseText.substring(0, 200)); // Log first 200 chars

            // Check if response is HTML error page
            if (responseText.trim().startsWith('<!DOCTYPE') ||
                    responseText.includes('<html') ||
                    responseText.includes('Error Page')) {
                console.error('❌ Server returned HTML error page instead of JSON');
                throw new Error('Server error: Received HTML error page. Check server logs.');
            }

            let result;
            try {
                result = JSON.parse(responseText);
            } catch (parseError) {
                console.error('❌ JSON parse error:', parseError);
                console.error('📄 Failed to parse:', responseText);
                throw new Error('Server returned invalid JSON: ' + responseText.substring(0, 100));
            }

            if (!response.ok) {
                throw new Error(result.error || `HTTP ${response.status}: ${response.statusText}`);
            }

            return result;
        } catch (error) {
            console.error('❌ Request failed:', error);
            throw error;
    }
    }

// Update addToCart method to use makeRequest
    async addToCart(productId, quantity = 1) {
        const params = new URLSearchParams();
        params.append('action', 'add');
        params.append('productId', productId.toString());
        params.append('quantity', quantity.toString());

        return await this.makeRequest(this.baseUrl, {
            method: 'POST',
            body: params
        });
    }

    // Get cart items
    async getCartItems() {
        try {
            const url = `${this.baseUrl}?format=json`;
            console.log('📋 Fetching cart items from:', url);

            const response = await fetch(url, {
                method: 'GET',
                credentials: 'include'
            });

            console.log('📡 Cart items response status:', response.status);

            if (!response.ok) {
                throw new Error(`Failed to fetch cart items: ${response.status} ${response.statusText}`);
            }

            const responseText = await response.text();
            console.log('📄 Cart items raw response:', responseText);

            let result;
            try {
                result = JSON.parse(responseText);
            } catch (parseError) {
                console.error('❌ Failed to parse cart items JSON:', parseError);
                throw new Error('Invalid JSON response from server');
            }

            console.log('🔍 Parsed cart data:', result);
            console.log('📦 Cart items array:', result.items);
            console.log('✅ Success status:', result.success);
            console.log('🔢 Number of items:', result.items ? result.items.length : 0);

            return result;
        } catch (error) {
            console.error('❌ Error fetching cart items:', error);
            throw error;
        }
    }

    // Get cart count
    async getCartCount() {
        try {
            const url = `${this.baseUrl}?action=count&format=json`;
            console.log('🔢 Fetching cart count from:', url);

            const response = await fetch(url, {
                credentials: 'include'
            });

            console.log('📡 Cart count response status:', response.status);

            if (!response.ok) {
                throw new Error('Failed to fetch cart count');
            }

            const responseText = await response.text();
            console.log('📄 Cart count raw response:', responseText);

            const result = JSON.parse(responseText);
            return result.count || 0;
        } catch (error) {
            console.error('Error getting cart count:', error);
            return 0;
        }
    }

    // Update cart item quantity
    async updateCartItem(cartItemId, quantity) {
        try {
            const params = new URLSearchParams();
            params.append('action', 'update');
            params.append('cartItemId', cartItemId.toString());
            params.append('quantity', quantity.toString());

            console.log('✏️ Updating cart item:', cartItemId, 'Quantity:', quantity);

            const response = await fetch(this.baseUrl, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded',
                },
                body: params
            });

            console.log('📡 Update response status:', response.status);

            const responseText = await response.text();
            console.log('📄 Update raw response:', responseText);

            let result;
            try {
                result = JSON.parse(responseText);
            } catch (parseError) {
                console.error('❌ Failed to parse update response:', parseError);
                throw new Error('Server returned invalid JSON');
            }

            if (!response.ok) {
                throw new Error(result.error || 'Failed to update cart item');
            }

            return result;
        } catch (error) {
            console.error('Error updating cart item:', error);
            throw error;
        }
    }

    // Remove item from cart
    async removeFromCart(cartItemId) {
        try {
            const params = new URLSearchParams();
            params.append('action', 'remove');
            params.append('cartItemId', cartItemId.toString());

            console.log('🗑️ Removing cart item:', cartItemId);

            const response = await fetch(this.baseUrl, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded',
                },
                body: params
            });

            console.log('📡 Remove response status:', response.status);

            const responseText = await response.text();
            console.log('📄 Remove raw response:', responseText);

            let result;
            try {
                result = JSON.parse(responseText);
            } catch (parseError) {
                console.error('❌ Failed to parse remove response:', parseError);
                throw new Error('Server returned invalid JSON');
            }

            if (!response.ok) {
                throw new Error(result.error || 'Failed to remove item');
            }

            return result;
        } catch (error) {
            console.error('Error removing item:', error);
            throw error;
        }
    }

    // Clear entire cart
    async clearCart() {
        try {
            const params = new URLSearchParams();
            params.append('action', 'clear');

            console.log('🧹 Clearing entire cart');

            const response = await fetch(this.baseUrl, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded',
                },
                body: params
            });

            console.log('📡 Clear cart response status:', response.status);

            const responseText = await response.text();
            console.log('📄 Clear cart raw response:', responseText);

            let result;
            try {
                result = JSON.parse(responseText);
            } catch (parseError) {
                console.error('❌ Failed to parse clear cart response:', parseError);
                throw new Error('Server returned invalid JSON');
            }

            if (!response.ok) {
                throw new Error(result.error || 'Failed to clear cart');
            }

            return result;
        } catch (error) {
            console.error('Error clearing cart:', error);
            throw error;
        }
    }
}

// Checkout Process Manager
class CheckoutProcess {
    constructor() {
        this.baseUrl = this.getBaseUrl();
        console.log('💰 CheckoutProcess initialized with URL:', this.baseUrl);
    }

    getBaseUrl() {
        const currentPath = window.location.pathname;
        const appPath = currentPath.substring(0, currentPath.lastIndexOf('/'));
        return window.location.origin + appPath;
    }

    async createOrder(orderData) {
        try {
            console.log('Creating order:', orderData);

            const response = await fetch(this.baseUrl + '/checkout/createOrder', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded',
                },
                body: new URLSearchParams(orderData)
            });

            if (!response.ok) {
                throw new Error(`HTTP ${response.status}: ${response.statusText}`);
            }

            const result = await response.json();

            if (result.success) {
                return result;
            } else {
                throw new Error(result.error || 'Failed to create order');
            }
        } catch (error) {
            console.error('Error creating order:', error);
            throw new Error('Failed to create order: ' + error.message);
        }
    }

    async initiatePayHerePayment(orderId) {
        try {
            console.log('Initiating PayHere payment for order:', orderId);

            const response = await fetch(this.baseUrl + '/checkout/payhere/initiate', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded',
                },
                body: new URLSearchParams({orderId: orderId})
            });

            if (!response.ok) {
                throw new Error(`HTTP ${response.status}: ${response.statusText}`);
            }

            const result = await response.json();

            if (result.success) {
                // Redirect to PayHere payment page
                this.submitToPayHere(result.payhereData, result.paymentUrl);
                return result;
            } else {
                throw new Error(result.error || 'Failed to initiate payment');
            }
        } catch (error) {
            console.error('Error initiating payment:', error);
            throw new Error('Error initiating payment: ' + error.message);
        }
    }

    submitToPayHere(payhereData, paymentUrl) {
        try {
            // Create a form and submit to PayHere
            const form = document.createElement('form');
            form.method = 'POST';
            form.action = paymentUrl;
            form.style.display = 'none';

            Object.keys(payhereData).forEach(key => {
                const input = document.createElement('input');
                input.type = 'hidden';
                input.name = key;
                input.value = payhereData[key];
                form.appendChild(input);
            });

            document.body.appendChild(form);
            console.log('Submitting to PayHere:', payhereData);
            form.submit();
        } catch (error) {
            console.error('Error submitting to PayHere:', error);
            throw error;
        }
    }

    async confirmOrder(orderId) {
        try {
            const response = await fetch(this.baseUrl + '/checkout/confirm', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded',
                },
                body: new URLSearchParams({orderId: orderId})
            });

            if (!response.ok) {
                throw new Error(`HTTP ${response.status}: ${response.statusText}`);
            }

            const result = await response.json();

            if (result.success) {
                return result;
            } else {
                throw new Error(result.error || 'Failed to confirm order');
            }
        } catch (error) {
            console.error('Error confirming order:', error);
            throw new Error('Error confirming order: ' + error.message);
        }
    }

    async getOrderSummary() {
        try {
            const response = await fetch(this.baseUrl + '/checkout/summary');

            if (!response.ok) {
                throw new Error(`HTTP ${response.status}: ${response.statusText}`);
            }

            const result = await response.json();
            return result;
        } catch (error) {
            console.error('Error getting order summary:', error);
            throw new Error('Error getting order summary: ' + error.message);
        }
    }
}

// Toast notification system
class ToastManager {
    static show(message, type = 'info') {
        // Create toast container if it doesn't exist
        let toastContainer = document.getElementById('toastContainer');
        if (!toastContainer) {
            toastContainer = document.createElement('div');
            toastContainer.id = 'toastContainer';
            toastContainer.className = 'toast-container position-fixed top-0 end-0 p-3';
            document.body.appendChild(toastContainer);
        }

        const toastId = 'toast-' + Date.now();

        const toastHtml = `
            <div id="${toastId}" class="toast align-items-center text-white bg-${this.getBgColor(type)} border-0" role="alert">
                <div class="d-flex">
                    <div class="toast-body">
                        <i class="fas ${this.getIcon(type)} me-2"></i>
                        ${message}
                    </div>
                    <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast"></button>
                </div>
            </div>
        `;

        toastContainer.insertAdjacentHTML('beforeend', toastHtml);

        const toastElement = document.getElementById(toastId);
        const toast = new bootstrap.Toast(toastElement, {delay: 3000});
        toast.show();

        // Remove toast from DOM after it's hidden
        toastElement.addEventListener('hidden.bs.toast', () => {
            toastElement.remove();
        });
    }

    static getBgColor(type) {
        const colors = {
            'success': 'success',
            'error': 'danger',
            'warning': 'warning',
            'info': 'info'
        };
        return colors[type] || 'info';
    }

    static getIcon(type) {
        const icons = {
            'success': 'fa-check-circle',
            'error': 'fa-exclamation-circle',
            'warning': 'fa-exclamation-triangle',
            'info': 'fa-info-circle'
        };
        return icons[type] || 'fa-info-circle';
    }
}

// Make classes globally available
window.CartManager = CartManager;
window.ToastManager = ToastManager;
window.CheckoutProcess = CheckoutProcess;

// Initialize session manager when DOM is loaded
document.addEventListener('DOMContentLoaded', function () {
    window.sessionManager = new SessionManager();
    console.log('✅ Session manager initialized');
});

window.addEventListener('load', function () {
    if (!window.sessionManager) {
        window.sessionManager = new SessionManager();
        console.log('✅ Session manager initialized on window load');
    }
});