/* ============================================================
   Boards 7 (v2): «підглянути й зберегти» слово з речення
   Тап = фліп-переклад in-place · Лонг-тап = закладка
   (синхронізовано з claude.ai/design; повний вміст — у проєкті
   «vocabee 2», тут — файл для локального відкриття канви)
   ============================================================ */
(function () {
  const ic = RD.ic, A = RD.ACCENTS;
  const Y0 = 18950, Y1 = 19110, Y2 = 20110;

  RD.sec({ x: 80, y: Y0, num: '14', text: 'Контекст · підглянути й зберегти слово', sub: 'Будь-яке слово речення, крім цільового жовтого: ТАП — слово фліпається в переклад прямо в реченні (3D rotateX, 250мс, на ~2с); ЛОНГ-ТАП — у закладки без перекладу, летить у бейдж біля хедера. Жодних барів і шитів — тренування не переривається. 5 безкоштовних підглядань за раунд, далі 1 монетка.' });

  const bmIc = (s, c) => '<span style="display:inline-flex;vertical-align:-2px">' + ic('bookmark', s, c, 2.2) + '</span>';
  function sessHead(o) {
    const badge = o.bm
      ? '<span style="flex:none;display:inline-flex;align-items:center;gap:5px;background:color-mix(in srgb, var(--yellow) 25%, transparent);border-radius:99px;padding:8px 12px;font-weight:800;font-size:13px;color:var(--ny-t);' + (o.pulse ? 'box-shadow:0 0 0 4px color-mix(in srgb, var(--yellow) 18%, transparent);' : '') + '">' + ic('bookmark', 14, 'var(--ny-t)', 2.2) + o.bm + '</span>'
      : '';
    return RD.statusbar(false) +
      '<div style="padding:8px 24px 0">' +
      '<div style="display:flex;align-items:center;gap:9px">' +
      '<span style="flex:1;min-width:0"><span style="display:block;font-size:22px;font-weight:800;letter-spacing:-.4px;color:var(--ink)">Слово в контексті</span>' +
      '<span style="display:block;margin-top:3px;font-size:13.5px;font-weight:700;color:var(--muted)">3 / 10 · правильно 2</span></span>' +
      badge +
      '<span style="flex:none;width:40px;height:40px;border-radius:13px;background:var(--neutral);display:flex;align-items:center;justify-content:center">' + ic('close', 19, 'var(--muted)', 2.2) + '</span></div>' +
      '<div style="margin-top:12px">' + P.progressLine(30) + '</div></div>';
  }
  const hlY = (w) => '<span style="background:var(--yellow);color:#5A4500;font-weight:800;border-radius:7px;padding:1px 7px;white-space:nowrap">' + w + '</span>';
  function ansChip(t) {
    return '<span style="height:48px;border-radius:99px;display:flex;align-items:center;justify-content:center;font-weight:800;font-size:15px;background:var(--neutral);color:var(--ink);border:1.6px solid transparent">' + t + '</span>';
  }

  function seg(mode) {
    const flipBase = 'display:inline-block;background:var(--tint);color:var(--purple-t);border-bottom:2px solid var(--purple-t);border-radius:7px 7px 3px 3px;padding:1px 8px;font-weight:800;white-space:nowrap';
    if (mode === 'flip' || mode === 'counter') return '<span style="position:relative;display:inline-block"><span style="' + flipBase + '">пекарня ' + bmIc(13, 'var(--purple-t)') + '</span>' +
      (mode === 'counter' ? '<span style="position:absolute;left:0;top:calc(100% + 6px);white-space:nowrap;background:var(--surface);border:1px solid var(--line);border-radius:99px;padding:4px 9px;font-size:11px;font-weight:700;color:var(--muted);box-shadow:var(--elev1)">ще 1 безкоштовне</span>' : '') + '</span>';
    if (mode === 'shimmer') return '<span style="display:inline-block;width:92px;height:26px;vertical-align:-4px;border-radius:8px;background:linear-gradient(100deg, var(--neutral) 20%, var(--line) 50%, var(--neutral) 80%)"></span>';
    if (mode === 'savedPre') return '<span style="display:inline-block;background:color-mix(in srgb,' + A.teal + ' 14%, transparent);color:' + A.teal + ';border-bottom:2px solid ' + A.teal + ';border-radius:7px 7px 3px 3px;padding:1px 8px;font-weight:800;white-space:nowrap">пекарня · булочна</span>' +
      ' <span style="display:inline-flex;align-items:center;gap:4px;vertical-align:2px;border:1.2px solid ' + A.teal + ';border-radius:99px;padding:3px 8px;font-size:10.5px;font-weight:800;color:' + A.teal + '">' + ic('plane', 11, A.teal, 2.4) + 'у Подорожі</span>';
    if (mode === 'savedPost') return '<span style="display:inline-block;background:color-mix(in srgb,' + A.teal + ' 14%, transparent);border-bottom:2px solid ' + A.teal + ';border-radius:7px 7px 3px 3px;padding:1px 8px;white-space:nowrap"><span style="font-weight:800;color:' + A.teal + '">пекарня</span><span style="font-weight:700;color:color-mix(in srgb,' + A.teal + ' 45%, transparent)"> · булочна</span></span>' +
      ' <span style="display:inline-flex;align-items:center;gap:4px;vertical-align:2px;border:1.2px solid ' + A.teal + ';border-radius:99px;padding:3px 8px;font-size:10.5px;font-weight:800;color:' + A.teal + '">' + ic('plane', 11, A.teal, 2.4) + 'у Подорожі</span>';
    if (mode === 'gate') return '<span style="position:relative;display:inline-block"><span style="display:inline-block;background:var(--neutral);border-bottom:2px dashed var(--muted3);border-radius:7px 7px 3px 3px;padding:1px 8px;font-weight:800;color:var(--ink);white-space:nowrap">bakery</span>' +
      '<span style="position:absolute;left:50%;bottom:calc(100% + 10px);transform:translateX(-50%);white-space:nowrap;display:inline-flex;align-items:center;gap:8px;background:var(--peach);border:1.2px solid var(--peach-b);border-radius:13px;padding:9px 13px;box-shadow:var(--elev2)">' +
      '<span style="display:inline-flex;align-items:center;gap:4px;font-size:12.5px;font-weight:800;color:var(--orange-t)">' + RD.coin(14) + '1 монетка</span>' +
      '<span style="width:1px;height:14px;background:var(--peach-b)"></span>' +
      '<span style="display:inline-flex;align-items:center;gap:5px;font-size:12.5px;font-weight:800;color:#fff;background:var(--orange);border-radius:9px;padding:5px 9px">' + ic('play', 13, '#fff', 2) + 'Відео за +10</span>' +
      '<span style="position:absolute;left:50%;top:100%;transform:translateX(-50%);width:0;height:0;border:7px solid transparent;border-top-color:var(--peach)"></span></span></span>';
    if (mode === 'fly') return '<span style="display:inline-block;background:var(--tint);color:var(--purple-t);border-radius:7px;padding:1px 8px;font-weight:800;white-space:nowrap">downtown</span>';
    return 'bakery';
  }

  function card(o) {
    o = o || {};
    const bak = seg(o.mode);
    const down = o.mode === 'fly' ? seg('fly') : 'downtown';
    const sent = 'He ' + hlY('runs') + ' a small ' + (o.mode === 'fly' ? 'bakery' : bak) + ' ' + down + '.';
    return '<div style="padding:16px 24px 0"><div class="card" style="border-radius:28px;padding:22px;box-shadow:var(--elev2);' + (o.mode === 'gate' ? 'overflow:visible' : '') + '">' +
      '<div style="display:flex;justify-content:space-between;align-items:center">' +
      '<span style="display:inline-flex;align-items:center;gap:6px;border-radius:99px;padding:6px 12px;font-weight:800;font-size:12px;background:var(--tint);color:var(--purple-t)">EN → UK · розуміння</span>' +
      '<span style="font-size:12px;font-weight:700;color:var(--muted2)">впізнавання</span></div>' +
      '<div style="margin-top:' + (o.mode === 'gate' ? 44 : 18) + 'px;font-size:22px;line-height:1.62;font-weight:700;color:var(--ink)">' + sent + '</div>' +
      '<div style="margin-top:' + (o.mode === 'counter' ? 30 : 10) + 'px;font-size:14px;font-weight:600;color:var(--muted)">Що означає тут?</div>' +
      '<div style="margin-top:16px;display:grid;grid-template-columns:1fr 1fr;gap:10px">' +
      ansChip('бігає') + ansChip('керує') + ansChip('балотується') + ansChip('тече') + '</div>' +
      '</div></div>';
  }

  RD.frame({ x: 80, y: Y1, theme: 'light', label: '1 · тап: bakery фліпається в «пекарня»', body: sessHead({ bm: 1 }) + card({ mode: 'flip' }) + '<div style="position:absolute;left:0;right:0;bottom:34px;text-align:center;font-size:12.5px;font-weight:600;color:var(--muted2)">повернеться через 2с · тап — раніше · ' + bmIc(12, 'var(--muted2)') + ' — у закладки</div>' });
  RD.frame({ x: 550, y: Y1, theme: 'dark', label: '2 · loading · шимер ~300мс', body: sessHead({ bm: 1 }) + card({ mode: 'shimmer' }) });
  RD.frame({ x: 1020, y: Y1, theme: 'dark', label: '3 · фліп із лічильником безкоштовних', body: sessHead({ bm: 1 }) + card({ mode: 'counter' }) });
  RD.frame({ x: 1490, y: Y1, theme: 'light', label: '4 · збережене слово · до ранжування', body: sessHead({}) + card({ mode: 'savedPre' }) + '<div style="position:absolute;left:0;right:0;bottom:34px;text-align:center;font-size:12.5px;font-weight:600;color:var(--muted2)">збережені переклади — миттєво, безкоштовно, офлайн</div>' });
  RD.frame({ x: 1960, y: Y1, theme: 'dark', label: '5 · збережене · найдоречніше виділено (~0.5с)', body: sessHead({}) + card({ mode: 'savedPost' }) + '<div style="position:absolute;left:0;right:0;bottom:34px;text-align:center;font-size:12.5px;font-weight:600;color:var(--muted2)">лонг-тап на збереженому → «відкрити у словнику»</div>' });

  RD.frame({ x: 80, y: Y2, theme: 'light', label: '6 · безкоштовні вичерпані + 0 монеток · тултип', body: sessHead({ bm: 1 }) + card({ mode: 'gate' }) });

  RD.frame({
    x: 550, y: Y2, theme: 'dark', label: '7 · лонг-тап: закладка летить у бейдж',
    body: sessHead({ bm: 2, pulse: true }) + card({ mode: 'fly' }) +
      '<span style="position:absolute;top:238px;right:150px;z-index:30;display:inline-flex;align-items:center;gap:5px;background:var(--surface);border:1.2px solid var(--line);border-radius:99px;padding:6px 11px;font-weight:800;font-size:12.5px;color:var(--ink);box-shadow:var(--elev2);transform:rotate(-8deg)">' + ic('bookmark', 13, 'var(--ny-t)', 2.2) + 'downtown</span>' +
      '<span style="position:absolute;top:274px;right:118px;z-index:29;display:inline-flex;border-radius:99px;padding:6px 11px;font-weight:800;font-size:12.5px;background:var(--surface);color:var(--muted2);opacity:.35;transform:rotate(-5deg)">downtown</span>' +
      '<span style="position:absolute;top:306px;right:96px;z-index:28;display:inline-flex;border-radius:99px;padding:6px 11px;font-weight:800;font-size:12.5px;background:var(--surface);color:var(--muted2);opacity:.15">downtown</span>' +
      '<div style="position:absolute;left:0;right:0;bottom:34px;text-align:center;font-size:12.5px;font-weight:600;color:var(--muted2)">без перекладу і без пауз — розбір після раунду</div>',
  });

  function newWordRow(o) {
    if (o.done) return '<div style="display:flex;align-items:center;gap:11px;padding:12px 2px;opacity:.75">' +
      '<span style="flex:none;width:36px;height:36px;border-radius:12px;background:var(--soft-green);display:flex;align-items:center;justify-content:center">' + ic('check', 17, 'var(--green-t)', 2.6) + '</span>' +
      '<span style="flex:1;min-width:0"><span style="font-weight:800;font-size:15.5px;color:var(--ink)">' + o.w + '</span>' +
      '<span style="display:block;margin-top:1px;font-size:12px;font-weight:600;color:var(--muted)">додано в Подорожі</span></span></div>';
    return '<div style="display:flex;align-items:center;gap:11px;padding:12px 2px">' +
      '<span style="flex:none;width:36px;height:36px;border-radius:12px;background:color-mix(in srgb, var(--yellow) 25%, transparent);display:flex;align-items:center;justify-content:center">' + ic('bookmark', 16, 'var(--ny-t)', 2.2) + '</span>' +
      '<span style="flex:1;min-width:0"><span style="font-weight:800;font-size:15.5px;color:var(--ink)">' + o.w + ' <span style="font-weight:600;font-size:12px;color:var(--muted2)">' + (o.ipa || '') + '</span></span>' +
      '<span style="display:block;margin-top:1px;font-size:12px;font-weight:600;color:var(--muted)">з речення: “He runs a small bakery…”</span></span>' +
      ic('chevR', 17, 'var(--muted3)', 2.2) + '</div>';
  }
  RD.frame({
    x: 1020, y: Y2, theme: 'light', label: '8 · результат · нові слова з речень',
    body: RD.statusbar(false) +
      '<div style="padding:8px 24px 0"><div style="font-size:22px;font-weight:800;letter-spacing:-.4px;color:var(--ink)">Слово в контексті</div></div>' +
      '<div style="padding:18px 24px 0;display:flex;align-items:center;gap:16px">' +
      '<div style="position:relative;flex:none;width:96px;height:96px;display:flex;align-items:center;justify-content:center">' +
      '<svg width="96" height="96" viewBox="0 0 96 96"><circle cx="48" cy="48" r="39" fill="none" stroke="var(--track)" stroke-width="10"/><circle cx="48" cy="48" r="39" fill="none" stroke="var(--purple)" stroke-width="10" stroke-linecap="round" stroke-dasharray="245" stroke-dashoffset="49" transform="rotate(-90 48 48)"/></svg>' +
      '<span style="position:absolute;font-size:22px;font-weight:800;color:var(--purple-t)">80%</span></div>' +
      '<span><span style="display:block;font-size:21px;font-weight:800;color:var(--ink)">Раунд завершено</span>' +
      '<span style="display:block;margin-top:4px;font-size:14px;font-weight:500;color:var(--muted)">Правильних: 8 із 10</span></span></div>' +
      '<div style="padding:18px 22px 0">' +
      '<div class="card" style="border-radius:18px;padding:14px 16px;border:1px solid var(--peach-b)">' +
      '<div style="font-weight:800;font-size:11.5px;letter-spacing:.6px;color:var(--orange-t)">НАЙПЛУТАНІША ПАРА</div>' +
      '<div style="margin-top:8px;font-size:14px;font-weight:700;color:var(--ink)">run = керує <span style="color:var(--muted2)">⇄</span> run = бігає <span style="font-weight:600;font-size:12.5px;color:var(--muted)">· 2 помилки</span></div></div>' +
      '<div class="card" style="border-radius:18px;padding:14px 16px;margin-top:12px">' +
      '<div style="display:flex;align-items:center;justify-content:space-between"><span style="font-weight:800;font-size:11.5px;letter-spacing:.6px;color:var(--muted2)">НОВІ СЛОВА З РЕЧЕНЬ (1)</span>' + ic('bookmark', 15, 'var(--ny-t)', 2.2) + '</div>' +
      newWordRow({ w: 'bakery', ipa: '/ˈbeɪkəri/' }) +
      '<div style="height:1px;background:var(--line2)"></div>' +
      newWordRow({ w: 'downtown', done: true }) +
      '<div style="margin-top:8px;height:48px;border-radius:14px;background:var(--tint);display:flex;align-items:center;justify-content:center;gap:8px;font-weight:800;font-size:14.5px;color:var(--purple-t)">Розібрати всі (1)</div></div></div>' +
      '<div style="position:absolute;left:24px;right:24px;bottom:34px">' + P.btn('Ще раунд', 'primary') + '</div>',
  });

  RD.note({
    x: 1490, y: Y2 + 40, w: 340, title: 'Правила «підглянути й зберегти»',
    items: [
      '<b>Тап</b> = фліп у переклад (rotateX 180°, 250мс, як флеш-картки), тримається ~2с або до повторного тапу; <b>лонг-тап</b> = закладка без перекладу',
      'Цільове жовте слово не інтерактивне — воно питання картки',
      '<b>5 безкоштовних підглядань/раунд</b> (конфіг з бекенда), далі 1 монетка; кеш на раунд — повторний тап того ж слова безкоштовний',
      'Лічильник показується від ≤2 залишку («ще 1 безкоштовне»), після вичерпання — «1 монетка»',
      'Збережені слова: teal, безкоштовно й офлайн; при мережі за ~0.5с найдоречніше значення стає жирним першим; лонг-тап → «відкрити у словнику»',
      'На фліпі — міні-закладка: тап додає в закладки разом із перекладом',
      'Розбір закладок: блок у результаті раунду → стандартний Add Word; розібрані отримують ✓ і зникають з лічильника',
    ],
  });
})();
