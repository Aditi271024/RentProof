/**
 * RENTPROOF - RENT REMINDER MANAGEMENT JAVASCRIPT
 * Module: Rent Reminder Management Module
 * Developer: Gungun (BTech CSE Project)
 * 
 * Features:
 * - Pure Vanilla JavaScript (No React, Vue, or Angular)
 * - Reactive reminder catalog management with localStorage persistence
 * - Synchronized with MySQL 'reminders' and 'tenants' schema
 * - Real-time dynamic search by title, tenant name, message, and type
 * - Filter tabs: All, Pending, Upcoming, Overdue, Completed, Maintenance
 * - Live statistical calculations (Total, Pending, Completed, Upcoming, Overdue)
 * - Due-date timeline intelligence (Countdown tags, Overdue detection)
 * - Complete CRUD workflows: Add, View, Edit, Update Status, and Delete
 * - Send Reminder dispatch simulator (simulates SMS / WhatsApp notification to tenant)
 * - Recent activity audit log tracking
 * - Clean responsive sidebar navigation and toast feedback
 */

// =============================================================================
// SEED DATA (Strictly aligned with database/rentproof.sql)
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

// Helper to calculate dynamic dates relative to today
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
    dueDate: getDateOffset(4), // Upcoming in 4 days
    reminderType: "Rent Due",
    reminderStatus: "Pending",
    createdAt: "2026-11-01 10:00:00"
  },
  {
    reminderId: 2,
    landlordId: 1,
    tenantId: null, // Property-wide
    tenantName: null,
    title: "Bi-annual Fire Extinguisher Inspection",
    message: "Inspect all fire safety cylinders, smoke sensors, and emergency exits across Sunrise PG & Ivory Heights.",
    dueDate: getDateOffset(12), // Upcoming in 12 days
    reminderType: "Maintenance",
    reminderStatus: "Pending",
    createdAt: "2026-11-02 11:30:00"
  },
  {
    reminderId: 3,
    landlordId: 1,
    tenantId: 2,
    tenantName: "Ankit Mishra",
    title: "AC Servicing Fee Pending",
    message: "Reminder: Shared quarterly air conditioning servicing fee of ₹650 for Room 102 is pending.",
    dueDate: getDateOffset(-6), // Overdue by 6 days
    reminderType: "Maintenance",
    reminderStatus: "Pending",
    createdAt: "2026-11-03 14:15:00"
  },
  {
    reminderId: 4,
    landlordId: 1,
    tenantId: 3,
    tenantName: "Sneha Kapoor",
    title: "November Rental Receipt Recorded",
    message: "Monthly rental of ₹28,000 for Ivory Heights Flat 702 received via UPI. Tax invoice issued.",
    dueDate: getDateOffset(-15),
    reminderType: "Rent Due",
    reminderStatus: "Completed",
    createdAt: "2026-11-02 16:45:00"
  },
  {
    reminderId: 5,
    landlordId: 1,
    tenantId: 1,
    tenantName: "Rohan Verma",
    title: "Sub-meter Electricity Bill Notice",
    message: "Sub-meter reading for October: 74 units @ ₹8/unit = ₹592. Kindly clear along with upcoming rental.",
    dueDate: getDateOffset(2), // Upcoming in 2 days
    reminderType: "General",
    reminderStatus: "Pending",
    createdAt: "2026-11-12 09:20:00"
  }
];

const SEED_ACTIVITIES = [
  {
    id: 1,
    icon: "🔔",
    text: "Created reminder <strong>'Upcoming December Rent Due'</strong> for <strong>Rohan Verma</strong>",
    time: "2 hours ago"
  },
  {
    id: 2,
    icon: "✅",
    text: "Marked reminder <strong>'November Rental Receipt Recorded'</strong> as <strong>Completed</strong>",
    time: "Yesterday at 4:30 PM"
  },
  {
    id: 3,
    icon: "⚠️",
    text: "Automated alert: Reminder <strong>'AC Servicing Fee Pending'</strong> is now <strong>Overdue</strong>",
    time: "3 days ago"
  }
];

// Active State
let currentTabFilter = "ALL";
let currentSearchQuery = "";
let currentTypeFilter = "ALL";
let reminderToDeleteId = null;

// =============================================================================
// INITIALIZATION ON DOM READY
// =============================================================================

document.addEventListener("DOMContentLoaded", () => {
  setupNavigationAndSidebar();
  setupActiveUser();

  // Route page actions based on active element
  if (document.getElementById("remindersTableBody")) {
    initRemindersDashboard();
  } else if (document.getElementById("addReminderForm")) {
    initAddReminderPage();
  } else if (document.getElementById("editReminderForm")) {
    initEditReminderPage();
  }
});

// =============================================================================
// LOCALSTORAGE DATA ACCESS LAYER
// =============================================================================

function getReminders() {
  const stored = localStorage.getItem("rentproof_reminders");
  if (stored) {
    try {
      return JSON.parse(stored);
    } catch (e) {
      console.error("Error parsing stored reminders:", e);
    }
  }
  saveReminders(SEED_REMINDERS);
  return [...SEED_REMINDERS];
}

function saveReminders(reminders) {
  localStorage.setItem("rentproof_reminders", JSON.stringify(reminders));
}

function getTenants() {
  const stored = localStorage.getItem("rentproof_tenants");
  if (stored) {
    try {
      return JSON.parse(stored);
    } catch (e) {
      console.error("Error parsing stored tenants:", e);
    }
  }
  localStorage.setItem("rentproof_tenants", JSON.stringify(SEED_TENANTS));
  return [...SEED_TENANTS];
}

function getActivities() {
  const stored = localStorage.getItem("rentproof_reminder_activities");
  if (stored) {
    try {
      return JSON.parse(stored);
    } catch (e) {
      console.error("Error parsing activities:", e);
    }
  }
  localStorage.setItem("rentproof_reminder_activities", JSON.stringify(SEED_ACTIVITIES));
  return [...SEED_ACTIVITIES];
}

function logActivity(icon, text) {
  const activities = getActivities();
  activities.unshift({
    id: Date.now(),
    icon: icon,
    text: text,
    time: "Just now"
  });
  if (activities.length > 8) {
    activities.pop();
  }
  localStorage.setItem("rentproof_reminder_activities", JSON.stringify(activities));
  renderActivityFeed();
}

// =============================================================================
// DASHBOARD LOGIC (reminders.html)
// =============================================================================

function initRemindersDashboard() {
  renderStats();
  renderRemindersTable();
  renderActivityFeed();
  setupDashboardEventListeners();
}

function setupDashboardEventListeners() {
  // Tab pills
  document.querySelectorAll(".tab-pill").forEach(pill => {
    pill.addEventListener("click", (e) => {
      document.querySelectorAll(".tab-pill").forEach(p => p.classList.remove("active"));
      pill.classList.add("active");
      currentTabFilter = pill.getAttribute("data-tab");
      renderRemindersTable();
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
    searchInput.addEventListener("input", (e) => {
      currentSearchQuery = e.target.value.trim().toLowerCase();
      renderRemindersTable();
    });
  }

  // Type filter dropdown
  const typeFilter = document.getElementById("reminderTypeFilter");
  if (typeFilter) {
    typeFilter.addEventListener("change", (e) => {
      currentTypeFilter = e.target.value;
      renderRemindersTable();
    });
  }
}

/**
 * Calculates and updates the 5 metric cards on the dashboard.
 */
function renderStats() {
  const reminders = getReminders();
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
        // Check if within 7 days
        const diffDays = getDaysDifference(today, r.dueDate);
        if (diffDays >= 0 && diffDays <= 7) {
          upcoming++;
        }
      }
    }
  });

  document.getElementById("statTotalReminders").textContent = total;
  document.getElementById("statPendingReminders").textContent = pending;
  document.getElementById("statCompletedReminders").textContent = completed;
  document.getElementById("statUpcomingReminders").textContent = upcoming;
  document.getElementById("statOverdueReminders").textContent = overdue;

  // Update tab badge counters
  const badgeAll = document.getElementById("badgeTabAll");
  const badgePending = document.getElementById("badgeTabPending");
  const badgeUpcoming = document.getElementById("badgeTabUpcoming");
  const badgeOverdue = document.getElementById("badgeTabOverdue");
  const badgeCompleted = document.getElementById("badgeTabCompleted");

  if (badgeAll) badgeAll.textContent = total;
  if (badgePending) badgePending.textContent = pending;
  if (badgeUpcoming) badgeUpcoming.textContent = upcoming;
  if (badgeOverdue) badgeOverdue.textContent = overdue;
  if (badgeCompleted) badgeCompleted.textContent = completed;
}

/**
 * Renders the filtered and searched reminders in the dashboard table.
 */
function renderRemindersTable() {
  const reminders = getReminders();
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

    // Search query
    if (currentSearchQuery) {
      const tenantStr = (r.tenantName || "All Tenants General").toLowerCase();
      const titleStr = (r.title || "").toLowerCase();
      const msgStr = (r.message || "").toLowerCase();
      const typeStr = (r.reminderType || "").toLowerCase();
      const statusStr = (r.reminderStatus || "").toLowerCase();

      const matches = tenantStr.includes(currentSearchQuery) ||
                      titleStr.includes(currentSearchQuery) ||
                      msgStr.includes(currentSearchQuery) ||
                      typeStr.includes(currentSearchQuery) ||
                      statusStr.includes(currentSearchQuery);
      if (!matches) return false;
    }

    return true;
  });

  // Sort by due date (earliest first)
  filtered.sort((a, b) => {
    if (a.dueDate < b.dueDate) return -1;
    if (a.dueDate > b.dueDate) return 1;
    return b.reminderId - a.reminderId;
  });

  // Update table row count indicator
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

  // Build rows HTML
  tableBody.innerHTML = filtered.map(r => {
    const isComp = (r.reminderStatus === "Completed" || r.reminderStatus === "Resolved");
    const isOverdue = !isComp && (r.dueDate < today);
    const diffDays = getDaysDifference(today, r.dueDate);

    // Due countdown string
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

    // Type badge class
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

    // Status badge
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

    // Tenant initials
    const tenantInitials = r.tenantName ? getInitials(r.tenantName) : "🏢";
    const tenantDisplay = r.tenantName ? r.tenantName : "General Property";
    const tenantSub = r.tenantName ? "Assigned Tenant" : "All Residents / Building";
    const avatarClass = r.tenantName ? "" : "general";

    // Row class
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
        <td>
          ${statusBadge}
        </td>
        <td>
          <div class="table-actions-cell">
            <!-- Mark Complete / Reopen -->
            <button 
              class="action-btn btn-complete" 
              title="${isComp ? 'Reopen (Mark as Pending)' : 'Mark as Completed'}" 
              onclick="toggleReminderStatus(${r.reminderId})"
            >
              ${isComp ? '↩️' : '✓'}
            </button>

            <!-- Send Notification Simulation -->
            <button 
              class="action-btn btn-send" 
              title="Dispatch Notice to Tenant" 
              onclick="openSendNoticeModal(${r.reminderId})"
            >
              📤
            </button>

            <!-- View Details -->
            <button 
              class="action-btn" 
              title="View Full Details" 
              onclick="openViewModal(${r.reminderId})"
            >
              👁️
            </button>

            <!-- Edit Reminder -->
            <a 
              href="edit-reminder.html?id=${r.reminderId}" 
              class="action-btn btn-edit" 
              title="Edit Reminder"
              style="text-decoration: none;"
            >
              ✏️
            </a>

            <!-- Delete Reminder -->
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
 * Toggles a reminder between Pending and Completed.
 */
function toggleReminderStatus(id) {
  const reminders = getReminders();
  const reminder = reminders.find(r => r.reminderId === id);
  if (!reminder) return;

  const wasCompleted = (reminder.reminderStatus === "Completed" || reminder.reminderStatus === "Resolved");
  reminder.reminderStatus = wasCompleted ? "Pending" : "Completed";

  saveReminders(reminders);
  renderStats();
  renderRemindersTable();

  const actionText = wasCompleted ? "reopened as Pending" : "marked as Completed";
  logActivity(
    wasCompleted ? "↩️" : "✅",
    `Reminder <strong>'${escapeHtml(reminder.title)}'</strong> ${actionText}.`
  );
  showToast(`Reminder #${id} ${actionText}!`, wasCompleted ? "info" : "success");
}

/**
 * Renders the recent activity log feed on the dashboard.
 */
function renderActivityFeed() {
  const container = document.getElementById("activityFeedContainer");
  if (!container) return;

  const activities = getActivities();
  if (activities.length === 0) {
    container.innerHTML = `<p style="font-size: 0.85rem; color: var(--slate-muted);">No recent reminder activities logged yet.</p>`;
    return;
  }

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
// MODAL HANDLERS
// =============================================================================

function openViewModal(id) {
  const reminders = getReminders();
  const r = reminders.find(item => item.reminderId === id);
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
  const reminders = getReminders();
  const r = reminders.find(item => item.reminderId === id);
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

function confirmExecuteDelete() {
  if (!reminderToDeleteId) return;

  let reminders = getReminders();
  const deletedItem = reminders.find(r => r.reminderId === reminderToDeleteId);
  reminders = reminders.filter(r => r.reminderId !== reminderToDeleteId);
  saveReminders(reminders);

  if (deletedItem) {
    logActivity("🗑️", `Deleted reminder <strong>'${escapeHtml(deletedItem.title)}'</strong>`);
  }

  closeDeleteModal();
  renderStats();
  renderRemindersTable();
  showToast(`Reminder #${reminderToDeleteId} deleted successfully.`, "danger");
}

/**
 * Dispatch Notice Simulation Modal
 */
function openSendNoticeModal(id) {
  const reminders = getReminders();
  const r = reminders.find(item => item.reminderId === id);
  if (!r) return;

  const tenants = getTenants();
  const tenant = tenants.find(t => t.tenantId === r.tenantId);

  const recipientPhone = tenant ? tenant.phone : "Property Residents Broadcast";
  const recipientEmail = tenant ? tenant.email : "tenants@rentproof.com";
  const recipientName = tenant ? tenant.fullName : (r.tenantName || "All Tenants");

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

        <div style="font-size: 0.8rem; color: var(--slate-muted);">
          Recipient Contact: <strong>+91 ${escapeHtml(recipientPhone)}</strong> &bull; <strong>${escapeHtml(recipientEmail)}</strong>
        </div>
      </div>
    `;
  }

  // Bind dispatch button
  const confirmBtn = document.getElementById("confirmSendNoticeBtn");
  if (confirmBtn) {
    confirmBtn.onclick = () => {
      // Update reminder status to Sent
      r.reminderStatus = "Sent";
      saveReminders(reminders);
      renderStats();
      renderRemindersTable();

      logActivity("📤", `Dispatched notification for <strong>'${escapeHtml(r.title)}'</strong> to <strong>${escapeHtml(recipientName)}</strong>`);
      closeSendNoticeModal();
      showToast(`Notice sent successfully to ${recipientName}!`, "info");
    };
  }

  document.getElementById("sendNoticeModal").classList.add("active");
}

function closeSendNoticeModal() {
  const modal = document.getElementById("sendNoticeModal");
  if (modal) modal.classList.remove("active");
}

// =============================================================================
// ADD REMINDER PAGE (add-reminder.html)
// =============================================================================

function initAddReminderPage() {
  populateTenantDropdown("reminderTenantSelect");
  setupTemplateChips();
  setupQuickDateButtons();
  setupAddFormValidation();
}

/**
 * Loads tenants dynamically from data store into <select>.
 * Meets Requirement 5: "Do not hardcode tenant records in final implementation. Load tenants from database."
 */
function populateTenantDropdown(selectElementId, selectedTenantId = null) {
  const selectEl = document.getElementById(selectElementId);
  if (!selectEl) return;

  const tenants = getTenants();
  selectEl.innerHTML = `
    <option value="">-- Select Recipient Tenant --</option>
    <option value="GENERAL" ${selectedTenantId === null ? 'selected' : ''}>🏢 Property-wide / All Tenants (General Notice)</option>
  `;

  tenants.forEach(t => {
    const isSelected = (selectedTenantId && selectedTenantId === t.tenantId);
    selectEl.innerHTML += `
      <option value="${t.tenantId}" ${isSelected ? 'selected' : ''}>
        👤 ${escapeHtml(t.fullName)} (${escapeHtml(t.propertyName)} - Room ${escapeHtml(t.roomNumber)})
      </option>
    `;
  });
}

/**
 * Handles template chip quick autofill.
 */
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

      // Customize message with selected tenant name if selected
      const tenantSelect = document.getElementById("reminderTenantSelect");
      let customizedMsg = msg;
      if (tenantSelect && tenantSelect.value && tenantSelect.value !== "GENERAL") {
        const tenants = getTenants();
        const t = tenants.find(item => item.tenantId == tenantSelect.value);
        if (t) {
          customizedMsg = customizedMsg.replace("{TENANT_NAME}", t.fullName);
          customizedMsg = customizedMsg.replace("{ROOM}", t.roomNumber);
        }
      } else {
        customizedMsg = customizedMsg.replace("{TENANT_NAME}", "Resident");
        customizedMsg = customizedMsg.replace("{ROOM}", "your assigned room");
      }

      if (msgInput && msg) msgInput.value = customizedMsg;
    });
  });
}

/**
 * Handles quick date shortcuts ("Tomorrow", "In 3 Days", "1st of Next Month").
 */
function setupQuickDateButtons() {
  document.querySelectorAll(".date-btn").forEach(btn => {
    btn.addEventListener("click", () => {
      const days = parseInt(btn.getAttribute("data-offset-days"), 10);
      const dateInput = document.getElementById("reminderDueDate");
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

/**
 * Validates and saves a new reminder.
 */
function setupAddFormValidation() {
  const form = document.getElementById("addReminderForm");
  if (!form) return;

  // Set default due date to 5 days ahead
  const dateInput = document.getElementById("reminderDueDate");
  if (dateInput && !dateInput.value) {
    dateInput.value = getDateOffset(5);
  }

  form.addEventListener("submit", (e) => {
    e.preventDefault();

    const tenantVal = document.getElementById("reminderTenantSelect").value;
    const titleVal = document.getElementById("reminderTitle").value.trim();
    const typeVal = document.getElementById("reminderType").value;
    const dueDateVal = document.getElementById("reminderDueDate").value;
    const statusVal = document.querySelector('input[name="reminderStatus"]:checked')?.value || "Pending";
    const messageVal = document.getElementById("reminderMessage").value.trim();

    // Reset previous errors
    clearFormErrors();
    let hasError = false;

    if (!tenantVal) {
      showFieldError("reminderTenantError", "Please select a recipient tenant or choose General Property-wide.");
      hasError = true;
    }

    if (!titleVal) {
      showFieldError("reminderTitleError", "Reminder title is required.");
      hasError = true;
    } else if (titleVal.length < 3) {
      showFieldError("reminderTitleError", "Reminder title must be at least 3 characters long.");
      hasError = true;
    } else if (titleVal.length > 150) {
      showFieldError("reminderTitleError", "Reminder title cannot exceed 150 characters.");
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

    if (!messageVal) {
      showFieldError("reminderMessageError", "Reminder message is required.");
      hasError = true;
    } else if (messageVal.length < 5) {
      showFieldError("reminderMessageError", "Reminder message must be at least 5 characters long.");
      hasError = true;
    }

    if (hasError) return;

    // Resolve tenant details
    const tenants = getTenants();
    let tenantId = null;
    let tenantName = null;

    if (tenantVal !== "GENERAL") {
      tenantId = parseInt(tenantVal, 10);
      const matched = tenants.find(t => t.tenantId === tenantId);
      if (matched) {
        tenantName = matched.fullName;
      }
    }

    const reminders = getReminders();
    const newId = reminders.length > 0 ? Math.max(...reminders.map(r => r.reminderId)) + 1 : 1;

    const newReminder = {
      reminderId: newId,
      landlordId: 1, // Default Landlord (Aditi Sharma)
      tenantId: tenantId,
      tenantName: tenantName,
      title: titleVal,
      message: messageVal,
      dueDate: dueDateVal,
      reminderType: typeVal,
      reminderStatus: statusVal,
      createdAt: new Date().toISOString().replace("T", " ").substring(0, 19)
    };

    reminders.push(newReminder);
    saveReminders(reminders);

    logActivity("➕", `Created new reminder <strong>'${escapeHtml(titleVal)}'</strong> for <strong>${escapeHtml(tenantName || 'All Residents')}</strong>`);

    // Redirect to dashboard with success query
    window.location.href = "reminders.html?created=true";
  });
}

// =============================================================================
// EDIT REMINDER PAGE (edit-reminder.html)
// =============================================================================

function initEditReminderPage() {
  const urlParams = new URLSearchParams(window.location.search);
  const reminderId = parseInt(urlParams.get("id"), 10);

  if (!reminderId) {
    alert("Invalid Reminder ID. Redirecting to Reminders Dashboard.");
    window.location.href = "reminders.html";
    return;
  }

  const reminders = getReminders();
  const reminder = reminders.find(r => r.reminderId === reminderId);

  if (!reminder) {
    alert(`Reminder #REM-${reminderId} not found. Redirecting.`);
    window.location.href = "reminders.html";
    return;
  }

  // Pre-fill form fields
  document.getElementById("editReminderIdDisplay").textContent = `#REM-${reminder.reminderId}`;
  populateTenantDropdown("editReminderTenantSelect", reminder.tenantId);

  document.getElementById("editReminderTitle").value = reminder.title;
  document.getElementById("editReminderType").value = reminder.reminderType;
  document.getElementById("editReminderDueDate").value = reminder.dueDate;
  document.getElementById("editReminderMessage").value = reminder.message;

  // Set radio status
  const statusRadio = document.querySelector(`input[name="editReminderStatus"][value="${reminder.reminderStatus}"]`);
  if (statusRadio) {
    statusRadio.checked = true;
  }

  setupQuickDateButtons();

  // Form submit handler
  const form = document.getElementById("editReminderForm");
  if (form) {
    form.addEventListener("submit", (e) => {
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

      // Update tenant
      const tenants = getTenants();
      if (tenantVal === "GENERAL") {
        reminder.tenantId = null;
        reminder.tenantName = null;
      } else {
        reminder.tenantId = parseInt(tenantVal, 10);
        const matched = tenants.find(t => t.tenantId === reminder.tenantId);
        reminder.tenantName = matched ? matched.fullName : null;
      }

      reminder.title = titleVal;
      reminder.reminderType = typeVal;
      reminder.dueDate = dueDateVal;
      reminder.reminderStatus = statusVal;
      reminder.message = messageVal;

      saveReminders(reminders);
      logActivity("✏️", `Updated reminder <strong>'${escapeHtml(titleVal)}'</strong> (#REM-${reminder.reminderId})`);

      window.location.href = "reminders.html?updated=true";
    });
  }
}

// =============================================================================
// SHARED NAVIGATION & UTILITY HELPERS
// =============================================================================

function setupNavigationAndSidebar() {
  const toggleBtn = document.getElementById("sidebarToggleBtn");
  const sidebar = document.getElementById("appSidebar");

  if (toggleBtn && sidebar) {
    // Restore sidebar preference
    const isCollapsed = localStorage.getItem("rentproof_sidebar_collapsed") === "true";
    if (isCollapsed) {
      sidebar.classList.add("collapsed");
    }

    toggleBtn.addEventListener("click", () => {
      sidebar.classList.toggle("collapsed");
      localStorage.setItem(
        "rentproof_sidebar_collapsed",
        sidebar.classList.contains("collapsed") ? "true" : "false"
      );
    });
  }

  // Check URL query parameters for feedback toast
  const urlParams = new URLSearchParams(window.location.search);
  if (urlParams.get("created") === "true") {
    showToast("New reminder created successfully!", "success");
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
  alert(`RentProof Team Module Notice:\n\n${moduleName}\nModule Lead: ${leadName}\n\nThis module is maintained independently by team members.`);
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
  return name.split(" ")
    .map(p => p.charAt(0))
    .join("")
    .toUpperCase()
    .substring(0, 2);
}

function formatDisplayDate(dateStr) {
  if (!dateStr) return "N/A";
  try {
    const d = new Date(dateStr + "T00:00:00");
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

function escapeHtml(str) {
  if (!str) return "";
  return String(str)
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#039;");
}
