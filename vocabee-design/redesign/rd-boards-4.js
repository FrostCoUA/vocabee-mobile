/* ============================================================
   Boards 4: перший старт (спрощений вхід), empty-стейти, v2 топбари
   ============================================================ */
(function () {
  const ic = RD.ic, A = RD.ACCENTS;

  /* ================= SECTION 9 — перший старт ================= */
  RD.sec({ x: 80, y: 10950, num: '9', text: 'Перший старт · спрощений вхід', sub: 'Splash → онбординг (3 слайди, скіп) → вхід → мови. Екран входу максимально спрощено: тільки Google і гостьовий режим. Прибрано email+пароль, Facebook і перемикач «увійти / зареєструватися».' });

  /* splash */
  RD.frame({
    x: 80, y: 11110, theme: 'light', label: 'Splash',
    body: '<div style="position:absolute;inset:0;background:radial-gradient(120% 100% at 50% 0%, #5B50F0 0%, #4F46E5 48%, #410FA3 100%)">' +
      RD.statusbar(true) +
      '<div style="position:absolute;inset:0;display:flex;flex-direction:column;align-items:center;justify-content:center">' +
      RD.logo(84, '#fff', '#FFCC00') +
      '<div style="margin-top:22px;font-size:44px;font-weight:800;letter-spacing:-1.5px;color:#fff">voca<span style="color:#FFCC00">bee</span></div>' +
      '<div style="margin-top:12px;font-size:15px;font-weight:600;color:rgba(255,255,255,.78)">Збирай слова. Будуй словники.</div></div></div>',
  });

  /* onboarding */
  RD.frame({
    x: 550, y: 11110, theme: 'light', label: 'Онбординг · слайд 1/3',
    body: '<div style="position:absolute;inset:0;background:radial-gradient(125% 90% at 50% 0%, #5B50F0 0%, #4F46E5 52%, #410FA3 100%);display:flex;flex-direction:column">' +
      RD.statusbar(true) +
      '<div style="display:flex;justify-content:flex-end;padding:4px 22px 0"><span style="font-size:15px;font-weight:700;color:rgba(255,255,255,.8)">Пропустити</span></div>' +
      '<div style="flex:1;display:flex;align-items:center;justify-content:center">' +
      '<svg width="220" height="190" viewBox="0 0 220 190"><rect x="34" y="26" width="152" height="140" rx="16" fill="rgba(255,255,255,.10)"/><rect x="52" y="48" width="116" height="96" rx="12" fill="#fff"/><rect x="66" y="66" width="64" height="8" rx="4" fill="#C7D2FE"/><rect x="66" y="82" width="88" height="8" rx="4" fill="#E0E7FF"/><rect x="64" y="96" width="56" height="14" rx="5" fill="#FFCC00"/><rect x="66" y="120" width="80" height="8" rx="4" fill="#E0E7FF"/><g transform="translate(150,104)"><rect x="-16" y="-16" width="40" height="50" rx="9" fill="#410FA3"/><path d="M-4 6l4 4 8-9" stroke="#FFCC00" stroke-width="3.4" fill="none" stroke-linecap="round" stroke-linejoin="round"/></g></svg></div>' +
      '<div style="padding:0 34px"><div style="font-size:29px;font-weight:800;line-height:1.18;letter-spacing:-.5px;color:#fff">Зберігай слова під час читання</div>' +
      '<div style="margin-top:14px;font-size:16px;line-height:1.55;font-weight:500;color:rgba(255,255,255,.8)">Натрапив на незнайоме слово? Збережи його одним дотиком — переклад підкаже AI.</div></div>' +
      '<div style="display:flex;align-items:center;justify-content:space-between;padding:34px 30px 46px">' +
      '<span style="display:flex;gap:8px"><span style="width:26px;height:8px;border-radius:99px;background:#FFCC00"></span><span style="width:8px;height:8px;border-radius:99px;background:rgba(255,255,255,.32)"></span><span style="width:8px;height:8px;border-radius:99px;background:rgba(255,255,255,.32)"></span></span>' +
      '<span style="display:inline-flex;align-items:center;gap:9px;background:#fff;color:#4F46E5;font-weight:800;font-size:16px;height:54px;padding:0 24px;border-radius:16px;box-shadow:0 12px 24px -10px rgba(0,0,0,.3)">Далі ' + ic('arrowR', 19, '#4F46E5', 2.2) + '</span></div></div>',
  });

  /* simplified auth */
  function authBody() {
    return RD.statusbar(false) +
      '<div style="display:flex;justify-content:flex-end;padding:2px 18px 0"><span style="font-size:15px;font-weight:700;color:var(--muted);padding:8px 10px">Пропустити</span></div>' +
      '<div style="position:absolute;top:120px;left:30px;right:30px;bottom:40px;display:flex;flex-direction:column">' +
      RD.logo(52, 'var(--purple)') +
      '<div style="margin-top:24px;font-size:30px;font-weight:800;letter-spacing:-.6px;color:var(--ink);line-height:1.15">Ласкаво просимо<br>до Vocabee</div>' +
      '<div style="margin-top:12px;font-size:15.5px;line-height:1.55;font-weight:500;color:var(--muted)">Збирай слова з книг і розмов, тренуйся та синхронізуй словники між пристроями.</div>' +
      '<div style="flex:1"></div>' +
      '<div style="height:56px;border-radius:16px;background:var(--surface);border:1.5px solid var(--line);box-shadow:var(--elev1);display:flex;align-items:center;justify-content:center;gap:10px;font-weight:700;font-size:16px;color:var(--ink)">' + RD.google(21) + 'Продовжити з Google</div>' +
      '<div style="margin-top:10px;height:52px;border-radius:16px;display:flex;align-items:center;justify-content:center;font-weight:700;font-size:15px;color:var(--muted)">Продовжити без акаунта</div>' +
      '<div style="margin-top:14px;display:flex;gap:9px;border-radius:14px;background:var(--neutral);padding:13px 14px">' + ic('help', 16, 'var(--muted2)', 1.9) +
      '<span style="font-size:12.5px;line-height:18px;font-weight:600;color:var(--muted)">Гостьовий режим: до 2 словників і 50 слів. Дані зберігаються лише на цьому пристрої.</span></div>' +
      '<div style="margin-top:16px;text-align:center;font-size:11.5px;font-weight:500;color:var(--muted2)">Продовжуючи, ти погоджуєшся з Умовами користування</div>' +
      '</div>';
  }
  RD.frame({ x: 1020, y: 11110, theme: 'light', label: 'Вхід · тільки Google (спрощено)', body: authBody() });
  RD.frame({ x: 1490, y: 11110, theme: 'dark', label: 'Вхід · dark', body: authBody() });

  /* language select */
  function langCard(flag, name, on) {
    return '<div style="position:relative;display:flex;align-items:center;gap:11px;padding:14px;border-radius:16px;background:' + (on ? 'var(--tint)' : 'var(--surface)') + ';border:1.5px solid ' + (on ? 'var(--purple)' : 'var(--line)') + '">' +
      '<span style="font-size:20px">' + flag + '</span><span style="font-weight:700;font-size:15px;color:' + (on ? 'var(--purple-t)' : 'var(--ink)') + '">' + name + '</span>' +
      (on ? '<span style="position:absolute;top:9px;right:9px;width:20px;height:20px;border-radius:99px;background:var(--purple);display:flex;align-items:center;justify-content:center">' + ic('check', 13, '#fff', 2.6) + '</span>' : '') + '</div>';
  }
  RD.frame({
    x: 1960, y: 11110, theme: 'light', label: 'Мови · одразу після входу',
    body: RD.statusbar(false) +
      '<div style="padding:8px 26px 0">' + RD.logo(38, 'var(--purple)') +
      '<div style="margin-top:18px;font-size:28px;font-weight:800;letter-spacing:-.5px;color:var(--ink)">Налаштуймо мови</div>' +
      '<div style="margin-top:8px;font-size:15px;line-height:1.5;font-weight:500;color:var(--muted)">Це стане типовою парою для всіх нових словників. Змінити можна в профілі.</div>' +
      '<div style="margin:24px 0 12px;font-size:13px;font-weight:800;letter-spacing:.05em;color:var(--muted2)">Я РОЗМОВЛЯЮ</div>' +
      '<div style="display:grid;grid-template-columns:1fr 1fr;gap:10px">' + langCard('🇺🇦', 'Українська', true) + langCard('🇵🇱', 'Польська') + '</div>' +
      '<div style="margin:24px 0 12px;font-size:13px;font-weight:800;letter-spacing:.05em;color:var(--muted2)">Я ВИВЧАЮ</div>' +
      '<div style="display:grid;grid-template-columns:1fr 1fr;gap:10px">' + langCard('🇬🇧', 'Англійська', true) + langCard('🇩🇪', 'Німецька') + langCard('🇪🇸', 'Іспанська') + langCard('🇫🇷', 'Французька') + '</div></div>' +
      '<div style="position:absolute;left:0;right:0;bottom:0;padding:16px 26px 30px;background:var(--surface);border-top:1px solid var(--line2)">' + P.btn('Готово', 'primary') + '</div>',
  });

  RD.note({
    x: 2430, y: 11150, w: 300, title: 'Що спрощено',
    items: [
      'Прибрано: email + пароль, Facebook, перемикач «вхід / реєстрація»',
      'Google — єдиний метод; «без акаунта» = гостьовий режим із карткою лімітів одразу',
      '«Пропустити» на онбордингу веде одразу на вхід',
      'Мови зберігаються в профіль після входу; при гостьовому вході — локально',
    ],
  });

  /* ================= SECTION 10 — empty-стейти ================= */
  RD.sec({ x: 80, y: 12250, num: '10', text: 'Empty-стейти на всіх табах', sub: 'Перший запуск: Словники без жодного словника (CTA замість FAB), Тренування без слів. Порожній словник — у секції 3, «порожній» Профіль (гість) — у секції 6.' });

  RD.frame({
    x: 80, y: 12410, theme: 'light', label: 'Словники · порожньо',
    body: RD.statusbar(false) + P.homeHeader({}) +
      '<div style="padding:2px 22px 0">' + P.banner('guest', { dicts: '0/2', words: '0/50' }) + '</div>' +
      '<div style="position:absolute;top:270px;left:36px;right:36px;bottom:120px;display:flex;flex-direction:column;align-items:center;justify-content:center;text-align:center">' +
      '<svg width="190" height="150" viewBox="0 0 190 150"><rect x="39" y="86" width="112" height="40" rx="13" fill="var(--tint)" opacity=".5"/><rect x="31" y="56" width="128" height="42" rx="13" fill="var(--tint)"/><rect x="23" y="22" width="144" height="46" rx="14" fill="var(--surface)" stroke="var(--line)" stroke-width="1.5"/><circle cx="145" cy="45" r="8" fill="#FFCC00"/><rect x="69" y="38" width="60" height="8" rx="4" fill="var(--track)"/><rect x="69" y="51" width="38" height="7" rx="3.5" fill="var(--line2)"/><polygon points="47,34 53.5,37.8 53.5,45.2 47,49 40.5,45.2 40.5,37.8" fill="var(--purple)" opacity=".4"/></svg>' +
      '<div style="margin-top:18px;font-size:21px;font-weight:800;color:var(--ink)">Поки що порожньо</div>' +
      '<div style="margin-top:9px;font-size:15px;line-height:23px;font-weight:500;color:var(--muted);max-width:280px">Створи свій перший тематичний словник — і починай збирати слова.</div>' +
      '<div style="margin-top:22px;width:100%">' + P.btn('Створити словник', 'primary', { icon: ic('plus', 19, '#fff', 2.2) }) + '</div></div>' +
      P.bottomBar(0),
  });

  RD.frame({
    x: 550, y: 12410, theme: 'dark', label: 'Тренування · порожньо',
    body: RD.statusbar(false) +
      '<div style="padding:8px 24px 0"><div style="font-size:30px;font-weight:800;letter-spacing:-.6px;color:var(--ink)">Почати практику</div>' +
      '<div style="margin-top:8px;font-size:15px;line-height:20px;font-weight:600;color:var(--muted)">Обери словники, які хочеш повторити сьогодні.</div></div>' +
      '<div style="position:absolute;top:220px;left:36px;right:36px;bottom:130px;display:flex;flex-direction:column;align-items:center;justify-content:center;text-align:center">' +
      '<svg width="160" height="112" viewBox="0 0 160 112"><rect x="28" y="14" width="104" height="76" rx="14" fill="var(--neutral)" stroke="var(--line)" stroke-width="1.5"/><rect x="46" y="36" width="68" height="9" rx="4.5" fill="var(--track)"/><rect x="58" y="54" width="44" height="9" rx="4.5" fill="var(--line2)"/><circle cx="80" cy="101" r="4" fill="var(--track)"/></svg>' +
      '<div style="margin-top:18px;font-size:21px;font-weight:800;color:var(--ink)">Немає слів для повторення</div>' +
      '<div style="margin-top:9px;font-size:15px;line-height:23px;font-weight:500;color:var(--muted);max-width:280px">Додай слова у словники — і вони з’являться тут для тренування.</div>' +
      '<div style="margin-top:22px;width:100%">' + P.btn('До словників', 'neutral', { icon: ic('book', 19, 'var(--ink)', 2), style: 'border:1.4px solid var(--line);' }) + '</div></div>' +
      P.bottomBar(1),
  });

  RD.note({
    x: 1020, y: 12450, w: 320, title: 'Правила порожніх станів',
    items: [
      'Ілюстрації — площинні, з токенів теми (tint / neutral / track), однакова мова в light і dark',
      'Один CTA: на Словниках — primary «Створити словник» (FAB ховається), на Тренуванні — neutral «До словників» (веде на таб Словники)',
      'Заголовок 21/800 + пояснення ≤ 2 рядки',
      'Порожній словник (секція 3): стрілка вказує на док вводу',
      'Гість у Профілі (секція 6) — це його «empty»: картка входу Google',
    ],
  });

  /* ================= SECTION 11 — v2 з топбарами ================= */
  RD.sec({ x: 80, y: 13500, num: '11', text: 'V2 · альтернатива з топбарами', sub: 'Системний топбар 56dp майже на всіх екранах: root — заголовок 22/800 зліва + дії; pushed — назад + центрований тайтл 17/800. Більше місця контенту і звичніший Android-патерн; натомість менше бренду і кольору теми. Порівняй із v1 вище.' });

  function tb(o) {
    return '<div style="position:relative;z-index:10;background:var(--surface);border-bottom:1px solid var(--line);' + (o.elevated ? 'box-shadow:0 8px 18px -14px rgba(17,24,39,.4);' : '') + '">' + RD.statusbar(false) +
      '<div style="position:relative;height:56px;display:flex;align-items:center;gap:10px;padding:0 12px">' +
      (o.back ? '<span style="flex:none;width:40px;height:40px;border-radius:13px;background:var(--neutral);display:flex;align-items:center;justify-content:center">' + ic('chevL', 20, 'var(--ink)', 2.2) + '</span>' : '<span style="width:8px"></span>') +
      (o.center
        ? '<span style="position:absolute;left:60px;right:60px;text-align:center;font-weight:800;font-size:17px;letter-spacing:-.2px;color:var(--ink);white-space:nowrap;overflow:hidden;text-overflow:ellipsis">' + o.title + '</span><span style="flex:1"></span>'
        : '<span style="flex:1;font-weight:800;font-size:22px;letter-spacing:-.4px;color:var(--ink)">' + o.title + '</span>') +
      (o.actions || '') + '</div></div>';
  }

  /* v2 home */
  RD.frame({
    x: 80, y: 13660, theme: 'light', label: 'V2 · Словники з топбаром',
    body: tb({
      title: 'Словники', elevated: true,
      actions: '<span style="display:inline-flex;align-items:center;gap:4px;background:var(--ny-bg);border:1px solid var(--ny-b);border-radius:99px;padding:6px 11px;font-weight:800;font-size:14px;color:var(--ink)">' + RD.coin(15) + '47</span>' +
        '<span style="width:40px;height:40px;border-radius:13px;background:var(--neutral);display:flex;align-items:center;justify-content:center">' + ic('search', 19, 'var(--ink)', 2) + '</span>',
    }) +
      '<div style="padding:14px 22px 0">' + P.banner('wallet', { bees: 47 }) + '</div>' +
      '<div style="padding:14px 22px 120px;display:grid;grid-template-columns:1fr 1fr;gap:13px">' +
      P.dictCard({ title: 'Подорожі', words: 6, accent: A.blue, today: true, know: 35, icon: 'plane' }) +
      P.dictCard({ title: 'Книга · «1984»', words: 7, accent: A.indigo, today: true, know: 20, icon: 'book' }) +
      P.dictCard({ title: 'Робота', words: 5, accent: A.grape, updated: 'вчора', know: 60, icon: 'brief' }) +
      P.dictCard({ title: 'Емоції', words: 5, accent: A.violet, updated: 'тиждень тому', know: 45, icon: 'heart' }) +
      '</div>' + P.fab() + P.bottomBar(0),
  });

  /* v2 detail */
  RD.frame({
    x: 550, y: 13660, theme: 'dark', label: 'V2 · Словник із плоским топбаром',
    body: tb({
      title: 'Книга · «1984»', back: true, center: true, elevated: true,
      actions: '<span style="display:inline-flex;align-items:center;gap:5px;background:var(--neutral);border-radius:13px;padding:8px 11px;font-size:14px;font-weight:700;color:var(--ink)">🇬🇧' + ic('arrowR', 12, 'var(--muted2)', 2) + '🇺🇦</span>',
    }) +
      '<div style="display:flex;align-items:center;gap:8px;padding:12px 18px 2px"><span style="width:26px;height:26px;border-radius:9px;background:color-mix(in srgb,' + A.indigo + ' 18%, transparent);display:flex;align-items:center;justify-content:center">' + ic('book', 15, 'var(--purple-t)', 2.1) + '</span><span style="font-size:13.5px;font-weight:700;color:var(--muted)">7 слів · сьогодні · 20% засвоєно</span></div>' +
      '<div style="padding:10px 16px 150px;display:flex;flex-direction:column;gap:10px">' +
      P.wordGroup({ word: 'resilience', ipa: '/rɪˈzɪliəns/', tr: 'стійкість, витривалість', accentT: 'var(--purple-t)' }) +
      P.wordGroup({ word: 'reluctant', ipa: '/rɪˈlʌktənt/', tr: 'неохочий', accentT: 'var(--purple-t)' }) +
      P.wordGroup({ word: 'gloomy', ipa: '/ˈɡluːmi/', tr: 'похмурий, темний', accentT: 'var(--purple-t)' }) +
      P.wordGroup({ word: 'whisper', ipa: '/ˈwɪspə/', tr: 'шепотіти', canExpand: false, accentT: 'var(--purple-t)' }) +
      '</div>' + P.dockBackdrop() + P.dock({ state: 'idle', accent: A.indigo }),
  });

  /* v2 practice */
  RD.frame({
    x: 1020, y: 13660, theme: 'light', label: 'V2 · Тренування з топбаром',
    body: tb({ title: 'Тренування', actions: '<span style="width:40px;height:40px;border-radius:13px;background:var(--neutral);display:flex;align-items:center;justify-content:center">' + ic('help', 19, 'var(--muted)', 1.9) + '</span>' }) +
      '<div style="padding:14px 24px 4px"><div style="font-size:13.5px;font-weight:700;color:var(--muted)">3 / 10 · правильно 2</div><div style="margin-top:8px">' + P.progressLine(30) + '</div></div>' +
      '<div style="position:absolute;top:210px;left:26px;right:26px;bottom:196px;border-radius:28px;background:var(--surface);border:2px solid ' + A.blue + ';box-shadow:var(--elev2);overflow:hidden">' +
      '<div style="position:relative;display:flex;flex-direction:column;align-items:center;justify-content:center;height:100%;padding:30px">' +
      '<span style="display:inline-flex;align-items:center;gap:7px;background:color-mix(in srgb,' + A.blue + ' 12%, transparent);border-radius:99px;padding:7px 12px;font-weight:800;font-size:13px;color:' + A.blue + '"><span style="width:8px;height:8px;border-radius:99px;background:' + A.blue + '"></span>Подорожі</span>' +
      '<div style="margin-top:28px;font-size:40px;font-weight:800;letter-spacing:-1.2px;color:var(--ink)">wander</div>' +
      '<div style="margin-top:8px;font-size:16px;font-weight:600;color:var(--muted2)">/ˈwɒndə/</div>' +
      '<div style="margin-top:18px">' + P.knowledgeBars(2, A.blue) + '</div></div></div>' +
      '<div style="position:absolute;left:24px;right:24px;bottom:106px;display:flex;gap:12px">' +
      P.answerBtn('Не знаю', 'close', 'var(--orange-t)', 'var(--peach)', true) +
      P.answerBtn('Знаю', 'check', 'var(--green-t)', 'var(--soft-green)', true) + '</div>' +
      P.bottomBar(1),
  });

  /* v2 profile */
  RD.frame({
    x: 1490, y: 13660, theme: 'dark', label: 'V2 · Профіль з топбаром',
    body: tb({ title: 'Профіль', actions: '<span style="width:40px;height:40px;border-radius:13px;background:var(--neutral);display:flex;align-items:center;justify-content:center">' + ic('edit', 18, 'var(--purple-t)', 1.9) + '</span>' }) +
      '<div style="padding:16px 22px 120px;display:flex;flex-direction:column;gap:16px">' +
      '<div class="card" style="border-radius:22px;display:flex;align-items:center;gap:15px;padding:18px">' +
      '<span style="flex:none;width:58px;height:58px;border-radius:99px;background:linear-gradient(135deg,#5B50F0,#410FA3);display:flex;align-items:center;justify-content:center;color:#fff;font-weight:800;font-size:21px">НК</span>' +
      '<span style="flex:1;min-width:0"><span style="display:block;font-weight:800;font-size:18px;color:var(--ink)">Надія Кобилінська</span>' +
      '<span style="display:block;margin-top:2px;font-weight:500;font-size:14px;color:var(--muted)">nadiia@vocabee.app</span></span></div>' +
      '<div style="display:flex;gap:11px">' +
      P.statCard('flame', '7', 'днів поспіль', 'var(--flame-bg)', 'var(--flame-t)') +
      P.statCard('bookmark', '26', 'слів збережено', 'var(--tint)', 'var(--purple-t)') +
      P.statCard('cards', '12', 'тренувань', 'var(--train-bg)', 'var(--train-t)') + '</div>' +
      '<div>' + P.sectionLabel('Налаштування') +
      '<div class="card" style="border-radius:18px;overflow:hidden">' +
      P.settingRow({ lead: ic('bell', 19, 'var(--muted)', 1.8), label: 'Сповіщення', sub: 'Нагадування про тренування', toggle: true }) + P.divider() +
      P.settingRow({ lead: ic('moon', 19, 'var(--muted)', 1.8), label: 'Темна тема', toggle: true }) + '</div></div>' +
      '</div>' + P.bottomBar(2),
  });

  RD.note({
    x: 1960, y: 13700, w: 330, title: 'V1 vs V2 · коли що',
    items: [
      '<b>V2 плюси:</b> +90dp контенту на словнику, звичний Android-патерн, простіший колапс (тільки тінь), дії завжди на місці',
      '<b>V2 мінуси:</b> зникає кольоровий hero — слабша ідентичність теми словника; топбар «зʼїдає» великий заголовок root-екранів',
      'Компроміс-рекомендація: v2-бар на root-табах (з монеткою і пошуком справа) + v1 hero тільки на словнику',
      'При скролі: бар піднімає тінь (elevated), заголовок root-екранів переїжджає в бар 22→17sp',
    ],
  });
})();
