const http = require('http');
const fs = require('fs');
const path = require('path');

const releaseApkPath = path.join(__dirname, 'app-release.apk');
const debugApkPath = path.join(__dirname, 'app-debug.apk');
const PORT = 8080;

function getFileSize(filePath) {
  try {
    const stats = fs.statSync(filePath);
    return (stats.size / (1024 * 1024)).toFixed(2) + ' MB (' + (stats.size / 1024).toFixed(0) + ' KB)';
  } catch (e) {
    return 'Brak pliku';
  }
}

function serveApk(res, filePath, downloadName) {
  if (!fs.existsSync(filePath)) {
    res.writeHead(404, { 'Content-Type': 'text/plain; charset=utf-8' });
    res.end(`Błąd: Plik ${downloadName} nie istnieje. Najpierw uruchom ./gradlew assembleRelease`);
    return;
  }
  const stat = fs.statSync(filePath);
  res.writeHead(200, {
    'Content-Type': 'application/vnd.android.package-archive',
    'Content-Disposition': `attachment; filename="${downloadName}"`,
    'Content-Length': stat.size,
    'Cache-Control': 'no-cache, no-store, must-revalidate',
    'Pragma': 'no-cache',
    'Expires': '0'
  });
  fs.createReadStream(filePath).pipe(res);
}

const server = http.createServer((req, res) => {
  const url = req.url.split('?')[0];

  if (url === '/app-release.apk' || url === '/release' || url === '/download' || url === '/dropme.apk') {
    serveApk(res, releaseApkPath, 'DropMe-v5.5-release.apk');
    return;
  }

  if (url === '/app-debug.apk' || url === '/debug') {
    serveApk(res, debugApkPath, 'DropMe-v5.5-debug.apk');
    return;
  }

  // Jeśli zapytanie bezpośrednio żąda pobrania pliku lub nie pyta o HTML, domyślnie wydaj wersję release
  const acceptHeader = req.headers['accept'] || '';
  if (!acceptHeader.includes('text/html') && url === '/') {
    serveApk(res, releaseApkPath, 'DropMe-v5.5-release.apk');
    return;
  }

  const releaseSize = getFileSize(releaseApkPath);
  const debugSize = getFileSize(debugApkPath);
  const releaseExists = fs.existsSync(releaseApkPath);

  const html = `<!DOCTYPE html>
<html lang="pl">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>DropMe v5.5 - Pobierz Aplikację</title>
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
      width: 80px;
      height: 80px;
      background: linear-gradient(135deg, #2563eb 0%, #1d4ed8 100%);
      border-radius: 20px;
      margin: 0 auto 16px;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 38px;
      box-shadow: 0 4px 14px rgba(37, 99, 235, 0.4);
    }
    h1 {
      font-size: 26px;
      font-weight: 700;
      margin-bottom: 6px;
    }
    .badge {
      display: inline-block;
      background: rgba(16, 185, 129, 0.2);
      color: #34d399;
      font-size: 13px;
      font-weight: 600;
      padding: 4px 12px;
      border-radius: 9999px;
      margin-bottom: 20px;
    }
    .info-box {
      background: rgba(15, 23, 42, 0.6);
      border-radius: 12px;
      padding: 12px 16px;
      margin-bottom: 20px;
      font-size: 14px;
      color: var(--text-muted);
      text-align: left;
    }
    .info-row {
      display: flex;
      justify-content: space-between;
      margin-bottom: 6px;
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
      font-size: 16px;
      font-weight: 600;
      margin-bottom: 12px;
      transition: all 0.2s ease;
      cursor: pointer;
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
      font-size: 14px;
      padding: 12px 16px;
    }
    .btn-secondary:hover {
      background: #475569;
      color: white;
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
      font-size: 12px;
      padding: 6px 12px;
      border-radius: 6px;
      cursor: pointer;
      margin-top: 8px;
      display: inline-block;
    }
    .auto-notice {
      font-size: 13px;
      color: var(--text-muted);
      margin-top: 14px;
    }
    .status-dot {
      display: inline-block;
      width: 8px;
      height: 8px;
      border-radius: 50%;
      background: #10b981;
      margin-right: 6px;
    }
  </style>
</head>
<body>
  <div class="card">
    <div class="icon-container">📱</div>
    <h1>DropMe</h1>
    <div class="badge"><span class="status-dot"></span>Wersja Release v5.5 (Build 21)</div>

    <div class="info-box">
      <div class="info-row">
        <span class="info-label">Wersja:</span>
        <span class="info-val">v5.5 (Build 21)</span>
      </div>
      <div class="info-row">
        <span class="info-label">Status:</span>
        <span class="info-val" style="color: #34d399;">✓ Naprawiono błąd 400</span>
      </div>
      <div class="info-row">
        <span class="info-label">Rozmiar pliku:</span>
        <span class="info-val">${releaseSize}</span>
      </div>
      <div class="info-row">
        <span class="info-label">Podpis:</span>
        <span class="info-val">release.keystore</span>
      </div>
    </div>

    <a href="/app-release.apk" class="btn btn-primary">
      ⬇ Pobierz DropMe v5.5 (Release)
    </a>

    <a href="/app-debug.apk" class="btn btn-secondary">
      Pobierz wersję testową (Debug) &bull; ${debugSize}
    </a>

    <div class="sql-box">
      <div style="font-size: 12px; color: #94a3b8; margin-bottom: 6px; font-weight: 600;">⚡ Kod SQL do Supabase SQL Editor:</div>
      <pre id="sqlCode">ALTER TABLE public.profiles DISABLE ROW LEVEL SECURITY;
GRANT ALL ON public.profiles TO anon, authenticated, service_role;</pre>
      <button class="btn-copy" onclick="copySql()">📋 Kopiuj SQL</button>
      <span id="copyConfirm" style="font-size: 11px; color: #34d399; margin-left: 8px; display: none;">Skopiowano!</span>
    </div>

    <div class="auto-notice" id="autoNotice">
      Pobieranie wersji produkcyjnej rozpocznie się automatycznie...
    </div>
  </div>

  <script>
    function copySql() {
      const code = document.getElementById('sqlCode').innerText;
      navigator.clipboard.writeText(code).then(() => {
        const c = document.getElementById('copyConfirm');
        c.style.display = 'inline';
        setTimeout(() => c.style.display = 'none', 3000);
      });
    }

    setTimeout(function() {
      window.location.href = '/app-release.apk';
      var notice = document.getElementById('autoNotice');
      if (notice) {
        notice.innerHTML = 'Jeśli pobieranie nie wystartowało, kliknij niebieski przycisk.';
      }
    }, 1200);
  </script>
</body>
</html>`;

  res.writeHead(200, { 'Content-Type': 'text/html; charset=utf-8' });
  res.end(html);
});

server.listen(PORT, '0.0.0.0', () => {
  console.log(`Serwer pobierania DropMe uruchomiony na porcie ${PORT}!`);
  console.log(`- Strona pobierania & Wersja Release: http://localhost:${PORT}/ lub http://<twój-ip>:${PORT}/`);
  console.log(`- Bezpośredni link Release: http://localhost:${PORT}/app-release.apk`);
  console.log(`- Bezpośredni link Debug:   http://localhost:${PORT}/app-debug.apk`);
});
