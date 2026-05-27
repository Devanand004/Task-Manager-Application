import { api } from './api/api.js';
import { state } from './state/state.js';
import { 
    renderTasks, 
    renderPagination, 
    renderLoadingSkeleton, 
    renderStatsDashboard, 
    renderNotificationsList, 
    renderActivityFeed 
} from './components/components.js';
import { showToast } from './utils/utils.js';

let editingTaskId = null;
let notifPollInterval = null;

document.addEventListener('DOMContentLoaded', () => {
    state.updateUserFromStorage();
    handleAuthState();

    window.addEventListener('auth-changed', () => {
        state.updateUserFromStorage();
        handleAuthState();
    });

    setupForms();
    setupFilters();
    setupNavigation();
    setupNotificationsDropdown();
});

function handleAuthState() {
    const authSection = document.getElementById("authSection");
    const appSection = document.getElementById("appSection");
    const userDisplay = document.getElementById("userDisplay");

    if (state.user.isAuthenticated) {
        if (authSection) authSection.classList.add("hidden");
        if (appSection) appSection.classList.remove("hidden");
        if (userDisplay) {
            userDisplay.textContent = `${state.user.username} (${state.user.role.replace('ROLE_', '')})`;
        }
        
        // Reset navigation state
        switchTab('tasks');
        
        // Start notifications polling every 30 seconds
        startNotifPolling();
    } else {
        if (authSection) authSection.classList.remove("hidden");
        if (appSection) appSection.classList.add("hidden");
        state.tasks = [];
        const list = document.getElementById("taskList");
        if (list) list.innerHTML = "";
        
        stopNotifPolling();
    }
}

async function loadTasks() {
    try {
        renderLoadingSkeleton();
        const res = await api.getTasks(state.filters);
        if (res.success && res.data) {
            state.tasks = res.data.content;
            state.pagination.totalPages = res.data.page.totalPages;
            state.pagination.totalElements = res.data.page.totalElements;
            
            renderTasks(editTask, deleteTask, archiveTask, restoreTask);
            renderPagination(onPageChange);
        }
    } catch (err) {
        console.error(err);
        showToast(err.message, 'error');
    }
}

async function loadStats() {
    if (!state.user.isAuthenticated) return;
    try {
        const res = await api.getStats();
        if (res.success && res.data) {
            renderStatsDashboard(res.data);
        }
    } catch (err) {
        console.error("Failed to load statistics scorecard: ", err);
    }
}

async function loadNotifications() {
    if (!state.user.isAuthenticated) return;
    try {
        const res = await api.getNotifications(0);
        if (res.success && res.data) {
            state.notifications = res.data.content;
            renderNotificationsList(state.notifications, markNotifAsRead);
        }
        
        const countRes = await api.getUnreadNotificationsCount();
        if (countRes.success) {
            state.unreadNotifications = countRes.data;
            updateNotifBadge();
        }
    } catch (err) {
        console.error("Failed to load notifications: ", err);
    }
}

function updateNotifBadge() {
    const badge = document.getElementById("notifBadge");
    if (!badge) return;
    if (state.unreadNotifications > 0) {
        badge.textContent = state.unreadNotifications;
        badge.classList.remove("hidden");
    } else {
        badge.classList.add("hidden");
    }
}

async function markNotifAsRead(id) {
    try {
        const res = await api.markNotificationRead(id);
        if (res.success) {
            loadNotifications();
        }
    } catch (err) {
        showToast(err.message, "error");
    }
}

async function loadActivityLogs() {
    if (!state.user.isAuthenticated) return;
    try {
        const res = await api.getActivity(0);
        if (res.success && res.data) {
            renderActivityFeed(res.data.content);
        }
    } catch (err) {
        console.error("Failed to load activity logs: ", err);
    }
}

function startNotifPolling() {
    loadNotifications();
    stopNotifPolling();
    notifPollInterval = setInterval(loadNotifications, 30000);
}

function stopNotifPolling() {
    if (notifPollInterval) {
        clearInterval(notifPollInterval);
        notifPollInterval = null;
    }
}

function onPageChange(newPage) {
    state.filters.page = newPage;
    loadTasks();
}

function setupNavigation() {
    const navTabs = document.querySelectorAll("#navigationTabs .nav-tab");
    navTabs.forEach(tab => {
        tab.addEventListener("click", () => {
            const tabName = tab.getAttribute("data-tab");
            switchTab(tabName);
        });
    });
}

function switchTab(tabName) {
    state.activeTab = tabName;
    
    // Update active tab visual highlight
    const navTabs = document.querySelectorAll("#navigationTabs .nav-tab");
    navTabs.forEach(t => {
        if (t.getAttribute("data-tab") === tabName) {
            t.classList.add("active");
        } else {
            t.classList.remove("active");
        }
    });

    // Hide all workspaces
    const workspaces = ['tasksWorkspace', 'archiveWorkspace', 'activityWorkspace', 'profileWorkspace'];
    workspaces.forEach(w => {
        const el = document.getElementById(w);
        if (el) el.classList.add("hidden");
    });

    // Toggle stats scorecard visibility (only show on Dashboard / tasks tab)
    const statsPanel = document.getElementById("statsPanel");
    if (statsPanel) {
        if (tabName === 'tasks') {
            statsPanel.classList.remove("hidden");
        } else {
            statsPanel.classList.add("hidden");
        }
    }

    // Show selected workspace and load its data
    if (tabName === 'tasks') {
        const el = document.getElementById("tasksWorkspace");
        if (el) el.classList.remove("hidden");
        state.filters.archived = false;
        state.filters.page = 0;
        loadTasks();
        loadStats();
    } else if (tabName === 'archive') {
        const el = document.getElementById("archiveWorkspace");
        if (el) el.classList.remove("hidden");
        state.filters.archived = true;
        state.filters.page = 0;
        loadTasks();
    } else if (tabName === 'activity') {
        const el = document.getElementById("activityWorkspace");
        if (el) el.classList.remove("hidden");
        loadActivityLogs();
    } else if (tabName === 'profile') {
        const el = document.getElementById("profileWorkspace");
        if (el) el.classList.remove("hidden");
        loadProfileData();
    }
}

async function loadProfileData() {
    try {
        const res = await api.getProfile();
        if (res.success && res.data) {
            const profileForm = document.getElementById("profileForm");
            if (profileForm) {
                profileForm.username.value = res.data.username;
            }
        }
    } catch (err) {
        showToast(err.message, "error");
    }
}

function setupNotificationsDropdown() {
    const bell = document.getElementById("notifBell");
    const dropdown = document.getElementById("notifDropdown");
    const markAllBtn = document.getElementById("markAllReadBtn");

    if (bell && dropdown) {
        bell.addEventListener("click", (e) => {
            e.stopPropagation();
            dropdown.classList.toggle("hidden");
            if (!dropdown.classList.contains("hidden")) {
                loadNotifications();
            }
        });

        // Close dropdown when clicking outside
        document.addEventListener("click", () => {
            dropdown.classList.add("hidden");
        });

        dropdown.addEventListener("click", (e) => {
            e.stopPropagation();
        });
    }

    if (markAllBtn) {
        markAllBtn.addEventListener("click", async () => {
            try {
                const res = await api.markAllNotificationsRead();
                if (res.success) {
                    loadNotifications();
                    showToast("All notifications marked as read", "success");
                }
            } catch (err) {
                showToast(err.message, "error");
            }
        });
    }
}

function setupForms() {
    const loginForm = document.getElementById("loginForm");
    if (loginForm) {
        loginForm.addEventListener("submit", async (e) => {
            e.preventDefault();
            const submitBtn = loginForm.querySelector("button[type='submit']");
            const originalContent = submitBtn.innerHTML;
            
            submitBtn.disabled = true;
            submitBtn.innerHTML = '<span class="btn-text">Signing in...</span> <i class="fa-solid fa-spinner fa-spin"></i>';
            
            const username = loginForm.username.value.trim();
            const password = loginForm.password.value;
            try {
                await api.login(username, password);
                loginForm.reset();
                showToast("Logged in successfully!", "success");
            } catch (err) {
                showToast(err.message, "error");
            } finally {
                submitBtn.disabled = false;
                submitBtn.innerHTML = originalContent;
            }
        });
    }

    const registerForm = document.getElementById("registerForm");
    if (registerForm) {
        registerForm.addEventListener("submit", async (e) => {
            e.preventDefault();
            const submitBtn = registerForm.querySelector("button[type='submit']");
            const originalContent = submitBtn.innerHTML;
            
            submitBtn.disabled = true;
            submitBtn.innerHTML = '<span class="btn-text">Registering...</span> <i class="fa-solid fa-spinner fa-spin"></i>';
            
            const username = registerForm.regUsername.value.trim();
            const password = registerForm.regPassword.value;
            const role = registerForm.regRole.value;
            try {
                await api.register(username, password, role);
                registerForm.reset();
                showToast("Registered successfully!", "success");
            } catch (err) {
                showToast(err.message, "error");
            } finally {
                submitBtn.disabled = false;
                submitBtn.innerHTML = originalContent;
            }
        });
    }

    const logoutBtn = document.getElementById("logoutBtn");
    if (logoutBtn) {
        logoutBtn.addEventListener("click", () => {
            api.logout();
            showToast("Logged out successfully!", "success");
        });
    }

    const toRegisterLink = document.getElementById("toRegister");
    const toLoginLink = document.getElementById("toLogin");
    const loginBox = document.getElementById("loginBox");
    const registerBox = document.getElementById("registerBox");

    if (toRegisterLink && toLoginLink && loginBox && registerBox) {
        toRegisterLink.addEventListener("click", (e) => {
            e.preventDefault();
            loginBox.classList.add("hidden");
            registerBox.classList.remove("hidden");
        });
        toLoginLink.addEventListener("click", (e) => {
            e.preventDefault();
            registerBox.classList.add("hidden");
            loginBox.classList.remove("hidden");
        });
    }

    const taskForm = document.getElementById("taskForm");
    if (taskForm) {
        taskForm.addEventListener("submit", async (e) => {
            e.preventDefault();
            const submitBtn = document.getElementById("submitBtn");
            const originalContent = submitBtn.innerHTML;
            
            submitBtn.disabled = true;
            submitBtn.innerHTML = `<span class="btn-text">Saving...</span> <i class="fa-solid fa-spinner fa-spin"></i>`;
            
            const taskData = {
                title: taskForm.title.value.trim(),
                description: taskForm.description.value.trim(),
                status: taskForm.status.value,
                priority: taskForm.priority.value,
                category: taskForm.category.value.trim() || null,
                dueDate: taskForm.dueDate.value || null
            };

            try {
                if (editingTaskId) {
                    await api.updateTask(editingTaskId, taskData);
                    showToast("Task updated successfully!", "success");
                } else {
                    await api.createTask(taskData);
                    showToast("Task created successfully!", "success");
                }
                resetTaskForm();
                loadTasks();
                loadStats();
            } catch (err) {
                showToast(err.message, "error");
            } finally {
                submitBtn.disabled = false;
                submitBtn.innerHTML = originalContent;
            }
        });
    }

    const profileForm = document.getElementById("profileForm");
    if (profileForm) {
        profileForm.addEventListener("submit", async (e) => {
            e.preventDefault();
            const saveProfileBtn = document.getElementById("saveProfileBtn");
            const original = saveProfileBtn.innerHTML;
            
            saveProfileBtn.disabled = true;
            saveProfileBtn.innerHTML = '<span class="btn-text">Saving Profile...</span> <i class="fa-solid fa-spinner fa-spin"></i>';
            
            const username = profileForm.username.value.trim();
            try {
                const res = await api.updateProfile({ username });
                if (res.success) {
                    // Update user storage
                    localStorage.setItem('username', username);
                    state.updateUserFromStorage();
                    
                    const userDisplay = document.getElementById("userDisplay");
                    if (userDisplay) {
                        userDisplay.textContent = `${state.user.username} (${state.user.role.replace('ROLE_', '')})`;
                    }
                    showToast(res.message || "Profile updated successfully!", "success");
                }
            } catch (err) {
                showToast(err.message, "error");
            } finally {
                saveProfileBtn.disabled = false;
                saveProfileBtn.innerHTML = original;
            }
        });
    }

    const passwordForm = document.getElementById("passwordForm");
    if (passwordForm) {
        passwordForm.addEventListener("submit", async (e) => {
            e.preventDefault();
            const changePasswordBtn = document.getElementById("changePasswordBtn");
            const original = changePasswordBtn.innerHTML;
            
            changePasswordBtn.disabled = true;
            changePasswordBtn.innerHTML = '<span class="btn-text">Updating Password...</span> <i class="fa-solid fa-spinner fa-spin"></i>';
            
            const currentPassword = passwordForm.currentPassword.value;
            const newPassword = passwordForm.newPassword.value;
            
            if (newPassword.length < 6) {
                showToast("Password must be at least 6 characters long", "error");
                changePasswordBtn.disabled = false;
                changePasswordBtn.innerHTML = original;
                return;
            }

            try {
                const res = await api.changePassword({ currentPassword, newPassword });
                if (res.success) {
                    passwordForm.reset();
                    showToast("Password updated successfully!", "success");
                }
            } catch (err) {
                showToast(err.message, "error");
            } finally {
                changePasswordBtn.disabled = false;
                changePasswordBtn.innerHTML = original;
            }
        });
    }
}

function resetTaskForm() {
    const form = document.getElementById("taskForm");
    if (form) form.reset();
    editingTaskId = null;
    const titleEl = document.getElementById("formTitle");
    const submitBtn = document.getElementById("submitBtn");
    if (titleEl) titleEl.textContent = "Create New Task";
    if (submitBtn) {
        submitBtn.innerHTML = '<span class="btn-text">Add Task</span><span class="btn-icon">⚡</span>';
    }
    
    // Set default select dropdown
    const prioritySelect = document.getElementById("priority");
    if (prioritySelect) prioritySelect.value = "MEDIUM";
}

function setupFilters() {
    const priorityFilter = document.getElementById("priorityFilter");
    if (priorityFilter) {
        priorityFilter.addEventListener("change", (e) => {
            state.filters.priority = e.target.value;
            state.filters.page = 0;
            loadTasks();
        });
    }

    const categoryFilter = document.getElementById("categoryFilter");
    if (categoryFilter) {
        let debounceTimeout;
        categoryFilter.addEventListener("input", (e) => {
            clearTimeout(debounceTimeout);
            debounceTimeout = setTimeout(() => {
                state.filters.category = e.target.value.trim();
                state.filters.page = 0;
                loadTasks();
            }, 300);
        });
    }

    const startDateFilter = document.getElementById("startDateFilter");
    if (startDateFilter) {
        startDateFilter.addEventListener("change", (e) => {
            state.filters.startDate = e.target.value;
            state.filters.page = 0;
            loadTasks();
        });
    }

    const endDateFilter = document.getElementById("endDateFilter");
    if (endDateFilter) {
        endDateFilter.addEventListener("change", (e) => {
            state.filters.endDate = e.target.value;
            state.filters.page = 0;
            loadTasks();
        });
    }

    const resetFiltersBtn = document.getElementById("resetFiltersBtn");
    if (resetFiltersBtn) {
        resetFiltersBtn.addEventListener("click", () => {
            if (priorityFilter) priorityFilter.value = "ALL";
            if (categoryFilter) categoryFilter.value = "";
            if (startDateFilter) startDateFilter.value = "";
            if (endDateFilter) endDateFilter.value = "";
            
            state.filters.priority = "ALL";
            state.filters.category = "";
            state.filters.startDate = "";
            state.filters.endDate = "";
            state.filters.page = 0;
            loadTasks();
        });
    }

    const searchInput = document.getElementById("searchInput");
    if (searchInput) {
        let debounceTimeout;
        searchInput.addEventListener("input", (e) => {
            clearTimeout(debounceTimeout);
            debounceTimeout = setTimeout(() => {
                state.filters.search = e.target.value.trim();
                state.filters.page = 0;
                loadTasks();
            }, 300);
        });
    }

    // Status Tabs Filter Setup
    const filtersContainer = document.getElementById("statusFilters");
    if (filtersContainer) {
        const tabs = filtersContainer.querySelectorAll(".filter-tab");
        tabs.forEach(tab => {
            tab.addEventListener("click", () => {
                tabs.forEach(t => t.classList.remove("active"));
                tab.classList.add("active");
                state.filters.status = tab.getAttribute("data-filter");
                state.filters.page = 0;
                loadTasks();
            });
        });
    }
}

async function editTask(id) {
    try {
        const res = await api.getTaskById(id);
        if (res.success && res.data) {
            const task = res.data;
            const taskForm = document.getElementById("taskForm");
            if (taskForm) {
                taskForm.title.value = task.title;
                taskForm.description.value = task.description || "";
                taskForm.dueDate.value = task.dueDate ? task.dueDate.split('T')[0] : "";
                taskForm.status.value = task.status || "PENDING";
                taskForm.priority.value = task.priority || "MEDIUM";
                taskForm.category.value = task.category || "";
                
                const titleEl = document.getElementById("formTitle");
                const submitBtn = document.getElementById("submitBtn");
                if (titleEl) titleEl.textContent = "Edit Task";
                if (submitBtn) {
                    submitBtn.innerHTML = '<span class="btn-text">Update Task</span><span class="btn-icon">⚡</span>';
                }
                editingTaskId = id;
                taskForm.scrollIntoView({ behavior: 'smooth' });
            }
        }
    } catch (err) {
        showToast(err.message, "error");
    }
}

async function archiveTask(id) {
    try {
        const res = await api.archiveTask(id);
        if (res.success) {
            showToast("Task archived successfully!", "success");
            loadTasks();
            loadStats();
        }
    } catch (err) {
        showToast(err.message, "error");
    }
}

async function restoreTask(id) {
    try {
        const res = await api.restoreTask(id);
        if (res.success) {
            showToast("Task restored successfully!", "success");
            switchTab('tasks');
        }
    } catch (err) {
        showToast(err.message, "error");
    }
}

async function deleteTask(id) {
    if (confirm("Are you sure you want to delete this task? This action is permanent!")) {
        try {
            await api.deleteTask(id);
            showToast("Task deleted successfully!", "success");
            loadTasks();
            loadStats();
        } catch (err) {
            showToast(err.message, "error");
        }
    }
}
