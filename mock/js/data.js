// data.js — モックDB（シードデータ + localStorage永続化 + CRUDアクセサ）
window.App = window.App || {};

App.data = (function () {
  const STORAGE_KEY = "raisesns_db";
  const SEED_PASSWORD = "password123";
  const u = App.utils;

  let db = null;

  function minutesAgo(min) {
    return new Date(Date.now() - min * 60 * 1000).toISOString();
  }

  function buildSeed() {
    const users = [
      { id: 1, username: "taro", email: "taro@example.com", password: SEED_PASSWORD, displayName: "太郎", bio: "学習中のエンジニアです。RaiseSNSを作っています。", avatarUrl: null, createdAt: minutesAgo(60 * 24 * 30) },
      { id: 2, username: "hanako", email: "hanako@example.com", password: SEED_PASSWORD, displayName: "花子", bio: "猫と写真が好きです🐱", avatarUrl: null, createdAt: minutesAgo(60 * 24 * 28) },
      { id: 3, username: "jiro", email: "jiro@example.com", password: SEED_PASSWORD, displayName: "次郎", bio: "フロントエンド勉強中。React歴3ヶ月。", avatarUrl: null, createdAt: minutesAgo(60 * 24 * 20) },
      { id: 4, username: "sakura", email: "sakura@example.com", password: SEED_PASSWORD, displayName: "さくら", bio: "旅行と美味しいものが好き。", avatarUrl: null, createdAt: minutesAgo(60 * 24 * 15) },
      { id: 5, username: "kenji", email: "kenji@example.com", password: SEED_PASSWORD, displayName: "健二", bio: "バックエンドエンジニア。Spring Boot触ってます。", avatarUrl: null, createdAt: minutesAgo(60 * 24 * 10) },
      { id: 6, username: "misaki", email: "misaki@example.com", password: SEED_PASSWORD, displayName: "美咲", bio: "デザイン担当。UI/UXが好きです。", avatarUrl: null, createdAt: minutesAgo(60 * 24 * 5) },
    ];

    const img = u.svgPlaceholder;
    const posts = [
      { id: 1, userId: 1, body: "今日はいい天気ですね。散歩日和です。", imageUrl: img("#8ecae6", "☀️"), createdAt: minutesAgo(5), updatedAt: minutesAgo(5) },
      { id: 2, userId: 2, body: "学習アプリ作ってます。もうすぐ完成しそう！", imageUrl: null, createdAt: minutesAgo(18), updatedAt: minutesAgo(18) },
      { id: 3, userId: 3, body: "Reactの勉強を始めました。stateとpropsがようやく分かってきた気がする。", imageUrl: null, createdAt: minutesAgo(40), updatedAt: minutesAgo(40) },
      { id: 4, userId: 4, body: "京都に旅行してきました。紅葉がきれいでした🍁", imageUrl: img("#e76f51", "🍁"), createdAt: minutesAgo(65), updatedAt: minutesAgo(65) },
      { id: 5, userId: 5, body: "Spring Bootのキャッチアップ中。@Transactionalの挙動を理解するのに時間かかった。", imageUrl: null, createdAt: minutesAgo(90), updatedAt: minutesAgo(90) },
      { id: 6, userId: 6, body: "新しいデザインツールを試してます。プロトタイピングが捗る。", imageUrl: img("#a29bfe", "🎨"), createdAt: minutesAgo(120), updatedAt: minutesAgo(120) },
      { id: 7, userId: 1, body: "コメントもいいねも試してみてください！", imageUrl: null, createdAt: minutesAgo(150), updatedAt: minutesAgo(150) },
      { id: 8, userId: 2, body: "猫を飼い始めました。名前はまだ決まってません。", imageUrl: img("#ffb703", "🐱"), createdAt: minutesAgo(200), updatedAt: minutesAgo(200) },
      { id: 9, userId: 4, body: "美味しいラーメン屋を発見しました。また行きたい。", imageUrl: img("#fb8500", "🍜"), createdAt: minutesAgo(300), updatedAt: minutesAgo(300) },
      { id: 10, userId: 3, body: "今日はカフェで作業。集中できました。", imageUrl: null, createdAt: minutesAgo(400), updatedAt: minutesAgo(400) },
      { id: 11, userId: 5, body: "APIの設計について考え中。RESTかGraphQLか悩ましい。", imageUrl: null, createdAt: minutesAgo(600), updatedAt: minutesAgo(600) },
      { id: 12, userId: 6, body: "UIデザインの勉強会に参加しました。学びが多かった。", imageUrl: null, createdAt: minutesAgo(800), updatedAt: minutesAgo(800) },
      { id: 13, userId: 2, body: "週末は登山に行ってきました。山頂からの景色は最高でした⛰️", imageUrl: img("#2a9d8f", "⛰️"), createdAt: minutesAgo(1000), updatedAt: minutesAgo(1000) },
      { id: 14, userId: 1, body: "RaiseSNSのモックアップができました。全部触れます。", imageUrl: null, createdAt: minutesAgo(1200), updatedAt: minutesAgo(1200) },
    ];

    const comments = [
      { id: 1, postId: 1, userId: 2, body: "いいですね！私も散歩行こうかな。", createdAt: minutesAgo(3), updatedAt: minutesAgo(3) },
      { id: 2, postId: 2, userId: 1, body: "完成楽しみにしてます！", createdAt: minutesAgo(10), updatedAt: minutesAgo(10) },
      { id: 3, postId: 4, userId: 1, body: "紅葉きれいですね、どこのお寺ですか？", createdAt: minutesAgo(50), updatedAt: minutesAgo(50) },
      { id: 4, postId: 4, userId: 6, body: "写真センスいいですね〜", createdAt: minutesAgo(30), updatedAt: minutesAgo(30) },
      { id: 5, postId: 8, userId: 4, body: "かわいい！名前決まったら教えてください", createdAt: minutesAgo(180), updatedAt: minutesAgo(180) },
      { id: 6, postId: 7, userId: 3, body: "試してみました、いいね機能便利ですね", createdAt: minutesAgo(140), updatedAt: minutesAgo(140) },
    ];

    const likes = [
      { id: 1, postId: 1, userId: 2, createdAt: minutesAgo(4) },
      { id: 2, postId: 1, userId: 3, createdAt: minutesAgo(3) },
      { id: 3, postId: 1, userId: 5, createdAt: minutesAgo(2) },
      { id: 4, postId: 2, userId: 1, createdAt: minutesAgo(15) },
      { id: 5, postId: 2, userId: 4, createdAt: minutesAgo(12) },
      { id: 6, postId: 4, userId: 1, createdAt: minutesAgo(60) },
      { id: 7, postId: 4, userId: 2, createdAt: minutesAgo(55) },
      { id: 8, postId: 4, userId: 6, createdAt: minutesAgo(45) },
      { id: 9, postId: 8, userId: 4, createdAt: minutesAgo(190) },
      { id: 10, postId: 8, userId: 5, createdAt: minutesAgo(185) },
      { id: 11, postId: 7, userId: 3, createdAt: minutesAgo(145) },
      { id: 12, postId: 13, userId: 1, createdAt: minutesAgo(950) },
    ];

    const follows = [
      { followerId: 1, followeeId: 2, createdAt: minutesAgo(60 * 24 * 10) },
      { followerId: 1, followeeId: 3, createdAt: minutesAgo(60 * 24 * 9) },
      { followerId: 1, followeeId: 4, createdAt: minutesAgo(60 * 24 * 8) },
      { followerId: 2, followeeId: 1, createdAt: minutesAgo(60 * 24 * 9) },
      { followerId: 3, followeeId: 1, createdAt: minutesAgo(60 * 24 * 7) },
      { followerId: 3, followeeId: 5, createdAt: minutesAgo(60 * 24 * 6) },
      { followerId: 4, followeeId: 1, createdAt: minutesAgo(60 * 24 * 5) },
      { followerId: 4, followeeId: 6, createdAt: minutesAgo(60 * 24 * 4) },
      { followerId: 5, followeeId: 1, createdAt: minutesAgo(60 * 24 * 3) },
      { followerId: 6, followeeId: 2, createdAt: minutesAgo(60 * 24 * 2) },
      { followerId: 6, followeeId: 1, createdAt: minutesAgo(60 * 24 * 1) },
    ];

    return {
      meta: { nextUserId: 7, nextPostId: 15, nextCommentId: 7, nextLikeId: 13 },
      users,
      posts,
      comments,
      likes,
      follows,
    };
  }

  function load() {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (raw) {
      try {
        db = JSON.parse(raw);
        return;
      } catch (e) {
        // フォールバックしてシードし直す
      }
    }
    db = buildSeed();
    save();
  }

  function save() {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(db));
  }

  function init() {
    load();
  }

  function resetSeed() {
    db = buildSeed();
    save();
  }

  // ----- users -----
  function getUserById(id) {
    return db.users.find((x) => x.id === Number(id)) || null;
  }

  function getUserByUsername(username) {
    if (!username) return null;
    const lower = username.toLowerCase();
    return db.users.find((x) => x.username.toLowerCase() === lower) || null;
  }

  function isUsernameTaken(username) {
    return !!getUserByUsername(username);
  }

  function isEmailTaken(email) {
    const lower = (email || "").toLowerCase();
    return db.users.some((x) => x.email.toLowerCase() === lower);
  }

  function createUser({ username, email, password, displayName }) {
    const id = db.meta.nextUserId++;
    const user = {
      id,
      username,
      email,
      password,
      displayName,
      bio: "",
      avatarUrl: null,
      createdAt: new Date().toISOString(),
    };
    db.users.push(user);
    save();
    return user;
  }

  function authenticateUser(email, password) {
    const lower = (email || "").toLowerCase();
    const user = db.users.find((x) => x.email.toLowerCase() === lower);
    if (!user || user.password !== password) return null;
    return user;
  }

  function updateUserProfile(userId, { displayName, bio, avatarUrl }) {
    const user = getUserById(userId);
    if (!user) return null;
    user.displayName = displayName;
    user.bio = bio || "";
    if (avatarUrl !== undefined) user.avatarUrl = avatarUrl;
    save();
    return user;
  }

  function searchUsers(keyword) {
    const lower = keyword.toLowerCase();
    return db.users.filter((x) => x.username.toLowerCase().includes(lower));
  }

  // ----- follows -----
  function isFollowing(followerId, followeeId) {
    return db.follows.some((f) => f.followerId === Number(followerId) && f.followeeId === Number(followeeId));
  }

  function followUser(followerId, followeeId) {
    followerId = Number(followerId);
    followeeId = Number(followeeId);
    if (followerId === followeeId) return false;
    if (isFollowing(followerId, followeeId)) return true;
    db.follows.push({ followerId, followeeId, createdAt: new Date().toISOString() });
    save();
    return true;
  }

  function unfollowUser(followerId, followeeId) {
    followerId = Number(followerId);
    followeeId = Number(followeeId);
    db.follows = db.follows.filter((f) => !(f.followerId === followerId && f.followeeId === followeeId));
    save();
    return true;
  }

  function getFollowCounts(userId) {
    userId = Number(userId);
    const followingCount = db.follows.filter((f) => f.followerId === userId).length;
    const followerCount = db.follows.filter((f) => f.followeeId === userId).length;
    return { followingCount, followerCount };
  }

  function enrichUserRow(user, currentUserId) {
    return {
      id: user.id,
      username: user.username,
      displayName: user.displayName,
      bio: user.bio,
      avatarUrl: user.avatarUrl,
      isFollowedByMe: currentUserId ? isFollowing(currentUserId, user.id) : false,
    };
  }

  function getFollowingList(userId, currentUserId) {
    userId = Number(userId);
    return db.follows
      .filter((f) => f.followerId === userId)
      .sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt))
      .map((f) => enrichUserRow(getUserById(f.followeeId), currentUserId))
      .filter(Boolean);
  }

  function getFollowersList(userId, currentUserId) {
    userId = Number(userId);
    return db.follows
      .filter((f) => f.followeeId === userId)
      .sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt))
      .map((f) => enrichUserRow(getUserById(f.followerId), currentUserId))
      .filter(Boolean);
  }

  // ----- posts -----
  function getPostById(id) {
    return db.posts.find((x) => x.id === Number(id)) || null;
  }

  function getEnrichedPost(id, currentUserId) {
    const post = getPostById(id);
    if (!post) return null;
    return enrichPost(post, currentUserId);
  }

  function getLikeCount(postId) {
    return db.likes.filter((l) => l.postId === Number(postId)).length;
  }

  function getCommentCount(postId) {
    return db.comments.filter((c) => c.postId === Number(postId)).length;
  }

  function isLikedByUser(postId, userId) {
    return db.likes.some((l) => l.postId === Number(postId) && l.userId === Number(userId));
  }

  function enrichPost(post, currentUserId) {
    return {
      id: post.id,
      userId: post.userId,
      author: getUserById(post.userId),
      body: post.body,
      imageUrl: post.imageUrl,
      createdAt: post.createdAt,
      updatedAt: post.updatedAt,
      likeCount: getLikeCount(post.id),
      commentCount: getCommentCount(post.id),
      isLikedByMe: currentUserId ? isLikedByUser(post.id, currentUserId) : false,
    };
  }

  function getTimelinePosts({ scope, currentUserId }) {
    let list = db.posts.slice();
    if (scope === "following") {
      const followingIds = db.follows.filter((f) => f.followerId === Number(currentUserId)).map((f) => f.followeeId);
      followingIds.push(Number(currentUserId));
      list = list.filter((p) => followingIds.includes(p.userId));
    }
    list.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));
    return list.map((p) => enrichPost(p, currentUserId));
  }

  function getPostsByUser(userId, currentUserId) {
    userId = Number(userId);
    return db.posts
      .filter((p) => p.userId === userId)
      .sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt))
      .map((p) => enrichPost(p, currentUserId));
  }

  function createPost({ userId, body, imageUrl }) {
    const id = db.meta.nextPostId++;
    const now = new Date().toISOString();
    const post = { id, userId: Number(userId), body, imageUrl: imageUrl || null, createdAt: now, updatedAt: now };
    db.posts.unshift(post);
    save();
    return post;
  }

  function updatePost(postId, body) {
    const post = getPostById(postId);
    if (!post) return null;
    post.body = body;
    post.updatedAt = new Date().toISOString();
    save();
    return post;
  }

  function deletePost(postId) {
    postId = Number(postId);
    db.posts = db.posts.filter((p) => p.id !== postId);
    db.comments = db.comments.filter((c) => c.postId !== postId);
    db.likes = db.likes.filter((l) => l.postId !== postId);
    save();
  }

  function toggleLike(postId, userId) {
    postId = Number(postId);
    userId = Number(userId);
    if (isLikedByUser(postId, userId)) {
      db.likes = db.likes.filter((l) => !(l.postId === postId && l.userId === userId));
    } else {
      db.likes.push({ id: db.meta.nextLikeId++, postId, userId, createdAt: new Date().toISOString() });
    }
    save();
    return { likeCount: getLikeCount(postId), isLikedByMe: isLikedByUser(postId, userId) };
  }

  // ----- comments -----
  function getCommentsByPost(postId) {
    postId = Number(postId);
    return db.comments
      .filter((c) => c.postId === postId)
      .sort((a, b) => new Date(a.createdAt) - new Date(b.createdAt))
      .map((c) => ({
        id: c.id,
        postId: c.postId,
        userId: c.userId,
        author: getUserById(c.userId),
        body: c.body,
        createdAt: c.createdAt,
        updatedAt: c.updatedAt,
      }));
  }

  function createComment({ postId, userId, body }) {
    const id = db.meta.nextCommentId++;
    const now = new Date().toISOString();
    const comment = { id, postId: Number(postId), userId: Number(userId), body, createdAt: now, updatedAt: now };
    db.comments.push(comment);
    save();
    return comment;
  }

  function updateComment(commentId, body) {
    const comment = db.comments.find((c) => c.id === Number(commentId));
    if (!comment) return null;
    comment.body = body;
    comment.updatedAt = new Date().toISOString();
    save();
    return comment;
  }

  function deleteComment(commentId) {
    db.comments = db.comments.filter((c) => c.id !== Number(commentId));
    save();
  }

  function getCommentById(commentId) {
    const c = db.comments.find((x) => x.id === Number(commentId));
    if (!c) return null;
    return { ...c, author: getUserById(c.userId) };
  }

  return {
    init,
    resetSeed,
    getUserById,
    getUserByUsername,
    isUsernameTaken,
    isEmailTaken,
    createUser,
    authenticateUser,
    updateUserProfile,
    searchUsers,
    isFollowing,
    followUser,
    unfollowUser,
    getFollowCounts,
    getFollowingList,
    getFollowersList,
    getPostById,
    getEnrichedPost,
    getTimelinePosts,
    getPostsByUser,
    createPost,
    updatePost,
    deletePost,
    toggleLike,
    getCommentsByPost,
    createComment,
    updateComment,
    deleteComment,
    getCommentById,
  };
})();
