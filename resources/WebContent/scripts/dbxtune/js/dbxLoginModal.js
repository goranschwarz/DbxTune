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

        var html =
            '<!-- ============================================================ -->\n' +
            '<!-- Login Modal (Bootstrap 4) — injected by dbxLoginModal.js     -->\n' +
            '<!-- ============================================================ -->\n' +
            '<div id="dbx-login-dialog" class="modal fade" tabindex="-1" role="dialog" aria-hidden="true">\n' +
            '  <div class="modal-dialog modal-dialog-centered">\n' +
            '    <div class="modal-content">\n' +
            '      <div class="modal-header">\n' +
            '        <h3>DbxCentral - Login</h3>\n' +
            '        <button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>\n' +
            '      </div>\n' +
            '      <div class="modal-body p-0">\n' +
            '        <ul class="nav nav-tabs px-3 pt-2" id="dbx-login-tabs" role="tablist">\n' +
            '          <li class="nav-item">\n' +
            '            <a class="nav-link active" id="dbx-tab-login" data-toggle="tab" href="#dbx-pane-login" role="tab">Login</a>\n' +
            '          </li>\n' +
            '          <li class="nav-item">\n' +
            '            <a class="nav-link" id="dbx-tab-register" data-toggle="tab" href="#dbx-pane-register" role="tab">Create Account</a>\n' +
            '          </li>\n' +
            '        </ul>\n' +
            '        <div class="tab-content px-3 pt-3">\n' +
            '\n' +
            '          <!-- LOGIN TAB -->\n' +
            '          <div class="tab-pane fade show active" id="dbx-pane-login" role="tabpanel">\n' +
            '            <form class="form" role="form" autocomplete="off" id="dbx-login-form"\n' +
            '                  novalidate method="POST" action="j_security_check">\n' +
            '              <div class="form-group">\n' +
            '                <label for="dbx-login-user-txt">Username</label>\n' +
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
            '              <!-- Hidden submit enables Enter-key validation -->\n' +
            '              <input type="submit" hidden />\n' +
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
            '          <!-- CREATE ACCOUNT TAB -->\n' +
            '          <div class="tab-pane fade" id="dbx-pane-register" role="tabpanel">\n' +
            '            <div class="form-group">\n' +
            '              <label for="dbx-reg-username-txt">Username</label>\n' +
            '              <input type="text" class="form-control" id="dbx-reg-username-txt"\n' +
            '                     maxlength="128" placeholder="Choose a username">\n' +
            '            </div>\n' +
            '            <div class="form-group">\n' +
            '              <label for="dbx-reg-email-txt">Email</label>\n' +
            '              <input type="email" class="form-control" id="dbx-reg-email-txt" placeholder="your@email.com">\n' +
            '            </div>\n' +
            '            <div class="form-group">\n' +
            '              <label for="dbx-reg-password-txt">Password</label>\n' +
            '              <input type="password" class="form-control" id="dbx-reg-password-txt" placeholder="At least 6 characters">\n' +
            '            </div>\n' +
            '            <div class="form-group">\n' +
            '              <label for="dbx-reg-confirm-txt">Confirm Password</label>\n' +
            '              <input type="password" class="form-control" id="dbx-reg-confirm-txt" placeholder="Repeat password">\n' +
            '            </div>\n' +
            '            <div id="dbx-reg-msg" class="mb-2"></div>\n' +
            '            <div class="modal-footer px-0">\n' +
            '              <button class="btn btn-outline-secondary" data-dismiss="modal" aria-hidden="true">Cancel</button>\n' +
            '              <button type="button" class="btn btn-primary float-right" onclick="dbxRegisterUser()">Create Account</button>\n' +
            '            </div>\n' +
            '          </div>\n' +
            '\n' +
            '        </div>\n' +
            '      </div>\n' +
            '    </div>\n' +
            '  </div>\n' +
            '</div>\n';

        document.body.insertAdjacentHTML('beforeend', html);
    }

    //--------------------------------------------------------------------------
    // 3. Wire up everything on DOM ready
    //--------------------------------------------------------------------------
    $(document).ready(function ()
    {
        injectNavbarLoginSection();
        injectLoginModal();

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

        // Intercept clicks on whatever login link exists in the navbar
        // (works for both the injected link above AND existing links in index.html / HtmlStatic)
        $(document).on('click', '#dbx-nb-isLoggedOut-div a', function (e)
        {
            e.preventDefault();
            dbxOpenLogin();
        });

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
                        $('#dbx-loginFailed-div').css('display', 'block');

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
                // Note: we intentionally do NOT clear sessionStorage.dbxReturnAfterLogin
                // when not logged in here — dbxOpenLogin() may have just set it (race condition).
            }
        });
    });

    //--------------------------------------------------------------------------
    // 4. Global API
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

    /** Self-registration via the API. */
    window.dbxRegisterUser = function ()
    {
        var username = $('#dbx-reg-username-txt').val().trim();
        var email    = $('#dbx-reg-email-txt').val().trim();
        var password = $('#dbx-reg-password-txt').val();
        var confirm  = $('#dbx-reg-confirm-txt').val();
        $('#dbx-reg-msg').html('');
        $.ajax({
            url: '/api/user/register',
            method: 'POST',
            data: { username: username, email: email, password: password, confirmPassword: confirm },
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

})();
