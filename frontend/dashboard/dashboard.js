/**
 * RENTPROOF - LANDLORD DASHBOARD CONTROLLER (dashboard.js)
 * Module: Dashboard & Portfolio Overview
 * Author: Aditi Sharma
 * 
 * Features:
 * - Real-time statistics aggregation directly from MySQL via JDBC API (/api/dashboard/summary)
 * - Explicit connection status when the backend is unavailable
 * - Welcome greeting with active Landlord session and live date formatting
 * - Dynamic Property room breakdown (Total, Occupied, Vacant)
 * - Recent payments audit table & Pending rent collections
 * - Upcoming scheduled reminders feed
 * - Full responsive sidebar state persistence
 */

document.addEventListener("DOMContentLoaded", async () => {
  if (!(await window.RentProofSecurity.requireSession())) return;
  setupNavigationAndSidebar();
  setupActiveUser();
  displayLiveDate();
  const searchInput = document.getElementById("dashboardSearchInput");
  if (searchInput) {
    searchInput.addEventListener("keydown", (event) => {
      if (event.key === "Enter") window.location.href = "../properties/properties.html";
    });
  }
  const notificationsButton = document.getElementById("dashboardNotificationsBtn");
  if (notificationsButton) {
    notificationsButton.addEventListener("click", () => {
      window.location.href = "../reminders/reminders.html";
    });
  }
  await loadDashboardData();
});

/**
 * Synchronizes and controls the collapsible sidebar navigation.
 */
function setupNavigationAndSidebar() {
  const sidebar = document.getElementById("appSidebar");
  const toggleBtn = document.getElementById("sidebarToggleBtn");

  if (!sidebar || !toggleBtn) return;

  const isCollapsed = localStorage.getItem("rentproof_sidebar_collapsed") === "true";
  if (isCollapsed) {
    sidebar.classList.add("collapsed");
  }

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

/**
 * Loads current logged-in landlord profile.
 */
function setupActiveUser() {
  const user = JSON.parse(localStorage.getItem("rentproof_user")) || {
    fullName: "Landlord",
    email: ""
  };

  const nameEl = document.getElementById("welcomeLandlordName");
  if (nameEl) nameEl.textContent = user.fullName;

  const headerAvatarEl = document.getElementById("headerAvatarInitials");
  if (headerAvatarEl) {
    const initials = user.fullName
      .split(" ")
      .map(n => n[0])
      .join("")
      .toUpperCase()
      .substring(0, 2);
    headerAvatarEl.textContent = initials || "AS";
  }

  const headerNameEl = document.getElementById("headerLandlordName");
  if (headerNameEl) headerNameEl.textContent = user.fullName;
}

/**
 * Renders the live formatted calendar date in the welcome section.
 */
function displayLiveDate() {
  const dateEl = document.getElementById("currentDateDisplay");
  if (!dateEl) return;

  const now = new Date();
  const options = { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' };
  dateEl.textContent = now.toLocaleDateString('en-IN', options);
}

/**
 * Fetches dashboard analytics from the authenticated Java backend.
 */
async function loadDashboardData() {
  try {
    const response = await fetch("/api/dashboard/summary", {
      credentials: "same-origin",
      headers: { "Accept": "application/json" }
    });
    if (!response.ok) throw new Error(`Dashboard API returned HTTP ${response.status}.`);
    const data = await response.json();
    if (!data.success || !data.stats) throw new Error("Dashboard API returned an invalid response.");
    renderDashboard(data.stats, data.properties, data.recentPayments, data.pendingRent, data.reminders, true);
  } catch (error) {
    console.error("[Dashboard] Failed to load live dashboard data:", error);
    const modeEl = document.getElementById("connectionModeText");
    if (modeEl) modeEl.textContent = "Database unavailable";
    [
      "statTotalProperties",
      "statTotalRooms",
      "statOccupiedRooms",
      "statTotalTenants",
      "statRentCollected",
      "statPendingRent"
    ].forEach((id) => {
      const element = document.getElementById(id);
      if (element) element.textContent = "N/A";
    });
    ["propertiesTableBody", "recentPaymentsBody", "pendingRentBody"].forEach((id) => {
      const element = document.getElementById(id);
      if (element) element.textContent = "Dashboard data is unavailable.";
    });
    const reminders = document.getElementById("remindersListContainer");
    if (reminders) reminders.textContent = "Dashboard data is unavailable.";
  }
}

/**
 * Renders all dashboard sections.
 */
function renderDashboard(stats, properties, recentPayments, pendingRent, reminders, isLiveBackend) {
  // 1. Statistics Cards
  document.getElementById("statTotalProperties").textContent = stats.totalProperties || 0;
  document.getElementById("statTotalRooms").textContent = stats.totalRooms || 0;
  document.getElementById("statOccupiedRooms").textContent = stats.occupiedRooms || 0;
  document.getElementById("statTotalTenants").textContent = stats.totalTenants || 0;
  document.getElementById("statRentCollected").textContent = formatCurrency(stats.rentCollected || 0);
  document.getElementById("statPendingRent").textContent = formatCurrency(stats.pendingRent || 0);

  // Connection mode indicator
  const modeEl = document.getElementById("connectionModeText");
  if (modeEl) {
    modeEl.textContent = isLiveBackend ? "Live MySQL JDBC Connected" : "Local Database Mode";
  }

  // 2. Property Overview Table
  renderPropertyOverview(properties);

  // 3. Recent Payments Table
  renderRecentPayments(recentPayments);

  // 4. Pending Rent Table
  renderPendingRent(pendingRent);

  // 5. Upcoming Reminders
  renderUpcomingReminders(reminders);
}

/**
 * Renders the Property Inventory Overview Table.
 */
function renderPropertyOverview(properties) {
  const tbody = document.getElementById("propertiesTableBody");
  if (!tbody) return;

  if (!properties || properties.length === 0) {
    tbody.innerHTML = `
      <tr>
        <td colspan="5" class="empty-placeholder">
          <div class="icon">🏢</div>
          <p>No properties registered yet. Click <strong>Add Property</strong> to get started.</p>
        </td>
      </tr>
    `;
    return;
  }

  tbody.innerHTML = properties.map(p => `
    <tr>
      <td>
        <strong style="color: var(--navy);">${escapeHtml(p.propertyName)}</strong>
        <div style="font-size: 0.78rem; color: var(--steel-blue);">${escapeHtml(p.propertyType || 'Residential')}</div>
      </td>
      <td>${escapeHtml(p.location)}</td>
      <td><strong>${p.totalRooms}</strong> Rooms</td>
      <td>
        <div class="room-breakdown">
          <span class="badge-occupied">${p.occupiedRooms} Occupied</span>
          <span class="badge-vacant">${p.vacantRooms} Vacant</span>
        </div>
      </td>
      <td>
        <a href="../properties/edit-property.html?id=${p.propertyId}" class="card-header-link">Manage →</a>
      </td>
    </tr>
  `).join("");
}

/**
 * Renders Recent Payment Transactions.
 */
function renderRecentPayments(payments) {
  const tbody = document.getElementById("recentPaymentsBody");
  if (!tbody) return;

  if (!payments || payments.length === 0) {
    tbody.innerHTML = `
      <tr>
        <td colspan="5" class="empty-placeholder">
          <div class="icon">💳</div>
          <p>No recent payment records found in database.</p>
        </td>
      </tr>
    `;
    return;
  }

  tbody.innerHTML = payments.map(pm => {
    const statusClass = (pm.paymentStatus || "").toLowerCase();
    return `
      <tr>
        <td><strong>${escapeHtml(pm.tenantName)}</strong></td>
        <td>${escapeHtml(pm.propertyName)}</td>
        <td><strong>${formatCurrency(pm.amount)}</strong></td>
        <td>${escapeHtml(pm.paymentDate)}</td>
        <td><span class="status-pill ${statusClass}">${escapeHtml(pm.paymentStatus)}</span></td>
      </tr>
    `;
  }).join("");
}

/**
 * Renders Pending / Overdue Rent Collections.
 */
function renderPendingRent(pending) {
  const tbody = document.getElementById("pendingRentBody");
  if (!tbody) return;

  if (!pending || pending.length === 0) {
    tbody.innerHTML = `
      <tr>
        <td colspan="5" class="empty-placeholder">
          <div class="icon">✅</div>
          <p>All rental payments are currently up to date! Zero pending dues.</p>
        </td>
      </tr>
    `;
    return;
  }

  tbody.innerHTML = pending.map(pr => `
    <tr>
      <td><strong>${escapeHtml(pr.tenantName)}</strong></td>
      <td>${escapeHtml(pr.propertyName)}</td>
      <td><strong style="color: var(--danger);">${formatCurrency(pr.amount)}</strong></td>
      <td>${escapeHtml(pr.paymentDate)}</td>
      <td><span class="status-pill pending">Pending</span></td>
    </tr>
  `).join("");
}

/**
 * Renders Upcoming Scheduled Reminders.
 */
function renderUpcomingReminders(reminders) {
  const container = document.getElementById("remindersListContainer");
  if (!container) return;

  if (!reminders || reminders.length === 0) {
    container.innerHTML = `
      <div class="empty-placeholder">
        <div class="icon">🔔</div>
        <p>No pending reminders. All system alerts are clear.</p>
      </div>
    `;
    return;
  }

  container.innerHTML = `
    <ul class="reminder-list">
      ${reminders.map(r => `
        <li class="reminder-item">
          <div class="reminder-main">
            <span class="reminder-icon">${getReminderEmoji(r.reminderType)}</span>
            <div class="reminder-details">
              <h4>${escapeHtml(r.title)}</h4>
              <p>Target: <strong>${escapeHtml(r.relatedTo)}</strong> &bull; Category: ${escapeHtml(r.reminderType || 'General')}</p>
            </div>
          </div>
          <div class="reminder-meta">
            <div class="reminder-due">Due: ${escapeHtml(r.dueDate)}</div>
            <span class="status-pill pending">${escapeHtml(r.reminderStatus)}</span>
          </div>
        </li>
      `).join("")}
    </ul>
  `;
}

function getReminderEmoji(type) {
  switch ((type || "").toLowerCase()) {
    case 'rent due': return '💰';
    case 'maintenance': return '🔧';
    case 'agreement expiry': return '📄';
    default: return '🔔';
  }
}

function formatCurrency(amount) {
  return "₹" + Number(amount || 0).toLocaleString("en-IN", {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  });
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

function showModuleNotice(moduleName, memberName) {
  alert(`RentProof Module Notice:\nThe ${moduleName} module is assigned to ${memberName}.\nThis module integrates with the centralized 'rentproof' MySQL database.`);
}
