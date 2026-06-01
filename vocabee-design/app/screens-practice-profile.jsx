/* ============================================================
   Vocabee — Practice (Тренування) + Profile
   ============================================================ */

function Toggle({ on, onToggle, accent = '#4F46E5' }) {
  return (
    <button className={'toggle' + (on ? ' on' : '')} style={on ? { background: accent } : null} onClick={onToggle} aria-pressed={on}>
      <span className="toggle-knob" />
    </button>
  );
}

/* ---------------- PRACTICE ---------------- */
function buildDeck(dicts) {
  const all = [];
  dicts.forEach((d) => d.words.forEach((w) => all.push({ ...w, dict: d.name, theme: d.theme })));
  for (let i = all.length - 1; i > 0; i--) { const j = Math.floor(Math.random() * (i + 1)); [all[i], all[j]] = [all[j], all[i]]; }
  return all.slice(0, 10);
}

function Practice({ dicts }) {
  const [deck, setDeck] = React.useState(() => buildDeck(dicts));
  const [i, setI] = React.useState(0);
  const [flipped, setFlipped] = React.useState(false);
  const [known, setKnown] = React.useState(0);
  const [done, setDone] = React.useState(false);
  const restart = () => { setDeck(buildDeck(dicts)); setI(0); setFlipped(false); setKnown(0); setDone(false); };

  const answer = (yes) => {
    if (yes) setKnown((k) => k + 1);
    if (i + 1 >= deck.length) { setDone(true); return; }
    setFlipped(false);
    setTimeout(() => setI(i + 1), 180);
  };

  if (deck.length === 0) {
    return (
      <div className="screen practice">
        <StatusBar />
        <div className="prac-head"><h1>Тренування</h1></div>
        <div className="empty" style={{ flex: 1 }}>
          <svg viewBox="0 0 200 150" width="160"><rect x="50" y="40" width="100" height="70" rx="14" fill="#EEF0FB"/><path d="M100 60v30M85 75h30" stroke="#C7D2FE" strokeWidth="6" strokeLinecap="round"/></svg>
          <h2>Немає слів для повторення</h2>
          <p>Додай слова у словники — і вони з’являться тут для тренування.</p>
        </div>
      </div>
    );
  }

  if (done) {
    const pct = Math.round((known / deck.length) * 100);
    return (
      <div className="screen practice">
        <StatusBar />
        <div className="prac-head"><h1>Тренування</h1></div>
        <div className="prac-done">
          <div className="prac-ring">
            <svg viewBox="0 0 120 120" width="150" height="150">
              <circle cx="60" cy="60" r="52" fill="none" stroke="#EEF0FB" strokeWidth="12"/>
              <circle cx="60" cy="60" r="52" fill="none" stroke="#4F46E5" strokeWidth="12" strokeLinecap="round"
                strokeDasharray={2 * Math.PI * 52} strokeDashoffset={2 * Math.PI * 52 * (1 - pct / 100)} transform="rotate(-90 60 60)" style={{ transition: 'stroke-dashoffset .9s ease' }} />
            </svg>
            <div className="prac-ring-num">{pct}%</div>
          </div>
          <h2>Чудова робота! 🎉</h2>
          <p>Ти знаєш <b>{known}</b> із <b>{deck.length}</b> слів цього раунду.</p>
          <button className="vbtn vbtn-primary vbtn-block" onClick={restart}>Ще раунд</button>
        </div>
      </div>
    );
  }

  const w = deck[i];
  const t = themeOf(w.theme);
  return (
    <div className="screen practice">
      <StatusBar />
      <div className="prac-head">
        <h1>Тренування</h1>
        <div className="prac-progress">
          <div className="prac-pcount">{i + 1} / {deck.length}</div>
          <div className="prac-bar"><span style={{ width: ((i) / deck.length) * 100 + '%' }} /></div>
        </div>
      </div>

      <div className="prac-stage">
        <div className={'flip' + (flipped ? ' flipped' : '')} onClick={() => setFlipped(!flipped)}>
          <div className="flip-face flip-front" style={{ borderColor: t.c }}>
            <span className="flip-dict" style={{ color: t.c }}><span className="flip-dot" style={{ background: t.c }} />{w.dict}</span>
            <div className="flip-word">{w.word}</div>
            <div className="flip-ipa">/{w.ipa}/</div>
            <SpeakerButton word={w.word} tone="line" />
            <div className="flip-hint">Торкнись, щоб побачити переклад</div>
          </div>
          <div className="flip-face flip-back" style={{ background: t.c }}>
            <div className="flip-tr">{w.tr}</div>
            <div className="flip-ctx">
              <div className="flip-ctx-en">{w.ctx}</div>
              <div className="flip-ctx-uk">{w.ctxTr}</div>
            </div>
          </div>
        </div>
      </div>

      <div className="prac-actions">
        <button className="prac-btn no" onClick={() => answer(false)}>
          <span className="prac-btn-ic"><Ic.close size={20} sw={2.4} color="#F76400" /></span> Не знаю
        </button>
        <button className="prac-btn yes" onClick={() => answer(true)}>
          <span className="prac-btn-ic"><Ic.check size={20} sw={2.4} color="#16A34A" /></span> Знаю
        </button>
      </div>
    </div>
  );
}

/* ---------------- PROFILE ---------------- */
function StatCard({ icon, value, label, tint, color }) {
  return (
    <div className="stat-card">
      <div className="stat-ic" style={{ background: tint, color }}>{icon}</div>
      <div className="stat-val">{value}</div>
      <div className="stat-label">{label}</div>
    </div>
  );
}

function SettingRow({ icon, label, sub, right, onClick, danger }) {
  return (
    <button className="set-row" onClick={onClick}>
      <span className="set-ic">{icon}</span>
      <span className="set-text">
        <span className="set-label" style={danger ? { color: '#DC2626' } : null}>{label}</span>
        {sub && <span className="set-sub">{sub}</span>}
      </span>
      <span className="set-right">{right || <Ic.chevR size={18} color="#C2C7D6" sw={2} />}</span>
    </button>
  );
}

function Profile({ dicts, settings, setSettings, onEditLang }) {
  const totalWords = dicts.reduce((a, d) => a + d.words.length, 0);
  return (
    <div className="screen profile">
      <StatusBar />
      <div className="prof-scroll">
        <h1 className="prof-h">Профіль</h1>

        <div className="prof-card">
          <div className="prof-avatar">НК</div>
          <div className="prof-id">
            <div className="prof-name">Надія Кобилінська</div>
            <div className="prof-email">nadiia@vocabee.app</div>
          </div>
          <button className="prof-edit" onClick={() => {}}><Ic.edit size={18} color="#4F46E5" sw={1.9} /></button>
        </div>

        <div className="stat-row">
          <StatCard icon={<Ic.flame size={20} sw={1.9} />} value="7" label="днів поспіль" tint="#FFF4D6" color="#E0820C" />
          <StatCard icon={<Ic.bookmark size={20} sw={1.9} />} value={totalWords} label="слів збережено" tint="#E0E7FF" color="#4F46E5" />
          <StatCard icon={<Ic.cards size={20} sw={1.9} />} value="12" label="тренувань" tint="#E6F6F1" color="#0E9FA5" />
        </div>

        <div className="set-group-label">Мови за замовчуванням</div>
        <div className="set-group">
          <SettingRow icon={<span className="set-flag">{langFlag(settings.lang.to)}</span>} label="Я розмовляю" sub={langName(settings.lang.to)} onClick={() => onEditLang('to')} />
          <div className="set-div" />
          <SettingRow icon={<span className="set-flag">{langFlag(settings.lang.from)}</span>} label="Я вивчаю" sub={langName(settings.lang.from)} onClick={() => onEditLang('from')} />
        </div>
        <div className="set-note">Нові словники створюються з цією парою мов автоматично.</div>

        <div className="set-group-label">Налаштування</div>
        <div className="set-group">
          <SettingRow icon={<Ic.bell size={19} color="#6B7280" sw={1.8} />} label="Сповіщення" sub="Нагадування про тренування"
            right={<Toggle on={settings.notif} onToggle={() => setSettings((s) => ({ ...s, notif: !s.notif }))} />} onClick={() => setSettings((s) => ({ ...s, notif: !s.notif }))} />
          <div className="set-div" />
          <SettingRow icon={<Ic.moon size={19} color="#6B7280" sw={1.8} />} label="Темна тема"
            right={<Toggle on={settings.dark} onToggle={() => setSettings((s) => ({ ...s, dark: !s.dark }))} />} onClick={() => setSettings((s) => ({ ...s, dark: !s.dark }))} />
        </div>

        <div className="set-group">
          <SettingRow icon={<Ic.invite size={19} color="#6B7280" sw={1.8} />} label="Запросити друзів" sub="Поділись Vocabee" onClick={() => {}} />
          <div className="set-div" />
          <SettingRow icon={<Ic.help size={19} color="#6B7280" sw={1.8} />} label="Допомога та підтримка" onClick={() => {}} />
        </div>

        <button className="prof-logout">Вийти</button>
        <div className="prof-ver">Vocabee · v1.0.0</div>
        <div style={{ height: 96 }} />
      </div>
    </div>
  );
}

Object.assign(window, { Toggle, Practice, Profile, StatCard, SettingRow, buildDeck });
