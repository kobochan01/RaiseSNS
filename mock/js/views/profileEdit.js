// views/profileEdit.js — プロフィール編集画面
window.App = window.App || {};
App.views = App.views || {};

App.views.profileEdit = (function () {
  const u = App.utils;
  const esc = u.escapeHtml;
  const BIO_MAX = 160;

  function template(user) {
    return `
      <a href="#/profile/${esc(user.username)}" class="profile-back">← 戻る</a>
      <div class="profile-header">
        <h2 class="auth-title">プロフィール編集</h2>
        <div class="form-field">
          <label>アイコン画像</label>
          <div id="avatar-preview-slot"></div>
          <label class="file-label">
            🖼 画像を選択
            <input type="file" id="avatar-input" accept="image/jpeg,image/png,image/gif,image/webp" />
          </label>
        </div>
        <div class="form-field">
          <label for="edit-display-name">表示名</label>
          <input type="text" id="edit-display-name" value="${esc(user.displayName)}" maxlength="50" required />
          <div class="error-text" id="err-display-name"></div>
        </div>
        <div class="form-field">
          <label for="edit-bio">自己紹介</label>
          <textarea id="edit-bio" rows="3" maxlength="${BIO_MAX}">${esc(user.bio || "")}</textarea>
          <div class="field-meta">
            <span></span>
            <span class="char-counter" id="bio-char-counter"></span>
          </div>
        </div>
        <button type="button" class="btn btn--primary" id="save-profile-btn">保存</button>
      </div>
    `;
  }

  function renderAvatarPreview(slot, user, dataUrl) {
    const previewUser = { id: user.id, displayName: user.displayName, avatarUrl: dataUrl !== undefined ? dataUrl : user.avatarUrl };
    slot.innerHTML = u.avatarHtml(previewUser, "lg");
  }

  function render(container, { params, currentUser }) {
    if (params.username !== currentUser.username) {
      App.router.navigate(`/profile/${params.username}`);
      return;
    }

    container.innerHTML = template(currentUser);

    let selectedAvatar = currentUser.avatarUrl || null;
    const avatarSlot = container.querySelector("#avatar-preview-slot");
    renderAvatarPreview(avatarSlot, currentUser, selectedAvatar);

    container.querySelector("#avatar-input").addEventListener("change", async (e) => {
      const file = e.target.files[0];
      if (!file) return;
      if (file.size > 5 * 1024 * 1024) {
        alert("画像サイズは5MB以下にしてください。");
        e.target.value = "";
        return;
      }
      selectedAvatar = await u.fileToDataUrl(file);
      renderAvatarPreview(avatarSlot, currentUser, selectedAvatar);
    });

    const bioInput = container.querySelector("#edit-bio");
    const bioCounter = container.querySelector("#bio-char-counter");
    function updateBioCounter() {
      bioCounter.textContent = `${bioInput.value.length} / ${BIO_MAX}`;
      bioCounter.classList.toggle("is-over", bioInput.value.length > BIO_MAX);
    }
    bioInput.addEventListener("input", updateBioCounter);
    updateBioCounter();

    container.querySelector("#save-profile-btn").addEventListener("click", () => {
      const errEl = container.querySelector("#err-display-name");
      errEl.textContent = "";
      const displayName = container.querySelector("#edit-display-name").value.trim();
      const bio = bioInput.value;

      if (!u.inRange(displayName, 1, 50)) {
        errEl.textContent = "表示名は1〜50文字で入力してください。";
        return;
      }
      if (bio.length > BIO_MAX) {
        return;
      }

      App.data.updateUserProfile(currentUser.id, { displayName, bio, avatarUrl: selectedAvatar });
      App.router.navigate(`/profile/${currentUser.username}`);
    });
  }

  return { render };
})();
