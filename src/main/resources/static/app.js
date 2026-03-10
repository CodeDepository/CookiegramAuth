/* ============================================================
   Cookiegram - app.js
   Cookie-based auth only. No tokens in localStorage.
   All fetches use credentials:"include" so the browser
   sends the HttpOnly CG_SESSION cookie automatically.
   ============================================================ */

var BASE = '';

/* ============================================================
   ROLE MAPS  (single source of truth used everywhere)
   ============================================================ */
var ROLE_HOME = {
  CUSTOMER: '/customer.html',
  EMPLOYEE: '/employee.html',
  OWNER:    '/owner.html'
};

var ROLE_LABEL = {
  CUSTOMER: 'customer',
  EMPLOYEE: 'employee',
  OWNER:    'owner'
};

/* ============================================================
   API FETCH
   ============================================================ */
async function apiFetch(path, options) {
  var method = (options && options.method) ? options.method : 'GET';
  var body   = (options && options.body   != null) ? options.body : null;

  var fetchOpts = {
    method:      method,
    credentials: 'include',
    headers:     { 'Content-Type': 'application/json' }
  };
  if (body !== null) fetchOpts.body = JSON.stringify(body);

  var res  = await fetch(BASE + path, fetchOpts);
  var text = await res.text();
  var data = null;
  try { data = text ? JSON.parse(text) : null; } catch (e) { data = { raw: text }; }

  if (!res.ok) {
    var err    = new Error(parseError(data, res.status));
    err.status = res.status;
    err.data   = data;
    throw err;
  }
  return data;
}

/* ============================================================
   ERROR PARSER
   ============================================================ */
function parseError(data, status) {
  if (!data) {
    if (status === 401) return 'Wrong credentials.';
    if (status === 403) return 'Not allowed.';
    if (status === 404) return 'Not found.';
    if (status >= 500)  return 'Server error - try again.';
    return 'Something went wrong.';
  }
  if (typeof data === 'string') return data;
  if (data.details && Array.isArray(data.details)) {
    return data.details.map(function(d) {
      return cap(d.field) + ': ' + d.message;
    }).join('\n');
  }
  return data.message || data.error || data.raw || 'Something went wrong.';
}

function cap(s) {
  return s ? s.charAt(0).toUpperCase() + s.slice(1) : s;
}

function esc(s) {
  return String(s)
    .replace(/&/g,  '&amp;')
    .replace(/</g,  '&lt;')
    .replace(/>/g,  '&gt;')
    .replace(/"/g,  '&quot;');
}

/* ============================================================
   TOAST NOTIFICATIONS
   ============================================================ */
var toast = (function() {
  var ICONS = {
    success: '<svg width="16" height="16" fill="none" viewBox="0 0 16 16"><circle cx="8" cy="8" r="6.5" stroke="currentColor" stroke-width="1.5"/><path d="M5 8l2 2 4-4" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/></svg>',
    error:   '<svg width="16" height="16" fill="none" viewBox="0 0 16 16"><circle cx="8" cy="8" r="6.5" stroke="currentColor" stroke-width="1.5"/><path d="M5.5 5.5l5 5M10.5 5.5l-5 5" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/></svg>',
    info:    '<svg width="16" height="16" fill="none" viewBox="0 0 16 16"><circle cx="8" cy="8" r="6.5" stroke="currentColor" stroke-width="1.5"/><path d="M8 7v4M8 5.5h.01" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/></svg>'
  };

  function getContainer() {
    var el = document.getElementById('toast-container');
    if (!el) {
      el = document.createElement('div');
      el.id = 'toast-container';
      document.body.appendChild(el);
    }
    return el;
  }

  function show(type, title, msg) {
    var c = getContainer();
    var t = document.createElement('div');
    t.className = 'toast toast-' + type;
    t.innerHTML =
      '<div class="toast-icon">' + (ICONS[type] || '') + '</div>' +
      '<div>' +
        '<div class="toast-title">' + esc(title) + '</div>' +
        (msg ? '<div class="toast-msg">' + esc(msg) + '</div>' : '') +
      '</div>';
    c.appendChild(t);

    var delay = msg ? 4500 : 3000;
    setTimeout(function() {
      t.classList.add('toast-exit');
      t.addEventListener('animationend', function() { t.remove(); }, { once: true });
    }, delay);
  }

  return {
    success: function(title, msg) { show('success', title, msg); },
    error:   function(title, msg) { show('error',   title, msg); },
    info:    function(title, msg) { show('info',    title, msg); }
  };
}());

/* ============================================================
   BUTTON LOADING STATE
   ============================================================ */
function setLoading(btn, on) {
  if (on) {
    btn.classList.add('loading');
    btn.disabled = true;
  } else {
    btn.classList.remove('loading');
    btn.disabled = false;
  }
}

/* ============================================================
   ALERT BANNERS
   ============================================================ */
var ALERT_ICONS = {
  error:   '<svg width="15" height="15" fill="none" viewBox="0 0 16 16"><circle cx="8" cy="8" r="6.5" stroke="currentColor" stroke-width="1.5"/><path d="M8 5v3.5M8 10.5h.01" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/></svg>',
  success: '<svg width="15" height="15" fill="none" viewBox="0 0 16 16"><circle cx="8" cy="8" r="6.5" stroke="currentColor" stroke-width="1.5"/><path d="M5 8l2 2 4-4" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/></svg>',
  info:    '<svg width="15" height="15" fill="none" viewBox="0 0 16 16"><circle cx="8" cy="8" r="6.5" stroke="currentColor" stroke-width="1.5"/><path d="M8 7v4M8 5.5h.01" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/></svg>'
};

function showAlert(el, type, msg) {
  el.className = 'alert alert-' + type + ' visible';
  el.innerHTML =
    '<span>' + (ALERT_ICONS[type] || '') + '</span>' +
    '<span>' + esc(msg) + '</span>';
}

function hideAlert(el) {
  el.className = 'alert';
  el.innerHTML = '';
}

/* ============================================================
   FIELD VALIDATION
   ============================================================ */
function setFieldError(inp, msg) {
  inp.classList.add('input-error');
  inp.classList.remove('input-success');
  var field = inp.closest ? inp.closest('.field') : null;
  var errEl = field ? field.querySelector('.field-error') : null;
  if (errEl) { errEl.textContent = msg; errEl.classList.add('visible'); }
}

function clearFieldError(inp) {
  inp.classList.remove('input-error');
  var field = inp.closest ? inp.closest('.field') : null;
  var errEl = field ? field.querySelector('.field-error') : null;
  if (errEl) errEl.classList.remove('visible');
}

function setFieldSuccess(inp) {
  inp.classList.remove('input-error');
  inp.classList.add('input-success');
  var field = inp.closest ? inp.closest('.field') : null;
  var errEl = field ? field.querySelector('.field-error') : null;
  if (errEl) errEl.classList.remove('visible');
}

/* ============================================================
   PASSWORD VISIBILITY TOGGLES
   ============================================================ */
var EYE_ON  = '<svg width="17" height="17" fill="none" viewBox="0 0 24 24"><path d="M1 12S5 4 12 4s11 8 11 8-4 8-11 8S1 12 1 12z" stroke="currentColor" stroke-width="1.75" stroke-linecap="round"/><circle cx="12" cy="12" r="3" stroke="currentColor" stroke-width="1.75"/></svg>';
var EYE_OFF = '<svg width="17" height="17" fill="none" viewBox="0 0 24 24"><path d="M17.94 17.94A10.07 10.07 0 0112 20c-7 0-11-8-11-8a18.45 18.45 0 015.06-5.94M9.9 4.24A9.12 9.12 0 0112 4c7 0 11 8 11 8a18.5 18.5 0 01-2.16 3.19M1 1l22 22" stroke="currentColor" stroke-width="1.75" stroke-linecap="round"/><path d="M10.73 10.73A3 3 0 0013.27 13.27" stroke="currentColor" stroke-width="1.75" stroke-linecap="round"/></svg>';

function initPasswordToggles() {
  var btns = document.querySelectorAll('.pw-toggle');
  for (var i = 0; i < btns.length; i++) {
    (function(btn) {
      btn.innerHTML = EYE_ON;
      btn.addEventListener('click', function() {
        var wrap = btn.closest ? btn.closest('.input-wrap') : btn.parentNode;
        var inp  = wrap ? wrap.querySelector('input') : null;
        if (!inp) return;
        var showing = inp.type === 'password';
        inp.type      = showing ? 'text' : 'password';
        btn.innerHTML = showing ? EYE_OFF : EYE_ON;
      });
    }(btns[i]));
  }
}

/* ============================================================
   PASSWORD STRENGTH BAR
   ============================================================ */
function initStrength(inputId, barId, labelId, wrapId) {
  var inp   = document.getElementById(inputId);
  var bar   = document.getElementById(barId);
  var lbl   = document.getElementById(labelId);
  var wrap  = document.getElementById(wrapId);
  if (!inp) return;

  inp.addEventListener('input', function() {
    var v = inp.value;
    if (!v) {
      if (wrap) wrap.classList.add('hidden');
      return;
    }
    if (wrap) wrap.classList.remove('hidden');

    var score = 0;
    if (v.length >= 6)           score++;
    if (v.length >= 10)          score++;
    if (/[A-Z]/.test(v))         score++;
    if (/[0-9]/.test(v))         score++;
    if (/[^A-Za-z0-9]/.test(v))  score++;

    var levels = [
      ['20%', '#ff5c5c', 'very weak'],
      ['40%', '#ff8c42', 'weak'],
      ['60%', '#ffd166', 'fair'],
      ['80%', '#4ecb71', 'good'],
      ['100%', '#27ae60', 'strong']
    ];
    var lvl = levels[Math.min(Math.max(score - 1, 0), 4)];
    if (bar) { bar.style.width = lvl[0]; bar.style.background = lvl[1]; }
    if (lbl) lbl.textContent = lvl[2];
  });
}

/* ============================================================
   CORE AUTH
   ============================================================ */

/* Single call to /api/auth/me. Returns user or null. */
async function checkAuth() {
  try {
    return await apiFetch('/api/auth/me');
  } catch (e) {
    return null;
  }
}

/* Redirect to the dashboard for this role. */
function redirectByRole(role) {
  var dest = ROLE_HOME[role];
  if (!dest) dest = '/login.html';
  window.location.replace(dest);
}

/*
  requireAuth(requiredRole)

  Called once at the top of every protected page.
  - Fetches /api/auth/me exactly once.
  - Redirects to /login.html if not logged in.
  - Redirects to their own dashboard if the role does not match.
  - Returns the user object on success, or null (page is already redirecting).
  - Pass null as requiredRole to allow any authenticated user.
*/
async function requireAuth(requiredRole) {
  var user = await checkAuth();

  if (!user) {
    window.location.replace('/login.html');
    return null;
  }

  if (requiredRole && user.role !== requiredRole) {
    var dest = ROLE_HOME[user.role];
    if (!dest) dest = '/login.html';
    window.location.replace(dest);
    return null;
  }

  document.body.classList.add('role-' + user.role.toLowerCase());
  return user;
}

/*
  hydrateNav(user)

  Accepts the user object already fetched by requireAuth() or checkAuth().
  Does NOT make any network calls when a user object is passed.
  Updates the nav to show the logged-in state.
*/
function hydrateNav(user) {
  var navAuth   = document.getElementById('nav-auth');
  var navUser   = document.getElementById('nav-user');
  var navName   = document.getElementById('nav-username');
  var navRole   = document.getElementById('nav-role');
  var navLogout = document.getElementById('nav-logout');
  var navDash   = document.getElementById('nav-dashboard');

  if (user) {
    document.body.classList.add('role-' + user.role.toLowerCase());
    if (navAuth)   navAuth.classList.add('hidden');
    if (navUser)   navUser.classList.remove('hidden');
    if (navName)   navName.textContent = user.username;
    if (navRole)   navRole.textContent = ROLE_LABEL[user.role] || user.role.toLowerCase();
    if (navDash)   navDash.href = ROLE_HOME[user.role] || '/';
    if (navLogout) {
      /* Remove any previous listener before adding a new one */
      var fresh = navLogout.cloneNode(true);
      navLogout.parentNode.replaceChild(fresh, navLogout);
      fresh.addEventListener('click', doLogout);
    }
  } else {
    if (navUser) navUser.classList.add('hidden');
    if (navAuth) navAuth.classList.remove('hidden');
  }
}

/* Sign the user out server-side, then go to the landing page. */
async function doLogout() {
  try {
    await apiFetch('/api/auth/logout', { method: 'POST' });
    toast.success('Signed out', 'See you next time!');
    setTimeout(function() { window.location.replace('/index.html'); }, 800);
  } catch (e) {
    toast.error('Logout failed', e.message);
  }
}

/* Wire up password toggles once the DOM is ready. */
document.addEventListener('DOMContentLoaded', initPasswordToggles);