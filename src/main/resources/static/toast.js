// CASEZERO toast notifications: show server messages and dismiss them automatically.
document.addEventListener("DOMContentLoaded", () => {
    document.querySelectorAll("[data-toast]").forEach((toast) => {
        const dismiss = () => {
            toast.classList.add("is-hiding");
            window.setTimeout(() => toast.remove(), 280);
        };
        window.setTimeout(dismiss, 4500);
    });
});
