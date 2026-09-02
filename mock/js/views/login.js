// views/login.js — ログイン画面
window.App = window.App || {};
App.views = App.views || {};

App.views.login = (function () {
  const esc = App.utils.escapeHtml;

  function template() {
    return `
      <div class="auth-screen">
        <div class="auth-title">おかえりなさい</div>
        <div class="auth-subtitle">RaiseSNSにログイン</div>
        <div class="auth-hint">プロトタイプ用ヒント：どのユーザーもパスワードは <strong>password123</strong> です（例: taro@example.com）</div>
        <div id="login-error-slot"></div>
        <form id="login-form" novalidate>
          <div class="form-field">
            <label for="login-email">メールアドレス</label>
            <input type="email" id="login-email" autocomplete="email" required />
          </div>
          <div class="form-field">
            <label for="login-password">パスワード</label>
            <input type="password" id="login-password" autocomplete="current-password" required />
          </div>
          <button type="submit" class="btn btn--primary btn--full">ログイン</button>
        </form>
        <div class="auth-footer">アカウントをお持ちでない方は <a href="#/signup">会員登録</a></div>
      </div>
    `;
  }

  function render(container) {
    container.innerHTML = template();

    const form = container.querySelector("#login-form");
    const errorSlot = container.querySelector("#login-error-slot");

    form.addEventListener("submit", (e) => {
      e.preventDefault();
      const email = container.querySelector("#login-email").value.trim();
      const password = container.querySelector("#login-password").value;

      const result = App.auth.login(email, password);
      if (!result.ok) {
        errorSlot.innerHTML = `<div class="form-error">${esc(result.error)}</div>`;
        return;
      }
      App.router.navigate("/timeline");
    });
  }

  return { render };
})();
