/* ============================================================
   Vocabee — Bottom sheets: Create dictionary (A) + Language (C)
   ============================================================ */

function BottomSheet({ title, children, onClose, foot }) {
  const [closing, setClosing] = React.useState(false);
  const close = () => { if (closing) return; setClosing(true); setTimeout(onClose, 270); };
  return (
    <div className={'sheet-root' + (closing ? ' closing' : '')}>
      <div className="sheet-backdrop" onClick={close} />
      <div className="sheet">
        <div className="sheet-handle" onClick={close} />
        {title && (
          <div className="sheet-head">
            <h2>{title}</h2>
            <button className="sheet-x" onClick={close}><Ic.close size={20} color="#9CA3AF" sw={2.1} /></button>
          </div>
        )}
        <div className="sheet-body">{children}</div>
        {foot && <div className="sheet-foot">{foot}</div>}
      </div>
    </div>
  );
}

/* ---------------- A · CREATE DICTIONARY ---------------- */
function CreateDictionary({ defaultLang, count, onCreate, onClose }) {
  const [name, setName] = React.useState('');
  const [theme, setTheme] = React.useState('indigo');
  const atLimit = count >= 5;
  const valid = name.trim().length > 0;
  const create = () => { if (valid) onCreate({ name: name.trim(), theme, lang: defaultLang }); };
  return (
    <BottomSheet title="Новий словник" onClose={onClose}
      foot={
        <button className="vbtn vbtn-primary vbtn-block" disabled={!valid} style={!valid ? { opacity: .45 } : null} onClick={create}>
          Створити
        </button>
      }>
      <label className="vlabel">Назва теми</label>
      <input className="vfield" autoFocus placeholder="напр. Подорожі, Робота, Книга…" value={name} onChange={(e) => setName(e.target.value)} maxLength={28} />

      <label className="vlabel" style={{ marginTop: 18 }}>Колір теми</label>
      <div className="swatches">
        {THEMES.map((t) => (
          <button key={t.key} className={'swatch' + (theme === t.key ? ' on' : '')} style={{ background: t.c }} onClick={() => setTheme(t.key)}>
            {theme === t.key && <Ic.check size={18} color="#fff" sw={2.6} />}
          </button>
        ))}
      </div>

      <div className="create-lang">
        <Ic.globe size={16} color="#9CA3AF" sw={1.8} />
        <span>Мова: {langFlag(defaultLang.from)} {langName(defaultLang.from)} → {langFlag(defaultLang.to)} {langName(defaultLang.to)}</span>
      </div>

      {atLimit && (
        <div className="limit-note">
          <Ic.star size={16} color="#E0820C" sw={1.9} />
          <span>Ти створив(ла) максимум 5 словників. <b>Переглянь відео</b>, щоб відкрити більше.</span>
        </div>
      )}
    </BottomSheet>
  );
}

/* ---------------- C · LANGUAGE PICKER / OVERRIDE ---------------- */
function LanguageSheet({ title, sub, current, exclude, onPick, onClose }) {
  return (
    <BottomSheet title={title} onClose={onClose}>
      {sub && <p className="sheet-sub">{sub}</p>}
      <div className="lang-list">
        {LANGS.filter((l) => l.code !== exclude).map((l) => (
          <button key={l.code} className={'lang-row' + (current === l.code ? ' on' : '')} onClick={() => onPick(l.code)}>
            <span className="lang-flag">{l.flag}</span>
            <span className="lang-row-name">{l.name}</span>
            {current === l.code && <span className="lang-row-check"><Ic.check size={15} color="#fff" sw={2.6} /></span>}
          </button>
        ))}
      </div>
    </BottomSheet>
  );
}

Object.assign(window, { BottomSheet, CreateDictionary, LanguageSheet });
