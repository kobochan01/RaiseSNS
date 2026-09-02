// auth.js — ログイン/会員登録/ログアウト、セッション管理
window.App = window.App || {};

App.auth = (function () {
  const SESSION_KEY = "raisesns_session";
  const u = App.utils;

  function currentUser() {
    const raw = localStorage.getItem(SESSION_KEY);
    if (!raw) return null;
    try {
      const { userId } = JSON.parse(raw);
      return App.data.getUserById(userId);
    } catch (e) {
      return null;
    }
  }

  function setSession(userId) {
    localStorage.setItem(SESSION_KEY, JSON.stringify({ userId }));
  }

  function login(email, password) {
    if (!email || !password) {
      return { ok: false, error: "メールアドレスとパスワードを入力してください。" };
    }
    const user = App.data.authenticateUser(email, password);
    if (!user) {
      return { ok: false, error: "メールアドレスまたはパスワードが正しくありません。" };
    }
    setSession(user.id);
    return { ok: true, user };
  }

  function logout() {
    localStorage.removeItem(SESSION_KEY);
  }

  function signup({ username, email, password, displayName }) {
    const errors = {};
    if (!u.isValidUsername(username)) {
      errors.username = "ユーザー名は3〜30文字の英数字とアンダースコアのみ使用できます。";
    } else if (App.data.isUsernameTaken(username)) {
      errors.username = "このユーザー名は既に使用されています。";
    }
    if (!u.isValidEmail(email)) {
      errors.email = "メールアドレスの形式が正しくありません。";
    } else if (App.data.isEmailTaken(email)) {
      errors.email = "このメールアドレスは既に登録されています。";
    }
    if (!password || password.length < 8) {
      errors.password = "パスワードは8文字以上で入力してください。";
    }
    if (!u.inRange(displayName, 1, 50)) {
      errors.displayName = "表示名は1〜50文字で入力してください。";
    }
    if (Object.keys(errors).length) {
      return { ok: false, errors };
    }
    const user = App.data.createUser({ username, email, password, displayName });
    setSession(user.id);
    return { ok: true, user };
  }

  return { currentUser, login, logout, signup };
})();
