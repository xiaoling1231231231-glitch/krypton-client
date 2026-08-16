const http = require('http');
const fs = require('fs');
const path = require('path');
const paths = require('./lib/paths');
const manifest = require('./lib/manifest');
const installer = require('./lib/installer');
const auth = require('./lib/auth');
const launch = require('./lib/launch');

const PORT = process.env.PORT || 5757;
paths.ensureDirs();

let versionJsonCache = {};
let activeProcess = null;

function json(res, code, data) {
  res.writeHead(code, { 'Content-Type': 'application/json' });
  res.end(JSON.stringify(data));
}

const server = http.createServer(async (req, res) => {
  const url = new URL(req.url, `http://localhost:${PORT}`);
  const p = url.pathname;

  try {
    // ---- Static UI ----
    if (req.method === 'GET' && !p.startsWith('/api/')) {
      let file = p === '/' ? 'index.html' : p.slice(1);
      const safe = path.normalize(file).replace(/^(\.\.(\/|\\|$))+/, '');
      const fp = path.join(__dirname, 'public', safe);
      if (fs.existsSync(fp) && fs.statSync(fp).isFile()) {
        const ext = path.extname(fp);
        const types = { '.html': 'text/html', '.js': 'text/javascript', '.css': 'text/css', '.svg': 'image/svg+xml', '.png': 'image/png', '.ico': 'image/x-icon' };
        res.writeHead(200, { 'Content-Type': types[ext] || 'application/octet-stream' });
        return res.end(fs.readFileSync(fp));
      }
      res.writeHead(404); return res.end('not found');
    }

    // ---- API ----
    if (p === '/api/config' && req.method === 'GET') {
      return json(res, 200, paths.loadConfig());
    }

    if (p === '/api/config' && req.method === 'POST') {
      let body = '';
      req.on('data', (c) => (body += c));
      req.on('end', () => {
        const cfg = { ...paths.loadConfig(), ...JSON.parse(body) };
        paths.saveConfig(cfg);
        json(res, 200, cfg);
      });
      return;
    }

    if (p === '/api/versions' && req.method === 'GET') {
      const m = await manifest.getVersionManifest();
      let fabric = [];
      try {
        const fg = await manifest.getFabricGameVersions();
        fabric = [...fg.modern, ...fg.legacy];
      } catch {}
      return json(res, 200, { minecraft: m, fabric });
    }

    if (p === '/api/account/add' && req.method === 'POST') {
      let body = '';
      req.on('data', (c) => (body += c));
      req.on('end', () => {
        const { username } = JSON.parse(body);
        const cfg = paths.loadConfig();
        const acc = auth.addOfflineAccount(cfg, username);
        json(res, 200, acc);
      });
      return;
    }

    // Microsoft OAuth device-code flow
    if (p === '/api/auth/ms/start' && req.method === 'POST') {
      try {
        const flow = await auth.msStart();
        return json(res, 200, {
          deviceCode: flow.device_code,
          userCode: flow.user_code,
          verificationUri: flow.verification_uri,
          expiresIn: flow.expires_in,
          interval: flow.interval,
          message: flow.message,
        });
      } catch (e) {
        return json(res, 500, { error: e.message });
      }
    }

    if (p === '/api/auth/ms/poll' && req.method === 'POST') {
      let body = '';
      req.on('data', (c) => (body += c));
      req.on('end', async () => {
        const { deviceCode } = JSON.parse(body);
        try {
          const result = await auth.msPoll(deviceCode);
          if (result.pending) return json(res, 200, { pending: true, error: result.error });
          const cfg = paths.loadConfig();
          const acc = auth.addAccount(cfg, result);
          return json(res, 200, { pending: false, account: acc });
        } catch (e) {
          return json(res, 500, { error: e.message });
        }
      });
      return;
    }

    if (p === '/api/install' && req.method === 'POST') {
      let body = '';
      req.on('data', (c) => (body += c));
      req.on('end', async () => {
        const { mcVersion, fabric } = JSON.parse(body);
        try {
          const m = await manifest.getVersionManifest();
          const v = m.versions.find((x) => x.id === mcVersion);
          if (!v) return json(res, 404, { error: 'version not found' });

          let versionJson;
          if (fabric) {
            const stable = await manifest.getLatestStableLoader(mcVersion);
            if (!stable) return json(res, 400, { error: `Fabric does not support ${mcVersion}` });
            const fabricProfile = await manifest.getFabricMetaFor(mcVersion, stable);
            versionJson = await fetchJson(v.url); // vanilla, for install
            const merged = mergeProfile(fabricProfile, versionJson);
            versionJsonCache[mcVersion] = merged;
            await installer.installVersion(versionJson, paths.getLibsDir(), paths.getVersionsDir());
            // write merged json so re-launches after restart work
            fs.mkdirSync(path.join(paths.getVersionsDir(), mcVersion), { recursive: true });
            fs.writeFileSync(path.join(paths.getVersionsDir(), mcVersion, `${mcVersion}.json`), JSON.stringify(merged, null, 2));
            await installer.installFabricApi(paths.getVersionModsDir(mcVersion), mcVersion);
            installKryptonMod(mcVersion);
          } else {
            versionJson = await fetchJson(v.url);
            versionJsonCache[mcVersion] = versionJson;
            await installer.installVersion(versionJson, paths.getLibsDir(), paths.getVersionsDir());
          }
          return json(res, 200, { installed: mcVersion });
        } catch (e) {
          return json(res, 500, { error: e.message });
        }
      });
      return;
    }

    if (p === '/api/mods' && req.method === 'GET') {
      const modsDir = paths.getModsDir();
      const files = fs.existsSync(modsDir)
        ? fs.readdirSync(modsDir).filter((f) => f.endsWith('.jar')).map((f) => ({ name: f, size: fs.statSync(path.join(modsDir, f)).size }))
        : [];
      return json(res, 200, { path: modsDir, files });
    }

    if (p === '/api/open-mods' && req.method === 'GET') {
      const { exec } = require('child_process');
      exec(`open "${paths.getModsDir()}"`);
      return json(res, 200, {});
    }

    if (p === '/api/launch' && req.method === 'POST') {
      let body = '';
      req.on('data', (c) => (body += c));
      req.on('end', () => {
        const { mcVersion, username, memory, width, height } = JSON.parse(body);
        try {
          const cfg = paths.loadConfig();
          const versionJson = versionJsonCache[mcVersion] ||
            (fs.existsSync(path.join(paths.getVersionsDir(), mcVersion, `${mcVersion}.json`))
              ? JSON.parse(fs.readFileSync(path.join(paths.getVersionsDir(), mcVersion, `${mcVersion}.json`), 'utf8'))
              : null);
          if (!versionJson) return json(res, 400, { error: 'version not installed' });
          stageMods(mcVersion);

          // Use the selected account if it exists (MSA or offline), else fall back.
          let account = null;
          if (username) {
            account = (cfg.accounts || []).find((a) => a.username === username);
          }
          if (!account && cfg.selectedAccount) {
            account = (cfg.accounts || []).find((a) => a.username === cfg.selectedAccount);
          }
          if (!account) account = auth.offlineAccount(username || 'KryptonUser');

          // Remember the last version for the CLI/UI.
          paths.saveConfig({ ...cfg, lastVersion: mcVersion });

          const gameDir = paths.getGameDir();
          const child = launch.launch({
            versionJson,
            account,
            gameDir,
            memory: memory || cfg.memory || 2048,
            width: width || 854,
            height: height || 480,
            javaPath: cfg.javaPath,
            onLog: (line) => console.log('[game]', line.trim()),
            onExit: (code) => { activeProcess = null; console.log(`[game] exited ${code}`); },
          });
          activeProcess = child;
          json(res, 200, { launched: true, pid: child.pid });
        } catch (e) {
          json(res, 500, { error: e.message });
        }
      });
      return;
    }

    json(res, 404, { error: 'not found' });
  } catch (e) {
    json(res, 500, { error: e.message });
  }
});

// Copy the version's mod jars into the global mods folder Fabric actually reads.
function stageMods(mcVersion) {
  try {
    const src = paths.getVersionModsDir(mcVersion);
    const dst = paths.getModsDir();
    fs.mkdirSync(dst, { recursive: true });
    for (const f of fs.readdirSync(dst)) {
      if (f.endsWith('.jar')) fs.unlinkSync(path.join(dst, f));
    }
    if (fs.existsSync(src)) {
      for (const f of fs.readdirSync(src)) {
        if (f.endsWith('.jar')) fs.copyFileSync(path.join(src, f), path.join(dst, f));
      }
    }
  } catch (e) {
    console.log('[krypton] staging mods failed:', e.message);
  }
}

async function fetchJson(url) {
  const { get } = require('./lib/manifest');
  return get(url);
}

// Copy the built Krypton mod jar into the version's mods folder so Fabric loads it.
function installKryptonMod(mcVersion) {
  try {
    const repoMod = path.join(__dirname, '..', 'mod', 'build', 'libs', 'krypton-client-1.0.0.jar');
    if (!fs.existsSync(repoMod)) return;
    const dest = path.join(paths.getVersionModsDir(mcVersion), 'krypton-client.jar');
    fs.copyFileSync(repoMod, dest);
    console.log(`[krypton] installed mod jar for ${mcVersion} to`, dest);
  } catch (e) {
    console.log('[krypton] mod install failed:', e.message);
  }
}

function mergeProfile(fabricProfile, vanillaJson) {
  const gameArgs = (fabricProfile.arguments && fabricProfile.arguments.game && fabricProfile.arguments.game.length)
    ? fabricProfile.arguments.game
    : ((vanillaJson.arguments && vanillaJson.arguments.game) || []);
  const jvmArgs = (fabricProfile.arguments && fabricProfile.arguments.jvm && fabricProfile.arguments.jvm.length)
    ? fabricProfile.arguments.jvm
    : ((vanillaJson.arguments && vanillaJson.arguments.jvm) || []);
  return {
    ...fabricProfile,
    id: vanillaJson.id,
    inheritsFrom: fabricProfile.inheritsFrom,
    client: fabricProfile.client || vanillaJson.client,
    downloads: fabricProfile.downloads || vanillaJson.downloads,
    assetIndex: fabricProfile.assetIndex || vanillaJson.assetIndex,
    arguments: { game: gameArgs, jvm: jvmArgs },
    minecraftArguments: fabricProfile.minecraftArguments || vanillaJson.minecraftArguments,
    libraries: [...(vanillaJson.libraries || []), ...(fabricProfile.libraries || [])],
  };
}

server.listen(PORT, () => {
  console.log(`Krypton Client launcher running at http://localhost:${PORT}`);
  console.log(`Game directory: ${paths.getGameDir()}`);
});