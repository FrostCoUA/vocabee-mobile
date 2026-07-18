/* ============================================================
   Boards 1: cover + tokens, Головна, топ-бар spec, Словник
   ============================================================ */
(function () {
  const ic = RD.ic, A = RD.ACCENTS;

  /* ================= SECTION 0 — cover + tokens ================= */
  RD.sec({ x: 80, y: 60, num: '0', text: 'Vocabee · дизайн-система імплементації', sub: 'Оновлено під фактичний стан апки (07.2026): перемальовані всі реалізовані екрани з Compose-коду. Режим «Слово в контексті» прибрано — контекст тепер на картках тренування й у словнику (секція 13). Адмінки та свагери — в окремому документі «Vocabee Admin Web».' });

  RD.board({
    x: 80, y: 260, w: 880, label: 'Що знайдено в коді і що виправлено',
    body:
      '<div style="display:grid;grid-template-columns:1fr 1fr;gap:26px">' +
      '<div><h3 style="margin:0 0 10px;font-size:15px;font-weight:800">Що вже є в імплементації</h3><ul style="margin:0;padding-left:17px;font-size:13.5px;line-height:1.55;color:#374151;font-weight:500">' +
      '<li>Інлайн-док додавання слова (поле + мік) замість пілюлі «+ Додати слово»</li>' +
      '<li>Повноекранна панель варіантів перекладу зі станами loading / error / results</li>' +
      '<li>Колапс хедера словника при скролі (148→64dp)</li>' +
      '<li>Групування слів: 1 картка = слово + всі переклади, розкривні деталі (значення, синоніми, форми)</li>' +
      '<li>Свайп-видалення карток і рядків, жовта підсвітка щойно доданого</li>' +
      '<li>Practice: вибір словників перед раундом, рівень засвоєння 0–100%, контекстне речення на картці + закладки з речень</li>' +
      '<li>Шити: create / language / delete / need-bees / auth / sync-conflict / exit / interrupt / вибір словника для закладок</li>' +
      '<li>Темна тема (значення підібрані «на око»)</li></ul></div>' +
      '<div><h3 style="margin:0 0 10px;font-size:15px;font-weight:800">Доопрацювання в цьому редизайні</h3><ul style="margin:0;padding-left:17px;font-size:13.5px;line-height:1.55;color:#374151;font-weight:500">' +
      '<li><b>Темна тема перебрана в систему токенів</b> — виправлені зламані значення (хендл шита, червоний, текстові акценти)</li>' +
      '<li><b>Багфікс:</b> заливка прогресу на картках у dark ставала білою (<code>Ink.copy(alpha=.10)</code>) — тепер фіксований чорний 13%</li>' +
      '<li>Для тексту в dark — окремі яскравіші акценти: <code>purple-t #9AA4FF</code>, <code>orange-t #FF9A62</code>, <code>green-t #43D17C</code></li>' +
      '<li>Фон Головної уніфіковано з рештою екранів (був білий, став Background)</li>' +
      '<li>Sync-conflict шит: додані картки порівняння станів + підписи наслідків</li>' +
      '<li>Exit-шит: «Залишитися» тепер основна дія</li>' +
      '<li>Practice setup: міні-прогрес засвоєння в рядку теми</li>' +
      '<li>Снекбар специфіковано (замість дефолтного M3)</li></ul></div></div>',
  });

  /* tokens table */
  const TOKENS = [
    { k: 'Background', l: '#F6F6F9', d: '#0F131D' },
    { k: 'Surface (card)', l: '#FFFFFF', d: '#171D2A' },
    { k: 'Sheet surface', l: '#FFFFFF', d: '#1E2433', fix: 'був #202638' },
    { k: 'Sheet handle', l: '#E2E4EC', d: '#3B4358', fix: 'був #CBD5E1 — світліший за поверхню' },
    { k: 'Ink (text)', l: '#111827', d: '#F3F5FA' },
    { k: 'Muted / Muted2', l: '#6B7280', d: '#AEB7C8' },
    { k: 'Line (borders)', l: '#EDEEF3', d: '#2A3143' },
    { k: 'Field bg', l: '#F5F6FA', d: '#1B2130' },
    { k: 'Tint (purple soft)', l: '#E0E7FF', d: '#2C3060' },
    { k: 'Purple — заливки', l: '#4F46E5', d: '#4F46E5' },
    { k: 'Purple — текст/іконки', l: '#4F46E5', d: '#9AA4FF', nw: true },
    { k: 'Orange text', l: '#C2410C', d: '#FF9A62', nw: true },
    { k: 'Green text', l: '#15803D', d: '#43D17C', nw: true },
    { k: 'Red', l: '#DC2626', d: '#E5484D', fix: 'яскравіший у dark' },
    { k: 'Yellow badge', l: '#FFCC00', d: '#FFCC00' },
    { k: 'Note peach', l: '#FFEDE0', d: '#3B2417' },
    { k: 'Scrim', l: 'rgba(17,24,39,.5)', d: 'rgba(0,0,0,.6)' },
  ];
  let tokRows = TOKENS.map(function (t) {
    return '<div class="tok-row"><span class="tok-name">' + t.k + '</span>' +
      '<span class="tok-cell"><span class="tok-chip" style="background:' + t.l + '"></span>' + t.l + '</span>' +
      '<span class="tok-cell"><span class="tok-chip" style="background:' + t.d + '"></span>' + t.d + '</span>' +
      '<span>' + (t.fix ? '<span class="fix">FIX · ' + t.fix + '</span>' : t.nw ? '<span class="fix new">NEW у dark</span>' : '') + '</span></div>';
  }).join('');
  const accStrip = Object.keys(A).map(function (k) {
    return '<span style="flex:1;height:44px;border-radius:12px;background:' + A[k] + ';display:flex;align-items:flex-end;justify-content:center;padding-bottom:4px;color:#fff;font-size:9.5px;font-weight:700">' + k + '</span>';
  }).join('');
  RD.board({
    x: 1010, y: 260, w: 950, label: 'Токени · light / dark',
    body: '<div class="tok-row" style="border-bottom:2px solid #E7E8EE"><span class="tok-name" style="color:#9CA3AF">ТОКЕН</span><span style="font-size:11px;font-weight:800;color:#9CA3AF">LIGHT</span><span style="font-size:11px;font-weight:800;color:#9CA3AF">DARK</span><span></span></div>' +
      tokRows +
      '<div style="margin-top:18px;font-size:12px;font-weight:800;color:#9CA3AF;letter-spacing:.04em">АКЦЕНТИ ОБКЛАДИНОК · однакові в обох темах</div>' +
      '<div style="display:flex;gap:8px;margin-top:8px">' + accStrip + '</div>' +
      '<div style="margin-top:14px;font-size:12.5px;color:#6B7280;font-weight:500;line-height:1.5">Заливка «% засвоєння» на кольорових картках: <b>чорний 13% в обох темах</b> (не Ink). Тіні в dark замінюються на бордер <code style="background:#F3F4F8;border-radius:4px;padding:1px 5px">line</code> + мʼякшу тінь.</div>',
  });

  /* ================= shared bodies ================= */
  function homeBody(o) {
    const cards = o.cards.map(function (c, i) {
      return o.swipeFirst && i === 0 ? P.dictCardSwiped(c) : P.dictCard(c);
    }).join('');
    return RD.statusbar(false) + P.homeHeader({ metrics: o.metrics }) +
      '<div style="padding:2px 22px 0">' + o.banner + '</div>' +
      '<div style="padding:16px 22px 120px;display:grid;grid-template-columns:1fr 1fr;gap:13px">' + cards + '</div>' +
      P.fab() + P.bottomBar(0);
  }
  const CARDS = [
    { title: 'Подорожі', words: 6, accent: A.blue, today: true, know: 35, icon: 'plane' },
    { title: 'Книга · «1984»', words: 7, accent: A.indigo, today: true, know: 20, icon: 'book' },
    { title: 'Робота', words: 5, accent: A.grape, updated: 'вчора', know: 60, icon: 'brief' },
    { title: 'Їжа та кулінарія', words: 3, accent: A.amber, updated: '3 дні тому', know: 10, icon: 'food' },
    { title: 'Емоції', words: 5, accent: A.violet, updated: 'тиждень тому', know: 45, icon: 'heart' },
  ];

  /* ================= SECTION 1 — Головна ================= */
  RD.sec({ x: 80, y: 1120, num: '1', text: 'Головна · Словники', sub: 'Сітка 2×N, банер-слот під заголовком, свайп картки (Видалити/Змінити), FAB. Замість логотипа — клікабельний флажок мови, яку вивчаю: тап → шит зміни мови, словники обраної пари йдуть угору без заголовка, решта пар — нижче групами «ТЕМИ 🇩🇪→🇺🇦».' });

  RD.frame({ x: 80, y: 1280, theme: 'light', label: 'Головна · банер гаманця', body: homeBody({ metrics: [5, 26], banner: P.banner('wallet', { bees: 47 }), cards: CARDS }) });
  RD.frame({ x: 550, y: 1280, theme: 'dark', label: 'Головна · dark', body: homeBody({ metrics: [5, 26], banner: P.banner('wallet', { bees: 47 }), cards: CARDS }) });
  RD.frame({ x: 1020, y: 1280, theme: 'light', label: 'Головна · гостьовий режим', body: homeBody({ metrics: [2, 14], banner: P.banner('guest', { dicts: '2/2', words: '14/50' }), cards: CARDS.slice(0, 2) }) });
  RD.frame({ x: 1490, y: 1280, theme: 'dark', label: 'Головна · свайп + критичний банер', body: homeBody({ metrics: [5, 26], banner: P.banner('walletCritical', { bees: 3 }), cards: CARDS, swipeFirst: true }) });

  /* мульти-мовні групи */
  function cardsGrid(cards) {
    return '<div style="padding:12px 22px 0;display:grid;grid-template-columns:1fr 1fr;gap:13px">' + cards.map(function (c) { return P.dictCard(c); }).join('') + '</div>';
  }
  RD.frame({
    x: 2430, y: 1280, theme: 'light', label: 'Головна · групи пар мов',
    body: RD.statusbar(false) + P.homeHeader({ metrics: [7, 38], flag: '🇬🇧' }) +
      cardsGrid(CARDS.slice(0, 2)) +
      '<div style="padding:14px 22px 0">' + P.pairGroupLabel('🇩🇪', '🇺🇦') + '</div>' +
      cardsGrid([
        { title: 'Німецька база', words: 9, accent: A.teal, updated: 'вчора', know: 30, icon: 'grad' },
        { title: 'Берлін · побут', words: 4, accent: A.plum, updated: '3 дні тому', know: 15, icon: 'bag' },
      ]) +
      '<div style="padding:14px 22px 120px">' + P.pairGroupLabel('🇵🇱', '🇺🇦') + '</div>' +
      P.fab() + P.bottomBar(0) +
      '<div style="position:absolute;left:0;right:0;bottom:96px;text-align:center;font-size:12px;font-weight:600;color:var(--muted2);z-index:26">верхня група 🇬🇧→🇺🇦 — без заголовка, це робоча пара</div>',
  });
  RD.frame({
    x: 2900, y: 1280, h: 844, theme: 'dark', label: 'Шит · зміна мови, яку вивчаю',
    body: P.sheetFrame({
      title: 'Я вивчаю', flags: ['🇬🇧', '🇺🇦'],
      body: '<div style="display:flex;flex-direction:column;gap:8px">' +
        langChoice('🇬🇧', 'Англійська', { selected: true, count: '4 словники' }) +
        langChoice('🇩🇪', 'Німецька', { count: '2 словники' }) +
        langChoice('🇵🇱', 'Польська', { count: '1 словник' }) +
        langChoice('🇪🇸', 'Іспанська', { disabled: true }) +
        langChoice('🇫🇷', 'Французька', { disabled: true }) +
        langChoice('🇮🇹', 'Італійська', { disabled: true }) + '</div>' +
        '<div style="padding-top:12px;font-size:12.5px;line-height:18px;font-weight:500;color:var(--muted2)">Неактивні — підтримувані мови без словників у парі з 🇺🇦. Новий словник іншої мови створюється через профіль або FAB.</div>',
    }),
  });
  function langChoice(flag, name, o) {
    o = o || {};
    const border = o.selected ? 'var(--purple)' : 'var(--line)';
    const bg = o.selected ? 'var(--tint)' : 'var(--surface)';
    return '<div style="display:flex;align-items:center;gap:13px;border-radius:15px;background:' + bg + ';border:1.5px solid ' + border + ';padding:14px 15px;' + (o.disabled ? 'opacity:.42' : '') + '">' +
      '<span style="font-size:22px;line-height:1">' + flag + '</span>' +
      '<span style="flex:1;font-weight:700;font-size:16px;color:' + (o.selected ? 'var(--purple-t)' : 'var(--ink)') + '">' + name + '</span>' +
      (o.count ? '<span style="font-size:12.5px;font-weight:700;color:var(--muted2)">' + o.count + '</span>' : '') +
      (o.selected ? '<span style="width:22px;height:22px;border-radius:99px;background:var(--purple);display:inline-flex;align-items:center;justify-content:center">' + ic('check', 14, '#fff', 2.8) + '</span>' : '') + '</div>';
  }
  RD.frame({
    x: 3370, y: 1280, theme: 'light', label: 'Тренування · вибір з групами пар',
    body: RD.statusbar(false) +
      '<div style="padding:8px 24px 0"><div style="display:flex;align-items:center;justify-content:space-between"><span style="font-size:30px;font-weight:800;letter-spacing:-.6px;color:var(--ink)">Тренування</span>' + P.flagChip('🇬🇧') + '</div>' +
      '<div style="margin-top:8px;font-size:15px;line-height:20px;font-weight:600;color:var(--muted)">Вибери словники для короткого раунду повторення.</div></div>' +
      '<div style="padding:10px 22px 0;display:flex;flex-direction:column;gap:12px">' +
      P.setupRow({ title: 'Подорожі', sub: '6 слів · 35% знаю', know: 35, accent: A.blue, selected: true, icon: 'plane' }) +
      P.setupRow({ title: 'Книга · «1984»', sub: '7 слів · 20% знаю', know: 20, accent: A.indigo, selected: true, icon: 'book' }) +
      P.pairGroupLabel('🇩🇪', '🇺🇦') +
      P.setupRow({ title: 'Німецька база', sub: '9 слів · 30% знаю', know: 30, accent: A.teal, icon: 'grad' }) +
      P.setupRow({ title: 'Берлін · побут', sub: '4 слова · 15% знаю', know: 15, accent: A.plum, icon: 'bag' }) +
      '</div>' +
      '<div style="position:absolute;left:0;right:0;bottom:80px;background:var(--surface);box-shadow:0 -10px 30px -18px rgba(0,0,0,.35);padding:14px 22px 18px;z-index:20">' +
      '<div style="display:flex;align-items:center;gap:8px;padding-bottom:10px;font-size:13px"><b style="color:var(--ink)">Вибрано: 2 теми</b><span style="width:4px;height:4px;border-radius:99px;background:var(--muted3)"></span><span style="font-weight:700;color:var(--muted)">13 слів</span></div>' +
      P.btn('Почати тренування', 'primary') + '</div>' +
      P.bottomBar(1),
  });

  RD.note({
    x: 3840, y: 1300, w: 330, title: 'Групи пар мов',
    items: [
      'Ключ фільтра — пара <b>мова-що-вивчаю → рідна</b> (рідна з профілю, тут не обирається)',
      'Флажок у хедері = поточна мова вивчення; тап → шит «Я вивчаю»',
      'Після вибору група обраної пари <b>анімовано переїжджає вгору</b> (reorder ~350мс, spring); вона без заголовка й без стікі — це робочі словники',
      'Інші пари — нижче, кожна з заголовком «ТЕМИ + флаги пари» (P.pairGroupLabel)',
      'У шиті — всі підтримувані мови; активні лише ті, де є словники в парі з рідною; біля активних — кількість словників',
      'Те саме групування на екрані вибору словників для тренування; раунд можна збирати тільки в межах однієї пари',
    ],
  });

  RD.note({
    x: 1960, y: 1300, w: 320, title: 'Банер-слот · пріоритет',
    items: [
      'Гість → <b>Гостьовий режим</b> (tint/purple); ліміт вичерпано → peach/orange',
      'Акаунт → <b>Гаманець</b>; баланс ≤ 3 → критичний (peach)',
      'Монетка — медовий гекс-коїн; sparkle лишається тільки для AI-контенту',
      'Свайп картки вліво відкриває зону 76px «Видалити» (red), поріг 42% ширини або velocity 320',
      'Картка: 162dp, r24; іконка теми зверху зліва, бейдж «сьогодні» — праворуч',
    ],
  });

  /* ================= SECTION 2 — топ-бар поведінка ================= */
  RD.sec({ x: 80, y: 2340, num: '2', text: 'Топ-бар · поведінка', sub: 'Три патерни: великий заголовок на кореневих екранах, колапс-хедер словника, плаваючий критичний банер поверх не-кореневих екранів. Значення з коду, приведені до системи.' });

  function headerFrag(p, label, height) {
    return '<div style="width:390px"><div class="t-light" style="position:relative;width:390px;border-radius:22px;overflow:hidden;background:var(--bg);box-shadow:0 10px 26px -18px rgba(17,24,39,.4)">' +
      P.detailHeader({ accent: A.indigo, title: 'Книга · «1984»', sub: '7 слів · сьогодні', progress: p, flags: ['🇬🇧', '🇺🇦'] }) +
      '<div style="height:14px"></div></div>' +
      '<div style="margin-top:10px;font-size:12px;font-weight:800;color:#6B7280;text-align:center">' + label + '</div></div>';
  }
  RD.board({
    x: 80, y: 2490, w: 1420, label: 'Колапс хедера словника · keyframes',
    body:
      '<div style="display:flex;gap:34px;align-items:flex-start">' +
      headerFrag(0, 'progress = 0 · розгорнутий · 148dp') +
      headerFrag(0.5, 'progress = 0.5 · перехід') +
      headerFrag(1, 'progress = 1 · компактний · 64dp') +
      '</div>' +
      '<div style="margin-top:26px;display:grid;grid-template-columns:repeat(3,1fr);gap:14px">' +
      [['Висота', '148dp → 64dp (+ статус-бар). progress = scroll / 84dp, clamp 0..1'],
       ['Заголовок', '28sp → 18sp · x: 18 → 70dp (стає поруч із «назад») · y: 62 → 11dp · 2 рядки → 1 рядок після 45%'],
       ['Підзаголовок і решта', 'підзаголовок: alpha = 1 − progress×1.45 (зникає до 70%) · тінь хедера зʼявляється при progress > 0.02 · радіус 28 знизу і кнопки (назад 40dp, напрямок мови) — константні']]
        .map(function (r) { return '<div style="background:#F8F9FC;border:1px solid #EEF0F5;border-radius:14px;padding:14px 16px"><div style="font-size:11.5px;font-weight:800;color:#9CA3AF;letter-spacing:.05em">' + r[0].toUpperCase() + '</div><div style="margin-top:6px;font-size:13px;font-weight:600;color:#374151;line-height:1.5">' + r[1] + '</div></div>'; }).join('') +
      '</div>',
  });

  RD.note({
    x: 1560, y: 2510, w: 340, title: 'Інші патерни топ-бару',
    items: [
      '<b>Кореневі екрани</b> (Словники / Тренування / Профіль): великий заголовок 30–34sp у контенті, скролиться разом зі сторінкою, без sticky-бару',
      '<b>Критичний банер монеток</b>: плаває під статус-баром на всіх екранах, крім Головної (там банер-слот), padding 16/10, тінь elev2',
      '<b>Панель перекладу</b>: без топ-бару — хедер панелі = слово-запит + «Варіанти перекладу», закриття — кнопкою ✕ у доку',
      'Кнопка «назад»: 40dp, r13, біла 18% на акценті / neutral на світлому',
    ],
  });

  /* floating critical demo */
  RD.frame({
    x: 1560, y: 2870, h: 290, r: 26, theme: 'light', label: 'Критичний банер поверх екрана',
    body: RD.statusbar(false) +
      '<div style="margin-top:2px">' + '<div style="position:relative;padding:0 0">' + P.criticalFloat(3).replace('position:absolute;', 'position:absolute;top:0;') + '</div></div>' +
      '<div style="padding:76px 24px 0;opacity:.45"><div style="font-size:30px;font-weight:800;letter-spacing:-.6px">Тренування</div>' +
      '<div style="margin-top:12px;height:84px;border-radius:20px;background:var(--surface);border:1px solid var(--line)"></div></div>',
  });

  /* ================= SECTION 3 — Словник ================= */
  RD.sec({ x: 80, y: 3180, num: '3', text: 'Словник · деталі', sub: 'Колапс-хедер, групи слів (слово + всі переклади в одній картці), розкривні AI-деталі, свайп-видалення рядка, жовта підсвітка щойно доданого слова, інлайн-док знизу.' });

  const RES_DETAILS = P.detailsBlock({
    accentT: 'var(--purple-t)',
    senses: [
      { pos: 'іменник', def: 'Здатність швидко відновлюватися після труднощів.', ex: 'Her resilience helped her recover quickly.' },
      { pos: '', def: 'Властивість матеріалу повертати форму.', ex: '' },
    ],
    syn: ['endurance', 'toughness', 'grit'],
    forms: ['resilient', 'resiliently'],
  });

  function detailRows(o) {
    return '<div style="padding:10px 16px 150px;display:flex;flex-direction:column;gap:10px">' +
      P.wordGroup({ word: 'resilience', ipa: '/rɪˈzɪliəns/', tr: 'стійкість, витривалість', expanded: o.expand, details: RES_DETAILS, accentT: 'var(--purple-t)' }) +
      P.wordGroup({ word: 'reluctant', ipa: '/rɪˈlʌktənt/', tr: 'неохочий', highlighted: o.highlight, accentT: 'var(--purple-t)' }) +
      P.wordGroup({ word: 'gloomy', ipa: '/ˈɡluːmi/', tr: 'похмурий, темний, безрадісний', accentT: 'var(--purple-t)' }) +
      P.wordGroup({ word: 'whisper', ipa: '/ˈwɪspə/', tr: 'шепотіти', canExpand: false, accentT: 'var(--purple-t)' }) +
      P.wordGroup({ word: 'vivid', ipa: '/ˈvɪvɪd/', tr: 'яскравий', canExpand: false, accentT: 'var(--purple-t)' }) +
      '</div>';
  }
  function detailBody(o) {
    return '<div style="display:flex;flex-direction:column;height:100%">' +
      P.detailHeader({ accent: A.indigo, title: 'Книга · «1984»', sub: '7 слів · сьогодні', progress: o.progress || 0, flags: ['🇬🇧', '🇺🇦'] }) +
      '<div style="flex:1;overflow:hidden;position:relative">' + (o.rows || detailRows(o)) + '</div></div>' +
      P.dockBackdrop() + P.dock({ state: 'idle', accent: A.indigo });
  }

  RD.frame({ x: 80, y: 3340, theme: 'light', label: 'Словник · розгорнуто + AI-деталі', body: detailBody({ expand: true }) });
  RD.frame({ x: 550, y: 3340, theme: 'dark', label: 'Словник · dark · підсвітка нового', body: detailBody({ highlight: true }) });
  RD.frame({ x: 1020, y: 3340, theme: 'light', label: 'Словник · заскролено (колапс)', body: detailBody({ progress: 1 }) });
  RD.frame({
    x: 1490, y: 3340, theme: 'dark', label: 'Словник · порожній стан',
    body: '<div style="display:flex;flex-direction:column;height:100%">' +
      P.detailHeader({ accent: A.teal, title: 'Нова тема', sub: '0 слів · сьогодні', progress: 0, flags: ['🇬🇧', '🇺🇦'] }) +
      '<div style="flex:1;display:flex;flex-direction:column;align-items:center;justify-content:center;padding:0 36px;text-align:center">' +
      '<div style="width:170px;height:130px;border-radius:14px;background:var(--neutral);border:1.5px solid var(--line);display:flex;flex-direction:column;gap:9px;justify-content:center;padding:0 28px">' +
      '<span style="height:8px;border-radius:4px;background:var(--line);width:62%"></span><span style="height:8px;border-radius:4px;background:var(--line2);width:84%"></span><span style="height:8px;border-radius:4px;background:var(--line2);width:48%"></span></div>' +
      '<div style="margin-top:18px;font-size:21px;font-weight:800;color:var(--ink)">Тема порожня</div></div></div>' +
      P.dockBackdrop() + P.dock({ state: 'idle', accent: A.teal }),
  });

  RD.note({
    x: 1960, y: 3360, w: 320, title: 'Рядок слова · правила',
    items: [
      'Група = слово + <b>всі</b> переклади через кому; 3 переклади ≠ 3 картки',
      'Шеврон активний (accent) лише якщо є деталі або текст обрізано; інакше muted3',
      'AI-деталі: значення (макс 3) з нумерацією, приклади в лапках, чіпи синонімів (accent 10%), антонімів (orange), форм (muted)',
      'Свайп вліво → зона 88px «Видалити»',
      'Щойно додане слово: бордер <code>yellow</code> 1px, скрол до нього',
      'Спікер: 38dp, tint + purple-t',
    ],
  });
})();
