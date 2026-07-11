/* ============================================================
   Boards 6: тренування «Слово в контексті»
   ============================================================ */
(function () {
  const ic = RD.ic, A = RD.ACCENTS;
  const Y0 = 15800, RA = 15960, RB = 16960, RC = 17960;

  RD.sec({ x: 80, y: Y0, num: '13', text: 'Тренування · «Слово в контексті»', sub: 'Для слів із 2+ перекладами вчимо пару «слово + переклад у контексті» (run → бігає / керує / балотується / тече). Кожна пара має власний прогрес 0–100%, оцінка бінарна, сесія 10 карток. Формат подає система за рівнем пари; ботом-бару нема — тренування фулскрін.' });

  /* ---------- shared ---------- */
  function sessHead(o) {
    return RD.statusbar(false) +
      '<div style="padding:8px 24px 0">' +
      '<div style="display:flex;align-items:center;gap:10px">' +
      '<span style="flex:1;min-width:0"><span style="display:block;font-size:22px;font-weight:800;letter-spacing:-.4px;color:var(--ink)">Слово в контексті</span>' +
      '<span style="display:block;margin-top:3px;font-size:13.5px;font-weight:700;color:var(--muted)">' + o.step + ' / 10 · правильно ' + o.correct + '</span></span>' +
      '<span style="flex:none;width:40px;height:40px;border-radius:13px;background:var(--neutral);display:flex;align-items:center;justify-content:center">' + ic('close', 19, 'var(--muted)', 2.2) + '</span></div>' +
      '<div style="margin-top:12px">' + P.progressLine(o.step * 10) + '</div></div>';
  }
  function dirBadge(dir) {
    const en = dir !== 'uk';
    return '<span style="display:inline-flex;align-items:center;gap:6px;border-radius:99px;padding:6px 12px;font-weight:800;font-size:12px;letter-spacing:.02em;' +
      (en ? 'background:var(--tint);color:var(--purple-t)' : 'background:color-mix(in srgb, var(--yellow) 25%, transparent);color:var(--ny-t)') + '">' +
      (en ? 'EN → UK · розуміння' : 'UK → EN · вживання') + '</span>';
  }
  const hl = (w) => '<span style="background:var(--yellow);color:#5A4500;font-weight:800;border-radius:7px;padding:1px 7px;white-space:nowrap">' + w + '</span>';
  function ansChip(t, state) {
    let s = 'background:var(--neutral);color:var(--ink);border:1.6px solid transparent';
    if (state === 'correct') s = 'background:var(--soft-green);color:var(--green-t);border:1.6px solid var(--green)';
    if (state === 'wrong') s = 'background:var(--peach);color:var(--orange-t);border:1.6px solid var(--orange)';
    if (state === 'dim') s = 'background:var(--neutral);color:var(--muted3);border:1.6px solid transparent';
    return '<span style="height:48px;border-radius:99px;display:flex;align-items:center;justify-content:center;gap:7px;font-weight:800;font-size:15px;' + s + '">' +
      (state === 'correct' ? ic('check', 16, 'var(--green-t)', 2.6) : '') + (state === 'wrong' ? ic('close', 15, 'var(--orange-t)', 2.6) : '') + t + '</span>';
  }
  function bigCard(inner) {
    return '<div style="padding:16px 24px 0"><div class="card" style="border-radius:28px;padding:22px;box-shadow:var(--elev2)">' + inner + '</div></div>';
  }
  const formatTag = (t) => '<span style="font-size:12px;font-weight:700;color:var(--muted2)">' + t + '</span>';

  /* ================= СЕТАП ================= */
  function typeCard(o) {
    return '<div class="card" style="position:relative;flex:1;border-radius:20px;padding:15px 14px;' + (o.on ? 'border:1.6px solid var(--purple);' : 'border:1px solid var(--line);') + '">' +
      (o.on ? '<span style="position:absolute;top:11px;right:11px;width:22px;height:22px;border-radius:99px;background:var(--purple);display:flex;align-items:center;justify-content:center">' + ic('check', 14, '#fff', 2.6) + '</span>' : '') +
      '<span style="width:44px;height:44px;border-radius:14px;background:' + (o.on ? 'var(--tint)' : 'var(--neutral)') + ';display:flex;align-items:center;justify-content:center">' + ic(o.icon, 22, o.on ? 'var(--purple-t)' : 'var(--muted)', 2) + '</span>' +
      '<div style="margin-top:12px;font-weight:800;font-size:15.5px;color:var(--ink)">' + o.title + '</div>' +
      '<div style="margin-top:4px;font-size:12px;line-height:16px;font-weight:600;color:var(--muted)">' + o.sub + '</div></div>';
  }
  function setupBody() {
    return RD.statusbar(false) +
      '<div style="padding:8px 24px 0"><div style="font-size:30px;font-weight:800;letter-spacing:-.6px;color:var(--ink)">Почати практику</div></div>' +
      '<div style="padding:14px 22px 0"><div style="display:flex;gap:11px">' +
      typeCard({ title: 'Класика', sub: 'флеш-картки, всі слова', icon: 'cards' }) +
      typeCard({ title: 'Слово в контексті', sub: 'для слів із 2+ перекладами', icon: 'chat', on: true }) +
      '</div></div>' +
      '<div style="padding:18px 24px 8px;display:flex;align-items:center"><span style="font-weight:800;font-size:12.5px;letter-spacing:.63px;color:var(--muted2)">СЛОВНИКИ</span><span style="flex:1"></span><span style="font-weight:800;font-size:13px;color:var(--purple-t)">Вибрати всі</span></div>' +
      '<div style="padding:0 22px;display:flex;flex-direction:column;gap:12px">' +
      P.setupRow({ title: 'Подорожі', sub: '12 слів · 8 із кількома перекладами', know: 35, accent: A.blue, selected: true, icon: 'plane' }) +
      P.setupRow({ title: 'Книга · «1984»', sub: '7 слів · 5 із кількома перекладами', know: 20, accent: A.indigo, selected: true, icon: 'book' }) +
      P.setupRow({ title: 'Робота', sub: '5 слів · 1 із кількома перекладами', know: 60, accent: A.grape, icon: 'brief' }) +
      '</div>' +
      '<div style="position:absolute;left:0;right:0;bottom:80px;background:var(--surface);box-shadow:0 -10px 30px -18px rgba(0,0,0,.35);padding:14px 22px 18px;z-index:20">' +
      '<div style="display:flex;align-items:center;gap:8px;padding-bottom:10px;font-size:13px"><b style="color:var(--ink)">Вибрано: 2 теми</b><span style="width:4px;height:4px;border-radius:99px;background:var(--muted3)"></span><span style="font-weight:700;color:var(--muted)">13 пар</span></div>' +
      P.btn('Почати тренування', 'primary') + '</div>' +
      P.bottomBar(1);
  }
  RD.frame({ x: 80, y: RA, theme: 'light', label: 'Сетап · вид тренування + словники', body: setupBody() });
  RD.frame({ x: 550, y: RA, theme: 'dark', label: 'Сетап · dark', body: setupBody() });

  /* ================= ФОРМАТ 1 · ВПІЗНАВАННЯ ================= */
  const SENT_RUN = 'He ' + hl('runs') + ' a small bakery downtown.';
  function recogBody(o) {
    let chips;
    if (o.state === 'ask') chips = [ansChip('бігає'), ansChip('керує'), ansChip('балотується'), ansChip('тече')];
    else if (o.state === 'wrong') chips = [ansChip('бігає', 'wrong'), ansChip('керує', 'correct'), ansChip('балотується', 'dim'), ansChip('тече', 'dim')];
    else chips = [ansChip('бігає', 'dim'), ansChip('керує', 'correct'), ansChip('балотується', 'dim'), ansChip('тече', 'dim')];
    return sessHead({ step: o.step || 3, correct: o.correct || 2 }) +
      bigCard(
        '<div style="display:flex;justify-content:space-between;align-items:center">' + dirBadge('en') + formatTag('впізнавання') + '</div>' +
        '<div style="margin-top:18px;font-size:22px;line-height:1.55;font-weight:700;color:var(--ink)">' + SENT_RUN + '</div>' +
        '<div style="margin-top:10px;font-size:14px;font-weight:600;color:var(--muted)">Що означає тут?</div>' +
        '<div style="margin-top:16px;display:grid;grid-template-columns:1fr 1fr;gap:10px">' + chips.join('') + '</div>' +
        (o.state === 'wrong'
          ? '<div style="margin-top:14px;border-radius:14px;background:var(--peach);border:1px solid var(--peach-b);padding:13px 14px">' +
            '<div style="font-weight:800;font-size:12.5px;color:var(--orange-t)">ПІДКАЗКА</div>' +
            '<div style="margin-top:5px;font-size:13.5px;line-height:19px;font-weight:600;color:var(--ink)">«бігає» — це коли: <i>He runs every morning.</i></div></div>'
          : '') +
        (o.state === 'right'
          ? '<div style="margin-top:16px;display:flex;justify-content:center"><span style="display:inline-flex;align-items:center;gap:6px;background:var(--soft-green);border-radius:99px;padding:7px 13px;font-weight:800;font-size:13px;color:var(--green-t)">' + ic('check', 15, 'var(--green-t)', 2.6) + 'пара run = керує · 40% → 60%</span></div>'
          : '')
      ) +
      (o.state === 'wrong'
        ? '<div style="position:absolute;left:24px;right:24px;bottom:28px"><div style="height:58px;border-radius:19px;background:var(--tint);display:flex;align-items:center;justify-content:center;gap:9px;font-weight:800;font-size:16.5px;color:var(--purple-t)">Далі ' + ic('chevR', 19, 'var(--purple-t)', 2.4) + '</div></div>'
        : '') +
      (o.state === 'right' ? '<div style="position:absolute;left:0;right:0;bottom:34px;text-align:center;font-size:12.5px;font-weight:600;color:var(--muted2)">наступна картка через 0.6с…</div>' : '');
  }
  RD.frame({ x: 1020, y: RA, theme: 'light', label: 'Формат 1 · впізнавання · питання', body: recogBody({ state: 'ask' }) });
  RD.frame({ x: 1490, y: RA, theme: 'dark', label: 'Формат 1 · помилка + міні-підказка', body: recogBody({ state: 'wrong' }) });
  RD.frame({ x: 1960, y: RA, theme: 'light', label: 'Формат 1 · правильно (+прогрес пари)', body: recogBody({ state: 'right', step: 4, correct: 3 }) });

  RD.note({
    x: 2430, y: RA + 40, w: 330, title: 'Механіка пар',
    items: [
      'Одиниця навчання — <b>пара</b> «слово + конкретний переклад»; у run їх 4, кожна зі своїм прогресом 0–100%',
      'Оцінка бінарна: правильно / ні (±20% пари)',
      '<b>Дистрактори — інші переклади цього ж слова</b>, не випадкові слова',
      'Формат подає система: 0–40% впізнавання · 40–70% згадування · всі значення ≥50% — метчинг',
      'Помилка → міні-підказка з контекстом сплутаної пари + «Далі»; правильно → авто-перехід',
      'Підсвітка слова: yellow, однакова в обох темах',
    ],
  });

  /* ================= ФОРМАТ 2 · ЗГАДУВАННЯ ================= */
  function recallBody(o) {
    const front = !o.back;
    return sessHead({ step: 6, correct: 5 }) +
      bigCard(
        '<div style="display:flex;justify-content:space-between;align-items:center">' + dirBadge('en') + formatTag('згадування') + '</div>' +
        (front
          ? '<div style="margin-top:18px;font-size:22px;line-height:1.55;font-weight:700;color:var(--ink)">The river ' + hl('runs') + ' through the valley.</div>' +
            '<div style="margin-top:10px;font-size:14px;font-weight:600;color:var(--muted)">Як перекладається тут?</div>' +
            '<div style="margin-top:34px;text-align:center;font-size:13.5px;font-weight:600;color:var(--muted2)">Торкнись, щоб перевернути</div>'
          : '<div style="margin-top:14px;font-size:14.5px;line-height:1.5;font-weight:600;color:var(--muted)">The river ' + hl('runs') + ' through the valley.</div>' +
            '<div style="margin-top:18px;text-align:center;font-size:34px;font-weight:800;letter-spacing:-.8px;color:var(--ink)">тече</div>' +
            '<div style="margin-top:16px;display:flex;justify-content:center;gap:7px;flex-wrap:wrap">' +
            ['бігає', 'керує', 'балотується'].map(function (t) { return '<span style="padding:7px 12px;border-radius:99px;background:var(--neutral);font-weight:700;font-size:13px;color:var(--muted3)">' + t + '</span>'; }).join('') +
            '</div><div style="margin-top:10px;text-align:center;font-size:12px;font-weight:600;color:var(--muted2)">інші значення run — тренуються окремо</div>')
      ) +
      '<div style="position:absolute;left:24px;right:24px;bottom:28px;display:flex;gap:12px">' +
      P.answerBtn('Не знаю', 'close', 'var(--orange-t)', 'var(--peach)', true) +
      P.answerBtn('Знаю', 'check', 'var(--green-t)', 'var(--soft-green)', true) + '</div>';
  }
  RD.frame({ x: 80, y: RB, theme: 'dark', label: 'Формат 2 · згадування · лице', body: recallBody({}) });
  RD.frame({ x: 550, y: RB, theme: 'light', label: 'Формат 2 · фліп · зворот', body: recallBody({ back: true }) });

  /* ================= ФОРМАТ 3 · МЕТЧИНГ ================= */
  function matchBody(o) {
    const pc = [A.indigo, A.teal, A.violet, A.amber];
    const S = [
      { t: 'She runs in the park every morning', m: 0 },
      { t: 'He runs a small bakery downtown', m: o.done ? 1 : 'sel' },
      { t: 'She will run for mayor next year', m: o.done ? 2 : null },
      { t: 'The river runs through the valley', m: o.done ? 3 : null },
    ];
    const T = [
      { t: 'тече', m: o.done ? 3 : null }, { t: 'бігає', m: 0 },
      { t: 'балотується', m: o.done ? 2 : null }, { t: 'керує', m: o.done ? 1 : null },
    ];
    function sRow(s, i) {
      let b = '1px solid var(--line)', bg = 'var(--surface)';
      if (s.m === 'sel') { b = '1.6px solid var(--purple)'; bg = 'var(--tint)'; }
      else if (s.m !== null) b = '1.6px solid ' + pc[s.m];
      return '<div style="border-radius:16px;background:' + bg + ';border:' + b + ';padding:11px 12px;display:flex;gap:9px;align-items:flex-start">' +
        '<span style="flex:none;width:20px;height:20px;border-radius:99px;margin-top:1px;background:' + (s.m !== null && s.m !== 'sel' ? pc[s.m] : 'var(--track)') + ';display:flex;align-items:center;justify-content:center;font-size:11px;font-weight:800;color:#fff">' + (i + 1) + '</span>' +
        '<span style="font-size:12.5px;line-height:17px;font-weight:600;color:var(--ink)">' + s.t + '</span></div>';
    }
    function tChip(c) {
      const on = c.m !== null;
      return '<div style="height:44px;border-radius:99px;display:flex;align-items:center;justify-content:center;gap:6px;font-weight:800;font-size:14px;' +
        (on ? 'background:color-mix(in srgb,' + pc[c.m] + ' 14%, transparent);color:' + pc[c.m] + ';border:1.6px solid ' + pc[c.m] : 'background:var(--neutral);color:var(--ink);border:1.6px solid transparent') + '">' +
        (on ? '<span style="width:16px;height:16px;border-radius:99px;background:' + pc[c.m] + ';display:flex;align-items:center;justify-content:center;font-size:9.5px;font-weight:800;color:#fff">' + (c.m + 1) + '</span>' : '') + c.t + '</div>';
    }
    return sessHead({ step: 9, correct: 8 }) +
      '<div style="padding:16px 24px 0"><div class="card" style="border-radius:28px;padding:20px;box-shadow:var(--elev2)">' +
      '<div style="display:flex;justify-content:space-between;align-items:center">' + dirBadge('en') + formatTag('метчинг · фінал слова') + '</div>' +
      '<div style="margin-top:14px;font-size:19px;font-weight:800;color:var(--ink)">run <span style="font-weight:600;font-size:13px;color:var(--muted2)">/rʌn/</span></div>' +
      '<div style="margin-top:4px;font-size:13.5px;font-weight:600;color:var(--muted)">З’єднай речення з перекладом</div>' +
      '<div style="margin-top:14px;display:grid;grid-template-columns:1.4fr 1fr;gap:9px">' +
      '<div style="display:flex;flex-direction:column;gap:9px">' + S.map(sRow).join('') + '</div>' +
      '<div style="display:flex;flex-direction:column;gap:9px">' + T.map(tChip).join('') + '</div>' +
      '</div>' +
      '<div style="margin-top:14px;text-align:center;font-size:12.5px;font-weight:700;color:var(--muted)">' + (o.done ? '4 / 4 з’єднано ' : '1 / 4 з’єднано · обери переклад для №2') + '</div>' +
      '</div></div>' +
      (o.done ? '<div style="position:absolute;left:24px;right:24px;bottom:28px"><div style="height:58px;border-radius:19px;background:var(--purple);box-shadow:0 10px 22px -10px rgba(79,70,229,.55);display:flex;align-items:center;justify-content:center;gap:9px;font-weight:800;font-size:16.5px;color:#fff">Далі ' + ic('chevR', 19, '#fff', 2.4) + '</div></div>' : '');
  }
  RD.frame({ x: 1020, y: RB, theme: 'light', label: 'Формат 3 · метчинг · в процесі', body: matchBody({}) });
  RD.frame({ x: 1490, y: RB, theme: 'dark', label: 'Формат 3 · всі пари з’єднано', body: matchBody({ done: true }) });

  /* UK→EN варіант */
  RD.frame({
    x: 1960, y: RB, theme: 'dark', label: 'UK → EN · вживання (пара ≥60%)',
    body: sessHead({ step: 7, correct: 6 }) +
      bigCard(
        '<div style="display:flex;justify-content:space-between;align-items:center">' + dirBadge('uk') + formatTag('впізнавання') + '</div>' +
        '<div style="margin-top:18px;font-size:22px;line-height:1.55;font-weight:700;color:var(--ink)">Вона ' + hl('керує') + ' невеликою пекарнею в центрі.</div>' +
        '<div style="margin-top:10px;font-size:14px;font-weight:600;color:var(--muted)">Яке слово тут пасує?</div>' +
        '<div style="margin-top:16px;display:grid;grid-template-columns:1fr 1fr;gap:10px">' +
        ansChip('runs') + ansChip('walks') + ansChip('leads') + ansChip('holds') + '</div>'
      ) +
      '<div style="position:absolute;left:0;right:0;bottom:34px;text-align:center;font-size:12.5px;font-weight:600;color:var(--muted2)">напрямок відкривається, коли пара досягає 60%</div>',
  });

  /* ================= EMPTY + РЕЗУЛЬТАТ ================= */
  RD.frame({
    x: 80, y: RC, theme: 'light', label: 'Empty · немає слів із 2+ перекладами',
    body: RD.statusbar(false) +
      '<div style="padding:8px 24px 0"><div style="font-size:30px;font-weight:800;letter-spacing:-.6px;color:var(--ink)">Почати практику</div></div>' +
      '<div style="padding:14px 22px 0"><div style="display:flex;gap:11px">' +
      typeCard({ title: 'Класика', sub: 'флеш-картки, всі слова', icon: 'cards' }) +
      typeCard({ title: 'Слово в контексті', sub: 'для слів із 2+ перекладами', icon: 'chat', on: true }) +
      '</div></div>' +
      '<div style="position:absolute;top:330px;left:36px;right:36px;bottom:130px;display:flex;flex-direction:column;align-items:center;justify-content:center;text-align:center">' +
      '<svg width="150" height="104" viewBox="0 0 150 104"><rect x="30" y="26" width="90" height="58" rx="13" fill="var(--surface)" stroke="var(--line)" stroke-width="1.5"/><rect x="22" y="18" width="90" height="58" rx="13" fill="var(--tint)" opacity=".55"/><rect x="44" y="42" width="52" height="8" rx="4" fill="var(--track)"/><rect x="44" y="57" width="34" height="7" rx="3.5" fill="var(--line2)"/><rect x="96" y="38" width="26" height="13" rx="6.5" fill="#FFCC00" opacity=".8"/></svg>' +
      '<div style="margin-top:18px;font-size:20px;font-weight:800;color:var(--ink)">Немає слів із кількома перекладами</div>' +
      '<div style="margin-top:9px;font-size:14.5px;line-height:22px;font-weight:500;color:var(--muted);max-width:290px">Такі пари з’являються, коли слово має 2+ збережені значення. Додай перекладів — і цей режим відкриється.</div>' +
      '<div style="margin-top:22px;width:100%">' + P.btn('До словників', 'neutral', { icon: ic('book', 19, 'var(--ink)', 2), style: 'border:1.4px solid var(--line);' }) + '</div></div>' +
      P.bottomBar(1),
  });

  RD.frame({
    x: 550, y: RC, theme: 'dark', label: 'Результат · найплутаніша пара',
    body: RD.statusbar(false) +
      '<div style="padding:8px 24px 0"><div style="font-size:22px;font-weight:800;letter-spacing:-.4px;color:var(--ink)">Слово в контексті</div></div>' +
      '<div style="padding:26px 24px 0;display:flex;flex-direction:column;align-items:center;text-align:center">' +
      '<div style="position:relative;width:140px;height:140px;display:flex;align-items:center;justify-content:center">' +
      '<svg width="140" height="140" viewBox="0 0 140 140"><circle cx="70" cy="70" r="58" fill="none" stroke="var(--track)" stroke-width="12"/><circle cx="70" cy="70" r="58" fill="none" stroke="var(--purple)" stroke-width="12" stroke-linecap="round" stroke-dasharray="364.4" stroke-dashoffset="72.9" transform="rotate(-90 70 70)"/></svg>' +
      '<span style="position:absolute;font-size:32px;font-weight:800;letter-spacing:-.7px;color:var(--purple-t)">80%</span></div>' +
      '<div style="margin-top:18px;font-size:24px;font-weight:800;color:var(--ink)">Раунд завершено</div>' +
      '<div style="margin-top:8px;font-size:15px;font-weight:500;color:var(--muted)">Правильних відповідей: 8 із 10.</div></div>' +
      '<div style="padding:22px 22px 0">' +
      '<div class="card" style="border-radius:18px;padding:16px;border:1px solid var(--peach-b)">' +
      '<div style="font-weight:800;font-size:12px;letter-spacing:.6px;color:var(--orange-t)">НАЙПЛУТАНІША ПАРА</div>' +
      '<div style="margin-top:12px;display:flex;align-items:center;gap:10px">' +
      '<span style="flex:1;border-radius:13px;background:var(--peach);padding:11px 12px;text-align:center"><span style="display:block;font-weight:800;font-size:15px;color:var(--ink)">run = керує</span><span style="display:block;margin-top:2px;font-size:11.5px;font-weight:700;color:var(--orange-t)">2 помилки</span></span>' +
      '<span style="flex:none;font-weight:800;font-size:15px;color:var(--muted2)">⇄</span>' +
      '<span style="flex:1;border-radius:13px;background:var(--neutral);padding:11px 12px;text-align:center"><span style="display:block;font-weight:800;font-size:15px;color:var(--ink)">run = бігає</span><span style="display:block;margin-top:2px;font-size:11.5px;font-weight:700;color:var(--muted)">обирав замість</span></span></div>' +
      '<div style="margin-top:11px;font-size:13px;line-height:18px;font-weight:600;color:var(--muted)">Ти двічі обрав «бігає» там, де run означає «керує». Ці пари прийдуть у наступному раунді першими.</div></div></div>' +
      '<div style="position:absolute;left:24px;right:24px;bottom:34px">' + P.btn('Ще раунд', 'primary') +
      '<div style="margin-top:10px">' + P.btn('Обрати теми', 'neutral', { style: 'border:1.4px solid var(--line);' }) + '</div></div>',
  });

  RD.note({
    x: 1020, y: RC + 40, w: 330, title: 'Напрямки і подача',
    items: [
      '<b>Без тумблера:</b> напрямок обирає система за дозріванням пари',
      'EN→UK (розуміння) — дефолт для нових пар; UK→EN (вживання) — коли пара ≥60%',
      'Бейдж напрямку на кожній картці: tint/purple для EN→UK, yellow-tint для UK→EN',
      'Метчинг — «фінальний бос»: приходить, коли всі значення слова ≥50%',
      'Сесія: 10 карток, найслабші пари першими; помилки раунду підмішуються в наступний',
      'Результат: блок найплутанішої пари — з логу помилок метчингу і впізнавання',
    ],
  });
})();
