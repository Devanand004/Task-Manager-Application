import { CONSTANTS } from '../config/constants.js';

export const state = {
    tasks: [],
    filters: {
        status: CONSTANTS.DEFAULTS.STATUS_FILTER,
        priority: 'ALL',
        category: '',
        archived: false,
        startDate: '',
        endDate: '',
        search: '',
        page: 0,
        size: CONSTANTS.DEFAULTS.PAGE_SIZE,
        sort: CONSTANTS.DEFAULTS.SORT
    },
    activeTab: 'tasks', // tasks, archive, activity, profile
    unreadNotifications: 0,
    notifications: [],
    pagination: {
        totalPages: 0,
        totalElements: 0,
        first: true,
        last: true
    },
    user: {
        isAuthenticated: false,
        username: '',
        role: ''
    },

    updateUserFromStorage() {
        const token = localStorage.getItem(CONSTANTS.STORAGE_KEYS.TOKEN);
        const username = localStorage.getItem(CONSTANTS.STORAGE_KEYS.USERNAME);
        const role = localStorage.getItem(CONSTANTS.STORAGE_KEYS.ROLE);

        if (token && username) {
            this.user.isAuthenticated = true;
            this.user.username = username;
            this.user.role = role || 'USER';
        } else {
            this.user.isAuthenticated = false;
            this.user.username = '';
            this.user.role = '';
        }
    }
};
