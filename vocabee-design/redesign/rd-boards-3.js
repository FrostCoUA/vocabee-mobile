/* ============================================================
   Boards 3: ботом-шити + системні елементи
   ============================================================ */
(function () {
  const ic = RD.ic, A = RD.ACCENTS;
  const SH_Y = 8000, SH_Y2 = 9040;

  /* ================= SECTION 7 — ботом-шити ================= */
  RD.sec({ x: 80, y: 7800, num: '7', text: 'Ботом-шити', sub: 'Єдина анатомія для всіх 8 шитів: хендл 42×5, заголовок 22 + кнопка ✕ 36dp, контент з відступом 24, кнопковий ряд знизу. Правило: основна дія праворуч (purple), руйнівна — червона заливка, відмова — neutral.' });

  function btnRow(a, b) {
    return '<div style="display:flex;gap:12px;padding-top:20px">' + a + b + '</div>';
  }

  /* 7.1 create — light */
  RD.frame({
    x: 80, y: SH_Y, h: 940, theme: 'light', label: 'A · Новий словник · з іконками тем',
    body: P.sheetFrame({
      title: 'Новий словник',
      body: P.sheetLabel('Назва теми') + P.field({ value: 'Серіали', focused: true }) +
        '<div style="height:18px"></div>' + P.sheetLabel('Іконка теми') + P.iconPicker(2, A.blue) +
        '<div style="height:18px"></div>' + P.sheetLabel('Колір теми') + P.swatches(1) +
        '<div style="height:9px"></div>' + P.langInfoStrip('Мова: 🇬🇧 Англійська → 🇺🇦 Українська') +
        '<div style="height:20px"></div>' + P.btn('Створити', 'primary'),
    }),
  });

  /* 7.2 create — dark */
  RD.frame({
    x: 550, y: SH_Y, h: 940, theme: 'dark', label: 'A · Новий словник · dark + ліміт',
    body: P.sheetFrame({
      title: 'Новий словник',
      body: P.sheetLabel('Назва теми') + P.field({ placeholder: 'напр. Подорожі, Робота, Книга…' }) +
        '<div style="height:18px"></div>' + P.sheetLabel('Іконка теми') + P.iconPicker(0, A.teal) +
        '<div style="height:18px"></div>' + P.sheetLabel('Колір теми') + P.swatches(6) +
        '<div style="height:9px"></div>' + P.langInfoStrip('Мова: 🇬🇧 Англійська → 🇺🇦 Українська') +
        '<div style="height:14px"></div>' +
        '<div style="display:flex;gap:9px;border-radius:14px;background:var(--ny-bg);border:1px solid var(--ny-b);padding:13px 14px">' + ic('star', 16, 'var(--flame-t)', 1.9) +
        '<span style="font-size:13px;line-height:19px;font-weight:600;color:var(--ny-t)">Ти створив(ла) максимум 5 словників. Переглянь відео, щоб відкрити більше.</span></div>' +
        '<div style="height:20px"></div>' + P.btn('Створити', 'primary'),
    }),
  });

  /* 7.3 language — light */
  RD.frame({
    x: 1020, y: SH_Y, h: 940, theme: 'light', label: 'B · Вибір мови',
    body: P.sheetFrame({
      title: 'Я вивчаю',
      body: '<div style="display:flex;flex-direction:column;gap:8px">' +
        P.langRow('🇬🇧', 'Англійська', true) + P.langRow('🇩🇪', 'Німецька') + P.langRow('🇪🇸', 'Іспанська') +
        P.langRow('🇫🇷', 'Французька') + P.langRow('🇵🇱', 'Польська') + '</div>',
    }),
  });

  /* 7.4 delete — dark */
  RD.frame({
    x: 1490, y: SH_Y, h: 940, theme: 'dark', label: 'C · Видалення словника',
    body: P.sheetFrame({
      title: 'Видалити словник?',
      body: '<div style="font-size:15px;line-height:21px;font-weight:500;color:var(--muted)">Словник «Книга · 1984» і 7 слів у ньому буде видалено.</div>' +
        '<div style="margin-top:8px;font-size:14px;font-weight:700;color:var(--red-t)">Цю дію не можна скасувати.</div>' +
        btnRow(P.btn('Скасувати', 'neutral', { grow: true }), P.btn('Видалити', 'danger', { grow: true })),
    }),
  });

  /* 7.5 bee gate — light */
  RD.frame({
    x: 80, y: SH_Y2, h: 780, theme: 'light', label: 'D · Потрібні монетки',
    body: P.sheetFrame({
      title: 'Потрібні монетки',
      body: P.banner('walletCritical', { bees: 3 }) +
        '<div style="margin-top:16px;font-size:15px;line-height:21px;font-weight:500;color:var(--muted)">Перші 2 словники безкоштовні. Новий словник коштує 10 монеток.</div>' +
        btnRow(P.btn('Пізніше', 'neutral', { grow: true }), P.btn('Відео за +10', 'primary', { grow: true, icon: ic('play', 19, '#fff', 1.9) })),
    }),
  });

  /* 7.6 auth gate — dark */
  RD.frame({
    x: 550, y: SH_Y2, h: 780, theme: 'dark', label: 'E · Потрібен акаунт',
    body: P.sheetFrame({
      title: 'Потрібен акаунт',
      body: P.banner('guestCritical', { dicts: '2/2', words: '50/50' }) +
        '<div style="margin-top:16px;font-size:15px;line-height:21px;font-weight:500;color:var(--muted)">Без акаунта можна зібрати 50 слів. Щоб продовжити додавати слова і отримувати переклади за монетки, увійди через Google.</div>' +
        btnRow(P.btn('Пізніше', 'neutral', { grow: true }), P.btn('Увійти Google', 'primary', { grow: true, icon: '<span style="background:#fff;border-radius:99px;padding:2px;display:flex">' + RD.google(16) + '</span>' })),
    }),
  });

  /* 7.7 sync conflict — light · REDESIGNED */
  function syncCard(title, icon, statLine) {
    return '<div style="flex:1;background:var(--bg);border-radius:16px;padding:14px"><div style="display:flex;align-items:center;gap:7px;font-size:12.5px;font-weight:800;color:var(--muted)">' + icon + title + '</div>' +
      '<div style="margin-top:8px;font-size:14px;font-weight:600;color:var(--ink)">' + statLine + '</div></div>';
  }
  function syncAction(label, sub, kind) {
    const skin = kind === 'primary' ? 'background:var(--purple);color:#fff' : kind === 'neutral' ? 'background:var(--neutral);color:var(--ink)' : 'background:transparent;color:var(--muted);border:1.5px solid var(--line)';
    return '<div style="border-radius:16px;padding:12px 18px;display:flex;flex-direction:column;align-items:center;' + skin + '">' +
      '<span style="font-weight:800;font-size:15.5px">' + label + '</span>' +
      '<span style="margin-top:2px;font-weight:500;font-size:12px;opacity:.75">' + sub + '</span></div>';
  }
  RD.frame({
    x: 1020, y: SH_Y2, h: 780, theme: 'light', label: 'F · Конфлікт синхронізації (перероблено)',
    body: P.sheetFrame({
      title: 'Є два стани акаунта',
      body: '<div style="font-size:14.5px;line-height:21px;font-weight:500;color:var(--muted)">На телефоні є локальні слова, а на цьому Google акаунті вже є дані. Обери, який стан лишити.</div>' +
        '<div style="display:flex;align-items:center;gap:10px;margin-top:14px">' +
        syncCard('На телефоні', ic('book', 15, 'var(--purple-t)', 2), '<b>3</b> словники · <b>24</b> слова') +
        '<span style="flex:none">' + ic('arrowR', 18, 'var(--muted3)', 2) + '</span>' +
        syncCard('На бекенді', ic('globe', 15, 'var(--purple-t)', 2), '<b>1</b> словник · <b>18</b> слів') + '</div>' +
        '<div style="display:flex;flex-direction:column;gap:10px;margin-top:18px">' +
        syncAction('Залити локальний стан', 'серверні дані буде перезаписано', 'primary') +
        syncAction('Взяти стан з бекенда', 'локальні 24 слова буде видалено', 'neutral') +
        syncAction('Увійти іншим email', 'нічого не зміниться', 'ghost') + '</div>',
    }),
  });

  /* 7.8 exit — dark · REDESIGNED */
  RD.frame({
    x: 1490, y: SH_Y2, h: 780, theme: 'dark', label: 'G · Вихід з апки (перероблено)',
    body: P.sheetFrame({
      title: 'Закрити Vocabee?',
      body: '<div style="font-size:15px;line-height:21px;font-weight:500;color:var(--muted)">Прогрес збережено локально — нічого не загубиться.</div>' +
        btnRow(P.btn('Закрити', 'neutral', { grow: true }), P.btn('Залишитися', 'primary', { grow: true })),
    }),
  });

  RD.note({
    x: 1960, y: SH_Y + 40, w: 320, title: 'Анатомія шита',
    items: [
      'Скрим: <code>rgba(17,24,39,.5)</code> light · <code>rgba(0,0,0,.6)</code> dark',
      'Поверхня: sheet-токен (НЕ surface), r28 зверху, тінь вгору',
      'Хендл 42×5, handle-токен — <b>фікс:</b> у dark був світліший за поверхню',
      'Заголовок 22/800 + ✕ 36dp r11 на sheetctl',
      'Контент: паддінг 24, низ 34 + safe-area',
      'Кнопки 56dp r16; пропорція Пізніше/CTA = 1 : 1.35',
    ],
  });
  RD.note({
    x: 1960, y: SH_Y2 + 40, w: 320, title: 'Що перероблено',
    items: [
      '<b>Sync-conflict:</b> замість 3 однакових кнопок — картки порівняння станів + підпис наслідку під кожною дією',
      '<b>Exit:</b> «Залишитися» стала основною (purple), «Закрити» — neutral; було навпаки',
      '<b>Create:</b> додано вибір іконки теми — 15 стандартних (подорожі, книга, кіно, робота…); обрана заливається кольором теми й з’являється на картці словника',
      'Delete: червона заливка тільки для незворотних дій',
      'Гейти (D, E) переиспользуют банери з Головної — один компонент',
    ],
  });

  /* ================= SECTION 8 — системні елементи ================= */
  const SYS_Y = 10250;
  RD.sec({ x: 80, y: SYS_Y - 140, num: '8', text: 'Ботом-бар, снекбар, FAB', sub: 'Нижня навігація з пілюлею на активному табі (патерн з імплементації — залишаємо), специфікований снекбар замість дефолтного M3, плаваюча кнопка створення.' });

  function barDemo(active, theme) {
    return '<div class="t-' + theme + '" style="position:relative;width:390px;height:96px;border-radius:20px;overflow:hidden;background:var(--bg);border:1px solid ' + (theme === 'dark' ? '#262D3E' : '#E7E8EE') + '">' + P.bottomBar(active) + '</div>';
  }
  RD.board({
    x: 80, y: SYS_Y, w: 900, label: 'Ботом-бар · стани',
    body: '<div style="display:flex;flex-direction:column;gap:18px">' +
      '<div style="display:flex;gap:24px">' + barDemo(0, 'light') + barDemo(1, 'dark') + '</div>' +
      '<ul style="margin:0;padding-left:17px;font-size:13px;line-height:1.55;color:#374151;font-weight:500">' +
      '<li>Висота 66dp + safe-area; surface + верхня лінія line</li>' +
      '<li>Активний таб: пілюля 86×36 r24 <b>purple</b> + біла іконка (stroke 2.35) + підпис purple-t; неактивний — muted2</li>' +
      '<li>Ховається на: деталях словника, панелі перекладу, під шитами не ховається (шит поверх)</li></ul></div>',
  });

  RD.board({
    x: 1020, y: SYS_Y, w: 880, label: 'Снекбар + FAB',
    body: '<div style="display:flex;gap:24px;align-items:flex-start">' +
      '<div style="flex:1;display:flex;flex-direction:column;gap:14px">' +
      '<div class="t-light" style="position:relative;height:76px;border-radius:18px;background:var(--bg);border:1px solid #E7E8EE;overflow:hidden">' + P.snackbar('+10 монеток на рахунку', null, 14) + '</div>' +
      '<div class="t-dark" style="position:relative;height:76px;border-radius:18px;background:var(--bg);border:1px solid #262D3E;overflow:hidden">' + P.snackbar('Слово видалено', 'Скасувати', 14) + '</div>' +
      '<ul style="margin:0;padding-left:17px;font-size:13px;line-height:1.55;color:#374151;font-weight:500">' +
      '<li>Поверхня <code>snack</code> (#1F2430 / #2A3143), r16, текст 14.5/600</li>' +
      '<li>Дія — жовтим (yellow), 6с автозакриття</li>' +
      '<li>Позиція: над доком/ботом-баром + 16dp</li></ul></div>' +
      '<div style="width:200px;display:flex;flex-direction:column;align-items:center;gap:12px">' +
      '<div class="t-light" style="position:relative;width:130px;height:130px;border-radius:20px;background:var(--bg);border:1px solid #E7E8EE">' +
      '<div style="position:absolute;right:24px;bottom:24px;width:62px;height:62px;border-radius:22px;background:var(--purple);display:flex;align-items:center;justify-content:center;box-shadow:0 16px 30px -10px rgba(79,70,229,.65)">' + ic('plus', 26, '#fff', 2.4) + '</div></div>' +
      '<div style="font-size:12.5px;font-weight:600;color:#6B7280;text-align:center;line-height:1.5">FAB 62dp r22, тільки на Головній, відступ 20/14 над ботом-баром</div></div></div>',
  });
})();
