/* ============================================================
   Vocabee — onboarding flow: Splash, Onboarding, Auth, Language
   ============================================================ */

function StatusBar({ light }) {
  const c = light ? 'rgba(255,255,255,.95)' : '#111827';
  return (
    <div className="vstatus" style={{ color: c }}>
      <span style={{ fontWeight: 700 }}>9:41</span>
      <span style={{ display: 'flex', gap: 6, alignItems: 'center' }}>
        <svg width="17" height="11" viewBox="0 0 17 11" fill={c}><rect x="0" y="7" width="3" height="4" rx="1"/><rect x="4.5" y="5" width="3" height="6" rx="1"/><rect x="9" y="2.5" width="3" height="8.5" rx="1"/><rect x="13.5" y="0" width="3" height="11" rx="1"/></svg>
        <svg width="16" height="11" viewBox="0 0 16 11" fill={c}><path d="M8 2.2c2 0 3.8.8 5.1 2l1.3-1.4A9 9 0 0 0 8 .2 9 9 0 0 0 1.6 2.8L2.9 4.2A7 7 0 0 1 8 2.2Z"/><path d="M8 5.6c1.1 0 2 .4 2.7 1.1l1.3-1.4A6 6 0 0 0 8 3.6a6 6 0 0 0-4 1.7l1.3 1.4A4 4 0 0 1 8 5.6Z"/><circle cx="8" cy="9.2" r="1.6"/></svg>
        <svg width="25" height="12" viewBox="0 0 25 12" fill="none"><rect x="0.5" y="0.5" width="21" height="11" rx="3" stroke={c} opacity="0.5"/><rect x="2" y="2" width="17" height="8" rx="1.6" fill={c}/><rect x="23" y="3.5" width="1.6" height="5" rx="0.8" fill={c} opacity="0.5"/></svg>
      </span>
    </div>
  );
}

/* ---------------- SPLASH ---------------- */
function Splash({ onDone }) {
  React.useEffect(() => {
    const t = setTimeout(onDone, 1900);
    return () => clearTimeout(t);
  }, []);
  return (
    <div className="screen splash" onClick={onDone}
      style={{ background: 'radial-gradient(120% 100% at 50% 0%, #5b50f0 0%, #4F46E5 48%, #410FA3 100%)', color: '#fff', alignItems: 'center', justifyContent: 'center' }}>
      <StatusBar light />
      <div className="splash-center">
        <div className="splash-mark"><Logo size={84} color="#fff" accent="#FFCC00" /></div>
        <div className="splash-word">voca<span style={{ color: '#FFCC00' }}>bee</span></div>
        <div className="splash-tag">Збирай слова. Будуй словники.</div>
      </div>
      <div className="splash-by">made with care</div>
    </div>
  );
}

/* ---------------- ONBOARDING ---------------- */
const SLIDES = [
  { art: 'read',     h: 'Зберігай слова під час читання', s: 'Натрапив на незнайоме слово в книзі? Збережи його одним дотиком — переклад підкаже AI.' },
  { art: 'organize', h: 'Створюй тематичні словники', s: 'Групуй слова за темами: подорожі, робота, улюблена книга. Кожен словник — свій колір.' },
  { art: 'practice', h: 'Повторюй, коли зручно', s: 'Картки для тренування з прикладами вживання. Кілька хвилин на день — і слова залишаються.' },
];

function OnbArt({ kind }) {
  if (kind === 'read') {
    return (
      <svg viewBox="0 0 220 200" width="220" height="200">
        <rect x="34" y="30" width="152" height="140" rx="16" fill="rgba(255,255,255,.10)"/>
        <rect x="52" y="52" width="116" height="96" rx="12" fill="#fff"/>
        <rect x="66" y="70" width="64" height="8" rx="4" fill="#C7D2FE"/>
        <rect x="66" y="86" width="88" height="8" rx="4" fill="#E0E7FF"/>
        <rect x="64" y="100" width="56" height="14" rx="5" fill="#FFCC00"/>
        <rect x="66" y="124" width="80" height="8" rx="4" fill="#E0E7FF"/>
        <g transform="translate(150,108)">
          <rect x="-16" y="-16" width="40" height="50" rx="9" fill="#410FA3"/>
          <path d="M-4 6l4 4 8-9" stroke="#FFCC00" strokeWidth="3.4" fill="none" strokeLinecap="round" strokeLinejoin="round"/>
        </g>
        <path d="M150 40l1.6 4.4 4.4 1.6-4.4 1.6L150 52l-1.6-4.4-4.4-1.6 4.4-1.6L150 40Z" fill="#FFCC00"/>
      </svg>
    );
  }
  if (kind === 'organize') {
    return (
      <svg viewBox="0 0 220 200" width="220" height="200">
        <rect x="48" y="118" width="124" height="44" rx="14" fill="rgba(255,255,255,.16)"/>
        <rect x="40" y="86" width="140" height="44" rx="14" fill="#7C5CF6"/>
        <rect x="32" y="52" width="156" height="48" rx="15" fill="#fff"/>
        <g transform="translate(54,76)" strokeLinejoin="round"><polygon points="13,0 6.5,11 -6.5,11 -13,0 -6.5,-11 6.5,-11" fill="#4F46E5"/></g>
        <rect x="78" y="68" width="70" height="8" rx="4" fill="#C7D2FE"/>
        <rect x="78" y="82" width="44" height="7" rx="3.5" fill="#E0E7FF"/>
        <circle cx="160" cy="76" r="9" fill="#FFCC00"/>
      </svg>
    );
  }
  return (
    <svg viewBox="0 0 220 200" width="220" height="200">
      <rect x="58" y="44" width="104" height="130" rx="18" fill="rgba(255,255,255,.12)" transform="rotate(-7 110 110)"/>
      <rect x="56" y="40" width="108" height="132" rx="18" fill="#fff"/>
      <rect x="78" y="64" width="64" height="11" rx="5.5" fill="#4F46E5"/>
      <rect x="84" y="86" width="52" height="7" rx="3.5" fill="#E0E7FF"/>
      <rect x="92" y="100" width="36" height="7" rx="3.5" fill="#E0E7FF"/>
      <g transform="translate(86,138)"><circle cx="0" cy="0" r="15" fill="#16A34A"/><path d="M-5 0l3.6 3.6L7-4" stroke="#fff" strokeWidth="3" fill="none" strokeLinecap="round" strokeLinejoin="round"/></g>
      <g transform="translate(134,138)"><circle cx="0" cy="0" r="15" fill="rgba(247,100,0,.16)"/><path d="M-4.5 -4.5l9 9M4.5 -4.5l-9 9" stroke="#F76400" strokeWidth="3" strokeLinecap="round"/></g>
    </svg>
  );
}

function Onboarding({ onDone }) {
  const [i, setI] = React.useState(0);
  const last = i === SLIDES.length - 1;
  const next = () => (last ? onDone() : setI(i + 1));
  const s = SLIDES[i];
  return (
    <div className="screen" style={{ background: 'radial-gradient(125% 90% at 50% 0%, #5b50f0 0%, #4F46E5 52%, #410FA3 100%)', color: '#fff' }}>
      <StatusBar light />
      <div className="onb-top">
        <button className="onb-skip" onClick={onDone}>Пропустити</button>
      </div>
      <div className="onb-art" key={i}><OnbArt kind={s.art} /></div>
      <div className="onb-copy" key={'c' + i}>
        <h1>{s.h}</h1>
        <p>{s.s}</p>
      </div>
      <div className="onb-foot">
        <div className="dots">
          {SLIDES.map((_, k) => <span key={k} className={'dot' + (k === i ? ' on' : '')} />)}
        </div>
        <button className="onb-next" onClick={next}>
          {last ? 'Почати' : 'Далі'} <Ic.arrowR size={19} color="#4F46E5" sw={2.2} />
        </button>
      </div>
    </div>
  );
}

/* ---------------- AUTH ---------------- */
function Auth({ onDone }) {
  const [mode, setMode] = React.useState('signup');
  const [email, setEmail] = React.useState('');
  const [pw, setPw] = React.useState('');
  const signup = mode === 'signup';
  return (
    <div className="screen auth">
      <StatusBar />
      <div className="auth-head">
        <button className="auth-skip" onClick={onDone}>Пропустити</button>
      </div>
      <div className="auth-body">
        <div className="auth-logo"><Logo size={46} /></div>
        <h1 className="auth-title">{signup ? 'Створи акаунт' : 'З поверненням'}</h1>
        <p className="auth-sub">{signup ? 'Кілька секунд — і починаємо збирати слова.' : 'Раді бачити тебе знову.'}</p>

        <label className="vlabel">Електронна пошта</label>
        <input className="vfield" type="email" placeholder="you@email.com" value={email} onChange={(e) => setEmail(e.target.value)} />
        <label className="vlabel">Пароль</label>
        <input className="vfield" type="password" placeholder="••••••••" value={pw} onChange={(e) => setPw(e.target.value)} />

        <button className="vbtn vbtn-primary vbtn-block" style={{ marginTop: 18 }} onClick={onDone}>
          {signup ? 'Зареєструватися' : 'Увійти'}
        </button>

        <div className="auth-or"><span>або</span></div>

        <button className="vbtn vbtn-social" onClick={onDone}><GoogleG /> Продовжити з Google</button>
        <button className="vbtn vbtn-social" onClick={onDone}>
          <svg width="20" height="20" viewBox="0 0 24 24" fill="#1877F2"><path d="M24 12a12 12 0 1 0-13.9 11.9v-8.4H7v-3.5h3.1V9.4c0-3 1.8-4.7 4.5-4.7 1.3 0 2.7.24 2.7.24v3h-1.5c-1.5 0-2 .93-2 1.9v2.2h3.4l-.55 3.5h-2.9v8.4A12 12 0 0 0 24 12Z"/></svg>
          Продовжити з Facebook
        </button>
      </div>
      <div className="auth-foot">
        {signup ? 'Вже маєш акаунт?' : 'Ще немає акаунта?'}
        <button onClick={() => setMode(signup ? 'login' : 'signup')}>{signup ? 'Увійти' : 'Зареєструватися'}</button>
      </div>
    </div>
  );
}

/* ---------------- LANGUAGE SELECTION (onboarding) ---------------- */
function LangPicker({ value, onChange, exclude }) {
  return (
    <div className="lang-grid">
      {LANGS.filter((l) => l.code !== exclude).map((l) => (
        <button key={l.code} className={'lang-card' + (value === l.code ? ' on' : '')} onClick={() => onChange(l.code)}>
          <span className="lang-flag">{l.flag}</span>
          <span className="lang-name">{l.name}</span>
          {value === l.code && <span className="lang-check"><Ic.check size={14} color="#fff" sw={2.6} /></span>}
        </button>
      ))}
    </div>
  );
}

function LanguageSelect({ onDone }) {
  const [speak, setSpeak] = React.useState('uk');
  const [learn, setLearn] = React.useState('en');
  return (
    <div className="screen lang">
      <StatusBar />
      <div className="lang-scroll">
        <div className="lang-logo"><Logo size={38} /></div>
        <h1 className="lang-title">Налаштуймо мови</h1>
        <p className="lang-sub">Це встановиться за замовчуванням для всіх нових словників. Змінити можна будь-коли в профілі.</p>

        <div className="lang-pair">
          <div className="lang-pill"><span className="lang-flag">{langFlag(speak)}</span>{langName(speak)}</div>
          <Ic.arrowR size={20} color="#9CA3AF" />
          <div className="lang-pill on"><span className="lang-flag">{langFlag(learn)}</span>{langName(learn)}</div>
        </div>

        <div className="lang-section">Я розмовляю</div>
        <LangPicker value={speak} onChange={setSpeak} exclude={learn} />

        <div className="lang-section">Я вивчаю</div>
        <LangPicker value={learn} onChange={setLearn} exclude={speak} />
      </div>
      <div className="lang-foot">
        <button className="vbtn vbtn-primary vbtn-block" onClick={() => onDone({ from: learn, to: speak })}>Готово</button>
      </div>
    </div>
  );
}

Object.assign(window, { StatusBar, Splash, Onboarding, Auth, LanguageSelect, LangPicker });
