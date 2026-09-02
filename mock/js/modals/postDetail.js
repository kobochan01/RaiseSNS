// modals/postDetail.js — 投稿詳細モーダル（いいね・コメントCRUD・投稿編集削除）
window.App = window.App || {};
App.modals = App.modals || {};

App.modals.postDetail = (function () {
  const u = App.utils;
  const esc = u.escapeHtml;

  let currentPostId = null;
  let onChangeCallback = null;

  function commentHtml(comment, currentUser) {
    const isOwner = currentUser && comment.userId === currentUser.id;
    return `
      <div class="comment-item" data-comment-id="${comment.id}">
        ${u.avatarHtml(comment.author, "sm")}
        <div class="comment-item__body">
          <div class="comment-item__head">
            <span class="comment-item__name" data-action="open-profile" data-username="${esc(comment.author.username)}">${esc(comment.author.displayName)}</span>
            <span class="comment-item__username" data-action="open-profile" data-username="${esc(comment.author.username)}">@${esc(comment.author.username)}</span>
            <span class="comment-item__time">・${u.formatRelativeTime(comment.createdAt)}</span>
          </div>
          <div class="comment-item__text-slot">
            <p class="comment-item__text">${esc(comment.body)}</p>
          </div>
          ${isOwner ? `
          <div class="comment-item__actions">
            <button type="button" class="btn-link" data-action="edit-comment">編集</button>
            <button type="button" class="btn-link" data-action="delete-comment">削除</button>
          </div>` : ""}
        </div>
      </div>
    `;
  }

  function template(post, comments, currentUser) {
    const isOwner = currentUser && post.userId === currentUser.id;
    const imageHtml = post.imageUrl ? `<img class="post-detail__image" src="${post.imageUrl}" alt="投稿画像" />` : "";
    return `
      <div class="modal-header">
        <h2>投稿の詳細</h2>
        <button type="button" class="modal-close" data-action="close" aria-label="閉じる">×</button>
      </div>
      <div class="modal-body">
        <div class="post-detail__head">
          ${u.avatarHtml(post.author, "md")}
          <div class="post-detail__meta">
            <div class="post-card__head">
              <span class="post-card__name" data-action="open-profile" data-username="${esc(post.author.username)}">${esc(post.author.displayName)}</span>
              <span class="post-card__username" data-action="open-profile" data-username="${esc(post.author.username)}">@${esc(post.author.username)}</span>
              <span class="post-card__time">・${u.formatRelativeTime(post.createdAt)}</span>
            </div>
          </div>
        </div>
        <div id="post-body-slot">
          <p class="post-detail__text">${esc(post.body)}</p>
        </div>
        ${imageHtml}
        <div class="post-detail__footer">
          <button type="button" class="action-btn like-btn ${post.isLikedByMe ? "is-liked" : ""}" data-action="toggle-like">
            <span class="icon">${post.isLikedByMe ? "♥" : "♡"}</span><span>${post.likeCount}</span>
          </button>
          ${isOwner ? `
          <div class="post-detail__owner-actions">
            <button type="button" class="btn-link" data-action="edit-post">編集</button>
            <button type="button" class="btn-link" data-action="delete-post">削除</button>
          </div>` : "<span></span>"}
        </div>
        <div class="comment-section">
          <h3>コメント（${comments.length}）</h3>
          <div id="comment-list">
            ${comments.length ? comments.map((c) => commentHtml(c, currentUser)).join("") : '<div class="empty-state">まだコメントがありません。</div>'}
          </div>
          <form id="comment-form" class="comment-form">
            <textarea id="comment-input" placeholder="コメントを入力...（1〜140文字）" maxlength="140" rows="1"></textarea>
            <button type="submit" class="btn btn--primary btn--sm">送信</button>
          </form>
        </div>
      </div>
    `;
  }

  function goProfile(username) {
    App.modal.close();
    App.router.navigate("/profile/" + username);
  }

  function build() {
    const currentUser = App.auth.currentUser();
    const post = App.data.getEnrichedPost(currentPostId, currentUser ? currentUser.id : null);
    if (!post) {
      App.modal.close();
      return;
    }
    const comments = App.data.getCommentsByPost(currentPostId);
    App.modal.open(template(post, comments, currentUser), {
      onClose: () => {
        if (onChangeCallback) onChangeCallback();
      },
    });
    attachHandlers(post, currentUser);
  }

  function attachHandlers(post, currentUser) {
    const root = App.modal.getBody();

    root.querySelector('[data-action="close"]').addEventListener("click", () => App.modal.close());

    root.querySelectorAll('[data-action="open-profile"]').forEach((el) => {
      el.addEventListener("click", () => goProfile(el.dataset.username));
    });

    root.querySelector('[data-action="toggle-like"]').addEventListener("click", () => {
      App.data.toggleLike(post.id, currentUser.id);
      build();
    });

    const editPostBtn = root.querySelector('[data-action="edit-post"]');
    if (editPostBtn) editPostBtn.addEventListener("click", () => startEditPost(post));

    const deletePostBtn = root.querySelector('[data-action="delete-post"]');
    if (deletePostBtn) {
      deletePostBtn.addEventListener("click", () => {
        if (confirm("この投稿を削除しますか？この操作は取り消せません。")) {
          App.data.deletePost(post.id);
          App.modal.close();
        }
      });
    }

    root.querySelectorAll('[data-action="edit-comment"]').forEach((btn) => {
      const item = btn.closest(".comment-item");
      btn.addEventListener("click", () => startEditComment(item.dataset.commentId));
    });

    root.querySelectorAll('[data-action="delete-comment"]').forEach((btn) => {
      const item = btn.closest(".comment-item");
      btn.addEventListener("click", () => {
        if (confirm("このコメントを削除しますか？")) {
          App.data.deleteComment(item.dataset.commentId);
          build();
        }
      });
    });

    const commentForm = root.querySelector("#comment-form");
    commentForm.addEventListener("submit", (e) => {
      e.preventDefault();
      const textarea = root.querySelector("#comment-input");
      const body = textarea.value.trim();
      if (!body || body.length > 140) return;
      App.data.createComment({ postId: post.id, userId: currentUser.id, body });
      build();
    });
  }

  function startEditPost(post) {
    const root = App.modal.getBody();
    const slot = root.querySelector("#post-body-slot");
    slot.innerHTML = `
      <textarea id="edit-post-textarea" class="post-create__textarea" maxlength="280">${esc(post.body)}</textarea>
      <div class="field-meta"><span></span><span class="char-counter" id="edit-char-counter"></span></div>
      <div class="edit-actions">
        <button type="button" class="btn btn--ghost btn--sm" id="cancel-edit-post">キャンセル</button>
        <button type="button" class="btn btn--primary btn--sm" id="save-edit-post">保存</button>
      </div>
    `;
    const ta = slot.querySelector("#edit-post-textarea");
    const counter = slot.querySelector("#edit-char-counter");
    function updateCounter() {
      counter.textContent = `${ta.value.length} / 280`;
      counter.classList.toggle("is-over", ta.value.length > 280);
    }
    ta.addEventListener("input", updateCounter);
    updateCounter();
    slot.querySelector("#cancel-edit-post").addEventListener("click", () => build());
    slot.querySelector("#save-edit-post").addEventListener("click", () => {
      const body = ta.value.trim();
      if (!body || body.length > 280) return;
      App.data.updatePost(post.id, body);
      build();
    });
  }

  function startEditComment(commentId) {
    const root = App.modal.getBody();
    const item = root.querySelector(`.comment-item[data-comment-id="${commentId}"]`);
    const slot = item.querySelector(".comment-item__text-slot");
    const comment = App.data.getCommentById(commentId);
    slot.innerHTML = `
      <textarea class="comment-edit-textarea" maxlength="140">${esc(comment.body)}</textarea>
      <div class="edit-actions">
        <button type="button" class="btn-link" id="cancel-edit-comment">キャンセル</button>
        <button type="button" class="btn-link" id="save-edit-comment">保存</button>
      </div>
    `;
    slot.querySelector("#cancel-edit-comment").addEventListener("click", () => build());
    slot.querySelector("#save-edit-comment").addEventListener("click", () => {
      const ta = slot.querySelector(".comment-edit-textarea");
      const body = ta.value.trim();
      if (!body || body.length > 140) return;
      App.data.updateComment(commentId, body);
      build();
    });
  }

  function open(postId, { onChange } = {}) {
    currentPostId = Number(postId);
    onChangeCallback = onChange || null;
    build();
  }

  return { open };
})();
