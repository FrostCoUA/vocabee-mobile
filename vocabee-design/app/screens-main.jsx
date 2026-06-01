/* ============================================================
   Vocabee — Home (Dictionaries) + Dictionary detail
   ============================================================ */

function speakWord(word) {
  try {
    if (!window.speechSynthesis) return;
    window.speechSynthesis.cancel();
    const u = new SpeechSynthesisUtterance(word);
    u.lang = 'en-US'; u.rate = 0.92;
    window.speechSynthesis.speak(u);
  } catch (e) {}
}

function SpeakerButton({ word, tone = 'tint' }) {
  const [on, setOn] = React.useState(false);
  const click = (e) => {
    e.stopPropagation();
    speakWord(word);
    setOn(true); setTimeout(() => setOn(false), 650);
  };
  return (
    <button className={'spk' + (on ? ' on' : '') + ' spk-' + tone} onClick={click} aria-label="вимова">
      <Ic.sound size={17} sw={1.9} />
    </button>
  );
}

function AiContext({ ctx, ctxTr }) {
  return (
    <div className="ai-ctx">
      <div className="ai-tag"><Ic.sparkle size={12} sw={1.7} /> AI приклад</div>
      <div className="ai-ctx-en">{ctx}</div>
      <div className="ai-ctx-uk">{ctxTr}</div>
    </div>
  );
}

function WordRow({ w, accent }) {
  const [open, setOpen] = React.useState(false);
  return (
    <div className={'wrow' + (open ? ' open' : '')}>
      <div className="wrow-main" onClick={() => setOpen(!open)}>
        <div className="wrow-text">
          <div className="wrow-top">
            <span className="wrow-word">{w.word}</span>
            <span className="wrow-ipa">/{w.ipa}/</span>
          </div>
          <div className="wrow-tr">{w.tr}</div>
        </div>
        <div className="wrow-actions">
          <SpeakerButton word={w.word} />
          <button className="wrow-exp" style={{ color: accent }} onClick={(e) => { e.stopPropagation(); setOpen(!open); }}>
            <Ic.chevD size={18} sw={2} style={{ transform: open ? 'rotate(180deg)' : 'none', transition: 'transform .25s' }} />
          </button>
        </div>
      </div>
      {open && <AiContext ctx={w.ctx} ctxTr={w.ctxTr} />}
    </div>
  );
}

/* ---------------- HOME (Dictionaries) ---------------- */
function HoneyCorner({ opacity = 0.16 }) {
  return (
    <svg className="dc-honey" viewBox="-58 -58 116 116" width="120" height="120" style={{ opacity }}>
      <g transform="translate(-19.5,0)" strokeLinejoin="round" fill="none" stroke="#fff" strokeWidth="5">
        <polygon points="26,0 13,22.5 -13,22.5 -26,0 -13,-22.5 13,-22.5" transform="translate(0,0)"/>
        <polygon points="26,0 13,22.5 -13,22.5 -26,0 -13,-22.5 13,-22.5" transform="translate(39,-22.5)"/>
        <polygon points="26,0 13,22.5 -13,22.5 -26,0 -13,-22.5 13,-22.5" transform="translate(39,22.5)"/>
      </g>
    </svg>
  );
}

function DictCard({ d, onOpen }) {
  const t = themeOf(d.theme);
  return (
    <button className="dict-card" style={{ background: t.c }} onClick={() => onOpen(d.id)}>
      <HoneyCorner />
      {d.used === 'сьогодні' && <span className="dict-badge">сьогодні</span>}
      <div className="dict-card-body">
        <div className="dict-name">{d.name}</div>
        <div className="dict-meta">
          <span className="dict-count">{d.words.length} слів</span>
          {d.used !== 'сьогодні' && <span className="dict-used">{d.used}</span>}
        </div>
      </div>
    </button>
  );
}

function HomeEmpty({ onCreate }) {
  return (
    <div className="empty">
      <svg viewBox="0 0 200 170" width="190" height="160">
        <rect x="44" y="92" width="112" height="40" rx="13" fill="#EEF0FB"/>
        <rect x="36" y="60" width="128" height="42" rx="13" fill="#E0E7FF"/>
        <rect x="28" y="26" width="144" height="46" rx="14" fill="#fff" stroke="#E5E7F2"/>
        <g transform="translate(52,49)" strokeLinejoin="round"><polygon points="12,0 6,10 -6,10 -12,0 -6,-10 6,-10" fill="#C7D2FE"/></g>
        <rect x="74" y="42" width="64" height="8" rx="4" fill="#DDE3F7"/>
        <rect x="74" y="55" width="40" height="7" rx="3.5" fill="#E9ECF8"/>
        <circle cx="150" cy="49" r="8" fill="#FFCC00"/>
      </svg>
      <h2>Поки що порожньо</h2>
      <p>Створи свій перший тематичний словник — і починай збирати слова.</p>
      <button className="vbtn vbtn-primary" onClick={onCreate}><Ic.plus size={19} color="#fff" sw={2.2} /> Створити словник</button>
    </div>
  );
}

function Home({ dicts, onOpen, onCreate }) {
  const total = dicts.reduce((a, d) => a + d.words.length, 0);
  return (
    <div className="screen home">
      <StatusBar />
      <div className="home-scroll">
        <div className="home-head">
          <div>
            <div className="home-hello">Привіт, Надіє 👋</div>
            <h1 className="home-title">Словники</h1>
          </div>
          <div className="home-logo"><Logo size={30} /></div>
        </div>
        {dicts.length > 0 && (
          <div className="home-sum">
            <span><b>{dicts.length}</b> словники</span>
            <span className="home-sum-dot" />
            <span><b>{total}</b> слів зібрано</span>
          </div>
        )}

        {dicts.length === 0 ? (
          <HomeEmpty onCreate={onCreate} />
        ) : (
          <div className="dict-grid">
            {dicts.map((d) => <DictCard key={d.id} d={d} onOpen={onOpen} />)}
          </div>
        )}
        <div style={{ height: 96 }} />
      </div>
      {dicts.length > 0 && (
        <button className="fab" onClick={onCreate} aria-label="Створити словник"><Ic.plus size={26} color="#fff" sw={2.4} /></button>
      )}
    </div>
  );
}

/* ---------------- DICTIONARY DETAIL ---------------- */
function DetailEmpty({ accent }) {
  return (
    <div className="empty det-empty">
      <svg viewBox="0 0 200 150" width="170" height="130">
        <rect x="40" y="34" width="120" height="84" rx="14" fill="#F4F5FB" stroke="#E7E9F4"/>
        <rect x="58" y="56" width="62" height="8" rx="4" fill="#E2E6F4"/>
        <rect x="58" y="72" width="84" height="8" rx="4" fill="#EBEDF7"/>
        <rect x="58" y="88" width="48" height="8" rx="4" fill="#EBEDF7"/>
      </svg>
      <h2>Ще немає слів</h2>
      <p>Натисни <b style={{ color: accent }}>«+ Додати слово»</b> нижче — введи або продиктуй слово, а решту підкаже AI.</p>
      <div className="det-empty-arrow" style={{ color: accent }}>
        <svg width="34" height="60" viewBox="0 0 34 60" fill="none" stroke={accent} strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round"><path d="M17 4v44M17 48l-9-9M17 48l9-9"/></svg>
      </div>
    </div>
  );
}

function DictionaryDetail({ d, onBack, onAddWord, onOpenLang, addPillRef, hideAddPill }) {
  const t = themeOf(d.theme);
  return (
    <div className="screen detail">
      <div className="detail-hero" style={{ background: t.c }}>
        <StatusBar light />
        <div className="detail-bar">
          <button className="icon-btn light" onClick={onBack}><Ic.chevL size={22} color="#fff" sw={2.2} /></button>
          <button className="detail-lang" onClick={onOpenLang}>
            <span>{langFlag(d.lang.from)}</span><Ic.arrowR size={13} color="rgba(255,255,255,.8)" sw={2} /><span>{langFlag(d.lang.to)}</span>
            <Ic.chevD size={14} color="rgba(255,255,255,.8)" sw={2.2} />
          </button>
        </div>
        <div className="detail-hero-body">
          <HoneyCorner opacity={0.18} />
          <div className="detail-name">{d.name}</div>
          <div className="detail-count">{d.words.length} слів · {d.used}</div>
        </div>
      </div>

      <div className="detail-scroll">
        {d.words.length === 0 ? (
          <DetailEmpty accent={t.c} />
        ) : (
          <div className="wlist">
            {d.words.map((w) => <WordRow key={w.id} w={w} accent={t.c} />)}
            <div style={{ height: 110 }} />
          </div>
        )}
      </div>

      {!hideAddPill && (
        <div className="add-pill-wrap">
          <button ref={addPillRef} className="add-pill" style={{ background: t.c }} onClick={onAddWord}>
            <Ic.plus size={21} color="#fff" sw={2.4} /> Додати слово
          </button>
        </div>
      )}
    </div>
  );
}

Object.assign(window, { speakWord, SpeakerButton, AiContext, WordRow, Home, DictCard, HomeEmpty, DictionaryDetail, HoneyCorner });
