// ==============================================================
// CINEPHILE — Shared API, Session, Dialog, Formatting & Toast Helper
// ==============================================================

const SESSION_KEY = 'cinephile_session';

function getSession() {
  try { return JSON.parse(localStorage.getItem(SESSION_KEY)); }
  catch { return null; }
}

function setSession(data) {
  localStorage.setItem(SESSION_KEY, JSON.stringify(data));
}

function clearSession() {
  localStorage.removeItem(SESSION_KEY);
}

async function apiRequest(path, options = {}) {
  const session = getSession();
  const headers = { 'Content-Type': 'application/json', ...(options.headers || {}) };
  if (session && session.token) {
    headers['Authorization'] = `Bearer ${session.token}`;
  }
  const res = await fetch(path, { ...options, headers });
  const text = await res.text();
  let json;
  try { json = JSON.parse(text); }
  catch { throw new Error(text || 'Unexpected server response'); }
  if (!res.ok || json.ok === false) {
    if (res.status === 401 || (json.error && json.error.includes('Please sign in'))) {
      clearSession();
    }
    throw new Error(json.error || `Request failed (${res.status})`);
  }
  return json;
}

function esc(str) {
  return String(str ?? '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;');
}

function formatTime12(timeStr) {
  if (!timeStr) return '';
  const [hRaw, mRaw] = String(timeStr).split(':');
  let h = parseInt(hRaw, 10);
  const m = mRaw || '00';
  const ampm = h >= 12 ? 'PM' : 'AM';
  h = h % 12 || 12;
  return `${h}:${m} ${ampm}`;
}

function formatDateFriendly(isoDate) {
  if (!isoDate) return '';
  const parts = String(isoDate).split('-').map(Number);
  if (parts.length !== 3) return isoDate;
  const d = new Date(parts[0], parts[1] - 1, parts[2]);
  const months = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
  const formatted = `${String(d.getDate()).padStart(2, '0')} ${months[d.getMonth()]} ${d.getFullYear()}`;

  const now = new Date();
  const today = new Date(now.getFullYear(), now.getMonth(), now.getDate());
  const diffDays = Math.round((d - today) / 86400000);
  if (diffDays === 0) return `Today (${formatted})`;
  if (diffDays === 1) return `Tomorrow (${formatted})`;
  return formatted;
}

function showToast(msg, type = 'success') {
  let container = document.getElementById('toast-container');
  if (!container) {
    container = document.createElement('div');
    container.id = 'toast-container';
    container.className = 'toast-container';
    container.setAttribute('aria-live', 'polite');
    document.body.appendChild(container);
  }
  const el = document.createElement('div');
  el.className = `toast ${type === 'error' ? 'error' : ''}`;
  el.textContent = msg;
  container.appendChild(el);
  setTimeout(() => el.remove(), 3800);
}

function renderNavbar() {
  const nav = document.getElementById('nav-actions');
  if (!nav) return;
  const session = getSession();
  const current = window.location.pathname;

  let html = `<a href="/index.html" class="nav-link" ${current === '/' || current.endsWith('index.html') ? 'aria-current="page"' : ''}>Now Showing</a>`;

  if (session) {
    if (session.role === 'ADMIN') {
      html += `<a href="/admin.html" class="nav-link" ${current.endsWith('admin.html') ? 'aria-current="page"' : ''}>Admin</a>`;
    } else {
      html += `<a href="/my-bookings.html" class="nav-link" ${current.endsWith('my-bookings.html') ? 'aria-current="page"' : ''}>My Tickets</a>`;
    }
    html += `
      <span class="user-pill">
        <span>${esc(session.name)}</span>
      </span>
      <button type="button" class="btn btn-danger btn-sm" id="btn-logout">Sign Out</button>
    `;
  } else {
    html += `<a href="/login.html" class="btn btn-primary btn-sm">Sign In</a>`;
  }

  nav.innerHTML = html;

  const logoutBtn = document.getElementById('btn-logout');
  if (logoutBtn) {
    logoutBtn.addEventListener('click', () => {
      clearSession();
      showToast('Signed out');
      setTimeout(() => { window.location.href = '/index.html'; }, 350);
    });
  }
}

function initDialogPolyfills() {
  if (!('closedBy' in HTMLDialogElement.prototype)) {
    const outside = (e, dialog) => {
      const r = dialog.getBoundingClientRect();
      return e.clientX < r.left || e.clientX > r.right || e.clientY < r.top || e.clientY > r.bottom;
    };
    for (const dialog of document.querySelectorAll('dialog[closedby="any"]')) {
      let pressedOutside = false;
      dialog.addEventListener('pointerdown', (e) => { pressedOutside = outside(e, dialog); });
      dialog.addEventListener('pointerup', (e) => {
        if (pressedOutside && outside(e, dialog)) dialog.close();
      });
    }
  }
}

function initPosterLockups() {
  if (!CSS.supports('text-fit', 'grow per-line-all')) {
    const fit = (lockup) => {
      const lines = [...lockup.querySelectorAll(':scope > span')];
      for (const line of lines) line.style.removeProperty('--text-fit-scale');
      const scales = lines.map(line => (line.offsetWidth > 0 ? lockup.clientWidth / line.offsetWidth : 1));
      lines.forEach((line, i) => line.style.setProperty('--text-fit-scale', scales[i]));
    };
    for (const lockup of document.querySelectorAll('.poster-lockup')) {
      new ResizeObserver(() => fit(lockup)).observe(lockup);
    }
  }
}

document.addEventListener('DOMContentLoaded', () => {
  renderNavbar();
  initDialogPolyfills();
  initPosterLockups();
});
