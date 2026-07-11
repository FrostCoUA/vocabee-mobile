/* ============================================================
   Vocabee Redesign — base: canvas helpers, icons, brand marks
   ============================================================ */
window.RD = {};
(function () {
  const RD = window.RD;

  RD.ACCENTS = {
    indigo: '#4F46E5', blue: '#5B7BFE', violet: '#7C5CF6', grape: '#410FA3',
    royal: '#3E63DD', plum: '#9333EA', teal: '#0E9FA5', amber: '#E0820C',
  };

  /* ---------- line icons (stroke, 24 viewBox) ---------- */
  const PATHS = {
    book: '<path d="M12 6.3C10 4.4 7.3 4.1 4.5 5.3V18c2.8-1.2 5.5-.9 7.5 1 2-1.9 4.7-2.2 7.5-1V5.3C16.7 4.1 14 4.4 12 6.3Z"/><path d="M12 6.3v12.7"/>',
    dumbbell: '<path d="M9.6 12h4.8"/><rect x="5.9" y="7.9" width="3.3" height="8.2" rx="1.5"/><rect x="14.8" y="7.9" width="3.3" height="8.2" rx="1.5"/><rect x="2.9" y="9.9" width="2.1" height="4.2" rx="1"/><rect x="19" y="9.9" width="2.1" height="4.2" rx="1"/>',
    user: '<circle cx="12" cy="7.7" r="3.7"/><path d="M5 19.4c.8-3.3 3.5-5.2 7-5.2s6.2 1.9 7 5.2"/>',
    plus: '<path d="M12 5v14M5 12h14"/>',
    mic: '<rect x="9" y="2.5" width="6" height="12" rx="3"/><path d="M5.5 11.5a6.5 6.5 0 0 0 13 0M12 18v3"/>',
    sound: '<path d="M4 9v6h4l5 4V5L8 9H4Z"/><path d="M17 9.5a3.5 3.5 0 0 1 0 5M19.5 7a7 7 0 0 1 0 10"/>',
    sparkle: '<path d="M12 3l1.8 5.2L19 10l-5.2 1.8L12 17l-1.8-5.2L5 10l5.2-1.8L12 3Z"/><path d="M19 3.5l1.5 4.7M5 17l1.2 3.2"/>',
    check: '<path d="M5 12.5l4.5 4.5L19 7"/>',
    chevR: '<path d="M9 5l7 7-7 7"/>',
    chevL: '<path d="M15 5l-7 7 7 7"/>',
    chevD: '<path d="M5 9l7 7 7-7"/>',
    search: '<circle cx="11" cy="11" r="7"/><path d="M20 20l-4-4"/>',
    close: '<path d="M6 6l12 12M18 6L6 18"/>',
    globe: '<circle cx="12" cy="12" r="9"/><path d="M3 12h18M12 3c3 3 3 15 0 18M12 3c-3 3-3 15 0 18"/>',
    bell: '<path d="M6 9a6 6 0 0 1 12 0c0 5 2 6 2 6H4s2-1 2-6Z"/><path d="M10 20a2 2 0 0 0 4 0"/>',
    moon: '<path d="M20 14.5A8 8 0 1 1 9.5 4a6.5 6.5 0 0 0 10.5 10.5Z"/>',
    edit: '<path d="M14 5l5 5M4 20l1-4L16 5l3 3L8 19l-4 1Z"/>',
    help: '<circle cx="12" cy="12" r="9"/><path d="M9.5 9.5a2.5 2.5 0 1 1 3.4 2.3c-.7.3-.9.8-.9 1.5v.3"/><circle cx="12" cy="17" r="0.7" fill="currentColor" stroke="none"/>',
    invite: '<circle cx="9" cy="8" r="3.5"/><path d="M3 20c0-3.3 2.7-5.5 6-5.5s6 2.2 6 5.5M18 8v6M15 11h6"/>',
    flame: '<path d="M12 3c1 3-1.5 4-1.5 6.5 0 1.4 1.1 2 1.5 2 .4 0 1.5-.6 1.5-2C13.5 8 16 9 16 13a4 4 0 0 1-8 0c0-2 1-3 1-4 0 0-2.5.5-2.5 3.5A6 6 0 0 0 18 13c0-6-6-6-6-10Z"/>',
    bookmark: '<path d="M6 4h12v16l-6-4-6 4V4Z"/>',
    star: '<path d="M12 3.5l2.6 5.3 5.9.9-4.3 4.1 1 5.8L12 17l-5.2 2.6 1-5.8L3.5 9.7l5.9-.9L12 3.5Z"/>',
    arrowR: '<path d="M5 12h14M13 6l6 6-6 6"/>',
    play: '<circle cx="12" cy="12" r="9"/><path d="M10 8.5v7l6-3.5-6-3.5Z" fill="currentColor" stroke="none"/>',
    trash: '<path d="M4 7h16M9 7V5a2 2 0 0 1 2-2h2a2 2 0 0 1 2 2v2M6 7l1 13h10l1-13M10 11v6M14 11v6"/>',
    cards: '<rect x="3" y="6" width="13" height="15" rx="2.5"/><path d="M8 3h10a2.5 2.5 0 0 1 2.5 2.5V17"/>',
    plane: '<path d="M21 15.5 3 11l2-3 4 1 4-5 2 1-2 4 5 1 1.5 2.5a1.4 1.4 0 0 1-1.5 2Z"/><path d="M6 20h9"/>',
    film: '<rect x="3" y="5" width="18" height="14" rx="2.5"/><path d="M7 5v14M17 5v14M3 9.5h4M3 14.5h4M17 9.5h4M17 14.5h4"/>',
    brief: '<rect x="3" y="7" width="18" height="13" rx="2.5"/><path d="M8 7V5.5A2.5 2.5 0 0 1 10.5 3h3A2.5 2.5 0 0 1 16 5.5V7M3 12.5h18"/>',
    grad: '<path d="M12 4 2 9l10 5 10-5-10-5Z"/><path d="M6 11.2V16c0 1 2.7 2.5 6 2.5s6-1.5 6-2.5v-4.8M22 9v5"/>',
    food: '<path d="M6 3v8M9 3v8M7.5 11v10M6 6.5h3"/><path d="M16 3c-1.5 1-2 3-2 5s.5 3 2 3 2-1 2-3-.5-4-2-5Zm0 8v10"/>',
    ball: '<circle cx="12" cy="12" r="9"/><path d="M5.6 5.8c3 3.4 3 9 0 12.4M18.4 5.8c-3 3.4-3 9 0 12.4"/>',
    music: '<path d="M9 18V6l10-2v12"/><circle cx="6.5" cy="18" r="2.5"/><circle cx="16.5" cy="16" r="2.5"/>',
    leaf: '<path d="M12 21v-7"/><path d="M12 14c-4 0-6-2.5-6-6 3 0 6 1.5 6 6Z"/><path d="M12 12c0-4 2.5-6 6-6 0 3.5-2 6-6 6Z"/>',
    laptop: '<rect x="4" y="4.5" width="16" height="11.5" rx="2"/><path d="M2 19.5h20"/>',
    bag: '<path d="M6 8h12l-1 12H7L6 8Z"/><path d="M9 8V6a3 3 0 0 1 6 0v2"/>',
    heart: '<path d="M12 20S4 15 4 9a4.5 4.5 0 0 1 8-2.8A4.5 4.5 0 0 1 20 9c0 6-8 11-8 11Z"/>',
    child: '<circle cx="12" cy="6.8" r="3"/><path d="M6 20c0-3.6 2.7-5.8 6-5.8s6 2.2 6 5.8"/><path d="M9.4 3.6 8.2 2.2M14.6 3.6l1.2-1.4"/>',
    chat: '<path d="M12 4c5 0 9 3 9 6.8s-4 6.8-9 6.8c-1 0-2-.1-2.9-.4L5 19.4l.8-3C4.1 15 3 13 3 10.8 3 7 7 4 12 4Z"/>',
    copy: '<rect x="9" y="9" width="11" height="11" rx="2.5"/><path d="M15 9V6.5A2.5 2.5 0 0 0 12.5 4h-6A2.5 2.5 0 0 0 4 6.5v6A2.5 2.5 0 0 0 6.5 15H9"/>',
    share: '<path d="M12 15V4M8 7.5 12 3.5l4 4"/><path d="M5 12v7.5h14V12"/>',
    clip: '<path d="M20.5 12.5l-7.8 7.8a5.3 5.3 0 0 1-7.5-7.5l8.2-8.2a3.5 3.5 0 0 1 5 5l-8.2 8.2a1.77 1.77 0 0 1-2.5-2.5l7.5-7.5"/>',
    image: '<rect x="3.5" y="5" width="17" height="14" rx="2.5"/><circle cx="9" cy="10" r="1.6"/><path d="M4.5 17.5 10 12l4 4 2.5-2.5 3 3"/>',
    dots: '<circle cx="5.5" cy="12" r="1.5" fill="currentColor" stroke="none"/><circle cx="12" cy="12" r="1.5" fill="currentColor" stroke="none"/><circle cx="18.5" cy="12" r="1.5" fill="currentColor" stroke="none"/>',
    send: '<path d="M22 2 11 13"/><path d="M22 2 15 22l-4-9-9-4 20-7Z"/>',
  };

  RD.ic = function (name, size, color, sw) {
    size = size || 20; color = color || 'var(--ink)'; sw = sw || 1.9;
    return '<svg width="' + size + '" height="' + size + '" viewBox="0 0 24 24" fill="none" stroke="' + color +
      '" stroke-width="' + sw + '" stroke-linecap="round" stroke-linejoin="round" style="flex:none;color:' + color + '">' +
      (PATHS[name] || '') + '</svg>';
  };

  /* ---------- brand ---------- */
  const HEX = '26,0 13,22.5 -13,22.5 -26,0 -13,-22.5 13,-22.5';
  RD.logo = function (size, color, accent) {
    color = color || 'var(--purple)'; accent = accent || '#FFCC00';
    return '<svg width="' + size + '" height="' + size + '" viewBox="-58 -58 116 116" style="flex:none">' +
      '<g transform="translate(-19.5,0)" stroke-linejoin="round">' +
      '<polygon points="' + HEX + '" fill="' + color + '" stroke="' + color + '" stroke-width="6"/>' +
      '<polygon points="' + HEX + '" transform="translate(39,-22.5)" fill="' + color + '" stroke="' + color + '" stroke-width="6"/>' +
      '<polygon points="' + HEX + '" transform="translate(39,22.5)" fill="' + accent + '" stroke="' + accent + '" stroke-width="6"/>' +
      '</g></svg>';
  };
  RD.honeycomb = function (size, alpha) {
    alpha = alpha || 0.18;
    return '<svg width="' + size + '" height="' + size + '" viewBox="-58 -58 116 116" style="position:absolute;top:-24px;right:-24px;opacity:' + alpha + '">' +
      '<g transform="translate(-19.5,0)" fill="none" stroke="#fff" stroke-width="5" stroke-linejoin="round">' +
      '<polygon points="' + HEX + '"/><polygon points="' + HEX + '" transform="translate(39,-22.5)"/><polygon points="' + HEX + '" transform="translate(39,22.5)"/>' +
      '</g></svg>';
  };
  /* ---------- honeycomb coin (currency) ---------- */
  RD.coin = function (size) {
    size = size || 16;
    return '<svg width="' + size + '" height="' + size + '" viewBox="-12 -12 24 24" style="flex:none">' +
      '<polygon points="10,0 5,8.66 -5,8.66 -10,0 -5,-8.66 5,-8.66" fill="#FFCC00" stroke="#E9B400" stroke-width="1" stroke-linejoin="round"/>' +
      '<polygon points="6.1,0 3.05,5.28 -3.05,5.28 -6.1,0 -3.05,-5.28 3.05,-5.28" fill="none" stroke="#96762B" stroke-width="1.7" stroke-linejoin="round" opacity=".55"/></svg>';
  };

  RD.google = function (size) {
    size = size || 20;
    return '<svg width="' + size + '" height="' + size + '" viewBox="0 0 24 24" style="flex:none">' +
      '<path fill="#4285F4" d="M21.6 12.2c0-.7-.06-1.3-.18-1.9H12v3.6h5.4a4.6 4.6 0 0 1-2 3v2.5h3.2c1.9-1.7 3-4.3 3-7.2Z"/>' +
      '<path fill="#34A853" d="M12 22c2.7 0 5-.9 6.6-2.4l-3.2-2.5c-.9.6-2 1-3.4 1-2.6 0-4.8-1.7-5.6-4.1H3.1v2.6A10 10 0 0 0 12 22Z"/>' +
      '<path fill="#FBBC05" d="M6.4 14c-.2-.6-.3-1.3-.3-2s.1-1.4.3-2V7.4H3.1A10 10 0 0 0 2 12c0 1.6.4 3.1 1.1 4.6L6.4 14Z"/>' +
      '<path fill="#EA4335" d="M12 5.9c1.5 0 2.8.5 3.8 1.5l2.8-2.8A10 10 0 0 0 3.1 7.4L6.4 10c.8-2.4 3-4.1 5.6-4.1Z"/></svg>';
  };

  /* ---------- status bar ---------- */
  RD.statusbar = function (light) {
    const c = light ? 'rgba(255,255,255,.95)' : 'var(--ink)';
    return '<div style="height:50px;flex:none;display:flex;align-items:center;justify-content:space-between;padding:14px 28px 0;font-size:15px;font-weight:700;color:' + c + '">' +
      '<span>9:41</span><span style="display:flex;gap:6px;align-items:center">' +
      '<svg width="17" height="11" viewBox="0 0 17 11" fill="' + c + '"><rect x="0" y="7" width="3" height="4" rx="1"/><rect x="4.5" y="5" width="3" height="6" rx="1"/><rect x="9" y="2.5" width="3" height="8.5" rx="1"/><rect x="13.5" y="0" width="3" height="11" rx="1"/></svg>' +
      '<svg width="16" height="11" viewBox="0 0 16 11" fill="' + c + '"><path d="M8 2.2c2 0 3.8.8 5.1 2l1.3-1.4A9 9 0 0 0 8 .2 9 9 0 0 0 1.6 2.8L2.9 4.2A7 7 0 0 1 8 2.2Z"/><path d="M8 5.6c1.1 0 2 .4 2.7 1.1l1.3-1.4A6 6 0 0 0 8 3.6a6 6 0 0 0-4 1.7l1.3 1.4A4 4 0 0 1 8 5.6Z"/><circle cx="8" cy="9.2" r="1.6"/></svg>' +
      '<svg width="25" height="12" viewBox="0 0 25 12" fill="none"><rect x="0.5" y="0.5" width="21" height="11" rx="3" stroke="' + c + '" opacity="0.5"/><rect x="2" y="2" width="17" height="8" rx="1.6" fill="' + c + '"/><rect x="23" y="3.5" width="1.6" height="5" rx="0.8" fill="' + c + '" opacity="0.5"/></svg>' +
      '</span></div>';
  };

  /* ---------- canvas placement ---------- */
  let maxX = 0, maxY = 0;
  function track(x, y, w, h) {
    maxX = Math.max(maxX, x + w + 200);
    maxY = Math.max(maxY, y + h + 240);
    document.body.style.width = maxX + 'px';
    document.body.style.height = maxY + 'px';
  }

  RD.frame = function (o) {
    const w = o.w || 390, h = o.h || 844, r = o.r == null ? 30 : o.r;
    const s = document.createElement('section');
    s.className = 'frame t-' + (o.theme || 'light');
    s.style.cssText = 'left:' + o.x + 'px;top:' + o.y + 'px;width:' + w + 'px;height:' + h + 'px;';
    s.setAttribute('data-screen-label', o.label || '');
    s.innerHTML = '<div class="frame-label">' + (o.label || '') +
      (o.theme === 'dark' ? ' <span class="lbl-chip dark-chip">dark</span>' : ' <span class="lbl-chip">light</span>') + '</div>' +
      '<div class="cv" style="border-radius:' + r + 'px">' + o.body + '</div>';
    document.body.appendChild(s);
    track(o.x, o.y, w, h);
    return s;
  };

  RD.board = function (o) { // non-phone wide spec board
    const s = document.createElement('section');
    s.className = 'board' + (o.cls ? ' ' + o.cls : '');
    s.style.cssText = 'left:' + o.x + 'px;top:' + o.y + 'px;width:' + o.w + 'px;' + (o.h ? 'height:' + o.h + 'px;' : '');
    s.setAttribute('data-screen-label', o.label || '');
    s.innerHTML = (o.label ? '<div class="frame-label">' + o.label + '</div>' : '') + o.body;
    document.body.appendChild(s);
    track(o.x, o.y, o.w, o.h || 400);
    return s;
  };

  RD.note = function (o) {
    const s = document.createElement('aside');
    s.className = 'note';
    s.style.cssText = 'left:' + o.x + 'px;top:' + o.y + 'px;width:' + (o.w || 300) + 'px;';
    s.innerHTML = '<h4>' + o.title + '</h4><ul>' + o.items.map(function (i) { return '<li>' + i + '</li>'; }).join('') + '</ul>';
    document.body.appendChild(s);
    track(o.x, o.y, o.w || 300, 200);
    return s;
  };

  RD.sec = function (o) {
    const s = document.createElement('div');
    s.className = 'sec-h';
    s.style.cssText = 'left:' + o.x + 'px;top:' + o.y + 'px;';
    s.innerHTML = '<span class="sec-num">' + o.num + '</span><h2>' + o.text + '</h2>' + (o.sub ? '<p>' + o.sub + '</p>' : '');
    document.body.appendChild(s);
    track(o.x, o.y, 700, 120);
    return s;
  };
})();
