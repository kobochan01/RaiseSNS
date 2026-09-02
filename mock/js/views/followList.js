// views/followList.js — フォロー・フォロワー一覧画面
window.App = window.App || {};
App.views = App.views || {};

App.views.followList = (function () {
  const u = App.utils;
  const esc = u.escapeHtml;

  function userRowHtml(row) {
    return `
      <div class="user-row" data-username="${esc(row.username)}">
        ${u.avatarHtml(row, "md")}
        <div class="user-row__info">
          <div class="user-row__name">${esc(row.displayName)}</div>
          <div class="user-row__username">@${esc(row.username)}</div>
        </div>
      </div>
    `;
  }

  function template(target, activeTab) {
    return `
      <a href="#/profile/${esc(target.username)}" class="profile-back">← 戻る</a>
      <div class="tabs">
        <button type="button" class="tab ${activeTab === "following" ? "is-active" : ""}" data-tab="following">フォロー中</button>
        <button type="button" class="tab ${activeTab === "followers" ? "is-active" : ""}" data-tab="followers">フォロワー</button>
      </div>
      <div id="user-list"></div>
    `;
  }

  function render(container, { params, query, currentUser }) {
    const target = App.data.getUserByUsername(params.username);
    if (!target) {
      container.innerHTML = `<a href="#/timeline" class="profile-back">← 戻る</a><div class="empty-state">ユーザーが見つかりませんでした。</div>`;
      return;
    }

    const activeTab = query.tab === "followers" ? "followers" : "following";
    container.innerHTML = template(target, activeTab);

    const listEl = container.querySelector("#user-list");
    const rows = activeTab === "following"
      ? App.data.getFollowingList(target.id, currentUser.id)
      : App.data.getFollowersList(target.id, currentUser.id);

    listEl.innerHTML = rows.length
      ? rows.map(userRowHtml).join("")
      : `<div class="empty-state">${activeTab === "following" ? "フォロー中のユーザーはいません。" : "フォロワーはいません。"}</div>`;

    container.querySelectorAll(".tab").forEach((btn) => {
      btn.addEventListener("click", () => {
        App.router.navigate(u.buildHash(`/profile/${target.username}/connections`, { tab: btn.dataset.tab }));
      });
    });

    listEl.addEventListener("click", (e) => {
      const row = e.target.closest(".user-row");
      if (!row) return;
      App.router.navigate("/profile/" + row.dataset.username);
    });
  }

  return { render };
})();
