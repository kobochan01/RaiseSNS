// views/signup.js — 会員登録画面
window.App = window.App || {};
App.views = App.views || {};

App.views.signup = (function () {
  const esc = App.utils.escapeHtml;

  function template() {
    return `
      <div class="auth-screen">
        <div class="auth-title">アカウント作成</div>
        <div class="auth-subtitle">RaiseSNSに会員登録</div>
        <form id="signup-form" novalidate>
          <div class="form-field">
            <label for="signup-username">ユーザー名（@username）</label>
            <input type="text" id="signup-username" placeholder="例: taro_dev" required />
            <div class="error-text" id="err-username"></div>
          </div>
          <div class="form-field">
            <label for="signup-display-name">表示名</label>
            <input type="text" id="signup-display-name" placeholder="例: 太郎" required />
            <div class="error-text" id="err-displayName"></div>
          </div>
          <div class="form-field">
            <label for="signup-email">メールアドレス</label>
            <input type="email" id="signup-email" autocomplete="email" required />
            <div class="error-text" id="err-email"></div>
          </div>
          <div class="form-field">
            <label for="signup-password">パスワード（8文字以上）</label>
            <input type="password" id="signup-password" autocomplete="new-password" required />
            <div class="error-text" id="err-password"></div>
          </div>
          <button type="submit" class="btn btn--primary btn--full">登録する</button>
        </form>
        <div class="auth-footer">既にアカウントをお持ちの方は <a href="#/login">ログイン</a></div>
      </div>
    `;
  }

  function clearErrors(container) {
    ["username", "displayName", "email", "password"].forEach((key) => {
      container.querySelector(`#err-${key}`).textContent = "";
    });
  }

  function render(container) {
    container.innerHTML = template();

    const form = container.querySelector("#signup-form");

    form.addEventListener("submit", (e) => {
      e.preventDefault();
      clearErrors(container);

      const username = container.querySelector("#signup-username").value.trim();
      const displayName = container.querySelector("#signup-display-name").value.trim();
      const email = container.querySelector("#signup-email").value.trim();
      const password = container.querySelector("#signup-password").value;

      const result = App.auth.signup({ username, email, password, displayName });
      if (!result.ok) {
        Object.entries(result.errors).forEach(([key, message]) => {
          const el = container.querySelector(`#err-${key}`);
          if (el) el.textContent = esc(message);
        });
        return;
      }
      App.router.navigate("/timeline");
    });
  }

  return { render };
})();
