/**
 * ==============================================================================
 * RENTPROOF - TENANT MANAGEMENT JAVASCRIPT (tenants.js)
 * Module: Tenant Management and Property Assignment
 * Author: Shweta
 * 
 * Features:
 * - Reactive Tenant dashboard with localStorage persistence aligned with MySQL schema
 * - Real-time search by Name, Phone Number, or Email
 * - Dual filtering by Property and Tenant Status ('Active', 'Moved Out', 'Pending')
 * - Dynamic room allocation ensuring ONLY Vacant rooms can be assigned
 * - Room occupancy synchronization (Active -> Occupied, Moved Out -> Vacant)
 * - Complete Form Validation (Regex for 10-digit Phone, Email RFC, non-negative rent)
 * - Modular architecture with cleanly isolated TenantAPI layer for backend integration
 * - View Tenant Details modal & Delete confirmation modal
 * - Responsive Collapsible Sidebar handling
 * ==============================================================================
 */

// ==========================================
// 1. SEED DATA ALIGNED WITH database/rentproof.sql
// ==========================================
const SEED_PROPERTIES = [
  {
    propertyId: 1,
    landlordId: 1,
    propertyName: "Sunrise Executive PG",
    propertyType: "PG",
    address: "Plot 42, Knowledge Park III",
    city: "Greater Noida",
    state: "Uttar Pradesh",
    pincode: "201306",
    totalRooms: 6,
    monthlyRent: 8500
  },
  {
    propertyId: 2,
    landlordId: 1,
    propertyName: "Ivory Heights 3BHK Flat",
    propertyType: "Flat",
    address: "Tower B, Flat 702, Golf City",
    city: "Noida",
    state: "Uttar Pradesh",
    pincode: "201301",
    totalRooms: 3,
    monthlyRent: 28000
  },
  {
    propertyId: 3,
    landlordId: 1,
    propertyName: "Blue Ridge Residency",
    propertyType: "Apartment",
    address: "Sector 62, Near Metro Station",
    city: "Noida",
    state: "Uttar Pradesh",
    pincode: "201309",
    totalRooms: 4,
    monthlyRent: 34000
  }
];

const SEED_ROOMS = [
  // Sunrise Executive PG (Property 1)
  { roomId: 1, propertyId: 1, roomNumber: "101", monthlyRent: 8500, roomStatus: "Occupied" },
  { roomId: 2, propertyId: 1, roomNumber: "102", monthlyRent: 8500, roomStatus: "Occupied" },
  { roomId: 3, propertyId: 1, roomNumber: "103", monthlyRent: 8500, roomStatus: "Vacant" },
  { roomId: 4, propertyId: 1, roomNumber: "201", monthlyRent: 9000, roomStatus: "Vacant" },
  { roomId: 5, propertyId: 1, roomNumber: "202", monthlyRent: 9000, roomStatus: "Occupied" },
  { roomId: 6, propertyId: 1, roomNumber: "203", monthlyRent: 9500, roomStatus: "Maintenance" },

  // Ivory Heights Flat (Property 2)
  { roomId: 7, propertyId: 2, roomNumber: "Room-A (Master)", monthlyRent: 11000, roomStatus: "Occupied" },
  { roomId: 8, propertyId: 2, roomNumber: "Room-B (Guest)", monthlyRent: 9000, roomStatus: "Vacant" },
  { roomId: 9, propertyId: 2, roomNumber: "Room-C (Standard)", monthlyRent: 8000, roomStatus: "Occupied" },

  // Blue Ridge Residency (Property 3)
  { roomId: 10, propertyId: 3, roomNumber: "B-101", monthlyRent: 8500, roomStatus: "Vacant" },
  { roomId: 11, propertyId: 3, roomNumber: "B-102", monthlyRent: 8500, roomStatus: "Occupied" },
  { roomId: 12, propertyId: 3, roomNumber: "B-103", monthlyRent: 8500, roomStatus: "Vacant" },
  { roomId: 13, propertyId: 3, roomNumber: "B-104", monthlyRent: 8500, roomStatus: "Vacant" }
];

const SEED_TENANTS = [
  {
    tenantId: 1,
    propertyId: 1,
    propertyName: "Sunrise Executive PG",
    roomId: 1,
    roomNumber: "101",
    fullName: "Rohan Verma",
    email: "rohan.v@gmail.com",
    phone: "9988776655",
    emergencyContact: "9811223344",
    permanentAddress: "H-42, Civil Lines, Kanpur, UP",
    idProofNumber: "",
    moveInDate: "2026-08-01",
    monthlyRent: 8500,
    securityDeposit: 17000,
    tenantStatus: "Active",
    createdAt: "2026-08-01 10:00:00"
  },
  {
    tenantId: 2,
    propertyId: 1,
    propertyName: "Sunrise Executive PG",
    roomId: 2,
    roomNumber: "102",
    fullName: "Ankit Mishra",
    email: "ankit.m@yahoo.com",
    phone: "9871122334",
    emergencyContact: "9911002233",
    permanentAddress: "12/B Park Street, Lucknow, UP",
    idProofNumber: "",
    moveInDate: "2026-09-15",
    monthlyRent: 8500,
    securityDeposit: 17000,
    tenantStatus: "Active",
    createdAt: "2026-09-15 11:30:00"
  },
  {
    tenantId: 3,
    propertyId: 2,
    propertyName: "Ivory Heights 3BHK Flat",
    roomId: 7,
    roomNumber: "Room-A (Master)",
    fullName: "Sneha Kapoor",
    email: "sneha.k@outlook.com",
    phone: "9711334455",
    emergencyContact: "9811556677",
    permanentAddress: "Flat 401, Silver Sands, Chandigarh",
    idProofNumber: "",
    moveInDate: "2026-07-01",
    monthlyRent: 11000,
    securityDeposit: 22000,
    tenantStatus: "Active",
    createdAt: "2026-07-01 09:15:00"
  },
  {
    tenantId: 4,
    propertyId: 3,
    propertyName: "Blue Ridge Residency",
    roomId: 11,
    roomNumber: "B-102",
    fullName: "Arjun Nair",
    email: "arjun.nair@techmail.com",
    phone: "9820112233",
    emergencyContact: "9845001122",
    permanentAddress: "Plot 88, Indiranagar, Bengaluru, KA",
    idProofNumber: "PASSPORT-Z1928374",
    moveInDate: "2026-10-01",
    monthlyRent: 8500,
    securityDeposit: 17000,
    tenantStatus: "Active",
    createdAt: "2026-10-01 14:00:00"
  }
];

// ==========================================
// 2. DATA STORAGE REPOSITORY (Live MySQL Synchronized)
// ==========================================

let cachedTenants = [];
let cachedProperties = [];
let cachedRooms = [];

async function syncBackendData() {
  const [tenantsRes, propsRes, roomsRes] = await Promise.all([
    fetch("/api/tenants"),
    fetch("/api/properties"),
    fetch("/api/rooms")
  ]);
  if (!tenantsRes.ok || !propsRes.ok || !roomsRes.ok) {
    throw new Error("Could not load tenant, property, and room data from the backend.");
  }
  const [tenants, properties, rooms] = await Promise.all([
    tenantsRes.json(),
    propsRes.json(),
    roomsRes.json()
  ]);
  if (!Array.isArray(tenants) || !Array.isArray(properties) || !Array.isArray(rooms)) {
    throw new Error("The backend returned invalid tenant, property, or room data.");
  }
  cachedTenants = tenants;
  cachedProperties = properties;
  cachedRooms = rooms;
}

function getStoredTenants() {
  return cachedTenants;
}

function saveStoredTenants(tenants) {
  cachedTenants = tenants;
}

function getStoredProperties() {
  return cachedProperties;
}

function getStoredRooms() {
  return cachedRooms;
}

function saveStoredRooms(rooms) {
  cachedRooms = rooms;
}

// ==========================================
// 3. ISOLATED TENANT API LAYER
// Connected to Core Java RentProofServer
// ==========================================
const TenantAPI = {
  // Live MySQL backend enabled
  useLiveBackend: true,
  apiBaseUrl: "/api/tenants",

  async getAllTenants() {
    if (this.useLiveBackend) {
      const res = await fetch(this.apiBaseUrl);
      if (!res.ok) throw new Error("Failed to fetch tenants from Java backend.");
      cachedTenants = await res.json();
      return cachedTenants;
    }
    return getStoredTenants();
  },

  async getTenantById(id) {
    if (this.useLiveBackend) {
      const res = await fetch(`${this.apiBaseUrl}/${id}`);
      if (!res.ok) throw new Error(`Failed to fetch tenant with ID ${id}`);
      return await res.json();
    }
    const tenants = getStoredTenants();
    return tenants.find(t => t.tenantId === Number(id)) || null;
  },

  async addTenant(tenantData) {
    if (this.useLiveBackend) {
      const res = await fetch(this.apiBaseUrl, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(tenantData)
      });
      if (!res.ok) {
        const err = await res.json().catch(() => ({}));
        throw new Error(err.error || "Backend tenant creation failed.");
      }
      const data = await res.json();
      await syncBackendData();
      return data.tenant || data;
    }

    // LocalStorage Demonstration Mode
    const tenants = getStoredTenants();
    const rooms = getStoredRooms();
    const properties = getStoredProperties();

    // Verify room is vacant
    const targetRoom = rooms.find(r => r.roomId === Number(tenantData.roomId));
    if (targetRoom && targetRoom.roomStatus === "Occupied") {
      throw new Error("Selected room is already occupied!");
    }

    const prop = properties.find(p => p.propertyId === Number(tenantData.propertyId));

    const newId = tenants.length > 0 ? Math.max(...tenants.map(t => t.tenantId)) + 1 : 1;
    const newTenant = {
      tenantId: newId,
      propertyId: Number(tenantData.propertyId),
      propertyName: prop ? prop.propertyName : "Property " + tenantData.propertyId,
      roomId: Number(tenantData.roomId),
      roomNumber: targetRoom ? targetRoom.roomNumber : "Room " + tenantData.roomId,
      fullName: tenantData.fullName.trim(),
      email: tenantData.email.trim(),
      phone: tenantData.phone.trim(),
      emergencyContact: tenantData.emergencyContact ? tenantData.emergencyContact.trim() : "",
      permanentAddress: tenantData.permanentAddress ? tenantData.permanentAddress.trim() : "",
      idProofNumber: tenantData.idProofNumber ? tenantData.idProofNumber.trim() : "",
      moveInDate: tenantData.moveInDate,
      monthlyRent: Number(tenantData.monthlyRent),
      securityDeposit: Number(tenantData.securityDeposit || 0),
      tenantStatus: tenantData.tenantStatus || "Active",
      createdAt: new Date().toISOString().replace("T", " ").substring(0, 19)
    };

    tenants.unshift(newTenant);
    saveStoredTenants(tenants);

    // Update room status to Occupied
    if (targetRoom && newTenant.tenantStatus === "Active") {
      targetRoom.roomStatus = "Occupied";
      saveStoredRooms(rooms);
    }

    return newTenant;
  },

  async updateTenant(id, updateData) {
    if (this.useLiveBackend) {
      const res = await fetch(`${this.apiBaseUrl}/${id}`, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(updateData)
      });
      if (!res.ok) {
        const err = await res.json().catch(() => ({}));
        throw new Error(err.error || "Backend tenant update failed.");
      }
      const data = await res.json();
      await syncBackendData();
      return data.tenant || data;
    }

    const tenants = getStoredTenants();
    const index = tenants.findIndex(t => t.tenantId === Number(id));
    if (index === -1) throw new Error("Tenant record not found.");

    const existing = tenants[index];
    const prevStatus = existing.tenantStatus;

    // Apply updates
    existing.fullName = updateData.fullName ? updateData.fullName.trim() : existing.fullName;
    existing.email = updateData.email ? updateData.email.trim() : existing.email;
    existing.phone = updateData.phone ? updateData.phone.trim() : existing.phone;
    existing.emergencyContact = updateData.emergencyContact !== undefined ? updateData.emergencyContact.trim() : existing.emergencyContact;
    existing.permanentAddress = updateData.permanentAddress !== undefined ? updateData.permanentAddress.trim() : existing.permanentAddress;
    if (updateData.monthlyRent !== undefined) {
      existing.monthlyRent = Number(updateData.monthlyRent);
    }
    if (updateData.tenantStatus) {
      existing.tenantStatus = updateData.tenantStatus;
    }

    tenants[index] = existing;
    saveStoredTenants(tenants);

    // Handle room status sync
    const rooms = getStoredRooms();
    const room = rooms.find(r => r.roomId === existing.roomId);
    if (room) {
      if (existing.tenantStatus === "Moved Out") {
        room.roomStatus = "Vacant";
        existing.moveOutDate = new Date().toISOString().substring(0, 10);
      } else if (existing.tenantStatus === "Active") {
        room.roomStatus = "Occupied";
      }
      saveStoredRooms(rooms);
    }

    return existing;
  },

  async deleteTenant(id) {
    if (this.useLiveBackend) {
      const res = await fetch(`${this.apiBaseUrl}/${id}`, { method: "DELETE" });
      if (!res.ok) {
        const err = await res.json().catch(() => ({}));
        throw new Error(err.error || "Backend tenant deletion failed.");
      }
      await syncBackendData();
      return true;
    }

    let tenants = getStoredTenants();
    const target = tenants.find(t => t.tenantId === Number(id));
    if (!target) return false;

    // Free the assigned room
    const rooms = getStoredRooms();
    const room = rooms.find(r => r.roomId === target.roomId);
    if (room) {
      room.roomStatus = "Vacant";
      saveStoredRooms(rooms);
    }

    tenants = tenants.filter(t => t.tenantId !== Number(id));
    saveStoredTenants(tenants);
    return true;
  },

  async assignProperty(tenantId, propertyId, roomId, monthlyRent, moveInDate) {
    if (this.useLiveBackend) {
      const res = await fetch(`${this.apiBaseUrl}/assign`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ tenantId, propertyId, roomId, monthlyRent, moveInDate })
      });
      if (!res.ok) {
        const err = await res.json().catch(() => ({}));
        throw new Error(err.error || "Property assignment failed in backend.");
      }
      await syncBackendData();
      return await res.json();
    }

    const tenants = getStoredTenants();
    const rooms = getStoredRooms();
    const properties = getStoredProperties();

    const tenant = tenants.find(t => t.tenantId === Number(tenantId));
    if (!tenant) throw new Error("Tenant not found.");

    const newRoom = rooms.find(r => r.roomId === Number(roomId));
    if (!newRoom) throw new Error("Room not found.");
    if (newRoom.roomStatus === "Occupied") throw new Error("Room is already occupied!");

    const prop = properties.find(p => p.propertyId === Number(propertyId));

    // Free old room if different
    if (tenant.roomId && tenant.roomId !== Number(roomId)) {
      const oldRoom = rooms.find(r => r.roomId === tenant.roomId);
      if (oldRoom) {
        oldRoom.roomStatus = "Vacant";
      }
    }

    // Assign new room (preserve original room default rent)
    newRoom.roomStatus = "Occupied";
    saveStoredRooms(rooms);

    tenant.propertyId = Number(propertyId);
    tenant.propertyName = prop ? prop.propertyName : "Property " + propertyId;
    tenant.roomId = Number(roomId);
    tenant.roomNumber = newRoom.roomNumber;
    tenant.monthlyRent = Number(monthlyRent);
    tenant.moveInDate = moveInDate;
    tenant.tenantStatus = "Active";

    saveStoredTenants(tenants);
    return tenant;
  }
};

// ==========================================
// 4. MAIN PAGE ROUTER & INITIALIZATION
// ==========================================
document.addEventListener("DOMContentLoaded", async () => {
  if (!(await window.RentProofSecurity.requireSession())) return;
  setupNavigationAndSidebar();
  setupActiveUser();
  document.addEventListener("click", handleTenantActionClick);

  const globalSearch = document.getElementById("globalTenantSearch");
  const tenantSearch = document.getElementById("tenantSearchInput");
  if (globalSearch && tenantSearch) {
    globalSearch.addEventListener("input", () => {
      tenantSearch.value = globalSearch.value;
      renderTenantDashboard();
    });
  }
  const emptyReset = document.getElementById("emptyResetFiltersBtn");
  const resetFilters = document.getElementById("resetFiltersBtn");
  if (emptyReset && resetFilters) emptyReset.addEventListener("click", () => resetFilters.click());
  const notificationButton = document.getElementById("tenantNotificationBtn");
  if (notificationButton) notificationButton.addEventListener("click", () => showToast("Tenant records are loaded from the backend.", "info"));

  // Load live data from MySQL via Core Java REST endpoints
  try {
    await syncBackendData();
  } catch (error) {
    console.error("[TenantUI] Could not load tenant data:", error);
    showToast(error.message || "Could not load tenant records.", "error");
  }

  // Route to the appropriate page controller
  if (document.getElementById("tenantsTableBody")) {
    initTenantsDashboard();
  } else if (document.getElementById("addTenantForm")) {
    initAddTenantForm();
  } else if (document.getElementById("editTenantForm")) {
    initEditTenantForm();
  } else if (document.getElementById("assignPropertyForm")) {
    initAssignPropertyForm();
  }
});

function handleTenantActionClick(event) {
  const button = event.target.closest("[data-tenant-action]");
  if (!button) return;

  const tenantId = Number(button.dataset.tenantId);
  if (button.dataset.tenantAction === "view") {
    openViewTenantModal(tenantId);
  } else if (button.dataset.tenantAction === "delete") {
    const tenant = cachedTenants.find((item) => Number(item.tenantId) === tenantId);
    openDeleteTenantModal(tenantId, tenant ? tenant.fullName : "this tenant");
  }
}

// ==========================================
// 5. TENANTS DASHBOARD CONTROLLER (tenants.html)
// ==========================================
function initTenantsDashboard() {
  const searchInput = document.getElementById("tenantSearchInput");
  const propertyFilter = document.getElementById("propertyFilter");
  const statusFilter = document.getElementById("statusFilter");
  const resetFiltersBtn = document.getElementById("resetFiltersBtn");

  // Load properties into filter dropdown
  populatePropertyFilterDropdown();

  // Render initial table and stats
  renderTenantDashboard();

  // Search input listener (debounce style)
  if (searchInput) {
    searchInput.addEventListener("input", () => {
      renderTenantDashboard();
    });
  }

  // Filter dropdown listeners
  if (propertyFilter) {
    propertyFilter.addEventListener("change", () => {
      renderTenantDashboard();
    });
  }

  if (statusFilter) {
    statusFilter.addEventListener("change", () => {
      renderTenantDashboard();
    });
  }

  if (resetFiltersBtn) {
    resetFiltersBtn.addEventListener("click", () => {
      if (searchInput) searchInput.value = "";
      if (propertyFilter) propertyFilter.value = "ALL";
      if (statusFilter) statusFilter.value = "ALL";
      renderTenantDashboard();
      showToast("Filters reset to default view.", "info");
    });
  }

  // Setup modal close buttons
  setupModalDismissals();
}

/**
 * Populates property filter dropdown with unique properties.
 */
function populatePropertyFilterDropdown() {
  const select = document.getElementById("propertyFilter");
  if (!select) return;

  const properties = getStoredProperties();
  select.innerHTML = '<option value="ALL">All Properties</option>';
  for (let i = 0; i < properties.length; i++) {
    const opt = document.createElement("option");
    opt.value = properties[i].propertyId;
    opt.textContent = properties[i].propertyName;
    select.appendChild(opt);
  }
}

/**
 * Filters and renders tenants table and updates summary statistics.
 */
function renderTenantDashboard() {
  const tenants = getStoredTenants();
  const rooms = getStoredRooms();

  // Update Summary Stats Cards
  updateStatsCards(tenants, rooms);

  // Apply Search and Filters
  const searchVal = (document.getElementById("tenantSearchInput")?.value || "").toLowerCase().trim();
  const propVal = document.getElementById("propertyFilter")?.value || "ALL";
  const statusVal = document.getElementById("statusFilter")?.value || "ALL";

  let filtered = tenants.filter(t => {
    // 1. Search Query: Matches Name, Phone, or Email
    const nameMatch = t.fullName && t.fullName.toLowerCase().includes(searchVal);
    const phoneMatch = t.phone && t.phone.includes(searchVal);
    const emailMatch = t.email && t.email.toLowerCase().includes(searchVal);
    const matchesSearch = !searchVal || nameMatch || phoneMatch || emailMatch;

    // 2. Property Filter
    const matchesProperty = (propVal === "ALL") || (String(t.propertyId) === String(propVal));

    // 3. Status Filter
    const matchesStatus = (statusVal === "ALL") || (t.tenantStatus && t.tenantStatus.toLowerCase() === statusVal.toLowerCase());

    return matchesSearch && matchesProperty && matchesStatus;
  });

  renderTenantsTable(filtered);
}

/**
 * Calculates and updates statistical indicator cards.
 */
function updateStatsCards(tenants, rooms) {
  const statTotal = document.getElementById("statTotalTenants");
  const statActive = document.getElementById("statActiveTenants");
  const statVacant = document.getElementById("statVacantRooms");

  let activeCount = 0;
  for (let i = 0; i < tenants.length; i++) {
    if (tenants[i].tenantStatus === "Active") {
      activeCount++;
    }
  }

  let vacantCount = 0;
  for (let i = 0; i < rooms.length; i++) {
    if (rooms[i].roomStatus === "Vacant") {
      vacantCount++;
    }
  }

  if (statTotal) statTotal.textContent = tenants.length;
  if (statActive) statActive.textContent = activeCount;
  if (statVacant) statVacant.textContent = vacantCount;
}

/**
 * Renders tenant table rows or shows empty state.
 */
function renderTenantsTable(tenants) {
  const tbody = document.getElementById("tenantsTableBody");
  const emptyState = document.getElementById("emptyStateContainer");
  const tableWrapper = document.getElementById("tenantsTableWrapper");

  if (!tbody) return;
  tbody.innerHTML = "";

  if (tenants.length === 0) {
    if (tableWrapper) tableWrapper.style.display = "none";
    if (emptyState) emptyState.style.display = "flex";
    return;
  }

  if (tableWrapper) tableWrapper.style.display = "block";
  if (emptyState) emptyState.style.display = "none";

  for (let i = 0; i < tenants.length; i++) {
    const t = tenants[i];
    const tr = document.createElement("tr");

    // Initials for avatar
    const initials = getInitials(t.fullName);

    // Status class
    const statusClass = getStatusClass(t.tenantStatus);

    tr.innerHTML = `
      <td data-label="Tenant ID"><span class="tenant-id-badge">#TN-${String(t.tenantId).padStart(3, '0')}</span></td>
      <td data-label="Tenant Name">
        <div class="tenant-name-cell">
          <div class="tenant-avatar">${initials}</div>
          <div class="tenant-name-text">${escapeHtml(t.fullName)}</div>
        </div>
      </td>
      <td data-label="Phone / Email">
        <div class="tenant-contact-cell">
          <span class="contact-phone">📞 ${escapeHtml(t.phone)}</span>
          <span class="contact-email">✉️ ${escapeHtml(t.email)}</span>
        </div>
      </td>
      <td data-label="Property Name">
        <span class="tenant-property-cell">${escapeHtml(t.propertyName || 'Unassigned')}</span>
      </td>
      <td data-label="Room Number">
        <span class="property-room-pill">
          🚪 <span class="room-badge">${escapeHtml(t.roomNumber || 'N/A')}</span>
        </span>
      </td>
      <td data-label="Monthly Rent">
        <span class="rent-amount">₹${Number(t.monthlyRent || 0).toLocaleString('en-IN')}<small>/mo</small></span>
      </td>
      <td data-label="Joining Date">
        <span class="tenant-date-cell">${formatDisplayDate(t.moveInDate)}</span>
      </td>
      <td data-label="Status">
        <span class="status-pill ${statusClass}">${escapeHtml(t.tenantStatus || 'Active')}</span>
      </td>
      <td data-label="Actions">
        <div class="table-actions">
          <button class="btn-action action-view" title="View Full Details" data-tenant-action="view" data-tenant-id="${t.tenantId}">
            👁️
          </button>
          <a href="edit-tenant.html?id=${t.tenantId}" class="btn-action action-edit" title="Edit Tenant">
            ✏️
          </a>
          <button class="btn-action action-delete" title="Delete Tenant" data-tenant-action="delete" data-tenant-id="${t.tenantId}">
            🗑️
          </button>
        </div>
      </td>
    `;
    tbody.appendChild(tr);
  }
}

// ==========================================
// 6. VIEW & DELETE MODAL HANDLERS
// ==========================================

function openViewTenantModal(tenantId) {
  const tenants = getStoredTenants();
  const t = tenants.find(x => x.tenantId === Number(tenantId));
  if (!t) return;

  const modal = document.getElementById("viewTenantModal");
  const modalContent = document.getElementById("viewTenantDetailsContent");

  if (!modal || !modalContent) return;

  const initials = getInitials(t.fullName);
  const statusClass = getStatusClass(t.tenantStatus);

  modalContent.innerHTML = `
    <div style="display: flex; align-items: center; gap: 16px; margin-bottom: 20px; padding-bottom: 16px; border-bottom: 1px solid rgba(198, 185, 159, 0.4);">
      <div class="tenant-avatar" style="width: 52px; height: 52px; font-size: 1.2rem;">${initials}</div>
      <div>
        <h4 style="font-size: 1.25rem; color: var(--navy); margin-bottom: 2px;">${escapeHtml(t.fullName)}</h4>
        <span class="status-pill ${statusClass}">${escapeHtml(t.tenantStatus)}</span>
      </div>
    </div>

    <div class="tenant-details-grid">
      <div class="detail-item">
        <span class="detail-label">Tenant ID</span>
        <span class="detail-value">#TN-${String(t.tenantId).padStart(3, '0')}</span>
      </div>
      <div class="detail-item">
        <span class="detail-label">Phone Number</span>
        <span class="detail-value">${escapeHtml(t.phone)}</span>
      </div>
      <div class="detail-item">
        <span class="detail-label">Email Address</span>
        <span class="detail-value">${escapeHtml(t.email)}</span>
      </div>
      <div class="detail-item">
        <span class="detail-label">Emergency Contact</span>
        <span class="detail-value">${escapeHtml(t.emergencyContact || 'Not Specified')}</span>
      </div>
      <div class="detail-item">
        <span class="detail-label">Assigned Property</span>
        <span class="detail-value">${escapeHtml(t.propertyName || 'N/A')}</span>
      </div>
      <div class="detail-item">
        <span class="detail-label">Assigned Room</span>
        <span class="detail-value">Room ${escapeHtml(t.roomNumber || 'N/A')}</span>
      </div>
      <div class="detail-item">
        <span class="detail-label">Monthly Rent</span>
        <span class="detail-value" style="color: var(--navy); font-weight: 700;">₹${Number(t.monthlyRent || 0).toLocaleString('en-IN')}/month</span>
      </div>
      <div class="detail-item">
        <span class="detail-label">Security Deposit</span>
        <span class="detail-value">₹${Number(t.securityDeposit || 0).toLocaleString('en-IN')}</span>
      </div>
      <div class="detail-item">
        <span class="detail-label">Move-In Date</span>
        <span class="detail-value">${formatDisplayDate(t.moveInDate)}</span>
      </div>
      <div class="detail-item">
        <span class="detail-label">Move-Out Date</span>
        <span class="detail-value">${t.moveOutDate ? formatDisplayDate(t.moveOutDate) : 'Currently Active'}</span>
      </div>
      <div class="detail-item full-width">
        <span class="detail-label">Permanent Address / ID Proof</span>
        <span class="detail-value">${escapeHtml(t.permanentAddress || t.idProofNumber || 'None recorded')}</span>
      </div>
    </div>
  `;

  modal.classList.add("active");
}

let pendingDeleteTenantId = null;

function openDeleteTenantModal(tenantId, tenantName) {
  pendingDeleteTenantId = tenantId;
  const modal = document.getElementById("deleteConfirmModal");
  const label = document.getElementById("deleteTenantNamePrompt");

  if (label) {
    label.textContent = tenantName;
  }
  if (modal) {
    modal.classList.add("active");
  }
}

function confirmDeleteTenantAction() {
  if (!pendingDeleteTenantId) return;

  TenantAPI.deleteTenant(pendingDeleteTenantId)
    .then(() => {
      closeAllModals();
      renderTenantDashboard();
      showToast("Tenant record removed successfully. Assigned room is now vacant.", "success");
      pendingDeleteTenantId = null;
    })
    .catch(err => {
      showToast(err.message, "error");
    });
}

function closeAllModals() {
  const modals = document.querySelectorAll(".modal-overlay");
  modals.forEach(m => m.classList.remove("active"));
  pendingDeleteTenantId = null;
}

function setupModalDismissals() {
  const closeBtns = document.querySelectorAll(".modal-close-btn, .modal-cancel-btn");
  closeBtns.forEach(btn => {
    btn.addEventListener("click", closeAllModals);
  });

  const confirmDeleteBtn = document.getElementById("confirmDeleteBtn");
  if (confirmDeleteBtn) {
    confirmDeleteBtn.addEventListener("click", confirmDeleteTenantAction);
  }

  // Dismiss on clicking backdrop
  const overlays = document.querySelectorAll(".modal-overlay");
  overlays.forEach(overlay => {
    overlay.addEventListener("click", (e) => {
      if (e.target === overlay) {
        closeAllModals();
      }
    });
  });
}

// ==========================================
// 7. ADD TENANT FORM CONTROLLER (add-tenant.html)
// ==========================================
function initAddTenantForm() {
  const form = document.getElementById("addTenantForm");
  const propSelect = document.getElementById("propertySelect");
  const roomSelect = document.getElementById("roomSelect");
  const rentInput = document.getElementById("monthlyRent");
  const propertyInfoBox = document.getElementById("propertyInfoBox");

  // Set default moveInDate to today
  const moveInDateInput = document.getElementById("moveInDate");
  if (moveInDateInput && !moveInDateInput.value) {
    moveInDateInput.value = new Date().toISOString().substring(0, 10);
  }

  // Populate Properties Dropdown
  const properties = getStoredProperties();
  if (propSelect) {
    propSelect.innerHTML = '<option value="">-- Choose Property --</option>';
    properties.forEach(p => {
      const opt = document.createElement("option");
      opt.value = p.propertyId;
      opt.textContent = `${p.propertyName} (${p.propertyType} - ${p.city})`;
      propSelect.appendChild(opt);
    });

    // Dynamic Room Selection when Property Changes
    propSelect.addEventListener("change", () => {
      const selectedPropId = Number(propSelect.value);
      updateRoomDropdownForProperty(selectedPropId, roomSelect, rentInput, propertyInfoBox);
    });
  }

  // When room selection changes, update rent if available
  if (roomSelect) {
    roomSelect.addEventListener("change", () => {
      const selectedRoomId = Number(roomSelect.value);
      const rooms = getStoredRooms();
      const r = rooms.find(rm => rm.roomId === selectedRoomId);
      if (r && rentInput) {
        rentInput.value = r.monthlyRent;
      }
    });
  }

  // Form Submission
  if (form) {
    form.addEventListener("submit", (e) => {
      e.preventDefault();
      handleSaveNewTenant(form);
    });
  }
}

/**
 * Filters and populates ONLY VACANT rooms belonging to the selected property.
 */
function updateRoomDropdownForProperty(propertyId, roomSelect, rentInput, propertyInfoBox) {
  if (!roomSelect) return;

  if (!propertyId) {
    roomSelect.innerHTML = '<option value="">-- Select Property First --</option>';
    roomSelect.disabled = true;
    if (propertyInfoBox) propertyInfoBox.classList.remove("active");
    return;
  }

  const allRooms = getStoredRooms();
  const properties = getStoredProperties();
  const selectedProp = properties.find(p => p.propertyId === propertyId);

  // Filter for Vacant rooms in this property
  const vacantRooms = allRooms.filter(r => r.propertyId === propertyId && r.roomStatus === "Vacant");

  roomSelect.disabled = false;
  roomSelect.innerHTML = "";

  if (vacantRooms.length === 0) {
    roomSelect.innerHTML = '<option value="">❌ No vacant rooms available in this property</option>';
    roomSelect.disabled = true;
  } else {
    roomSelect.innerHTML = '<option value="">-- Select Vacant Room --</option>';
    vacantRooms.forEach(r => {
      const opt = document.createElement("option");
      opt.value = r.roomId;
      opt.textContent = `Room ${r.roomNumber} (Rent: ₹${Number(r.monthlyRent).toLocaleString('en-IN')}/mo)`;
      roomSelect.appendChild(opt);
    });
  }

  // Update Property Info Box
  if (propertyInfoBox && selectedProp) {
    const totalPropRooms = allRooms.filter(r => r.propertyId === propertyId).length;
    propertyInfoBox.innerHTML = `
      <h4>🏢 ${escapeHtml(selectedProp.propertyName)}</h4>
      <div class="property-info-meta">
        <span>📍 ${escapeHtml(selectedProp.address)}, ${escapeHtml(selectedProp.city)}</span>
        <span>🚪 Total Rooms: <strong>${totalPropRooms}</strong></span>
        <span class="room-availability-badge" style="color: ${vacantRooms.length > 0 ? 'var(--success)' : 'var(--danger)'};">
          ● ${vacantRooms.length} Vacant Room${vacantRooms.length === 1 ? '' : 's'} Available
        </span>
      </div>
    `;
    propertyInfoBox.classList.add("active");
  }
}

/**
 * Validates and submits tenant creation form.
 */
function handleSaveNewTenant(form) {
  clearFormValidationErrors();

  const fullName = document.getElementById("fullName")?.value.trim();
  const phone = document.getElementById("phone")?.value.trim();
  const email = document.getElementById("email")?.value.trim();
  const emergencyContact = document.getElementById("emergencyContact")?.value.trim();
  const permanentAddress = document.getElementById("permanentAddress")?.value.trim();
  const moveInDate = document.getElementById("moveInDate")?.value;
  const propertyId = document.getElementById("propertySelect")?.value;
  const roomId = document.getElementById("roomSelect")?.value;
  const monthlyRent = document.getElementById("monthlyRent")?.value;
  const securityDeposit = document.getElementById("securityDeposit")?.value || 0;

  // Validation Flags
  let isValid = true;

  if (!fullName || fullName.length < 3) {
    setFieldError("fullName", "Full Name must be at least 3 characters.");
    isValid = false;
  }

  const phoneRegex = /^[6-9]\d{9}$/;
  if (!phone || !phoneRegex.test(phone)) {
    setFieldError("phone", "Enter a valid 10-digit mobile number (e.g. 9876543210).");
    isValid = false;
  }

  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  if (!email || !emailRegex.test(email)) {
    setFieldError("email", "Enter a valid email address (e.g. name@example.com).");
    isValid = false;
  }

  if (emergencyContact && !phoneRegex.test(emergencyContact)) {
    setFieldError("emergencyContact", "Emergency contact must be a valid 10-digit number.");
    isValid = false;
  }

  if (!permanentAddress || permanentAddress.length < 5) {
    setFieldError("permanentAddress", "Permanent address is required (at least 5 characters).");
    isValid = false;
  }

  if (!moveInDate) {
    setFieldError("moveInDate", "Please select a joining / move-in date.");
    isValid = false;
  }

  if (!propertyId) {
    setFieldError("propertySelect", "Please select a property.");
    isValid = false;
  }

  if (!roomId) {
    setFieldError("roomSelect", "Please select a vacant room.");
    isValid = false;
  }

  if (monthlyRent === "" || Number(monthlyRent) < 0) {
    setFieldError("monthlyRent", "Monthly rent cannot be negative.");
    isValid = false;
  }

  if (securityDeposit !== "" && Number(securityDeposit) < 0) {
    setFieldError("securityDeposit", "Security deposit cannot be negative.");
    isValid = false;
  }

  if (!isValid) return;

  const tenantPayload = {
    fullName,
    phone,
    email,
    emergencyContact,
    permanentAddress,
    moveInDate,
    propertyId: Number(propertyId),
    roomId: Number(roomId),
    monthlyRent: Number(monthlyRent),
    securityDeposit: Number(securityDeposit),
    tenantStatus: "Active"
  };

  TenantAPI.addTenant(tenantPayload)
    .then(newTenant => {
      showToast(`Tenant '${newTenant.fullName}' registered successfully!`, "success");
      setTimeout(() => {
        window.location.href = "tenants.html";
      }, 1200);
    })
    .catch(err => {
      showToast(err.message, "error");
    });
}

// ==========================================
// 8. EDIT TENANT FORM CONTROLLER (edit-tenant.html)
// ==========================================
function initEditTenantForm() {
  const urlParams = new URLSearchParams(window.location.search);
  const tenantId = urlParams.get("id");

  if (!tenantId) {
    showToast("No tenant ID specified for editing.", "error");
    setTimeout(() => { window.location.href = "tenants.html"; }, 1500);
    return;
  }

  const tenant = getStoredTenants().find(t => t.tenantId === Number(tenantId));
  if (!tenant) {
    showToast("Tenant record not found.", "error");
    setTimeout(() => { window.location.href = "tenants.html"; }, 1500);
    return;
  }

  // Pre-fill fields
  document.getElementById("editTenantId").value = tenant.tenantId;
  document.getElementById("fullName").value = tenant.fullName || "";
  document.getElementById("phone").value = tenant.phone || "";
  document.getElementById("email").value = tenant.email || "";
  document.getElementById("emergencyContact").value = tenant.emergencyContact || "";
  document.getElementById("permanentAddress").value = tenant.permanentAddress || tenant.idProofNumber || "";
  document.getElementById("monthlyRent").value = tenant.monthlyRent || 0;
  document.getElementById("tenantStatus").value = tenant.tenantStatus || "Active";

  // Display locked Property & Room details
  const propDisplay = document.getElementById("displayPropertyName");
  const roomDisplay = document.getElementById("displayRoomNumber");
  if (propDisplay) propDisplay.textContent = tenant.propertyName || "Property " + tenant.propertyId;
  if (roomDisplay) roomDisplay.textContent = "Room " + (tenant.roomNumber || tenant.roomId);

  const form = document.getElementById("editTenantForm");
  if (form) {
    form.addEventListener("submit", (e) => {
      e.preventDefault();
      handleSaveEditedTenant(tenant.tenantId);
    });
  }
}

function handleSaveEditedTenant(tenantId) {
  clearFormValidationErrors();

  const fullName = document.getElementById("fullName")?.value.trim();
  const phone = document.getElementById("phone")?.value.trim();
  const email = document.getElementById("email")?.value.trim();
  const emergencyContact = document.getElementById("emergencyContact")?.value.trim();
  const permanentAddress = document.getElementById("permanentAddress")?.value.trim();
  const monthlyRent = document.getElementById("monthlyRent")?.value;
  const tenantStatus = document.getElementById("tenantStatus")?.value;

  let isValid = true;

  if (!fullName || fullName.length < 3) {
    setFieldError("fullName", "Full Name must be at least 3 characters.");
    isValid = false;
  }

  const phoneRegex = /^[6-9]\d{9}$/;
  if (!phone || !phoneRegex.test(phone)) {
    setFieldError("phone", "Enter a valid 10-digit mobile number.");
    isValid = false;
  }

  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  if (!email || !emailRegex.test(email)) {
    setFieldError("email", "Enter a valid email address.");
    isValid = false;
  }

  if (emergencyContact && !phoneRegex.test(emergencyContact)) {
    setFieldError("emergencyContact", "Emergency contact must be 10 digits.");
    isValid = false;
  }

  if (monthlyRent === "" || Number(monthlyRent) < 0) {
    setFieldError("monthlyRent", "Monthly rent cannot be negative.");
    isValid = false;
  }

  if (!isValid) return;

  const updateData = {
    fullName,
    phone,
    email,
    emergencyContact,
    permanentAddress,
    monthlyRent: Number(monthlyRent),
    tenantStatus
  };

  TenantAPI.updateTenant(tenantId, updateData)
    .then(updated => {
      showToast(`Tenant profile for '${updated.fullName}' updated successfully!`, "success");
      setTimeout(() => {
        window.location.href = "tenants.html";
      }, 1200);
    })
    .catch(err => {
      showToast(err.message, "error");
    });
}

// ==========================================
// 9. ASSIGN PROPERTY FORM CONTROLLER (assign-property.html)
// ==========================================
function initAssignPropertyForm() {
  const tenantSelect = document.getElementById("assignTenantSelect");
  const propSelect = document.getElementById("assignPropertySelect");
  const roomSelect = document.getElementById("assignRoomSelect");
  const rentInput = document.getElementById("assignMonthlyRent");
  const moveInDateInput = document.getElementById("assignMoveInDate");
  const propertyInfoBox = document.getElementById("assignPropertyInfoBox");
  const form = document.getElementById("assignPropertyForm");

  // Set default date to today
  if (moveInDateInput && !moveInDateInput.value) {
    moveInDateInput.value = new Date().toISOString().substring(0, 10);
  }

  // Populate Tenants Dropdown
  const tenants = getStoredTenants();
  if (tenantSelect) {
    tenantSelect.innerHTML = '<option value="">-- Choose Tenant --</option>';
    tenants.forEach(t => {
      const opt = document.createElement("option");
      opt.value = t.tenantId;
      opt.textContent = `${t.fullName} (Currently: ${t.propertyName || 'Unassigned'} - Room ${t.roomNumber || 'None'})`;
      tenantSelect.appendChild(opt);
    });
  }

  // Populate Properties Dropdown
  const properties = getStoredProperties();
  if (propSelect) {
    propSelect.innerHTML = '<option value="">-- Choose Property --</option>';
    properties.forEach(p => {
      const opt = document.createElement("option");
      opt.value = p.propertyId;
      opt.textContent = `${p.propertyName} (${p.propertyType} - ${p.city})`;
      propSelect.appendChild(opt);
    });

    propSelect.addEventListener("change", () => {
      const selectedPropId = Number(propSelect.value);
      updateRoomDropdownForProperty(selectedPropId, roomSelect, rentInput, propertyInfoBox);
    });
  }

  // Room Select change auto-fills rent
  if (roomSelect) {
    roomSelect.addEventListener("change", () => {
      const selectedRoomId = Number(roomSelect.value);
      const rooms = getStoredRooms();
      const r = rooms.find(rm => rm.roomId === selectedRoomId);
      if (r && rentInput) {
        rentInput.value = r.monthlyRent;
      }
    });
  }

  // Form submission opens confirmation modal
  if (form) {
    form.addEventListener("submit", (e) => {
      e.preventDefault();
      handleAssignmentConfirmationCheck();
    });
  }

  // Modal confirm assignment button
  const confirmAssignBtn = document.getElementById("confirmAssignmentBtn");
  if (confirmAssignBtn) {
    confirmAssignBtn.addEventListener("click", executePropertyAssignment);
  }

  setupModalDismissals();
}

let pendingAssignmentData = null;

function handleAssignmentConfirmationCheck() {
  clearFormValidationErrors();

  const tenantId = document.getElementById("assignTenantSelect")?.value;
  const propertyId = document.getElementById("assignPropertySelect")?.value;
  const roomId = document.getElementById("assignRoomSelect")?.value;
  const monthlyRent = document.getElementById("assignMonthlyRent")?.value;
  const moveInDate = document.getElementById("assignMoveInDate")?.value;

  let isValid = true;
  if (!tenantId) {
    setFieldError("assignTenantSelect", "Please select a tenant.");
    isValid = false;
  }
  if (!propertyId) {
    setFieldError("assignPropertySelect", "Please select a property.");
    isValid = false;
  }
  if (!roomId) {
    setFieldError("assignRoomSelect", "Please select an available vacant room.");
    isValid = false;
  }
  if (monthlyRent === "" || Number(monthlyRent) < 0) {
    setFieldError("assignMonthlyRent", "Monthly rent cannot be negative.");
    isValid = false;
  }
  if (!moveInDate) {
    setFieldError("assignMoveInDate", "Please specify assignment / move-in date.");
    isValid = false;
  }

  if (!isValid) return;

  const tenants = getStoredTenants();
  const properties = getStoredProperties();
  const rooms = getStoredRooms();

  const tenant = tenants.find(t => t.tenantId === Number(tenantId));
  const prop = properties.find(p => p.propertyId === Number(propertyId));
  const room = rooms.find(r => r.roomId === Number(roomId));

  // Store pending assignment
  pendingAssignmentData = {
    tenantId: Number(tenantId),
    propertyId: Number(propertyId),
    roomId: Number(roomId),
    monthlyRent: Number(monthlyRent),
    moveInDate
  };

  // Populate Confirmation Dialog text
  const promptDetails = document.getElementById("assignmentPromptDetails");
  if (promptDetails && tenant && prop && room) {
    promptDetails.innerHTML = `
      Assign <strong>${escapeHtml(tenant.fullName)}</strong> to 
      <strong>${escapeHtml(prop.propertyName)}</strong> (Room <strong>${escapeHtml(room.roomNumber)}</strong>) 
      at <strong>₹${Number(monthlyRent).toLocaleString('en-IN')}/month</strong> effective from <strong>${formatDisplayDate(moveInDate)}</strong>?
    `;
  }

  const modal = document.getElementById("assignConfirmModal");
  if (modal) modal.classList.add("active");
}

function executePropertyAssignment() {
  if (!pendingAssignmentData) return;

  const { tenantId, propertyId, roomId, monthlyRent, moveInDate } = pendingAssignmentData;

  TenantAPI.assignProperty(tenantId, propertyId, roomId, monthlyRent, moveInDate)
    .then(updatedTenant => {
      closeAllModals();
      showToast(`Room assigned successfully to ${updatedTenant.fullName}!`, "success");
      setTimeout(() => {
        window.location.href = "tenants.html";
      }, 1300);
    })
    .catch(err => {
      closeAllModals();
      showToast(err.message, "error");
    });
}

// ==========================================
// 10. FORM VALIDATION HELPERS
// ==========================================
function setFieldError(fieldId, errorMsg) {
  const el = document.getElementById(fieldId);
  if (el) {
    el.classList.add("is-invalid");
  }
  const errorEl = document.getElementById(`${fieldId}Error`);
  if (errorEl) {
    errorEl.textContent = errorMsg;
  }
}

function clearFormValidationErrors() {
  const invalids = document.querySelectorAll(".is-invalid");
  invalids.forEach(el => el.classList.remove("is-invalid"));

  const errorMsgs = document.querySelectorAll(".form-error-feedback");
  errorMsgs.forEach(el => el.textContent = "");
}

// ==========================================
// 11. GENERAL UTILITIES & SIDEBAR NAVIGATION
// ==========================================
function getInitials(name) {
  if (!name) return "TN";
  const parts = name.trim().split(/\s+/);
  if (parts.length >= 2) {
    return (parts[0][0] + parts[1][0]).toUpperCase();
  }
  return parts[0].substring(0, 2).toUpperCase();
}

function getStatusClass(status) {
  if (!status) return "status-active";
  switch (status.toLowerCase()) {
    case "active": return "status-active";
    case "moved out": return "status-moved-out";
    case "pending": return "status-pending";
    default: return "status-active";
  }
}

function formatDisplayDate(dateStr) {
  if (!dateStr) return "N/A";
  try {
    const d = new Date(dateStr);
    if (isNaN(d.getTime())) return dateStr;
    return d.toLocaleDateString("en-IN", { day: "2-digit", month: "short", year: "numeric" });
  } catch (e) {
    return dateStr;
  }
}

function escapeHtml(str) {
  if (!str) return "";
  return String(str)
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#039;");
}

function showToast(message, type = "info") {
  let container = document.querySelector(".toast-container");
  if (!container) {
    container = document.createElement("div");
    container.className = "toast-container";
    document.body.appendChild(container);
  }

  const toast = document.createElement("div");
  toast.className = `toast toast-${type}`;
  let icon = "ℹ️";
  if (type === "success") icon = "✅";
  if (type === "error") icon = "⚠️";

  toast.innerHTML = `<span>${icon}</span><span>${escapeHtml(message)}</span>`;
  container.appendChild(toast);

  setTimeout(() => {
    toast.style.opacity = "0";
    toast.style.transform = "translateX(50px)";
    toast.style.transition = "all 0.3s ease";
    setTimeout(() => {
      toast.remove();
    }, 300);
  }, 3500);
}

function setupNavigationAndSidebar() {
  const toggleBtn = document.getElementById("sidebarToggleBtn");
  const sidebar = document.getElementById("appSidebar");

  if (toggleBtn && sidebar) {
    toggleBtn.addEventListener("click", () => {
      sidebar.classList.toggle("collapsed");
      document.body.classList.toggle("sidebar-collapsed");
    });
  }

}

function setupActiveUser() {
  const avatar = document.getElementById("landlordAvatarInitials");
  const nameLabel = document.getElementById("landlordDisplayName");
  if (avatar) avatar.textContent = "AS";
  if (nameLabel) nameLabel.textContent = "Aditi Sharma";
}

function showModuleNotice(moduleName, leadName) {
  alert(`RentProof Project Notice:\nThe ${moduleName} module is assigned to ${leadName}.`);
}
