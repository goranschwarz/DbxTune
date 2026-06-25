/*******************************************************************************
 * dbxLoginModal.js
 *
 * Injectable login modal for DbxCentral.
 * Include this script on any page to get:
 *   - Login/user section injected into the existing navbar (if not already there)
 *   - Full login modal dialog (Login tab + Create Account tab)
 *   - dbxOpenLogin() global function
 *   - Handles ?login URL parameter on page load
 *     (auto-opens modal if not logged in; redirects back to origin page after login)
 *
 * Usage:
 *   <script src="/scripts/dbxtune/js/dbxLoginModal.js"></script>
 *
 * The Login link in the navbar calls dbxOpenLogin().
 * The form submits to j_security_check (container-managed auth).
 * On successful login Jetty redirects to index.html?login=...; the ?login
 * handler there reads sessionStorage.dbxReturnAfterLogin and sends the user
 * back to the page they came from.
 ******************************************************************************/

(function () {

    //--------------------------------------------------------------------------
    // 1. Inject navbar login/user section (if not already present on this page)
    //--------------------------------------------------------------------------
    function injectNavbarLoginSection()
    {
        if (document.getElementById('dbx-nb-isLoggedOut-div'))
            return; // already present (index.html has it inline)

        var navbarCollapse = document.querySelector('.navbar-collapse');
        if (!navbarCollapse)
            return;

        var ul = document.createElement('ul');
        ul.className = 'navbar-nav';
        ul.innerHTML =
            // IS LOGGED IN
            '<div id="dbx-nb-isLoggedIn-div" style="display:none;">' +
            '  <li class="nav-item dropdown">' +
            '    <a class="nav-link dropdown-toggle" href="#"' +
            '       id="dbx-nb-userDropdown" data-toggle="dropdown"' +
            '       aria-haspopup="true" aria-expanded="false">' +
            '      <i class="fa fa-user"></i>' +
            '      <span id="dbx-nb-isLoggedInUser-div"></span>' +
            '    </a>' +
            '    <div class="dropdown-menu dropdown-menu-right" aria-labelledby="dbx-nb-userDropdown">' +
            '      <a class="dropdown-item" href="#" onclick="dbxOpenSettings(); return false;">' +
            '        <i class="fa fa-cog"></i> Settings</a>' +
            '      <a class="dropdown-item" href="/logout">' +
            '        <i class="fa fa-sign-out"></i> Logout</a>' +
            '    </div>' +
            '  </li>' +
            '</div>' +
            // IS LOGGED OUT
            '<div id="dbx-nb-isLoggedOut-div">' +
            '  <a class="nav-link" href="#"' +
            '     onclick="dbxOpenLogin(); return false;">' +
            '    <i class="fa fa-sign-in"></i>' +
            '    <span data-toggle="tooltip" title="Log in as a specific user.">Login</span>' +
            '  </a>' +
            '</div>';

        navbarCollapse.appendChild(ul);
    }

    //--------------------------------------------------------------------------
    // 2. Inject the login modal HTML (if not already present on this page)
    //--------------------------------------------------------------------------
    function injectLoginModal()
    {
        if (document.getElementById('dbx-login-dialog'))
            return; // already present (index.html has it inline — will be removed later)

        // The second tab (Create Account / Request Access) is rendered after
        // fetchLoginConfig() returns so we know which variant to show.
        // For now inject just the skeleton + login tab; the second tab is
        // injected by applyLoginConfig() called from fetchAndInjectOAuthButtons.
        var html =
            '<div id="dbx-login-dialog" class="modal fade" tabindex="-1" role="dialog" aria-hidden="true">\n' +
            '  <div class="modal-dialog modal-dialog-centered" style="max-width:460px;">\n' +
            '    <div class="modal-content">\n' +
            '      <div class="modal-header">\n' +
            '        <h3>DbxCentral - Login</h3>\n' +
            '        <button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>\n' +
            '      </div>\n' +
            '      <div class="modal-body p-0" style="overflow-y:auto;max-height:85vh;">\n' +
            '        <ul class="nav nav-tabs px-3 pt-2" id="dbx-login-tabs" role="tablist">\n' +
            '          <li class="nav-item">\n' +
            '            <a class="nav-link active" id="dbx-tab-login" data-toggle="tab" href="#dbx-pane-login" role="tab">Login</a>\n' +
            '          </li>\n' +
            '          <!-- second tab injected by applyLoginConfig() -->\n' +
            '        </ul>\n' +
            '        <div class="tab-content px-3 pt-3">\n' +
            '\n' +
            '          <!-- LOGIN TAB -->\n' +
            '          <div class="tab-pane fade show active" id="dbx-pane-login" role="tabpanel">\n' +
            '            <form class="form" role="form" autocomplete="off" id="dbx-login-form"\n' +
            '                  novalidate method="POST" action="j_security_check">\n' +
            '              <div class="form-group">\n' +
            '                <label for="dbx-login-user-txt">Email or username</label>\n' +
            '                <span class="fa fa-question-circle"\n' +
            '                      data-toggle="tooltip" data-placement="bottom" data-html="true"\n' +
            '                      title="The default &lt;code&gt;admin&lt;/code&gt; password is the IP address of the DbxCentral host.">&nbsp;</span>\n' +
            '                <input type="text" class="form-control" name="j_username"\n' +
            '                       id="dbx-login-user-txt" required>\n' +
            '                <div class="invalid-feedback">Oops, you missed this one.</div>\n' +
            '              </div>\n' +
            '              <div class="form-group">\n' +
            '                <label for="dbx-login-passwd-txt">Password</label>\n' +
            '                <input type="password" class="form-control" name="j_password"\n' +
            '                       id="dbx-login-passwd-txt" required autocomplete="new-password">\n' +
            '                <div class="invalid-feedback">Enter your password too!</div>\n' +
            '              </div>\n' +
            '              <div id="dbx-loginFailed-div" class="custom-control mb-2" style="display:none;">\n' +
            '                <b><font color="red">Login failed!</font></b>\n' +
            '              </div>\n' +
            '              <div class="form-group">\n' +
            '                <a href="" class="small" onclick="dbxToggleForgotPassword(); return false;">Forgot password?</a>\n' +
            '                <div id="dbx-forgotpw-div" style="display:none;" class="mt-2 p-2 border rounded bg-light">\n' +
            '                  <p class="mb-1 small">Enter your email address and we will send you a temporary password.</p>\n' +
            '                  <div class="input-group input-group-sm">\n' +
            '                    <input type="email" class="form-control" id="dbx-forgot-email-txt" placeholder="Email address">\n' +
            '                    <div class="input-group-append">\n' +
            '                      <button class="btn btn-success" type="button" onclick="dbxSendForgotPassword()">Send</button>\n' +
            '                    </div>\n' +
            '                  </div>\n' +
            '                  <div id="dbx-forgot-msg" class="mt-1 small"></div>\n' +
            '                </div>\n' +
            '              </div>\n' +
            '              <div class="modal-footer px-0">\n' +
            '                <button class="btn btn-outline-secondary" data-dismiss="modal" aria-hidden="true">Cancel</button>\n' +
            '                <button type="submit" class="btn btn-success float-right" id="dbx-login-btn">Login</button>\n' +
            '              </div>\n' +
            '            </form>\n' +
            '          </div>\n' +
            '\n' +
            '          <!-- second pane injected by applyLoginConfig() -->\n' +
            '\n' +
            '        </div>\n' +
            '      </div>\n' +
            '    </div>\n' +
            '  </div>\n' +
            '</div>\n';

        document.body.insertAdjacentHTML('beforeend', html);
    }

    /**
     * Injects the second modal tab based on server config.
     *
     * Tab/button label follows requireApproval (the user-facing question is
     * "will I get in immediately or wait for an admin?"):
     *   requireApproval=true  → "Request Access" / "Submit Request"
     *   requireApproval=false → "Create Account"
     *
     * Form fields follow oauthEnabled (no password when OAuth handles auth):
     *   oauthEnabled=true  → email + fullName + reason (no password)
     *   oauthEnabled=false → email + password + confirm (+ fullName/reason when requireApproval)
     */
    function applyLoginConfig(cfg)
    {
        var needsApproval = cfg.requireApproval;
        var tabLabel      = needsApproval ? 'Request Access' : 'Create Account';
        var btnLabel      = needsApproval ? 'Submit Request'  : 'Create Account';
        var approvalBanner = needsApproval
            ? '  <div class="alert alert-warning py-2 px-3 small mb-2" role="alert">' +
              '    <i class="fa fa-info-circle"></i>&nbsp;' +
              '    Admin approval required before you can log in.' +
              '  </div>\n'
            : '';

        var tabHtml, paneHtml;

        if (cfg.oauthEnabled)
        {
            // OAuth path — no password field; login is via the OAuth button
            tabHtml = '<li class="nav-item">' +
                      '  <a class="nav-link" id="dbx-tab-request" data-toggle="tab" href="#dbx-pane-request" role="tab">' + tabLabel + '</a>' +
                      '</li>';

            paneHtml =
                '<div class="tab-pane fade" id="dbx-pane-request" role="tabpanel">\n' +
                approvalBanner +
                '  <div class="form-group">\n' +
                '    <label for="dbx-req-email-txt">Email <span class="text-danger">*</span></label>\n' +
                '    <input type="email" class="form-control" id="dbx-req-email-txt" placeholder="your@email.com">\n' +
                '  </div>\n' +
                '  <div class="form-group">\n' +
                '    <label for="dbx-req-name-txt">Full Name</label>\n' +
                '    <input type="text" class="form-control" id="dbx-req-name-txt" placeholder="Your name (optional)">\n' +
                '  </div>\n' +
                (needsApproval
                    ? '  <div class="form-group">\n' +
                      '    <label for="dbx-req-reason-txt">Reason for Access</label>\n' +
                      '    <textarea class="form-control" id="dbx-req-reason-txt" rows="2" placeholder="Why do you need access? (optional)"></textarea>\n' +
                      '  </div>\n'
                    : '') +
                '  <div id="dbx-req-msg" class="mb-2"></div>\n' +
                '  <div class="modal-footer px-0">\n' +
                '    <button class="btn btn-outline-secondary" data-dismiss="modal" aria-hidden="true">Cancel</button>\n' +
                '    <button type="button" class="btn btn-primary float-right" onclick="dbxRequestAccess()">' + btnLabel + '</button>\n' +
                '  </div>\n' +
                '</div>\n';
        }
        else
        {
            // Password-based path
            tabHtml = '<li class="nav-item">' +
                      '  <a class="nav-link" id="dbx-tab-register" data-toggle="tab" href="#dbx-pane-register" role="tab">' + tabLabel + '</a>' +
                      '</li>';

            paneHtml =
                '<div class="tab-pane fade" id="dbx-pane-register" role="tabpanel">\n' +
                approvalBanner +
                '  <div class="form-group mb-2">\n' +
                '    <label class="mb-1" for="dbx-reg-email-txt">Email <span class="text-muted small">(used as your login)</span></label>\n' +
                '    <input type="email" class="form-control form-control-sm" id="dbx-reg-email-txt" placeholder="your@email.com">\n' +
                '  </div>\n' +
                '  <div class="form-group mb-2">\n' +
                '    <label class="mb-1" for="dbx-reg-password-txt">Password</label>\n' +
                '    <input type="password" class="form-control form-control-sm" id="dbx-reg-password-txt" placeholder="At least 8 characters">\n' +
                '    <div class="progress mt-1" style="height:4px;">\n' +
                '      <div id="dbx-reg-pw-strength-bar" class="progress-bar" role="progressbar" style="width:0%;transition:width 0.2s,background-color 0.2s;"></div>\n' +
                '    </div>\n' +
                '    <small id="dbx-reg-pw-strength-lbl" class="form-text text-muted"></small>\n' +
                '  </div>\n' +
                '  <div class="form-group mb-2">\n' +
                '    <label class="mb-1" for="dbx-reg-confirm-txt">Confirm Password</label>\n' +
                '    <input type="password" class="form-control form-control-sm" id="dbx-reg-confirm-txt" placeholder="Repeat password">\n' +
                '  </div>\n' +
                (needsApproval
                    ? '  <div class="form-group mb-2">\n' +
                      '    <label class="mb-1" for="dbx-reg-name-txt">Full Name</label>\n' +
                      '    <input type="text" class="form-control form-control-sm" id="dbx-reg-name-txt" placeholder="Your name (optional)">\n' +
                      '  </div>\n' +
                      '  <div class="form-group mb-2">\n' +
                      '    <label class="mb-1" for="dbx-reg-reason-txt">Reason for Access</label>\n' +
                      '    <textarea class="form-control form-control-sm" id="dbx-reg-reason-txt" rows="2" placeholder="Why do you need access? (optional)"></textarea>\n' +
                      '  </div>\n'
                    : '') +
                '  <div id="dbx-reg-msg" class="mb-2"></div>\n' +
                '  <div class="modal-footer px-0">\n' +
                '    <button class="btn btn-outline-secondary" data-dismiss="modal" aria-hidden="true">Cancel</button>\n' +
                '    <button type="button" class="btn btn-primary float-right" onclick="dbxRegisterUser()">' + btnLabel + '</button>\n' +
                '  </div>\n' +
                '</div>\n';
        }

        if (tabHtml)
            $('#dbx-login-tabs').append(tabHtml);
        if (paneHtml)
        {
            $('#dbx-login-dialog .tab-content').append(paneHtml);

            // Wire strength indicator + button enable/disable after pane exists in the DOM
            $(document).on('input', '#dbx-reg-password-txt', function ()
            {
                var pw    = $(this).val();
                var score = dbxPasswordStrength(pw);
                var bar   = $('#dbx-reg-pw-strength-bar');
                var lbl   = $('#dbx-reg-pw-strength-lbl');
                var configs = [
                    { pct: 0,   color: '',          text: '' },
                    { pct: 33,  color: '#dc3545',   text: 'Weak' },
                    { pct: 66,  color: '#fd7e14',   text: 'Fair' },
                    { pct: 100, color: '#28a745',   text: 'Strong' }
                ];
                var c = configs[score];
                bar.css({ width: c.pct + '%', 'background-color': c.color });
                lbl.text(c.text).css('color', c.color || '');
                // Disable Create Account button until minimum length is met
                $('#dbx-pane-register button.btn-primary').prop('disabled', pw.length < 8);
            });
        }
    }

    /** Returns 0 (empty) | 1 (weak) | 2 (fair) | 3 (strong). */
    function dbxPasswordStrength(pw)
    {
        if (!pw) return 0;
        var score = 0;
        if (pw.length >= 8)  score++;
        if (pw.length >= 12) score++;
        if (/[A-Z]/.test(pw) && /[a-z]/.test(pw)) score++;
        if (/[0-9]/.test(pw))  score++;
        if (/[^A-Za-z0-9]/.test(pw)) score++;
        // map raw score (0-5) to 3 levels
        if (score <= 1) return 1;
        if (score <= 3) return 2;
        return 3;
    }

    //--------------------------------------------------------------------------
    // 3. Wire up everything on DOM ready
    //--------------------------------------------------------------------------
    $(document).ready(function ()
    {
        injectNavbarLoginSection();
        injectLoginModal();
        injectSettingsModal();

        // Bootstrap form validation on submit
        $(document).on('submit', '#dbx-login-form', function (event)
        {
            var form = $(this);
            if (form[0].checkValidity() === false)
            {
                event.preventDefault();
                event.stopPropagation();
            }
            form.addClass('was-validated');
        });

        // Enter key in username or password field submits the login form
        $(document).on('keydown', '#dbx-login-user-txt, #dbx-login-passwd-txt', function (e)
        {
            if (e.key === 'Enter')
                $('#dbx-login-form').submit();
        });

        // Intercept clicks on whatever login link exists in the navbar
        // (works for both the injected link above AND existing links in index.html / HtmlStatic)
        $(document).on('click', '#dbx-nb-isLoggedOut-div a', function (e)
        {
            e.preventDefault();
            dbxOpenLogin();
        });

        // Bootstrap 4 sets aria-hidden="true" on the modal when closing, but if a field
        // inside still has focus the browser warns and the modal can get stuck.
        // Blur the focused element before Bootstrap applies aria-hidden.
        $(document).on('hide.bs.modal', '#dbx-login-dialog', function ()
        {
            if (document.activeElement && $.contains(this, document.activeElement))
                document.activeElement.blur();
        });

        // Fetch enabled OAuth providers and inject buttons into the login tab
        fetchAndInjectOAuthButtons();

        // Always call isLoggedIn() on every page load:
        //   - updates the navbar (logged-in dropdown vs Login link)
        //   - handles ?login URL parameter (Jetty-intercepted auth redirect)
        //   - handles post-voluntary-login redirect (Jetty sends to / with no ?login param)
        isLoggedIn(function (loggedIn, asUserName)
        {
            var hasLoginParam = (typeof isParameter === 'function') && isParameter('login');

            if (hasLoginParam)
            {
                if (!loggedIn)
                {
                    // Jetty sent us here with ?login — not logged in yet, show the modal
                    var returnUrl = (typeof getParameter === 'function') ? getParameter('returnUrl', '') : '';
                    if (!returnUrl && document.referrer)
                    {
                        try
                        {
                            var ref = new URL(document.referrer);
                            if (ref.origin === window.location.origin
                                    && ref.pathname !== '/index.html'
                                    && ref.pathname !== '/'
                                    && ref.pathname !== '/index.jsp')
                                returnUrl = ref.pathname + ref.search + ref.hash;
                        }
                        catch (e) {}
                    }
                    if (returnUrl)
                        sessionStorage.setItem('dbxReturnAfterLogin', returnUrl);
                    else
                        sessionStorage.removeItem('dbxReturnAfterLogin');

                    // Show login-failed banner if Jetty redirected here after a bad password
                    var loginParam = (typeof getParameter === 'function') ? getParameter('login', 'open') : 'open';
                    if (loginParam === 'failed')
                    {
                        $('#dbx-loginFailed-div').css('display', 'block');
                        // If the form is inside a collapsed <details> (OAuth mode), open it so
                        // the user can see the "Login Failed!" message and the login fields.
                        // fetchAndInjectOAuthButtons() is async, so hook shown.bs.modal instead.
                        $('#dbx-login-dialog').one('shown.bs.modal', function ()
                        {
                            $('#dbx-admin-login-fields').attr('open', true);
                        });
                    }

                    $('#dbx-login-dialog').modal({ backdrop: 'static', keyboard: false });
                    $('#dbx-login-dialog').modal('show');
                }
                else
                {
                    // ?login present but already logged in — send user back to where they came from
                    var returnUrl = sessionStorage.getItem('dbxReturnAfterLogin');
                    if (returnUrl)
                    {
                        console.log('dbxLoginModal: login successful (?login path), returning to: ' + returnUrl);
                        sessionStorage.removeItem('dbxReturnAfterLogin');
                        window.location.href = returnUrl;
                    }
                }
            }
            else
            {
                // No ?login param — check for pending return URL from a voluntary login
                // (Jetty redirects to / with no ?login when there was no saved request)
                var returnUrl = sessionStorage.getItem('dbxReturnAfterLogin');
                if (loggedIn && returnUrl && returnUrl !== window.location.href)
                {
                    console.log('dbxLoginModal: login successful (/ path), returning to: ' + returnUrl);
                    sessionStorage.removeItem('dbxReturnAfterLogin');
                    window.location.href = returnUrl;
                }
                else if (loggedIn)
                {
                    // Already on the right page — clear any stale return URL so it
                    // cannot cause spurious redirects on subsequent page navigations.
                    sessionStorage.removeItem('dbxReturnAfterLogin');
                }
            }
        });
    });

    //--------------------------------------------------------------------------
    // 4. OAuth provider buttons + login config
    //--------------------------------------------------------------------------

    /**
     * Fetches /api/login/config and /api/login/providers in parallel.
     * - Injects OAuth "Sign in with …" buttons into the login tab.
     * - Calls applyLoginConfig() to inject the correct second tab.
     */
    function fetchAndInjectOAuthButtons()
    {
        var configReq   = $.ajax({ url: '/api/login/config',    method: 'GET', dataType: 'json' });
        var providersReq = $.ajax({ url: '/api/login/providers', method: 'GET', dataType: 'json' });

        $.when(configReq, providersReq)
            .then(function (configResult, providersResult)
            {
                var cfg       = configResult[0]   || { mandatory: false, requireApproval: false, oauthEnabled: false };
                var providers = providersResult[0] || [];

                // Inject OAuth buttons into login tab
                if (providers.length > 0)
                {
                    // Build OAuth buttons
                    var html = '<div id="dbx-oauth-section" class="mt-1 mb-1">';
                    $.each(providers, function (i, p)
                    {
                        html +=
                            '<a href="' + p.startUrl + '" class="btn btn-outline-secondary btn-block mb-1" style="text-align:left;">' +
                            p.iconHtml +
                            'Sign in with ' + $('<span>').text(p.displayName).html() +
                            '</a>';
                    });
                    html += '</div>';
                    $(document).find('#dbx-pane-login form').prepend(html);

                    // Wrap username/password fields in a <details> so they collapse natively.
                    // wrapAll() with nested HTML doesn't work reliably — build the element manually.
                    var $form    = $(document).find('#dbx-login-form');
                    var $fields  = $form.children('.form-group, .custom-control, .modal-footer');
                    var $details = $('<details id="dbx-admin-login-fields" class="mt-2"></details>');
                    var $summary = $('<summary class="small text-muted mb-2" style="cursor:pointer;">Login Options</summary>');
                    $details.append($summary);
                    $fields.first().before($details);
                    $details.append($fields);
                }

                // Inject second tab (Create Account or Request Access) based on config
                applyLoginConfig(cfg);
            })
            .fail(function ()
            {
                // Config fetch failed — fall back to showing Create Account tab
                applyLoginConfig({ mandatory: false, requireApproval: false, oauthEnabled: false });
            });
    }

    //--------------------------------------------------------------------------
    // 5. Global API
    //--------------------------------------------------------------------------

    /** Open the login modal from anywhere (e.g. navbar Login link). */
    window.dbxOpenLogin = function ()
    {
        // Remember where to return after login
        sessionStorage.setItem('dbxReturnAfterLogin', window.location.href);

        // Reset modal state
        $('#dbx-loginFailed-div').css('display', 'none');
        $('#dbx-forgotpw-div').hide();
        $('#dbx-login-btn').prop('disabled', false);
        $('#dbx-login-user-txt, #dbx-login-passwd-txt').val('');
        $('#dbx-login-form').removeClass('was-validated');
        if ($('#dbx-tab-login').length)
            $('#dbx-tab-login').tab('show');

        $('#dbx-login-dialog').modal({ backdrop: 'static', keyboard: false });
        $('#dbx-login-dialog').modal('show');
    };

    /** Toggle the forgot-password panel inside the login modal. */
    window.dbxToggleForgotPassword = function ()
    {
        $('#dbx-forgotpw-div').slideToggle(200, function ()
        {
            var visible = $(this).is(':visible');
            $('#dbx-login-btn').prop('disabled', visible);
        });
    };

    /** Send a forgot-password email via the API. */
    window.dbxSendForgotPassword = function ()
    {
        var email = $('#dbx-forgot-email-txt').val().trim();
        if (!email)
        {
            $('#dbx-forgot-msg').html('<span class="text-danger">Please enter your email address.</span>');
            return;
        }
        $('#dbx-forgot-msg').html('<span class="text-muted">Sending...</span>');
        $.ajax({
            url: '/api/user/forgot-password',
            method: 'POST',
            data: { email: email },
            dataType: 'json',
            success: function (r)
            {
                var cls = r.success ? 'text-success' : 'text-danger';
                $('#dbx-forgot-msg').html('<span class="' + cls + '">' + r.message + '</span>');
                if (r.success)
                {
                    setTimeout(function ()
                    {
                        $('#dbx-forgotpw-div').slideUp(200);
                        $('#dbx-login-btn').prop('disabled', false);
                    }, 10000);
                }
            },
            error: function ()
            {
                $('#dbx-forgot-msg').html('<span class="text-danger">Request failed. Please try again later.</span>');
            }
        });
    };

    /** Request access (pending admin approval). */
    window.dbxRequestAccess = function ()
    {
        var email    = $('#dbx-req-email-txt').val().trim();
        var fullName = $('#dbx-req-name-txt').length    ? $('#dbx-req-name-txt').val().trim()   : '';
        var reason   = $('#dbx-req-reason-txt').length  ? $('#dbx-req-reason-txt').val().trim() : '';
        $('#dbx-req-msg').html('');
        if (!email || !email.includes('@'))
        {
            $('#dbx-req-msg').html('<span class="text-danger">Please enter a valid email address.</span>');
            return;
        }
        $.ajax({
            url: '/api/user/request-access',
            method: 'POST',
            data: { email: email, fullName: fullName, reason: reason },
            dataType: 'json',
            success: function (r)
            {
                var cls = r.success ? 'text-success' : 'text-danger';
                $('#dbx-req-msg').html('<span class="' + cls + '">' + r.message + '</span>');
                if (r.success)
                {
                    $('#dbx-req-email-txt').val('');
                    $('#dbx-req-name-txt').val('');
                    $('#dbx-req-reason-txt').val('');
                }
            },
            error: function (xhr)
            {
                var msg = 'Request failed. Please try again later.';
                try { var r = JSON.parse(xhr.responseText); if (r && r.message) msg = r.message; } catch (e) {}
                $('#dbx-req-msg').html('<span class="text-danger">' + msg + '</span>');
            }
        });
    };

    /** Self-registration via the API. Email is used as the login name. */
    window.dbxRegisterUser = function ()
    {
        var email    = $('#dbx-reg-email-txt').val().trim();
        var password = $('#dbx-reg-password-txt').val();
        var confirm  = $('#dbx-reg-confirm-txt').val();
        $('#dbx-reg-msg').html('');
        if (!email || !email.includes('@'))
        {
            $('#dbx-reg-msg').html('<span class="text-danger">Please enter a valid email address.</span>');
            return;
        }
        if (password.length < 8)
        {
            $('#dbx-reg-msg').html('<span class="text-danger">Password must be at least 8 characters.</span>');
            return;
        }
        if (password !== confirm)
        {
            $('#dbx-reg-msg').html('<span class="text-danger">Passwords do not match.</span>');
            return;
        }
        // Pick up optional fields present only in requireApproval mode
        var fullName = $('#dbx-reg-name-txt').val ? $('#dbx-reg-name-txt').val().trim() : '';
        var reason   = $('#dbx-reg-reason-txt').val ? $('#dbx-reg-reason-txt').val().trim() : '';

        $.ajax({
            url: '/api/user/register',
            method: 'POST',
            data: { email: email, password: password, confirmPassword: confirm, fullName: fullName, reason: reason },
            dataType: 'json',
            success: function (r)
            {
                if (r.success)
                {
                    $('#dbx-reg-msg').html('<span class="text-success">' + r.message + '</span>');
                    setTimeout(function () { $('#dbx-tab-login').tab('show'); }, 1500);
                }
                else
                {
                    $('#dbx-reg-msg').html('<span class="text-danger">' + r.message + '</span>');
                }
            },
            error: function ()
            {
                $('#dbx-reg-msg').html('<span class="text-danger">Registration failed. Please try again later.</span>');
            }
        });
    };

    //--------------------------------------------------------------------------
    // Settings modal — inject HTML + expose global functions
    //--------------------------------------------------------------------------
    function injectSettingsModal()
    {
        if (document.getElementById('dbx-settings-dialog'))
            return;

        var html =
            '<div class="modal fade" id="dbx-settings-dialog" tabindex="-1" role="dialog" aria-labelledby="dbx-settings-title">' +
            '  <div class="modal-dialog" role="document">' +
            '    <div class="modal-content">' +
            '      <div class="modal-header">' +
            '        <h5 class="modal-title" id="dbx-settings-title"><i class="fa fa-cog"></i> Account Settings</h5>' +
            '        <button type="button" class="close" data-dismiss="modal" aria-label="Close"><span aria-hidden="true">&times;</span></button>' +
            '      </div>' +
            '      <div class="modal-body">' +
            '        <div class="form-group">' +
            '          <label class="small font-weight-bold">Username</label>' +
            '          <input type="text" class="form-control form-control-sm" id="dbx-settings-username" readonly>' +
            '        </div>' +
            '        <hr>' +
            '        <h6>Change Email</h6>' +
            '        <div class="form-group mt-2">' +
            '          <input type="email" class="form-control form-control-sm" id="dbx-settings-email" placeholder="Email address">' +
            '        </div>' +
            '        <button class="btn btn-sm btn-primary" type="button" onclick="dbxSaveEmail()">Update Email</button>' +
            '        <div id="dbx-settings-email-msg" class="mt-1 small"></div>' +
            '        <hr>' +
            '        <h6>Change Full Name</h6>' +
            '        <div class="form-group mt-2">' +
            '          <input type="text" class="form-control form-control-sm" id="dbx-settings-fullname" placeholder="Display name (optional)">' +
            '        </div>' +
            '        <button class="btn btn-sm btn-primary" type="button" onclick="dbxSaveFullName()">Update Full Name</button>' +
            '        <div id="dbx-settings-fullname-msg" class="mt-1 small"></div>' +
            '        <hr>' +
            '        <h6>Change Password</h6>' +
            '        <div class="form-group mt-2">' +
            '          <input type="password" class="form-control form-control-sm" id="dbx-settings-current-pw" placeholder="Current password" autocomplete="current-password">' +
            '        </div>' +
            '        <div class="form-group">' +
            '          <input type="password" class="form-control form-control-sm" id="dbx-settings-new-pw" placeholder="New password (min 8 characters)" autocomplete="new-password">' +
            '          <div class="progress mt-1" style="height:4px;">' +
            '            <div id="dbx-settings-pw-strength-bar" class="progress-bar" role="progressbar" style="width:0%;transition:width 0.2s,background-color 0.2s;"></div>' +
            '          </div>' +
            '          <small id="dbx-settings-pw-strength-lbl" class="form-text text-muted"></small>' +
            '        </div>' +
            '        <div class="form-group">' +
            '          <input type="password" class="form-control form-control-sm" id="dbx-settings-confirm-pw" placeholder="Confirm new password" autocomplete="new-password">' +
            '        </div>' +
            '        <button class="btn btn-sm btn-primary" id="dbx-settings-pw-btn" type="button" onclick="dbxSavePassword()">Change Password</button>' +
            '        <div id="dbx-settings-pw-msg" class="mt-1 small"></div>' +
            '      </div>' +
            '      <div class="modal-footer">' +
            '        <button type="button" class="btn btn-secondary" data-dismiss="modal">Close</button>' +
            '      </div>' +
            '    </div>' +
            '  </div>' +
            '</div>';

        document.body.insertAdjacentHTML('beforeend', html);

        $(document).on('input', '#dbx-settings-new-pw', function ()
        {
            var pw      = $(this).val();
            var score   = dbxPasswordStrength(pw);
            var configs = [
                { pct: 0,   color: '',        text: '' },
                { pct: 33,  color: '#dc3545', text: 'Weak' },
                { pct: 66,  color: '#fd7e14', text: 'Fair' },
                { pct: 100, color: '#28a745', text: 'Strong' }
            ];
            var c = configs[score];
            $('#dbx-settings-pw-strength-bar').css({ width: c.pct + '%', 'background-color': c.color });
            $('#dbx-settings-pw-strength-lbl').text(c.text).css('color', c.color || '');
            $('#dbx-settings-pw-btn').prop('disabled', pw.length > 0 && pw.length < 8);
        });
    }

    function dbxPasswordStrength(pw)
    {
        if (!pw) return 0;
        var score = 0;
        if (pw.length >= 8)  score++;
        if (pw.length >= 12) score++;
        if (/[A-Z]/.test(pw) && /[a-z]/.test(pw)) score++;
        if (/[0-9]/.test(pw))  score++;
        if (/[^A-Za-z0-9]/.test(pw)) score++;
        if (score <= 1) return 1;
        if (score <= 3) return 2;
        return 3;
    }

    window.dbxPasswordStrength = dbxPasswordStrength;

    window.dbxOpenSettings = function ()
    {
        $('#dbx-settings-email-msg').html('');
        $('#dbx-settings-fullname-msg').html('');
        $('#dbx-settings-pw-msg').html('');
        $('#dbx-settings-current-pw, #dbx-settings-new-pw, #dbx-settings-confirm-pw').val('');
        $.ajax({
            url: '/api/user/settings?op=profile',
            method: 'GET',
            dataType: 'json',
            success: function (r)
            {
                $('#dbx-settings-username').val(r.username || '');
                $('#dbx-settings-email').val(r.email || '');
                $('#dbx-settings-fullname').val(r.fullName || '');
            }
        });
        $('#dbx-settings-dialog').modal('show');
    };

    window.dbxSaveEmail = function ()
    {
        var email = $('#dbx-settings-email').val().trim();
        if (!email || !email.includes('@'))
        {
            $('#dbx-settings-email-msg').html('<span class="text-danger">Please enter a valid email address.</span>');
            return;
        }
        $('#dbx-settings-email-msg').html('<span class="text-muted">Saving...</span>');
        $.ajax({
            url: '/api/user/settings',
            method: 'POST',
            data: { op: 'changeEmail', email: email },
            dataType: 'json',
            success: function (r)
            {
                var cls = r.success ? 'text-success' : 'text-danger';
                $('#dbx-settings-email-msg').html('<span class="' + cls + '">' + r.message + '</span>');
            },
            error: function () { $('#dbx-settings-email-msg').html('<span class="text-danger">Request failed.</span>'); }
        });
    };

    window.dbxSaveFullName = function ()
    {
        var fullName = $('#dbx-settings-fullname').val().trim();
        $('#dbx-settings-fullname-msg').html('<span class="text-muted">Saving...</span>');
        $.ajax({
            url: '/api/user/settings',
            method: 'POST',
            data: { op: 'changeFullName', fullName: fullName },
            dataType: 'json',
            success: function (r)
            {
                var cls = r.success ? 'text-success' : 'text-danger';
                $('#dbx-settings-fullname-msg').html('<span class="' + cls + '">' + r.message + '</span>');
            },
            error: function () { $('#dbx-settings-fullname-msg').html('<span class="text-danger">Request failed.</span>'); }
        });
    };

    window.dbxSavePassword = function ()
    {
        var cur = $('#dbx-settings-current-pw').val();
        var np  = $('#dbx-settings-new-pw').val();
        var cp  = $('#dbx-settings-confirm-pw').val();
        if (!cur)          { $('#dbx-settings-pw-msg').html('<span class="text-danger">Enter your current password.</span>'); return; }
        if (np.length < 8) { $('#dbx-settings-pw-msg').html('<span class="text-danger">New password must be at least 8 characters.</span>'); return; }
        if (np !== cp)     { $('#dbx-settings-pw-msg').html('<span class="text-danger">Passwords do not match.</span>'); return; }
        $('#dbx-settings-pw-msg').html('<span class="text-muted">Saving...</span>');
        $.ajax({
            url: '/api/user/settings',
            method: 'POST',
            data: { op: 'changePassword', currentPassword: cur, newPassword: np, confirmPassword: cp },
            dataType: 'json',
            success: function (r)
            {
                var cls = r.success ? 'text-success' : 'text-danger';
                $('#dbx-settings-pw-msg').html('<span class="' + cls + '">' + r.message + '</span>');
                if (r.success)
                    $('#dbx-settings-current-pw, #dbx-settings-new-pw, #dbx-settings-confirm-pw').val('');
            },
            error: function () { $('#dbx-settings-pw-msg').html('<span class="text-danger">Request failed.</span>'); }
        });
    };


})();
