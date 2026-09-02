// utils.js — 汎用ヘルパー関数
window.App = window.App || {};

App.utils = (function () {
  function escapeHtml(str) {
    if (str === null || str === undefined) return "";
    return String(str)
      .replace(/&/g, "&amp;")
      .replace(/</g, "&lt;")
      .replace(/>/g, "&gt;")
      .replace(/"/g, "&quot;")
      .replace(/'/g, "&#39;");
  }

  function formatRelativeTime(isoString) {
    const target = new Date(isoString).getTime();
    const diffMs = Date.now() - target;
    const diffSec = Math.floor(diffMs / 1000);
    if (diffSec < 60) return "たった今";
    const diffMin = Math.floor(diffSec / 60);
    if (diffMin < 60) return `${diffMin}分前`;
    const diffHour = Math.floor(diffMin / 60);
    if (diffHour < 24) return `${diffHour}時間前`;
    const diffDay = Math.floor(diffHour / 24);
    if (diffDay < 7) return `${diffDay}日前`;
    const d = new Date(isoString);
    return `${d.getFullYear()}/${d.getMonth() + 1}/${d.getDate()}`;
  }

  function fileToDataUrl(file) {
    return new Promise((resolve, reject) => {
      const reader = new FileReader();
      reader.onload = () => resolve(reader.result);
      reader.onerror = reject;
      reader.readAsDataURL(file);
    });
  }

  const AVATAR_COLORS = ["#f4b400", "#db4437", "#4285f4", "#0f9d58", "#ab47bc", "#00acc1", "#ff7043", "#9e9d24"];

  function avatarColor(userId) {
    const n = Number(userId) || 0;
    return AVATAR_COLORS[n % AVATAR_COLORS.length];
  }

  function avatarHtml(user, size) {
    size = size || "md";
    if (user && user.avatarUrl) {
      return `<div class="avatar avatar--${size}" style="background-image:url('${user.avatarUrl}')"></div>`;
    }
    const initial = user && user.displayName ? escapeHtml(user.displayName.charAt(0)) : "?";
    const color = avatarColor(user ? user.id : 0);
    return `<div class="avatar avatar--${size}" style="background-color:${color}">${initial}</div>`;
  }

  function svgPlaceholder(bgColor, emoji) {
    const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="600" height="360">
      <rect width="100%" height="100%" fill="${bgColor}"/>
      <text x="50%" y="50%" font-size="120" text-anchor="middle" dominant-baseline="central">${emoji}</text>
    </svg>`;
    return "data:image/svg+xml;utf8," + encodeURIComponent(svg);
  }

  function isValidEmail(email) {
    return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
  }

  function isValidUsername(username) {
    return /^[A-Za-z0-9_]{3,30}$/.test(username);
  }

  function inRange(str, min, max) {
    const len = (str || "").length;
    return len >= min && len <= max;
  }

  function parseHash(hash) {
    const raw = (hash || "").replace(/^#/, "");
    const [path, queryString] = raw.split("?");
    const query = {};
    if (queryString) {
      queryString.split("&").forEach((pair) => {
        if (!pair) return;
        const [k, v] = pair.split("=");
        query[decodeURIComponent(k)] = decodeURIComponent(v || "");
      });
    }
    return { path: path || "/", query };
  }

  function buildHash(path, query) {
    let hash = "#" + path;
    if (query && Object.keys(query).length) {
      const qs = Object.entries(query)
        .filter(([, v]) => v !== undefined && v !== null && v !== "")
        .map(([k, v]) => `${encodeURIComponent(k)}=${encodeURIComponent(v)}`)
        .join("&");
      if (qs) hash += "?" + qs;
    }
    return hash;
  }

  function postCardHtml(post) {
    const author = post.author;
    const imageHtml = post.imageUrl
      ? `<img class="post-card__image" src="${post.imageUrl}" alt="投稿画像" />`
      : "";
    return `
      <article class="post-card" data-action="open-post" data-post-id="${post.id}">
        ${avatarHtml(author, "md")}
        <div class="post-card__body">
          <div class="post-card__head">
            <span class="post-card__name" data-action="open-profile" data-username="${escapeHtml(author.username)}">${escapeHtml(author.displayName)}</span>
            <span class="post-card__username" data-action="open-profile" data-username="${escapeHtml(author.username)}">@${escapeHtml(author.username)}</span>
            <span class="post-card__time">・${formatRelativeTime(post.createdAt)}</span>
          </div>
          <p class="post-card__text" data-action="open-post" data-post-id="${post.id}">${escapeHtml(post.body)}</p>
          ${imageHtml}
          <div class="post-card__actions">
            <button type="button" class="action-btn like-btn ${post.isLikedByMe ? "is-liked" : ""}" data-action="toggle-like" data-post-id="${post.id}">
              <span class="icon">${post.isLikedByMe ? "♥" : "♡"}</span><span class="like-count">${post.likeCount}</span>
            </button>
            <span class="action-btn" data-action="open-post" data-post-id="${post.id}">
              <span class="icon">💬</span><span>${post.commentCount}</span>
            </span>
          </div>
        </div>
      </article>
    `;
  }

  return {
    escapeHtml,
    formatRelativeTime,
    fileToDataUrl,
    avatarColor,
    avatarHtml,
    svgPlaceholder,
    isValidEmail,
    isValidUsername,
    inRange,
    parseHash,
    buildHash,
    postCardHtml,
  };
})();
