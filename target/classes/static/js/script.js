/**
 * CAR RENTAL SYSTEM - FRONTEND SCRIPT
 * Dynamic fare calculation and client-side validation
 */

document.addEventListener('DOMContentLoaded', function () {
    initBookingCalculator();
    initLoginFormHelpers();
});

/**
 * BR1 & BR4 Dynamic Fare Calculator
 */
function initBookingCalculator() {
    const quantityInput = document.getElementById('bookingQuantity');
    const rateElement = document.getElementById('vehicleRate');
    
    if (!quantityInput || !rateElement) return;

    const rate = parseFloat(rateElement.dataset.rate || rateElement.textContent || '0');
    const baseTotalElem = document.getElementById('calcBaseTotal');
    const bookingChargeElem = document.getElementById('calcBookingCharge');
    const grandTotalElem = document.getElementById('calcGrandTotal');
    const quantityErrorElem = document.getElementById('quantityError');
    const submitBtn = document.getElementById('submitBookingBtn');

    function recalculate() {
        let qty = parseInt(quantityInput.value, 10);

        if (isNaN(qty) || qty < 1 || qty > 5) {
            if (quantityErrorElem) {
                quantityErrorElem.textContent = 'Quantity must be between 1 and 5.';
                quantityErrorElem.style.display = 'block';
            }
            if (submitBtn) submitBtn.disabled = true;
            return;
        } else {
            if (quantityErrorElem) {
                quantityErrorElem.style.display = 'none';
            }
            if (submitBtn) submitBtn.disabled = false;
        }

        const baseTotal = rate * qty;
        // BR4: Booking Charge rule
        const bookingCharge = baseTotal < 1000 ? 50 : 0;
        const grandTotal = baseTotal + bookingCharge;

        if (baseTotalElem) baseTotalElem.textContent = '₹' + baseTotal.toFixed(2);
        if (bookingChargeElem) bookingChargeElem.textContent = '₹' + bookingCharge.toFixed(2);
        if (grandTotalElem) grandTotalElem.textContent = '₹' + grandTotal.toFixed(2);
    }

    quantityInput.addEventListener('input', recalculate);
    quantityInput.addEventListener('change', recalculate);
    // Initial calculation on page load
    recalculate();

    // Form client-side validation
    const bookingForm = document.getElementById('bookingForm');
    if (bookingForm) {
        bookingForm.addEventListener('submit', function (e) {
            const qty = parseInt(quantityInput.value, 10);
            if (isNaN(qty) || qty < 1 || qty > 5) {
                e.preventDefault();
                alert('Quantity must be between 1 and 5.');
                return;
            }

            const phoneInput = document.getElementById('customerPhone');
            if (phoneInput && !/^[0-9]{10}$/.test(phoneInput.value.trim())) {
                e.preventDefault();
                alert('Phone number must contain exactly 10 digits.');
                phoneInput.focus();
                return;
            }
        });
    }
}

/**
 * Convenient 1-click login filler for evaluation/viva demo
 */
function initLoginFormHelpers() {
    window.fillCredentials = function (user, pass) {
        const uInput = document.getElementById('username');
        const pInput = document.getElementById('password');
        if (uInput && pInput) {
            uInput.value = user;
            pInput.value = pass;
        }
    };
}
