/**
 * RENTPROOF - AUTHENTICATION CLIENT LOGIC
 * Module: Landlord Authentication
 * Author: Aditi Sharma
 * 
 * Features:
 * - Real-time client-side form validation
 * - Email format & Indian 10-digit phone number checking
 * - Password confirmation validation & show/hide toggle
 * - Standalone demo session state (localStorage persistence)
 * - Clear alerts and error states
 * - Easy API-integration bridge for backend Servlets / REST endpoints
 */

// Regular expressions conforming to Core Java AuthService patterns
const EMAIL_REGEX = /^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/;
const PHONE_REGEX = /^[6-9]\d{9}$/;

// Pre-seeded Demo Landlord Accounts (matches database/rentproof.sql)
const DEFAULT_DEMO_ACCOUNTS = [
  {
    landlordId: 1,
    fullName: "Aditi Sharma",
    email: "aditi.landlord@rentproof.com",
    phone: "9876543210",
    password: "RentProof@123"
  },
  {
    landlordId: 2,
    fullName: "Rajesh Kumar Verma",
    email: "rajesh.verma@gmail.com",
    phone: "9811223344",
    password: "RentProof@123"
  }
];

// Initialize on DOM ready
document.addEventListener("DOMContentLoaded", () => {
  setupPasswordToggles();
  setupLoginForm();
  setupRegisterForm();
  setupDemoQuickFill();
});

/**
 * Toggles password visibility between 'password' and 'text'.
 */
function setupPasswordToggles() {
  const toggleButtons = document.querySelectorAll(".password-toggle-btn");
  toggleButtons.forEach((btn) => {
    btn.addEventListener("click", () => {
      const targetInputId = btn.getAttribute("data-target");
      const targetInput = document.getElementById(targetInputId);
      if (targetInput) {
        if (targetInput.type === "password") {
          targetInput.type = "text";
          btn.textContent = "🙈 Hide";
        } else {
          targetInput.type = "password";
          btn.textContent = "👁️ Show";
        }
      }
    });
  });
}

/**
 * Handles Landlord Login Form Validation and Submission.
 */
function setupLoginForm() {
  const loginForm = document.getElementById("loginForm");
  if (!loginForm) return;

  loginForm.addEventListener("submit", (e) => {
    e.preventDefault();
    clearAlert("loginAlert");

    const emailInput = document.getElementById("loginEmail");
    const passwordInput = document.getElementById("loginPassword");

    let isValid = true;

    // Validate Email
    if (!emailInput.value.trim()) {
      showFieldError("loginEmailError", "Email address is required.");
      emailInput.classList.add("is-invalid");
      isValid = false;
    } else if (!EMAIL_REGEX.test(emailInput.value.trim())) {
      showFieldError("loginEmailError", "Please enter a valid email address (e.g. aditi@rentproof.com).");
      emailInput.classList.add("is-invalid");
      isValid = false;
    } else {
      clearFieldError("loginEmailError");
      emailInput.classList.remove("is-invalid");
    }

    // Validate Password
    if (!passwordInput.value) {
      showFieldError("loginPasswordError", "Password is required.");
      passwordInput.classList.add("is-invalid");
      isValid = false;
    } else {
      clearFieldError("loginPasswordError");
      passwordInput.classList.remove("is-invalid");
    }

    if (!isValid) return;

    const email = emailInput.value.trim().toLowerCase();
    const password = passwordInput.value;

    // Check credentials against local user accounts or demo seed
    const registeredUsers = JSON.parse(localStorage.getItem("rentproof_registered_users")) || DEFAULT_DEMO_ACCOUNTS;
    const matchedUser = registeredUsers.find(
      (u) => u.email.toLowerCase() === email && u.password === password
    );

    if (matchedUser) {
      showAlert("loginAlert", "Login successful! Redirecting to Properties Dashboard...", "success");

      // Save active session
      localStorage.setItem("rentproof_user", JSON.stringify({
        landlordId: matchedUser.landlordId || 1,
        fullName: matchedUser.fullName,
        email: matchedUser.email,
        phone: matchedUser.phone
      }));

      setTimeout(() => {
        window.location.href = "../properties/properties.html";
      }, 900);
    } else {
      showAlert(
        "loginAlert",
        "Invalid email or password. Please verify your credentials or use the Demo Quick-Fill below.",
        "error"
      );
    }
  });
}

/**
 * Handles Landlord Registration Form Validation and Submission.
 */
function setupRegisterForm() {
  const registerForm = document.getElementById("registerForm");
  if (!registerForm) return;

  registerForm.addEventListener("submit", (e) => {
    e.preventDefault();
    clearAlert("registerAlert");

    const nameInput = document.getElementById("regFullName");
    const emailInput = document.getElementById("regEmail");
    const phoneInput = document.getElementById("regPhone");
    const passInput = document.getElementById("regPassword");
    const confirmInput = document.getElementById("regConfirmPassword");

    let isValid = true;

    // 1. Full Name
    if (!nameInput.value.trim() || nameInput.value.trim().length < 2) {
      showFieldError("regNameError", "Full Name must be at least 2 characters.");
      nameInput.classList.add("is-invalid");
      isValid = false;
    } else {
      clearFieldError("regNameError");
      nameInput.classList.remove("is-invalid");
    }

    // 2. Email
    if (!emailInput.value.trim()) {
      showFieldError("regEmailError", "Email address is required.");
      emailInput.classList.add("is-invalid");
      isValid = false;
    } else if (!EMAIL_REGEX.test(emailInput.value.trim())) {
      showFieldError("regEmailError", "Please enter a valid email format.");
      emailInput.classList.add("is-invalid");
      isValid = false;
    } else {
      clearFieldError("regEmailError");
      emailInput.classList.remove("is-invalid");
    }

    // 3. Phone (10 digits Indian format)
    if (!phoneInput.value.trim()) {
      showFieldError("regPhoneError", "Phone number is required.");
      phoneInput.classList.add("is-invalid");
      isValid = false;
    } else if (!PHONE_REGEX.test(phoneInput.value.trim())) {
      showFieldError("regPhoneError", "Must be a valid 10-digit Indian mobile number (e.g. 9876543210).");
      phoneInput.classList.add("is-invalid");
      isValid = false;
    } else {
      clearFieldError("regPhoneError");
      phoneInput.classList.remove("is-invalid");
    }

    // 4. Password
    if (!passInput.value || passInput.value.length < 6) {
      showFieldError("regPasswordError", "Password must be at least 6 characters long.");
      passInput.classList.add("is-invalid");
      isValid = false;
    } else {
      clearFieldError("regPasswordError");
      passInput.classList.remove("is-invalid");
    }

    // 5. Password Confirmation
    if (passInput.value !== confirmInput.value) {
      showFieldError("regConfirmError", "Passwords do not match.");
      confirmInput.classList.add("is-invalid");
      isValid = false;
    } else {
      clearFieldError("regConfirmError");
      confirmInput.classList.remove("is-invalid");
    }

    if (!isValid) return;

    // Check duplicate email
    const registeredUsers = JSON.parse(localStorage.getItem("rentproof_registered_users")) || [...DEFAULT_DEMO_ACCOUNTS];
    const exists = registeredUsers.some((u) => u.email.toLowerCase() === emailInput.value.trim().toLowerCase());

    if (exists) {
      showAlert("registerAlert", "An account with this email already exists. Please login instead.", "error");
      return;
    }

    // Create new landlord
    const newLandlord = {
      landlordId: Date.now(),
      fullName: nameInput.value.trim(),
      email: emailInput.value.trim().toLowerCase(),
      phone: phoneInput.value.trim(),
      password: passInput.value // In real backend, Java AuthService hashes this using SHA-256
    };

    registeredUsers.push(newLandlord);
    localStorage.setItem("rentproof_registered_users", JSON.stringify(registeredUsers));

    showAlert("registerAlert", "Account created successfully! Redirecting to login...", "success");

    setTimeout(() => {
      window.location.href = "login.html?registered=true";
    }, 1200);
  });
}

/**
 * Fills demo credentials for quick faculty evaluation.
 */
function setupDemoQuickFill() {
  const quickFillBtn = document.getElementById("quickFillBtn");
  if (!quickFillBtn) return;

  quickFillBtn.addEventListener("click", () => {
    const emailInput = document.getElementById("loginEmail");
    const passwordInput = document.getElementById("loginPassword");
    if (emailInput && passwordInput) {
      emailInput.value = "aditi.landlord@rentproof.com";
      passwordInput.value = "RentProof@123";
      clearFieldError("loginEmailError");
      clearFieldError("loginPasswordError");
      emailInput.classList.remove("is-invalid");
      passwordInput.classList.remove("is-invalid");
    }
  });
}

/**
 * Forgot password modal / notification placeholder.
 */
function handleForgotPassword() {
  const emailInput = document.getElementById("loginEmail");
  const email = emailInput && emailInput.value.trim() ? emailInput.value.trim() : "your registered email";
  alert("Password Reset Request:\nA password recovery link has been prepared for " + email + ".\n(In production, an automated reset token is sent via email.)");
}

// Utility Helpers
function showFieldError(elementId, message) {
  const el = document.getElementById(elementId);
  if (el) {
    el.textContent = message;
    el.classList.add("visible");
  }
}

function clearFieldError(elementId) {
  const el = document.getElementById(elementId);
  if (el) {
    el.textContent = "";
    el.classList.remove("visible");
  }
}

function showAlert(containerId, message, type) {
  const container = document.getElementById(containerId);
  if (!container) return;
  container.className = `alert alert-${type}`;
  container.textContent = message;
  container.style.display = "flex";
}

function clearAlert(containerId) {
  const container = document.getElementById(containerId);
  if (!container) return;
  container.style.display = "none";
  container.textContent = "";
}
