"use strict";
const API = ((window.APP_CONFIG && window.APP_CONFIG.API_BASE_URL) || "").replace(/\/$/, "");
const $ = (s, root = document) => root.querySelector(s);
const $$ = (s, root = document) => [...root.querySelectorAll(s)];
const esc = (v) => String(v ?? "").replace(/[&<>"']/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]));
const fmtDate = (d) => (d ? new Date(d).toLocaleString() : "");

// Tokens live in sessionStorage (cleared when the tab closes). For production, prefer httpOnly cookies.
const session = {
  get access() { return sessionStorage.getItem("cms.access"); },
  get refresh() { return sessionStorage.getItem("cms.refresh"); },
  get user() { try { return JSON.parse(sessionStorage.getItem("cms.user")); } catch { return null; } },
  save(d) {
    sessionStorage.setItem("cms.access", d.accessToken);
    sessionStorage.setItem("cms.refresh", d.refreshToken);
    if (d.user) sessionStorage.setItem("cms.user", JSON.stringify(d.user));
  },
  clear() { sessionStorage.clear(); }
};

const state = { view: "posts", page: 0, size: 5, sort: "createdAt,desc", categories: [] };

async function api(path, { method = "GET", body, auth = true, retry = true } = {}) {
  const headers = { "Content-Type": "application/json" };
  if (auth && session.access) headers.Authorization = "Bearer " + session.access;
  const started = performance.now();
  let res;
  try {
    res = await fetch(API + path, { method, headers, body: body ? JSON.stringify(body) : undefined });
  } catch {
    setTrace(method, path, "network error", Math.round(performance.now() - started), "-");
    throw new Error("Cannot reach the server. If it is hosted on a free plan it may be waking up - try again in a minute.");
  }
  setTrace(method, path, res.status, Math.round(performance.now() - started), res.headers.get("X-Correlation-ID") || "-");
  if (res.status === 401 && auth && retry && session.refresh && await tryRefresh()) {
    return api(path, { method, body, auth, retry: false });
  }
  if (res.status === 401 && auth) { signOut(); throw new Error("Your session expired. Please sign in again."); }
  if (res.status === 204) return null;
  const json = await res.json().catch(() => null);
  if (!res.ok) {
    const details = json && json.validationErrors ? " (" + Object.values(json.validationErrors).join("; ") + ")" : "";
    throw new Error(((json && json.message) || "Request failed") + details);
  }
  return json;
}

async function tryRefresh() {
  try {
    const res = await fetch(API + "/api/auth/refresh", { method: "POST", headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ refreshToken: session.refresh }) });
    if (!res.ok) return false;
    session.save((await res.json()).data);
    return true;
  } catch { return false; }
}

function setTrace(method, path, status, ms, cid) {
  $("#trace-line").textContent = `${method} ${path}  status=${status}  time=${ms}ms  correlationId=${cid}`;
}
function notify(msg, kind = "good") {
  const n = $("#notice");
  n.textContent = msg; n.className = "notice " + kind;
  clearTimeout(notify.t); notify.t = setTimeout(() => n.classList.add("hidden"), 5000);
}
const isAdmin = () => (session.user && session.user.role) === "ADMIN";

/* ---------- auth ---------- */
$$(".tab").forEach((t) => t.addEventListener("click", () => {
  $$(".tab").forEach((x) => x.classList.toggle("active", x === t));
  $("#login-form").classList.toggle("hidden", t.dataset.auth !== "login");
  $("#register-form").classList.toggle("hidden", t.dataset.auth !== "register");
  $("#auth-error").classList.add("hidden");
}));
function authError(msg) { const e = $("#auth-error"); e.textContent = msg; e.classList.remove("hidden"); }

$("#login-form").addEventListener("submit", async (e) => {
  e.preventDefault();
  const f = new FormData(e.target);
  try {
    const res = await api("/api/auth/login", { method: "POST", auth: false, body: { username: f.get("username"), password: f.get("password") } });
    session.save(res.data); e.target.reset(); showApp();
  } catch (err) { authError(err.message); }
});
$("#register-form").addEventListener("submit", async (e) => {
  e.preventDefault();
  const f = new FormData(e.target);
  const body = { username: f.get("username"), email: f.get("email"), password: f.get("password") };
  if (f.get("phone")) body.phone = f.get("phone");
  try {
    await api("/api/auth/register", { method: "POST", auth: false, body });
    const res = await api("/api/auth/login", { method: "POST", auth: false, body: { username: body.username, password: body.password } });
    session.save(res.data); e.target.reset(); showApp();
  } catch (err) { authError(err.message); }
});
async function signOut() {
  const refreshToken = session.refresh;
  session.clear();
  if (refreshToken) fetch(API + "/api/auth/logout", { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ refreshToken }) }).catch(() => {});
  $("#auth-view").classList.remove("hidden"); $("#app-view").classList.add("hidden");
  $("#user-chip").classList.add("hidden"); $("#logout-btn").classList.add("hidden");
}
$("#logout-btn").addEventListener("click", signOut);

function showApp() {
  $("#auth-view").classList.add("hidden"); $("#app-view").classList.remove("hidden");
  const u = session.user;
  $("#user-chip").textContent = `${u.username} · ${u.role}`; $("#user-chip").classList.remove("hidden");
  $("#logout-btn").classList.remove("hidden");
  $$(".admin-only").forEach((el) => el.classList.toggle("hidden", !isAdmin()));
  go("posts");
}
$$(".nav").forEach((b) => b.addEventListener("click", () => go(b.dataset.view)));
function go(view) {
  state.view = view;
  $$(".nav").forEach((b) => b.classList.toggle("active", b.dataset.view === view));
  ({ posts: renderPosts, categories: renderCategories, users: renderUsers, perf: renderPerf }[view])();
}
const guard = (fn) => async (...a) => { try { await fn(...a); } catch (e) { notify(e.message, "err"); } };

/* ---------- posts ---------- */
const renderPosts = guard(async () => {
  const [posts, cats] = await Promise.all([
    api(`/api/posts?page=${state.page}&size=${state.size}&sort=${encodeURIComponent(state.sort)}`),
    api("/api/categories")
  ]);
  state.categories = cats.data;
  const p = posts.data, me = session.user;
  $("#view").innerHTML = `
    <h2>Posts</h2>
    <div class="toolbar">
      <label>Sort by<select id="sort">
        ${[["createdAt,desc", "Newest first"], ["createdAt,asc", "Oldest first"], ["title,asc", "Title A–Z"], ["title,desc", "Title Z–A"], ["updatedAt,desc", "Recently updated"]]
          .map(([v, l]) => `<option value="${v}" ${v === state.sort ? "selected" : ""}>${l}</option>`).join("")}</select></label>
      <label>Per page<select id="size">${[5, 10, 20].map((n) => `<option ${n === state.size ? "selected" : ""}>${n}</option>`).join("")}</select></label>
      <button class="btn primary" id="new-post">New post</button>
    </div>
    <div id="post-form-slot"></div>
    <div class="panel">${p.content.length ? p.content.map((x) => `
      <article class="post">
        <h3>${esc(x.title)}</h3>
        <div class="meta">${esc(x.categoryName)} · by ${esc(x.authorUsername)} · ${fmtDate(x.createdAt)}</div>
        <p>${esc(x.content)}</p>
        ${(isAdmin() || x.authorId === me.id) ? `<div class="row-actions">
          <button class="btn small" data-edit="${x.id}">Edit</button>
          <button class="btn small danger" data-del="${x.id}">Delete</button></div>` : ""}
      </article>`).join("") : "<p>No posts yet. Create the first one.</p>"}
    </div>
    <div class="pager">
      <button class="btn small" id="prev" ${p.first ? "disabled" : ""}>Previous</button>
      <span>Page ${p.page + 1} of ${Math.max(p.totalPages, 1)} · ${p.totalElements} posts</span>
      <button class="btn small" id="next" ${p.last ? "disabled" : ""}>Next</button>
    </div>`;
  $("#sort").onchange = (e) => { state.sort = e.target.value; state.page = 0; renderPosts(); };
  $("#size").onchange = (e) => { state.size = +e.target.value; state.page = 0; renderPosts(); };
  $("#prev").onclick = () => { state.page--; renderPosts(); };
  $("#next").onclick = () => { state.page++; renderPosts(); };
  $("#new-post").onclick = () => postForm();
  $$("[data-edit]").forEach((b) => b.onclick = guard(async () => postForm((await api("/api/posts/" + b.dataset.edit)).data)));
  $$("[data-del]").forEach((b) => b.onclick = guard(async () => {
    if (!confirm("Delete this post?")) return;
    await api("/api/posts/" + b.dataset.del, { method: "DELETE" });
    notify("Post deleted"); renderPosts();
  }));
});

function postForm(post) {
  $("#post-form-slot").innerHTML = `
    <form class="panel form" id="post-form">
      <h2>${post ? "Edit post" : "New post"}</h2>
      <label>Title<input name="title" required maxlength="150" value="${esc(post?.title)}"></label>
      <label>Category<select name="categoryId" required>${state.categories.map((c) =>
        `<option value="${c.id}" ${post && post.categoryId === c.id ? "selected" : ""}>${esc(c.name)}</option>`).join("")}</select></label>
      <label>Content<textarea name="content" required maxlength="5000">${esc(post?.content)}</textarea></label>
      <div class="row-actions"><button class="btn primary" type="submit">Save post</button>
      <button class="btn" type="button" id="cancel-post">Cancel</button></div>
    </form>`;
  $("#cancel-post").onclick = () => ($("#post-form-slot").innerHTML = "");
  $("#post-form").onsubmit = guard(async (e) => {
    e.preventDefault();
    const f = new FormData(e.target);
    const body = { title: f.get("title"), content: f.get("content"), categoryId: +f.get("categoryId") };
    await api(post ? "/api/posts/" + post.id : "/api/posts", { method: post ? "PUT" : "POST", body });
    notify(post ? "Post updated" : "Post created"); renderPosts();
  });
}

/* ---------- categories ---------- */
const renderCategories = guard(async () => {
  const cats = (await api("/api/categories")).data;
  $("#view").innerHTML = `
    <h2>Categories</h2>
    <p class="meta">Served from an in-memory cache after the first request. Check the "Last request" time below when you reload.</p>
    ${isAdmin() ? `<form class="panel form" id="cat-form"><label>Name<input name="name" required maxlength="60"></label>
      <label>Description<input name="description" maxlength="255"></label>
      <div><button class="btn primary" type="submit">Add category</button></div></form>` : ""}
    <div class="panel table-wrap"><table><thead><tr><th>Name</th><th>Description</th>${isAdmin() ? "<th></th>" : ""}</tr></thead><tbody>
      ${cats.map((c) => `<tr><td>${esc(c.name)}</td><td>${esc(c.description)}</td>
        ${isAdmin() ? `<td><button class="btn small danger" data-delcat="${c.id}">Delete</button></td>` : ""}</tr>`).join("")}
    </tbody></table></div>`;
  const form = $("#cat-form");
  if (form) form.onsubmit = guard(async (e) => {
    e.preventDefault(); const f = new FormData(form);
    await api("/api/categories", { method: "POST", body: { name: f.get("name"), description: f.get("description") } });
    notify("Category added"); renderCategories();
  });
  $$("[data-delcat]").forEach((b) => b.onclick = guard(async () => {
    if (!confirm("Delete this category?")) return;
    await api("/api/categories/" + b.dataset.delcat, { method: "DELETE" });
    notify("Category deleted"); renderCategories();
  }));
});

/* ---------- users (admin) ---------- */
const renderUsers = guard(async () => {
  const res = (await api("/api/users?page=0&size=50&sort=username,asc")).data;
  $("#view").innerHTML = `
    <h2>Users</h2>
    <p class="meta">Phone numbers are stored AES-GCM encrypted and shown masked.</p>
    <div class="panel table-wrap"><table><thead><tr><th>Username</th><th>Email</th><th>Phone</th><th>Role</th><th></th></tr></thead><tbody>
      ${res.content.map((u) => `<tr><td>${esc(u.username)}</td><td>${esc(u.email)}</td><td>${esc(u.phone)}</td>
        <td><select data-role="${u.id}">${["USER", "ADMIN"].map((r) => `<option ${r === u.role ? "selected" : ""}>${r}</option>`).join("")}</select></td>
        <td><button class="btn small danger" data-deluser="${u.id}">Delete</button></td></tr>`).join("")}
    </tbody></table></div>`;
  $$("[data-role]").forEach((s) => s.onchange = guard(async () => {
    await api(`/api/users/${s.dataset.role}/role`, { method: "PATCH", body: { role: s.value } });
    notify("Role updated");
  }));
  $$("[data-deluser]").forEach((b) => b.onclick = guard(async () => {
    if (!confirm("Delete this user and all their posts?")) return;
    await api("/api/users/" + b.dataset.deluser, { method: "DELETE" });
    notify("User deleted"); renderUsers();
  }));
});

/* ---------- performance (admin) ---------- */
const renderPerf = guard(async () => {
  $("#view").innerHTML = `<h2>Performance</h2>
    <p class="meta">Runs the same query two ways on the server and reports the real number of SQL statements and time.</p>
    <button class="btn primary" id="run-bench">Run comparison</button><div id="bench-out" style="margin-top:1rem"></div>`;
  $("#run-bench").onclick = guard(async () => {
    const r = (await api("/api/benchmark/posts?size=20")).data;
    const card = (x) => `<div class="panel"><div class="meta">${esc(x.strategy)}</div>
      <div class="big">${x.sqlStatements} SQL statements</div><div>${x.executionTimeMs} ms · ${x.postsReturned} posts</div></div>`;
    $("#bench-out").innerHTML = `<div class="two-col">${card(r.withoutOptimization)}${card(r.withJoinFetch)}</div><p class="meta">${esc(r.note)}</p>`;
  });
});

/* ---------- boot ---------- */
(async function init() {
  $("#docs-link").href = API + "/swagger-ui.html";
  const status = $("#server-status");
  try {
    const res = await fetch(API + "/actuator/health");
    status.className = "status " + (res.ok ? "up" : "down");
    status.lastElementChild.textContent = res.ok ? "Server online" : "Server unavailable";
  } catch {
    status.className = "status down"; status.lastElementChild.textContent = "Server unreachable";
  }
  if (session.access && session.user) showApp();
})();
