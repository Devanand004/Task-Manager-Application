import { escapeHTML, formatDate } from '../utils/utils.js';
import { state } from '../state/state.js';

export function renderTasks(onEdit, onDelete, onArchive, onRestore) {
    const list = state.filters.archived 
        ? document.getElementById("archiveTaskList") 
        : document.getElementById("taskList");
    if (!list) return;
    list.innerHTML = "";

    if (state.tasks.length === 0) {
        list.innerHTML = `
            <li class="empty-state">
                <span class="empty-icon">🍃</span>
                <p>No tasks found. Add one or change filters to get started!</p>
            </li>
        `;
        return;
    }

    state.tasks.forEach((task) => {
        const li = document.createElement("li");
        li.className = "task-item";
        const dueDate = formatDate(task.dueDate);
        const statusClass = task.status ? task.status.toLowerCase() : "pending";
        const statusDisplay = task.status ? task.status.replace('_', ' ') : 'PENDING';
        
        const priorityClass = task.priority ? task.priority.toLowerCase() : "medium";
        const priorityDisplay = task.priority ? task.priority : 'MEDIUM';

        const isAdmin = state.user.role === 'ROLE_ADMIN';
        
        const deleteButton = isAdmin 
            ? `<button class="btn-delete" data-id="${task.id}" title="Delete Task" aria-label="Delete task: ${escapeHTML(task.title)}">
                <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="3 6 5 6 21 6"></polyline><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path><line x1="10" y1="11" x2="10" y2="17"></line><line x1="14" y1="11" x2="14" y2="17"></line></svg>
                Delete
               </button>` 
            : '';

        const archiveButton = task.archived
            ? `<button class="btn-restore" data-id="${task.id}" title="Restore Task" aria-label="Restore task: ${escapeHTML(task.title)}">
                <i class="fa-solid fa-trash-restore" style="font-size:10px;"></i> Restore
               </button>`
            : `<button class="btn-archive" data-id="${task.id}" title="Archive Task" aria-label="Archive task: ${escapeHTML(task.title)}">
                <i class="fa-solid fa-box-archive" style="font-size:10px;"></i> Archive
               </button>`;

        const editButton = !task.archived
            ? `<button class="btn-edit" data-id="${task.id}" title="Edit Task" aria-label="Edit task: ${escapeHTML(task.title)}">
                <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path><path d="M18.5 2.5a2.121 2.121 0 1 1 3 3L12 15l-4 1 1-4 9.5-9.5z"></path></svg>
                Edit
               </button>`
            : '';

        li.innerHTML = `
            <div class="task-info">
                <div class="task-header">
                    <strong class="task-title">${escapeHTML(task.title)}</strong>
                    <span class="status-badge ${statusClass}">${escapeHTML(statusDisplay)}</span>
                    <span class="priority-badge ${priorityClass}">${escapeHTML(priorityDisplay)}</span>
                    ${task.category ? `<span class="category-badge">${escapeHTML(task.category)}</span>` : ''}
                </div>
                <p class="task-description">${task.description ? escapeHTML(task.description) : '<i>No description</i>'}</p>
                <div class="task-meta">
                    <span class="meta-item"><span class="meta-icon">📅</span> Due: ${dueDate}</span>
                </div>
            </div>
            <div class="task-actions">
                ${editButton}
                ${archiveButton}
                ${deleteButton}
            </div>
        `;
        list.appendChild(li);
    });

    list.querySelectorAll('.btn-edit').forEach(btn => {
        btn.addEventListener('click', () => onEdit(parseInt(btn.getAttribute('data-id'))));
    });

    list.querySelectorAll('.btn-archive').forEach(btn => {
        btn.addEventListener('click', () => onArchive(parseInt(btn.getAttribute('data-id'))));
    });

    list.querySelectorAll('.btn-restore').forEach(btn => {
        btn.addEventListener('click', () => onRestore(parseInt(btn.getAttribute('data-id'))));
    });

    list.querySelectorAll('.btn-delete').forEach(btn => {
        btn.addEventListener('click', () => onDelete(parseInt(btn.getAttribute('data-id'))));
    });
}

export function renderPagination(onPageChange) {
    const container = state.filters.archived 
        ? document.getElementById("archivePaginationContainer") 
        : document.getElementById("paginationContainer");
    if (!container) return;
    container.innerHTML = "";

    if (state.pagination.totalPages <= 1) return;

    const nav = document.createElement("nav");
    nav.className = "pagination-nav";

    const prevButton = document.createElement("button");
    prevButton.className = "btn-pagination";
    prevButton.disabled = state.filters.page === 0;
    prevButton.innerHTML = "&laquo; Prev";
    prevButton.addEventListener("click", () => onPageChange(state.filters.page - 1));
    nav.appendChild(prevButton);

    const info = document.createElement("span");
    info.className = "pagination-info";
    info.textContent = `Page ${state.filters.page + 1} of ${state.pagination.totalPages}`;
    nav.appendChild(info);

    const nextButton = document.createElement("button");
    nextButton.className = "btn-pagination";
    nextButton.disabled = state.filters.page >= state.pagination.totalPages - 1;
    nextButton.innerHTML = "Next &raquo;";
    nextButton.addEventListener("click", () => onPageChange(state.filters.page + 1));
    nav.appendChild(nextButton);

    container.appendChild(nav);
}

export function renderStats() {
    const totalEl = document.getElementById("totalTasksCount");
    if (totalEl) {
        totalEl.textContent = state.pagination.totalElements;
    }
}

export function renderLoadingSkeleton() {
    const list = state.filters.archived 
        ? document.getElementById("archiveTaskList") 
        : document.getElementById("taskList");
    if (!list) return;
    list.innerHTML = `
        <li class="task-item skeleton">
            <div class="task-info">
                <div class="task-header">
                    <span class="skeleton-line title"></span>
                    <span class="skeleton-line badge"></span>
                </div>
                <div class="skeleton-line desc"></div>
                <div class="skeleton-line meta"></div>
            </div>
        </li>
        <li class="task-item skeleton">
            <div class="task-info">
                <div class="task-header">
                    <span class="skeleton-line title"></span>
                    <span class="skeleton-line badge"></span>
                </div>
                <div class="skeleton-line desc"></div>
                <div class="skeleton-line meta"></div>
            </div>
        </li>
        <li class="task-item skeleton">
            <div class="task-info">
                <div class="task-header">
                    <span class="skeleton-line title"></span>
                    <span class="skeleton-line badge"></span>
                </div>
                <div class="skeleton-line desc"></div>
                <div class="skeleton-line meta"></div>
            </div>
        </li>
    `;
}

export function renderStatsDashboard(stats) {
    const statsGrid = document.getElementById("statsGrid");
    if (!statsGrid) return;
    
    const percentage = stats.totalTasks > 0 ? Math.round((stats.completedTasks / stats.totalTasks) * 100) : 0;

    statsGrid.innerHTML = `
        <div class="stats-card">
            <h4>Total Tasks</h4>
            <div class="stats-val" id="totalStatsVal">${stats.totalTasks}</div>
        </div>
        <div class="stats-card">
            <h4>Completed</h4>
            <div class="stats-val success-text" id="completedStatsVal">${stats.completedTasks}</div>
        </div>
        <div class="stats-card">
            <h4>Pending</h4>
            <div class="stats-val warning-text" id="pendingStatsVal">${stats.pendingTasks}</div>
        </div>
        <div class="stats-card">
            <h4>Overdue</h4>
            <div class="stats-val danger-text" id="overdueStatsVal">${stats.overdueTasks}</div>
        </div>
        <div class="stats-progress-container" style="grid-column: 1 / -1; margin-top: 15px;">
            <div style="display:flex; justify-content:space-between; margin-bottom:8px; font-weight:600; font-size:0.9rem;">
                <span>Completion Analytics</span>
                <span>${percentage}%</span>
            </div>
            <div class="progress-bar-bg" style="background:rgba(255,255,255,0.05); border-radius:10px; height:12px; overflow:hidden;">
                <div class="progress-bar-fill" style="width: ${percentage}%; background:linear-gradient(90deg, var(--primary) 0%, #10b981 100%); height:100%; transition: width 0.5s ease;"></div>
            </div>
        </div>
    `;
    
    const totalEl = document.getElementById("totalTasksCount");
    if (totalEl) {
        totalEl.textContent = stats.totalTasks;
    }
}

export function renderNotificationsList(notifications, onRead) {
    const list = document.getElementById("notificationsList");
    if (!list) return;
    list.innerHTML = "";

    if (notifications.length === 0) {
        list.innerHTML = `<li class="notification-item empty"><p>No new alerts</p></li>`;
        return;
    }

    notifications.forEach(n => {
        const li = document.createElement("li");
        li.className = `notification-item ${n.read ? 'read' : 'unread'}`;
        
        const time = new Date(n.createdAt).toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'});
        const date = new Date(n.createdAt).toLocaleDateString([], {month: 'short', day: 'numeric'});

        li.innerHTML = `
            <div class="notif-content">
                <p class="notif-msg">${escapeHTML(n.message)}</p>
                <span class="notif-time">${date} at ${time}</span>
            </div>
            ${!n.read ? `<button class="btn-notif-read" data-id="${n.id}" aria-label="Mark read">✓</button>` : ''}
        `;
        list.appendChild(li);
    });

    list.querySelectorAll('.btn-notif-read').forEach(btn => {
        btn.addEventListener('click', () => onRead(parseInt(btn.getAttribute('data-id'))));
    });
}

export function renderActivityFeed(activities) {
    const list = document.getElementById("activityFeedList");
    if (!list) return;
    list.innerHTML = "";

    if (activities.length === 0) {
        list.innerHTML = `<li class="activity-item empty"><p>No recent activity logged</p></li>`;
        return;
    }

    activities.forEach(act => {
        const li = document.createElement("li");
        li.className = "activity-item";
        
        const time = new Date(act.createdAt).toLocaleString([], {month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit'});

        li.innerHTML = `
            <div class="activity-badge-dot ${act.action.toLowerCase()}"></div>
            <div class="activity-details">
                <span class="activity-action-label">${escapeHTML(act.action)}</span>
                <p class="activity-desc">${escapeHTML(act.details)}</p>
                <span class="activity-time">${time} by <b>${escapeHTML(act.username)}</b></span>
            </div>
        `;
        list.appendChild(li);
    });
}
