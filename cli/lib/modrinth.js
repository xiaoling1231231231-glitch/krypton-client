const fs = require('fs');
const path = require('path');
const https = require('https');

const API = 'https://api.modrinth.com/v2';

function httpGet(url, headers = {}) {
  return new Promise((resolve, reject) => {
    https.get(url, { headers: { 'user-agent': 'krypton-client/1.0.0', ...headers } }, (res) => {
      let data = '';
      res.on('data', (c) => (data += c));
      res.on('end', () => {
        if (res.statusCode !== 200) {
          return reject(new Error(`HTTP ${res.statusCode} for ${url}\n${data.slice(0, 300)}`));
        }
        try { resolve(JSON.parse(data)); } catch (e) { reject(new Error('Bad JSON from Modrinth: ' + e.message)); }
      });
    }).on('error', reject);
  });
}

function httpDownload(url, dest) {
  return new Promise((resolve, reject) => {
    const file = fs.createWriteStream(dest);
    https.get(url, { headers: { 'user-agent': 'krypton-client/1.0.0' } }, (res) => {
      if (res.statusCode !== 200) {
        reject(new Error(`HTTP ${res.statusCode} downloading ${url}`));
        file.close();
        return;
      }
      res.pipe(file);
      file.on('finish', () => { file.close(); resolve(dest); });
    }).on('error', (e) => { file.close(); fs.unlinkSync(dest); reject(e); });
  });
}

async function search(query, limit = 10) {
  const facets = encodeURIComponent(JSON.stringify([['project_type:mod']]));
  return httpGet(`${API}/search?query=${encodeURIComponent(query)}&limit=${limit}&facets=${facets}`);
}

async function getProject(slug) {
  return httpGet(`${API}/project/${encodeURIComponent(slug)}`);
}

async function getVersions(projectId, gameVersion, loader = 'fabric') {
  const q = `game_versions=${encodeURIComponent(JSON.stringify([gameVersion]))}&loaders=${encodeURIComponent(JSON.stringify([loader]))}`;
  return httpGet(`${API}/project/${encodeURIComponent(projectId)}/version?${q}`);
}

module.exports = { API, search, getProject, getVersions, httpDownload };
