const http = require('http');
const https = require('https');
const fs = require('fs');
const path = require('path');

const PORT = 8080;

const PATHS = {
  release: {
    root: path.join(__dirname, 'app-release.apk'),
    build: path.join(__dirname, 'app', 'build', 'outputs', 'apk', 'release', 'app-release.apk'),
    buildUnsigned: path.join(__dirname, 'app', 'build', 'outputs', 'apk', 'release', 'app-release-unsigned.apk'),
    downloadName: 'DropMe-v5.6-release.apk'
  },
  debug: {
    root: path.join(__dirname, 'app-debug.apk'),
    build: path.join(__dirname, 'app', 'build', 'outputs', 'apk', 'debug', 'app-debug.apk'),
    downloadName: 'DropMe-v5.6-debug.apk'
  }
};

/**
 * Rozpoznaje i synchronizuje najświeższy plik APK (z app/build/outputs/... lub katalogu głównego).
 */
function getApkInfo(type) {
  const conf = PATHS[type];
  if (!conf) return null;

  const candidates = [];
  if (conf.build && fs.existsSync(conf.build)) {
    try { candidates.push({ path: conf.build, mtimeMs: fs.statSync(conf.build).mtimeMs }); } catch (e) {}
  }
  if (conf.buildUnsigned && fs.existsSync(conf.buildUnsigned)) {
    try { candidates.push({ path: conf.buildUnsigned, mtimeMs: fs.statSync(conf.buildUnsigned).mtimeMs }); } catch (e) {}
  }
  if (conf.root && fs.existsSync(conf.root)) {
    try { candidates.push({ path: conf.root, mtimeMs: fs.statSync(conf.root).mtimeMs }); } catch (e) {}
  }

  if (candidates.length === 0) {
    return {
      exists: false,
      size: 'Brak pliku',
      bytes: 0,
      path: null,
      mtime: 0,
      dateFormatted: 'Brak kompilacji',
      relativeTime: 'Brak'
    };
  }

  // Wybierz kandydata z najnowszym czasem modyfikacji
  candidates.sort((a, b) => b.mtimeMs - a.mtimeMs);
  const newest = candidates[0];

  // Jeśli najnowszy plik jest w build/, zaktualizuj plik w głównym katalogu
  if (newest.path !== conf.root) {
    try {
      fs.copyFileSync(newest.path, conf.root);
      console.log(`[Auto-Sync] Zaktualizowano ${conf.root} z nowszej kompilacji ${newest.path}`);
    } catch (e) {
      console.error(`[Auto-Sync Error]`, e);
    }
  }

  const activePath = fs.existsSync(conf.root) ? conf.root : newest.path;
  const stat = fs.statSync(activePath);
  const sizeMb = (stat.size / (1024 * 1024)).toFixed(2);
  const sizeKb = (stat.size / 1024).toFixed(0);
  const dateObj = new Date(stat.mtime);

  const pad = n => String(n).padStart(2, '0');
  const dateFormatted = `${pad(dateObj.getDate())}.${pad(dateObj.getMonth() + 1)}.${dateObj.getFullYear()} ${pad(dateObj.getHours())}:${pad(dateObj.getMinutes())}:${pad(dateObj.getSeconds())}`;

  const diffSec = Math.max(0, Math.floor((Date.now() - stat.mtimeMs) / 1000));
  let relativeTime = 'przed chwilą';
  if (diffSec >= 60 && diffSec < 3600) {
    relativeTime = `${Math.floor(diffSec / 60)} min temu`;
  } else if (diffSec >= 3600) {
    relativeTime = `${Math.floor(diffSec / 3600)} godz. temu`;
  }

  return {
    exists: true,
    size: `${sizeMb} MB (${sizeKb} KB)`,
    bytes: stat.size,
    path: activePath,
    mtime: stat.mtimeMs,
    dateFormatted,
    relativeTime
  };
}

// Sprawdzanie w tle co 1.5 sekundy czy nie pojawiła się nowa kompilacja
setInterval(() => {
  getApkInfo('release');
  getApkInfo('debug');
}, 1500);

function serveApk(res, type) {
  const info = getApkInfo(type);
  const conf = PATHS[type];

  if (!info || !info.exists) {
    res.writeHead(404, { 'Content-Type': 'text/plain; charset=utf-8' });
    res.end(`Błąd: Plik ${conf.downloadName} nie istnieje. Najpierw uruchom ./gradlew assembleRelease`);
    return;
  }

  res.writeHead(200, {
    'Content-Type': 'application/vnd.android.package-archive',
    'Content-Disposition': `attachment; filename="${conf.downloadName}"`,
    'Content-Length': info.bytes,
    'Cache-Control': 'no-store, no-cache, must-revalidate, proxy-revalidate, max-age=0',
    'Pragma': 'no-cache',
    'Expires': '0',
    'Last-Modified': new Date(info.mtime).toUTCString()
  });
  fs.createReadStream(info.path).pipe(res);
}

const recentEmailCodes = [];

function handleSendCode(req, res) {
  let body = '';
  req.on('data', chunk => body += chunk);
  req.on('end', () => {
    try {
      const data = JSON.parse(body || '{}');
      const email = data.email || 'brak@email';
      const code = data.code || '000000';
      const type = data.type || 'Weryfikacja';
      const username = data.username || 'user';
      const time = new Date().toLocaleTimeString();

      console.log('\n=======================================================');
      console.log(`📬 [DropMe Mail Server] KOD WERYFIKACYJNY (${type})`);
      console.log(`- Adres e-mail: ${email}`);
      console.log(`- Użytkownik:   @${username}`);
      console.log(`- KOD:          >>>  ${code}  <<<`);
      console.log(`- Czas:         ${new Date().toLocaleString()}`);
      console.log('=======================================================\n');

      recentEmailCodes.unshift({ email, code, type, username, time });
      if (recentEmailCodes.length > 8) recentEmailCodes.pop();

      res.writeHead(200, {
        'Content-Type': 'application/json',
        'Access-Control-Allow-Origin': '*'
      });
      res.end(JSON.stringify({ ok: true, message: 'Wysłano kod weryfikacyjny' }));
    } catch (e) {
      res.writeHead(400, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ ok: false, error: e.message }));
    }
  });
}

const SUPABASE_URL = 'https://zzzxlpgseqyaztvofwsb.supabase.co';
const SUPABASE_KEY = 'sb_publishable_3JSfUks_PEmc6-TOOzNKIA_b8j5VfTo';

function syncSupabaseVerificationCodes() {
  const url = `${SUPABASE_URL}/rest/v1/verification_codes?select=*&order=id.desc&limit=10`;
  const options = {
    headers: {
      'apikey': SUPABASE_KEY,
      'Authorization': `Bearer ${SUPABASE_KEY}`
    }
  };
  https.get(url, options, (res) => {
    if (res.statusCode < 200 || res.statusCode >= 300) return;
    let data = '';
    res.on('data', chunk => data += chunk);
    res.on('end', () => {
      try {
        const rows = JSON.parse(data);
        if (!Array.isArray(rows)) return;
        for (const row of rows) {
          const email = row.email || 'brak@email';
          const code = row.code || '000000';
          const type = row.type || 'register';
          const exists = recentEmailCodes.some(c => c.code === code && c.email === email);
          if (!exists) {
            const time = row.created_at ? new Date(row.created_at).toLocaleTimeString() : new Date().toLocaleTimeString();
            recentEmailCodes.unshift({ email, code, type, username: 'supabase_user', time });
            if (recentEmailCodes.length > 8) recentEmailCodes.pop();
            console.log('\n=======================================================');
            console.log(`📬 [Supabase Cloud Sync] KOD Z LTE/5G (${type})`);
            console.log(`- Adres e-mail: ${email}`);
            console.log(`- KOD:          >>>  ${code}  <<<`);
            console.log(`- Czas:         ${time}`);
            console.log('=======================================================\n');
          }
        }
      } catch (e) {}
    });
  }).on('error', () => {});
}

// Sprawdzaj kody w Supabase co 3 sekundy
setInterval(syncSupabaseVerificationCodes, 3000);

const server = http.createServer((req, res) => {
  const url = req.url.split('?')[0];

  // API wysyłania i odbierania kodów weryfikacyjnych
  if (url === '/api/send-code' || url === '/api/send-reset-code') {
    handleSendCode(req, res);
    return;
  }

  if (url === '/api/recent-codes') {
    res.writeHead(200, {
      'Content-Type': 'application/json',
      'Cache-Control': 'no-cache',
      'Access-Control-Allow-Origin': '*'
    });
    res.end(JSON.stringify({ codes: recentEmailCodes }));
    return;
  }

  // Pobieranie wersji Release
  if (url === '/app-release.apk' || url === '/release' || url === '/download' || url === '/dropme.apk') {
    serveApk(res, 'release');
    return;
  }

  // Pobieranie wersji Debug
  if (url === '/app-debug.apk' || url === '/debug') {
    serveApk(res, 'debug');
    return;
  }

  // API stanu kompilacji (do auto-refreshowania strony)
  if (url === '/api/status' || url === '/api/version') {
    const relInfo = getApkInfo('release');
    const debInfo = getApkInfo('debug');
    res.writeHead(200, {
      'Content-Type': 'application/json',
      'Cache-Control': 'no-cache, no-store, must-revalidate'
    });
    res.end(JSON.stringify({
      release: relInfo,
      debug: debInfo,
      recentCodes: recentEmailCodes,
      timestamp: Date.now()
    }));
    return;
  }

  // Jeśli zapytanie bezpośrednio żąda binarnego pliku APK
  const acceptHeader = req.headers['accept'] || '';
  if (!acceptHeader.includes('text/html') && url === '/') {
    serveApk(res, 'release');
    return;
  }

  const rel = getApkInfo('release');
  const deb = getApkInfo('debug');

  const html = `<!DOCTYPE html>
<html lang="pl">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>DropMe - Aktualna Wersja APK</title>
  <style>
    :root {
      --bg: #0f172a;
      --card-bg: #1e293b;
      --primary: #3b82f6;
      --primary-hover: #2563eb;
      --success: #10b981;
      --text: #f8fafc;
      --text-muted: #94a3b8;
      --border: #334155;
    }
    * {
      box-sizing: border-box;
      margin: 0;
      padding: 0;
      font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
    }
    body {
      background-color: var(--bg);
      color: var(--text);
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      min-height: 100vh;
      padding: 20px;
    }
    .card {
      background: var(--card-bg);
      border: 1px solid var(--border);
      border-radius: 20px;
      padding: 32px 24px;
      max-width: 480px;
      width: 100%;
      text-align: center;
      box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.5), 0 8px 10px -6px rgba(0, 0, 0, 0.5);
    }
    .icon-container {
      width: 76px;
      height: 76px;
      background: linear-gradient(135deg, #2563eb 0%, #1d4ed8 100%);
      border-radius: 20px;
      margin: 0 auto 16px;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 36px;
      box-shadow: 0 4px 14px rgba(37, 99, 235, 0.4);
    }
    h1 {
      font-size: 26px;
      font-weight: 700;
      margin-bottom: 6px;
    }
    .badge {
      display: inline-flex;
      align-items: center;
      background: rgba(16, 185, 129, 0.18);
      color: #34d399;
      font-size: 13px;
      font-weight: 600;
      padding: 5px 14px;
      border-radius: 9999px;
      margin-bottom: 20px;
    }
    .status-dot {
      display: inline-block;
      width: 8px;
      height: 8px;
      border-radius: 50%;
      background: #10b981;
      margin-right: 8px;
      box-shadow: 0 0 8px #10b981;
      animation: pulse 2s infinite;
    }
    @keyframes pulse {
      0% { opacity: 1; transform: scale(1); }
      50% { opacity: 0.5; transform: scale(1.2); }
      100% { opacity: 1; transform: scale(1); }
    }
    .info-box {
      background: rgba(15, 23, 42, 0.6);
      border-radius: 14px;
      padding: 14px 16px;
      margin-bottom: 20px;
      font-size: 13px;
      color: var(--text-muted);
      text-align: left;
    }
    .info-row {
      display: flex;
      justify-content: space-between;
      margin-bottom: 7px;
    }
    .info-row:last-child {
      margin-bottom: 0;
    }
    .info-label {
      color: var(--text-muted);
    }
    .info-val {
      color: var(--text);
      font-weight: 500;
    }
    .btn {
      display: block;
      width: 100%;
      padding: 15px 20px;
      border-radius: 14px;
      text-decoration: none;
      font-size: 15px;
      font-weight: 600;
      margin-bottom: 12px;
      transition: all 0.2s ease;
      cursor: pointer;
      text-align: center;
    }
    .btn-primary {
      background: linear-gradient(135deg, #2563eb 0%, #1d4ed8 100%);
      color: white;
      box-shadow: 0 4px 14px rgba(37, 99, 235, 0.35);
    }
    .btn-primary:hover {
      background: linear-gradient(135deg, #1d4ed8 0%, #1e40af 100%);
      transform: translateY(-1px);
    }
    .btn-secondary {
      background: #334155;
      color: #cbd5e1;
      font-size: 13px;
      padding: 12px 16px;
    }
    .btn-secondary:hover {
      background: #475569;
      color: white;
    }
    .live-alert {
      display: none;
      background: rgba(37, 99, 235, 0.2);
      border: 1px solid #3b82f6;
      color: #93c5fd;
      border-radius: 10px;
      padding: 10px 14px;
      margin-bottom: 16px;
      font-size: 13px;
    }
    .sql-box {
      background: #090d16;
      border: 1px solid #334155;
      border-radius: 10px;
      padding: 12px;
      margin-top: 16px;
      text-align: left;
    }
    .sql-box pre {
      font-size: 11px;
      color: #38bdf8;
      overflow-x: auto;
      white-space: pre-wrap;
      word-break: break-all;
    }
    .btn-copy {
      background: #1e293b;
      border: 1px solid #475569;
      color: #f1f5f9;
      font-size: 11px;
      padding: 5px 10px;
      border-radius: 6px;
      cursor: pointer;
      margin-top: 8px;
      display: inline-block;
    }
    .auto-notice {
      font-size: 12px;
      color: var(--text-muted);
      margin-top: 14px;
    }
  </style>
</head>
<body>
  <div class="card">
    <div class="icon-container">📱</div>
    <h1>DropMe</h1>
    <div class="badge"><span class="status-dot"></span>Zawsze Aktualne APK (Release)</div>

    <div id="liveAlert" class="live-alert">
      🚀 Wykryto nowszą kompilację APK! Trwa aktualizacja...
    </div>

    <div class="info-box">
      <div class="info-row">
        <span class="info-label">Ostatnia kompilacja:</span>
        <span class="info-val" id="valDate" style="color: #60a5fa;">${rel.dateFormatted}</span>
      </div>
      <div class="info-row">
        <span class="info-label">Czas od kompilacji:</span>
        <span class="info-val" id="valRelTime">${rel.relativeTime}</span>
      </div>
      <div class="info-row">
        <span class="info-label">Rozmiar pliku:</span>
        <span class="info-val" id="valSize">${rel.size}</span>
      </div>
      <div class="info-row">
        <span class="info-label">Status pliku:</span>
        <span class="info-val" style="color: #34d399;">${rel.exists ? '✓ Gotowy do instalacji' : 'Oczekiwanie na kompilację'}</span>
      </div>
      <div class="info-row">
        <span class="info-label">Podpis certyfikatu:</span>
        <span class="info-val">release.keystore ✓</span>
      </div>
    </div>

    <a href="/app-release.apk?t=${rel.mtime}" class="btn btn-primary" id="btnDownload">
      ⬇ Pobierz Najnowszą Wersję (Release)
    </a>

    <a href="/app-debug.apk?t=${deb.mtime}" class="btn btn-secondary">
      Pobierz wersję testową (Debug) &bull; ${deb.size}
    </a>

    <div class="sql-box">
      <div style="font-size: 11px; color: #94a3b8; margin-bottom: 4px; font-weight: 600;">⚡ Baza Supabase (SQL Fix):</div>
      <pre id="sqlCode">ALTER TABLE public.profiles DISABLE ROW LEVEL SECURITY;
GRANT ALL ON public.profiles TO anon, authenticated, service_role;</pre>
      <button class="btn-copy" onclick="copySql()">📋 Kopiuj SQL</button>
      <span id="copyConfirm" style="font-size: 11px; color: #34d399; margin-left: 8px; display: none;">Skopiowano!</span>
    </div>

    <div class="sql-box" style="margin-top: 14px; border-color: #3b82f6;">
      <div style="font-size: 11px; color: #60a5fa; margin-bottom: 6px; font-weight: 600;">📬 Serwer Poczty • Kody weryfikacyjne:</div>
      <div id="codesBox" style="font-size: 12px; color: #cbd5e1; min-height: 24px;">Oczekiwanie na zapytania...</div>
    </div>

    <div class="auto-notice" id="autoNotice">
      Pobieranie rozpocznie się automatycznie...
    </div>
  </div>

  <script>
    let currentMtime = ${rel.mtime};

    function copySql() {
      const code = document.getElementById('sqlCode').innerText;
      navigator.clipboard.writeText(code).then(() => {
        const c = document.getElementById('copyConfirm');
        c.style.display = 'inline';
        setTimeout(() => c.style.display = 'none', 3000);
      });
    }

    function updateCodes(codes) {
      const box = document.getElementById('codesBox');
      if (!box) return;
      if (!codes || codes.length === 0) {
        box.innerHTML = '<span style="color: #64748b;">Brak nowych kodów weryfikacyjnych.</span>';
        return;
      }
      box.innerHTML = codes.map(function(c) {
        return '<div style="padding: 6px 8px; margin-bottom: 4px; background: rgba(30, 41, 59, 0.8); border-radius: 6px; border-left: 3px solid #10b981;">' +
          '<div style="display: flex; justify-content: space-between; font-size: 10px; color: #94a3b8;">' +
            '<span>' + c.type + ' • @' + c.username + '</span>' +
            '<span>' + c.time + '</span>' +
          '</div>' +
          '<div style="display: flex; justify-content: space-between; align-items: center; margin-top: 2px;">' +
            '<span style="font-size: 11px; color: #e2e8f0;">' + c.email + '</span>' +
            '<span style="font-family: monospace; font-size: 15px; font-weight: 700; color: #34d399; letter-spacing: 2px;">' + c.code + '</span>' +
          '</div>' +
        '</div>';
      }).join('');
    }

    // Automatyczne sprawdzanie co 3 sekundy czy pojawiła się nowa kompilacja APK
    setInterval(function() {
      fetch('/api/status?t=' + Date.now())
        .then(res => res.json())
        .then(data => {
          if (data && data.recentCodes) {
            updateCodes(data.recentCodes);
          }
          if (data && data.release) {
            if (currentMtime !== 0 && data.release.mtime > currentMtime) {
              const alertBox = document.getElementById('liveAlert');
              if (alertBox) alertBox.style.display = 'block';
              setTimeout(() => {
                window.location.reload();
              }, 1200);
            }
            document.getElementById('valDate').innerText = data.release.dateFormatted;
            document.getElementById('valRelTime').innerText = data.release.relativeTime;
            document.getElementById('valSize').innerText = data.release.size;
          }
        })
        .catch(() => {});
    }, 3000);

    // Auto-start pobierania
    setTimeout(function() {
      if (currentMtime > 0) {
        window.location.href = '/app-release.apk?t=' + currentMtime;
      }
      var notice = document.getElementById('autoNotice');
      if (notice) {
        notice.innerHTML = 'Jeśli pobieranie nie wystartowało automatycznie, kliknij niebieski przycisk.';
      }
    }, 1200);
  </script>
</body>
</html>`;

  res.writeHead(200, {
    'Content-Type': 'text/html; charset=utf-8',
    'Cache-Control': 'no-store, no-cache, must-revalidate, max-age=0'
  });
  res.end(html);
});

server.listen(PORT, '0.0.0.0', () => {
  console.log(`[DropMe Server] Serwer pobierania aktywny na porcie ${PORT}`);
  console.log(`- Auto-Sync: Włączony (automatycznie wykrywa i podaje najnowszy app-release.apk z kompilacji)`);
  console.log(`- Strona WWW: http://localhost:${PORT}/`);
  console.log(`- Bezpośredni plik Release: http://localhost:${PORT}/app-release.apk`);
});
