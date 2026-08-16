const https = require('https');

const MOJANG_MANIFEST = 'https://piston-meta.mojang.com/mc/game/version_manifest_v2.json';
const FABRIC_LOADER_META = 'https://meta.fabricmc.net/v2/versions/loader';
const FABRIC_GAME_META = 'https://meta.fabricmc.net/v2/versions/game';

function get(url) {
  return new Promise((resolve, reject) => {
    https.get(url, (res) => {
      if (res.statusCode >= 300 && res.statusCode < 400 && res.headers.location) {
        return get(res.headers.location).then(resolve, reject);
      }
      if (res.statusCode !== 200) {
        res.resume();
        return reject(new Error(`GET ${url} -> ${res.statusCode}`));
      }
      let data = '';
      res.setEncoding('utf8');
      res.on('data', (c) => (data += c));
      res.on('end', () => {
        try { resolve(JSON.parse(data)); } catch (e) { reject(e); }
      });
    }).on('error', reject);
  });
}

async function getVersionManifest() {
  const m = await get(MOJANG_MANIFEST);
  const versions = m.versions.map((v) => ({
    id: v.id,
    type: v.type,
    releaseTime: v.releaseTime,
    url: v.url,
  }));
  const latest = m.latest;
  return { versions, latest };
}

// Returns fabric-supported game versions
async function getFabricGameVersions() {
  const v = await get(FABRIC_GAME_META);
  return v.map((g) => g.version);
}

async function getFabricLoaders() {
  const v = await get(FABRIC_LOADER_META);
  return v.map((l) => l.version);
}

// Get fabric launcher meta for a game version + loader
async function getFabricMeta(gameVersion, loaderVersion) {
  return get(`https://meta.fabricmc.net/v2/versions/loader/${encodeURIComponent(gameVersion)}/${encodeURIComponent(loaderVersion)}/profile/json`);
}

// Resolve the newest stable loader + its matching intermediary for a game version
async function getLatestStableLoader(gameVersion) {
  // Legacy Fabric for 1.8.9 and other pre-1.14 versions
  try {
    const legacyEntries = await get(`https://meta.legacyfabric.net/v2/versions/loader/${encodeURIComponent(gameVersion)}`);
    const stable = legacyEntries.find((e) => e.loader.stable);
    if (stable) {
      return { loader: stable.loader.version, intermediary: stable.intermediary.version, legacy: true };
    }
  } catch {}
  const entries = await get(`https://meta.fabricmc.net/v2/versions/loader/${encodeURIComponent(gameVersion)}`);
  const stable = entries.find((e) => e.loader.stable);
  if (!stable) return null;
  return { loader: stable.loader.version, intermediary: stable.intermediary.version, legacy: false };
}

async function getFabricMetaFor(gameVersion, stable) {
  const base = stable.legacy ? 'https://meta.legacyfabric.net/v2/versions/loader' : 'https://meta.fabricmc.net/v2/versions/loader';
  return get(`${base}/${encodeURIComponent(gameVersion)}/${encodeURIComponent(stable.loader)}/profile/json`);
}

async function getFabricGameVersions() {
  const results = {};
  try {
    const modern = await get(FABRIC_GAME_META);
    results.modern = modern.map((g) => g.version);
  } catch { results.modern = []; }
  try {
    const legacy = await get('https://meta.legacyfabric.net/v2/versions/game');
    results.legacy = legacy.map((g) => g.version);
  } catch { results.legacy = []; }
  return results;
}

module.exports = { get, getVersionManifest, getFabricGameVersions, getFabricLoaders, getFabricMeta, getFabricMetaFor, getLatestStableLoader };