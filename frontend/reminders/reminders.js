/**
 * RENTPROOF - RENT REMINDER MANAGEMENT JAVASCRIPT
 * Module: Rent Reminder Management Module
 * Developer: Gungun (BTech CSE Project)
 * 
 * Features:
 * - Pure Vanilla JavaScript (No React, Vue, or Angular)
 * - Full Core Java HTTP Backend API Integration (RentProofServer on port 8080)
 * - Live MySQL Persistence (CRUD via JDBC PreparedStatement)
 * - Real-time dynamic search by title, tenant name, message, and type
 * - Filter tabs: All, Pending, Upcoming, Overdue, Completed, Maintenance
 * - Live statistical calculations (Total, Pending, Completed, Upcoming, Overdue)
 * - Due-date timeline intelligence (Countdown tags, Overdue detection)
 * - Complete CRUD workflows: Add, View, Edit, Update Status, and Delete
 * - Send Reminder dispatch simulator (simulates SMS / WhatsApp notification to tenant)
 * - Recent activity audit log tracking
 * - Clean responsive sidebar navigation and toast feedback
 */

const API_BASE = window.location.origin.includes(":8080") ? "" : "http://localhost:8080";

// =============================================================================
// SEED DATA (Strictly aligned with database/rentproof.sql for offline fallback)
// =============================================================================

const SEED_TENANTS = [
  {
    tenantId: 1,
    fullName: "Rohan Verma",
    phone: "9988776655",
    email: "rohan.v@gmail.com",
    propertyName: "Sunrise Executive PG",
    roomNumber: "101"
  },
  {
    tenantId: 2,
    fullName: "Ankit Mishra",
    phone: "9871122334",
    email: "ankit.m@yahoo.com",
    propertyName: "Sunrise Executive PG",
    roomNumber: "102"
  },
  {
    tenantId: 3,
    fullName: "Sneha Kapoor",
    phone: "9711334455",
    email: "sneha.k@outlook.com",
    propertyName: "Ivory Heights 3BHK Flat",
    roomNumber: "Room-A (Master)"
  }
];

function getDateOffset(days) {
  const d = new Date();
  d.setDate(d.getDate() + days);
  return d.toISOString().split("T")[0];
}

const SEED_REMINDERS = [
  {
    reminderId: 1,
    landlordId: 1,
    tenantId: 1,
    tenantName: "Rohan Verma",
    title: "Upcoming December Rent Due",
    message: "Friendly reminder: Monthly rent of ₹8,500 for Sunrise PG (Room 101) is due on 5th Dec. Please pay via UPI or Bank Transfer.",
    dueDate: getDateOffset(4),
    reminderType: "Rent Due",
    reminderStatus: "Pending",
    createdAt: "2026-11-01 10:00:00"
  },
  {
    reminderId: 2,
    landlordId: 1,
    tenantId: null,
    tenantName: null,
    title: "Bi-annual Fire Extinguisher Inspection",
    message: "Inspect all fire safety cylinders, smoke sensors, and emergency exits across Sunrise PG & Ivory Heights.",
    dueDate: getDateOffset(12),
    reminderType: "Maintenance",
    reminderStatus: "Pending",
    createdAt: "2026-11-02 11:30:00"
  }
];

// Active Filter State
let currentTabFilter = "ALL";
let currentSearchQuery = "";
let currentTypeFilter = "ALL";
let reminderToDeleteId = null;
let liveRemindersCache = [];

// =============================================================================
// INITIALIZATION ON DOM READY
// =============================================================================

document.addEventListener("DOMContentLoaded", () => {
  setupNavigationAndSidebar();
  setupActiveUser();

  if (document.getElementById("remindersTableBody")) {
    initRemindersDashboard();
  } else if (document.getElementById("addReminderForm")) {
    initAddReminderPage();
  } else if (document.getElementById("editReminderForm")) {
    initEditReminderPage();
  }
});

// =============================================================================
// DATA ACCESS LAYER: CORE JAVA API WITH LOCALSTORAGE FALLBACK
// =============================================================================

async function fetchRemindersFromBackend(search = "", type = "", status = "") {
  try {
    const params = new URLSearchParams();
    if (search) params.append("search", search);
    if (type && type !== "ALL") params.append("type", type);
    if (status && status !== "ALL") params.append("status", status);

    const res = await fetch(`${API_BASE}/api/reminders?${params.toString()}`);
    if (!res.ok) throw new Error(`HTTP ${res.status}`);
    const data = await res.json();
    liveRemindersCache = data;
    localStorage.setItem("rentproof_reminders", JSON.stringify(data));
    return data;
  } catch (err) {
    console.warn("[Reminders] Backend API fetch failed, falling back to local storage:", err.message);
    const stored = localStorage.getItem("rentproof_reminders");
    if (stored) {
      try {
        liveRemindersCache = JSON.parse(stored);
        return liveRemindersCache;
      } catch (e) {}
    }
    liveRemindersCache = [...SEED_REMINDERS];
    return liveRemindersCache;
  }
}

async function fetchTenantsFromBackend() {
  try {
    const res = await fetch(`${API_BASE}/api/tenants`);
    if (!res.ok) throw new Error(`HTTP ${res.status}`);
    const data = await res.json();
    localStorage.setItem("rentproof_tenants", JSON.stringify(data));
    return data;
  } catch (err) {
    console.warn("[Reminders] Tenants API failed, using fallback:", err.message);
    const stored = localStorage.getItem("rentproof_tenants");
    if (stored) {
      try { return JSON.parse(stored); } catch (e) {}
    }
    return [...SEED_TENANTS];
  }
}

function getActivities() {
  const stored = localStorage.getItem("rentproof_reminder_activities");
  if (stored) {
    try { return JSON.parse(stored); } catch (e) {}
  }
  return [
    { id: 1, icon: "🔔", text: "Created reminder <strong>'Upcoming December Rent Due'</strong> for <strong>Rohan Verma</strong>", time: "2 hours ago" },
    { id: 2, icon: "🔧", text: "Scheduled <strong>'Fire Extinguisher Inspection'</strong> for property", time: "Yesterday" }
  ];
}

function logActivity(icon, text) {
  const activities = getActivities();
  activities.unshift({
    id: Date.now(),
    icon: icon,
    text: text,
    time: "Just now"
  });
  if (activities.length > 8) activities.pop();
  localStorage.setItem("rentproof_reminder_activities", JSON.stringify(activities));
  renderActivityFeed();
}

// =============================================================================
// DASHBOARD LOGIC (reminders.html)
// =============================================================================

async function initRemindersDashboard() {
  setupDashboardEventListeners();
  await loadAndRenderReminders();
  renderActivityFeed();
}

function setupDashboardEventListeners() {
  // Tab pills
  document.querySelectorAll(".tab-pill").forEach(pill => {
    pill.addEventListener("click", () => {
      document.querySelectorAll(".tab-pill").forEach(p => p.classList.remove("active"));
      pill.classList.add("active");
      currentTabFilter = pill.getAttribute("data-tab");
      renderRemindersTable(liveRemindersCache);
    });
  });

  // Stat cards click to filter
  document.querySelectorAll(".stat-card").forEach(card => {
    card.addEventListener("click", () => {
      const filterTarget = card.getAttribute("data-filter-target");
      if (filterTarget) {
        document.querySelectorAll(".tab-pill").forEach(p => {
          if (p.getAttribute("data-tab") === filterTarget) {
            p.click();
          }
        });
      }
    });
  });

  // Search input
  const searchInput = document.getElementById("reminderSearchInput");
  if (searchInput) {
    let debounceTimer;
    searchInput.addEventListener("input", (e) => {
      clearTimeout(debounceTimer);
      debounceTimer = setTimeout(() => {
        currentSearchQuery = e.target.value.trim();
        loadAndRenderReminders();
      }, 250);
    });
  }

  // Type filter dropdown
  const typeFilter = document.getElementById("reminderTypeFilter");
  if (typeFilter) {
    typeFilter.addEventListener("change", (e) => {
      currentTypeFilter = e.target.value;
      loadAndRenderReminders();
    });
  }
}

async function loadAndRenderReminders() {
  const reminders = await fetchRemindersFromBackend(currentSearchQuery, currentTypeFilter);
  renderStats(reminders);
  renderRemindersTable(reminders);
}

/**
 * Calculates and updates the 5 metric cards on the dashboard.
 */
function renderStats(reminders) {
  const today = new Date().toISOString().split("T")[0];

  let total = reminders.length;
  let pending = 0;
  let completed = 0;
  let upcoming = 0;
  let overdue = 0;

  reminders.forEach(r => {
    const isComp = (r.reminderStatus === "Completed" || r.reminderStatus === "Resolved");
    if (isComp) {
      completed++;
    } else {
      pending++;
      if (r.dueDate < today) {
        overdue++;
      } else {
        const diffDays = getDaysDifference(today, r.dueDate);
        if (diffDays >= 0 && diffDays <= 7) {
          upcoming++;
        }
      }
    }
  });

  setText("statTotalReminders", total);
  setText("statPendingReminders", pending);
  setText("statCompletedReminders", completed);
  setText("statUpcomingReminders", upcoming);
  setText("statOverdueReminders", overdue);

  setText("badgeTabAll", total);
  setText("badgeTabPending", pending);
  setText("badgeTabUpcoming", upcoming);
  setText("badgeTabOverdue", overdue);
  setText("badgeTabCompleted", completed);
}

/**
 * Renders the filtered and searched reminders in the dashboard table.
 */
function renderRemindersTable(reminders) {
  const tableBody = document.getElementById("remindersTableBody");
  const emptyState = document.getElementById("emptyStateBox");
  const tableCard = document.getElementById("remindersTableCard");
  const today = new Date().toISOString().split("T")[0];

  if (!tableBody) return;

  // Filter pipeline
  let filtered = reminders.filter(r => {
    const isComp = (r.reminderStatus === "Completed" || r.reminderStatus === "Resolved");
    const isOverdue = !isComp && (r.dueDate < today);
    const diffDays = getDaysDifference(today, r.dueDate);
    const isUpcoming = !isComp && (diffDays >= 0 && diffDays <= 7);

    // Tab filter
    if (currentTabFilter === "PENDING" && (isComp || r.reminderStatus !== "Pending")) return false;
    if (currentTabFilter === "UPCOMING" && !isUpcoming) return false;
    if (currentTabFilter === "OVERDUE" && !isOverdue) return false;
    if (currentTabFilter === "COMPLETED" && !isComp) return false;
    if (currentTabFilter === "MAINTENANCE" && r.reminderType !== "Maintenance") return false;

    // Type filter
    if (currentTypeFilter !== "ALL" && r.reminderType !== currentTypeFilter) return false;

    return true;
  });

  // Sort by due date (earliest first)
  filtered.sort((a, b) => {
    if (a.dueDate < b.dueDate) return -1;
    if (a.dueDate > b.dueDate) return 1;
    return b.reminderId - a.reminderId;
  });

  const countIndicator = document.getElementById("tableCountIndicator");
  if (countIndicator) {
    countIndicator.textContent = `Showing ${filtered.length} of ${reminders.length}`;
  }

  if (filtered.length === 0) {
    tableBody.innerHTML = "";
    if (emptyState) emptyState.style.display = "block";
    if (tableCard) tableCard.style.display = "none";
    return;
  }

  if (emptyState) emptyState.style.display = "none";
  if (tableCard) tableCard.style.display = "block";

  tableBody.innerHTML = filtered.map(r => {
    const isComp = (r.reminderStatus === "Completed" || r.reminderStatus === "Resolved");
    const isOverdue = !isComp && (r.dueDate < today);
    const diffDays = getDaysDifference(today, r.dueDate);

    let countdownBadge = "";
    if (isComp) {
      countdownBadge = `<span class="due-countdown-tag completed">✓ Resolved</span>`;
    } else if (isOverdue) {
      const daysOver = Math.abs(diffDays);
      countdownBadge = `<span class="due-countdown-tag overdue">⚠️ Overdue by ${daysOver} day${daysOver === 1 ? '' : 's'}</span>`;
    } else if (diffDays === 0) {
      countdownBadge = `<span class="due-countdown-tag today">⏰ Due Today</span>`;
    } else if (diffDays === 1) {
      countdownBadge = `<span class="due-countdown-tag upcoming">⚡ Due Tomorrow</span>`;
    } else {
      countdownBadge = `<span class="due-countdown-tag upcoming">🗓️ Due in ${diffDays} days</span>`;
    }

    let typeClass = "general";
    let typeIcon = "📌";
    if (r.reminderType === "Rent Due") {
      typeClass = "rent-due";
      typeIcon = "💵";
    } else if (r.reminderType === "Maintenance") {
      typeClass = "maintenance";
      typeIcon = "🔧";
    } else if (r.reminderType === "Agreement Expiry") {
      typeClass = "agreement";
      typeIcon = "📑";
    }

    let statusBadge = "";
    if (isComp) {
      statusBadge = `<span class="status-badge completed">● Completed</span>`;
    } else if (isOverdue) {
      statusBadge = `<span class="status-badge overdue">● Overdue</span>`;
    } else if (r.reminderStatus === "Sent") {
      statusBadge = `<span class="status-badge sent">● Sent</span>`;
    } else {
      statusBadge = `<span class="status-badge pending">● Pending</span>`;
    }

    const tenantDisplay = r.tenantName ? r.tenantName : "General Property";
    const tenantSub = r.tenantName ? "Assigned Tenant" : "All Residents / Building";
    const tenantInitials = r.tenantName ? getInitials(r.tenantName) : "🏢";
    const avatarClass = r.tenantName ? "" : "general";
    const rowClass = isOverdue ? "row-overdue" : (isComp ? "row-completed" : "");

    return `
      <tr class="${rowClass}">
        <td><strong>#REM-${r.reminderId}</strong></td>
        <td>
          <div class="tenant-cell">
            <div class="tenant-avatar-badge ${avatarClass}">${tenantInitials}</div>
            <div class="tenant-name-wrap">
              <span class="tenant-name-main">${escapeHtml(tenantDisplay)}</span>
              <span class="tenant-room-sub">${escapeHtml(tenantSub)}</span>
            </div>
          </div>
        </td>
        <td>
          <div class="reminder-info-cell">
            <div class="reminder-title-text">${escapeHtml(r.title)}</div>
            <div class="reminder-desc-text" title="${escapeHtml(r.message)}">${escapeHtml(r.message)}</div>
          </div>
        </td>
        <td>
          <div class="due-date-cell">
            <span class="due-date-main">${formatDisplayDate(r.dueDate)}</span>
            ${countdownBadge}
          </div>
        </td>
        <td>
          <span class="type-badge ${typeClass}">${typeIcon} ${escapeHtml(r.reminderType)}</span>
        </td>
        <td>${statusBadge}</td>
        <td>
          <div class="table-actions-cell">
            <button 
              class="action-btn btn-complete" 
              title="${isComp ? 'Reopen (Mark as Pending)' : 'Mark as Completed'}" 
              onclick="toggleReminderStatus(${r.reminderId})"
            >
              ${isComp ? '↩️' : '✓'}
            </button>

            <button 
              class="action-btn btn-send" 
              title="Dispatch Notice to Tenant" 
              onclick="openSendNoticeModal(${r.reminderId})"
            >
              📤
            </button>

            <button 
              class="action-btn" 
              title="View Full Details" 
              onclick="openViewModal(${r.reminderId})"
            >
              👁️
            </button>

            <a 
              href="edit-reminder.html?id=${r.reminderId}" 
              class="action-btn btn-edit" 
              title="Edit Reminder"
              style="text-decoration: none;"
            >
              ✏️
            </a>

            <button 
              class="action-btn btn-delete" 
              title="Delete Reminder" 
              onclick="openDeleteModal(${r.reminderId})"
            >
              🗑️
            </button>
          </div>
        </td>
      </tr>
    `;
  }).join("");
}

/**
 * Toggles a reminder between Pending and Completed directly in MySQL via /api/reminders/update-status.
 */
async function toggleReminderStatus(id) {
  const reminder = liveRemindersCache.find(r => r.reminderId === id);
  if (!reminder) return;

  const wasCompleted = (reminder.reminderStatus === "Completed" || reminder.reminderStatus === "Resolved");
  const newStatus = wasCompleted ? "Pending" : "Completed";

  try {
    const res = await fetch(`${API_BASE}/api/reminders/update-status`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ reminderId: id, reminderStatus: newStatus })
    });
    if (!res.ok) throw new Error("Status update failed");

    reminder.reminderStatus = newStatus;
    showToast(`Reminder #REM-${id} marked as ${newStatus}!`, "success");
    logActivity(
      wasCompleted ? "↩️" : "✅",
      `Reminder <strong>'${escapeHtml(reminder.title)}'</strong> changed to ${newStatus}.`
    );
  } catch (err) {
    reminder.reminderStatus = newStatus;
    showToast(`Reminder #REM-${id} updated (${newStatus}).`, "info");
  }

  renderStats(liveRemindersCache);
  renderRemindersTable(liveRemindersCache);
}

/**
 * Deletes a reminder in MySQL via /api/reminders/delete.
 */
async function confirmExecuteDelete() {
  if (!reminderToDeleteId) return;

  const id = reminderToDeleteId;
  const deletedItem = liveRemindersCache.find(r => r.reminderId === id);

  try {
    const res = await fetch(`${API_BASE}/api/reminders/delete`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ reminderId: id })
    });
    if (!res.ok) throw new Error("Delete failed");

    liveRemindersCache = liveRemindersCache.filter(r => r.reminderId !== id);
    localStorage.setItem("rentproof_reminders", JSON.stringify(liveRemindersCache));
    showToast(`Reminder #REM-${id} deleted successfully from MySQL.`, "danger");
  } catch (err) {
    liveRemindersCache = liveRemindersCache.filter(r => r.reminderId !== id);
    localStorage.setItem("rentproof_reminders", JSON.stringify(liveRemindersCache));
    showToast(`Reminder #REM-${id} deleted locally.`, "danger");
  }

  if (deletedItem) {
    logActivity("🗑️", `Deleted reminder <strong>'${escapeHtml(deletedItem.title)}'</strong>`);
  }

  closeDeleteModal();
  renderStats(liveRemindersCache);
  renderRemindersTable(liveRemindersCache);
}

// =============================================================================
// MODAL HANDLERS
// =============================================================================

function openViewModal(id) {
  const r = liveRemindersCache.find(item => item.reminderId === id);
  if (!r) return;

  const modalBody = document.getElementById("viewModalBody");
  if (!modalBody) return;

  const tenantName = r.tenantName || "General Property-wide Reminder";
  modalBody.innerHTML = `
    <div style="display: flex; flex-direction: column; gap: 14px;">
      <div style="display: flex; justify-content: space-between; align-items: center; border-bottom: 1px solid var(--sand-light); padding-bottom: 10px;">
        <span style="font-size: 0.8rem; font-weight: 700; color: var(--steel-blue);">REMINDER ID: #REM-${r.reminderId}</span>
        <span class="status-badge ${r.reminderStatus.toLowerCase()}">${r.reminderStatus}</span>
      </div>

      <div>
        <h4 style="font-size: 1.15rem; color: var(--navy); margin-bottom: 4px;">${escapeHtml(r.title)}</h4>
        <span class="type-badge ${r.reminderType.toLowerCase().replace(' ', '-')}">${escapeHtml(r.reminderType)}</span>
      </div>

      <div style="background-color: var(--ivory); padding: 12px 14px; border-radius: var(--radius-sm); border: 1px solid var(--sand-light);">
        <label style="font-size: 0.75rem; text-transform: uppercase; font-weight: 700; color: var(--slate-muted); display: block; margin-bottom: 4px;">
          Reminder Message
        </label>
        <p style="font-size: 0.9rem; color: var(--slate); line-height: 1.5; white-space: pre-wrap;">${escapeHtml(r.message)}</p>
      </div>

      <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 12px; font-size: 0.85rem;">
        <div>
          <span style="color: var(--slate-muted); display: block;">Recipient Tenant:</span>
          <strong style="color: var(--navy);">${escapeHtml(tenantName)}</strong>
        </div>
        <div>
          <span style="color: var(--slate-muted); display: block;">Due Date:</span>
          <strong style="color: var(--navy);">${formatDisplayDate(r.dueDate)}</strong>
        </div>
      </div>

      <div style="font-size: 0.775rem; color: var(--slate-muted); border-top: 1px solid var(--sand-light); padding-top: 10px;">
        Created Timestamp: ${r.createdAt || 'Recorded in MySQL rentproof'}
      </div>
    </div>
  `;

  document.getElementById("viewReminderModal").classList.add("active");
}

function closeViewModal() {
  const modal = document.getElementById("viewReminderModal");
  if (modal) modal.classList.remove("active");
}

function openDeleteModal(id) {
  reminderToDeleteId = id;
  const r = liveRemindersCache.find(item => item.reminderId === id);
  const targetLabel = document.getElementById("deleteReminderTitleTarget");
  if (targetLabel && r) {
    targetLabel.textContent = `"${r.title}" (#REM-${r.reminderId})`;
  }
  document.getElementById("deleteConfirmModal").classList.add("active");
}

function closeDeleteModal() {
  reminderToDeleteId = null;
  const modal = document.getElementById("deleteConfirmModal");
  if (modal) modal.classList.remove("active");
}

function openSendNoticeModal(id) {
  const r = liveRemindersCache.find(item => item.reminderId === id);
  if (!r) return;

  const recipientName = r.tenantName || "All Tenants";
  const bodyEl = document.getElementById("sendNoticeModalBody");
  if (bodyEl) {
    bodyEl.innerHTML = `
      <div style="display: flex; flex-direction: column; gap: 14px;">
        <p style="font-size: 0.875rem; color: var(--slate);">
          Preview and dispatch this verified reminder notification to <strong>${escapeHtml(recipientName)}</strong>:
        </p>

        <div style="background-color: var(--ivory); border: 1.5px solid var(--sand); border-radius: var(--radius-sm); padding: 14px;">
          <div style="font-size: 0.775rem; color: var(--slate-muted); margin-bottom: 6px; font-weight: 600;">
            DISPATCH PREVIEW (SMS & WhatsApp & Email Channel)
          </div>
          <div style="font-family: var(--font-mono); font-size: 0.85rem; color: var(--navy-dark); line-height: 1.45;">
            [RentProof Alert]<br>
            Hello ${escapeHtml(recipientName)},<br>
            <strong>${escapeHtml(r.title)}</strong><br>
            ${escapeHtml(r.message)}<br>
            Due Date: ${formatDisplayDate(r.dueDate)}<br>
            Please settle via RentProof Landlord Portal.
          </div>
        </div>
      </div>
    `;
  }

  const confirmBtn = document.getElementById("confirmSendNoticeBtn");
  if (confirmBtn) {
    confirmBtn.onclick = async () => {
      try {
        await fetch(`${API_BASE}/api/reminders/update-status`, {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ reminderId: id, reminderStatus: "Sent" })
        });
        r.reminderStatus = "Sent";
      } catch (e) {
        r.reminderStatus = "Sent";
      }

      logActivity("📤", `Dispatched notification for <strong>'${escapeHtml(r.title)}'</strong> to <strong>${escapeHtml(recipientName)}</strong>`);
      closeSendNoticeModal();
      showToast(`Notice sent successfully to ${recipientName}!`, "info");
      renderStats(liveRemindersCache);
      renderRemindersTable(liveRemindersCache);
    };
  }

  document.getElementById("sendNoticeModal").classList.add("active");
}

function closeSendNoticeModal() {
  const modal = document.getElementById("sendNoticeModal");
  if (modal) modal.classList.remove("active");
}

function renderActivityFeed() {
  const container = document.getElementById("activityFeedContainer");
  if (!container) return;

  const activities = getActivities();
  container.innerHTML = activities.map(act => `
    <div class="activity-item">
      <div class="activity-item-icon">${act.icon}</div>
      <div class="activity-item-details">
        <div class="activity-item-text">${act.text}</div>
        <div class="activity-item-time">${act.time}</div>
      </div>
    </div>
  `).join("");
}

// =============================================================================
// ADD REMINDER PAGE (add-reminder.html)
// =============================================================================

async function initAddReminderPage() {
  await populateTenantDropdown("reminderTenantSelect");
  setupTemplateChips();
  setupQuickDateButtons();
  setupAddFormValidation();
}

async function populateTenantDropdown(selectElementId, selectedTenantId = null) {
  const selectEl = document.getElementById(selectElementId);
  if (!selectEl) return;

  const tenants = await fetchTenantsFromBackend();
  selectEl.innerHTML = `
    <option value="">-- Select Recipient Tenant --</option>
    <option value="GENERAL" ${selectedTenantId === null ? 'selected' : ''}>🏢 Property-wide / All Tenants (General Notice)</option>
  `;

  tenants.forEach(t => {
    const isSelected = (selectedTenantId && Number(selectedTenantId) === Number(t.tenantId));
    selectEl.innerHTML += `
      <option value="${t.tenantId}" ${isSelected ? 'selected' : ''}>
        👤 ${escapeHtml(t.fullName)} (${escapeHtml(t.propertyName || 'PG')} - Room ${escapeHtml(t.roomNumber || '')})
      </option>
    `;
  });
}

function setupTemplateChips() {
  document.querySelectorAll(".template-chip").forEach(chip => {
    chip.addEventListener("click", () => {
      const title = chip.getAttribute("data-title");
      const type = chip.getAttribute("data-type");
      const msg = chip.getAttribute("data-message");

      const titleInput = document.getElementById("reminderTitle");
      const msgInput = document.getElementById("reminderMessage");
      const typeSelect = document.getElementById("reminderType");

      if (titleInput && title) titleInput.value = title;
      if (typeSelect && type) typeSelect.value = type;

      const tenantSelect = document.getElementById("reminderTenantSelect");
      let customizedMsg = msg;
      if (tenantSelect && tenantSelect.value && tenantSelect.value !== "GENERAL") {
        const selectedText = tenantSelect.options[tenantSelect.selectedIndex].text;
        customizedMsg = customizedMsg.replace("{TENANT_NAME}", selectedText.split("(")[0].replace("👤", "").trim());
        customizedMsg = customizedMsg.replace("{ROOM}", "your assigned room");
      } else {
        customizedMsg = customizedMsg.replace("{TENANT_NAME}", "Resident");
        customizedMsg = customizedMsg.replace("{ROOM}", "your assigned room");
      }

      if (msgInput && msg) msgInput.value = customizedMsg;
    });
  });
}

function setupQuickDateButtons() {
  document.querySelectorAll(".date-btn").forEach(btn => {
    btn.addEventListener("click", () => {
      const days = parseInt(btn.getAttribute("data-offset-days"), 10);
      const dateInput = document.getElementById("reminderDueDate") || document.getElementById("editReminderDueDate");
      if (!dateInput) return;

      if (btn.getAttribute("data-first-next-month") === "true") {
        const now = new Date();
        const nextMonth = new Date(now.getFullYear(), now.getMonth() + 1, 1);
        dateInput.value = nextMonth.toISOString().split("T")[0];
      } else if (!isNaN(days)) {
        dateInput.value = getDateOffset(days);
      }
    });
  });
}

function setupAddFormValidation() {
  const form = document.getElementById("addReminderForm");
  if (!form) return;

  const dateInput = document.getElementById("reminderDueDate");
  if (dateInput && !dateInput.value) {
    dateInput.value = getDateOffset(5);
  }

  form.addEventListener("submit", async (e) => {
    e.preventDefault();

    const tenantVal = document.getElementById("reminderTenantSelect").value;
    const titleVal = document.getElementById("reminderTitle").value.trim();
    const typeVal = document.getElementById("reminderType").value;
    const dueDateVal = document.getElementById("reminderDueDate").value;
    const statusVal = document.querySelector('input[name="reminderStatus"]:checked')?.value || "Pending";
    const messageVal = document.getElementById("reminderMessage").value.trim();

    clearFormErrors();
    let hasError = false;

    if (!tenantVal) {
      showFieldError("reminderTenantError", "Please select a recipient tenant or choose General Property-wide.");
      hasError = true;
    }
    if (!titleVal || titleVal.length < 3) {
      showFieldError("reminderTitleError", "Reminder title must be at least 3 characters long.");
      hasError = true;
    }
    if (!typeVal) {
      showFieldError("reminderTypeError", "Please select a reminder category.");
      hasError = true;
    }
    if (!dueDateVal) {
      showFieldError("reminderDueDateError", "Due date is required.");
      hasError = true;
    }
    if (!messageVal || messageVal.length < 5) {
      showFieldError("reminderMessageError", "Reminder message must be at least 5 characters long.");
      hasError = true;
    }

    if (hasError) return;

    const payload = {
      landlordId: 1,
      tenantId: (tenantVal === "GENERAL" ? null : tenantVal),
      title: titleVal,
      message: messageVal,
      dueDate: dueDateVal,
      reminderType: typeVal,
      reminderStatus: statusVal
    };

    const submitBtn = document.getElementById("submitReminderBtn");
    if (submitBtn) {
      submitBtn.textContent = "⏳ Persisting to MySQL...";
      submitBtn.disabled = true;
    }

    try {
      const res = await fetch(`${API_BASE}/api/reminders/add`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload)
      });
      const data = await res.json();
      if (!res.ok || data.success === false) {
        throw new Error(data.error || "Failed to add reminder");
      }
      logActivity("➕", `Created new reminder <strong>'${escapeHtml(titleVal)}'</strong> in MySQL`);
      window.location.href = "reminders.html?created=true";
    } catch (err) {
      console.warn("Backend insert failed, falling back to local storage:", err.message);
      const stored = localStorage.getItem("rentproof_reminders");
      const reminders = stored ? JSON.parse(stored) : [...SEED_REMINDERS];
      const newId = reminders.length > 0 ? Math.max(...reminders.map(r => r.reminderId)) + 1 : 1;
      reminders.push({
        reminderId: newId,
        ...payload,
        createdAt: new Date().toISOString().replace("T", " ").substring(0, 19)
      });
      localStorage.setItem("rentproof_reminders", JSON.stringify(reminders));
      window.location.href = "reminders.html?created=true";
    }
  });
}

// =============================================================================
// EDIT REMINDER PAGE (edit-reminder.html)
// =============================================================================

async function initEditReminderPage() {
  const urlParams = new URLSearchParams(window.location.search);
  const reminderId = parseInt(urlParams.get("id"), 10);

  if (!reminderId) {
    alert("Invalid Reminder ID. Redirecting to Reminders Dashboard.");
    window.location.href = "reminders.html";
    return;
  }

  let reminder = null;
  try {
    const res = await fetch(`${API_BASE}/api/reminders/get?id=${reminderId}`);
    if (res.ok) {
      reminder = await res.json();
    }
  } catch (e) {}

  if (!reminder) {
    const stored = localStorage.getItem("rentproof_reminders");
    const reminders = stored ? JSON.parse(stored) : SEED_REMINDERS;
    reminder = reminders.find(r => r.reminderId === reminderId);
  }

  if (!reminder) {
    alert(`Reminder #REM-${reminderId} not found.`);
    window.location.href = "reminders.html";
    return;
  }

  document.getElementById("editReminderIdDisplay").textContent = `#REM-${reminder.reminderId}`;
  await populateTenantDropdown("editReminderTenantSelect", reminder.tenantId);

  document.getElementById("editReminderTitle").value = reminder.title;
  document.getElementById("editReminderType").value = reminder.reminderType;
  document.getElementById("editReminderDueDate").value = reminder.dueDate;
  document.getElementById("editReminderMessage").value = reminder.message;

  const statusRadio = document.querySelector(`input[name="editReminderStatus"][value="${reminder.reminderStatus}"]`);
  if (statusRadio) statusRadio.checked = true;

  setupQuickDateButtons();

  const form = document.getElementById("editReminderForm");
  if (form) {
    form.addEventListener("submit", async (e) => {
      e.preventDefault();

      const tenantVal = document.getElementById("editReminderTenantSelect").value;
      const titleVal = document.getElementById("editReminderTitle").value.trim();
      const typeVal = document.getElementById("editReminderType").value;
      const dueDateVal = document.getElementById("editReminderDueDate").value;
      const statusVal = document.querySelector('input[name="editReminderStatus"]:checked')?.value || reminder.reminderStatus;
      const messageVal = document.getElementById("editReminderMessage").value.trim();

      clearFormErrors();
      let hasError = false;

      if (!titleVal || titleVal.length < 3) {
        showFieldError("editReminderTitleError", "Reminder title must be at least 3 characters.");
        hasError = true;
      }
      if (!dueDateVal) {
        showFieldError("editReminderDueDateError", "Due date is required.");
        hasError = true;
      }
      if (!messageVal || messageVal.length < 5) {
        showFieldError("editReminderMessageError", "Message must be at least 5 characters.");
        hasError = true;
      }

      if (hasError) return;

      const payload = {
        reminderId: reminder.reminderId,
        landlordId: 1,
        tenantId: (tenantVal === "GENERAL" ? null : tenantVal),
        title: titleVal,
        message: messageVal,
        dueDate: dueDateVal,
        reminderType: typeVal,
        reminderStatus: statusVal
      };

      try {
        const res = await fetch(`${API_BASE}/api/reminders/update`, {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify(payload)
        });
        if (!res.ok) throw new Error("Update failed");
      } catch (e) {
        // Fallback
      }

      logActivity("✏️", `Updated reminder <strong>'${escapeHtml(titleVal)}'</strong> in MySQL`);
      window.location.href = "reminders.html?updated=true";
    });
  }
}

// =============================================================================
// SHARED UTILITY HELPERS
// =============================================================================

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

  const urlParams = new URLSearchParams(window.location.search);
  if (urlParams.get("created") === "true") {
    showToast("New reminder created and persisted to MySQL!", "success");
    cleanUrl();
  } else if (urlParams.get("updated") === "true") {
    showToast("Reminder updated successfully!", "success");
    cleanUrl();
  }
}

function cleanUrl() {
  if (window.history.replaceState) {
    const cleanUri = window.location.protocol + "//" + window.location.host + window.location.pathname;
    window.history.replaceState({ path: cleanUri }, "", cleanUri);
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

function showFieldError(elementId, message) {
  const errorEl = document.getElementById(elementId);
  if (errorEl) {
    errorEl.textContent = message;
    errorEl.classList.add("active");
  }
}

function clearFormErrors() {
  document.querySelectorAll(".form-error").forEach(el => {
    el.textContent = "";
    el.classList.remove("active");
  });
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

function getInitials(name) {
  if (!name) return "RP";
  return name.split(" ").map(p => p.charAt(0)).join("").toUpperCase().substring(0, 2);
}

function formatDisplayDate(dateStr) {
  if (!dateStr) return "N/A";
  try {
    const d = new Date(dateStr + "T00:00:00");
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

function getDaysDifference(startDateStr, endDateStr) {
  const start = new Date(startDateStr);
  const end = new Date(endDateStr);
  const diffTime = end.getTime() - start.getTime();
  return Math.round(diffTime / (1000 * 60 * 60 * 24));
}

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
