// views/search.js — ユーザー検索画面
window.App = window.App || {};
App.views = App.views || {};

App.views.search = (function () {
  const u = App.utils;
  const esc = u.escapeHtml;

  function userRowHtml(user) {
    return `
      <div class="user-row" data-username="${esc(user.username)}">
        ${u.avatarHtml(user, "md")}
        <div class="user-row__info">
          <div class="user-row__name">${esc(user.displayName)}</div>
          <div class="user-row__username">@${esc(user.username)}</div>
          ${user.bio ? `<div class="user-row__bio">${esc(user.bio)}</div>` : ""}
        </div>
      </div>
    `;
  }

  function render(container, { query, currentUser }) {
    const q = (query.q || "").trim();

    const searchInput = document.getElementById("search-input");
    if (searchInput) searchInput.value = q;

    if (!q) {
      container.innerHTML = `<div class="empty-state">ユーザー名（@username）を入力して検索してください。</div>`;
      return;
    }

    const results = App.data.searchUsers(q);

    container.innerHTML = `
      <div class="search-result-count">「${esc(q)}」の検索結果（${results.length}件）</div>
      <div id="search-result-list">
        ${results.length ? results.map(userRowHtml).join("") : `<div class="empty-state">該当するユーザーが見つかりませんでした。</div>`}
      </div>
    `;

    const listEl = container.querySelector("#search-result-list");
    listEl.addEventListener("click", (e) => {
      const row = e.target.closest(".user-row");
      if (!row) return;
      App.router.navigate("/profile/" + row.dataset.username);
    });
  }

  return { render };
})();
