/**
 * RENTPROOF - PROPERTY MANAGEMENT JAVASCRIPT
 * Module: Property Management
 * Author: Aditi Sharma
 * 
 * Features:
 * - Reactive property catalog management with localStorage persistence
 * - Real-time search by name, city, address
 * - Filtering by Property Type (PG, Flat, Apartment, House)
 * - Live statistical calculations (Total Properties, Total Rooms, Occupied, Vacant)
 * - Add, Edit, Delete CRUD workflows
 * - Delete safety confirmation modal to protect data integrity
 * - Responsive collapsible sidebar with preference persistence
 * - Ready for Core Java JDBC backend integration
 */

// Default Seed Properties (strictly aligned with database/rentproof.sql)
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
    occupiedRooms: 4,
    monthlyRent: 8500,
    description: "Premium student PG with 3-time meals, high-speed Wi-Fi, power backup and 24x7 biometric security."
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
    occupiedRooms: 2,
    monthlyRent: 28000,
    description: "Spacious semi-furnished apartment for working professionals, modular kitchen and reserved car parking."
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
    occupiedRooms: 1,
    monthlyRent: 34000,
    description: "Modern 4-room independent apartment with club house access, swimming pool and round-the-clock maintenance."
  }
];

// Initialize on page load
document.addEventListener("DOMContentLoaded", () => {
  setupNavigationAndSidebar();
  setupActiveUser();

  // Detect which page is currently open
  if (document.getElementById("propertiesGrid")) {
    initPropertiesDashboard();
  } else if (document.getElementById("addPropertyForm")) {
    initAddPropertyForm();
  } else if (document.getElementById("editPropertyForm")) {
    initEditPropertyForm();
  }
});

/**
 * Loads properties from localStorage or seeds initial demo dataset.
 */
function getProperties() {
  const stored = localStorage.getItem("rentproof_properties");
  if (stored) {
    try {
      return JSON.parse(stored);
    } catch (e) {
      console.error("Error parsing stored properties:", e);
    }
  }
  // If empty, initialize with seed
  saveProperties(SEED_PROPERTIES);
  return [...SEED_PROPERTIES];
}

/**
 * Saves properties array into localStorage.
 */
function saveProperties(properties) {
  localStorage.setItem("rentproof_properties", JSON.stringify(properties));
}

/**
 * Synchronizes and controls the collapsible sidebar navigation.
 */
function setupNavigationAndSidebar() {
  const sidebar = document.getElementById("appSidebar");
  const toggleBtn = document.getElementById("sidebarToggleBtn");

  if (!sidebar || !toggleBtn) return;

  // Restore saved state from localStorage
  const isCollapsed = localStorage.getItem("rentproof_sidebar_collapsed") === "true";
  if (isCollapsed) {
    sidebar.classList.add("collapsed");
  }

  // Toggle on button click
  toggleBtn.addEventListener("click", () => {
    sidebar.classList.toggle("collapsed");
    const collapsed = sidebar.classList.contains("collapsed");
    localStorage.setItem("rentproof_sidebar_collapsed", collapsed);
  });

  // Mobile drawer support
  window.addEventListener("resize", () => {
    if (window.innerWidth <= 768) {
      sidebar.classList.remove("collapsed");
    }
  });
}

/**
 * Displays active landlord session in header.
 */
function setupActiveUser() {
  const user = JSON.parse(localStorage.getItem("rentproof_user")) || {
    fullName: "Aditi Sharma",
    email: "aditi.landlord@rentproof.com"
  };

  const nameEl = document.getElementById("landlordDisplayName");
  const avatarEl = document.getElementById("landlordAvatarInitials");

  if (nameEl) {
    nameEl.textContent = user.fullName;
  }
  if (avatarEl) {
    const initials = user.fullName
      .split(" ")
      .map((n) => n[0])
      .join("")
      .substring(0, 2)
      .toUpperCase();
    avatarEl.textContent = initials || "AS";
  }

  // Setup logout handler
  const logoutBtn = document.getElementById("logoutBtn");
  if (logoutBtn) {
    logoutBtn.addEventListener("click", (e) => {
      e.preventDefault();
      if (confirm("Are you sure you want to sign out of RentProof?")) {
        localStorage.removeItem("rentproof_user");
        window.location.href = "../auth/login.html";
      }
    });
  }
}

/**
 * Placeholder alert for other team members' modules.
 */
function showModuleNotice(moduleName, memberName) {
  alert(
    `RentProof Module Notice:\nThe ${moduleName} module is assigned to ${memberName}.\nAditi's module (Database, Authentication & Property Management) provides the database foreign keys for this module.`
  );
}

/* ==============================================================================
   DASHBOARD / PROPERTIES LISTING (properties.html)
   ============================================================================== */

let currentFilterType = "ALL";
let currentSearchQuery = "";

function initPropertiesDashboard() {
  calculateAndRenderStats();
  renderFilteredProperties();

  // Search input handler
  const searchInput = document.getElementById("propertySearchInput");
  if (searchInput) {
    searchInput.addEventListener("input", (e) => {
      currentSearchQuery = e.target.value.trim().toLowerCase();
      renderFilteredProperties();
    });
  }

  // Type filter dropdown handler
  const typeFilter = document.getElementById("propertyTypeFilter");
  if (typeFilter) {
    typeFilter.addEventListener("change", (e) => {
      currentFilterType = e.target.value;
      renderFilteredProperties();
    });
  }
}

/**
 * Computes and renders top statistics cards.
 */
function calculateAndRenderStats() {
  const properties = getProperties();

  let totalProperties = properties.length;
  let totalRooms = 0;
  let totalOccupied = 0;

  for (let i = 0; i < properties.length; i++) {
    const p = properties[i];
    const rooms = parseInt(p.totalRooms) || 1;
    const occupied = parseInt(p.occupiedRooms) || 0;
    totalRooms += rooms;
    totalOccupied += occupied;
  }

  const totalVacant = Math.max(0, totalRooms - totalOccupied);

  const statPropEl = document.getElementById("statTotalProperties");
  const statRoomsEl = document.getElementById("statTotalRooms");
  const statOccupiedEl = document.getElementById("statOccupiedRooms");
  const statVacantEl = document.getElementById("statVacantRooms");

  if (statPropEl) statPropEl.textContent = totalProperties;
  if (statRoomsEl) statRoomsEl.textContent = totalRooms;
  if (statOccupiedEl) statOccupiedEl.textContent = totalOccupied;
  if (statVacantEl) statVacantEl.textContent = totalVacant;
}

/**
 * Filters and renders properties inside the grid.
 */
function renderFilteredProperties() {
  const grid = document.getElementById("propertiesGrid");
  const emptyState = document.getElementById("emptyStateBox");
  if (!grid) return;

  const all = getProperties();

  const filtered = all.filter((p) => {
    // Type filter check
    const matchesType = currentFilterType === "ALL" || p.propertyType.toUpperCase() === currentFilterType.toUpperCase();

    // Search query check (name, city, address)
    const matchesQuery =
      !currentSearchQuery ||
      p.propertyName.toLowerCase().includes(currentSearchQuery) ||
      p.city.toLowerCase().includes(currentSearchQuery) ||
      p.address.toLowerCase().includes(currentSearchQuery);

    return matchesType && matchesQuery;
  });

  if (filtered.length === 0) {
    grid.innerHTML = "";
    if (emptyState) emptyState.style.display = "block";
    return;
  }

  if (emptyState) emptyState.style.display = "none";

  let html = "";
  for (let i = 0; i < filtered.length; i++) {
    const prop = filtered[i];
    const totalRooms = parseInt(prop.totalRooms) || 1;
    const occupied = parseInt(prop.occupiedRooms) || 0;
    const vacant = Math.max(0, totalRooms - occupied);
    const occupancyPercent = Math.min(100, Math.round((occupied / totalRooms) * 100));

    // Badge styling
    let badgeClass = "badge-pg";
    if (prop.propertyType === "Flat") badgeClass = "badge-flat";
    else if (prop.propertyType === "Apartment") badgeClass = "badge-apartment";
    else if (prop.propertyType === "House") badgeClass = "badge-house";

    html += `
      <div class="property-card" id="card-${prop.propertyId}">
        <div class="property-card-top">
          <div class="property-name-wrap">
            <h3 class="property-name-heading">${escapeHtml(prop.propertyName)}</h3>
            <div class="property-location-tag">
              <span>📍 ${escapeHtml(prop.city)}, ${escapeHtml(prop.state)}</span>
            </div>
          </div>
          <span class="badge ${badgeClass}">${escapeHtml(prop.propertyType)}</span>
        </div>

        <div class="property-card-body">
          <div class="property-address-text">
            ${escapeHtml(prop.address)}, PIN: ${escapeHtml(prop.pincode)}
          </div>

          <div class="property-metrics-row">
            <div class="metric-item">
              <span class="metric-label">Total Rooms</span>
              <span class="metric-val">${totalRooms} Units</span>
            </div>
            <div class="metric-item" style="text-align: right;">
              <span class="metric-label">Monthly Rent</span>
              <span class="metric-val metric-rent">₹${Number(prop.monthlyRent).toLocaleString('en-IN')}</span>
            </div>
          </div>

          <div class="occupancy-meter">
            <div class="meter-header">
              <span class="meter-label">Occupancy Status</span>
              <span class="meter-counts">${occupied} Occupied &bull; ${vacant} Vacant</span>
            </div>
            <div class="meter-bar-track">
              <div class="meter-bar-fill" style="width: ${occupancyPercent}%;"></div>
            </div>
          </div>
        </div>

        <div class="property-card-actions">
          <button class="btn btn-outline btn-sm" onclick="viewPropertyDetails(${prop.propertyId})">
            🔍 Details
          </button>
          <div class="card-action-btns">
            <a href="edit-property.html?id=${prop.propertyId}" class="btn btn-secondary btn-sm">
              ✏️ Edit
            </a>
            <button class="btn btn-danger btn-sm" onclick="promptDeleteProperty(${prop.propertyId})">
              🗑️ Delete
            </button>
          </div>
        </div>
      </div>
    `;
  }

  grid.innerHTML = html;
}

/**
 * Opens details modal with complete breakdown.
 */
function viewPropertyDetails(propertyId) {
  const properties = getProperties();
  const prop = properties.find((p) => p.propertyId === propertyId);
  if (!prop) return;

  const total = parseInt(prop.totalRooms) || 1;
  const occupied = parseInt(prop.occupiedRooms) || 0;
  const vacant = Math.max(0, total - occupied);

  const modalBody = document.getElementById("detailsModalBody");
  if (!modalBody) return;

  modalBody.innerHTML = `
    <div style="display: flex; flex-direction: column; gap: 14px;">
      <div>
        <h4 style="color: var(--navy); margin-bottom: 4px;">${escapeHtml(prop.propertyName)}</h4>
        <span class="badge badge-pg">${escapeHtml(prop.propertyType)}</span>
        <span style="font-size: 0.85rem; color: var(--slate-muted); margin-left: 8px;">Property ID: #${prop.propertyId}</span>
      </div>

      <div style="background-color: var(--ivory); padding: 12px; border-radius: 8px; border: 1px solid var(--sand-light);">
        <strong>Address:</strong><br>
        ${escapeHtml(prop.address)}<br>
        ${escapeHtml(prop.city)}, ${escapeHtml(prop.state)} - ${escapeHtml(prop.pincode)}
      </div>

      <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 12px;">
        <div style="background-color: var(--ivory); padding: 10px; border-radius: 8px;">
          <small style="color: var(--slate-muted);">Monthly Rent</small><br>
          <strong style="color: var(--steel-blue); font-size: 1.1rem;">₹${Number(prop.monthlyRent).toLocaleString('en-IN')} / mo</strong>
        </div>
        <div style="background-color: var(--ivory); padding: 10px; border-radius: 8px;">
          <small style="color: var(--slate-muted);">Room Inventory</small><br>
          <strong>${total} Total (${occupied} Occupied, ${vacant} Vacant)</strong>
        </div>
      </div>

      <div>
        <strong>Description & Amenities:</strong>
        <p style="margin-top: 4px; font-size: 0.9rem; color: var(--slate);">
          ${escapeHtml(prop.description || "No specific description provided.")}
        </p>
      </div>

      <div style="padding: 10px; background-color: var(--sand-soft); border-radius: 6px; font-size: 0.8rem; color: var(--navy);">
        💡 <strong>Database Foreign Key:</strong> Shweta's <code>tenants</code> and Aanvi's <code>payments</code> tables link to this property via <code>property_id = ${prop.propertyId}</code>.
      </div>
    </div>
  `;

  document.getElementById("propertyDetailsModal").classList.add("active");
}

function closeDetailsModal() {
  const modal = document.getElementById("propertyDetailsModal");
  if (modal) modal.classList.remove("active");
}

/**
 * Opens delete confirmation modal.
 */
let propertyPendingDeletion = null;

function promptDeleteProperty(propertyId) {
  const properties = getProperties();
  const prop = properties.find((p) => p.propertyId === propertyId);
  if (!prop) return;

  propertyPendingDeletion = propertyId;
  const nameEl = document.getElementById("deletePropertyNameTarget");
  if (nameEl) nameEl.textContent = `"${prop.propertyName}" (ID: #${prop.propertyId})`;

  document.getElementById("deleteConfirmModal").classList.add("active");
}

function closeDeleteModal() {
  propertyPendingDeletion = null;
  const modal = document.getElementById("deleteConfirmModal");
  if (modal) modal.classList.remove("active");
}

function confirmExecuteDelete() {
  if (!propertyPendingDeletion) return;

  let properties = getProperties();
  properties = properties.filter((p) => p.propertyId !== propertyPendingDeletion);
  saveProperties(properties);

  closeDeleteModal();
  calculateAndRenderStats();
  renderFilteredProperties();

  // Show toast notification
  alert("Property successfully removed from inventory.");
}

/* ==============================================================================
   ADD PROPERTY (add-property.html)
   ============================================================================== */

function initAddPropertyForm() {
  const form = document.getElementById("addPropertyForm");
  if (!form) return;

  form.addEventListener("submit", (e) => {
    e.preventDefault();

    const nameInput = document.getElementById("propName");
    const typeSelect = document.getElementById("propType");
    const addressInput = document.getElementById("propAddress");
    const cityInput = document.getElementById("propCity");
    const stateInput = document.getElementById("propState");
    const pincodeInput = document.getElementById("propPincode");
    const roomsInput = document.getElementById("propTotalRooms");
    const rentInput = document.getElementById("propRent");
    const descInput = document.getElementById("propDescription");

    let isValid = true;

    // Name Validation
    if (!nameInput.value.trim() || nameInput.value.trim().length < 3) {
      showFieldError("propNameError", "Property Name must be at least 3 characters.");
      nameInput.classList.add("is-invalid");
      isValid = false;
    } else {
      clearFieldError("propNameError");
      nameInput.classList.remove("is-invalid");
    }

    // Type Validation
    if (!typeSelect.value) {
      showFieldError("propTypeError", "Please select a valid property type.");
      typeSelect.classList.add("is-invalid");
      isValid = false;
    } else {
      clearFieldError("propTypeError");
      typeSelect.classList.remove("is-invalid");
    }

    // Address
    if (!addressInput.value.trim()) {
      showFieldError("propAddressError", "Street address is required.");
      addressInput.classList.add("is-invalid");
      isValid = false;
    } else {
      clearFieldError("propAddressError");
      addressInput.classList.remove("is-invalid");
    }

    // City
    if (!cityInput.value.trim()) {
      showFieldError("propCityError", "City is required.");
      cityInput.classList.add("is-invalid");
      isValid = false;
    } else {
      clearFieldError("propCityError");
      cityInput.classList.remove("is-invalid");
    }

    // State
    if (!stateInput.value.trim()) {
      showFieldError("propStateError", "State is required.");
      stateInput.classList.add("is-invalid");
      isValid = false;
    } else {
      clearFieldError("propStateError");
      stateInput.classList.remove("is-invalid");
    }

    // Pincode (6 digits)
    const pinRegex = /^[1-9][0-9]{5}$/;
    if (!pinRegex.test(pincodeInput.value.trim())) {
      showFieldError("propPincodeError", "Please enter a valid 6-digit Indian PIN code.");
      pincodeInput.classList.add("is-invalid");
      isValid = false;
    } else {
      clearFieldError("propPincodeError");
      pincodeInput.classList.remove("is-invalid");
    }

    // Total Rooms
    const totalRooms = parseInt(roomsInput.value);
    if (isNaN(totalRooms) || totalRooms < 1) {
      showFieldError("propRoomsError", "Total rooms must be at least 1.");
      roomsInput.classList.add("is-invalid");
      isValid = false;
    } else {
      clearFieldError("propRoomsError");
      roomsInput.classList.remove("is-invalid");
    }

    // Rent
    const rent = parseFloat(rentInput.value);
    if (isNaN(rent) || rent < 0) {
      showFieldError("propRentError", "Monthly rent cannot be negative.");
      rentInput.classList.add("is-invalid");
      isValid = false;
    } else {
      clearFieldError("propRentError");
      rentInput.classList.remove("is-invalid");
    }

    if (!isValid) return;

    // Construct new property
    const properties = getProperties();
    const newProperty = {
      propertyId: Date.now(),
      landlordId: 1, // Current logged-in landlord
      propertyName: nameInput.value.trim(),
      propertyType: typeSelect.value,
      address: addressInput.value.trim(),
      city: cityInput.value.trim(),
      state: stateInput.value.trim(),
      pincode: pincodeInput.value.trim(),
      totalRooms: totalRooms,
      occupiedRooms: 0,
      monthlyRent: rent,
      description: descInput.value.trim()
    };

    properties.unshift(newProperty);
    saveProperties(properties);

    alert("Property added successfully! Redirecting to properties dashboard...");
    window.location.href = "properties.html";
  });
}

/* ==============================================================================
   EDIT PROPERTY (edit-property.html)
   ============================================================================== */

function initEditPropertyForm() {
  const form = document.getElementById("editPropertyForm");
  if (!form) return;

  const urlParams = new URLSearchParams(window.location.search);
  const propertyId = parseInt(urlParams.get("id"));

  const properties = getProperties();
  const property = properties.find((p) => p.propertyId === propertyId);

  if (!property) {
    alert("Property record not found. Returning to dashboard.");
    window.location.href = "properties.html";
    return;
  }

  // Pre-fill form fields
  document.getElementById("editPropertyId").value = property.propertyId;
  document.getElementById("editPropName").value = property.propertyName;
  document.getElementById("editPropType").value = property.propertyType;
  document.getElementById("editPropAddress").value = property.address;
  document.getElementById("editPropCity").value = property.city;
  document.getElementById("editPropState").value = property.state;
  document.getElementById("editPropPincode").value = property.pincode;
  document.getElementById("editPropTotalRooms").value = property.totalRooms;
  document.getElementById("editPropRent").value = property.monthlyRent;
  document.getElementById("editPropDescription").value = property.description || "";

  form.addEventListener("submit", (e) => {
    e.preventDefault();

    const nameInput = document.getElementById("editPropName");
    const typeSelect = document.getElementById("editPropType");
    const addressInput = document.getElementById("editPropAddress");
    const cityInput = document.getElementById("editPropCity");
    const stateInput = document.getElementById("editPropState");
    const pincodeInput = document.getElementById("editPropPincode");
    const roomsInput = document.getElementById("editPropTotalRooms");
    const rentInput = document.getElementById("editPropRent");
    const descInput = document.getElementById("editPropDescription");

    let isValid = true;

    if (!nameInput.value.trim() || nameInput.value.trim().length < 3) {
      showFieldError("editPropNameError", "Property Name must be at least 3 characters.");
      nameInput.classList.add("is-invalid");
      isValid = false;
    } else {
      clearFieldError("editPropNameError");
      nameInput.classList.remove("is-invalid");
    }

    const pinRegex = /^[1-9][0-9]{5}$/;
    if (!pinRegex.test(pincodeInput.value.trim())) {
      showFieldError("editPropPincodeError", "Please enter a valid 6-digit Indian PIN code.");
      pincodeInput.classList.add("is-invalid");
      isValid = false;
    } else {
      clearFieldError("editPropPincodeError");
      pincodeInput.classList.remove("is-invalid");
    }

    const totalRooms = parseInt(roomsInput.value);
    if (isNaN(totalRooms) || totalRooms < 1) {
      showFieldError("editPropRoomsError", "Total rooms must be at least 1.");
      roomsInput.classList.add("is-invalid");
      isValid = false;
    } else {
      clearFieldError("editPropRoomsError");
      roomsInput.classList.remove("is-invalid");
    }

    const rent = parseFloat(rentInput.value);
    if (isNaN(rent) || rent < 0) {
      showFieldError("editPropRentError", "Monthly rent cannot be negative.");
      rentInput.classList.add("is-invalid");
      isValid = false;
    } else {
      clearFieldError("editPropRentError");
      rentInput.classList.remove("is-invalid");
    }

    if (!isValid) return;

    // Update property details in storage
    const allProps = getProperties();
    const index = allProps.findIndex((p) => p.propertyId === propertyId);
    if (index !== -1) {
      allProps[index].propertyName = nameInput.value.trim();
      allProps[index].propertyType = typeSelect.value;
      allProps[index].address = addressInput.value.trim();
      allProps[index].city = cityInput.value.trim();
      allProps[index].state = stateInput.value.trim();
      allProps[index].pincode = pincodeInput.value.trim();
      allProps[index].totalRooms = totalRooms;
      allProps[index].monthlyRent = rent;
      allProps[index].description = descInput.value.trim();

      saveProperties(allProps);
      alert("Property updated successfully!");
      window.location.href = "properties.html";
    }
  });
}

// Utility Helpers
function escapeHtml(text) {
  if (!text) return "";
  return String(text)
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#039;");
}

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
