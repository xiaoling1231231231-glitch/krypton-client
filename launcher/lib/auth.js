const crypto = require('crypto');
const https = require('https');
const paths = require('./paths');

const MS_CLIENT_ID = 'c36a9fb6-4f2a-41ff-90bd-ae7cc92031eb'; // Prism Launcher's registered device-code client id (public client flow)
const MS_SCOPE = 'XboxLive.signin offline_access';

function httpJson(url, method, body, bearer) {
  return new Promise((resolve, reject) => {
    const u = new URL(url);
    const data = body ? JSON.stringify(body) : null;
    const req = https.request({
      hostname: u.hostname,
      path: u.pathname + u.search,
      method: method || 'GET',
      headers: {
        'Content-Type': 'application/json',
        Accept: 'application/json',
        ...(bearer ? { Authorization: 'Bearer ' + bearer } : {}),
        ...(data ? { 'Content-Length': Buffer.byteLength(data) } : {}),
      },
    }, (res) => {
      let d = '';
      res.on('data', (c) => (d += c));
      res.on('end', () => {
        try { resolve(JSON.parse(d)); }
        catch { resolve({}); }
      });
    });
    req.on('error', reject);
    if (data) req.write(data);
    req.end();
  });
}

function httpForm(url, body) {
  return new Promise((resolve, reject) => {
    const u = new URL(url);
    const data = new URLSearchParams(body).toString();
    const req = https.request({
      hostname: u.hostname,
      path: u.pathname,
      method: 'POST',
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded',
        'Content-Length': Buffer.byteLength(data),
      },
    }, (res) => {
      let d = '';
      res.on('data', (c) => (d += c));
      res.on('end', () => {
        try { resolve(JSON.parse(d)); }
        catch { resolve({}); }
      });
    });
    req.on('error', reject);
    req.write(data);
    req.end();
  });
}

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

// ---- Microsoft device-code OAuth flow ----

async function msStart() {
  const res = await httpForm('https://login.microsoftonline.com/consumers/oauth2/v2.0/devicecode', {
    client_id: MS_CLIENT_ID,
    scope: MS_SCOPE,
  });
  if (res.error) throw new Error('ms device code failed: ' + res.error_description);
  return res; // device_code, user_code, verification_uri, expires_in, interval, message
}

async function msPoll(deviceCode) {
  const res = await httpForm('https://login.microsoftonline.com/consumers/oauth2/v2.0/token', {
    client_id: MS_CLIENT_ID,
    grant_type: 'urn:ietf:params:oauth:grant-type:device_code',
    device_code: deviceCode,
  });
  if (res.error) return { pending: true, error: res.error };
  // access_token, refresh_token, expires_in
  const minecraft = await xboxFlow(res.access_token);
  return { pending: false, ...minecraft };
}

async function xboxFlow(msAccessToken) {
  // 1) XBL token
  const xbl = await httpJson('https://user.auth.xboxlive.com/user/authenticate', 'POST', {
    Properties: { AuthMethod: 'RDP', SiteName: 'user.auth.xboxlive.com', RpsTicket: `d=${msAccessToken}` },
    RelyingParty: 'http://auth.xboxlive.com',
    TokenType: 'JWT',
  });
  if (!xbl.Token) throw new Error('xbl auth failed');

  // 2) XSTS token
  const xsts = await httpJson('https://xsts.auth.xboxlive.com/xsts/authorize', 'POST', {
    Properties: { SandboxId: 'RETAIL', UserTokens: [xbl.Token] },
    RelyingParty: 'rp://api.minecraftservices.com/',
    TokenType: 'JWT',
  });
  if (!xsts.Token) throw new Error('xsts auth failed (check account age)');
  const uhs = xsts.DisplayClaims.xui[0].uhs;

  // 3) Minecraft login_with_xbox
  const ms = await httpJson('https://api.minecraftservices.com/authentication/login_with_xbox', 'POST', {
    identityToken: `XBL3.0 x=${uhs};${xsts.Token}`,
  });
  if (!ms.access_token) throw new Error('minecraft login failed');

  // 4) Profile
  const prof = await httpJson('https://api.minecraftservices.com/minecraft/profile', 'GET', null, ms.access_token);
  if (!prof.id) throw new Error('no minecraft profile on account');

  return {
    type: 'msa',
    username: prof.name,
    uuid: prof.id,
    accessToken: ms.access_token,
    displayName: prof.name,
    expiresIn: ms.expires_in,
  };
}

function addAccount(cfg, account) {
  const acc = { type: account.type || 'offline', username: account.username, uuid: account.uuid, accessToken: account.accessToken, displayName: account.displayName };
  const existing = (cfg.accounts || []).filter((a) => !(a.username === acc.username && a.type === acc.type));
  cfg.accounts = [...existing, acc];
  cfg.selectedAccount = acc.username;
  paths.saveConfig(cfg);
  return acc;
}

function addOfflineAccount(cfg, username) {
  return addAccount(cfg, offlineAccount(username));
}

module.exports = { offlineAccount, addAccount, addOfflineAccount, msStart, msPoll };