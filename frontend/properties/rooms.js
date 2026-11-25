const roomPageState = {
  properties: [],
  rooms: [],
  propertyId: 0
};

document.addEventListener("DOMContentLoaded", async () => {
  if (!(await window.RentProofSecurity.requireSession())) return;

  setupRoomPageControls();
  setupRoomPageUser();
  await loadRoomProperties();
});

function setupRoomPageControls() {
  const sidebar = document.getElementById("appSidebar");
  const toggleButton = document.getElementById("sidebarToggleBtn");
  const propertySelect = document.getElementById("roomPropertySelect");
  const addForm = document.getElementById("addRoomForm");
  const editForm = document.getElementById("editRoomForm");

  if (sidebar && toggleButton) {
    const collapsed = localStorage.getItem("rentproof_sidebar_collapsed") === "true";
    if (collapsed) sidebar.classList.add("collapsed");
    toggleButton.addEventListener("click", () => {
      sidebar.classList.toggle("collapsed");
      localStorage.setItem("rentproof_sidebar_collapsed", sidebar.classList.contains("collapsed"));
    });
  }

  if (propertySelect) {
    propertySelect.addEventListener("change", async () => {
      roomPageState.propertyId = Number(propertySelect.value) || 0;
      clearRoomPageError();
      await loadRooms();
    });
  }

  if (addForm) addForm.addEventListener("submit", handleAddRoom);
  if (editForm) editForm.addEventListener("submit", handleUpdateRoom);

  document.getElementById("refreshRoomsButton")?.addEventListener("click", loadRooms);
  document.getElementById("roomsTableBody")?.addEventListener("click", handleRoomTableAction);
  document.getElementById("editRoomModal")?.addEventListener("click", (event) => {
    if (event.target.id === "editRoomModal") closeEditRoomModal();
  });
  document.querySelectorAll('[data-room-action="close-edit"]').forEach((button) => {
    button.addEventListener("click", closeEditRoomModal);
  });
}

function setupRoomPageUser() {
  let user = {};
  try {
    user = JSON.parse(localStorage.getItem("rentproof_user") || "{}");
  } catch (error) {
    console.error("Could not read the authenticated landlord profile.", error);
  }

  const name = user.fullName || "Landlord";
  const nameElement = document.getElementById("landlordDisplayName");
  const avatarElement = document.getElementById("landlordAvatarInitials");
  if (nameElement) nameElement.textContent = name;
  if (avatarElement) {
    avatarElement.textContent = name.split(/\s+/).map((part) => part[0] || "").join("").slice(0, 2).toUpperCase();
  }
}

async function loadRoomProperties() {
  const select = document.getElementById("roomPropertySelect");
  try {
    const response = await fetch("/api/properties");
    const properties = await readRoomApiResponse(response);
    if (!Array.isArray(properties)) throw new Error("The property list response was invalid.");

    roomPageState.properties = properties;
    select.replaceChildren();

    if (properties.length === 0) {
      select.add(new Option("No properties available", ""));
      select.disabled = true;
      document.getElementById("roomsEmptyState").hidden = false;
      showRoomPageError("Add a property before creating rooms.");
      return;
    }

    const requestedId = Number(new URLSearchParams(window.location.search).get("propertyId")) || 0;
    if (requestedId && !properties.some((property) => Number(property.propertyId) === requestedId)) {
      select.add(new Option("Select a property", ""));
      select.disabled = true;
      showRoomPageError("The selected property is unavailable or you do not own it.");
      return;
    }

    for (const property of properties) {
      select.add(new Option(property.propertyName, String(property.propertyId)));
    }
    roomPageState.propertyId = requestedId || Number(properties[0].propertyId);
    select.value = String(roomPageState.propertyId);
    select.disabled = false;
    await loadRooms();
  } catch (error) {
    select.disabled = true;
    showRoomPageError(error.message || "Unable to load properties.");
  }
}

async function loadRooms() {
  const body = document.getElementById("roomsTableBody");
  const emptyState = document.getElementById("roomsEmptyState");
  const property = roomPageState.properties.find(
    (item) => Number(item.propertyId) === roomPageState.propertyId
  );
  body.replaceChildren();

  if (!property) {
    emptyState.hidden = false;
    document.getElementById("roomsPropertyLabel").textContent = "Select a property to view its rooms.";
    return;
  }

  document.getElementById("roomsPropertyLabel").textContent = property.propertyName;
  emptyState.hidden = true;
  try {
    const response = await fetch(`/api/rooms?propertyId=${encodeURIComponent(roomPageState.propertyId)}`);
    const rooms = await readRoomApiResponse(response);
    if (!Array.isArray(rooms)) throw new Error("The room list response was invalid.");
    roomPageState.rooms = rooms;
    renderRoomRows(rooms);
    emptyState.hidden = rooms.length > 0;
  } catch (error) {
    roomPageState.rooms = [];
    showRoomPageError(error.message || "Unable to load rooms.");
  }
}

function renderRoomRows(rooms) {
  const body = document.getElementById("roomsTableBody");
  body.replaceChildren();

  for (const room of rooms) {
    const row = document.createElement("tr");
    appendRoomCell(row, room.roomNumber);
    appendRoomCell(row, `₹${Number(room.monthlyRent).toLocaleString("en-IN", {
      minimumFractionDigits: 2,
      maximumFractionDigits: 2
    })}`);

    const statusCell = document.createElement("td");
    const status = document.createElement("span");
    const statusKey = String(room.roomStatus || "").toLowerCase();
    status.className = `room-status-badge room-status-${statusKey}`;
    status.textContent = room.roomStatus || "Unknown";
    statusCell.appendChild(status);
    row.appendChild(statusCell);

    const actions = document.createElement("td");
    const actionGroup = document.createElement("div");
    actionGroup.className = "rooms-table-actions";
    actionGroup.appendChild(createRoomActionButton("edit", room.roomId, "Edit", "btn btn-secondary btn-sm"));
    actionGroup.appendChild(createRoomActionButton("delete", room.roomId, "Delete", "btn btn-danger btn-sm"));
    actions.appendChild(actionGroup);
    row.appendChild(actions);
    body.appendChild(row);
  }
}

function appendRoomCell(row, value) {
  const cell = document.createElement("td");
  cell.textContent = value == null ? "" : String(value);
  row.appendChild(cell);
}

function createRoomActionButton(action, roomId, label, className) {
  const button = document.createElement("button");
  button.type = "button";
  button.className = className;
  button.dataset.roomAction = action;
  button.dataset.roomId = String(roomId);
  button.textContent = label;
  return button;
}

async function handleAddRoom(event) {
  event.preventDefault();
  clearRoomPageError();
  setRoomPageStatus("");

  const roomNumberInput = document.getElementById("addRoomNumber");
  const rentInput = document.getElementById("addRoomRent");
  const roomNumber = roomNumberInput.value.trim();
  const monthlyRent = Number(rentInput.value);
  if (!roomPageState.propertyId || !roomNumber || roomNumber.length > 20 ||
      rentInput.value.trim() === "" || !Number.isFinite(monthlyRent) || monthlyRent < 0) {
    showRoomPageError("Enter a room number (up to 20 characters) and a non-negative monthly rent.");
    return;
  }

  const button = document.getElementById("addRoomButton");
  button.disabled = true;
  try {
    const response = await fetch("/api/rooms/add", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        propertyId: roomPageState.propertyId,
        roomNumber,
        monthlyRent,
        roomStatus: "Vacant"
      })
    });
    await readRoomApiResponse(response);
    event.target.reset();
    setRoomPageStatus("Room added successfully.");
    await loadRooms();
  } catch (error) {
    showRoomPageError(error.message || "Unable to add room.");
  } finally {
    button.disabled = false;
  }
}

function handleRoomTableAction(event) {
  const button = event.target.closest("button[data-room-action]");
  if (!button) return;

  const roomId = Number(button.dataset.roomId);
  const room = roomPageState.rooms.find((item) => Number(item.roomId) === roomId);
  if (!room) return;

  if (button.dataset.roomAction === "edit") openEditRoomModal(room);
  if (button.dataset.roomAction === "delete") deleteRoom(room);
}

function openEditRoomModal(room) {
  clearRoomPageError();
  document.getElementById("editRoomError").hidden = true;
  document.getElementById("editRoomId").value = String(room.roomId);
  document.getElementById("editRoomNumber").value = room.roomNumber || "";
  document.getElementById("editRoomRent").value = String(room.monthlyRent);

  const statusSelect = document.getElementById("editRoomStatus");
  const occupiedOption = statusSelect.querySelector('option[value="Occupied"]');
  occupiedOption.disabled = room.roomStatus !== "Occupied";
  statusSelect.value = room.roomStatus;
  document.getElementById("editRoomModal").classList.add("active");
  document.getElementById("editRoomModal").setAttribute("aria-hidden", "false");
}

function closeEditRoomModal() {
  const modal = document.getElementById("editRoomModal");
  modal.classList.remove("active");
  modal.setAttribute("aria-hidden", "true");
}

async function handleUpdateRoom(event) {
  event.preventDefault();
  const roomId = Number(document.getElementById("editRoomId").value);
  const roomNumber = document.getElementById("editRoomNumber").value.trim();
  const rentInput = document.getElementById("editRoomRent");
  const monthlyRent = Number(rentInput.value);
  const roomStatus = document.getElementById("editRoomStatus").value;
  if (!roomId || !roomNumber || roomNumber.length > 20 ||
      rentInput.value.trim() === "" || !Number.isFinite(monthlyRent) || monthlyRent < 0) {
    showRoomEditError("Enter a room number (up to 20 characters) and a non-negative monthly rent.");
    return;
  }

  const button = document.getElementById("saveRoomButton");
  button.disabled = true;
  try {
    const response = await fetch("/api/rooms/update", {
      method: "PUT",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ roomId, roomNumber, monthlyRent, roomStatus })
    });
    await readRoomApiResponse(response);
    closeEditRoomModal();
    setRoomPageStatus("Room updated successfully.");
    await loadRooms();
  } catch (error) {
    showRoomEditError(error.message || "Unable to update room.");
  } finally {
    button.disabled = false;
  }
}

async function deleteRoom(room) {
  if (!window.confirm(`Delete room ${room.roomNumber}? This is only allowed when no tenant or payment records are linked.`)) {
    return;
  }

  clearRoomPageError();
  try {
    const response = await fetch("/api/rooms/delete", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ roomId: room.roomId })
    });
    await readRoomApiResponse(response);
    setRoomPageStatus(`Room ${room.roomNumber} deleted.`);
    await loadRooms();
  } catch (error) {
    showRoomPageError(error.message || "Unable to delete room.");
  }
}

async function readRoomApiResponse(response) {
  let data;
  try {
    data = await response.json();
  } catch (error) {
    throw new Error(`The server returned an invalid response (HTTP ${response.status}).`);
  }

  if (!response.ok || data.success === false) {
    throw new Error(data.error || data.message || `Request failed (HTTP ${response.status}).`);
  }
  return data;
}

function showRoomPageError(message) {
  const error = document.getElementById("roomPageError");
  error.textContent = message;
  error.hidden = false;
}

function clearRoomPageError() {
  const error = document.getElementById("roomPageError");
  error.textContent = "";
  error.hidden = true;
}

function showRoomEditError(message) {
  const error = document.getElementById("editRoomError");
  error.textContent = message;
  error.hidden = false;
}

function setRoomPageStatus(message) {
  const status = document.getElementById("roomPageStatus");
  status.textContent = message;
  status.classList.toggle("rooms-message-success", Boolean(message));
}
