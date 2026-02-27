// Checkout Manager for Web_Project_1
class CheckoutManager {
    constructor() {
        this.contextPath = '/Web_Project_1';
        this.baseUrl = window.location.origin + this.contextPath;
        this.cartItems = [];
        this.totals = {};
        
        console.log('🔧 Context Path:', this.contextPath);
        console.log('🔧 Base URL:', this.baseUrl);
    }

    // Load checkout summary from backend
    async loadCheckoutSummary() {
        try {
            console.log('🔄 Loading checkout summary from:', this.contextPath + '/checkout/summary');
            
            const response = await fetch(this.contextPath + '/checkout/summary', {
                method: 'GET',
                headers: {
                    'Content-Type': 'application/json',
                },
                credentials: 'include'
            });

            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }

            const data = await response.json();
            console.log('📦 Checkout summary data:', data);
            
            if (data.items && Array.isArray(data.items)) {
                this.cartItems = data.items;
                this.totals = data.totals || {};
                this.displayOrderItems();
                this.updateOrderTotals();
                return data;
            } else {
                throw new Error('Invalid cart data structure');
            }
            
        } catch (error) {
            console.error('❌ Error loading checkout summary:', error);
            this.showEmptyCart();
            throw error;
        }
    }

    // Display order items in the summary
    displayOrderItems() {
        const container = document.getElementById('orderItems');
        
        if (!container) {
            console.error('❌ Order items container not found');
            return;
        }

        if (!this.cartItems || this.cartItems.length === 0) {
            this.showEmptyCart();
            return;
        }

        let html = '';
        
        this.cartItems.forEach((item, index) => {
            const productName = item.productName || 'Unnamed Product';
            const price = parseFloat(item.price) || 0;
            const quantity = parseInt(item.quantity) || 1;
            const subtotal = parseFloat(item.subtotal) || (price * quantity);
            const imageUrl = item.imageUrl || item.productImage || 
                           'https://via.placeholder.com/80?text=No+Image';

            html += `
                <div class="order-item mb-3 pb-3 border-bottom">
                    <div class="row align-items-center">
                        <div class="col-3 col-md-2">
                            <img src="${imageUrl}" 
                                 class="img-fluid rounded" 
                                 alt="${productName}"
                                 style="width: 60px; height: 60px; object-fit: cover;">
                        </div>
                        <div class="col-6 col-md-7">
                            <h6 class="mb-1">${productName}</h6>
                            <p class="text-muted mb-0 small">$${price.toFixed(2)} × ${quantity}</p>
                        </div>
                        <div class="col-3 col-md-3 text-end">
                            <span class="fw-bold text-primary">$${subtotal.toFixed(2)}</span>
                        </div>
                    </div>
                </div>
            `;
        });

        container.innerHTML = html;
        console.log('✅ Order items displayed:', this.cartItems.length);
    }

    // Update order totals
    updateOrderTotals() {
        const subtotal = parseFloat(this.totals.subtotal) || 0;
        const shipping = parseFloat(this.totals.shipping) || 5.99;
        const tax = parseFloat(this.totals.tax) || (subtotal * 0.1);
        const total = parseFloat(this.totals.total) || (subtotal + shipping + tax);

        // Update DOM elements
        this.updateElement('subtotal', `$${subtotal.toFixed(2)}`);
        this.updateElement('shipping', `$${shipping.toFixed(2)}`);
        this.updateElement('tax', `$${tax.toFixed(2)}`);
        this.updateElement('total', `$${total.toFixed(2)}`);

        console.log('💰 Totals updated - Subtotal:', subtotal, 'Total:', total);
    }

    // Helper to update DOM elements
    updateElement(id, value) {
        const element = document.getElementById(id);
        if (element) {
            element.textContent = value;
        }
    }

    // Show empty cart state
    showEmptyCart() {
        const container = document.getElementById('orderItems');
        if (container) {
            container.innerHTML = `
                <div class="text-center py-4">
                    <i class="fas fa-shopping-cart fa-2x text-muted mb-3"></i>
                    <h5 class="text-muted">Your cart is empty</h5>
                    <p class="text-muted">Add some products to proceed with checkout</p>
                    <a href="products.html" class="btn btn-primary btn-sm">
                        <i class="fas fa-shopping-bag me-2"></i>Continue Shopping
                    </a>
                </div>
            `;
        }
        
        // Reset totals to zero
        this.updateElement('subtotal', '$0.00');
        this.updateElement('shipping', '$0.00');
        this.updateElement('tax', '$0.00');
        this.updateElement('total', '$0.00');
    }

    // Create order in backend
    async createOrder(shippingData) {
        try {
            console.log('🔄 Creating order...', shippingData);
            
            const response = await fetch(this.contextPath + '/checkout/createOrder', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded',
                },
                credentials: 'include',
                body: new URLSearchParams({
                    shippingAddress: JSON.stringify(shippingData),
                    paymentMethod: document.querySelector('input[name="paymentMethod"]:checked').value,
                    totalAmount: this.totals.total || '0'
                })
            });

            const result = await response.json();
            console.log('📦 Order creation result:', result);
            
            if (result.success) {
                return result;
            } else {
                throw new Error(result.error || 'Failed to create order');
            }
            
        } catch (error) {
            console.error('❌ Error creating order:', error);
            throw error;
        }
    }
}

// Process payment based on selected method - SIMPLIFIED VERSION
async function processPayment() {
    try {
        console.log('💰 Processing payment...');
        
        const paymentMethod = document.querySelector('input[name="paymentMethod"]:checked').value;
        console.log('Selected payment method:', paymentMethod);

        // Collect shipping data
        const shippingData = {
            firstName: document.getElementById('firstName').value,
            lastName: document.getElementById('lastName').value,
            email: document.getElementById('email').value,
            phone: document.getElementById('phone').value,
            address: document.getElementById('address').value,
            city: document.getElementById('city').value,
            postalCode: document.getElementById('postalCode').value,
            country: document.getElementById('country').value
        };

        console.log('✅ Creating order with shipping data:', shippingData);

        // Create order first
        const orderResult = await window.checkoutManager.createOrder(shippingData);
        
        if (paymentMethod === 'payhere') {
            await processPayHerePayment(orderResult);
        } else if (paymentMethod === 'cod') {
            await processCashOnDelivery(orderResult);
        }
        
    } catch (error) {
        console.error('❌ Payment processing error:', error);
        if (window.ToastManager) {
            window.ToastManager.show('Payment failed: ' + error.message, 'error');
        }
    }
}

// Process PayHere payment
async function processPayHerePayment(orderData) {
    try {
        console.log('🔗 Initiating PayHere payment for order:', orderData.orderId);
        
        const response = await fetch('/Web_Project_1/checkout/payhere/initiate', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded',
            },
            credentials: 'include',
            body: new URLSearchParams({
                orderId: orderData.orderId
            })
        });

        const result = await response.json();
        console.log('🔗 PayHere initiation result:', result);
        
        if (result.success && result.payhereData) {
            console.log('✅ Redirecting to PayHere...');
            
            // Create and submit form to PayHere
            const form = document.createElement('form');
            form.method = 'POST';
            form.action = result.paymentUrl;
            form.style.display = 'none';
            
            // Add all PayHere parameters
            Object.keys(result.payhereData).forEach(key => {
                const input = document.createElement('input');
                input.type = 'hidden';
                input.name = key;
                input.value = result.payhereData[key];
                form.appendChild(input);
            });
            
            document.body.appendChild(form);
            form.submit();
            
        } else {
            throw new Error(result.error || 'Failed to initiate PayHere payment');
        }
    } catch (error) {
        console.error('❌ PayHere payment error:', error);
        throw error;
    }
}

// Process Cash on Delivery
async function processCashOnDelivery(orderData) {
    try {
        console.log('💵 Processing Cash on Delivery for order:', orderData.orderId);
        
        const response = await fetch('/Web_Project_1/payment/finalize', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded',
            },
            credentials: 'include',
            body: new URLSearchParams({
                orderId: orderData.orderId,
                paymentMethod: 'cod',
                paymentId: 'cod_' + Date.now()
            })
        });

        const result = await response.json();
        console.log('💵 COD result:', result);
        
        if (result.success) {
            if (window.ToastManager) {
                window.ToastManager.show('Order placed successfully! You will pay when delivered.', 'success');
            }
            // Redirect to order confirmation page
            setTimeout(() => {
                window.location.href = 'order-success.html?orderId=' + orderData.orderId;
            }, 2000);
        } else {
            throw new Error(result.error || 'Failed to place COD order');
        }
    } catch (error) {
        console.error('❌ COD processing error:', error);
        throw error;
    }
}

// Global functions for checkout flow
function proceedToPayment() {
    console.log('➡️ Proceeding to payment...');
    
    // Validate shipping form
    const shippingForm = document.getElementById('shippingForm');
    if (!shippingForm.checkValidity()) {
        shippingForm.classList.add('was-validated');
        if (window.ToastManager) {
            window.ToastManager.show('Please fill all required shipping information', 'error');
        }
        return;
    }

    // Collect shipping data
    const shippingData = {
        firstName: document.getElementById('firstName').value,
        lastName: document.getElementById('lastName').value,
        email: document.getElementById('email').value,
        phone: document.getElementById('phone').value,
        address: document.getElementById('address').value,
        city: document.getElementById('city').value,
        postalCode: document.getElementById('postalCode').value,
        country: document.getElementById('country').value
    };

    // Show payment section, hide shipping section
    document.getElementById('shippingSection').style.display = 'none';
    document.getElementById('paymentSection').style.display = 'block';
    
    // Update checkout steps
    updateCheckoutSteps(2);
    
    console.log('✅ Shipping data collected:', shippingData);
}

function backToShipping() {
    console.log('⬅️ Going back to shipping...');
    
    document.getElementById('paymentSection').style.display = 'none';
    document.getElementById('shippingSection').style.display = 'block';
    updateCheckoutSteps(1);
}

function updateCheckoutSteps(activeStep) {
    const steps = document.querySelectorAll('.checkout-steps .step');
    steps.forEach((step, index) => {
        if (index + 1 === activeStep) {
            step.classList.add('active');
        } else {
            step.classList.remove('active');
        }
    });
}

// Payment method change handler
function setupPaymentMethodHandlers() {
    const paymentMethods = document.querySelectorAll('input[name="paymentMethod"]');
    const payhereDetails = document.getElementById('payhereDetails');
    const codDetails = document.getElementById('codDetails');

    paymentMethods.forEach(method => {
        method.addEventListener('change', function() {
            if (this.value === 'payhere') {
                payhereDetails.style.display = 'block';
                codDetails.style.display = 'none';
            } else if (this.value === 'cod') {
                payhereDetails.style.display = 'none';
                codDetails.style.display = 'block';
            }
        });
    });
}

// Initialize checkout when page loads
document.addEventListener('DOMContentLoaded', function() {
    console.log('🚀 Checkout page initialized for Web_Project_1');
    
    // Initialize checkout manager
    window.checkoutManager = new CheckoutManager();
    
    // Setup payment method handlers
    setupPaymentMethodHandlers();
    
    // Load checkout summary
    setTimeout(() => {
        window.checkoutManager.loadCheckoutSummary()
            .then(() => console.log('✅ Checkout summary loaded successfully'))
            .catch(error => {
                console.error('❌ Failed to load checkout summary:', error);
                if (window.ToastManager) {
                    window.ToastManager.show('Failed to load cart items: ' + error.message, 'error');
                }
            });
    }, 500);
    
    // Make functions global
    window.proceedToPayment = proceedToPayment;
    window.backToShipping = backToShipping;
    window.processPayment = processPayment;
});