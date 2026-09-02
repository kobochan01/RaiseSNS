// modal.js — 汎用モーダル開閉ユーティリティ
window.App = window.App || {};

App.modal = (function () {
  let root = null;
  let onCloseCallback = null;

  function getRoot() {
    if (!root) root = document.getElementById("modal-root");
    return root;
  }

  function open(innerHtml, { onClose } = {}) {
    const el = getRoot();
    onCloseCallback = onClose || null;
    el.innerHTML = `<div class="modal-overlay"><div class="modal" role="dialog" aria-modal="true">${innerHtml}</div></div>`;
    el.setAttribute("aria-hidden", "false");
    document.body.style.overflow = "hidden";

    el.querySelector(".modal-overlay").addEventListener("click", (e) => {
      if (e.target.classList.contains("modal-overlay")) close();
    });
    document.addEventListener("keydown", handleEscape);
  }

  function handleEscape(e) {
    if (e.key === "Escape") close();
  }

  function close() {
    const el = getRoot();
    el.innerHTML = "";
    el.setAttribute("aria-hidden", "true");
    document.body.style.overflow = "";
    document.removeEventListener("keydown", handleEscape);
    const cb = onCloseCallback;
    onCloseCallback = null;
    if (cb) cb();
  }

  function getBody() {
    return getRoot().querySelector(".modal");
  }

  return { open, close, getBody };
})();
