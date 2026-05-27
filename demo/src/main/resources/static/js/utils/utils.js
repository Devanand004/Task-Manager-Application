export function escapeHTML(str) {
    if (!str) return '';
    const div = document.createElement('div');
    div.textContent = str;
    return div.innerHTML;
}

export function formatDate(dateString) {
    if (!dateString) return 'No due date';
    const dateOnly = dateString.split('T')[0];
    const parts = dateOnly.split('-');
    if (parts.length === 3) {
        const year = parts[0];
        const month = parts[1];
        const day = parts[2];
        const dateObj = new Date(year, month - 1, day);
        return dateObj.toLocaleDateString(undefined, { year: 'numeric', month: 'short', day: 'numeric' });
    }
    return dateString;
}

export function showToast(message, type = "info") {
    let container = document.getElementById("toastContainer");
    if (!container) {
        container = document.createElement("div");
        container.id = "toastContainer";
        container.className = "toast-container";
        document.body.appendChild(container);
    }
    
    const toast = document.createElement("div");
    toast.className = `toast ${type}`;
    toast.innerHTML = `
        <div class="toast-content">
            <span class="toast-icon">${type === 'success' ? '✨' : type === 'error' ? '⚠️' : 'ℹ️'}</span>
            <span class="toast-message"></span>
        </div>
        <span class="toast-close">&times;</span>
    `;
    
    toast.querySelector(".toast-message").textContent = message;
    
    const closeBtn = toast.querySelector(".toast-close");
    if (closeBtn) {
        closeBtn.addEventListener("click", () => {
            toast.remove();
        });
    }
    
    container.appendChild(toast);
    
    setTimeout(() => {
        toast.classList.add("fade-out");
        setTimeout(() => toast.remove(), 500);
    }, 4000);
}
