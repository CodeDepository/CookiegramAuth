/* ============================================================
   Cookiegram – app.js  (cookie-based auth, no token storage)
   ============================================================ */

const BASE = '';

/* ── API fetch ─────────────────────────────────────────────
   All calls use credentials:"include" — browser sends
   HttpOnly CG_SESSION cookie automatically.
   No tokens ever touch JS.
   ─────────────────────────────────────────────────────── */
async function apiFetch(path, { method = 'GET', body = null } = {}) {
  const opts = {
    method,
    credentials: 'include',
    headers: { 'Content-Type': 'application/json' },
  };
  if (body !== null) opts.body = JSON.stringify(body);
  const res  = await fetch(BASE + path, opts);
  let data   = null;
  const text = await res.text();
  try { data = text ? JSON.parse(text) : null; } catch { data = { raw: text }; }
  if (!res.ok) {
    const err  = new Error(parseError(data, res.status));
    err.status = res.status;
    err.data   = data;
    throw err;
  }
  return data;
}

/* ── Error parser ────────────────────────────────────────── */
function parseError(data, status) {
  if (!data) {
    if (status === 401) return 'Wrong credentials.';
    if (status === 403) return 'Not allowed.';
    if (status === 404) return 'Not found.';
    if (status >= 500)  return 'Server error — try again.';
    return 'Something went wrong.';
  }
  if (typeof data === 'string') return data;
  if (data.details && Array.isArray(data.details))
    return data.details.map(d => `${cap(d.field)}: ${d.message}`).join('\n');
  return data.message || data.error || data.raw || 'Something went wrong.';
}
function cap(s) { return s ? s.charAt(0).toUpperCase() + s.slice(1) : s; }

/* ── Toast ───────────────────────────────────────────────── */
const toast = (() => {
  function init() {
    let el = document.getElementById('toast-container');
    if (!el) { el = document.createElement('div'); el.id = 'toast-container'; document.body.appendChild(el); }
    return el;
  }
  const ICONS = {
    success: `<svg width="16" height="16" fill="none" viewBox="0 0 16 16"><circle cx="8" cy="8" r="6.5" stroke="currentColor" stroke-width="1.5"/><path d="M5 8l2 2 4-4" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/></svg>`,
    error:   `<svg width="16" height="16" fill="none" viewBox="0 0 16 16"><circle cx="8" cy="8" r="6.5" stroke="currentColor" stroke-width="1.5"/><path d="M5.5 5.5l5 5M10.5 5.5l-5 5" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/></svg>`,
    info:    `<svg width="16" height="16" fill="none" viewBox="0 0 16 16"><circle cx="8" cy="8" r="6.5" stroke="currentColor" stroke-width="1.5"/><path d="M8 7v4M8 5.5h.01" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/></svg>`,
  };
  function show(type, title, msg) {
    const c = init();
    const t = document.createElement('div');
    t.className = `toast toast-${type}`;
    t.innerHTML = `<div class="toast-icon">${ICONS[type]||''}</div><div><div class="toast-title">${esc(title)}</div>${msg?`<div class="toast-msg">${esc(msg)}</div>`:''}</div>`;
    c.appendChild(t);
    setTimeout(() => { t.classList.add('toast-exit'); t.addEventListener('animationend', () => t.remove(), {once:true}); }, msg ? 5000 : 3500);
  }
  return {
    success: (t, m) => show('success', t, m),
    error:   (t, m) => show('error',   t, m),
    info:    (t, m) => show('info',    t, m),
  };
})();

function esc(s) {
  return String(s).replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;');
}

/* ── Loading button ──────────────────────────────────────── */
function setLoading(btn, on) {
  on ? (btn.classList.add('loading'),    btn.disabled = true)
     : (btn.classList.remove('loading'), btn.disabled = false);
}

/* ── Alert helpers ───────────────────────────────────────── */
const ALERT_ICONS = {
  error:   `<svg width="15" height="15" fill="none" viewBox="0 0 16 16"><circle cx="8" cy="8" r="6.5" stroke="currentColor" stroke-width="1.5"/><path d="M8 5v3.5M8 10.5h.01" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/></svg>`,
  success: `<svg width="15" height="15" fill="none" viewBox="0 0 16 16"><circle cx="8" cy="8" r="6.5" stroke="currentColor" stroke-width="1.5"/><path d="M5 8l2 2 4-4" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/></svg>`,
  info:    `<svg width="15" height="15" fill="none" viewBox="0 0 16 16"><circle cx="8" cy="8" r="6.5" stroke="currentColor" stroke-width="1.5"/><path d="M8 7v4M8 5.5h.01" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/></svg>`,
};
function showAlert(el, type, msg) {
  el.className = `alert alert-${type} visible`;
  el.innerHTML = `<span>${ALERT_ICONS[type]||''}</span><span>${esc(msg)}</span>`;
}
function hideAlert(el) { el.className = 'alert'; el.innerHTML = ''; }

/* ── Password toggles ────────────────────────────────────── */
const EYE = `<svg width="17" height="17" fill="none" viewBox="0 0 24 24"><path d="M1 12S5 4 12 4s11 8 11 8-4 8-11 8S1 12 1 12z" stroke="currentColor" stroke-width="1.75" stroke-linecap="round"/><circle cx="12" cy="12" r="3" stroke="currentColor" stroke-width="1.75"/></svg>`;
const EYE_OFF = `<svg width="17" height="17" fill="none" viewBox="0 0 24 24"><path d="M17.94 17.94A10.07 10.07 0 0112 20c-7 0-11-8-11-8a18.45 18.45 0 015.06-5.94M9.9 4.24A9.12 9.12 0 0112 4c7 0 11 8 11 8a18.5 18.5 0 01-2.16 3.19M1 1l22 22" stroke="currentColor" stroke-width="1.75" stroke-linecap="round"/><path d="M10.73 10.73A3 3 0 0013.27 13.27" stroke="currentColor" stroke-width="1.75" stroke-linecap="round"/></svg>`;

function initPasswordToggles() {
  document.querySelectorAll('.pw-toggle').forEach(btn => {
    btn.innerHTML = EYE;
    btn.addEventListener('click', () => {
      const inp = btn.closest('.input-wrap').querySelector('input');
      const show = inp.type === 'password';
      inp.type = show ? 'text' : 'password';
      btn.innerHTML = show ? EYE_OFF : EYE;
    });
  });
}

/* ── Field validation helpers ────────────────────────────── */
function setFieldError(inp, msg) {
  inp.classList.add('input-error');
  inp.classList.remove('input-success');
  const e = inp.closest('.field')?.querySelector('.field-error');
  if (e) { e.textContent = msg; e.classList.add('visible'); }
}
function clearFieldError(inp) {
  inp.classList.remove('input-error');
  const e = inp.closest('.field')?.querySelector('.field-error');
  if (e) e.classList.remove('visible');
}
function setFieldSuccess(inp) {
  inp.classList.remove('input-error');
  inp.classList.add('input-success');
  const e = inp.closest('.field')?.querySelector('.field-error');
  if (e) e.classList.remove('visible');
}

/* ── Auth helpers ────────────────────────────────────────── */
async function checkAuth() {
  try { return await apiFetch('/api/auth/me'); } catch { return null; }
}

async function hydrateNav() {
  const user      = await checkAuth();
  const navAuth   = document.getElementById('nav-auth');
  const navUser   = document.getElementById('nav-user');
  const navName   = document.getElementById('nav-username');
  const navLogout = document.getElementById('nav-logout');
  if (user) {
    navAuth   && navAuth.classList.add('hidden');
    navUser   && navUser.classList.remove('hidden');
    navName   && (navName.textContent = user.username);
    navLogout && navLogout.addEventListener('click', doLogout);
  } else {
    navUser && navUser.classList.add('hidden');
    navAuth && navAuth.classList.remove('hidden');
  }
  return user;
}

async function doLogout() {
  try {
    await apiFetch('/api/auth/logout', { method: 'POST' });
    toast.success('Signed out', 'See you next time 🍪');
    setTimeout(() => { window.location.href = '/'; }, 700);
  } catch (e) {
    toast.error('Logout failed', e.message);
  }
}

/* ── Password strength ───────────────────────────────────── */
function initStrength(inputId, barId, labelId, wrapId) {
  const inp   = document.getElementById(inputId);
  const bar   = document.getElementById(barId);
  const label = document.getElementById(labelId);
  const wrap  = document.getElementById(wrapId);
  if (!inp) return;
  inp.addEventListener('input', () => {
    const v = inp.value;
    if (!v) { wrap && wrap.classList.add('hidden'); return; }
    wrap && wrap.classList.remove('hidden');
    let s = 0;
    if (v.length >= 6)  s++;
    if (v.length >= 10) s++;
    if (/[A-Z]/.test(v)) s++;
    if (/[0-9]/.test(v)) s++;
    if (/[^A-Za-z0-9]/.test(v)) s++;
    const lvl = [
      ['20%','#ff5c5c','very weak'],
      ['40%','#ff8c42','weak'],
      ['60%','#ffd166','fair'],
      ['80%','#4ecb71','good'],
      ['100%','#27ae60','strong ✓'],
    ][Math.min(s - 1, 4)] || ['20%','#ff5c5c','very weak'];
    if (bar)   { bar.style.width = lvl[0]; bar.style.background = lvl[1]; }
    if (label) label.textContent = lvl[2];
  });
}

document.addEventListener('DOMContentLoaded', initPasswordToggles);