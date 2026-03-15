let editingTaskId = null;

document.addEventListener("DOMContentLoaded", function () {
  fetchTasks();
  document.getElementById("taskForm").addEventListener("submit", function (e) {
    e.preventDefault();
    if (editingTaskId) {
      updateTask();
    } else {
      addTask();
    }
  });
});

function fetchTasks() {
  fetch("/api/tasks")
    .then((res) => res.json())
    .then((tasks) => {
      const list = document.getElementById("taskList");
      list.innerHTML = "";
      if (tasks.length === 0) {
        list.innerHTML = '<li class="empty-state">No tasks yet. Add one to get started!</li>';
        return;
      }
      tasks.forEach((task) => {
        const li = document.createElement("li");
        const dueDate = task.dueDate ? new Date(task.dueDate).toLocaleDateString() : "No due date";
        const statusClass = task.status ? task.status.toLowerCase() : "pending";
        
        li.innerHTML = `
          <div class="task-info">
            <div class="task-header">
              <strong>${task.title}</strong>
              <span class="status-badge ${statusClass}">${task.status || 'PENDING'}</span>
            </div>
            <p class="task-description">${task.description || 'No description'}</p>
            <small class="task-meta">Due: ${dueDate}</small>
          </div>
          <div class="task-actions">
            <button class="btn-edit" onclick="editTask(${task.id})">Edit</button>
            <button class="btn-delete" onclick="deleteTask(${task.id})">Delete</button>
          </div>
        `;
        list.appendChild(li);
      });
    })
    .catch((error) => console.error("Error fetching tasks:", error));
}

function addTask() {
  const form = document.getElementById("taskForm");
  const task = {
    title: form.title.value,
    description: form.description.value,
    dueDate: form.dueDate.value || null,
    status: form.status.value || "PENDING"
  };
  
  fetch("/api/tasks", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(task),
  })
  .then((res) => res.json())
  .then(() => {
    form.reset();
    editingTaskId = null;
    document.getElementById("formTitle").textContent = "Add New Task";
    document.getElementById("submitBtn").textContent = "Add Task";
    fetchTasks();
  })
  .catch((error) => console.error("Error adding task:", error));
}

function editTask(id) {
  fetch(`/api/tasks/${id}`)
    .then((res) => res.json())
    .then((task) => {
      document.getElementById("title").value = task.title;
      document.getElementById("description").value = task.description || "";
      document.getElementById("dueDate").value = task.dueDate ? task.dueDate.split('T')[0] : "";
      document.getElementById("status").value = task.status || "PENDING";
      document.getElementById("formTitle").textContent = "Edit Task";
      document.getElementById("submitBtn").textContent = "Update Task";
      editingTaskId = id;
      document.getElementById("title").focus();
    })
    .catch((error) => console.error("Error fetching task:", error));
}

function updateTask() {
  const form = document.getElementById("taskForm");
  const task = {
    title: form.title.value,
    description: form.description.value,
    dueDate: form.dueDate.value || null,
    status: form.status.value || "PENDING"
  };
  
  fetch(`/api/tasks/${editingTaskId}`, {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(task),
  })
  .then((res) => res.json())
  .then(() => {
    form.reset();
    editingTaskId = null;
    document.getElementById("formTitle").textContent = "Add New Task";
    document.getElementById("submitBtn").textContent = "Add Task";
    fetchTasks();
  })
  .catch((error) => console.error("Error updating task:", error));
}

function deleteTask(id) {
  if (confirm("Are you sure you want to delete this task?")) {
    fetch(`/api/tasks/${id}`, {
      method: "DELETE",
    })
    .then(() => fetchTasks())
    .catch((error) => console.error("Error deleting task:", error));
  }
}
