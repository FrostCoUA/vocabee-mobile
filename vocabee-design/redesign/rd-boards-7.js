/* ============================================================
   Boards 7: Контекст — як реалізовано (ContextGlossarySentence)
   Кожне слово речення (крім цільового) підкреслене й тапабельне:
   тап → Popup над словом (слово + переклад + дія).
   У тренуванні дія = закладка, у словнику = додати у словник.
   ============================================================ */
(function () {
  const ic = RD.ic, A = RD.ACCENTS;
  const Y0 = 15800, Y1 = 16030, Y2 = 17030, Y3 = 18040;

  RD.sec({ x: 80, y: Y0, num: '13', text: 'Контекст · тап по слову в реченні', sub: 'Всі слова живуть у контексті: речення — прямо на картці тренування й у розкритій картці словника. Тап по будь-якому слову (крім цільового жовтого) відкриває попап з перекладом і букмаркою; збережені в закладки слова зафарбовуються світло-жовтим. У словнику дія — «+» одразу додає слово.' });

  /* ---------- helpers ---------- */
  const bmBadge = (n, pulse) => '<span style="flex:none;display:inline-flex;align-items:center;gap:5px;background:color-mix(in srgb, var(--yellow) 28%, transparent);border-radius:99px;padding:9px 12px;font-weight:800;font-size:13px;color:var(--ny-t);' + (pulse ? 'box-shadow:0 0 0 4px color-mix(in srgb, var(--yellow) 18%, transparent);' : '') + '">' + ic('bookmark', 14, 'var(--ny-t)', 2.2) + n + '</span>';
  function sessHead(o) {
    o = o || {};
    return RD.statusbar(false) +
      '<div style="padding:8px 24px 0">' +
      '<div style="display:flex;align-items:center;gap:9px">' +
      '<span style="flex:1;font-size:30px;font-weight:800;letter-spacing:-.6px;color:var(--ink)">Тренування</span>' +
      (o.bm ? bmBadge(o.bm, o.pulse) : '') +
      '<span style="flex:none;width:44px;height:44px;border-radius:14px;background:var(--neutral);display:flex;align-items:center;justify-content:center">' + ic('close', 21, 'var(--muted2)', 2.2) + '</span></div>' +
      '<div style="margin:12px 0 8px;font-size:13.5px;font-weight:700;color:var(--muted)">3 / 10 · правильно 2</div>' +
      P.progressLine(30) + '</div>';
  }
  const hlY = (w) => '<span style="background:var(--yellow);color:#5A4500;font-weight:800;border-radius:4px;padding:0 4px">' + w + '</span>';
  const tok = (w, mode) => {
    if (mode === 'sel') return '<span style="position:relative;display:inline-block;background:var(--tint);color:var(--purple-t);font-weight:800;border-radius:5px;padding:0 4px">' + w + '</span>';
    if (mode === 'bm') return '<span style="background:color-mix(in srgb, var(--yellow) 30%, transparent);color:var(--ny-t);font-weight:800;border-radius:5px;padding:0 4px">' + w + '</span>';
    return w; // звичайне слово — без підкреслень, але тапабельне
  };

  /* поповер = Surface r12, бордер purple-t 22%, elev2; слово 11/600 muted2, переклад 14/800 ink, дія 34dp r10 */
  function popover(o) {
    const actBg = o.saved ? 'color-mix(in srgb, var(--yellow) 34%, transparent)' : 'var(--tint)';
    const actIc = o.saved ? ic('check', 17, 'var(--ny-t)', 2.2) : ic(o.action === 'add' ? 'plus' : 'bookmark', 17, 'var(--purple-t)', 2.2);
    return '<span style="position:absolute;left:50%;bottom:calc(100% + 9px);transform:translateX(-' + (o.shift || 50) + '%);z-index:6;white-space:nowrap;display:inline-flex;align-items:center;background:var(--surface);border:1px solid color-mix(in srgb, var(--purple-t) 22%, transparent);border-radius:12px;padding:8px 8px 8px 13px;box-shadow:var(--elev2)">' +
      '<span style="display:flex;flex-direction:column;align-items:flex-start"><span style="font-size:11px;font-weight:600;color:var(--muted2)">' + o.w + '</span>' +
      '<span style="font-size:14px;font-weight:800;color:var(--ink)">' + o.tr + '</span></span>' +
      '<span style="margin-left:10px;width:34px;height:34px;border-radius:10px;background:' + actBg + ';display:flex;align-items:center;justify-content:center">' + actIc + '</span></span>';
  }
  const tokPop = (w, o) => '<span style="position:relative;display:inline-block;background:' + (o && o.saved ? 'color-mix(in srgb, var(--yellow) 30%, transparent)' : 'var(--tint)') + ';color:' + (o && o.saved ? 'var(--ny-t)' : 'var(--purple-t)') + ';font-weight:800;border-radius:5px;padding:0 4px">' + w + popover(Object.assign({ w: w }, o)) + '</span>';

  function sentence(mode) {
    if (mode === 'pop') return 'He ' + hlY('runs') + ' a small ' + tokPop('bakery', { tr: 'пекарня' }) + ' downtown.';
    if (mode === 'saved') return 'He ' + hlY('runs') + ' a small ' + tokPop('bakery', { tr: 'пекарня', saved: true, shift: 60 }) + ' ' + tok('downtown', 'bm') + '.';
    return 'He ' + hlY('runs') + ' a small bakery ' + tok('downtown', 'bm') + '.';
  }

  /* лицьова сторона флип-картки з реченням внизу — 1:1 з PracticeFlipCard */
  function flipFront(o) {
    o = o || {};
    return '<div style="position:absolute;top:196px;left:20px;right:20px;bottom:110px;border-radius:28px;background:var(--surface);border:2px solid ' + A.blue + ';box-shadow:var(--elev2);' + (o.mode ? 'overflow:visible' : 'overflow:hidden') + '">' +
      '<div style="position:absolute;left:0;top:0;bottom:0;width:40%;border-radius:26px 0 0 26px;background:color-mix(in srgb,' + A.blue + ' 8%, transparent)"></div>' +
      '<div style="position:relative;display:flex;flex-direction:column;align-items:center;padding:34px 30px 0">' +
      '<span style="display:inline-flex;align-items:center;gap:7px;background:color-mix(in srgb,' + A.blue + ' 12%, transparent);border-radius:99px;padding:7px 12px;font-weight:800;font-size:13px;color:' + A.blue + '"><span style="width:8px;height:8px;border-radius:99px;background:' + A.blue + '"></span>Подорожі</span>' +
      '<div style="margin-top:30px;font-size:40px;font-weight:800;letter-spacing:-1.2px;color:var(--ink)">bakery</div>' +
      '<div style="margin-top:8px;font-size:16px;font-weight:600;color:var(--muted2)">/ˈbeɪkəri/</div>' +
      '<div style="margin-top:18px">' + P.knowledgeBars(2, A.blue) + '</div>' +
      '<span style="margin-top:14px;width:48px;height:48px;border-radius:15px;background:var(--neutral);display:flex;align-items:center;justify-content:center">' + ic('sound', 21, 'var(--purple-t)', 1.9) + '</span></div>' +
      '<div style="position:absolute;left:30px;right:30px;bottom:26px;text-align:center;font-size:14px;line-height:20px;font-weight:600;color:var(--muted)">' + sentence(o.mode) + '</div>' +
      '</div>' +
      '<div style="position:absolute;left:20px;right:20px;bottom:30px;display:flex;gap:12px">' +
      P.answerBtn('Не знаю', 'close', 'var(--orange-t)', 'var(--peach)', true) +
      P.answerBtn('Знаю', 'check', 'var(--green-t)', 'var(--soft-green)', true) + '</div>';
  }

  /* ---------- row 1: у тренуванні ---------- */
  RD.frame({ x: 80, y: Y1, theme: 'light', label: '1 · картка з реченням · downtown уже в закладках', body: sessHead({ bm: 1 }) + flipFront() + '<div style="position:absolute;left:0;right:0;bottom:4px;text-align:center;font-size:12px;font-weight:600;color:var(--muted2)">жовте — цільове слово · зафарбоване світло-жовтим — закладка</div>' });
  RD.frame({ x: 550, y: Y1, theme: 'light', label: '2 · тап по слову: попап переклад + букмарка', body: sessHead({ bm: 1 }) + flipFront({ mode: 'pop' }) });
  RD.frame({ x: 1020, y: Y1, theme: 'dark', label: '3 · dark · збережене = жовтий стан ✓', body: sessHead({ bm: 2, pulse: true }) + flipFront({ mode: 'saved' }) });

  RD.note({
    x: 1490, y: Y1 + 40, w: 340, title: 'Поведінка (ContextGlossarySentence)',
    items: [
      'Токени речення приходять з бекенда разом із перекладами (context-glossary); легасі-записи дозбагачуються ліниво при показі картки',
      '<b>Тап по слову</b> → Popup над словом: слово 11/600 muted2, переклад 14/800 ink, дія 34dp r10; бордер purple-t 22%, тінь elev2',
      'Слова без підкреслень: звичайний текст тапабельний; вибране (попап відкритий) — заливка tint/purple, <b>збережене в закладки — жовта заливка</b> (як бейдж)',
      'Автозакриття через <b>2.4с</b> або тап поза поповером',
      'Дія у тренуванні — <b>закладка</b> (bookmark, tint): без пауз, слово летить у жовтий бейдж біля хедера; повторний тап у поповері знімає закладку',
      'Дія у словнику — <b>«+»</b>: слово одразу додається у поточний словник; збережене = жовтий фон + ✓',
      'Тап по контекстній зоні не перевертає картку (жест перехоплюється) — відповідь не розкривається випадково',
    ],
  });

  /* ---------- row 2: розбір закладок ---------- */
  function bmRow(o) {
    const act = o.saved
      ? '<span style="flex:none;width:38px;height:38px;border-radius:11px;background:var(--soft-green);display:flex;align-items:center;justify-content:center">' + ic('check', 18, 'var(--green-t)', 2.4) + '</span>'
      : '<span style="flex:none;width:38px;height:38px;border-radius:11px;background:var(--tint);display:flex;align-items:center;justify-content:center">' + ic('plus', 18, 'var(--purple-t)', 2.4) + '</span>';
    return '<div style="display:flex;align-items:center;gap:10px;padding:10px 0">' +
      '<span style="flex:none;width:38px;height:38px;border-radius:11px;background:color-mix(in srgb, var(--yellow) 28%, transparent);display:flex;align-items:center;justify-content:center">' + ic('bookmark', 17, 'var(--ny-t)', 2) + '</span>' +
      '<span style="flex:1;min-width:0"><span style="font-weight:800;font-size:14.5px;color:var(--ink)">' + o.w + '</span>' +
      '<span style="display:block;font-size:12.5px;font-weight:600;color:var(--muted)">' + o.tr + '</span>' +
      '<span style="display:block;font-size:10.5px;font-weight:500;color:var(--muted2);white-space:nowrap;overflow:hidden;text-overflow:ellipsis">з речення: “' + o.s + '”</span></span>' + act + '</div>';
  }
  RD.frame({
    x: 80, y: Y2, theme: 'light', label: '4 · результат раунду · «+» одне слово або «Додати всі»',
    body: RD.statusbar(false) +
      '<div style="padding:8px 24px 0;display:flex;align-items:center"><span style="flex:1;font-size:30px;font-weight:800;letter-spacing:-.6px;color:var(--ink)">Тренування</span>' + bmBadge(2) + '</div>' +
      '<div style="padding:20px 22px 0;display:flex;flex-direction:column;align-items:center">' +
      '<div style="position:relative;width:132px;height:132px;display:flex;align-items:center;justify-content:center">' +
      '<svg width="132" height="132" viewBox="0 0 132 132"><circle cx="66" cy="66" r="55" fill="none" stroke="var(--track)" stroke-width="11"/><circle cx="66" cy="66" r="55" fill="none" stroke="var(--purple)" stroke-width="11" stroke-linecap="round" stroke-dasharray="345.6" stroke-dashoffset="69" transform="rotate(-90 66 66)"/></svg>' +
      '<span style="position:absolute;font-size:31px;font-weight:800;color:var(--purple-t)">80%</span></div>' +
      '<div style="margin-top:16px;font-size:23px;font-weight:800;color:var(--ink)">Раунд завершено</div>' +
      '<div style="margin-top:6px;font-size:15px;font-weight:500;color:var(--muted)">Правильних відповідей: 8 із 10.</div></div>' +
      '<div style="padding:14px 22px 0">' +
      '<div class="card" style="border-radius:20px;padding:14px;border:1.2px solid var(--line)">' +
      '<div style="display:flex;align-items:center;justify-content:space-between"><span style="font-weight:800;font-size:11px;letter-spacing:.45px;color:var(--muted2)">НОВІ СЛОВА З РЕЧЕНЬ (2)</span>' + ic('bookmark', 15, 'var(--ny-t)', 2) + '</div>' +
      bmRow({ w: 'bakery', tr: 'пекарня', s: 'He runs a small bakery downtown.' }) +
      '<div style="height:1px;background:var(--line)"></div>' +
      bmRow({ w: 'downtown', tr: 'центр міста', s: 'He runs a small bakery downtown.' }) +
      '<div style="margin-top:4px;height:48px;border-radius:14px;background:var(--tint);display:flex;align-items:center;justify-content:center;font-weight:800;font-size:14.5px;color:var(--purple-t)">Додати всі (2)</div></div></div>' +
      '<div style="position:absolute;left:22px;right:22px;bottom:34px">' + P.btn('Ще раунд', 'primary') +
      '<div style="height:10px"></div>' + P.btn('Обрати теми', 'neutral', { style: 'border:1.4px solid var(--line);' }) + '</div>',
  });

  /* шит вибору словника для закладок */
  RD.frame({
    x: 550, y: Y2, h: 844, theme: 'dark', label: '5 · шит · куди зберегти закладки',
    body: P.sheetFrame({
      title: 'Обрати словник',
      body: '<div style="font-size:14px;font-weight:500;color:var(--muted);padding-bottom:14px">2 слова · 2 монетки · баланс 47</div>' +
        '<div style="display:flex;flex-direction:column;gap:9px">' +
        [['plane', 'Подорожі', A.blue], ['book', 'Книга · «1984»', A.indigo], ['heart', 'Емоції', A.violet]].map(function (t) {
          return '<div style="height:58px;border-radius:16px;background:var(--neutral);border:1px solid var(--line);display:flex;align-items:center;padding:0 14px;gap:11px">' +
            ic(t[0], 21, t[2], 2) + '<span style="flex:1;font-weight:800;font-size:15px;color:var(--ink)">' + t[1] + '</span>' + ic('chevR', 18, 'var(--muted2)', 2) + '</div>';
        }).join('') + '</div>' +
        '<div style="padding-top:12px;font-size:12.5px;line-height:18px;font-weight:500;color:var(--muted2)">Показуються лише словники з тією ж парою мов. Кожне слово коштує 1 монетку (повний AI-пошук з деталями).</div>',
    }),
  });

  /* гейти збереження закладок (BeeRewardSheet / AuthRequiredSheet · BookmarkSave) */
  RD.frame({
    x: 80, y: Y3, h: 844, theme: 'light', label: '8 · шит · мало монеток (гейт BookmarkSave)',
    body: P.sheetFrame({
      title: 'Закладки зачекають',
      body: P.banner('walletCritical', { bees: 1 }) +
        '<div style="margin-top:16px;font-size:15px;line-height:21px;font-weight:500;color:var(--muted)">Для додавання слова із закладок потрібна 1 монетка. Подивись відео — закладки залишаться тут.</div>' +
        '<div style="display:flex;gap:12px;padding-top:20px">' +
        P.btn('Пізніше', 'neutral', { grow: true }) +
        P.btn('Відео за +10', 'primary', { grow: true, icon: ic('play', 19, '#fff', 1.9) }) + '</div>',
    }),
  });
  RD.frame({
    x: 550, y: Y3, h: 844, theme: 'light', label: '9 · шит · гість (гейт BookmarkSave)',
    body: P.sheetFrame({
      title: 'Потрібен акаунт',
      body: P.banner('guestCritical', { dicts: '2/2', words: '50/50' }) +
        '<div style="margin-top:16px;font-size:15px;line-height:21px;font-weight:500;color:var(--muted)">Закладки залишаться в цьому раунді. Увійди через Google, щоб додати їх у словник і за потреби отримати монетки за рекламу.</div>' +
        '<div style="padding-top:20px">' + P.btn('Увійти через Google', 'primary') + '</div>',
    }),
  });
  RD.frame({
    x: 1020, y: Y3, theme: 'dark', label: '10 · після збереження · ✓ і оновлений бейдж',
    body: RD.statusbar(false) +
      '<div style="padding:8px 24px 0;display:flex;align-items:center"><span style="flex:1;font-size:30px;font-weight:800;letter-spacing:-.6px;color:var(--ink)">Тренування</span>' + bmBadge(1) + '</div>' +
      '<div style="padding:14px 22px 0">' +
      '<div class="card" style="border-radius:20px;padding:14px;border:1.2px solid var(--line)">' +
      '<div style="display:flex;align-items:center;justify-content:space-between"><span style="font-weight:800;font-size:11px;letter-spacing:.45px;color:var(--muted2)">НОВІ СЛОВА З РЕЧЕНЬ (2)</span>' + ic('bookmark', 15, 'var(--ny-t)', 2) + '</div>' +
      bmRow({ w: 'bakery', tr: 'пекарня', s: 'He runs a small bakery downtown.', saved: true }) +
      '<div style="height:1px;background:var(--line)"></div>' +
      bmRow({ w: 'downtown', tr: 'центр міста', s: 'He runs a small bakery downtown.' }) +
      '<div style="margin-top:4px;height:48px;border-radius:14px;background:var(--tint);display:flex;align-items:center;justify-content:center;font-weight:800;font-size:14.5px;color:var(--purple-t)">Додати всі (1)</div></div></div>' +
      '<div style="position:absolute;left:22px;right:22px;bottom:96px;height:52px;border-radius:15px;background:var(--ink);color:var(--bg);display:flex;align-items:center;padding:0 16px;gap:10px;box-shadow:var(--elev2)">' + ic('check', 17, 'var(--green-t)', 2.4) + '<span style="font-weight:700;font-size:13.5px">bakery додано в «Подорожі» · −1 монетка</span></div>' +
      '<div style="position:absolute;left:0;right:0;bottom:8px;text-align:center;font-size:12px;font-weight:600;color:var(--muted2)">збережене → ✓ і зникає з бейджа; «Додати всі» рахує решту</div>',
  });

  /* шит переривання тренування */
  RD.frame({
    x: 1020, y: Y2, h: 844, theme: 'light', label: '6 · шит · перервати тренування',
    body: P.sheetFrame({
      title: 'Перервати тренування?',
      body: '<div style="font-size:15px;line-height:21px;font-weight:500;color:var(--muted)">Раунд ще не завершено. Уже збережені відповіді залишаться в прогресі, але поточну сесію буде завершено.</div>' +
        '<div style="display:flex;gap:12px;padding-top:20px">' +
        P.btn('Перервати', 'neutral', { grow: true, style: 'border:1.4px solid var(--line);color:var(--red-t);' }) +
        P.btn('Продовжити', 'primary', { grow: true }) + '</div>',
    }),
  });

  /* контекст у словнику */
  RD.frame({
    x: 1490, y: Y2, theme: 'dark', label: '7 · словник · контекстний приклад + «+»',
    body: '<div style="display:flex;flex-direction:column;height:100%">' +
      P.detailHeader({ accent: A.indigo, title: 'Книга · «1984»', sub: '7 слів · сьогодні', progress: 0, flags: ['🇬🇧', '🇺🇦'] }) +
      '<div style="padding:10px 16px;display:flex;flex-direction:column;gap:10px">' +
      P.wordGroup({
        word: 'bakery', ipa: '/ˈbeɪkəri/', tr: 'пекарня', expanded: true, accentT: 'var(--purple-t)',
        details: '<div style="border-radius:13px;background:linear-gradient(var(--ctx1),var(--ctx2));border:1px solid var(--ctx-b);padding:14px;overflow:visible">' +
          '<div style="font-weight:800;font-size:11.5px;color:var(--muted2)">КОНТЕКСТНИЙ ПРИКЛАД</div>' +
          '<div style="position:relative;margin-top:9px;font-size:14px;line-height:20px;font-weight:600;color:var(--muted)">He ' + hlY('runs') + ' a small ' + tokPop('bakery', { tr: 'пекарня', action: 'add', shift: 46 }) + ' ' + tok('downtown') + '.</div></div>',
      }) +
      P.wordGroup({ word: 'reluctant', ipa: '/rɪˈlʌktənt/', tr: 'неохочий', accentT: 'var(--purple-t)' }) +
      '</div></div>' +
      P.dockBackdrop() + P.dock({ state: 'idle', accent: A.indigo }) +
      '<div style="position:absolute;left:0;right:0;bottom:4px;text-align:center;font-size:12px;font-weight:600;color:var(--muted2)">той самий поповер, дія «+» — слово додається у цей словник</div>',
  });

  RD.note({
    x: 1960, y: Y2 + 40, w: 340, title: 'Закладки → словник',
    items: [
      'Закладка зберігає слово + переклад + речення-джерело; лічильник у бейджі біля хедера сесії',
      'На результаті: «+» біля рядка → шит вибору для одного слова, «Додати всі» → для всіх разом (<code>PracticeBookmarkSelection.One/All</code>)',
      'Вибір словника — стандартний шит; фільтр по парі мов раунду (<code>compatibleBookmarkTopics</code>); якщо жодного — текст «Немає словника з такою парою мов»',
      'Збереження коштує <b>1 монетку за слово</b>; дублі вже збережених слів не тарифікуються',
      'Гейти (ряд нижче): гість → «Потрібен акаунт», мало монеток → «Закладки зачекають»; закладки НЕ губляться — лишаються в списку',
      'Після збереження рядок дістає ✓ і зникає з бейджа; незбережені живуть лише до кінця раунду',
    ],
  });
})();
