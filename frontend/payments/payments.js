/**
 * RENTPROOF - PAYMENT MANAGEMENT MODULE (AANVI)
 * Vanilla JavaScript (No Frameworks)
 * 
 * Features:
 * - Dynamic Backend API connectivity with graceful offline localStorage fallback
 * - Seed dataset matching database/rentproof.sql exactly
 * - Real-time statistics (Total Collected, Successful, Pending, Overdue)
 * - Add Payment workflow with cascading Tenant -> Property -> Room auto-fill
 * - Interactive Real-Time Receipt Preview during payment entry
 * - Multi-criteria search and filter on Payment History
 * - View Digital Official Receipt Modal with print capability
 * - Quick Status Update & Delete confirmation workflows
 * - Sidebar collapse & Landlord session management
 */

// API Base URL (Relative for server, or fallback to port 8080)
const API_BASE = window.location.origin.includes(":8080") ? "" : "http://localhost:8080";

// Seed Datasets (Strictly synchronized with database/rentproof.sql)
const SEED_PROPERTIES = [
  { propertyId: 1, propertyName: "Sunrise Executive PG", propertyType: "PG", city: "Greater Noida" },
  { propertyId: 2, propertyName: "Ivory Heights 3BHK Flat", propertyType: "Flat", city: "Noida" },
  { propertyId: 3, propertyName: "Blue Ridge Residency", propertyType: "Apartment", city: "Noida" }
];

const SEED_ROOMS = [
  { roomId: 1, propertyId: 1, roomNumber: "101", monthlyRent: 8500, roomStatus: "Occupied" },
  { roomId: 2, propertyId: 1, roomNumber: "102", monthlyRent: 8500, roomStatus: "Occupied" },
  { roomId: 3, propertyId: 1, roomNumber: "103", monthlyRent: 8500, roomStatus: "Vacant" },
  { roomId: 7, propertyId: 2, roomNumber: "Room-A (Master)", monthlyRent: 11000, roomStatus: "Occupied" },
  { roomId: 8, propertyId: 2, roomNumber: "Room-B (Guest)", monthlyRent: 9000, roomStatus: "Vacant" },
  { roomId: 10, propertyId: 3, roomNumber: "B-101", monthlyRent: 8500, roomStatus: "Vacant" },
  { roomId: 11, propertyId: 3, roomNumber: "B-102", monthlyRent: 8500, roomStatus: "Occupied" }
];

const SEED_TENANTS = [
  {
    tenantId: 1,
    fullName: "Rohan Verma",
    phone: "9988776655",
    propertyId: 1,
    propertyName: "Sunrise Executive PG",
    roomId: 1,
    roomNumber: "101",
    monthlyRent: 8500
  },
  {
    tenantId: 2,
    fullName: "Ankit Mishra",
    phone: "9871122334",
    propertyId: 1,
    propertyName: "Sunrise Executive PG",
    roomId: 2,
    roomNumber: "102",
    monthlyRent: 8500
  },
  {
    tenantId: 3,
    fullName: "Sneha Kapoor",
    phone: "9711334455",
    propertyId: 2,
    propertyName: "Ivory Heights 3BHK Flat",
    roomId: 7,
    roomNumber: "Room-A (Master)",
    monthlyRent: 11000
  }
];

const SEED_PAYMENTS = [
  {
    paymentId: 1,
    tenantId: 1,
    tenantName: "Rohan Verma",
    propertyId: 1,
    propertyName: "Sunrise Executive PG",
    roomId: 1,
    roomNumber: "101",
    amount: 8500.0,
    paymentDate: "2026-11-05",
    paymentMonth: "November 2026",
    paymentMode: "UPI",
    transactionRef: "UPI/20261105/98218731",
    paymentStatus: "Paid",
    remarks: "Rent received via Google Pay"
  },
  {
    paymentId: 2,
    tenantId: 2,
    tenantName: "Ankit Mishra",
    propertyId: 1,
    propertyName: "Sunrise Executive PG",
    roomId: 2,
    roomNumber: "102",
    amount: 8500.0,
    paymentDate: "2026-11-04",
    paymentMonth: "November 2026",
    paymentMode: "Bank Transfer",
    transactionRef: "NEFT/HDFC/001928371",
    paymentStatus: "Paid",
    remarks: "On-time monthly rental"
  },
  {
    paymentId: 3,
    tenantId: 3,
    tenantName: "Sneha Kapoor",
    propertyId: 2,
    propertyName: "Ivory Heights 3BHK Flat",
    roomId: 7,
    roomNumber: "Room-A (Master)",
    amount: 11000.0,
    paymentDate: "2026-11-02",
    paymentMonth: "November 2026",
    paymentMode: "UPI",
    transactionRef: "UPI/20261102/44129031",
    paymentStatus: "Paid",
    remarks: "Direct payment received"
  },
  {
    paymentId: 4,
    tenantId: 1,
    tenantName: "Rohan Verma",
    propertyId: 1,
    propertyName: "Sunrise Executive PG",
    roomId: 1,
    roomNumber: "101",
    amount: 8500.0,
    paymentDate: "2026-12-05",
    paymentMonth: "December 2026",
    paymentMode: "UPI",
    transactionRef: "PENDING-DUE-01",
    paymentStatus: "Pending",
    remarks: "Upcoming cycle invoice generated"
  },
  {
    paymentId: 5,
    tenantId: 2,
    tenantName: "Ankit Mishra",
    propertyId: 1,
    propertyName: "Sunrise Executive PG",
    roomId: 2,
    roomNumber: "102",
    amount: 8500.0,
    paymentDate: "2026-10-05",
    paymentMonth: "October 2026",
    paymentMode: "Cash",
    transactionRef: "CASH-REC-OCT26",
    paymentStatus: "Paid",
    remarks: "Physical cash payment handed in person"
  }
];

// Document Initialization
document.addEventListener("DOMContentLoaded", async () => {
  if (!(await window.RentProofSecurity.requireSession())) return;
  setupNavigationAndSidebar();
  setupActiveUser();

  if (document.getElementById("paymentsDashboardView")) {
    initPaymentsDashboard();
  } else if (document.getElementById("addPaymentForm")) {
    initAddPaymentForm();
  } else if (document.getElementById("paymentHistoryView")) {
    initPaymentHistory();
  }
});

/* ==============================================================================
   DATA ACCESS LAYER (API First with LocalStorage Fallback)
   ============================================================================== */

/**
 * Loads all payments asynchronously from live Java backend API.
 */
async function loadPayments() {
  try {
    const res = await fetch(`${API_BASE}/api/payments`);
    if (res.ok) {
      const data = await res.json();
      if (Array.isArray(data)) {
        saveLocalPayments(data);
        return data;
      }
    }
  } catch (err) {
    console.error("[PaymentAPI] Failed to fetch payments from backend:", err);
  }
  return getLocalPayments();
}

/**
 * Loads dashboard statistics directly from MySQL aggregation.
 */
async function loadDashboardStats() {
  try {
    const res = await fetch(`${API_BASE}/api/payments/stats`);
    if (res.ok) {
      const stats = await res.json();
      return {
        totalRentCollected: stats.totalCollected || 0,
        successfulPaymentCount: stats.successful || 0,
        pendingPaymentCount: stats.pending || 0,
        overduePaymentCount: stats.overdue || 0
      };
    }
  } catch (err) {
    console.error("[PaymentAPI] Failed to fetch stats from backend:", err);
  }

  const payments = getLocalPayments();
  let totalCollected = 0;
  let successful = 0;
  let pending = 0;
  let overdue = 0;

  payments.forEach((p) => {
    if (p.paymentStatus === "Paid") {
      totalCollected += Number(p.amount) || 0;
      successful++;
    } else if (p.paymentStatus === "Pending") {
      pending++;
    } else if (p.paymentStatus === "Overdue") {
      overdue++;
    }
  });

  return {
    totalRentCollected: totalCollected,
    successfulPaymentCount: successful,
    pendingPaymentCount: pending,
    overduePaymentCount: overdue
  };
}

/**
 * Loads active tenants list from live MySQL database.
 */
async function loadTenants() {
  try {
    const res = await fetch(`${API_BASE}/api/tenants`);
    if (res.ok) {
      const data = await res.json();
      if (Array.isArray(data)) return data;
    }
  } catch (err) {
    console.error("[PaymentAPI] Failed to fetch tenants:", err);
  }
  return [...SEED_TENANTS];
}

/**
 * Loads properties list from live MySQL database.
 */
async function loadProperties() {
  try {
    const res = await fetch(`${API_BASE}/api/properties`);
    if (res.ok) {
      const data = await res.json();
      if (Array.isArray(data)) return data;
    }
  } catch (err) {
    console.error("[PaymentAPI] Failed to fetch properties:", err);
  }
  return [...SEED_PROPERTIES];
}

/**
 * Loads rooms by property from live MySQL database.
 */
async function loadRooms(propertyId) {
  try {
    const res = await fetch(`${API_BASE}/api/rooms?propertyId=${propertyId}`);
    if (res.ok) {
      const data = await res.json();
      if (Array.isArray(data)) return data;
    }
  } catch (err) {
    console.error("[PaymentAPI] Failed to fetch rooms:", err);
  }
  return SEED_ROOMS.filter((r) => Number(r.propertyId) === Number(propertyId));
}

/**
 * Saves a new payment record directly into MySQL.
 */
async function createPayment(paymentData) {
  const res = await fetch(`${API_BASE}/api/payments/add`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(paymentData)
  });

  const resp = await res.json().catch(() => ({}));
  if (!res.ok || !resp.success) {
    throw new Error(resp.error || "Failed to record payment in database.");
  }

  const generatedId = resp.paymentId;
  const payments = getLocalPayments();
  const newRecord = {
    ...paymentData,
    paymentId: generatedId,
    amount: parseFloat(paymentData.amount)
  };
  payments.unshift(newRecord);
  saveLocalPayments(payments);

  return { success: true, paymentId: generatedId, backendSynced: true };
}

/**
 * Updates payment status directly in MySQL.
 */
async function updatePaymentStatus(paymentId, newStatus) {
  const res = await fetch(`${API_BASE}/api/payments/update-status`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ paymentId, status: newStatus })
  });

  const resp = await res.json().catch(() => ({}));
  if (!res.ok || !resp.success) {
    throw new Error(resp.error || "Failed to update payment status in database.");
  }

  const payments = getLocalPayments();
  const target = payments.find((p) => p.paymentId === paymentId);
  if (target) {
    target.paymentStatus = newStatus;
    saveLocalPayments(payments);
  }
  return true;
}

/**
 * Deletes a payment record directly from MySQL.
 */
async function deletePayment(paymentId) {
  const res = await fetch(`${API_BASE}/api/payments/delete`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ paymentId })
  });

  const resp = await res.json().catch(() => ({}));
  if (!res.ok || !resp.success) {
    throw new Error(resp.error || "Failed to delete payment from database.");
  }

  const payments = getLocalPayments();
  const filtered = payments.filter((p) => p.paymentId !== paymentId);
  saveLocalPayments(filtered);
  return true;
}

// LocalStorage helpers
function getLocalPayments() {
  const stored = localStorage.getItem("rentproof_payments");
  if (stored) {
    try {
      return JSON.parse(stored);
    } catch (e) {
      console.error("Failed to parse stored payments:", e);
    }
  }
  saveLocalPayments(SEED_PAYMENTS);
  return [...SEED_PAYMENTS];
}

function saveLocalPayments(payments) {
  localStorage.setItem("rentproof_payments", JSON.stringify(payments));
}

/* ==============================================================================
   PAGE 1: PAYMENTS DASHBOARD (payments.html)
   ============================================================================== */

async function initPaymentsDashboard() {
  await renderDashboardStats();
  await renderRecentPaymentsTable();
}

async function renderDashboardStats() {
  const stats = await loadDashboardStats();

  const totalEl = document.getElementById("statTotalCollected");
  const successEl = document.getElementById("statSuccessfulCount");
  const pendingEl = document.getElementById("statPendingCount");
  const overdueEl = document.getElementById("statOverdueCount");

  if (totalEl) totalEl.textContent = formatCurrency(stats.totalRentCollected || 0);
  if (successEl) successEl.textContent = stats.successfulPaymentCount || 0;
  if (pendingEl) pendingEl.textContent = stats.pendingPaymentCount || 0;
  if (overdueEl) overdueEl.textContent = stats.overduePaymentCount || 0;
}

async function renderRecentPaymentsTable() {
  const tbody = document.getElementById("recentPaymentsTableBody");
  if (!tbody) return;

  const payments = await loadPayments();
  const recent = payments.slice(0, 5); // top 5 recent

  if (recent.length === 0) {
    tbody.innerHTML = `
      <tr>
        <td colspan="7" class="empty-state">
          <div class="empty-state-icon">💳</div>
          <p>No payment records found. Click <strong>Record Payment</strong> to add your first transaction.</p>
        </td>
      </tr>
    `;
    return;
  }

  tbody.innerHTML = recent
    .map(
      (p) => `
    <tr>
      <td><span class="payment-id-tag">#PAY-${String(p.paymentId).padStart(4, "0")}</span></td>
      <td>
        <div style="font-weight: 600; color: var(--navy);">${escapeHtml(p.tenantName || "Tenant #" + p.tenantId)}</div>
        <div style="font-size: 0.78rem; color: var(--slate-muted);">${escapeHtml(p.propertyName || "")} &bull; ${escapeHtml(p.roomNumber || "")}</div>
      </td>
      <td><span class="payment-amount">${formatCurrency(p.amount)}</span></td>
      <td>${formatDate(p.paymentDate)}</td>
      <td><span class="badge-mode">${getModeIcon(p.paymentMode)} ${escapeHtml(p.paymentMode)}</span></td>
      <td><span class="badge-status status-${(p.paymentStatus || "").toLowerCase()}">${escapeHtml(p.paymentStatus)}</span></td>
      <td>
        <div class="table-actions">
          <button class="btn-table-action" onclick="viewPaymentReceipt(${p.paymentId})" title="View Official Receipt">📄 Receipt</button>
          <button class="btn-table-action" onclick="openStatusModal(${p.paymentId}, '${p.paymentStatus}')" title="Change Status">✏️ Status</button>
        </div>
      </td>
    </tr>
  `
    )
    .join("");
}

/* ==============================================================================
   PAGE 2: RECORD NEW PAYMENT (add-payment.html)
   ============================================================================== */

let allTenants = [];
let allProperties = [];

async function initAddPaymentForm() {
  allTenants = await loadTenants();
  allProperties = await loadProperties();

  // Populate Tenant Dropdown
  const tenantSelect = document.getElementById("tenantSelect");
  if (tenantSelect) {
    tenantSelect.innerHTML = `<option value="">-- Choose Tenant --</option>` +
      allTenants
        .map(
          (t) => `<option value="${t.tenantId}" data-prop="${t.propertyId}" data-room="${t.roomId}" data-rent="${t.monthlyRent}">
            ${escapeHtml(t.fullName)} (${escapeHtml(t.propertyName)} - Room ${escapeHtml(t.roomNumber)})
          </option>`
        )
        .join("");

    // Auto-fill and cascade on tenant selection
    tenantSelect.addEventListener("change", (e) => {
      const selectedId = Number(e.target.value);
      const tenant = allTenants.find((t) => t.tenantId === selectedId);
      if (tenant) {
        // Auto-select property
        const propSelect = document.getElementById("propertySelect");
        if (propSelect) propSelect.value = tenant.propertyId;

        // Auto-populate rooms and select assigned room
        updateRoomsDropdown(tenant.propertyId, tenant.roomId);

        // Auto-populate rent amount
        const amountInput = document.getElementById("paymentAmount");
        if (amountInput) amountInput.value = tenant.monthlyRent;
      }
      updateReceiptPreview();
    });
  }

  // Populate Property Dropdown
  const propertySelect = document.getElementById("propertySelect");
  if (propertySelect) {
    propertySelect.innerHTML = `<option value="">-- Select Property --</option>` +
      allProperties
        .map((p) => `<option value="${p.propertyId}">${escapeHtml(p.propertyName)} [${p.propertyType}]</option>`)
        .join("");

    propertySelect.addEventListener("change", (e) => {
      updateRoomsDropdown(Number(e.target.value));
      updateReceiptPreview();
    });
  }

  // Room select listener
  const roomSelect = document.getElementById("roomSelect");
  if (roomSelect) {
    roomSelect.addEventListener("change", (e) => {
      const selectedOpt = roomSelect.options[roomSelect.selectedIndex];
      if (selectedOpt && selectedOpt.dataset.rent) {
        const amountInput = document.getElementById("paymentAmount");
        if (amountInput && !amountInput.value) {
          amountInput.value = selectedOpt.dataset.rent;
        }
      }
      updateReceiptPreview();
    });
  }

  // Default payment date to today
  const dateInput = document.getElementById("paymentDate");
  if (dateInput && !dateInput.value) {
    dateInput.value = new Date().toISOString().split("T")[0];
  }

  // Payment Month auto-suggest
  const monthSelect = document.getElementById("paymentMonth");
  if (monthSelect) {
    const today = new Date();
    const currentMonth = today.toLocaleString("default", { month: "long" }) + " " + today.getFullYear();
    monthSelect.value = currentMonth;
  }

  // Listen to input changes for live receipt preview
  const inputsToWatch = ["paymentAmount", "paymentDate", "paymentMonth", "transactionRef", "paymentStatus", "paymentRemarks"];
  inputsToWatch.forEach((id) => {
    const el = document.getElementById(id);
    if (el) el.addEventListener("input", updateReceiptPreview);
  });

  // Mode radio listeners
  document.querySelectorAll('input[name="paymentMode"]').forEach((radio) => {
    radio.addEventListener("change", (e) => {
      const refGroup = document.getElementById("refGroup");
      const refInput = document.getElementById("transactionRef");
      if (e.target.value === "Cash") {
        if (refInput && !refInput.value) refInput.placeholder = "Optional (e.g. Cash Receipt #)";
      } else {
        if (refInput) refInput.placeholder = "e.g. UPI Ref / UTR / Cheque No.";
      }
      updateReceiptPreview();
    });
  });

  // Handle Form Submission
  const form = document.getElementById("addPaymentForm");
  if (form) {
    form.addEventListener("submit", handleAddPaymentSubmit);
  }

  updateReceiptPreview();
}

async function updateRoomsDropdown(propertyId, selectRoomId = null) {
  const roomSelect = document.getElementById("roomSelect");
  if (!roomSelect) return;

  if (!propertyId) {
    roomSelect.innerHTML = `<option value="">-- Choose Property First --</option>`;
    return;
  }

  const rooms = await loadRooms(propertyId);
  roomSelect.innerHTML = `<option value="">-- Select Room --</option>` +
    rooms
      .map(
        (r) => `<option value="${r.roomId}" data-rent="${r.monthlyRent}" ${selectRoomId === r.roomId ? "selected" : ""}>
          Room ${escapeHtml(r.roomNumber)} - ${formatCurrency(r.monthlyRent)}/mo
        </option>`
      )
      .join("");
}

function updateReceiptPreview() {
  const tenantSelect = document.getElementById("tenantSelect");
  const propSelect = document.getElementById("propertySelect");
  const roomSelect = document.getElementById("roomSelect");
  const amountInput = document.getElementById("paymentAmount");
  const dateInput = document.getElementById("paymentDate");
  const monthSelect = document.getElementById("paymentMonth");
  const refInput = document.getElementById("transactionRef");
  const statusSelect = document.getElementById("paymentStatus");
  const modeRadio = document.querySelector('input[name="paymentMode"]:checked');

  const tenantName = tenantSelect && tenantSelect.selectedIndex > 0 ? tenantSelect.options[tenantSelect.selectedIndex].text.split(" (")[0] : "Tenant Not Selected";
  const propertyName = propSelect && propSelect.selectedIndex > 0 ? propSelect.options[propSelect.selectedIndex].text.split(" [")[0] : "Property Not Selected";
  const roomNumber = roomSelect && roomSelect.selectedIndex > 0 ? roomSelect.options[roomSelect.selectedIndex].text.split(" -")[0] : "Room Not Selected";
  const amount = amountInput && amountInput.value ? parseFloat(amountInput.value) : 0;
  const date = dateInput && dateInput.value ? dateInput.value : "--";
  const month = monthSelect ? monthSelect.value : "--";
  const mode = modeRadio ? modeRadio.value : "Cash";
  const ref = refInput && refInput.value ? refInput.value : "(Auto / Unspecified)";
  const status = statusSelect ? statusSelect.value : "Paid";

  setPreviewText("previewTenant", tenantName);
  setPreviewText("previewProperty", propertyName);
  setPreviewText("previewRoom", roomNumber);
  setPreviewText("previewDate", formatDate(date));
  setPreviewText("previewMonth", month);
  setPreviewText("previewMode", `${getModeIcon(mode)} ${mode}`);
  setPreviewText("previewRef", ref);
  setPreviewText("previewTotal", formatCurrency(amount));

  const stampEl = document.getElementById("previewStatusStamp");
  if (stampEl) {
    stampEl.textContent = status.toUpperCase();
    stampEl.className = `receipt-stamp stamp-${status.toLowerCase()}`;
  }
}

function setPreviewText(id, text) {
  const el = document.getElementById(id);
  if (el) el.textContent = text;
}

async function handleAddPaymentSubmit(e) {
  e.preventDefault();

  const tenantSelect = document.getElementById("tenantSelect");
  const propertySelect = document.getElementById("propertySelect");
  const roomSelect = document.getElementById("roomSelect");
  const amountInput = document.getElementById("paymentAmount");
  const dateInput = document.getElementById("paymentDate");
  const monthSelect = document.getElementById("paymentMonth");
  const statusSelect = document.getElementById("paymentStatus");
  const remarksInput = document.getElementById("paymentRemarks");
  const refInput = document.getElementById("transactionRef");
  const modeRadio = document.querySelector('input[name="paymentMode"]:checked');

  // Input Validation
  let hasError = false;

  const tenantId = parseInt(tenantSelect.value, 10);
  if (!tenantId) {
    showFieldError("tenantSelect", "Please select a tenant.");
    hasError = true;
  } else {
    clearFieldError("tenantSelect");
  }

  const propertyId = parseInt(propertySelect.value, 10);
  if (!propertyId) {
    showFieldError("propertySelect", "Please select a property.");
    hasError = true;
  } else {
    clearFieldError("propertySelect");
  }

  const roomId = parseInt(roomSelect.value, 10);
  if (!roomId) {
    showFieldError("roomSelect", "Please select a room.");
    hasError = true;
  } else {
    clearFieldError("roomSelect");
  }

  const amount = parseFloat(amountInput.value);
  if (isNaN(amount) || amount <= 0) {
    showFieldError("paymentAmount", "Amount must be a positive number.");
    hasError = true;
  } else {
    clearFieldError("paymentAmount");
  }

  const paymentDate = dateInput.value.trim();
  if (!paymentDate) {
    showFieldError("paymentDate", "Payment date is required.");
    hasError = true;
  } else {
    clearFieldError("paymentDate");
  }

  const paymentMode = modeRadio ? modeRadio.value : "Cash";
  const paymentStatus = statusSelect ? statusSelect.value : "Paid";
  const transactionRef = refInput ? refInput.value.trim() : "";

  // Validation: Electronic payments with Paid status should have a reference
  if ((paymentMode === "UPI" || paymentMode === "Bank Transfer") && paymentStatus === "Paid" && !transactionRef) {
    showFieldError("transactionRef", `Transaction reference is required for completed ${paymentMode} payments.`);
    hasError = true;
  } else {
    clearFieldError("transactionRef");
  }

  if (hasError) return;

  // Selected names for relational display
  const tenantName = tenantSelect.options[tenantSelect.selectedIndex].text.split(" (")[0];
  const propertyName = propertySelect.options[propertySelect.selectedIndex].text.split(" [")[0];
  const roomNumber = roomSelect.options[roomSelect.selectedIndex].text.split(" -")[0];

  const paymentPayload = {
    tenantId,
    tenantName,
    propertyId,
    propertyName,
    roomId,
    roomNumber,
    amount,
    paymentDate,
    paymentMonth: monthSelect.value.trim(),
    paymentMode,
    transactionRef: transactionRef || (paymentMode === "Cash" ? "CASH-REC-" + Date.now().toString().slice(-4) : ""),
    paymentStatus,
    remarks: remarksInput ? remarksInput.value.trim() : ""
  };

  const submitBtn = document.getElementById("submitPaymentBtn");
  if (submitBtn) {
    submitBtn.disabled = true;
    submitBtn.textContent = "Recording Payment...";
  }

  try {
    const result = await createPayment(paymentPayload);
    showToast(`Payment #${result.paymentId} recorded successfully!`, "success");
    setTimeout(() => {
      window.location.href = "payments.html";
    }, 1200);
  } catch (err) {
    alert("Error recording payment: " + err.message);
    if (submitBtn) {
      submitBtn.disabled = false;
      submitBtn.textContent = "💾 Record Payment";
    }
  }
}

function showFieldError(inputId, message) {
  const input = document.getElementById(inputId);
  if (input) input.classList.add("is-invalid");
  const errEl = document.getElementById(inputId + "Error");
  if (errEl) {
    errEl.textContent = message;
    errEl.classList.add("visible");
  }
}

function clearFieldError(inputId) {
  const input = document.getElementById(inputId);
  if (input) input.classList.remove("is-invalid");
  const errEl = document.getElementById(inputId + "Error");
  if (errEl) {
    errEl.textContent = "";
    errEl.classList.remove("visible");
  }
}

/* ==============================================================================
   PAGE 3: PAYMENT HISTORY (payment-history.html)
   ============================================================================== */

let allHistoryPayments = [];
let currentHistoryFilter = {
  search: "",
  tenantId: "ALL",
  month: "ALL",
  status: "ALL",
  mode: "ALL"
};

async function initPaymentHistory() {
  allHistoryPayments = await loadPayments();
  const tenants = await loadTenants();

  // Populate Tenant Filter Dropdown
  const filterTenant = document.getElementById("filterTenant");
  if (filterTenant) {
    filterTenant.innerHTML = `<option value="ALL">All Tenants</option>` +
      tenants.map((t) => `<option value="${t.tenantId}">${escapeHtml(t.fullName)}</option>`).join("");
  }

  // Populate Month Filter Dropdown with unique months
  const filterMonth = document.getElementById("filterMonth");
  if (filterMonth) {
    const months = Array.from(new Set(allHistoryPayments.map((p) => p.paymentMonth).filter(Boolean)));
    filterMonth.innerHTML = `<option value="ALL">All Months</option>` +
      months.map((m) => `<option value="${escapeHtml(m)}">${escapeHtml(m)}</option>`).join("");
  }

  // Search input handler
  const searchInput = document.getElementById("historySearchInput");
  if (searchInput) {
    searchInput.addEventListener("input", (e) => {
      currentHistoryFilter.search = e.target.value.trim().toLowerCase();
      renderHistoryTable();
    });
  }

  // Filter change handlers
  const filterElements = [
    { id: "filterTenant", key: "tenantId" },
    { id: "filterMonth", key: "month" },
    { id: "filterStatus", key: "status" },
    { id: "filterMode", key: "mode" }
  ];

  filterElements.forEach(({ id, key }) => {
    const el = document.getElementById(id);
    if (el) {
      el.addEventListener("change", (e) => {
        currentHistoryFilter[key] = e.target.value;
        renderHistoryTable();
      });
    }
  });

  // Reset Filters button
  const resetBtn = document.getElementById("resetFiltersBtn");
  if (resetBtn) {
    resetBtn.addEventListener("click", () => {
      currentHistoryFilter = { search: "", tenantId: "ALL", month: "ALL", status: "ALL", mode: "ALL" };
      if (searchInput) searchInput.value = "";
      if (filterTenant) filterTenant.value = "ALL";
      if (filterMonth) filterMonth.value = "ALL";
      const statusEl = document.getElementById("filterStatus");
      if (statusEl) statusEl.value = "ALL";
      const modeEl = document.getElementById("filterMode");
      if (modeEl) modeEl.value = "ALL";
      renderHistoryTable();
    });
  }

  renderHistoryTable();
}

function renderHistoryTable() {
  const tbody = document.getElementById("paymentHistoryTableBody");
  const countEl = document.getElementById("historyCountBadge");
  if (!tbody) return;

  const filtered = allHistoryPayments.filter((p) => {
    // Search text matches tenant, property, ref, or remarks
    if (currentHistoryFilter.search) {
      const q = currentHistoryFilter.search;
      const tName = (p.tenantName || "").toLowerCase();
      const pName = (p.propertyName || "").toLowerCase();
      const ref = (p.transactionRef || "").toLowerCase();
      const rem = (p.remarks || "").toLowerCase();
      if (!tName.includes(q) && !pName.includes(q) && !ref.includes(q) && !rem.includes(q)) {
        return false;
      }
    }

    // Tenant Filter
    if (currentHistoryFilter.tenantId !== "ALL") {
      if (Number(p.tenantId) !== Number(currentHistoryFilter.tenantId)) {
        return false;
      }
    }

    // Month Filter
    if (currentHistoryFilter.month !== "ALL") {
      if (p.paymentMonth !== currentHistoryFilter.month) {
        return false;
      }
    }

    // Status Filter
    if (currentHistoryFilter.status !== "ALL") {
      if (p.paymentStatus !== currentHistoryFilter.status) {
        return false;
      }
    }

    // Mode Filter
    if (currentHistoryFilter.mode !== "ALL") {
      if (p.paymentMode !== currentHistoryFilter.mode) {
        return false;
      }
    }

    return true;
  });

  if (countEl) {
    countEl.textContent = `Showing ${filtered.length} of ${allHistoryPayments.length} payments`;
  }

  if (filtered.length === 0) {
    tbody.innerHTML = `
      <tr>
        <td colspan="10" class="empty-state">
          <div class="empty-state-icon">🔍</div>
          <p>No payments match the selected search and filter criteria.</p>
        </td>
      </tr>
    `;
    return;
  }

  tbody.innerHTML = filtered
    .map(
      (p) => `
    <tr>
      <td><span class="payment-id-tag">#PAY-${String(p.paymentId).padStart(4, "0")}</span></td>
      <td><strong>${escapeHtml(p.tenantName || "Tenant #" + p.tenantId)}</strong></td>
      <td>${escapeHtml(p.propertyName || "Property #" + p.propertyId)}</td>
      <td><span class="badge-mode">${escapeHtml(p.roomNumber || "Room #" + p.roomId)}</span></td>
      <td><span class="payment-amount">${formatCurrency(p.amount)}</span></td>
      <td>${formatDate(p.paymentDate)}</td>
      <td>${escapeHtml(p.paymentMonth || "")}</td>
      <td><span class="badge-mode">${getModeIcon(p.paymentMode)} ${escapeHtml(p.paymentMode)}</span></td>
      <td><span class="badge-status status-${(p.paymentStatus || "").toLowerCase()}">${escapeHtml(p.paymentStatus)}</span></td>
      <td>
        <div class="table-actions">
          <button class="btn-table-action" onclick="viewPaymentReceipt(${p.paymentId})" title="View Official Receipt">📄 Receipt</button>
          <button class="btn-table-action" onclick="openStatusModal(${p.paymentId}, '${p.paymentStatus}')" title="Update Status">✏️</button>
          <button class="btn-table-action danger" onclick="confirmDeletePayment(${p.paymentId})" title="Delete Payment">🗑️</button>
        </div>
      </td>
    </tr>
  `
    )
    .join("");
}

/* ==============================================================================
   MODALS: RECEIPT & STATUS MANAGEMENT
   ============================================================================== */

let currentActivePaymentId = null;

async function viewPaymentReceipt(paymentId) {
  const payments = await loadPayments();
  const payment = payments.find((p) => p.paymentId === paymentId);
  if (!payment) return;

  const modal = document.getElementById("receiptModal");
  if (!modal) return;

  document.getElementById("modalReceiptNo").textContent = `PAY-${String(payment.paymentId).padStart(5, "0")}`;
  document.getElementById("modalReceiptDate").textContent = formatDate(payment.paymentDate);
  document.getElementById("modalTenantName").textContent = payment.tenantName || "Tenant #" + payment.tenantId;
  document.getElementById("modalPropertyName").textContent = payment.propertyName || "Property #" + payment.propertyId;
  document.getElementById("modalRoomNumber").textContent = payment.roomNumber || "Room #" + payment.roomId;
  document.getElementById("modalPaymentMonth").textContent = payment.paymentMonth;
  document.getElementById("modalPaymentMode").textContent = `${getModeIcon(payment.paymentMode)} ${payment.paymentMode}`;
  document.getElementById("modalTransactionRef").textContent = payment.transactionRef || "N/A";
  document.getElementById("modalRemarks").textContent = payment.remarks || "No remarks provided.";
  document.getElementById("modalAmount").textContent = formatCurrency(payment.amount);

  const stamp = document.getElementById("modalStatusStamp");
  if (stamp) {
    stamp.textContent = (payment.paymentStatus || "PAID").toUpperCase();
    stamp.className = `receipt-stamp stamp-${(payment.paymentStatus || "paid").toLowerCase()}`;
  }

  modal.classList.add("active");
}

function closeReceiptModal() {
  const modal = document.getElementById("receiptModal");
  if (modal) modal.classList.remove("active");
}

function printReceipt() {
  window.print();
}

function openStatusModal(paymentId, currentStatus) {
  currentActivePaymentId = paymentId;
  const modal = document.getElementById("statusModal");
  const select = document.getElementById("statusModalSelect");
  if (modal && select) {
    select.value = currentStatus;
    modal.classList.add("active");
  }
}

function closeStatusModal() {
  const modal = document.getElementById("statusModal");
  if (modal) modal.classList.remove("active");
  currentActivePaymentId = null;
}

async function saveStatusModalChange() {
  if (!currentActivePaymentId) return;
  const select = document.getElementById("statusModalSelect");
  if (!select) return;

  const newStatus = select.value;
  await updatePaymentStatus(currentActivePaymentId, newStatus);
  closeStatusModal();
  showToast(`Payment #${currentActivePaymentId} status changed to ${newStatus}`, "success");

  // Re-render whichever view is active
  if (document.getElementById("paymentsDashboardView")) {
    await renderDashboardStats();
    await renderRecentPaymentsTable();
  } else if (document.getElementById("paymentHistoryView")) {
    allHistoryPayments = await loadPayments();
    renderHistoryTable();
  }
}

function confirmDeletePayment(paymentId) {
  if (confirm(`Are you sure you want to permanently delete Payment #${paymentId}?\nThis operation will remove the transaction record.`)) {
    deletePayment(paymentId).then(() => {
      showToast(`Payment #${paymentId} deleted.`, "info");
      if (document.getElementById("paymentHistoryView")) {
        allHistoryPayments = allHistoryPayments.filter((p) => p.paymentId !== paymentId);
        renderHistoryTable();
      } else if (document.getElementById("paymentsDashboardView")) {
        renderDashboardStats();
        renderRecentPaymentsTable();
      }
    });
  }
}

/* ==============================================================================
   SHARED UI: SIDEBAR, NOTIFICATIONS & TOASTS
   ============================================================================== */

function setupNavigationAndSidebar() {
  const sidebar = document.getElementById("appSidebar");
  const toggleBtn = document.getElementById("sidebarToggleBtn");
  if (!sidebar || !toggleBtn) return;

  const isCollapsed = localStorage.getItem("rentproof_sidebar_collapsed") === "true";
  if (isCollapsed) sidebar.classList.add("collapsed");

  toggleBtn.addEventListener("click", () => {
    sidebar.classList.toggle("collapsed");
    localStorage.setItem("rentproof_sidebar_collapsed", sidebar.classList.contains("collapsed"));
  });

  window.addEventListener("resize", () => {
    if (window.innerWidth <= 768) {
      sidebar.classList.remove("collapsed");
    }
  });
}

function setupActiveUser() {
  const user = JSON.parse(localStorage.getItem("rentproof_user")) || {
    fullName: "Aanvi",
    email: "aanvi.payments@rentproof.com"
  };

  const nameEl = document.getElementById("landlordDisplayName");
  const avatarEl = document.getElementById("landlordAvatarInitials");

  if (nameEl) nameEl.textContent = user.fullName;
  if (avatarEl) {
    const initials = user.fullName
      .split(" ")
      .map((n) => n[0])
      .join("")
      .substring(0, 2)
      .toUpperCase();
    avatarEl.textContent = initials || "AV";
  }

}

function showModuleNotice(moduleName, memberName) {
  alert(
    `RentProof Module Notice:\nThe ${moduleName} module is assigned to ${memberName}.\nAanvi's module (Payment Management) integrates with the shared 'rentproof' database and foreign keys.`
  );
}

function showToast(message, type = "info") {
  let container = document.getElementById("toastContainer");
  if (!container) {
    container = document.createElement("div");
    container.id = "toastContainer";
    container.className = "toast-container";
    document.body.appendChild(container);
  }

  const toast = document.createElement("div");
  toast.className = `toast toast-${type}`;
  toast.innerHTML = `<span>${escapeHtml(message)}</span>`;
  container.appendChild(toast);

  setTimeout(() => {
    toast.remove();
  }, 3500);
}

// Formatters
function formatCurrency(amount) {
  return "₹" + Number(amount || 0).toLocaleString("en-IN", {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  });
}

function formatDate(dateStr) {
  if (!dateStr || dateStr === "--") return "--";
  try {
    const d = new Date(dateStr);
    if (isNaN(d.getTime())) return dateStr;
    return d.toLocaleDateString("en-IN", {
      day: "2-digit",
      month: "short",
      year: "numeric"
    });
  } catch (e) {
    return dateStr;
  }
}

function getModeIcon(mode) {
  switch ((mode || "").toLowerCase()) {
    case "upi":
      return "📱";
    case "bank transfer":
      return "🏦";
    case "cash":
      return "💵";
    case "cheque":
      return "📝";
    default:
      return "💳";
  }
}

function escapeHtml(str) {
  if (str === null || str === undefined) return "";
  return String(str)
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#039;");
}

