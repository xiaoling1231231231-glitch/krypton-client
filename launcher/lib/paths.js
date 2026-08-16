const os = require('os');
const path = require('path');
const fs = require('fs');

function getLauncherDir() {
  const base = path.join(os.homedir(), 'Library', 'Application Support', 'KryptonClient');
  return base;
}

function getGameDir() {
  return path.join(getLauncherDir(), 'minecraft');
}

function getModsDir() {
  return path.join(getGameDir(), 'mods');
}

// Version-scoped mods so different MC versions can have different mod jars.
function getVersionModsDir(mcVersion) {
  return path.join(getGameDir(), 'mods', mcVersion);
}

function getLibsDir() {
  return path.join(getGameDir(), 'libraries');
}

function getAssetsDir() {
  return path.join(getGameDir(), 'assets');
}

function getVersionsDir() {
  return path.join(getGameDir(), 'versions');
}

function getLogDir() {
  const d = path.join(getLauncherDir(), 'logs');
  return d;
}

function ensureDirs() {
  [getLauncherDir(), getGameDir(), getModsDir(), getLibsDir(), getAssetsDir(), getVersionsDir(), getLogDir()]
    .forEach((d) => fs.mkdirSync(d, { recursive: true }));
}

function saveConfig(cfg) {
  ensureDirs();
  fs.writeFileSync(path.join(getLauncherDir(), 'launcher.json'), JSON.stringify(cfg, null, 2));
}

function loadConfig() {
  const p = path.join(getLauncherDir(), 'launcher.json');
  if (!fs.existsSync(p)) return { accounts: [], selectedAccount: null, javaPath: null, memory: 2048, modsEnabled: true };
  try {
    return JSON.parse(fs.readFileSync(p, 'utf8'));
  } catch {
    return { accounts: [], selectedAccount: null, javaPath: null, memory: 2048, modsEnabled: true };
  }
}

module.exports = { getLauncherDir, getGameDir, getModsDir, getVersionModsDir, getLibsDir, getAssetsDir, getVersionsDir, getLogDir, ensureDirs, saveConfig, loadConfig };