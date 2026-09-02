// router.js — hashベースの簡易ルーター、認証ガード、共通ヘッダー制御
window.App = window.App || {};

App.router = (function () {
  const u = App.utils;

  const routes = [
    { pattern: /^\/login$/, view: "login", guard: "guest" },
    { pattern: /^\/signup$/, view: "signup", guard: "guest" },
    { pattern: /^\/timeline$/, view: "timeline", guard: "auth" },
    { pattern: /^\/profile\/([^/]+)\/edit$/, view: "profileEdit", guard: "auth", paramNames: ["username"] },
    { pattern: /^\/profile\/([^/]+)\/connections$/, view: "followList", guard: "auth", paramNames: ["username"] },
    { pattern: /^\/profile\/([^/]+)$/, view: "profile", guard: "auth", paramNames: ["username"] },
    { pattern: /^\/search$/, view: "search", guard: "auth" },
  ];

  function match(path) {
    for (const route of routes) {
      const m = path.match(route.pattern);
      if (m) {
        const params = {};
        (route.paramNames || []).forEach((name, i) => {
          params[name] = decodeURIComponent(m[i + 1]);
        });
        return { route, params };
      }
    }
    return null;
  }

  function navigate(path) {
    const target = path.startsWith("#") ? path : "#" + path;
    if (location.hash === target) {
      render();
    } else {
      location.hash = target;
    }
  }

  function updateHeader(user, isGuestScreen) {
    const header = document.getElementById("app-header");
    header.classList.toggle("is-hidden", !!isGuestScreen);
    const nameEl = document.getElementById("header-username");
    if (nameEl) {
      nameEl.textContent = user ? "@" + user.username : "";
      nameEl.href = user ? "#/profile/" + user.username : "#/timeline";
    }
  }

  function render() {
    const { path, query } = u.parseHash(location.hash);
    const user = App.auth.currentUser();

    if (path === "/" || path === "") {
      navigate(user ? "/timeline" : "/login");
      return;
    }

    const matched = match(path);
    if (!matched) {
      navigate(user ? "/timeline" : "/login");
      return;
    }

    const { route, params } = matched;

    if (route.guard === "auth" && !user) {
      navigate("/login");
      return;
    }
    if (route.guard === "guest" && user) {
      navigate("/timeline");
      return;
    }

    updateHeader(user, route.guard === "guest");

    const container = document.getElementById("view-root");
    const viewModule = App.views[route.view];
    if (!viewModule || typeof viewModule.render !== "function") {
      container.innerHTML = `<div class="empty-state">画面の読み込みに失敗しました。</div>`;
      return;
    }
    viewModule.render(container, { params, query, currentUser: user });
    window.scrollTo(0, 0);
  }

  function init() {
    window.addEventListener("hashchange", render);

    document.getElementById("logout-btn").addEventListener("click", () => {
      App.auth.logout();
      navigate("/login");
    });

    document.getElementById("search-form").addEventListener("submit", (e) => {
      e.preventDefault();
      const q = document.getElementById("search-input").value.trim();
      navigate(u.buildHash("/search", { q }));
    });

    render();
  }

  return { init, navigate };
})();
