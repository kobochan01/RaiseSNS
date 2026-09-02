// views/timeline.js — タイムライン画面（全体／フォロー中タブ）
window.App = window.App || {};
App.views = App.views || {};

App.views.timeline = (function () {
  const u = App.utils;

  function template(activeTab) {
    return `
      <div class="tabs">
        <button type="button" class="tab ${activeTab === "all" ? "is-active" : ""}" data-tab="all">全体</button>
        <button type="button" class="tab ${activeTab === "following" ? "is-active" : ""}" data-tab="following">フォロー中</button>
      </div>
      <div class="timeline-toolbar">
        <button type="button" class="btn btn--primary" id="open-post-create">＋ 投稿</button>
      </div>
      <div id="post-list"></div>
    `;
  }

  function renderList(listEl, posts) {
    if (!posts.length) {
      listEl.innerHTML = `<div class="empty-state">まだ投稿がありません。「フォロー中」タブの場合は、ユーザーをフォローすると表示されます。</div>`;
      return;
    }
    listEl.innerHTML = posts.map(u.postCardHtml).join("");
  }

  function render(container, { query, currentUser }) {
    const activeTab = query.tab === "following" ? "following" : "all";
    container.innerHTML = template(activeTab);

    const listEl = container.querySelector("#post-list");

    function reload() {
      const posts = App.data.getTimelinePosts({ scope: activeTab, currentUserId: currentUser.id });
      renderList(listEl, posts);
    }
    reload();

    container.querySelectorAll(".tab").forEach((btn) => {
      btn.addEventListener("click", () => {
        App.router.navigate(u.buildHash("/timeline", { tab: btn.dataset.tab }));
      });
    });

    container.querySelector("#open-post-create").addEventListener("click", () => {
      App.modals.postCreate.open({ onCreated: reload });
    });

    listEl.addEventListener("click", (e) => {
      const target = e.target.closest("[data-action]");
      if (!target) return;
      const action = target.dataset.action;
      if (action === "open-profile") {
        e.stopPropagation();
        App.router.navigate("/profile/" + target.dataset.username);
      } else if (action === "open-post") {
        App.modals.postDetail.open(target.dataset.postId, { onChange: reload });
      } else if (action === "toggle-like") {
        e.stopPropagation();
        App.data.toggleLike(target.dataset.postId, currentUser.id);
        reload();
      }
    });
  }

  return { render };
})();
