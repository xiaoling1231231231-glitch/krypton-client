const fs = require('fs');
const path = require('path');
const { spawn } = require('child_process');
const paths = require('./paths');

function isJavaRuntime() {
  return typeof window === 'undefined' && typeof require === 'function';
}

function findJava() {
  // Prefer JAVA_HOME, then /usr/libexec/java_home, then which java
  if (process.env.JAVA_HOME) {
    const p = path.join(process.env.JAVA_HOME, 'bin', 'java');
    if (fs.existsSync(p)) return p;
  }
  try {
    const { execSync } = require('child_process');
    const out = execSync('/usr/libexec/java_home 2>/dev/null || true').toString().trim();
    if (out) {
      const p = path.join(out, 'bin', 'java');
      if (fs.existsSync(p)) return p;
    }
  } catch {}
  return 'java';
}

function unzip(file, outDir) {
  const { execSync } = require('child_process');
  fs.mkdirSync(outDir, { recursive: true });
  try { execSync(`unzip -o -q "${file}" -d "${outDir}"`); } catch {}
}

function extractNatives(lib, libsDir, nativesDir) {
  const dl = lib.downloads;
  if (!dl || !dl.classifiers) return null;
  const classifier = dl.classifiers['natives-macos'] || dl.classifiers['natives-osx'];
  if (!classifier) return null;
  const src = path.join(libsDir, classifier.path);
  if (!fs.existsSync(src)) return null;
  unzip(src, nativesDir);
  return src;
}

function buildClasspath(versionJson, libsDir, versionsDir) {
  const entries = [];
  for (const lib of versionJson.libraries || []) {
    if (!lib.rules || allowByRules(lib.rules)) {
      const dl = lib.downloads && lib.downloads.artifact;
      if (dl) {
        const p = path.join(libsDir, dl.path);
        if (fs.existsSync(p)) entries.push(p);
        continue;
      }
      const p = derivePath(lib.name);
      if (p && fs.existsSync(p)) entries.push(p);
    }
  }
  const vdir = path.join(versionsDir, versionJson.id);
  const jar = path.join(vdir, `${versionJson.id}.jar`);
  if (fs.existsSync(jar)) entries.push(jar);
  return entries;
}

function allowByRules(rules) {
  let allowed = false;
  let set = false;
  for (const rule of rules) {
    // Feature-gated args (quick play, demo, etc.) are only added when the
    // launcher explicitly opts in. We enable none, so skip them.
    if (rule.features) {
      allowed = false;
      set = true;
      continue;
    }
    if (rule.action === 'allow') {
      if (!rule.os) { allowed = true; set = true; }
      else if (rule.os.name === 'osx') { allowed = true; set = true; }
    } else if (rule.action === 'disallow') {
      if (!rule.os) { allowed = false; set = true; }
      else if (rule.os.name === 'osx') { allowed = false; set = true; }
    }
  }
  return set ? allowed : true;
}

function derivePath(name) {
  const parts = name.split(':');
  if (parts.length < 3) return null;
  const [group, artifact, version] = parts;
  const groupPath = group.split('.').join('/');
  return path.join(paths.getLibsDir(), groupPath, artifact, version, `${artifact}-${version}.jar`);
}

function buildArgs(versionJson, opts) {
  const { username, uuid, accessToken, versionType = 'release', gameDir, assetsDir } = opts;
  const subs = {
    '${auth_player_name}': username,
    '${auth_uuid}': uuid,
    '${auth_access_token}': accessToken,
    '${version_name}': versionJson.id,
    '${version_type}': versionType,
    '${game_directory}': gameDir,
    '${assets_root}': assetsDir,
    '${assets_index_name}': versionJson.assetIndex.id,
    '${user_type}': 'msa',
    '${resolution_width}': opts.width || 854,
    '${resolution_height}': opts.height || 480,
    '${user_properties}': '{}',
    '${launcher_name}': 'Krypton Client',
    '${launcher_version}': '1.0.0',
    '${natives_directory}': opts.nativesDir,
    '${classpath}': opts.classpath,
    '${library_directory}': paths.getLibsDir(),
    '${clientid}': 'krypton',
  };
  const map = (s) => s.replace(/\$\{[^}]+\}/g, (m) => subs[m] ?? m);
  const gameArgs = versionJson.minecraftArguments
    ? versionJson.minecraftArguments.split(' ')
    : (versionJson.arguments && versionJson.arguments.game) || [];
  const mapped = [];
  for (const a of gameArgs) {
    if (typeof a === 'string') {
      mapped.push(map(a));
    } else if (a && typeof a === 'object' && a.value) {
      if (a.rules && !allowByRules(a.rules)) continue;
      const vals = Array.isArray(a.value) ? a.value : [a.value];
      vals.forEach((vv) => mapped.push(map(vv)));
    }
  }
  return mapped;
}

function launch(opts) {
  const { versionJson, account, gameDir, memory, width, height, javaPath, onLog, onExit } = opts;
  const libsDir = paths.getLibsDir();
  const nativesDir = path.join(gameDir, 'bin', 'natives');
  fs.mkdirSync(nativesDir, { recursive: true });

  for (const lib of versionJson.libraries || []) {
    if (lib.downloads && lib.downloads.classifiers) extractNatives(lib, libsDir, nativesDir);
  }

  const classpath = buildClasspath(versionJson, libsDir, paths.getVersionsDir());
  const args = buildArgs(versionJson, {
    username: account.displayName,
    uuid: account.uuid,
    accessToken: account.accessToken,
    gameDir,
    assetsDir: paths.getAssetsDir(),
    nativesDir,
    classpath: classpath.join(':'),
    width,
    height,
  });

  const javaArgs = [
    `-Xmx${memory}m`,
    '-Xmn128M',
    '-Dfml.ignoreInvalidMinecraftCertificates=true',
    '-Dfml.ignorePatchDiscrepancies=true',
    '-Djava.library.path=' + nativesDir,
    // macOS requires the main thread to be the first thread for GLFW
    ...(process.platform === 'darwin' ? ['-XstartOnFirstThread'] : []),
    '-cp', classpath.join(':'),
  ];
  // 1.17+ uses -cp before main class; older uses GameDir param
  const hasMainClass = !!versionJson.mainClass;
  if (versionJson.minecraftArguments) {
    // 1.12 and below: mainClass gameDir args
    javaArgs.push(versionJson.mainClass || 'net.minecraft.client.main.Main');
    javaArgs.push(...args);
    javaArgs.push(gameDir);
  } else {
    javaArgs.push(versionJson.mainClass || 'net.minecraft.client.main.Main');
    javaArgs.push(...args);
  }

  const java = javaPath || findJava();
  onLog(`Launching java: ${java}\nArgs: ${javaArgs.join(' ')}`);
  const child = spawn(java, javaArgs, { cwd: gameDir, env: { ...process.env, MCDIR: gameDir } });
  child.stdout.on('data', (d) => onLog(d.toString()));
  child.stderr.on('data', (d) => onLog(d.toString()));
  child.on('error', (e) => onLog('ERROR: ' + e.message));
  child.on('exit', (code) => onExit(code));
  return child;
}

module.exports = { launch, buildClasspath, buildArgs, findJava };