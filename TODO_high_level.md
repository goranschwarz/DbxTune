# DbxTune — Future Refactoring TODO

Items are roughly ordered by priority / impact. Higher items unblock lower ones.

---

## 🔴 High Priority

### 1. Bootstrap 5 migration (all pages)
- **What:** `index.html`, `admin/admin.html`, `desktop_app.html`, `HtmlStatic.java` (server-rendered pages) are on Bootstrap **4.6.2**. `config.html` is already on BS5.
- **Why:** Bootstrap 4 is in maintenance-only mode. BS5 ships without jQuery as a hard dependency, changes `data-toggle` → `data-bs-toggle`, `data-dismiss` → `data-bs-dismiss`, `dropdown-menu-right` → `dropdown-menu-end`, `font-weight-bold` → `fw-bold`, `form-group` → `mb-3`, etc.
- **Scope:** All HTML files + `HtmlStatic.java` navbar/modal output.
- **Do together with:** items 3 and 5 below (they depend on BS version).

### 2. Chart.js upgrade: 2.7.3 → 4.x
- **What:** `index.html` and `graph.html` load Chart.js 2.7.3 (2018). Current stable is 4.x.
- **Why:** v3/v4 bring major performance improvements, a better plugin API, built-in time-series adapter, and `chartjs-plugin-annotation` 3.x (the commented-out CDN links already hint at this upgrade having been attempted).
- **Breaking changes:** Config structure changed significantly (`options.scales` layout, plugin registration, `Chart.helpers` API). Requires testing all chart views.
- **Also:** Upgrade bundled plugins: `chartjs-plugin-zoom` (0.7 → 2.x), `chartjs-plugin-annotation` (0.5 → 3.x). Both have breaking changes that align with Chart.js 4.

### 3. Font Awesome 4 → 6 (Free)
- **What:** All pages load Font Awesome **4.4.0**. Current Free is 6.x.
- **Why:** FA 6 adds many new icons, better SVG support, and is actively maintained. FA 4 prefix `fa fa-*` changes to `fa-solid fa-*` (though a compatibility shim exists).
- **Note:** FA 5/6 Free drops some icons that were in FA 4 — audit icon usage before upgrading.

### 4. Moment.js → Day.js (or date-fns)
- **What:** `moment.js` + `moment-duration-format` are loaded on all HtmlStatic-rendered pages and `index.html`.
- **Why:** Moment.js is officially in [maintenance mode](https://momentjs.com/docs/#/-project-status/) — no new features. Day.js is a 2 kB drop-in replacement with the same API. Alternatively, date-fns for a functional approach.
- **Note:** Chart.js 4 requires a date adapter; if upgrading Chart.js, use `chartjs-adapter-dayjs` instead of `chartjs-adapter-moment`.

---

## 🟡 Medium Priority

### 5. Shared UI components — move out of HtmlStatic.java / duplicated HTML
- **What:** The Settings modal HTML and JS functions currently live in **three places** simultaneously: `HtmlStatic.getJavaScriptAtEnd()` (Java string concatenation), and copied into `index.html`, `admin.html`, `desktop_app.html`, `config.html`.
- **Why:** Any change to the modal requires editing 5 files. Will also get worse as more shared dialogs are added (e.g. notifications, profile).
- **Solution:** Move the modal HTML and `dbxOpenSettings()` / `dbxSaveEmail()` / `dbxSavePassword()` JS functions into `dbxcentral.utils.js` (already loaded on every page). The JS injects the modal DOM on `$(document).ready`. Remove the duplicated copies from each HTML file and strip the Settings block out of `HtmlStatic.getJavaScriptAtEnd()`.

### 6. Template engine for static HTML pages
- **What:** `index.html`, `admin.html`, `desktop_app.html` each duplicate the full `<head>`, navbar, and JS imports. The server-rendered pages use `HtmlStatic.java` as a poor-man's template.
- **Why:** Adding a new global CSS/JS library requires editing every file.
- **Solution (light):** Migrate static pages to JSP and use `<%@ include file="..." %>` or `<jsp:include>` for the shared `<head>` block and navbar fragment. Jetty supports JSP with Jasper.
- **Solution (modern):** Introduce Thymeleaf or Freemarker — natural templates, works well with Jetty, enables `th:fragment` / `#include` directives. Significant but worthwhile investment.

### 7. jQuery dependency reduction
- **What:** All pages load jQuery 3.7.1 for DOM manipulation and AJAX.
- **Why:** Bootstrap 5 no longer needs jQuery. Modern vanilla JS (`fetch`, `querySelector`, `classList`) covers most use cases. This is a long-term goal, not urgent.
- **Approach:** After BS5 migration, incrementally replace `$.ajax()` → `fetch()` and `$(selector)` → `document.querySelector()` in new code. Don't rewrite existing code wholesale.

### 8. Bootstrap Table upgrade
- **What:** `config.html` and `admin/admin.html` use `bootstrap-table 1.22.1`.
- **Why:** Verify compatibility with BS5 (bootstrap-table 1.22+ supports BS5 but requires the `data-bs-*` attributes). After BS5 migration, confirm all table features still work.

---

## 🟢 Lower Priority / Nice to Have

### 9. CSRF protection
- **What:** All `POST` forms (login, register, forgot-password, settings, admin operations) have no CSRF token.
- **Why:** Jetty FORM auth provides some protection since `j_security_check` is POST-only and the session cookie is HttpOnly, but explicit CSRF tokens add defence-in-depth.
- **Solution:** Add a servlet filter that generates a per-session token, stored in the session and validated on all state-changing POSTs. Or use a library like OWASP CSRF Guard.

### 10. HTTP security headers
- **What:** Add standard security headers to all responses.
- **Headers to add:**
  - `Content-Security-Policy` (restrict script/style sources)
  - `X-Content-Type-Options: nosniff`
  - `X-Frame-Options: SAMEORIGIN`
  - `Strict-Transport-Security` (if HTTPS)
  - `Referrer-Policy: same-origin`
- **Solution:** A Jetty `HandlerWrapper` or servlet `Filter` that adds these headers to every response.

### 11. Clean up commented-out CDN fallbacks in HTML files
- **What:** Several pages (`index.html`, `config.html`) have many blocks of commented-out CDN `<script>` / `<link>` tags left from version experiments.
- **Why:** Pure housekeeping — reduces noise when reading the files.

### 12. `HtmlStatic.java` string concatenation cleanup
- **What:** `getJavaScriptAtEnd()` mixes `<script>` JS and raw HTML (the modal div) in one method, built via hundreds of `sb.append(...)` lines.
- **Why:** Hard to read and edit. After item 5 is done (shared JS in `dbxcentral.utils.js`) and item 6 (JSP templates), this method shrinks to near-nothing and can be removed entirely.

### 13. User email verification on registration
- **What:** `UserRegistrationServlet` currently accepts any email address without verifying it exists/is owned by the registrant.
- **Why:** Prevents fake registrations and ensures password-recovery emails reach real users.
- **Solution:** On register, send a verification email with a time-limited token. Mark account as `pending` until verified. Add a `Verified` column to `CENTRAL_USERS` and a `UserVerifyServlet` at `/api/user/verify`.

### 14. Account lockout / rate limiting
- **What:** No brute-force protection on `/j_security_check` (Jetty login) or `/api/user/forgot-password`.
- **Why:** Password spray and enumeration attacks are trivial without rate limiting.
- **Solution:** A servlet `Filter` that tracks failed attempts per IP (in-memory `ConcurrentHashMap` with a TTL) and returns `429 Too Many Requests` after N failures within a time window.

### 15. Audit log for user administration actions
- **What:** Admin operations in `UsersAdminServlet` (add/delete user, reset password, change roles) have no persistent audit trail.
- **Why:** For compliance and accountability — who changed what and when.
- **Solution:** A simple `CENTRAL_AUDIT_LOG` table with `(timestamp, actor, action, target, details)`. Write a row on every user-management operation.

---

## 📦 Library Version Summary (current → recommended)

| Library | Current | Recommended |
|---|---|---|
| Bootstrap | 4.6.2 (most pages) / 5.x (config.html) | **5.3.x** everywhere |
| Bootstrap Table | 1.22.1 | 1.23.x (verify BS5 compat) |
| Chart.js | 2.7.3 | **4.4.x** |
| chartjs-plugin-zoom | 0.7.x (bundled) | 2.x |
| chartjs-plugin-annotation | 0.5.x (bundled) | 3.x |
| Font Awesome | 4.4.0 | **6.x Free** |
| jQuery | 3.7.1 | 3.7.1 (keep; reduce usage over time) |
| Moment.js | 2.x | **Day.js 1.x** |
| Bouncy Castle (bcprov) | 1.72 | check for latest 1.8x |
