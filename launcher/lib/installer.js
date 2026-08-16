const fs = require('fs');
const path = require('path');
const https = require('https');
const paths = require('./paths');

function download(url, dest) {
  return new Promise((resolve, reject) => {
    fs.mkdirSync(path.dirname(dest), { recursive: true });
    https.get(url, (res) => {
      if (res.statusCode >= 300 && res.statusCode < 400 && res.headers.location) {
        res.resume();
        return download(res.headers.location, dest).then(resolve, reject);
      }
      if (res.statusCode !== 200) {
        res.resume();
        return reject(new Error(`GET ${url} -> ${res.statusCode}`));
      }
      const file = fs.createWriteStream(dest);
      res.pipe(file);
      file.on('finish', () => file.close(() => resolve(dest)));
      file.on('error', reject);
    }).on('error', reject);
  });
}

// Resolve maven path from a library name like "group:artifact:version"
function mavenPath(name) {
  const parts = name.split(':');
  if (parts.length < 3) return null;
  const [group, artifact, version] = parts;
  const groupPath = group.split('.').join('/');
  return `${groupPath}/${artifact}/${version}/${artifact}-${version}.jar`;
}

function resolveLibrary(lib, libsDir) {
  const artifact = lib.downloads && (lib.downloads.artifact || lib.downloads.classifiers && lib.downloads.classifiers['sources']);
  if (artifact) {
    const dest = path.join(libsDir, artifact.path);
    return { url: artifact.url, dest, sha1: artifact.sha1 };
  }
  // Fabric-style: name + custom maven url, no downloads key
  if (lib.name && lib.url) {
    const rel = mavenPath(lib.name);
    if (rel) {
      return { url: lib.url.replace(/\/+$/, '') + '/' + rel, dest: path.join(libsDir, rel) };
    }
  }
  return null;
}

function downloadLib(lib, libsDir) {
  const r = resolveLibrary(lib, libsDir);
  if (!r) return Promise.resolve();
  if (fs.existsSync(r.dest)) return Promise.resolve();
  return download(r.url, r.dest);
}

function parseRules(rules) {
  if (!rules) return true;
  let allowed = true;
  for (const rule of rules) {
    if (rule.features) { allowed = false; continue; }
    if (rule.action === 'allow') {
      if (!rule.os || !rule.os.name) { allowed = true; continue; }
      if (rule.os.name === 'osx') allowed = true;
    } else if (rule.action === 'disallow') {
      if (rule.os && rule.os.name === 'osx') allowed = false;
    }
  }
  return allowed;
}

function downloadAssets(assetsObj, assetsDir) {
  const indexUrl = assetsObj.url;
  return download(indexUrl, path.join(assetsDir, 'indexes', `${assetsObj.id}.json`))
    .then(() => {
      const index = JSON.parse(fs.readFileSync(path.join(assetsDir, 'indexes', `${assetsObj.id}.json`), 'utf8'));
      const objects = Object.entries(index.objects);
      return downloadInParallel(objects.map(([name, o]) => {
        const dest = path.join(assetsDir, 'objects', o.hash.slice(0, 2), o.hash);
        if (fs.existsSync(dest)) return null;
        return () => download(`https://resources.download.minecraft.net/${o.hash.slice(0, 2)}/${o.hash}`, dest);
      }), 8);
    });
}

async function downloadInParallel(fns, concurrency) {
  const active = [];
  const queue = fns.filter(Boolean);
  let i = 0;
  const next = () => {
    if (i >= queue.length) return Promise.resolve();
    const fn = queue[i++];
    return fn().catch(() => {}).then(next);
  };
  for (let n = 0; n < Math.min(concurrency, queue.length); n++) active.push(next());
  await Promise.all(active);
}

async function installVersion(versionJson, libsDir, versionsDir) {
  const vdir = path.join(versionsDir, versionJson.id);
  fs.mkdirSync(vdir, { recursive: true });
  const jarDest = path.join(vdir, `${versionJson.id}.jar`);
  if (!fs.existsSync(jarDest)) {
    const dl = versionJson.downloads.client;
    await download(dl.url, jarDest);
  }
  fs.writeFileSync(path.join(vdir, `${versionJson.id}.json`), JSON.stringify(versionJson, null, 2));
  // Libraries (both vanilla `downloads` libs and fabric maven-style libs)
  const libs = versionJson.libraries
    .filter((l) => l.rules ? parseRules(l.rules) : true);
  await downloadInParallel(libs.map((l) => () => downloadLib(l, libsDir)), 8);
  // Assets
  await downloadAssets(versionJson.assetIndex, paths.getAssetsDir());
  return jarDest;
}

async function installFabricApi(modsDir, mcVersion) {
  // Resolve fabric-api version for the game version via Modrinth
  const url = `https://api.modrinth.com/v2/project/fabric-api/version?game_versions=["${mcVersion}"]&loaders=["fabric"]`;
  return new Promise((resolve, reject) => {
    https.get(url, (res) => {
      let data = '';
      res.on('data', (c) => (data += c));
      res.on('end', () => {
        try {
          const versions = JSON.parse(data);
          if (!versions.length) return resolve(false);
          const best = versions[0];
          const file = best.files.find((f) => f.primary);
          const dest = path.join(modsDir, `fabric-api-${best.version_number}.jar`);
          if (fs.existsSync(dest)) return resolve(true);
          download(file.url, dest).then(() => resolve(true)).catch(reject);
        } catch (e) { reject(e); }
      });
    }).on('error', reject);
  });
}

module.exports = { installVersion, installFabricApi, download, downloadInParallel, downloadLib, resolveLibrary };