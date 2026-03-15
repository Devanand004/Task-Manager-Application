const API_BASE_URL = 'http://localhost:8080/api/tasks';

// DOM Elements
const tasksList = document.getElementById('tasksList');
const currentDateEl = document.getElementById('currentDate');
const pendingCountEl = document.getElementById('pendingCount');
const progressCountEl = document.getElementById('progressCount');
const completedCountEl = document.getElementById('completedCount');
const totalCountEl = document.getElementById('totalCount');
const filterTabs = document.querySelectorAll('.filter-tab');

// Modal Elements
const taskModal = document.getElementById('taskModal');
const openAddModalBtn = document.getElementById('openAddModalBtn');
const closeModalBtn = document.getElementById('closeModalBtn');
const cancelBtn = document.getElementById('cancelBtn');
const taskForm = document.getElementById('taskForm');
const modalTitle = document.getElementById('modalTitle');

// Form Inputs
const inputId = document.getElementById('taskId');
const inputTitle = document.getElementById('taskTitle');
const inputDesc = document.getElementById('taskDescription');
const inputStatus = document.getElementById('taskStatus');
const inputDueDate = document.getElementById('taskDueDate');

// Toast Elements
const toast = document.getElementById('toast');
const toastMessage = document.getElementById('toastMessage');

// State
let allTasks = [];
let currentFilter = 'ALL';

// Initialize App
document.addEventListener('DOMContentLoaded', () => {
    // Set Current Date
    const options = { weekday: 'long', month: 'short', day: 'numeric' };
    currentDateEl.textContent = new Date().toLocaleDateString('en-US', options);

    // Fetch Initial Data
    fetchTasks();

    // Event Listeners
    openAddModalBtn.addEventListener('click', openAddModal);
    closeModalBtn.addEventListener('click', closeModal);
    cancelBtn.addEventListener('click', closeModal);
    taskForm.addEventListener('submit', handleFormSubmit);

    // Close modal on outside click
    window.addEventListener('click', (e) => {
        if (e.target === taskModal) closeModal();
    });

    // Filter Tabs
    filterTabs.forEach(tab => {
        tab.addEventListener('click', () => {
            // Update active styling
            filterTabs.forEach(t => t.classList.remove('active'));
            tab.classList.add('active');
            
            // Apply filter
            currentFilter = tab.getAttribute('data-filter');
            renderTasks();
        });
    });
});

// API Calls
async function fetchTasks() {
    try {
        const response = await fetch(API_BASE_URL);
        if (!response.ok) throw new Error('Failed to fetch tasks');
        
        allTasks = await response.json();
        updateStats();
        renderTasks();
    } catch (error) {
        showToast('Error loading tasks from server', 'error');
        console.error(error);
        tasksList.innerHTML = `<div class="empty-state">
            <i class="fa-solid fa-triangle-exclamation" style="color:#EF4444"></i>
            <h3>Connection Error</h3>
            <p>Could not connect to the backend API.</p>
        </div>`;
    }
}

async function saveTask(taskData, id = null) {
    try {
        const method = id ? 'PUT' : 'POST';
        const url = id ? `${API_BASE_URL}/${id}` : API_BASE_URL;
        
        const response = await fetch(url, {
            method: method,
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(taskData)
        });

        if (!response.ok) {
            const errData = await response.json();
            throw new Error(errData.message || 'Validation failed');
        }

        const savedTask = await response.json();
        
        if (id) {
            // Update existing
            const idx = allTasks.findIndex(t => t.id === id);
            if(idx !== -1) allTasks[idx] = savedTask;
            showToast('Task updated successfully');
        } else {
            // Add new
            allTasks.push(savedTask);
            showToast('New task created');
        }

        updateStats();
        renderTasks();
        closeModal();
    } catch (error) {
        showToast(error.message, 'error');
        console.error(error);
    }
}

async function deleteTask(id) {
    if (!confirm('Are you sure you want to delete this task?')) return;

    try {
        const response = await fetch(`${API_BASE_URL}/${id}`, {
            method: 'DELETE'
        });

        if (!response.ok) throw new Error('Failed to delete task');

        allTasks = allTasks.filter(t => t.id !== id);
        updateStats();
        renderTasks();
        showToast('Task deleted successfully');
    } catch (error) {
        showToast('Error deleting task', 'error');
        console.error(error);
    }
}

// UI Rendering
function renderTasks() {
    tasksList.innerHTML = '';
    
    // Filter tasks
    const filteredTasks = currentFilter === 'ALL' 
        ? allTasks 
        : allTasks.filter(t => t.status === currentFilter);

    if (filteredTasks.length === 0) {
        tasksList.innerHTML = `
            <div class="empty-state">
                <i class="fa-solid fa-clipboard-check"></i>
                <h3>No tasks found</h3>
                <p>You're all caught up! Enjoy your day.</p>
            </div>
        `;
        return;
    }

    // Sort by updated time, newest first
    filteredTasks.sort((a, b) => new Date(b.updatedAt || b.createdAt) - new Date(a.updatedAt || a.createdAt));

    filteredTasks.forEach(task => {
        const dateObj = task.dueDate ? new Date(task.dueDate) : null;
        const dateStr = dateObj ? dateObj.toLocaleDateString('en-US', { month: 'short', day: 'numeric' }) : 'No due date';
        
        // Format Status
        let statusDisplay = 'Pending';
        if(task.status === 'IN_PROGRESS') statusDisplay = 'In Progress';
        if(task.status === 'COMPLETED') statusDisplay = 'Completed';

        const taskEl = document.createElement('div');
        taskEl.className = `task-item status-${task.status}`;
        taskEl.innerHTML = `
            <div class="task-content">
                <h3 class="task-title">${escapeHTML(task.title)}</h3>
                <p class="task-desc">${task.description ? escapeHTML(task.description) : '<i>No description provided</i>'}</p>
                <div class="task-meta">
                    <span class="meta-item status-badge badge-${task.status}">${statusDisplay}</span>
                    <span class="meta-item"><i class="fa-regular fa-calendar"></i> Due: ${dateStr}</span>
                </div>
            </div>
            <div class="task-actions">
                <button class="action-btn edit" onclick="openEditModal(${task.id})" title="Edit Task">
                    <i class="fa-solid fa-pen"></i>
                </button>
                <button class="action-btn delete" onclick="deleteTask(${task.id})" title="Delete Task">
                    <i class="fa-solid fa-trash"></i>
                </button>
            </div>
        `;
        tasksList.appendChild(taskEl);
    });
}

function updateStats() {
    const pending = allTasks.filter(t => t.status === 'PENDING').length;
    const progress = allTasks.filter(t => t.status === 'IN_PROGRESS').length;
    const completed = allTasks.filter(t => t.status === 'COMPLETED').length;

    pendingCountEl.textContent = pending;
    progressCountEl.textContent = progress;
    completedCountEl.textContent = completed;
    totalCountEl.textContent = allTasks.length;
}

// Modal Handlers
function openAddModal() {
    taskForm.reset();
    inputId.value = '';
    inputStatus.value = 'PENDING';
    modalTitle.textContent = 'Create New Task';
    taskModal.classList.add('active');
    setTimeout(() => inputTitle.focus(), 100);
}

function openEditModal(id) {
    const task = allTasks.find(t => t.id === id);
    if (!task) return;

    inputId.value = task.id;
    inputTitle.value = task.title;
    inputDesc.value = task.description || '';
    inputStatus.value = task.status;
    
    if (task.dueDate) {
        // Format to YYYY-MM-DD for date input
        const date = new Date(task.dueDate);
        inputDueDate.value = date.toISOString().split('T')[0];
    } else {
        inputDueDate.value = '';
    }

    modalTitle.textContent = 'Edit Task';
    taskModal.classList.add('active');
}

function closeModal() {
    taskModal.classList.remove('active');
}

function handleFormSubmit(e) {
    e.preventDefault();
    
    // Build Payload
    const taskData = {
        title: inputTitle.value.trim(),
        description: inputDesc.value.trim(),
        status: inputStatus.value,
        dueDate: inputDueDate.value ? new Date(inputDueDate.value).toISOString() : null
    };

    const id = inputId.value ? parseInt(inputId.value) : null;
    saveTask(taskData, id);
}

// Utilities
function showToast(msg, type = 'success') {
    toastMessage.textContent = msg;
    toast.className = 'toast show';
    
    if (type === 'error') {
        toast.style.borderLeftColor = '#EF4444';
        toast.querySelector('.toast-icon').className = 'fa-solid fa-triangle-exclamation toast-icon';
        toast.querySelector('.toast-icon').style.color = '#EF4444';
    } else {
        toast.style.borderLeftColor = 'var(--secondary)';
        toast.querySelector('.toast-icon').className = 'fa-solid fa-check-circle toast-icon';
        toast.querySelector('.toast-icon').style.color = 'var(--secondary)';
    }

    setTimeout(() => {
        toast.classList.remove('show');
    }, 3000);
}

function escapeHTML(str) {
    const div = document.createElement('div');
    div.textContent = str;
    return div.innerHTML;
}
