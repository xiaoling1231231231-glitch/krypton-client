const crypto = require('crypto');
const paths = require('./paths');

function offlineAccount(username) {
  const uuid = '00000000-0000-0000-0000-' + crypto.createHash('md5').update(username).digest('hex').slice(0, 12);
  return {
    type: 'offline',
    username,
    uuid,
    accessToken: '0',
    displayName: username,
  };
}

// Microsoft OAuth is a browser flow; we keep an offline-capable profile system
// so the client works out of the box, and leave a clear seam for MS auth.
function createAccount({ username, type = 'offline' }) {
  if (type === 'offline') return offlineAccount(username);
  return offlineAccount(username); // ms auth stub
}

function addAccount(cfg, username) {
  const acc = createAccount({ username });
  const existing = (cfg.accounts || []).filter((a) => a.username !== username);
  cfg.accounts = [...existing, acc];
  cfg.selectedAccount = username;
  paths.saveConfig(cfg);
  return acc;
}

module.exports = { offlineAccount, createAccount, addAccount };