// views/profile.js — プロフィール画面
window.App = window.App || {};
App.views = App.views || {};

App.views.profile = (function () {
  const u = App.utils;
  const esc = u.escapeHtml;

  function template(target, currentUser, counts, isFollowedByMe) {
    const isSelf = currentUser.username === target.username;
    const actionHtml = isSelf
      ? `<button type="button" class="btn btn--outline btn--sm" id="edit-profile-btn">編集</button>`
      : `<button type="button" class="btn ${isFollowedByMe ? "btn--outline" : "btn--primary"} btn--sm" id="follow-btn">${isFollowedByMe ? "フォロー中" : "フォロー"}</button>`;

    return `
      <a href="#/timeline" class="profile-back">← 戻る</a>
      <div class="profile-header">
        <div class="profile-top">
          ${u.avatarHtml(target, "lg")}
          ${actionHtml}
        </div>
        <div class="profile-name">${esc(target.displayName)}</div>
        <div class="profile-username">@${esc(target.username)}</div>
        ${target.bio ? `<div class="profile-bio">${esc(target.bio)}</div>` : ""}
        <div class="profile-stats">
          <a href="#" id="link-following">フォロー中 <strong>${counts.followingCount}</strong></a>
          <a href="#" id="link-followers">フォロワー <strong>${counts.followerCount}</strong></a>
        </div>
      </div>
      <div class="profile-posts-title">投稿</div>
      <div id="profile-post-list"></div>
    `;
  }

  function renderPostList(listEl, posts) {
    if (!posts.length) {
      listEl.innerHTML = `<div class="empty-state">まだ投稿がありません。</div>`;
      return;
    }
    listEl.innerHTML = posts.map(u.postCardHtml).join("");
  }

  function render(container, { params, currentUser }) {
    const target = App.data.getUserByUsername(params.username);
    if (!target) {
      container.innerHTML = `<a href="#/timeline" class="profile-back">← 戻る</a><div class="empty-state">ユーザーが見つかりませんでした。</div>`;
      return;
    }

    const counts = App.data.getFollowCounts(target.id);
    const isFollowedByMe = App.data.isFollowing(currentUser.id, target.id);
    container.innerHTML = template(target, currentUser, counts, isFollowedByMe);

    const listEl = container.querySelector("#profile-post-list");
    function reloadPosts() {
      renderPostList(listEl, App.data.getPostsByUser(target.id, currentUser.id));
    }
    reloadPosts();

    const editBtn = container.querySelector("#edit-profile-btn");
    if (editBtn) {
      editBtn.addEventListener("click", () => App.router.navigate(`/profile/${target.username}/edit`));
    }

    const followBtn = container.querySelector("#follow-btn");
    if (followBtn) {
      followBtn.addEventListener("click", () => {
        if (App.data.isFollowing(currentUser.id, target.id)) {
          App.data.unfollowUser(currentUser.id, target.id);
        } else {
          App.data.followUser(currentUser.id, target.id);
        }
        render(container, { params, currentUser });
      });
    }

    container.querySelector("#link-following").addEventListener("click", (e) => {
      e.preventDefault();
      App.router.navigate(u.buildHash(`/profile/${target.username}/connections`, { tab: "following" }));
    });
    container.querySelector("#link-followers").addEventListener("click", (e) => {
      e.preventDefault();
      App.router.navigate(u.buildHash(`/profile/${target.username}/connections`, { tab: "followers" }));
    });

    listEl.addEventListener("click", (e) => {
      const el = e.target.closest("[data-action]");
      if (!el) return;
      const action = el.dataset.action;
      if (action === "open-profile") {
        e.stopPropagation();
        App.router.navigate("/profile/" + el.dataset.username);
      } else if (action === "open-post") {
        App.modals.postDetail.open(el.dataset.postId, { onChange: reloadPosts });
      } else if (action === "toggle-like") {
        e.stopPropagation();
        App.data.toggleLike(el.dataset.postId, currentUser.id);
        reloadPosts();
      }
    });
  }

  return { render };
})();
