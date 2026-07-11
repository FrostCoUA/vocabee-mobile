/* ============================================================
   Vocabee Redesign — parts library (returns HTML strings)
   Geometry mirrors the Compose implementation (dp ≈ px).
   ============================================================ */
window.P = {};
(function () {
  const P = window.P, ic = RD.ic;

  /* ---------- buttons ---------- */
  P.btn = function (label, kind, o) {
    o = o || {};
    const h = o.h || 56, base = 'height:' + h + 'px;border-radius:16px;display:flex;align-items:center;justify-content:center;gap:9px;font-weight:800;font-size:16px;' + (o.grow ? 'flex:1;' : 'width:100%;');
    let skin = '';
    if (kind === 'primary') skin = 'background:var(--purple);color:#fff;box-shadow:0 10px 22px -10px rgba(79,70,229,.55);';
    else if (kind === 'neutral') skin = 'background:var(--neutral);color:var(--ink);';
    else if (kind === 'danger') skin = 'background:var(--red);color:#fff;';
    else if (kind === 'ghost') skin = 'background:transparent;color:var(--muted);';
    else if (kind === 'outline') skin = 'background:var(--surface);border:1.5px solid var(--line);color:var(--ink);font-weight:700;';
    return '<div style="' + base + skin + (o.style || '') + '">' + (o.icon || '') + label + '</div>';
  };

  /* ---------- bee balance badge (honeycomb coin) ---------- */
  P.beeBadge = function (n, accent) {
    accent = accent || 'var(--ink)';
    return '<span style="display:inline-flex;align-items:center;gap:5px;background:var(--surface);border-radius:99px;padding:6px 11px;font-weight:800;font-size:15px;color:' + accent + ';box-shadow:0 3px 8px -4px rgba(17,24,39,.25)">' + RD.coin(15) + n + '</span>';
  };

  /* ---------- banners (home slot) — compact single-row ---------- */
  P.banner = function (kind, o) {
    o = o || {};
    const critical = kind === 'guestCritical' || kind === 'walletCritical';
    const accent = critical ? 'var(--orange-t)' : 'var(--purple-t)';
    const bg = critical ? 'var(--peach)' : 'var(--tint)';
    const border = critical ? 'var(--peach-b)' : 'color-mix(in srgb, var(--purple-t) 22%, transparent)';
    const isGuest = kind.indexOf('guest') === 0;
    // lead: coin balance pill (wallet) merges icon + number; guest keeps a user tile
    const lead = isGuest
      ? '<span style="flex:none;width:38px;height:38px;border-radius:12px;background:var(--surface);display:flex;align-items:center;justify-content:center">' + ic('user', 20, accent, 2.1) + '</span>'
      : '<span style="flex:none;display:inline-flex;align-items:center;gap:4px;background:var(--surface);border-radius:12px;padding:8px 10px;font-weight:800;font-size:15px;color:' + accent + '">' + RD.coin(16) + (o.bees != null ? o.bees : 47) + '</span>';
    let title, sub;
    if (isGuest) {
      title = critical ? 'Ліміт гостя вичерпано' : 'Гостьовий режим';
      sub = critical ? 'Увійди, щоб зберігати далі' : 'Словники ' + (o.dicts || '2/2') + ' · слова ' + (o.words || '50/50');
    } else {
      title = critical ? 'Монетки закінчуються' : 'Переклади за монетки';
      sub = critical ? 'Відео дає +10 монеток' : '1 пошук — 1 монетка · відео +10';
    }
    const clamp = 'display:block;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;';
    return '<div style="display:flex;align-items:center;gap:11px;background:' + bg + ';border:1.2px solid ' + border + ';border-radius:18px;padding:11px 13px">' +
      lead +
      '<span style="flex:1;min-width:0"><span style="' + clamp + 'font-weight:800;font-size:15px;color:var(--ink)">' + title + '</span>' +
      '<span style="' + clamp + 'margin-top:1px;font-weight:600;font-size:12.5px;line-height:15px;color:var(--muted)">' + sub + '</span></span>' +
      ic('chevR', 18, accent, 2.4) + '</div>';
  };

  P.criticalFloat = function (n) {
    return '<div style="position:absolute;left:16px;right:16px;z-index:30;display:flex;align-items:center;gap:11px;background:var(--peach);border:1.2px solid var(--peach-b);border-radius:18px;padding:11px 13px;box-shadow:var(--elev2)">' +
      '<span style="flex:none;width:38px;height:38px;border-radius:13px;background:var(--surface);display:flex;align-items:center;justify-content:center">' + RD.coin(21) + '</span>' +
      '<span style="flex:1;font-weight:800;font-size:13.5px;line-height:17px;color:var(--ink)">Лишилось ' + n + ' монетки — відео дасть +10</span>' +
      '<span style="flex:none;width:26px;height:26px;border-radius:99px;background:var(--surface);display:flex;align-items:center;justify-content:center">' + ic('chevR', 14, 'var(--orange-t)', 2.4) + '</span></div>';
  };

  /* ---------- dictionary card ---------- */
  P.dictCard = function (o) {
    const know = o.know || 0;
    return '<div style="position:relative;height:162px;border-radius:24px;overflow:hidden;background:' + o.accent + ';box-shadow:var(--elev1)">' +
      '<div style="position:absolute;left:0;top:0;bottom:0;width:' + know + '%;background:rgba(0,0,0,.13)"></div>' +
      RD.honeycomb(120, 0.16) +
      (o.icon ? '<span style="position:absolute;top:14px;left:14px;width:32px;height:32px;border-radius:11px;background:rgba(255,255,255,.22);display:flex;align-items:center;justify-content:center">' + ic(o.icon, 18, '#fff', 2) + '</span>' : '') +
      (o.today ? '<span style="position:absolute;top:14px;right:14px;background:var(--yellow);color:#5A4500;font-size:11.5px;font-weight:800;padding:5px 10px;border-radius:99px">сьогодні</span>' : '') +
      '<div style="position:absolute;left:18px;right:18px;bottom:16px">' +
      '<div style="color:#fff;font-weight:800;font-size:17.5px;line-height:21px;' + (o.nowrap ? 'white-space:nowrap;overflow:hidden;text-overflow:ellipsis;' : 'max-height:42px;overflow:hidden;') + '">' + o.title + '</div>' +
      '<div style="display:flex;align-items:center;gap:8px;margin-top:11px">' +
      '<span style="background:rgba(255,255,255,.22);color:#fff;font-weight:700;font-size:13.5px;padding:4px 10px;border-radius:99px;white-space:nowrap">' + o.words + ' слів</span>' +
      (o.updated ? '<span style="color:rgba(255,255,255,.82);font-weight:600;font-size:12.5px;white-space:nowrap">' + o.updated + '</span>' : '') +
      '</div></div></div>';
  };

  P.dictCardSwiped = function (o) {
    return '<div style="position:relative;height:162px;border-radius:24px;overflow:hidden;background:var(--red)">' +
      '<div style="position:absolute;right:0;top:0;bottom:0;width:76px;display:flex;flex-direction:column;align-items:center;justify-content:center;gap:5px;color:#fff">' +
      ic('close', 22, '#fff', 2.4) + '<span style="font-size:10.5px;font-weight:800">Видалити</span></div>' +
      '<div style="position:absolute;inset:0;right:76px;border-radius:24px;overflow:hidden">' + P.dictCard(Object.assign({}, o, { nowrap: true })) + '</div></div>';
  };

  P.fab = function () {
    return '<div style="position:absolute;right:20px;bottom:94px;width:62px;height:62px;border-radius:22px;background:var(--purple);display:flex;align-items:center;justify-content:center;box-shadow:0 16px 30px -10px rgba(79,70,229,.65);z-index:20">' + ic('plus', 26, '#fff', 2.4) + '</div>';
  };

  /* ---------- bottom bar ---------- */
  P.bottomBar = function (active) {
    const tabs = [['book', 'Словники'], ['dumbbell', 'Тренування'], ['user', 'Профіль']];
    return '<div style="position:absolute;left:0;right:0;bottom:0;height:80px;background:var(--surface);border-top:1px solid var(--line);display:flex;padding-bottom:14px;z-index:25">' +
      tabs.map(function (t, i) {
        const on = i === active;
        return '<div style="flex:1;display:flex;flex-direction:column;align-items:center;justify-content:center;gap:3px">' +
          '<span style="width:86px;height:36px;border-radius:24px;display:flex;align-items:center;justify-content:center;background:' + (on ? 'var(--purple)' : 'transparent') + '">' +
          ic(t[0], on ? 25 : 24, on ? '#fff' : 'var(--muted2)', on ? 2.35 : 2) + '</span>' +
          '<span style="font-size:11px;font-weight:700;color:' + (on ? 'var(--purple-t)' : 'var(--muted2)') + '">' + t[1] + '</span></div>';
      }).join('') + '</div>';
  };

  /* ---------- home header ---------- */
  P.homeHeader = function (o) {
    return '<div style="padding:14px 22px 0">' +
      '<div style="display:flex;align-items:flex-start;justify-content:space-between;padding:6px 0 10px">' +
      '<span style="font-size:34px;font-weight:800;letter-spacing:-1px;color:var(--ink)">Словники</span>' + RD.logo(30) + '</div>' +
      (o && o.metrics ? '<div style="display:flex;align-items:center;gap:11px;padding-bottom:6px;font-size:14px"><span><b style="color:var(--ink)">' + o.metrics[0] + '</b> <span style="color:var(--muted);font-weight:600">словники</span></span><span style="width:4px;height:4px;border-radius:99px;background:var(--muted2)"></span><span><b style="color:var(--ink)">' + o.metrics[1] + '</b> <span style="color:var(--muted);font-weight:600">слів зібрано</span></span></div>' : '') +
      '</div>';
  };

  /* ---------- word rows ---------- */
  P.speaker = function () {
    return '<span style="flex:none;width:38px;height:38px;border-radius:12px;background:var(--tint);display:flex;align-items:center;justify-content:center">' + ic('sound', 17, 'var(--purple-t)', 1.9) + '</span>';
  };

  P.wordGroup = function (o) {
    const chev = '<span style="flex:none;display:flex;transform:rotate(' + (o.expanded ? 180 : 0) + 'deg)">' + ic('chevD', 20, o.canExpand === false ? 'var(--muted3)' : o.accentT || 'var(--purple-t)', 2) + '</span>';
    return '<div class="card" style="border-radius:18px;' + (o.highlighted ? 'border:1px solid var(--yellow);' : '') + '">' +
      '<div style="display:flex;align-items:center;gap:12px;padding:15px">' +
      '<span style="flex:1;min-width:0"><span style="display:flex;align-items:baseline;gap:9px"><span style="font-weight:800;font-size:18px;letter-spacing:-.18px;color:var(--ink);white-space:nowrap;overflow:hidden;text-overflow:ellipsis">' + o.word + '</span>' +
      (o.ipa ? '<span style="font-weight:600;font-size:13px;color:var(--muted2);white-space:nowrap">' + o.ipa + '</span>' : '') + '</span>' +
      '<span style="display:block;margin-top:3px;font-weight:600;font-size:15px;color:var(--muted);overflow:hidden;max-height:' + (o.expanded ? 'none' : '40px') + '">' + o.tr + '</span></span>' +
      P.speaker() + chev + '</div>' +
      (o.expanded && o.details ? '<div style="padding:0 15px 15px">' + o.details + '</div>' : '') +
      '</div>';
  };

  P.detailsBlock = function (o) {
    let inner = '';
    (o.senses || []).forEach(function (s, i) {
      inner += '<div style="display:flex;flex-direction:column;gap:6px">' +
        '<div style="display:flex;gap:8px;align-items:center"><span style="background:color-mix(in srgb,' + o.accentT + ' 12%, transparent);color:' + o.accentT + ';font-weight:800;font-size:12px;padding:2px 8px;border-radius:99px">' + (i + 1) + '</span>' +
        (s.pos ? '<span style="color:var(--muted2);font-weight:600;font-size:12px">' + s.pos + '</span>' : '') + '</div>' +
        '<div style="color:var(--ink);font-weight:600;font-size:14px;line-height:19px">' + s.def + '</div>' +
        (s.ex ? '<div style="color:var(--muted);font-weight:500;font-size:13px;line-height:18px">“' + s.ex + '”</div>' : '') + '</div>';
    });
    if (o.syn) inner += P.chipsRow('Синоніми', o.syn, o.accentT);
    if (o.ant) inner += P.chipsRow('Антоніми', o.ant, 'var(--orange-t)');
    if (o.forms) inner += P.chipsRow('Форми', o.forms, 'var(--muted)');
    return '<div style="display:flex;flex-direction:column;gap:14px;border-radius:13px;padding:14px;background:linear-gradient(180deg,var(--ctx1),var(--ctx2));border:1px solid var(--ctx-b)">' + inner + '</div>';
  };

  P.chipsRow = function (label, values, accent) {
    return '<div style="display:flex;flex-direction:column;gap:6px"><span style="color:var(--muted2);font-weight:800;font-size:11px;letter-spacing:.5px">' + label.toUpperCase() + '</span>' +
      '<span style="display:flex;flex-wrap:wrap;gap:6px">' + values.map(function (v) {
        return '<span style="background:color-mix(in srgb,' + accent + ' 10%, transparent);color:' + accent + ';font-weight:600;font-size:12px;padding:5px 9px;border-radius:10px">' + v + '</span>';
      }).join('') + '</span></div>';
  };

  /* ---------- detail header (expanded/collapsed) ---------- */
  P.detailHeader = function (o) {
    // o: accent, title, sub, progress 0..1, flags [a,b]
    const p = o.progress || 0;
    const H = 50 + 148 - (148 - 64) * p; // status(50) + 148→64
    const titleSize = 28 - 10 * p, titleTop = 50 + 62 - 51 * p, titleLeft = 18 + 52 * p;
    const subAlpha = Math.max(0, 1 - p * 1.45);
    return '<div style="position:relative;height:' + H + 'px;background:' + o.accent + ';border-radius:0 0 28px 28px;overflow:hidden;flex:none;' + (p > 0.02 ? 'box-shadow:0 8px 18px -8px rgba(0,0,0,.35);' : '') + '">' +
      RD.honeycomb(120, 0.18) + RD.statusbar(true) +
      '<div style="position:absolute;top:52px;left:18px;right:18px;display:flex;align-items:center;justify-content:space-between">' +
      '<span style="width:40px;height:40px;border-radius:13px;background:rgba(255,255,255,.18);display:flex;align-items:center;justify-content:center">' + ic('chevL', 22, '#fff', 2.2) + '</span>' +
      '<span style="display:flex;align-items:center;gap:6px;background:rgba(255,255,255,.16);border-radius:13px;padding:8px 12px;font-size:15px">' + o.flags[0] + ' ' + ic('arrowR', 13, 'rgba(255,255,255,.8)', 2) + ' ' + o.flags[1] + '</span></div>' +
      '<div style="position:absolute;top:' + titleTop + 'px;left:' + titleLeft + 'px;right:18px;color:#fff;font-weight:800;font-size:' + titleSize + 'px;letter-spacing:-.5px;line-height:1.1;white-space:' + (p > 0.45 ? 'nowrap' : 'normal') + ';overflow:hidden;text-overflow:ellipsis">' + o.title + '</div>' +
      (subAlpha > 0.01 ? '<div style="position:absolute;top:' + (50 + 103 - 14 * p) + 'px;left:18px;color:rgba(255,255,255,.82);font-weight:600;font-size:14.5px;opacity:' + subAlpha + '">' + o.sub + '</div>' : '') +
      '</div>';
  };

  /* ---------- add word dock ---------- */
  P.dock = function (o) {
    // o.state: idle | typing | listening | cancel ; o.accent ; o.lines (array => multi-line, grows)
    const listening = o.state === 'listening';
    const cancel = o.state === 'cancel' || o.state === 'typing';
    const lines = o.lines && o.lines.length ? o.lines : null;
    let field;
    if (listening) {
      let bars = '';
      for (let i = 0; i < 24; i++) {
        const h = 6 + Math.abs(Math.sin(i * 1.7)) * 22;
        bars += '<span style="width:4px;height:' + h.toFixed(0) + 'px;border-radius:99px;background:var(--orange);opacity:' + (0.55 + 0.45 * Math.abs(Math.cos(i))).toFixed(2) + '"></span>';
      }
      field = '<div style="flex:1;height:58px;border-radius:18px;background:var(--field);border:1.5px solid var(--line);display:flex;align-items:center;justify-content:center;gap:3px;padding:0 15px">' + bars + '</div>';
    } else if (lines) {
      // multi-line phrase: field grows, top-aligned, icon pinned to first line
      const body = lines.map(function (ln, i) { return '<span style="display:block;font-weight:700;font-size:17px;line-height:22px;color:var(--ink)">' + ln + (i === lines.length - 1 ? '<span class="caret"></span>' : '') + '</span>'; }).join('');
      field = '<div style="flex:1;min-height:58px;border-radius:18px;background:var(--surface);border:1.5px solid var(--purple);display:flex;align-items:flex-start;gap:10px;padding:16px 15px">' +
        '<span style="flex:none;margin-top:2px">' + ic('search', 19, 'var(--muted2)', 2) + '</span>' +
        '<span style="flex:1;min-width:0">' + body + '</span></div>';
    } else {
      // empty or single line: FIXED 58px, exactly the mic's height
      field = '<div style="flex:1;min-width:0;height:58px;border-radius:18px;background:' + (o.value ? 'var(--surface)' : 'var(--field)') + ';border:1.5px solid ' + (o.state === 'typing' ? 'var(--purple)' : 'var(--line)') + ';display:flex;align-items:center;gap:10px;padding:0 15px;box-sizing:border-box;overflow:hidden">' +
        ic('search', 19, 'var(--muted2)', 2) +
        (o.value
          ? '<span style="flex:1;min-width:0;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;font-weight:700;font-size:17px;color:var(--ink)">' + o.value + '<span class="caret"></span></span>'
          : '<span style="flex:1;min-width:0;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;font-weight:500;font-size:16.5px;color:var(--muted2)">Введи слово або фразу…</span>') +
        '</div>';
    }
    const micBg = cancel ? 'var(--ink)' : (listening ? 'var(--orange)' : o.accent);
    const micIc = cancel ? 'close' : 'mic';
    const micIcColor = cancel ? 'var(--bg)' : '#fff';
    return '<div style="position:absolute;left:16px;right:16px;bottom:' + (o.bottom || 24) + 'px;display:flex;align-items:flex-end;gap:10px;z-index:40">' +
      field +
      '<span style="flex:none;width:58px;height:58px;border-radius:18px;background:' + micBg + ';display:flex;align-items:center;justify-content:center;box-shadow:0 10px 22px -10px rgba(0,0,0,.4)">' + ic(micIc, 24, micIcColor, 2) + '</span></div>';
  };

  P.dockBackdrop = function (h) {
    return '<div style="position:absolute;left:0;right:0;bottom:0;height:' + (h || 140) + 'px;background:linear-gradient(180deg,transparent, color-mix(in srgb, var(--bg) 85%, transparent) 40%, var(--bg) 95%);z-index:35"></div>';
  };

  /* ---------- translation panel ---------- */
  P.panelHeader = function (q) {
    return '<div style="display:flex;align-items:center;justify-content:space-between;padding:0 4px">' +
      '<span><span style="display:block;font-weight:800;font-size:16px;color:var(--ink)">' + q + '</span>' +
      '<span style="display:block;margin-top:2px;font-weight:600;font-size:12.5px;color:var(--muted2)">Варіанти перекладу</span></span>' +
      ic('sparkle', 17, 'var(--purple-t)', 1.8) + '</div>';
  };

  P.resultRow = function (o) {
    const added = !!o.added;
    return '<div style="border-radius:17px;background:' + (added ? 'var(--neutral)' : 'var(--surface)') + ';border:1.5px solid ' + (added ? 'var(--line2)' : 'var(--line)') + ';padding:14px;display:flex;flex-direction:column;gap:12px">' +
      '<div style="display:flex;align-items:center;gap:12px">' +
      '<span style="flex:1;min-width:0"><span style="display:flex;align-items:baseline;gap:8px"><span style="font-weight:800;font-size:17px;color:var(--ink)">' + o.word + '</span>' +
      (o.ipa ? '<span style="font-weight:600;font-size:12.5px;color:var(--muted2)">' + o.ipa + '</span>' : '') +
      ic('sparkle', 11, 'var(--purple-t)', 1.7) + '</span>' +
      '<span style="display:block;margin-top:3px;font-weight:600;font-size:14.5px;color:var(--muted)">' + o.tr + '</span></span>' +
      '<span style="flex:none;width:34px;height:34px;border-radius:99px;background:var(--chip);display:flex;align-items:center;justify-content:center;transform:rotate(' + (o.expanded ? 180 : 0) + 'deg)">' + ic('chevD', 18, o.canExpand === false ? 'var(--muted3)' : o.accentT, 2.1) + '</span>' +
      '<span style="flex:none;width:44px;height:44px;border-radius:14px;background:' + (added ? 'var(--purple)' : o.accent) + ';display:flex;align-items:center;justify-content:center">' + ic(added ? 'check' : 'plus', 20, '#fff', 2.6) + '</span></div>' +
      (o.expanded && o.details ? o.details : '') +
      '</div>';
  };

  P.aiFooter = function () {
    return '<div style="display:flex;align-items:center;justify-content:center;gap:7px;padding-top:14px">' + ic('sparkle', 13, 'var(--purple-t)', 1.7) +
      '<span style="font-weight:600;font-size:12.5px;color:var(--muted2)">Переклади та приклади згенеровано AI</span></div>';
  };

  /* ---------- practice ---------- */
  P.setupRow = function (o) {
    return '<div class="card" style="height:84px;border-radius:20px;display:flex;align-items:center;gap:13px;padding:0 14px;' + (o.selected ? 'border:1.6px solid color-mix(in srgb,' + o.accent + ' 72%, transparent);' : 'border:1px solid var(--line);') + '">' +
      '<span style="flex:none;width:50px;height:50px;border-radius:16px;background:color-mix(in srgb,' + o.accent + ' ' + (o.selected ? 20 : 13) + '%, transparent);display:flex;align-items:center;justify-content:center">' + ic(o.icon || 'book', 24, o.accent, 2.1) + '</span>' +
      '<span style="flex:1;min-width:0"><span style="display:block;font-weight:800;font-size:16px;color:var(--ink);white-space:nowrap;overflow:hidden;text-overflow:ellipsis">' + o.title + '</span>' +
      '<span style="display:block;margin-top:4px;font-weight:600;font-size:13px;color:var(--muted)">' + o.sub + '</span>' +
      '<span style="display:block;margin-top:6px;height:4px;border-radius:99px;background:var(--track);overflow:hidden"><span style="display:block;height:100%;width:' + o.know + '%;background:' + o.accent + ';border-radius:99px"></span></span></span>' +
      '<span style="flex:none;width:28px;height:28px;border-radius:99px;border:2px solid ' + (o.selected ? 'var(--purple)' : 'var(--muted3)') + ';background:' + (o.selected ? 'var(--purple)' : 'transparent') + ';display:flex;align-items:center;justify-content:center">' + (o.selected ? ic('check', 17, '#fff', 2.5) : '') + '</span></div>';
  };

  P.knowledgeBars = function (level, accent) {
    let bars = '';
    for (let i = 0; i < 5; i++) bars += '<span style="width:28px;height:6px;border-radius:99px;background:' + (i < level ? accent : 'var(--track)') + '"></span>';
    return '<div style="display:flex;flex-direction:column;align-items:center;gap:8px"><span style="display:flex;gap:7px">' + bars + '</span>' +
      '<span style="font-weight:800;font-size:13px;color:var(--muted)">Рівень засвоєння: ' + level + '/5</span></div>';
  };

  P.answerBtn = function (label, icon, colorT, softBg, grow) {
    return '<div style="' + (grow ? 'flex:1;' : 'width:100%;') + 'height:62px;border-radius:19px;background:var(--surface);border:2px solid var(--line);display:flex;align-items:center;justify-content:center;gap:9px">' +
      '<span style="font-weight:800;font-size:16.5px;color:' + colorT + '">' + label + '</span>' +
      '<span style="width:30px;height:30px;border-radius:99px;background:' + softBg + ';display:flex;align-items:center;justify-content:center">' + ic(icon, 18, colorT, 2.4) + '</span></div>';
  };

  P.progressLine = function (pct) {
    return '<div style="height:7px;border-radius:99px;background:var(--track);overflow:hidden"><span style="display:block;height:100%;width:' + pct + '%;background:var(--purple);border-radius:99px"></span></div>';
  };

  /* ---------- profile ---------- */
  P.statCard = function (icon, value, label, bgVar, colorVar) {
    return '<div class="card" style="flex:1;height:118px;border-radius:18px;display:flex;flex-direction:column;align-items:center;padding:15px 10px 0">' +
      '<span style="width:38px;height:38px;border-radius:12px;background:' + bgVar + ';display:flex;align-items:center;justify-content:center">' + ic(icon, 20, colorVar, 1.9) + '</span>' +
      '<span style="margin-top:9px;font-weight:800;font-size:22px;letter-spacing:-.4px;color:var(--ink)">' + value + '</span>' +
      '<span style="font-weight:600;font-size:11.5px;line-height:14px;color:var(--muted);text-align:center">' + label + '</span></div>';
  };

  P.settingRow = function (o) {
    let right;
    if (o.toggle != null) {
      right = '<span style="width:48px;height:28px;border-radius:99px;background:' + (o.toggle ? 'var(--purple)' : 'var(--switch)') + ';display:flex;align-items:center;justify-content:' + (o.toggle ? 'flex-end' : 'flex-start') + ';padding:3px"><span style="width:22px;height:22px;border-radius:99px;background:#fff;box-shadow:0 1px 3px rgba(0,0,0,.25)"></span></span>';
    } else right = ic('chevR', 18, 'var(--muted3)', 2);
    return '<div style="display:flex;align-items:center;gap:13px;padding:15px 16px">' +
      '<span style="flex:none;width:34px;height:34px;border-radius:11px;background:var(--neutral);display:flex;align-items:center;justify-content:center;font-size:20px">' + o.lead + '</span>' +
      '<span style="flex:1;min-width:0"><span style="display:block;font-weight:700;font-size:15.5px;color:var(--ink)">' + o.label + '</span>' +
      (o.sub ? '<span style="display:block;margin-top:2px;font-weight:500;font-size:13px;color:var(--muted)">' + o.sub + '</span>' : '') + '</span>' + right + '</div>';
  };

  P.divider = function () { return '<div style="height:1px;background:var(--line2);margin-left:63px"></div>'; };
  P.sectionLabel = function (t) { return '<div style="padding:0 4px 10px;font-weight:800;font-size:12.5px;letter-spacing:.63px;color:var(--muted2)">' + t.toUpperCase() + '</div>'; };

  /* ---------- sheets ---------- */
  P.sheetFrame = function (o) {
    // returns full phone-height fragment: ghost home + scrim + sheet
    const ghost = '<div style="position:absolute;inset:0;background:var(--bg)">' + RD.statusbar(false) +
      '<div style="padding:14px 22px;opacity:.55"><div style="font-size:34px;font-weight:800;color:var(--ink);letter-spacing:-1px">Словники</div>' +
      '<div style="display:flex;gap:13px;margin-top:16px"><div style="flex:1;height:140px;border-radius:24px;background:' + RD.ACCENTS.blue + ';opacity:.5"></div><div style="flex:1;height:140px;border-radius:24px;background:' + RD.ACCENTS.indigo + ';opacity:.5"></div></div></div></div>';
    return ghost +
      '<div style="position:absolute;inset:0;background:var(--scrim)"></div>' +
      '<div style="position:absolute;left:0;right:0;bottom:0;background:var(--sheet);border-radius:28px 28px 0 0;box-shadow:0 -12px 40px -12px rgba(0,0,0,.35)">' +
      '<div style="display:flex;justify-content:center;padding:6px 0 12px"><span style="width:42px;height:5px;border-radius:99px;background:var(--handle)"></span></div>' +
      '<div style="padding:0 24px 34px">' +
      (o.title ? '<div style="display:flex;align-items:center;justify-content:space-between;padding-bottom:18px"><span style="font-weight:800;font-size:22px;letter-spacing:-.44px;color:var(--ink)">' + o.title + '</span>' +
        '<span style="width:36px;height:36px;border-radius:11px;background:var(--sheetctl);display:flex;align-items:center;justify-content:center">' + ic('close', 20, 'var(--muted2)', 2.1) + '</span></div>' : '') +
      o.body + '</div></div>';
  };

  P.langInfoStrip = function (txt) {
    return '<div style="display:flex;align-items:center;gap:8px;border-radius:14px;background:var(--bg);padding:13px 14px">' + ic('globe', 16, 'var(--muted2)', 1.8) +
      '<span style="font-weight:600;font-size:13.5px;color:var(--muted)">' + txt + '</span></div>';
  };

  P.langRow = function (flag, name, selected) {
    return '<div style="display:flex;align-items:center;gap:13px;border-radius:15px;padding:14px 15px;background:' + (selected ? 'var(--tint)' : 'var(--surface)') + ';border:1.5px solid ' + (selected ? 'var(--purple)' : 'var(--line)') + '">' +
      '<span style="font-size:22px">' + flag + '</span>' +
      '<span style="flex:1;font-weight:700;font-size:16px;color:' + (selected ? 'var(--purple-t)' : 'var(--ink)') + '">' + name + '</span>' +
      (selected ? '<span style="width:22px;height:22px;border-radius:99px;background:var(--purple);display:flex;align-items:center;justify-content:center">' + ic('check', 15, '#fff', 2.6) + '</span>' : '') + '</div>';
  };

  P.swatches = function (sel) {
    const keys = Object.keys(RD.ACCENTS);
    let rows = '';
    for (let r = 0; r < 2; r++) {
      let row = '';
      for (let c = 0; c < 4; c++) {
        const i = r * 4 + c, col = RD.ACCENTS[keys[i]];
        row += '<span style="width:46px;height:46px;border-radius:14px;background:' + col + ';display:flex;align-items:center;justify-content:center;' +
          (i === sel ? 'outline:3px solid var(--sheet);outline-offset:-3px;box-shadow:0 0 0 2.5px ' + col + ';' : '') + '">' +
          (i === sel ? ic('check', 18, '#fff', 2.6) : '') + '</span>';
      }
      rows += '<div style="display:flex;gap:11px;margin-bottom:11px">' + row + '</div>';
    }
    return rows;
  };

  P.field = function (o) {
    return '<div style="height:54px;border-radius:15px;background:' + (o.focused ? 'var(--surface)' : 'var(--field)') + ';border:1.5px solid ' + (o.focused ? 'var(--purple)' : 'var(--line)') + ';display:flex;align-items:center;padding:0 17px;font-weight:' + (o.value ? 600 : 500) + ';font-size:16px;color:' + (o.value ? 'var(--ink)' : 'var(--muted2)') + '">' + (o.value || o.placeholder) + (o.focused && o.value ? '<span class="caret"></span>' : '') + '</div>';
  };

  P.sheetLabel = function (t) { return '<div style="padding:0 2px 8px;font-weight:700;font-size:13.5px;color:var(--muted)">' + t + '</div>'; };

  /* ---------- topic icon picker (create sheet) ---------- */
  P.ICON_TOPICS = ['plane', 'book', 'film', 'brief', 'grad', 'food', 'ball', 'music', 'leaf', 'laptop', 'bag', 'heart', 'child', 'chat', 'star'];
  P.iconPicker = function (sel, accent) {
    return '<div style="display:grid;grid-template-columns:repeat(5,1fr);gap:9px">' + P.ICON_TOPICS.map(function (n, i) {
      const on = i === sel;
      return '<span style="aspect-ratio:1;border-radius:14px;display:flex;align-items:center;justify-content:center;background:' + (on ? accent : 'var(--neutral)') + ';' + (on ? 'box-shadow:0 6px 14px -6px ' + accent + ';' : '') + '">' + ic(n, 23, on ? '#fff' : 'var(--muted)', 2) + '</span>';
    }).join('') + '</div>';
  };

  P.snackbar = function (text, action, bottom) {
    return '<div style="position:absolute;left:16px;right:16px;bottom:' + (bottom || 96) + 'px;z-index:45;display:flex;align-items:center;justify-content:space-between;gap:12px;background:var(--snack);color:var(--snack-t);border-radius:16px;padding:14px 16px;box-shadow:0 14px 30px -10px rgba(0,0,0,.5)">' +
      '<span style="font-weight:600;font-size:14.5px">' + text + '</span>' +
      (action ? '<span style="font-weight:800;font-size:14.5px;color:var(--yellow)">' + action + '</span>' : '') + '</div>';
  };
})();
