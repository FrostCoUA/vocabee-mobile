/* ============================================================
   Vocabee — data + icons + logo
   Learning model: user SPEAKS Ukrainian, is LEARNING English.
   Saved words are English; translation shown is Ukrainian.
   ============================================================ */

/* ---------- Logo (Honeycomb cell — the app's brand mark) ---------- */
function Logo({ size = 40, color = '#4F46E5', accent = '#FFCC00' }) {
  return (
    <svg width={size} height={size} viewBox="-58 -58 116 116" style={{ display: 'block' }}>
      <g transform="translate(-19.5,0)" strokeLinejoin="round">
        <polygon points="26,0 13,22.5 -13,22.5 -26,0 -13,-22.5 13,-22.5" transform="translate(0,0)" fill={color} stroke={color} strokeWidth="6" />
        <polygon points="26,0 13,22.5 -13,22.5 -26,0 -13,-22.5 13,-22.5" transform="translate(39,-22.5)" fill={color} stroke={color} strokeWidth="6" />
        <polygon points="26,0 13,22.5 -13,22.5 -26,0 -13,-22.5 13,-22.5" transform="translate(39,22.5)" fill={accent} stroke={accent} strokeWidth="6" />
      </g>
    </svg>
  );
}

/* ---------- Icon set (geometric line icons) ---------- */
const Ic = {
  book: (p) => (<svg viewBox="0 0 24 24" fill="none" {...iconProps(p)}><path d="M4 5.5A2.5 2.5 0 0 1 6.5 3H20v15H6.5A2.5 2.5 0 0 0 4 20.5V5.5Z"/><path d="M4 20.5A2.5 2.5 0 0 1 6.5 18H20v3H6.5A2.5 2.5 0 0 1 4 20.5Z"/></svg>),
  cards: (p) => (<svg viewBox="0 0 24 24" fill="none" {...iconProps(p)}><rect x="3" y="6" width="13" height="15" rx="2.5"/><path d="M8 3h10a2.5 2.5 0 0 1 2.5 2.5V17"/></svg>),
  user: (p) => (<svg viewBox="0 0 24 24" fill="none" {...iconProps(p)}><circle cx="12" cy="8" r="4"/><path d="M5 20c0-3.5 3.1-6 7-6s7 2.5 7 6"/></svg>),
  plus: (p) => (<svg viewBox="0 0 24 24" fill="none" {...iconProps(p)}><path d="M12 5v14M5 12h14"/></svg>),
  mic: (p) => (<svg viewBox="0 0 24 24" fill="none" {...iconProps(p)}><rect x="9" y="2.5" width="6" height="12" rx="3"/><path d="M5.5 11.5a6.5 6.5 0 0 0 13 0M12 18v3"/></svg>),
  sound: (p) => (<svg viewBox="0 0 24 24" fill="none" {...iconProps(p)}><path d="M4 9v6h4l5 4V5L8 9H4Z"/><path d="M17 9.5a3.5 3.5 0 0 1 0 5M19.5 7a7 7 0 0 1 0 10"/></svg>),
  sparkle: (p) => (<svg viewBox="0 0 24 24" fill="none" {...iconProps(p)}><path d="M12 3l1.8 5.2L19 10l-5.2 1.8L12 17l-1.8-5.2L5 10l5.2-1.8L12 3Z"/><path d="M19 3.5l.7 2 .8 2.7M5 17l.6 1.6.6 1.6" strokeWidth={p && p.thin ? 1.2 : 1.6}/></svg>),
  check: (p) => (<svg viewBox="0 0 24 24" fill="none" {...iconProps(p)}><path d="M5 12.5l4.5 4.5L19 7"/></svg>),
  chevR: (p) => (<svg viewBox="0 0 24 24" fill="none" {...iconProps(p)}><path d="M9 5l7 7-7 7"/></svg>),
  chevL: (p) => (<svg viewBox="0 0 24 24" fill="none" {...iconProps(p)}><path d="M15 5l-7 7 7 7"/></svg>),
  chevD: (p) => (<svg viewBox="0 0 24 24" fill="none" {...iconProps(p)}><path d="M5 9l7 7 7-7"/></svg>),
  search: (p) => (<svg viewBox="0 0 24 24" fill="none" {...iconProps(p)}><circle cx="11" cy="11" r="7"/><path d="M20 20l-4-4"/></svg>),
  close: (p) => (<svg viewBox="0 0 24 24" fill="none" {...iconProps(p)}><path d="M6 6l12 12M18 6L6 18"/></svg>),
  globe: (p) => (<svg viewBox="0 0 24 24" fill="none" {...iconProps(p)}><circle cx="12" cy="12" r="9"/><path d="M3 12h18M12 3c3 3 3 15 0 18M12 3c-3 3-3 15 0 18"/></svg>),
  bell: (p) => (<svg viewBox="0 0 24 24" fill="none" {...iconProps(p)}><path d="M6 9a6 6 0 0 1 12 0c0 5 2 6 2 6H4s2-1 2-6Z"/><path d="M10 20a2 2 0 0 0 4 0"/></svg>),
  moon: (p) => (<svg viewBox="0 0 24 24" fill="none" {...iconProps(p)}><path d="M20 14.5A8 8 0 1 1 9.5 4a6.5 6.5 0 0 0 10.5 10.5Z"/></svg>),
  edit: (p) => (<svg viewBox="0 0 24 24" fill="none" {...iconProps(p)}><path d="M14 5l5 5M4 20l1-4L16 5l3 3L8 19l-4 1Z"/></svg>),
  help: (p) => (<svg viewBox="0 0 24 24" fill="none" {...iconProps(p)}><circle cx="12" cy="12" r="9"/><path d="M9.5 9.5a2.5 2.5 0 1 1 3.4 2.3c-.7.3-.9.8-.9 1.5v.3"/><circle cx="12" cy="17" r="0.6" fill="currentColor" stroke="none"/></svg>),
  invite: (p) => (<svg viewBox="0 0 24 24" fill="none" {...iconProps(p)}><circle cx="9" cy="8" r="3.5"/><path d="M3 20c0-3.3 2.7-5.5 6-5.5s6 2.2 6 5.5M18 8v6M21 11h-6"/></svg>),
  flame: (p) => (<svg viewBox="0 0 24 24" fill="none" {...iconProps(p)}><path d="M12 3c1 3-1.5 4-1.5 6.5 0 1.4 1.1 2 1.5 2 .4 0 1.5-.6 1.5-2C13.5 8 16 9 16 13a4 4 0 0 1-8 0c0-2 1-3 1-4 0 0-2.5.5-2.5 3.5A6 6 0 0 0 18 13c0-6-6-6-6-10Z"/></svg>),
  arrowR: (p) => (<svg viewBox="0 0 24 24" fill="none" {...iconProps(p)}><path d="M5 12h14M13 6l6 6-6 6"/></svg>),
  bookmark: (p) => (<svg viewBox="0 0 24 24" fill="none" {...iconProps(p)}><path d="M6 4h12v16l-6-4-6 4V4Z"/></svg>),
  star: (p) => (<svg viewBox="0 0 24 24" fill="none" {...iconProps(p)}><path d="M12 3.5l2.6 5.3 5.9.9-4.3 4.1 1 5.8L12 17l-5.2 2.6 1-5.8L3.5 9.7l5.9-.9L12 3.5Z"/></svg>),
};
function iconProps(p) {
  p = p || {};
  return {
    width: p.size || 24, height: p.size || 24,
    stroke: p.color || 'currentColor',
    strokeWidth: p.sw || 1.9, strokeLinecap: 'round', strokeLinejoin: 'round',
    style: p.style,
  };
}

/* Brand logos for social auth (simple geometric) */
function GoogleG({ size = 20 }) {
  return (<svg width={size} height={size} viewBox="0 0 24 24"><path fill="#4285F4" d="M21.6 12.2c0-.7-.06-1.3-.18-1.9H12v3.6h5.4a4.6 4.6 0 0 1-2 3v2.5h3.2c1.9-1.7 3-4.3 3-7.2Z"/><path fill="#34A853" d="M12 22c2.7 0 5-.9 6.6-2.4l-3.2-2.5c-.9.6-2 1-3.4 1-2.6 0-4.8-1.7-5.6-4.1H3.1v2.6A10 10 0 0 0 12 22Z"/><path fill="#FBBC05" d="M6.4 14c-.2-.6-.3-1.3-.3-2s.1-1.4.3-2V7.4H3.1A10 10 0 0 0 2 12c0 1.6.4 3.1 1.1 4.6L6.4 14Z"/><path fill="#EA4335" d="M12 5.9c1.5 0 2.8.5 3.8 1.5l2.8-2.8A10 10 0 0 0 3.1 7.4L6.4 10c.8-2.4 3-4.1 5.6-4.1Z"/></svg>);
}

/* ============================================================
   Simulated AI dictionary  (English headword -> data)
   ============================================================ */
const LEXICON = {
  resilience:  { tr: 'стійкість',        ipa: 'rɪˈzɪliəns',   ctx: 'Her resilience helped her recover quickly.', ctxTr: 'Її стійкість допомогла їй швидко відновитися.' },
  reluctant:   { tr: 'неохочий',          ipa: 'rɪˈlʌktənt',   ctx: 'He was reluctant to leave so early.',          ctxTr: 'Він неохоче йшов так рано.' },
  remote:      { tr: 'віддалений',        ipa: 'rɪˈməʊt',      ctx: 'They live in a remote village.',               ctxTr: 'Вони живуть у віддаленому селі.' },
  remarkable:  { tr: 'визначний',         ipa: 'rɪˈmɑːkəbl',   ctx: 'It was a remarkable achievement.',             ctxTr: 'Це було визначне досягнення.' },
  luggage:     { tr: 'багаж',             ipa: 'ˈlʌɡɪdʒ',      ctx: 'I lost my luggage at the airport.',            ctxTr: 'Я загубив свій багаж в аеропорту.' },
  delay:       { tr: 'затримка',          ipa: 'dɪˈleɪ',       ctx: 'The flight had a two-hour delay.',             ctxTr: 'Рейс мав двогодинну затримку.' },
  departure:   { tr: 'відправлення',      ipa: 'dɪˈpɑːtʃə',    ctx: 'The departure gate has changed.',              ctxTr: 'Вихід на посадку змінився.' },
  destination: { tr: 'пункт призначення', ipa: 'ˌdestɪˈneɪʃn', ctx: 'We reached our destination by noon.',          ctxTr: 'Ми досягли пункту призначення опівдні.' },
  nourish:     { tr: 'живити',            ipa: 'ˈnʌrɪʃ',       ctx: 'Vegetables nourish the body.',                 ctxTr: 'Овочі живлять тіло.' },
  savory:      { tr: 'пікантний',         ipa: 'ˈseɪvəri',     ctx: 'The soup had a rich, savory taste.',           ctxTr: 'Суп мав насичений, пікантний смак.' },
  tender:      { tr: 'ніжний',            ipa: 'ˈtendə',       ctx: 'The meat was soft and tender.',                ctxTr: 'М’ясо було м’яким і ніжним.' },
  brittle:     { tr: 'крихкий',           ipa: 'ˈbrɪtl',       ctx: 'The old paper was brittle and yellow.',        ctxTr: 'Старий папір був крихким і жовтим.' },
  negotiate:   { tr: 'вести переговори',  ipa: 'nɪˈɡəʊʃieɪt',  ctx: 'They will negotiate the contract today.',      ctxTr: 'Вони сьогодні вестимуть переговори щодо контракту.' },
  revenue:     { tr: 'дохід',             ipa: 'ˈrevənjuː',    ctx: 'The company doubled its revenue.',             ctxTr: 'Компанія подвоїла свій дохід.' },
  stakeholder: { tr: 'зацікавлена сторона', ipa: 'ˈsteɪkhəʊldə', ctx: 'Every stakeholder was invited.',             ctxTr: 'Кожну зацікавлену сторону було запрошено.' },
  leverage:    { tr: 'важіль впливу',     ipa: 'ˈliːvərɪdʒ',   ctx: 'We can leverage our network here.',            ctxTr: 'Ми можемо використати наш важіль впливу тут.' },
  deadline:    { tr: 'кінцевий термін',   ipa: 'ˈdedlaɪn',     ctx: 'The deadline is next Friday.',                 ctxTr: 'Кінцевий термін — наступної п’ятниці.' },
  curious:     { tr: 'допитливий',        ipa: 'ˈkjʊəriəs',    ctx: 'The child was curious about everything.',      ctxTr: 'Дитина була допитлива до всього.' },
  whisper:     { tr: 'шепотіти',          ipa: 'ˈwɪspə',       ctx: 'She began to whisper a secret.',               ctxTr: 'Вона почала шепотіти таємницю.' },
  gloomy:      { tr: 'похмурий',          ipa: 'ˈɡluːmi',      ctx: 'The sky looked gloomy before the storm.',      ctxTr: 'Небо виглядало похмурим перед бурею.' },
  vivid:       { tr: 'яскравий',          ipa: 'ˈvɪvɪd',       ctx: 'She had a vivid memory of that day.',          ctxTr: 'Вона мала яскравий спогад про той день.' },
  linger:      { tr: 'затримуватися',     ipa: 'ˈlɪŋɡə',       ctx: 'The smell of coffee lingered in the room.',    ctxTr: 'Запах кави затримався в кімнаті.' },
  fragile:     { tr: 'тендітний',         ipa: 'ˈfrædʒaɪl',    ctx: 'Handle the vase, it is fragile.',              ctxTr: 'Обережно з вазою, вона тендітна.' },
  generous:    { tr: 'щедрий',            ipa: 'ˈdʒenərəs',    ctx: 'He gave a generous donation.',                 ctxTr: 'Він зробив щедру пожертву.' },
  glimpse:     { tr: 'проблиск',          ipa: 'ɡlɪmps',       ctx: 'I caught a glimpse of the sea.',               ctxTr: 'Я вловив проблиск моря.' },
  wander:      { tr: 'блукати',           ipa: 'ˈwɒndə',       ctx: 'We loved to wander the old streets.',          ctxTr: 'Ми любили блукати старими вулицями.' },
  cozy:        { tr: 'затишний',          ipa: 'ˈkəʊzi',       ctx: 'The cabin was warm and cozy.',                 ctxTr: 'Хатина була теплою та затишною.' },
  brave:       { tr: 'хоробрий',          ipa: 'breɪv',        ctx: 'It was a brave decision to make.',             ctxTr: 'Це було хоробре рішення.' },
  gentle:      { tr: 'лагідний',          ipa: 'ˈdʒentl',      ctx: 'A gentle breeze moved the curtains.',          ctxTr: 'Лагідний вітерець ворушив фіранки.' },
};
const LEX_KEYS = Object.keys(LEXICON);

/* Voice "recognition" pool — tapping mic resolves to one of these */
const VOICE_POOL = ['resilience', 'wander', 'savory', 'negotiate', 'vivid', 'cozy'];

function lookup(word) {
  const k = (word || '').trim().toLowerCase();
  return LEXICON[k] ? { word: k, ...LEXICON[k] } : null;
}
function suggest(query) {
  const q = (query || '').trim().toLowerCase();
  if (!q) return [];
  const starts = LEX_KEYS.filter((k) => k.startsWith(q));
  const contains = LEX_KEYS.filter((k) => !k.startsWith(q) && k.includes(q));
  return [...starts, ...contains].slice(0, 6).map((k) => ({ word: k, ...LEXICON[k] }));
}

/* ---------- Theme colors for dictionaries (cool brand family + restraint) ---------- */
const THEMES = [
  { key: 'indigo', c: '#4F46E5', deep: '#4036c9' },
  { key: 'blue',   c: '#5B7BFE', deep: '#4663e6' },
  { key: 'violet', c: '#7C5CF6', deep: '#6a47ed' },
  { key: 'grape',  c: '#410FA3', deep: '#350a87' },
  { key: 'royal',  c: '#3E63DD', deep: '#3151c4' },
  { key: 'plum',   c: '#9333EA', deep: '#7e22ce' },
  { key: 'teal',   c: '#0E9FA5', deep: '#0b878c' },
  { key: 'amber',  c: '#E0820C', deep: '#c4710a' },
];
const themeOf = (key) => THEMES.find((t) => t.key === key) || THEMES[0];

function mkWord(en) {
  const d = LEXICON[en];
  return { id: 'w_' + en, word: en, tr: d.tr, ipa: d.ipa, ctx: d.ctx, ctxTr: d.ctxTr };
}

/* ---------- Seed dictionaries ---------- */
const SEED_DICTS = [
  { id: 'd_travel',  name: 'Подорожі',        theme: 'blue',   lang: { from: 'en', to: 'uk' }, used: 'сьогодні',
    words: ['luggage', 'delay', 'departure', 'destination', 'wander', 'glimpse'].map(mkWord) },
  { id: 'd_book',    name: 'Книга · «1984»',  theme: 'indigo', lang: { from: 'en', to: 'uk' }, used: 'сьогодні',
    words: ['resilience', 'reluctant', 'gloomy', 'whisper', 'vivid', 'linger', 'brittle'].map(mkWord) },
  { id: 'd_work',    name: 'Робота',          theme: 'grape',  lang: { from: 'en', to: 'uk' }, used: 'вчора',
    words: ['negotiate', 'revenue', 'stakeholder', 'leverage', 'deadline'].map(mkWord) },
  { id: 'd_food',    name: 'Їжа та кулінарія', theme: 'amber', lang: { from: 'en', to: 'uk' }, used: '3 дні тому',
    words: ['savory', 'tender', 'nourish'].map(mkWord) },
  { id: 'd_feelings', name: 'Емоції',         theme: 'violet', lang: { from: 'en', to: 'uk' }, used: 'тиждень тому',
    words: ['curious', 'generous', 'gentle', 'brave', 'cozy'].map(mkWord) },
];

const LANGS = [
  { code: 'uk', name: 'Українська', flag: '🇺🇦', native: 'Ukrainian' },
  { code: 'en', name: 'Англійська', flag: '🇬🇧', native: 'English' },
  { code: 'de', name: 'Німецька',   flag: '🇩🇪', native: 'German' },
  { code: 'es', name: 'Іспанська',  flag: '🇪🇸', native: 'Spanish' },
  { code: 'fr', name: 'Французька', flag: '🇫🇷', native: 'French' },
  { code: 'pl', name: 'Польська',   flag: '🇵🇱', native: 'Polish' },
  { code: 'it', name: 'Італійська', flag: '🇮🇹', native: 'Italian' },
];
const langName = (code) => (LANGS.find((l) => l.code === code) || {}).name || code;
const langFlag = (code) => (LANGS.find((l) => l.code === code) || {}).flag || '🏳️';

Object.assign(window, {
  Logo, Ic, GoogleG,
  LEXICON, LEX_KEYS, VOICE_POOL, lookup, suggest,
  THEMES, themeOf, SEED_DICTS, mkWord,
  LANGS, langName, langFlag,
});
