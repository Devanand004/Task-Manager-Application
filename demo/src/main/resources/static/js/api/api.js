import { CONSTANTS } from '../config/constants.js';

export const api = {
    getToken() {
        return localStorage.getItem(CONSTANTS.STORAGE_KEYS.TOKEN);
    },

    setToken(token) {
        localStorage.setItem(CONSTANTS.STORAGE_KEYS.TOKEN, token);
    },

    clearToken() {
        localStorage.removeItem(CONSTANTS.STORAGE_KEYS.TOKEN);
        localStorage.removeItem(CONSTANTS.STORAGE_KEYS.USERNAME);
        localStorage.removeItem(CONSTANTS.STORAGE_KEYS.ROLE);
    },

    getUser() {
        return {
            username: localStorage.getItem(CONSTANTS.STORAGE_KEYS.USERNAME),
            role: localStorage.getItem(CONSTANTS.STORAGE_KEYS.ROLE)
        };
    },

    setUser(username, role) {
        localStorage.setItem(CONSTANTS.STORAGE_KEYS.USERNAME, username);
        localStorage.setItem(CONSTANTS.STORAGE_KEYS.ROLE, role);
    },

    async request(endpoint, options = {}) {
        const token = this.getToken();
        const headers = {
            'Content-Type': 'application/json',
            ...options.headers
        };

        if (token) {
            headers['Authorization'] = `Bearer ${token}`;
        }

        const config = {
            ...options,
            headers
        };

        const response = await fetch(`${CONSTANTS.API_BASE_URL}${endpoint}`, config);

        if (response.status === 401) {
            this.clearToken();
            window.dispatchEvent(new CustomEvent('auth-changed'));
            throw new Error('Authentication required or session expired');
        }

        const data = await response.json().catch(() => ({}));
        if (!response.ok) {
            if (response.status === 403) {
                throw new Error(data.message || 'You do not have permission to perform this action');
            }
            const message = data.message || Object.values(data.details || {}).join(', ') || 'Request failed';
            throw new Error(message);
        }

        return data;
    },

    async login(username, password) {
        const res = await this.request('/auth/login', {
            method: 'POST',
            body: JSON.stringify({ username, password })
        });
        if (res.success && res.data) {
            this.setToken(res.data.token);
            this.setUser(res.data.username, res.data.role);
            window.dispatchEvent(new CustomEvent('auth-changed'));
        }
        return res;
    },

    async register(username, password, role = 'USER') {
        const res = await this.request('/auth/register', {
            method: 'POST',
            body: JSON.stringify({ username, password, role })
        });
        if (res.success && res.data) {
            this.setToken(res.data.token);
            this.setUser(res.data.username, res.data.role);
            window.dispatchEvent(new CustomEvent('auth-changed'));
        }
        return res;
    },

    async logout() {
        this.clearToken();
        window.dispatchEvent(new CustomEvent('auth-changed'));
    },

    async getTasks(params = {}) {
        const query = new URLSearchParams();
        if (params.status && params.status !== 'ALL') query.append('status', params.status);
        if (params.priority && params.priority !== 'ALL') query.append('priority', params.priority);
        if (params.category) query.append('category', params.category);
        if (params.archived !== undefined) query.append('archived', params.archived);
        if (params.startDate) query.append('startDate', params.startDate);
        if (params.endDate) query.append('endDate', params.endDate);
        if (params.search) query.append('search', params.search);
        
        query.append('page', params.page || 0);
        query.append('size', params.size || 10);
        query.append('sort', params.sort || 'createdAt,desc');

        return this.request(`/tasks?${query.toString()}`);
    },

    async getTaskById(id) {
        return this.request(`/tasks/${id}`);
    },

    async createTask(taskData) {
        return this.request('/tasks', {
            method: 'POST',
            body: JSON.stringify(taskData)
        });
    },

    async updateTask(id, taskData) {
        return this.request(`/tasks/${id}`, {
            method: 'PUT',
            body: JSON.stringify(taskData)
        });
    },

    async archiveTask(id) {
        return this.request(`/tasks/${id}/archive`, {
            method: 'PUT'
        });
    },

    async restoreTask(id) {
        return this.request(`/tasks/${id}/restore`, {
            method: 'PUT'
        });
    },

    async deleteTask(id) {
        return this.request(`/tasks/${id}`, {
            method: 'DELETE'
        });
    },

    async getStats() {
        return this.request('/tasks/stats');
    },

    async getNotifications(page = 0) {
        return this.request(`/notifications?page=${page}&size=10`);
    },

    async getUnreadNotificationsCount() {
        return this.request('/notifications/unread-count');
    },

    async markNotificationRead(id) {
        return this.request(`/notifications/${id}/read`, {
            method: 'PUT'
        });
    },

    async markAllNotificationsRead() {
        return this.request('/notifications/mark-all-read', {
            method: 'POST'
        });
    },

    async getProfile() {
        return this.request('/profile');
    },

    async updateProfile(profileData) {
        return this.request('/profile', {
            method: 'PUT',
            body: JSON.stringify(profileData)
        });
    },

    async changePassword(passwordData) {
        return this.request('/profile/password', {
            method: 'PUT',
            body: JSON.stringify(passwordData)
        });
    },

    async getActivity(page = 0) {
        return this.request(`/tasks/activity?page=${page}&size=15`);
    }
};
