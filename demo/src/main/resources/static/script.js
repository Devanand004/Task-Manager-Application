document.addEventListener("DOMContentLoaded", function () {
  fetchTasks();
  document.getElementById("taskForm").addEventListener("submit", function (e) {
    e.preventDefault();
    addTask();
  });
});

function fetchTasks() {
  fetch("/api/tasks")
    .then((res) => res.json())
    .then((tasks) => {
      const list = document.getElementById("taskList");
      list.innerHTML = "";
      tasks.forEach((task) => {
        const li = document.createElement("li");
        li.innerHTML =
          `<strong>${task.title}</strong> - ${task.description || ""} (Due: ${
            task.dueDate || "N/A"
          }) ` + `<button onclick="deleteTask(${task.id})">Delete</button>`;
        list.appendChild(li);
      });
    });
}

function addTask() {
  const form = document.getElementById("taskForm");
  const task = {
    title: form.title.value,
    description: form.description.value,
    dueDate: form.dueDate.value,
  };
  fetch("/api/tasks", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(task),
  }).then(() => {
    form.reset();
    fetchTasks();
  });
}

function deleteTask(id) {
  fetch(`/api/tasks/${id}`, {
    method: "DELETE",
  }).then(fetchTasks);
}
