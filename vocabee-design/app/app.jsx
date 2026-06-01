/* ============================================================
   Vocabee — App shell, navigation, tab bar, mount
   ============================================================ */

function TabBar({ tab, onTab }) {
  const tabs = [
    { id: 'dicts', label: 'Словники', icon: Ic.book },
    { id: 'practice', label: 'Тренування', icon: Ic.cards },
    { id: 'profile', label: 'Профіль', icon: Ic.user },
  ];
  return (
    <div className="tabbar">
      {tabs.map((t) => {
        const on = tab === t.id;
        return (
          <button key={t.id} className={'tab' + (on ? ' on' : '')} onClick={() => onTab(t.id)}>
            <t.icon size={24} sw={on ? 2.1 : 1.8} color={on ? '#4F46E5' : '#9CA3AF'} />
            <span>{t.label}</span>
          </button>
        );
      })}
    </div>
  );
}

function App() {
  const [flow, setFlow] = React.useState('splash');
  const [tab, setTab] = React.useState('dicts');
  const [view, setView] = React.useState('home');      // within dicts tab
  const [dicts, setDicts] = React.useState(SEED_DICTS);
  const [activeId, setActiveId] = React.useState(null);
  const [settings, setSettings] = React.useState({ lang: { from: 'en', to: 'uk' }, notif: true, dark: false });
  const [sheet, setSheet] = React.useState(null);
  const [addWord, setAddWord] = React.useState(null);
  const [scale, setScale] = React.useState(1);

  const frameRef = React.useRef(null);
  const pillRef = React.useRef(null);

  // scale-to-fit
  React.useEffect(() => {
    const fit = () => {
      const s = Math.min(1, (window.innerHeight - 44) / 844, (window.innerWidth - 32) / 390);
      setScale(s);
    };
    fit(); window.addEventListener('resize', fit);
    return () => window.removeEventListener('resize', fit);
  }, []);

  const activeDict = dicts.find((d) => d.id === activeId);

  const openDict = (id) => {
    setDicts((ds) => {
      const found = ds.find((d) => d.id === id);
      const rest = ds.filter((d) => d.id !== id);
      return [{ ...found, used: 'сьогодні' }, ...rest];
    });
    setActiveId(id); setView('detail');
  };

  const createDict = ({ name, theme, lang }) => {
    const id = 'd_' + Date.now();
    setDicts((ds) => [{ id, name, theme, lang, used: 'сьогодні', words: [] }, ...ds]);
    setSheet(null);
    setActiveId(id); setView('detail');
  };

  const addWordToDict = (r) => {
    setDicts((ds) => ds.map((d) => {
      if (d.id !== addWord.dictId) return d;
      if (d.words.some((w) => w.word === r.word)) return d;
      return { ...d, words: [mkWord(r.word), ...d.words], used: 'сьогодні' };
    }));
  };

  const startAddWord = () => {
    const fr = frameRef.current.getBoundingClientRect();
    const pr = pillRef.current.getBoundingClientRect();
    const origin = {
      left: (pr.left - fr.left) / scale,
      top: (pr.top - fr.top) / scale,
      width: pr.width / scale,
      height: pr.height / scale,
    };
    setAddWord({ origin, dictId: activeId });
  };

  const setDictLang = (dictId, code, which) => {
    setDicts((ds) => ds.map((d) => d.id === dictId ? { ...d, lang: { ...d.lang, [which]: code } } : d));
  };

  /* ---- render screens ---- */
  let screen = null;
  if (flow === 'splash') screen = <Splash onDone={() => setFlow('onboarding')} />;
  else if (flow === 'onboarding') screen = <Onboarding onDone={() => setFlow('auth')} />;
  else if (flow === 'auth') screen = <Auth onDone={() => setFlow('lang')} />;
  else if (flow === 'lang') screen = <LanguageSelect onDone={(pair) => { setSettings((s) => ({ ...s, lang: pair })); setFlow('app'); }} />;
  else {
    if (tab === 'dicts') {
      screen = (view === 'detail' && activeDict)
        ? <DictionaryDetail d={activeDict} onBack={() => setView('home')} onAddWord={startAddWord}
            onOpenLang={() => setSheet({ type: 'langDict', dictId: activeDict.id })}
            addPillRef={pillRef} hideAddPill={!!addWord} />
        : <Home dicts={dicts} onOpen={openDict} onCreate={() => setSheet({ type: 'create' })} />;
    } else if (tab === 'practice') {
      screen = <Practice dicts={dicts} />;
    } else {
      screen = <Profile dicts={dicts} settings={settings} setSettings={setSettings}
        onEditLang={(which) => setSheet({ type: 'langProfile', which })} />;
    }
  }

  const showTabs = flow === 'app';

  return (
    <div className="stage">
      <div className="app-scaler" style={{ transform: `scale(${scale})` }}>
        <div className={'app-frame' + (settings.dark ? ' dark' : '')} ref={frameRef}>
          <div className="screen-host">{screen}</div>
          {showTabs && <TabBar tab={tab} onTab={(t) => { setTab(t); if (t === 'dicts') setView('home'); }} />}

          {addWord && activeDict && (
            <AddWord origin={addWord.origin} theme={activeDict.theme} dict={activeDict} existing={activeDict.words}
              onAdd={addWordToDict} onClose={() => setAddWord(null)}
              onOpenLang={() => setSheet({ type: 'langDict', dictId: activeDict.id })} />
          )}

          {sheet && sheet.type === 'create' && (
            <CreateDictionary defaultLang={settings.lang} count={dicts.length} onCreate={createDict} onClose={() => setSheet(null)} />
          )}
          {sheet && sheet.type === 'langDict' && (() => {
            const d = dicts.find((x) => x.id === sheet.dictId);
            return (
              <LanguageSheet title="Мова словника" sub="Лише для цього словника. За замовчуванням використовується мова з профілю."
                current={d.lang.from} exclude={d.lang.to}
                onPick={(code) => { setDictLang(d.id, code, 'from'); setSheet(null); }} onClose={() => setSheet(null)} />
            );
          })()}
          {sheet && sheet.type === 'langProfile' && (
            <LanguageSheet
              title={sheet.which === 'to' ? 'Я розмовляю' : 'Я вивчаю'}
              current={settings.lang[sheet.which]}
              exclude={sheet.which === 'to' ? settings.lang.from : settings.lang.to}
              onPick={(code) => { setSettings((s) => ({ ...s, lang: { ...s.lang, [sheet.which]: code } })); setSheet(null); }}
              onClose={() => setSheet(null)} />
          )}
        </div>
      </div>
    </div>
  );
}

ReactDOM.createRoot(document.getElementById('root')).render(<App />);
