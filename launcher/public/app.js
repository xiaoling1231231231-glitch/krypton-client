const api = (p, opts) => fetch(p, opts).then((r) => r.json().catch(() => ({})));

let state = {
  minecraft: [],
  fabric: [],
  selected: null,
  config: { accounts: [], memory: 2048, width: 854, height: 480 },
};

const $ = (id) => document.getElementById(id);

async function init() {
  bindTabs();
  state.config = await api('/api/config');
  state.selected = state.config.lastVersion || null;
  $('memory-input').value = state.config.memory || 2048;
  $('width-input').value = state.config.width || 854;
  $('height-input').value = state.config.height || 480;
  $('java-input').value = state.config.javaPath || '';
  renderAccounts();
  loadVersions();
  loadMods();
  bindSettings();
}

function bindTabs() {
  document.querySelectorAll('.nav-btn').forEach((btn) => {
    btn.addEventListener('click', () => {
      document.querySelectorAll('.nav-btn').forEach((b) => b.classList.remove('active'));
      document.querySelectorAll('.tab').forEach((t) => t.classList.remove('active'));
      btn.classList.add('active');
      $('tab-' + btn.dataset.tab).classList.add('active');
    });
  });
}

async function loadVersions() {
  const data = await api('/api/versions');
  state.minecraft = data.minecraft.versions;
  state.fabric = data.fabric || [];
  renderVersionList();

  $('version-search').addEventListener('input', renderVersionList);
  $('fabric-toggle').addEventListener('change', renderVersionList);
  if (state.selected) updateSelectedDisplay();
}

function renderVersionList() {
  const q = $('version-search').value.toLowerCase();
  const fabricOnly = $('fabric-toggle').checked;
  const list = $('version-list');
  list.innerHTML = '';
  let versions = state.minecraft;
  if (fabricOnly) versions = versions.filter((v) => state.fabric.includes(v.id));
  versions = versions.filter((v) => v.id.toLowerCase().includes(q));
  const matched = versions.slice(0, 300);

  const releases = matched.filter((v) => v.type === 'release');
  const snapshots = matched.filter((v) => v.type !== 'release');

  if (releases.length) {
    list.appendChild(header('Releases'));
    releases.forEach((v) => list.appendChild(item(v)));
  }
  if (snapshots.length) {
    list.appendChild(header('Snapshots & others'));
    snapshots.forEach((v) => list.appendChild(item(v)));
  }
  if (!matched.length) list.innerHTML = '<p class="muted">No versions found.</p>';
}

function header(label) {
  const el = document.createElement('div');
  el.className = 'list-header';
  el.textContent = label;
  return el;
}

function item(v) {
  const fabricSupported = state.fabric.includes(v.id);
  const el = document.createElement('div');
  el.className = 'version-item' + (state.selected === v.id ? ' selected' : '');
  el.innerHTML = `
    <div class="vname">${escapeHtml(v.id)}</div>
    <div class="vtype">${v.type} • ${v.releaseTime.slice(0, 10)}</div>
    ${fabricSupported ? '<span class="vbadge">Fabric ✓</span>' : ''}`;
  el.addEventListener('click', () => {
    state.selected = v.id;
    updateSelectedDisplay(fabricSupported);
    renderVersionList();
  });
  return el;
}

function updateSelectedDisplay(fabricSupported) {
  const v = state.minecraft.find((x) => x.id === state.selected);
  if (!v) return;
  const fab = fabricSupported !== undefined ? fabricSupported : state.fabric.includes(v.id);
  $('selected-version').textContent = v.id;
  $('selected-version-detail').textContent = `${v.type} • ${fab ? 'Fabric supported' : 'Vanilla'}`;
  $('play-btn').disabled = false;
}

function renderAccounts() {
  const sel = $('account-select');
  sel.innerHTML = '';
  const accounts = state.config.accounts || [];
  accounts.forEach((a) => {
    const o = document.createElement('option');
    o.value = a.username;
    o.textContent = `${a.displayName} (${a.type === 'msa' ? 'Microsoft' : 'Offline'})`;
    sel.appendChild(o);
  });
  if (accounts.length && !accounts.find((a) => a.username === sel.value)) {
    const selAcc = state.config.selectedAccount;
    if (selAcc) sel.value = selAcc;
  }
}

function bindSettings() {
  $('add-account-btn').addEventListener('click', async () => {
    const name = $('new-account-name').value.trim();
    if (!name) return;
    await api('/api/account/add', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ username: name }) });
    state.config = await api('/api/config');
    renderAccounts();
    $('new-account-name').value = '';
  });

  $('ms-login-btn').addEventListener('click', msLogin);

  $('save-settings-btn').addEventListener('click', async () => {
    await api('/api/config', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        memory: parseInt($('memory-input').value) || 2048,
        width: parseInt($('width-input').value) || 854,
        height: parseInt($('height-input').value) || 480,
        javaPath: $('java-input').value.trim(),
      }),
    });
    setStatus('Settings saved', 'done');
  });

  $('open-mods-btn').addEventListener('click', () => {
    fetch('/api/open-mods').catch(() => {});
  });

  $('play-btn').addEventListener('click', play);
}

let msPollTimer = null;

async function msLogin() {
  setStatus('Starting Microsoft sign-in...');
  const res = await api('/api/auth/ms/start', { method: 'POST' });
  if (res.error) return setStatus('MS login failed: ' + res.error, 'err');

  const flow = $('ms-flow');
  flow.classList.remove('hidden');
  $('ms-message').textContent = res.message || 'Sign in with your Microsoft account.';
  $('ms-code').textContent = res.userCode;

  clearInterval(msPollTimer);
  const intervalMs = (res.interval || 5) * 1000;
  msPollTimer = setInterval(async () => {
    const r = await api('/api/auth/ms/poll', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ deviceCode: res.deviceCode }),
    });
    if (r.pending) {
      if (r.error === 'authorization_pending') return;
      if (r.error === 'expired_token') {
        clearInterval(msPollTimer);
        setStatus('Login window expired. Try again.', 'err');
      }
      return;
    }
    clearInterval(msPollTimer);
    flow.classList.add('hidden');
    state.config = await api('/api/config');
    renderAccounts();
    setStatus(`Signed in as ${r.account.displayName}`, 'done');
  }, intervalMs);
}

async function play() {
  const mcVersion = state.selected;
  if (!mcVersion) return;
  const fabric = $('fabric-toggle').checked;
  const username = $('account-select').value || 'KryptonUser';
  await api('/api/config', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ lastVersion: mcVersion }),
  });
  setStatus(`Installing ${mcVersion}${fabric ? ' + Fabric' : ''}...`);
  const inst = await api('/api/install', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ mcVersion, fabric }) });
  if (inst.error) return setStatus('Install failed: ' + inst.error, 'err');
  setStatus('Installed. Launching...');
  const mem = parseInt($('memory-input').value) || 2048;
  const w = parseInt($('width-input').value) || 854;
  const h = parseInt($('height-input').value) || 480;
  const launch = await api('/api/launch', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ mcVersion, username, memory: mem, width: w, height: h }) });
  if (launch.error) return setStatus('Launch failed: ' + launch.error, 'err');
  setStatus(`Launched (pid ${launch.pid}). Close this window or keep it running — logs show in terminal.`, 'done');
}

async function loadMods() {
  const mods = await api('/api/mods');
  if (mods.path) $('mods-path').textContent = mods.path;
  const list = $('mods-list');
  list.innerHTML = '';
  (mods.files || []).forEach((m) => {
    const el = document.createElement('div');
    el.className = 'mod-file';
    el.innerHTML = `<span>${escapeHtml(m.name)}</span><span class="size">${(m.size / 1024).toFixed(0)} KB</span>`;
    list.appendChild(el);
  });
}

function setStatus(msg, cls) {
  const el = $('status');
  el.className = 'status' + (cls ? ' ' + cls : '');
  el.textContent = msg;
}

function escapeHtml(s) {
  return s.replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
}

init();