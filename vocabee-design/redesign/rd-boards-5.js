/* ============================================================
   Boards 5: Запросити друзів (QR + share) і Допомога (форма)
   ============================================================ */
(function () {
  const ic = RD.ic, A = RD.ACCENTS;
  const Y0 = 14650, Y1 = 14810;

  RD.sec({ x: 80, y: Y0, num: '12', text: 'Запросити друзів · Допомога', sub: 'Два пуш-екрани з Профілю. Інвайт: QR-код із реф-лінкою, копіювання, системний share і швидкі кнопки месенджерів (тільки встановлені на девайсі). Допомога: форма на бек із вкладенням скріншота.' });

  /* ---------- shared pushed top bar ---------- */
  function pushedBar(title) {
    return RD.statusbar(false) +
      '<div style="display:flex;align-items:center;gap:12px;padding:8px 18px 0">' +
      '<span style="flex:none;width:40px;height:40px;border-radius:13px;background:var(--neutral);display:flex;align-items:center;justify-content:center">' + ic('chevL', 20, 'var(--ink)', 2.2) + '</span>' +
      '<span style="font-weight:800;font-size:22px;letter-spacing:-.4px;color:var(--ink)">' + title + '</span></div>';
  }

  /* ---------- fake-QR (finder patterns + deterministic modules) ---------- */
  function qrSvg(size) {
    const n = 21, c = size / n, dark = '#111827';
    let cells = '';
    const inFinder = (i, j) => (i < 7 && j < 7) || (i >= n - 7 && j < 7) || (i < 7 && j >= n - 7);
    const inLogo = (i, j) => i > 7 && i < 13 && j > 7 && j < 13;
    const rnd = (i, j) => { const v = Math.sin(i * 127.1 + j * 311.7) * 43758.5453; return (v - Math.floor(v)) > 0.52; };
    for (let i = 0; i < n; i++) for (let j = 0; j < n; j++) {
      if (inFinder(i, j) || inLogo(i, j) || !rnd(i, j)) continue;
      cells += '<rect x="' + (i * c).toFixed(1) + '" y="' + (j * c).toFixed(1) + '" width="' + (c * 0.9).toFixed(1) + '" height="' + (c * 0.9).toFixed(1) + '" rx="1.2"/>';
    }
    function finder(fx, fy) {
      return '<rect x="' + fx * c + '" y="' + fy * c + '" width="' + 7 * c + '" height="' + 7 * c + '" rx="' + c * 1.6 + '" fill="' + dark + '"/>' +
        '<rect x="' + (fx + 1) * c + '" y="' + (fy + 1) * c + '" width="' + 5 * c + '" height="' + 5 * c + '" rx="' + c * 1.1 + '" fill="#fff"/>' +
        '<rect x="' + (fx + 2) * c + '" y="' + (fy + 2) * c + '" width="' + 3 * c + '" height="' + 3 * c + '" rx="' + c * 0.7 + '" fill="' + dark + '"/>';
    }
    return '<svg width="' + size + '" height="' + size + '" viewBox="0 0 ' + size + ' ' + size + '">' +
      '<g fill="' + dark + '">' + cells + '</g>' + finder(0, 0) + finder(14, 0) + finder(0, 14) + '</svg>';
  }

  /* ---------- messenger buttons ---------- */
  function msgBtn(bg, inner, label) {
    return '<span style="display:flex;flex-direction:column;align-items:center;gap:6px">' +
      '<span style="width:54px;height:54px;border-radius:99px;background:' + bg + ';display:flex;align-items:center;justify-content:center">' + inner + '</span>' +
      '<span style="font-size:11px;font-weight:700;color:var(--muted)">' + label + '</span></span>';
  }
  const tgPlane = '<svg width="24" height="24" viewBox="0 0 24 24"><path d="M20.5 4 3.8 10.6l5 1.9L10.6 18l2.8-3.6 4.3 3.1L20.5 4Z" fill="#fff"/><path d="M8.8 12.5l8-6" stroke="rgba(255,255,255,.55)" stroke-width="1.2"/></svg>';

  function inviteBody(o) {
    o = o || {};
    return pushedBar('Запросити друзів') +
      '<div style="padding:16px 22px 40px;display:flex;flex-direction:column;gap:14px">' +
      /* hero + QR */
      '<div style="position:relative;border-radius:24px;overflow:hidden;background:radial-gradient(120% 100% at 50% 0%, #5B50F0 0%, #4F46E5 55%, #410FA3 100%);padding:24px 20px 20px;display:flex;flex-direction:column;align-items:center">' +
      RD.honeycomb(120, 0.16) +
      '<div style="position:relative;width:178px;height:178px;border-radius:18px;background:#fff;box-shadow:0 14px 30px -12px rgba(0,0,0,.45);display:flex;align-items:center;justify-content:center">' +
      qrSvg(146) +
      '<span style="position:absolute;width:36px;height:36px;border-radius:10px;background:#fff;display:flex;align-items:center;justify-content:center">' + RD.logo(26, '#4F46E5') + '</span></div>' +
      '<div style="margin-top:14px;font-weight:800;font-size:16px;color:#fff">Скануй — і вчімося разом</div>' +
      '<div style="margin-top:9px;display:inline-flex;align-items:center;gap:6px;background:rgba(255,255,255,.16);border-radius:99px;padding:6px 13px;font-weight:800;font-size:13px;color:#FFCC00">' + RD.coin(14) + '+10 монеток тобі й другові</div>' +
      '</div>' +
      /* link + copy */
      '<div style="display:flex;align-items:center;gap:10px;height:54px;border-radius:15px;background:var(--field);border:1.5px solid var(--line);padding:0 7px 0 15px;box-sizing:border-box">' +
      ic('globe', 17, 'var(--muted2)', 1.9) +
      '<span style="flex:1;min-width:0;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;font-weight:700;font-size:14.5px;color:var(--ink)">vocabee.app/i/nadiia-7k2</span>' +
      '<span style="flex:none;width:40px;height:40px;border-radius:12px;background:var(--surface);border:1px solid var(--line);display:flex;align-items:center;justify-content:center">' + ic('copy', 18, 'var(--purple-t)', 2) + '</span></div>' +
      P.btn('Поділитися', 'primary', { icon: ic('share', 19, '#fff', 2.2) }) +
      /* installed messengers */
      '<div style="display:flex;justify-content:space-around;padding:4px 6px 0">' +
      msgBtn('#229ED9', tgPlane, 'Telegram') +
      msgBtn('#25D366', ic('chat', 24, '#fff', 2), 'WhatsApp') +
      msgBtn('#7360F2', ic('chat', 24, '#fff', 2), 'Viber') +
      msgBtn('var(--neutral)', ic('dots', 24, 'var(--muted)', 2), 'Ще') +
      '</div></div>' +
      (o.copied ? P.snackbar('Лінк скопійовано', null, 30) : '');
  }

  RD.frame({ x: 80, y: Y1, theme: 'light', label: 'Запросити друзів · QR + share', body: inviteBody() });
  RD.frame({ x: 550, y: Y1, theme: 'dark', label: 'Запросити друзів · dark · лінк скопійовано', body: inviteBody({ copied: true }) });

  /* ---------- help form ---------- */
  function chip(t, on) {
    return '<span style="padding:9px 14px;border-radius:99px;font-weight:700;font-size:13.5px;' + (on ? 'background:var(--purple);color:#fff' : 'background:var(--neutral);color:var(--muted)') + '">' + t + '</span>';
  }
  function helpBody(o) {
    o = o || {};
    const msgField = o.filled
      ? '<div style="min-height:126px;border-radius:15px;background:var(--surface);border:1.5px solid var(--purple);padding:14px 15px;font-weight:600;font-size:15px;color:var(--ink);line-height:21px;box-sizing:border-box">Після додавання слова голосом воно не з’являється у списку, поки не перезайдеш у словник.<span class="caret"></span></div>'
      : '<div style="min-height:126px;border-radius:15px;background:var(--field);border:1.5px solid var(--line);padding:14px 15px;font-weight:500;font-size:15px;color:var(--muted2);line-height:21px;box-sizing:border-box">Опиши, що сталося або що хочеться покращити…</div>';
    const attach = o.attached
      ? '<div style="display:flex;gap:10px;align-items:center">' +
        '<span style="position:relative;width:56px;height:56px;border-radius:13px;background:var(--neutral);border:1px solid var(--line);display:flex;align-items:center;justify-content:center">' + ic('image', 22, 'var(--muted)', 1.9) +
        '<span style="position:absolute;top:-7px;right:-7px;width:22px;height:22px;border-radius:99px;background:var(--ink);display:flex;align-items:center;justify-content:center">' + ic('close', 12, 'var(--bg)', 2.6) + '</span></span>' +
        '<span style="flex:1;min-width:0"><span style="display:block;font-weight:700;font-size:13.5px;color:var(--ink);white-space:nowrap;overflow:hidden;text-overflow:ellipsis">screenshot_0711.png</span>' +
        '<span style="display:block;margin-top:2px;font-weight:600;font-size:12px;color:var(--muted2)">1.2 MB</span></span>' +
        '<span style="flex:none;width:56px;height:56px;border-radius:13px;border:1.6px dashed var(--muted3);display:flex;align-items:center;justify-content:center">' + ic('plus', 20, 'var(--muted)', 2.2) + '</span></div>'
      : '<div style="height:54px;border-radius:15px;border:1.6px dashed var(--muted3);display:flex;align-items:center;justify-content:center;gap:9px;font-weight:700;font-size:14.5px;color:var(--muted)">' + ic('clip', 18, 'var(--muted)', 2) + 'Додати фото</div>';
    return pushedBar('Допомога та підтримка') +
      '<div style="padding:14px 22px 40px;display:flex;flex-direction:column;gap:15px">' +
      '<div style="font-size:14.5px;line-height:20px;font-weight:500;color:var(--muted)">Опиши проблему чи ідею — відповімо на пошту протягом доби.</div>' +
      '<div>' + P.sheetLabel('Тема') + '<div style="display:flex;gap:8px;flex-wrap:wrap">' + chip('Помилка', o.filled) + chip('Ідея', false) + chip('Оплата', false) + chip('Інше', !o.filled) + '</div></div>' +
      '<div>' + P.sheetLabel('Повідомлення') + msgField + '</div>' +
      '<div>' + P.sheetLabel('Скріншот · необов’язково') + attach + '</div>' +
      (o.guest
        ? '<div>' + P.sheetLabel('Email для відповіді') + P.field({ value: 'you@email.com' }) + '</div>'
        : P.langInfoStrip('Відповімо на nadiia@vocabee.app')) +
      P.btn('Надіслати', 'primary', { icon: ic('send', 18, '#fff', 2) }) +
      '</div>' +
      (o.sent ? P.snackbar('Надіслано! Відповімо протягом доби', null, 30) : '');
  }

  RD.frame({ x: 1020, y: Y1, theme: 'light', label: 'Допомога · порожня форма (акаунт)', body: helpBody() });
  RD.frame({ x: 1490, y: Y1, theme: 'dark', label: 'Допомога · заповнена (гість) + надіслано', body: helpBody({ guest: true, filled: true, attached: true, sent: true }) });

  RD.note({
    x: 1960, y: Y1 + 40, w: 330, title: 'Поведінка',
    items: [
      '<b>Реф-лінка</b> vocabee.app/i/&lt;код&gt; — universal link: відкриває апку або стор; +10 монеток обом після першого входу друга',
      'QR = та сама лінка; копі → снекбар «Лінк скопійовано»',
      '<b>Месенджери:</b> показуються лише встановлені (deep-link resolve); «Ще» = системний share sheet — той самий, що кнопка «Поділитися»',
      '<b>Допомога:</b> POST на бек — тема + текст + до 3 фото ≤ 5MB (стискаються до 1080px)',
      'Email: з акаунта автоматично; для гостя — обовʼязкове поле',
      '«Надіслати» активна, коли є текст ≥ 10 символів; після відправки — снекбар і повернення в Профіль',
    ],
  });
})();
