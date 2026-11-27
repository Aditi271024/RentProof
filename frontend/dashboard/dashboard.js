/**
 * RENTPROOF - DASHBOARD MODULE JAVASCRIPT
 * Module: Dashboard Analytics Module
 * Developer: Gungun (BTech CSE Project)
 * 
 * Features:
 * - Pure Vanilla JavaScript (No React, Vue, or Angular)
 * - Live Core Java API integration via RentProofServer on port 8080
 * - Real-time statistics directly from MySQL database:
 *   * Total properties & room inventory (occupied vs vacant with rate)
 *   * Financials (Total rent collected, successful, pending, overdue)
 *   * Reminders (Total, pending, upcoming within 7 days, overdue alerts)
 *   * Active tenants count
 * - Actionable urgent reminders widget with quick-completion workflow
 * - Unified recent activity feed
 * - One-click live data refresh
 * - Graceful offline fallback
 */

const API_BASE = window.location.origin.includes(":8080") ? "" : "http://localhost:8080";

// Fallback seed data (strictly aligned with database/rentproof.sql)
const FALLBACK_STATS = {
  totalProperties: 5,
  totalTenants: 3,
  totalRooms: 13,
  occupiedRooms: 6,
  vacantRooms: 6,
  maintenanceRooms: 1,
  occupancyRate: 46.2,
  totalPayments: 3,
  totalRentCollected: 28000.0,
  completedPayments: 3,
  pendingPayments: 0,
  overduePayments: 0,
  totalReminders: 2,
  pendingReminders: 2,
  upcomingReminders: 1,
  overdueReminders: 0,
  completedReminders: 0
};

document.addEventListener("DOMContentLoaded", () => {
  setupNavigationAndSidebar();
  setupActiveUser();
  loadAllDashboardData();

  const refreshBtn = document.getElementById("refreshDashboardBtn");
  if (refreshBtn) {
    refreshBtn.addEventListener("click", () => {
      refreshBtn.textContent = "⏳ Refreshing...";
      refreshBtn.disabled = true;
      loadAllDashboardData(() => {
        refreshBtn.textContent = "🔄 Refresh Live Data";
        refreshBtn.disabled = false;
        showToast("Dashboard metrics refreshed from MySQL!", "success");
      });
    });
  }
});

function loadAllDashboardData(callback) {
  Promise.all([
    fetchDashboardStats(),
    fetchUrgentReminders(),
    fetchRecentActivities()
  ]).finally(() => {
    if (typeof callback === "function") callback();
  });
}

/**
 * 1. Fetches live system statistics from GET /api/dashboard/stats
 */
async function fetchDashboardStats() {
  const statusPill = document.getElementById("systemStatusPill");
  try {
    const res = await fetch(`${API_BASE}/api/dashboard/stats`);
    if (!res.ok) throw new Error(`HTTP ${res.status}`);
    const stats = await res.json();

    renderStatsUI(stats);
    if (statusPill) {
      statusPill.className = "system-status-pill";
      statusPill.innerHTML = `<span class="status-dot"></span> MySQL Connected &bull; Port 8080`;
    }
  } catch (err) {
    console.warn("Backend API unavailable, using local data:", err.message);
    renderStatsUI(FALLBACK_STATS);
    if (statusPill) {
      statusPill.className = "system-status-pill offline";
      statusPill.innerHTML = `<span class="status-dot"></span> Offline Fallback Mode`;
    }
  }
}

/**
 * Renders stats onto the DOM.
 */
function renderStatsUI(stats) {
  // Properties Card
  setText("dashTotalProperties", stats.totalProperties ?? 0);
  setText("dashTotalRooms", stats.totalRooms ?? 0);
  setText("dashOccupiedRooms", stats.occupiedRooms ?? 0);
  setText("dashVacantRooms", stats.vacantRooms ?? 0);
  setText("dashOccupancyRate", `${(stats.occupancyRate ?? 0).toFixed(1)}%`);

  const fillBar = document.getElementById("occupancyProgressBar");
  if (fillBar) {
    fillBar.style.width = `${Math.min(100, Math.max(0, stats.occupancyRate ?? 0))}%`;
  }

  // Financials Card
  const formattedRent = Number(stats.totalRentCollected ?? 0).toLocaleString("en-IN", {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  });
  setText("dashTotalRentCollected", `₹${formattedRent}`);
  setText("dashPaidPayments", stats.completedPayments ?? 0);
  setText("dashPendingPayments", stats.pendingPayments ?? 0);
  setText("dashOverduePayments", stats.overduePayments ?? 0);

  // Reminders Card (Gungun's Module)
  setText("dashTotalReminders", stats.totalReminders ?? 0);
  setText("dashPendingReminders", stats.pendingReminders ?? 0);
  setText("dashUpcomingReminders", stats.upcomingReminders ?? 0);
  setText("dashOverdueReminders", stats.overdueReminders ?? 0);

  // Tenants Card
  setText("dashTotalTenants", stats.totalTenants ?? 0);
}

/**
 * 2. Fetches urgent / overdue reminders for action widget.
 */
async function fetchUrgentReminders() {
  const container = document.getElementById("urgentRemindersBody");
  if (!container) return;

  try {
    const res = await fetch(`${API_BASE}/api/dashboard/urgent-reminders`);
    if (!res.ok) throw new Error(`HTTP ${res.status}`);
    const list = await res.json();
    renderUrgentRemindersUI(list);
  } catch (err) {
    // Fallback using seed
    renderUrgentRemindersUI([
      {
        reminderId: 1,
        title: "Upcoming December Rent Due",
        dueDate: "2026-12-05",
        reminderType: "Rent Due",
        reminderStatus: "Pending",
        tenantName: "Rohan Verma",
        daysLeft: 9
      },
      {
        reminderId: 2,
        title: "Bi-annual Fire Extinguisher Inspection",
        dueDate: "2026-11-30",
        reminderType: "Maintenance",
        reminderStatus: "Pending",
        tenantName: "General Property",
        daysLeft: 4
      }
    ]);
  }
}

function renderUrgentRemindersUI(reminders) {
  const container = document.getElementById("urgentRemindersBody");
  if (!container) return;

  if (!reminders || reminders.length === 0) {
    container.innerHTML = `
      <tr>
        <td colspan="5" style="text-align: center; color: var(--slate-muted); padding: 24px;">
          🎉 All rent reminders and maintenance notices are currently resolved!
        </td>
      </tr>
    `;
    return;
  }

  container.innerHTML = reminders.map(r => {
    const isOverdue = r.daysLeft < 0;
    const rowClass = isOverdue ? "is-overdue" : "";

    let badge = "";
    if (isOverdue) {
      badge = `<span style="color: var(--danger); font-weight: 700;">⚠️ Overdue (${Math.abs(r.daysLeft)}d ago)</span>`;
    } else if (r.daysLeft === 0) {
      badge = `<span style="color: #B45309; font-weight: 700;">⏰ Due Today</span>`;
    } else {
      badge = `<span style="color: #1D4ED8; font-weight: 600;">🗓️ Due in ${r.daysLeft} days</span>`;
    }

    return `
      <tr class="${rowClass}">
        <td><strong>#REM-${r.reminderId}</strong></td>
        <td>
          <span class="urgent-tenant-text">${escapeHtml(r.tenantName || 'General Property')}</span>
        </td>
        <td>
          <span class="urgent-title-text">${escapeHtml(r.title)}</span>
        </td>
        <td>
          <div>${formatDisplayDate(r.dueDate)}</div>
          <div style="font-size: 0.72rem;">${badge}</div>
        </td>
        <td style="text-align: right;">
          <button 
            class="btn btn-outline btn-sm" 
            title="Mark as Completed in MySQL"
            onclick="quickCompleteReminder(${r.reminderId})"
          >
            ✓ Resolve
          </button>
        </td>
      </tr>
    `;
  }).join("");
}

/**
 * Quick-resolve a reminder directly from the Dashboard table.
 */
async function quickCompleteReminder(id) {
  try {
    const res = await fetch(`${API_BASE}/api/reminders/update-status`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ reminderId: id, reminderStatus: "Completed" })
    });
    if (!res.ok) throw new Error("Status update failed");
    showToast(`Reminder #REM-${id} marked as Completed!`, "success");
    loadAllDashboardData();
  } catch (e) {
    showToast(`Completed Reminder #REM-${id} locally.`, "info");
    loadAllDashboardData();
  }
}

/**
 * 3. Fetches live unified activity stream from GET /api/dashboard/recent-activity
 */
async function fetchRecentActivities() {
  const container = document.getElementById("dashboardActivityContainer");
  if (!container) return;

  try {
    const res = await fetch(`${API_BASE}/api/dashboard/recent-activity`);
    if (!res.ok) throw new Error(`HTTP ${res.status}`);
    const acts = await res.json();
    renderActivitiesUI(acts);
  } catch (err) {
    renderActivitiesUI([
      { icon: "🔔", description: "Upcoming Rental Due reminder scheduled for Rohan Verma", createdAt: "2026-11-01" },
      { icon: "🔧", description: "Fire safety inspection maintenance scheduled", createdAt: "2026-11-02" },
      { icon: "💳", description: "Payment of Rs. 8500 received from Rohan Verma (November 2026)", createdAt: "2026-11-05" }
    ]);
  }
}

function renderActivitiesUI(activities) {
  const container = document.getElementById("dashboardActivityContainer");
  if (!container) return;

  if (!activities || activities.length === 0) {
    container.innerHTML = `<p style="font-size: 0.85rem; color: var(--slate-muted); padding: 12px;">No activity logged yet.</p>`;
    return;
  }

  container.innerHTML = activities.map(act => `
    <div class="activity-item-row">
      <div class="activity-icon">${act.icon || '📌'}</div>
      <div class="activity-body">
        <div class="activity-text">${escapeHtml(act.description)}</div>
        <div class="activity-time">${formatDisplayDate(act.createdAt)}</div>
      </div>
    </div>
  `).join("");
}

// Helpers
function setText(id, text) {
  const el = document.getElementById(id);
  if (el) el.textContent = text;
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

function formatDisplayDate(dateStr) {
  if (!dateStr) return "N/A";
  try {
    const d = new Date(dateStr.replace(" ", "T"));
    if (isNaN(d.getTime())) return dateStr;
    return d.toLocaleDateString("en-US", {
      day: "numeric",
      month: "short",
      year: "numeric"
    });
  } catch (e) {
    return dateStr;
  }
}

function setupNavigationAndSidebar() {
  const toggleBtn = document.getElementById("sidebarToggleBtn");
  const sidebar = document.getElementById("appSidebar");

  if (toggleBtn && sidebar) {
    const isCollapsed = localStorage.getItem("rentproof_sidebar_collapsed") === "true";
    if (isCollapsed) sidebar.classList.add("collapsed");

    toggleBtn.addEventListener("click", () => {
      sidebar.classList.toggle("collapsed");
      localStorage.setItem("rentproof_sidebar_collapsed", sidebar.classList.contains("collapsed") ? "true" : "false");
    });
  }
}

function setupActiveUser() {
  const nameEl = document.getElementById("landlordDisplayName");
  const avatarEl = document.getElementById("landlordAvatarInitials");
  if (nameEl) nameEl.textContent = "Aditi Sharma";
  if (avatarEl) avatarEl.textContent = "AS";
}

function showModuleNotice(moduleName, leadName) {
  alert(`RentProof Team Module Notice:\n\n${moduleName}\nModule Lead: ${leadName}\n\nMaintained independently by assigned team members.`);
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
  toast.className = `toast ${type}`;
  toast.innerHTML = `<span>${escapeHtml(message)}</span>`;
  container.appendChild(toast);

  setTimeout(() => {
    toast.style.opacity = "0";
    toast.style.transition = "opacity 300ms ease";
    setTimeout(() => toast.remove(), 300);
  }, 3500);
}
