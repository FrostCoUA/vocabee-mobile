/* ============================================================
   Boards 2: Додати слово, Тренування, Профіль
   ============================================================ */
(function () {
  const ic = RD.ic, A = RD.ACCENTS;

  /* ================= SECTION 4 — Додати слово ================= */
  RD.sec({ x: 80, y: 4400, num: '4', text: 'Додати слово · док + панель', sub: 'Поле і мік завжди внизу словника. Введення тексту або голос → повноекранна панель варіантів. Дебаунс 700мс, 1 монетка списується перед запитом. Мік: утримання 250мс = запис (hold-to-talk), відпустив — стоп із ґрейсом 700мс.' });

  const RESULTS = [
    { word: 'resilience', ipa: '/rɪˈzɪliəns/', tr: 'стійкість, витривалість', added: true },
    { word: 'resilient', ipa: '/rɪˈzɪliənt/', tr: 'стійкий, витривалий' },
    { word: 'resiliently', ipa: '', tr: 'стійко' },
  ];
  function panelBody(o) {
    const rows = (o.rows || RESULTS).map(function (r) {
      return P.resultRow(Object.assign({ accent: A.indigo, accentT: 'var(--purple-t)' }, r));
    }).join('');
    return '<div style="position:absolute;inset:0;background:var(--surface)">' + RD.statusbar(false) +
      '<div style="padding:12px 24px 0;display:flex;flex-direction:column;gap:10px;height:100%;box-sizing:border-box">' +
      P.panelHeader(o.q) +
      '<div style="display:flex;flex-direction:column;gap:9px;overflow:hidden;flex:1;padding-bottom:120px">' + rows + '</div>' +
      '</div></div>' +
      P.dockBackdrop(150) + P.dock({ state: 'typing', value: o.value, accent: A.indigo });
  }

  RD.frame({ x: 80, y: 4560, theme: 'light', label: 'Панель перекладів · результати', body: panelBody({ q: 'resilience', value: 'resilience' }) });
  RD.frame({ x: 550, y: 4560, theme: 'dark', label: 'Панель перекладів · dark', body: panelBody({ q: 'resilience', value: 'resilience' }) });

  /* listening state over detail */
  RD.frame({
    x: 1020, y: 4560, theme: 'light', label: 'Голос · слухаю (мік = orange)',
    body: '<div style="display:flex;flex-direction:column;height:100%">' +
      P.detailHeader({ accent: A.indigo, title: 'Книга · «1984»', sub: '7 слів · сьогодні', progress: 0, flags: ['🇬🇧', '🇺🇦'] }) +
      '<div style="flex:1;position:relative;opacity:.5">' +
      '<div style="padding:10px 16px;display:flex;flex-direction:column;gap:10px">' +
      P.wordGroup({ word: 'resilience', ipa: '/rɪˈzɪliəns/', tr: 'стійкість', accentT: 'var(--purple-t)' }) +
      P.wordGroup({ word: 'reluctant', ipa: '/rɪˈlʌktənt/', tr: 'неохочий', accentT: 'var(--purple-t)' }) +
      '</div></div></div>' +
      P.snackbar('Голосове введення перервано: немає мови', null, 108) +
      P.dockBackdrop() + P.dock({ state: 'listening', accent: A.indigo }),
  });

  /* voice recognition error — full screen (was leaking English raw string) */
  function voiceErrorBody(o) {
    o = o || {};
    return '<div style="position:absolute;inset:0;background:var(--surface);display:flex;flex-direction:column">' +
      RD.statusbar(false) +
      '<div style="display:flex;align-items:center;justify-content:space-between;padding:2px 24px 0">' +
      '<span><span style="display:block;font-weight:800;font-size:16px;color:var(--ink)">Голосове введення</span>' +
      '<span style="display:block;margin-top:2px;font-weight:600;font-size:12.5px;color:var(--muted2)">Спробуй ще раз</span></span>' +
      ic('sparkle', 17, 'var(--purple-t)', 1.8) + '</div>' +
      '<div style="flex:1;display:flex;flex-direction:column;align-items:center;justify-content:center;text-align:center;padding:0 40px;gap:11px">' +
      '<span style="width:76px;height:76px;border-radius:99px;background:color-mix(in srgb, var(--orange-t) 12%, transparent);display:flex;align-items:center;justify-content:center">' + ic('mic', 32, 'var(--orange-t)', 2) + '</span>' +
      '<div style="margin-top:6px;font-size:20px;font-weight:800;color:var(--ink)">Не вдалося розпізнати</div>' +
      '<div style="font-size:14.5px;line-height:20px;font-weight:500;color:var(--muted)">Скажи слово трохи повільніше й ближче до мікрофона — або введи його вручну.</div>' +
      '<div style="margin-top:8px;height:50px;border-radius:16px;background:var(--tint);display:flex;align-items:center;justify-content:center;gap:8px;padding:0 22px;font-weight:800;font-size:15px;color:var(--purple-t)">' + ic('mic', 18, 'var(--purple-t)', 2) + 'Спробувати ще раз</div>' +
      '</div></div>' +
      P.dockBackdrop(150) + P.dock({ state: 'cancel', accent: A.indigo });
  }
  RD.frame({ x: 1490, y: 4560, theme: 'dark', label: 'Голос · не розпізнано (перероблено)', body: voiceErrorBody() });
  RD.frame({ x: 1960, y: 4560, theme: 'light', label: 'Голос · не розпізнано · light', body: voiceErrorBody() });

  /* panel states board */
  function miniPanel(inner, label, theme) {
    return '<div style="width:318px"><div class="t-' + (theme || 'light') + '" style="position:relative;height:250px;border-radius:20px;overflow:hidden;background:var(--surface);border:1px solid ' + (theme === 'dark' ? '#262D3E' : '#E7E8EE') + ';padding:18px;box-sizing:border-box;color:var(--ink)">' + inner + '</div>' +
      '<div style="margin-top:9px;text-align:center;font-size:12px;font-weight:800;color:#6B7280">' + label + '</div></div>';
  }
  const centered = 'display:flex;flex-direction:column;align-items:center;justify-content:center;height:100%;text-align:center;gap:10px';
  RD.board({
    x: 2430, y: 4560, w: 790, label: 'Стани панелі',
    body: '<div style="display:flex;flex-wrap:wrap;gap:26px">' +
      miniPanel('<div style="' + centered + '"><span class="spinner" style="width:32px;height:32px;border-radius:99px;border:3px solid var(--tint);border-top-color:var(--purple)"></span><span style="font-size:14px;font-weight:600;color:var(--muted)">Шукаю переклад…</span></div>', 'Loading · спінер в accent') +
      miniPanel('<div style="' + centered + '">' + ic('close', 26, 'var(--orange-t)', 2) + '<span style="font-size:16px;font-weight:800">Не вдалось отримати переклад</span><span style="font-size:13.5px;font-weight:500;color:var(--muted)">Перевір інтернет і спробуй ще раз</span></div>', 'Error · без списання монетки', 'dark') +
      miniPanel('<div style="' + centered + '">' + ic('search', 26, 'var(--muted3)', 1.7) + '<span style="font-size:14.5px;font-weight:600;color:var(--muted2)">Нічого не знайдено для «resilliance»</span></div>', 'Порожньо') +
      miniPanel('<div style="' + centered + '">' + ic('mic', 24, 'var(--orange-t)', 2) + '<span style="font-size:16px;font-weight:800">Не вдалося розпізнати</span><span style="font-size:13.5px;font-weight:500;color:var(--muted)">Скажи слово ще раз, ближче до мікрофона</span></div>', 'Помилка голосу → повний борд ліворуч', 'dark') +
      '</div>',
  });

  RD.note({
    x: 3260, y: 4560, w: 300, title: 'Док · стани міка',
    items: [
      '<b>Idle</b>: мік = акцент словника',
      '<b>Typing / панель відкрита</b>: кнопка стає ✕ (Ink) — закрити пошук',
      '<b>Listening</b>: мік orange, у полі — хвиля з 24 барів (4×5..28dp, цикл 700мс)',
      '<b>Помилка голосу:</b> ✕ закриває, окрема кнопка «Спробувати ще раз» перезапускає запис',
      '<b>Фікс:</b> сабтекст був сирим англ. «Could not recognize the word» — тепер локалізовано; хедер у помилці більше не пише «Варіанти перекладу»',
      'Ліміт: гість без монеток → шит-гейт замість запиту',
    ],
  });

  /* dock height behaviour */
  function dockDemo(o, label, theme) {
    return '<div style="width:390px"><div class="t-' + (theme || 'light') + '" style="position:relative;height:118px;border-radius:20px;overflow:hidden;background:var(--bg);border:1px solid ' + (theme === 'dark' ? '#262D3E' : '#E7E8EE') + '">' +
      P.dock(Object.assign({ accent: A.indigo, bottom: 18 }, o)) + '</div>' +
      '<div style="margin-top:9px;text-align:center;font-size:12px;font-weight:800;color:#6B7280">' + label + '</div></div>';
  }
  RD.board({
    x: 3260, y: 4990, w: 466, label: 'Висота поля вводу',
    body: '<div style="display:flex;flex-direction:column;gap:16px">' +
      dockDemo({ state: 'idle' }, 'Пусте — 58dp (= мік)') +
      dockDemo({ state: 'typing', value: 'resilience' }, 'Один рядок — 58dp (= мік)') +
      dockDemo({ state: 'typing', lines: ['a piece of', 'cake'] }, 'Кілька рядків — росте вгору', 'dark') +
      '<div style="font-size:12.5px;line-height:1.55;color:#374151;font-weight:500">Поле і мік вирівняні по <b>низу</b>. Пусте або 1 рядок = фіксовані 58dp, як у міка (плейсхолдер не переноситься — nowrap + …). З 2-го рядка поле росте вгору (min-height 58 → до ~150dp / 5 рядків), мік лишається 58dp унизу.</div>' +
      '</div>',
  });

  /* ================= SECTION 5 — Тренування ================= */
  RD.sec({ x: 80, y: 5560, num: '5', text: 'Тренування', sub: 'Спершу вибір словників, потім раунд з 10 фліп-карток за найгіршим засвоєнням. На лицьовій — контекстне речення з тапабельними словами (секція 13). «Знаю» +20%, «Не знаю» −20% і показ перекладу з «Далі».' });

  function setupBody() {
    return RD.statusbar(false) +
      '<div style="padding:8px 24px 0"><div style="display:flex;align-items:center;justify-content:space-between"><span style="font-size:30px;font-weight:800;letter-spacing:-.6px;color:var(--ink)">Тренування</span>' + P.flagChip('🇬🇧') + '</div>' +
      '<div style="margin-top:8px;font-size:15px;line-height:20px;font-weight:600;color:var(--muted)">Вибери словники для короткого раунду повторення.</div>' +
      '<div style="display:flex;align-items:center;margin-top:16px;padding-bottom:8px"><span style="font-weight:800;font-size:12.5px;letter-spacing:.63px;color:var(--muted2)">СЛОВНИКИ</span><span style="flex:1"></span><span style="font-weight:800;font-size:13px;color:var(--purple-t)">Вибрати всі</span></div></div>' +
      '<div style="padding:0 22px;display:flex;flex-direction:column;gap:12px">' +
      P.setupRow({ title: 'Подорожі', sub: '6 слів · 35% знаю', know: 35, accent: A.blue, selected: true, icon: 'plane' }) +
      P.setupRow({ title: 'Книга · «1984»', sub: '7 слів · 20% знаю', know: 20, accent: A.indigo, selected: true, icon: 'book' }) +
      P.setupRow({ title: 'Робота', sub: '5 слів · 60% знаю', know: 60, accent: A.grape, icon: 'brief' }) +
      P.setupRow({ title: 'Емоції', sub: '5 слів · 45% знаю', know: 45, accent: A.violet, icon: 'heart' }) +
      '</div>' +
      '<div style="position:absolute;left:0;right:0;bottom:80px;background:var(--surface);box-shadow:0 -10px 30px -18px rgba(0,0,0,.35);padding:14px 22px 18px;z-index:20">' +
      '<div style="display:flex;align-items:center;gap:8px;padding-bottom:10px;font-size:13px"><b style="color:var(--ink)">Вибрано: 2 теми</b><span style="width:4px;height:4px;border-radius:99px;background:var(--muted3)"></span><span style="font-weight:700;color:var(--muted)">13 слів</span></div>' +
      P.btn('Почати тренування', 'primary') + '</div>' +
      P.bottomBar(1);
  }
  RD.frame({ x: 80, y: 5720, theme: 'light', label: 'Практика · вибір словників', body: setupBody() });
  RD.frame({ x: 550, y: 5720, theme: 'dark', label: 'Практика · вибір · dark', body: setupBody() });

  function sessHead(step, correct) {
    return RD.statusbar(false) + '<div style="padding:8px 24px 4px"><div style="display:flex;align-items:center;gap:9px"><span style="flex:1;font-size:30px;font-weight:800;letter-spacing:-.6px;color:var(--ink)">Тренування</span>' +
      '<span style="flex:none;display:inline-flex;align-items:center;gap:5px;background:color-mix(in srgb, var(--yellow) 28%, transparent);border-radius:99px;padding:9px 12px;font-weight:800;font-size:13px;color:var(--ny-t)">' + ic('bookmark', 14, 'var(--ny-t)', 2.2) + '1</span>' +
      '<span style="flex:none;width:44px;height:44px;border-radius:14px;background:var(--neutral);display:flex;align-items:center;justify-content:center">' + ic('close', 21, 'var(--muted2)', 2.2) + '</span></div>' +
      '<div style="margin:10px 0 8px;font-size:13.5px;font-weight:700;color:var(--muted)">' + step + ' / 10 · правильно ' + correct + '</div>' +
      P.progressLine(step * 10) + '</div>';
  }
  RD.frame({
    x: 1020, y: 5720, theme: 'light', label: 'Раунд · лицьова сторона',
    body: sessHead(3, 2) +
      '<div style="position:absolute;top:196px;left:26px;right:26px;bottom:196px;border-radius:28px;background:var(--surface);border:2px solid ' + A.blue + ';box-shadow:var(--elev2);overflow:hidden">' +
      '<div style="position:absolute;left:0;top:0;bottom:0;width:40%;background:color-mix(in srgb,' + A.blue + ' 8%, transparent)"></div>' +
      '<div style="position:relative;display:flex;flex-direction:column;align-items:center;height:100%;padding:26px 26px 0;box-sizing:border-box">' +
      '<span style="display:inline-flex;align-items:center;gap:7px;background:color-mix(in srgb,' + A.blue + ' 12%, transparent);border-radius:99px;padding:7px 12px;font-weight:800;font-size:13px;color:' + A.blue + '"><span style="width:8px;height:8px;border-radius:99px;background:' + A.blue + '"></span>Подорожі</span>' +
      '<div style="margin-top:26px;font-size:40px;font-weight:800;letter-spacing:-1.2px;color:var(--ink)">wander</div>' +
      '<div style="margin-top:8px;font-size:16px;font-weight:600;color:var(--muted2)">/ˈwɒndə/</div>' +
      '<div style="margin-top:16px">' + P.knowledgeBars(2, A.blue) + '</div>' +
      '<span style="margin-top:14px;width:48px;height:48px;border-radius:15px;background:var(--neutral);display:flex;align-items:center;justify-content:center">' + ic('sound', 21, 'var(--purple-t)', 1.9) + '</span>' +
      '<div style="position:absolute;left:26px;right:26px;bottom:22px;text-align:center;font-size:14px;line-height:22px;font-weight:600;color:var(--muted)">He likes to <span style="background:var(--yellow);color:#5A4500;font-weight:800;border-radius:4px;padding:0 4px">wander</span> around old <span style="background:color-mix(in srgb, var(--yellow) 30%, transparent);color:var(--ny-t);font-weight:800;border-radius:5px;padding:0 4px">towns</span>.</div>' +
      '</div></div>' +
      '<div style="position:absolute;left:24px;right:24px;bottom:106px;display:flex;gap:12px">' +
      P.answerBtn('Не знаю', 'close', 'var(--orange-t)', 'var(--peach)', true) +
      P.answerBtn('Знаю', 'check', 'var(--green-t)', 'var(--soft-green)', true) + '</div>' +
      P.bottomBar(1),
  });
  RD.frame({
    x: 1490, y: 5720, theme: 'dark', label: 'Раунд · переклад після «Не знаю»',
    body: sessHead(3, 2) +
      '<div style="position:absolute;top:196px;left:26px;right:26px;bottom:196px;border-radius:28px;background:' + A.blue + ';box-shadow:var(--elev2);overflow:hidden">' +
      '<div style="position:absolute;left:0;top:0;bottom:0;width:40%;background:rgba(0,0,0,.13)"></div>' +
      '<div style="position:relative;display:flex;flex-direction:column;align-items:center;justify-content:center;height:100%;padding:30px;box-sizing:border-box">' +
      '<div style="font-size:32px;font-weight:800;letter-spacing:-.6px;color:#fff;text-align:center">блукати, мандрувати</div>' +
      '<div style="position:absolute;left:30px;right:30px;bottom:24px;text-align:center;font-size:14px;line-height:20px;font-weight:600;color:rgba(255,255,255,.82)">Він любить блукати старими містами.</div></div></div>' +
      '<div style="position:absolute;left:24px;right:24px;bottom:106px">' +
      '<div style="height:62px;border-radius:19px;background:var(--tint);display:flex;align-items:center;justify-content:center;gap:9px;font-weight:800;font-size:16.5px;color:var(--purple-t)">Далі ' + ic('chevR', 20, 'var(--purple-t)', 2.4) + '</div></div>' +
      P.bottomBar(1),
  });

  /* done state */
  RD.frame({
    x: 1960, y: 5720, theme: 'light', label: 'Раунд завершено',
    body: RD.statusbar(false) + '<div style="padding:8px 24px 4px"><div style="font-size:30px;font-weight:800;letter-spacing:-.6px;color:var(--ink)">Тренування</div></div>' +
      '<div style="position:absolute;top:180px;left:36px;right:36px;bottom:120px;display:flex;flex-direction:column;align-items:center;justify-content:center">' +
      '<div style="position:relative;width:150px;height:150px;display:flex;align-items:center;justify-content:center">' +
      '<svg width="150" height="150" viewBox="0 0 150 150"><circle cx="75" cy="75" r="63" fill="none" stroke="var(--track)" stroke-width="12"/><circle cx="75" cy="75" r="63" fill="none" stroke="var(--purple)" stroke-width="12" stroke-linecap="round" stroke-dasharray="395.8" stroke-dashoffset="79" transform="rotate(-90 75 75)"/></svg>' +
      '<span style="position:absolute;font-size:34px;font-weight:800;letter-spacing:-.7px;color:var(--purple-t)">80%</span></div>' +
      '<div style="margin-top:22px;font-size:24px;font-weight:800;color:var(--ink)">Раунд завершено</div>' +
      '<div style="margin-top:9px;font-size:15.5px;font-weight:500;color:var(--muted)">Правильних відповідей: 8 із 10.</div>' +
      '<div style="margin-top:26px;width:100%">' + P.btn('Ще раунд', 'primary') + '</div>' +
      '<div style="margin-top:10px;width:100%">' + P.btn('Обрати теми', 'neutral', { style: 'border:1.4px solid var(--line);' }) + '</div></div>' +
      P.bottomBar(1),
  });

  RD.note({
    x: 2440, y: 5760, w: 300, title: 'Логіка раунду',
    items: [
      'Колода: 10 карток, перемішані й відсортовані за <b>найнижчим</b> засвоєнням',
      'Фліп: тап або горизонтальний свайп (rotateY 460мс); ручний показ відповіді = «Не знаю»',
      '«Знаю» +20% · «Не знаю» −20%, потім показ перекладу і кнопка <b>«Далі»</b> (tint)',
      'Рівень: 5 сегментів по 20% + підпис «Рівень засвоєння: N/5»',
      'На звороті — переклад + метадані (що означає / розшифровка / дослівно) і переклад прикладу внизу',
      'Ботом-бар схований на час сесії; вихід — ✕ у хедері → шит «Перервати тренування?» (секція 13)',
      'Заливка на звороті — чорний 13% (фікс для dark)',
    ],
  });

  /* ================= SECTION 6 — Профіль ================= */
  RD.sec({ x: 80, y: 6800, num: '6', text: 'Профіль', sub: 'Два стани акаунта: локальний (гість) з картою входу через Google і авторизований з identity-карткою. Перемикач темної теми живе тут — джерело істини для всієї апки.' });

  function statsRow(words) {
    return '<div style="display:flex;gap:11px">' +
      P.statCard('flame', '7', 'днів поспіль', 'var(--flame-bg)', 'var(--flame-t)') +
      P.statCard('bookmark', words, 'слів збережено', 'var(--tint)', 'var(--purple-t)') +
      P.statCard('cards', '12', 'тренувань', 'var(--train-bg)', 'var(--train-t)') + '</div>';
  }
  function langGroup() {
    return P.sectionLabel('Мови за замовчуванням') +
      '<div class="card" style="border-radius:18px;overflow:hidden">' +
      P.settingRow({ lead: '🇺🇦', label: 'Я розмовляю', sub: 'Українська' }) + P.divider() +
      P.settingRow({ lead: '🇬🇧', label: 'Я вивчаю', sub: 'Англійська' }) + '</div>' +
      '<div style="padding:9px 4px 0;font-size:12.5px;line-height:18px;font-weight:500;color:var(--muted2)">Нові словники створюються з цією парою мов автоматично.</div>';
  }
  function settingsGroup(darkOn) {
    return P.sectionLabel('Налаштування') +
      '<div class="card" style="border-radius:18px;overflow:hidden">' +
      P.settingRow({ lead: ic('bell', 19, 'var(--muted)', 1.8), label: 'Сповіщення', sub: 'Нагадування про тренування', toggle: true }) + P.divider() +
      P.settingRow({ lead: ic('moon', 19, 'var(--muted)', 1.8), label: 'Темна тема', toggle: darkOn }) + '</div>';
  }

  RD.frame({
    x: 80, y: 6960, theme: 'light', label: 'Профіль · авторизований',
    body: RD.statusbar(false) +
      '<div style="padding:14px 22px 120px;display:flex;flex-direction:column;gap:16px">' +
      '<div style="font-size:30px;font-weight:800;letter-spacing:-.6px;color:var(--ink);padding-top:6px">Профіль</div>' +
      '<div class="card" style="border-radius:22px;display:flex;align-items:center;gap:15px;padding:18px">' +
      '<span style="flex:none;width:58px;height:58px;border-radius:99px;background:linear-gradient(135deg,#5B50F0,#410FA3);display:flex;align-items:center;justify-content:center;color:#fff;font-weight:800;font-size:21px">НК</span>' +
      '<span style="flex:1;min-width:0"><span style="display:block;font-weight:800;font-size:18px;color:var(--ink)">Надія Кобилінська</span>' +
      '<span style="display:block;margin-top:2px;font-weight:500;font-size:14px;color:var(--muted)">nadiia@vocabee.app</span></span>' +
      '<span style="width:40px;height:40px;border-radius:13px;background:var(--tint);display:flex;align-items:center;justify-content:center">' + ic('edit', 18, 'var(--purple-t)', 1.9) + '</span></div>' +
      statsRow('26') + '<div>' + langGroup() + '</div><div>' + settingsGroup(false) + '</div>' +
      '<div class="card" style="border-radius:15px;height:50px;display:flex;align-items:center;justify-content:center;font-weight:800;font-size:15.5px;color:var(--red-t)">Вийти</div>' +
      '</div>' + P.bottomBar(2, 'f'),
  });

  RD.frame({
    x: 550, y: 6960, theme: 'dark', label: 'Профіль · гість · dark',
    body: RD.statusbar(false) +
      '<div style="padding:14px 22px 120px;display:flex;flex-direction:column;gap:16px">' +
      '<div style="font-size:30px;font-weight:800;letter-spacing:-.6px;color:var(--ink);padding-top:6px">Профіль</div>' +
      '<div class="card" style="border-radius:22px;padding:18px;display:flex;flex-direction:column;gap:14px">' +
      '<div style="display:flex;align-items:center;gap:15px">' +
      '<span style="flex:none;width:52px;height:52px;border-radius:16px;background:var(--tint);display:flex;align-items:center;justify-content:center">' + RD.logo(30, 'var(--purple-t)') + '</span>' +
      '<span style="flex:1"><span style="display:block;font-weight:800;font-size:18px;color:var(--ink)">Локальний профіль</span>' +
      '<span style="display:block;margin-top:2px;font-weight:500;font-size:13.5px;line-height:18px;color:var(--muted)">Увійди, щоб синхронізувати словники.</span></span></div>' +
      '<div style="height:50px;border-radius:15px;background:var(--neutral);border:1.5px solid var(--line);display:flex;align-items:center;justify-content:center;gap:9px;font-weight:700;font-size:15.5px;color:var(--ink)">' + RD.google(20) + 'Продовжити з Google</div></div>' +
      statsRow('14') + '<div>' + langGroup() + '</div><div>' + settingsGroup(true) + '</div>' +
      '<div style="text-align:center;font-size:12px;font-weight:600;color:var(--muted2);padding-top:4px">Vocabee · v1.0.0</div>' +
      '</div>' + P.bottomBar(2),
  });

  RD.note({
    x: 1020, y: 7000, w: 320, title: 'Профіль · нотатки',
    items: [
      '«Вийти» — тільки для авторизованого; текст red-t на surface-картці',
      'Google-кнопка: neutral surface + line, НЕ біла в dark',
      'Аватар-градієнт purple → deep purple; однаковий в обох темах',
      'Перемикач теми пише в бекенд (<code>darkThemeEnabled</code>) — тема їде за акаунтом між пристроями',
      'Стати: flame/bookmark/cards — фони flame-bg / tint / train-bg, у dark токени темнішають, текст яскравішає',
    ],
  });
})();
