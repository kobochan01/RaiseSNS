// modals/postCreate.js — 投稿作成モーダル
window.App = window.App || {};
App.modals = App.modals || {};

App.modals.postCreate = (function () {
  const MAX_LEN = 280;
  let selectedImage = null;

  function template() {
    return `
      <div class="modal-header">
        <h2>投稿を作成</h2>
        <button type="button" class="modal-close" data-action="close" aria-label="閉じる">×</button>
      </div>
      <div class="modal-body">
        <textarea id="post-body" class="post-create__textarea" placeholder="今なにしてる？" maxlength="500"></textarea>
        <div class="field-meta">
          <span></span>
          <span class="char-counter" id="post-char-counter">0 / ${MAX_LEN}</span>
        </div>
        <div id="image-preview-slot"></div>
        <div class="post-create__footer">
          <label class="file-label">
            🖼 画像を選択
            <input type="file" id="post-image-input" accept="image/jpeg,image/png,image/gif,image/webp" />
          </label>
          <button type="button" class="btn btn--primary" id="submit-post" disabled>投稿する</button>
        </div>
      </div>
    `;
  }

  function renderPreview(container) {
    const slot = container.querySelector("#image-preview-slot");
    if (!selectedImage) {
      slot.innerHTML = "";
      return;
    }
    slot.innerHTML = `
      <div class="image-preview-wrap">
        <img class="image-preview" src="${selectedImage}" alt="添付画像プレビュー" />
        <button type="button" class="image-remove-btn" id="remove-image" aria-label="画像を削除">×</button>
      </div>
    `;
    slot.querySelector("#remove-image").addEventListener("click", () => {
      selectedImage = null;
      container.querySelector("#post-image-input").value = "";
      renderPreview(container);
    });
  }

  function updateSubmitState(container) {
    const body = container.querySelector("#post-body").value;
    const counter = container.querySelector("#post-char-counter");
    counter.textContent = `${body.length} / ${MAX_LEN}`;
    counter.classList.toggle("is-over", body.length > MAX_LEN);
    const submitBtn = container.querySelector("#submit-post");
    submitBtn.disabled = body.trim().length === 0 || body.length > MAX_LEN;
  }

  function open({ onCreated } = {}) {
    selectedImage = null;
    App.modal.open(template());
    const container = App.modal.getBody();

    container.querySelector('[data-action="close"]').addEventListener("click", () => App.modal.close());

    const textarea = container.querySelector("#post-body");
    textarea.addEventListener("input", () => updateSubmitState(container));
    updateSubmitState(container);

    container.querySelector("#post-image-input").addEventListener("change", async (e) => {
      const file = e.target.files[0];
      if (!file) return;
      if (file.size > 5 * 1024 * 1024) {
        alert("画像サイズは5MB以下にしてください。");
        e.target.value = "";
        return;
      }
      selectedImage = await App.utils.fileToDataUrl(file);
      renderPreview(container);
    });

    container.querySelector("#submit-post").addEventListener("click", () => {
      const body = textarea.value.trim();
      if (!body || body.length > MAX_LEN) return;
      const currentUser = App.auth.currentUser();
      App.data.createPost({ userId: currentUser.id, body, imageUrl: selectedImage });
      App.modal.close();
      if (onCreated) onCreated();
    });
  }

  return { open };
})();
