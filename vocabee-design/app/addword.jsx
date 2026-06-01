/* ============================================================
   Vocabee — Add word  (signature container-transform flow)
   The pill morphs into a full-screen surface, content staggers
   in, live AI suggestions, voice input, add-feedback, reverse.
   ============================================================ */

function ResultRow({ r, state, onAdd, accent }) {
  // state: 'add' | 'adding' | 'added'
  const added = state === 'added';
  const adding = state === 'adding';
  return (
    <div className={'res-row' + (adding ? ' pulse' : '') + (added ? ' is-added' : '')}>
      <div className="res-text">
        <div className="res-top">
          <span className="res-word">{r.word}</span>
          <span className="res-ipa">/{r.ipa}/</span>
          <span className="res-ai"><Ic.sparkle size={11} sw={1.7} /></span>
        </div>
        <div className="res-tr">{r.tr}</div>
      </div>
      {added ? (
        <div className="res-added"><Ic.check size={16} color="#16A34A" sw={2.4} /> додано</div>
      ) : (
        <button className="res-add" style={{ background: accent }} onClick={(e) => onAdd(r, e)}>
          {adding ? <Ic.check size={20} color="#fff" sw={2.6} /> : <Ic.plus size={20} color="#fff" sw={2.6} />}
        </button>
      )}
    </div>
  );
}

function Waveform({ active }) {
  const bars = 28;
  return (
    <div className={'wave' + (active ? ' on' : '')}>
      {Array.from({ length: bars }).map((_, k) => (
        <span key={k} style={{ animationDelay: (k * 45) % 600 + 'ms' }} />
      ))}
    </div>
  );
}

function AddWord({ origin, theme, dict, existing, onAdd, onClose, onOpenLang }) {
  const t = themeOf(theme);
  const [closing, setClosing] = React.useState(false);
  const [q, setQ] = React.useState('');
  const [recording, setRecording] = React.useState(false);
  const [localAdded, setLocalAdded] = React.useState({});   // word -> true
  const [adding, setAdding] = React.useState(null);          // word currently animating
  const [flyer, setFlyer] = React.useState(null);
  const wrapRef = React.useRef(null);
  const recTimer = React.useRef(null);

  const existingSet = React.useMemo(() => {
    const s = {}; (existing || []).forEach((w) => { s[w.word] = w; }); return s;
  }, [existing]);

  const requestClose = () => {
    if (closing) return;
    setClosing(true);
    setTimeout(onClose, 400);
  };

  const results = React.useMemo(() => suggest(q), [q]);
  const exactExisting = React.useMemo(() => {
    const k = q.trim().toLowerCase();
    return existingSet[k] || null;
  }, [q, existingSet]);

  const rowState = (word) => (localAdded[word] || existingSet[word]) ? 'added' : (adding === word ? 'adding' : 'add');

  const doAdd = (r, e) => {
    if (localAdded[r.word] || existingSet[r.word]) return;
    setAdding(r.word);
    // flyer chip
    try {
      const wrap = wrapRef.current.getBoundingClientRect();
      const btn = e.currentTarget.getBoundingClientRect();
      setFlyer({ word: r.word, x: btn.left - wrap.left + btn.width / 2, y: btn.top - wrap.top + btn.height / 2 });
      setTimeout(() => setFlyer(null), 700);
    } catch (err) {}
    setTimeout(() => {
      setLocalAdded((m) => ({ ...m, [r.word]: true }));
      setAdding(null);
      onAdd(r);
    }, 360);
  };

  // voice
  const startRec = () => {
    setRecording(true);
    recTimer.current = setTimeout(stopRec, 1900);
  };
  const stopRec = () => {
    clearTimeout(recTimer.current);
    setRecording(false);
    const word = VOICE_POOL[Math.floor(Math.random() * VOICE_POOL.length)];
    setQ(word);
  };
  const toggleRec = () => (recording ? stopRec() : startRec());
  React.useEffect(() => () => clearTimeout(recTimer.current), []);

  const addedCount = Object.keys(localAdded).length;

  const originVars = {
    '--ox': origin.left + 'px', '--oy': origin.top + 'px',
    '--ow': origin.width + 'px', '--oh': origin.height + 'px', '--tc': t.c,
  };

  return (
    <div className={'aw-root' + (closing ? ' closing' : '')} ref={wrapRef} style={originVars}>
      <div className="aw-surface" />

      <div className="aw-content">
        <div className="aw-head">
          <div className="aw-head-title">Додати у <b>«{dict.name}»</b></div>
          <button className="aw-lang" onClick={onOpenLang}>
            <span>{langFlag(dict.lang.from)}</span><Ic.arrowR size={12} color="#9CA3AF" sw={2} /><span>{langFlag(dict.lang.to)}</span>
            <Ic.chevD size={13} color="#9CA3AF" sw={2.2} />
          </button>
          <button className="aw-close" onClick={requestClose}><Ic.close size={22} color="#6B7280" sw={2.2} /></button>
        </div>

        <div className="aw-field-wrap">
          <Ic.search size={20} color="#9CA3AF" sw={2} />
          <input className="aw-field" autoFocus={false} placeholder="Введи слово англійською…" value={q}
            onChange={(e) => setQ(e.target.value)} />
          {q && <button className="aw-clear" onClick={() => setQ('')}><Ic.close size={16} color="#9CA3AF" sw={2.2} /></button>}
        </div>

        <div className="aw-body">
          {recording ? (
            <div className="aw-rec">
              <Waveform active />
              <button className="aw-mic recording" onClick={toggleRec}>
                <span className="aw-mic-ring" />
                <Ic.mic size={32} color="#fff" sw={1.9} />
              </button>
              <div className="aw-rec-hint">Слухаю… <span>торкнись, щоб зупинити</span></div>
            </div>
          ) : !q ? (
            <div className="aw-idle">
              <button className="aw-mic" style={{ background: t.c }} onClick={startRec}>
                <Ic.mic size={34} color="#fff" sw={1.9} />
              </button>
              <div className="aw-idle-title">Продиктуй слово</div>
              <div className="aw-idle-sub">або почни вводити його у поле вгорі</div>
            </div>
          ) : (
            <div className="aw-results">
              {exactExisting && (
                <div className="res-row is-existing">
                  <div className="res-text">
                    <div className="res-top"><span className="res-word">{exactExisting.word}</span><span className="res-ipa">/{exactExisting.ipa}/</span></div>
                    <div className="res-tr">{exactExisting.tr}</div>
                  </div>
                  <div className="res-added existing"><Ic.check size={15} color="#fff" sw={2.6} /> вже у словнику</div>
                </div>
              )}
              {results.filter((r) => !(exactExisting && r.word === exactExisting.word)).map((r) => (
                <ResultRow key={r.word} r={r} state={rowState(r.word)} accent={t.c} onAdd={doAdd} />
              ))}
              {results.length === 0 && !exactExisting && (
                <div className="aw-none"><Ic.search size={26} color="#D1D5DB" sw={1.7} /><span>Нічого не знайдено для «{q}»</span></div>
              )}
              {results.length > 0 && (
                <div className="aw-ai-note"><Ic.sparkle size={13} sw={1.7} /> Переклади та приклади згенеровано AI</div>
              )}
            </div>
          )}
        </div>

        {addedCount > 0 && (
          <div className="aw-footbar">
            <span><b>{addedCount}</b> {addedCount === 1 ? 'слово додано' : 'слів додано'}</span>
            <button onClick={requestClose}>Готово</button>
          </div>
        )}
      </div>

      {flyer && (
        <div className="aw-flyer" style={{ left: flyer.x, top: flyer.y, background: t.c }}>{flyer.word}</div>
      )}
    </div>
  );
}

Object.assign(window, { AddWord, ResultRow, Waveform });
