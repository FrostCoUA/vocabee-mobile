/* ============================================================
   Boards 8: бібліотека для реюзу — всі іконки + всі компоненти
   Підпис під кожним демо = точний виклик функції.
   Іконки: RD.* (rd-base.js) · Компоненти: P.* (rd-parts.js)
   ============================================================ */
(function () {
  const ic = RD.ic, A = RD.ACCENTS;

  /* ================= ІКОНКИ · панель поруч із токенами ================= */
  const iconCells = RD.ICON_NAMES.map(function (n) {
    return '<div style="display:flex;flex-direction:column;align-items:center;gap:7px;min-width:0">' +
      '<span style="width:44px;height:44px;border-radius:12px;background:#F6F6F9;border:1px solid #EEF0F5;display:flex;align-items:center;justify-content:center">' + ic(n, 22, '#111827', 1.9) + '</span>' +
      '<span style="font-family:ui-monospace,monospace;font-size:10px;color:#6B7280;white-space:nowrap">' + n + '</span></div>';
  }).join('');
  RD.board({
    x: 2010, y: 260, w: 810, cls: 't-light', label: 'Іконки · повний сет · RD.ic(name, size, color, strokeWidth)',
    body: '<div style="display:grid;grid-template-columns:repeat(9,1fr);gap:14px 6px">' + iconCells + '</div>' +
      '<div style="margin-top:16px;font-size:12px;color:#6B7280;font-weight:500;line-height:1.5">Стиль: stroke 1.9–2.2, round caps/joins, viewBox 24, дефолтний колір var(--ink). Перелік доступний як <code style="background:#F3F4F8;border-radius:4px;padding:1px 5px;font-size:11px">RD.ICON_NAMES</code>; іконки тем словників — <code style="background:#F3F4F8;border-radius:4px;padding:1px 5px;font-size:11px">P.ICON_TOPICS</code>.</div>',
  });

  /* ================= СЕКЦІЯ 14 · компоненти ================= */
  RD.sec({ x: 80, y: 19100, num: '14', text: 'Бібліотека компонентів', sub: 'Кожен елемент системи — одна функція: P.* у redesign/rd-parts.js, брендові примітиви RD.* у redesign/rd-base.js. Підпис під демо — точний виклик. Всі кольори через токени теми, тому будь-який компонент працює в light і dark без змін.' });

  const RB1 = 19260, RB2 = 19980, RB3 = 20650;
  function cap(t) { return '<div style="margin-top:8px;font-family:ui-monospace,monospace;font-size:10.5px;line-height:1.5;color:#9CA3AF">' + t + '</div>'; }
  function demo(inner, capText) { return '<div style="margin-bottom:18px">' + inner + cap(capText) + '</div>'; }
  function box(inner, h) { return '<div style="position:relative;height:' + h + 'px;border-radius:16px;overflow:hidden;background:var(--bg);border:1px solid #EEF0F5">' + inner + '</div>'; }

  /* ---------- B1 · кнопки ---------- */
  RD.board({
    x: 80, y: RB1, w: 470, cls: 't-light', label: 'Кнопки',
    body:
      demo(P.btn('Створити', 'primary'), 'P.btn("Створити","primary")') +
      demo(P.btn('Скасувати', 'neutral'), 'P.btn("Скасувати","neutral")') +
      demo(P.btn('Видалити', 'danger'), 'P.btn("Видалити","danger")') +
      demo(P.btn('Пізніше', 'ghost'), 'P.btn("Пізніше","ghost")') +
      demo(P.btn('Продовжити з Google', 'outline', { icon: RD.google(19) }), 'P.btn("…","outline",{icon:RD.google(19)})') +
      demo('<div style="display:flex;gap:12px">' + P.btn('Пізніше', 'neutral', { grow: true }) + P.btn('Відео за +10', 'primary', { grow: true, icon: ic('play', 19, '#fff', 1.9) }) + '</div>', 'ряд шита: {grow:true}, пропорція 1:1.35'),
  });

  /* ---------- B2 · атоми ---------- */
  RD.board({
    x: 590, y: RB1, w: 470, cls: 't-light', label: 'Атоми · монета, бейджі, прогрес',
    body:
      demo('<div style="display:flex;align-items:center;gap:16px">' + RD.coin(28) + RD.coin(20) + RD.coin(14) + '<span style="width:1px;height:26px;background:#EEF0F5"></span>' + RD.logo(30) + RD.logo(22) + '</div>', 'RD.coin(size) · RD.logo(size, color, accent)') +
      demo(P.beeBadge(47), 'P.beeBadge(47)') +
      demo('<div style="display:flex;align-items:center;gap:14px">' + P.speaker() + P.progressLine(60).replace('height:7px', 'height:7px;flex:1') + '</div>', 'P.speaker() · P.progressLine(pct)') +
      demo(P.knowledgeBars(3, A.indigo), 'P.knowledgeBars(level, accent)') +
      demo(P.sectionLabel('Мови за замовчуванням') + '<div style="height:2px"></div>' + P.sheetLabel('Назва теми') + P.field({ placeholder: 'напр. Подорожі…' }), 'P.sectionLabel(t) · P.sheetLabel(t) · P.field({placeholder|value,focused})'),
  });

  /* ---------- B3 · банери ---------- */
  RD.board({
    x: 1100, y: RB1, w: 520, cls: 't-light', label: 'Банери Головної + критичний',
    body:
      demo(P.banner('wallet', { bees: 47 }), 'P.banner("wallet",{bees:47})') +
      demo(P.banner('guest', { dicts: '2/2', words: '14/50' }), 'P.banner("guest",{dicts,words})') +
      demo(P.banner('walletCritical', { bees: 3 }), 'P.banner("walletCritical",{bees:3})') +
      demo(box(P.criticalFloat(3).replace('position:absolute;', 'position:absolute;top:14px;'), 92), 'P.criticalFloat(3) — плаває під статус-баром'),
  });

  /* ---------- B4 · картки словників ---------- */
  RD.board({
    x: 1660, y: RB1, w: 470, cls: 't-light', label: 'Картки словників + FAB',
    body:
      demo('<div style="display:grid;grid-template-columns:1fr 1fr;gap:13px">' +
        P.dictCard({ title: 'Подорожі', words: 6, accent: A.blue, today: true, know: 35, icon: 'plane' }) +
        P.dictCard({ title: 'Емоції', words: 5, accent: A.violet, updated: 'вчора', know: 45, icon: 'heart' }) + '</div>',
        'P.dictCard({title,words,accent,today|updated,know,icon})') +
      demo(P.dictCardSwiped({ title: 'Робота', words: 5, accent: A.grape, know: 60, icon: 'brief' }), 'P.dictCardSwiped({…}) — свайп-стан') +
      demo(box(P.fab().replace('bottom:94px', 'bottom:20px'), 120), 'P.fab() — тільки на Головній'),
  });

  /* ---------- B5 · рядки слова ---------- */
  RD.board({
    x: 80, y: RB2, w: 470, cls: 't-light', label: 'Слово у словнику',
    body:
      demo(P.wordGroup({ word: 'resilience', ipa: '/rɪˈzɪliəns/', tr: 'стійкість, витривалість', accentT: 'var(--purple-t)' }), 'P.wordGroup({word,ipa,tr,accentT})') +
      demo(P.wordGroup({
        word: 'resilience', ipa: '/rɪˈzɪliəns/', tr: 'стійкість, витривалість', expanded: true, accentT: 'var(--purple-t)',
        details: P.detailsBlock({ accentT: 'var(--purple-t)', senses: [{ pos: 'іменник', def: 'Здатність швидко відновлюватися після труднощів.', ex: 'Her resilience helped her recover.' }], syn: ['grit', 'toughness'], forms: ['resilient'] }),
      }), 'P.wordGroup({…,expanded:true,details:P.detailsBlock({senses,syn,ant,forms,accentT})})') +
      demo(P.chipsRow('Синоніми', ['endurance', 'grit'], 'var(--purple-t)'), 'P.chipsRow(label, values, accent)'),
  });

  /* ---------- B6 · панель перекладу ---------- */
  RD.board({
    x: 590, y: RB2, w: 470, cls: 't-light', label: 'Панель перекладу',
    body:
      demo(P.panelHeader('resilience'), 'P.panelHeader(query)') +
      demo(P.resultRow({ word: 'resilient', ipa: '/rɪˈzɪliənt/', tr: 'стійкий, витривалий', accent: A.indigo, accentT: 'var(--purple-t)' }), 'P.resultRow({word,ipa,tr,accent,accentT})') +
      demo(P.resultRow({ word: 'resilience', ipa: '/rɪˈzɪliəns/', tr: 'стійкість', added: true, accent: A.indigo, accentT: 'var(--purple-t)' }), 'P.resultRow({…,added:true})'),
  });

  /* ---------- B7 · тренування ---------- */
  RD.board({
    x: 1100, y: RB2, w: 520, cls: 't-light', label: 'Тренування',
    body:
      demo(P.setupRow({ title: 'Подорожі', sub: '6 слів · 35% знаю', know: 35, accent: A.blue, selected: true, icon: 'plane' }), 'P.setupRow({title,sub,know,accent,selected,icon})') +
      demo('<div style="display:flex;gap:12px">' + P.answerBtn('Не знаю', 'close', 'var(--orange-t)', 'var(--peach)', true) + P.answerBtn('Знаю', 'check', 'var(--green-t)', 'var(--soft-green)', true) + '</div>', 'P.answerBtn(label, icon, colorT, softBg, grow)'),
  });

  /* ---------- B8 · профіль ---------- */
  RD.board({
    x: 1660, y: RB2, w: 470, cls: 't-light', label: 'Профіль · статистика і налаштування',
    body:
      demo('<div style="display:flex;gap:11px">' +
        P.statCard('flame', '7', 'днів поспіль', 'var(--flame-bg)', 'var(--flame-t)') +
        P.statCard('bookmark', '26', 'слів', 'var(--tint)', 'var(--purple-t)') +
        P.statCard('cards', '12', 'тренувань', 'var(--train-bg)', 'var(--train-t)') + '</div>',
        'P.statCard(icon, value, label, bgToken, colorToken)') +
      demo('<div class="card" style="border-radius:18px;overflow:hidden">' +
        P.settingRow({ lead: ic('bell', 19, 'var(--muted)', 1.8), label: 'Сповіщення', sub: 'Нагадування', toggle: true }) +
        P.divider() +
        P.settingRow({ lead: '🇬🇧', label: 'Я вивчаю', sub: 'Англійська' }) + '</div>',
        'P.settingRow({lead,label,sub,toggle?}) + P.divider() у .card'),
  });

  /* ---------- B9 · хедер словника + док ---------- */
  RD.board({
    x: 80, y: RB3, w: 470, cls: 't-light', label: 'Хедер словника + док вводу',
    body:
      demo('<div style="border-radius:16px;overflow:hidden">' + P.detailHeader({ accent: A.indigo, title: 'Книга · «1984»', sub: '7 слів · сьогодні', progress: 0, flags: ['🇬🇧', '🇺🇦'] }) + '</div>', 'P.detailHeader({accent,title,sub,progress:0..1,flags})') +
      demo(box(P.dock({ state: 'idle', accent: A.indigo, bottom: 20 }), 104), 'P.dock({state:"idle|typing|listening|cancel",value,lines,accent})') +
      demo(box('<div style="position:absolute;inset:0">' + P.dockBackdrop(90) + '</div>', 90), 'P.dockBackdrop(h) — градієнт під доком'),
  });

  /* ---------- B10 · шит-елементи ---------- */
  RD.board({
    x: 590, y: RB3, w: 520, cls: 't-light', label: 'Елементи ботом-шитів',
    body:
      demo(P.langRow('🇬🇧', 'Англійська', true) + '<div style="height:8px"></div>' + P.langRow('🇩🇪', 'Німецька'), 'P.langRow(flag, name, selected)') +
      demo(P.swatches(1), 'P.swatches(selectedIndex) — 12 кольорів, 2 ряди по 6') +
      demo(P.iconPicker(0, A.indigo), 'P.iconPicker(selectedIndex, accent) — P.ICON_TOPICS') +
      demo(box(P.snackbar('Слово видалено', 'Скасувати', 14), 78), 'P.snackbar(text, action?, bottom)'),
  });

  /* ---------- B11 · навігація ---------- */
  RD.board({
    x: 1160, y: RB3, w: 470, cls: 't-light', label: 'Навігація і каркас',
    body:
      demo(box(RD.statusbar(false), 58), 'RD.statusbar(light?)') +
      demo(box(P.homeHeader({ metrics: [5, 26] }), 120), 'P.homeHeader({metrics:[dicts,words]})') +
      demo(box(P.bottomBar(0), 96), 'P.bottomBar(activeIndex)') +
      demo(box(RD.honeycomb(110, 0.5).replace('position:absolute', 'position:absolute;') , 96).replace('background:var(--bg)', 'background:' + A.indigo), 'RD.honeycomb(size, alpha) — декор карток/hero') +
      '<div style="font-size:12.5px;color:#6B7280;font-weight:500;line-height:1.55">Каркас шита цілком: <code style="background:#F3F4F8;border-radius:4px;padding:1px 5px;font-size:11px">P.sheetFrame({title, body})</code> — привид Головної + скрим + поверхня з хендлом і ✕ (приклади — секція 7).</div>',
  });
})();
