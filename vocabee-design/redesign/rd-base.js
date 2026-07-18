/* ============================================================
   Vocabee Redesign — base: canvas helpers, icons, brand marks
   ============================================================ */
window.RD = {};
(function () {
  const RD = window.RD;

  RD.ACCENTS = {
    indigo: '#4F46E5', blue: '#5B7BFE', violet: '#7C5CF6', grape: '#410FA3',
    royal: '#3E63DD', plum: '#9333EA', teal: '#0E9FA5', amber: '#E0820C',
    rose: '#D6336C', emerald: '#17845A', navy: '#1E40AF', graphite: '#52525B',
  };

  /* ---------- line icons (stroke, 24 viewBox) ---------- */
  const PATHS = {
    book: '<path fill="currentColor" stroke="none" d="M19 2L14 6.5V17.5L19 13V2M6.5 5C4.55 5 2.45 5.4 1 6.5V21.16C1 21.41 1.25 21.66 1.5 21.66C1.6 21.66 1.65 21.59 1.75 21.59C3.1 20.94 5.05 20.5 6.5 20.5C8.45 20.5 10.55 20.9 12 22C13.35 21.15 15.8 20.5 17.5 20.5C19.15 20.5 20.85 20.81 22.25 21.56C22.35 21.61 22.4 21.59 22.5 21.59C22.75 21.59 23 21.34 23 21.09V6.5C22.4 6.05 21.75 5.75 21 5.5V19C19.9 18.65 18.7 18.5 17.5 18.5C15.8 18.5 13.35 19.15 12 20V6.5C10.55 5.4 8.45 5 6.5 5Z"/>',
    bookTab: '<path fill="currentColor" stroke="none" d="M6.5 20C8.2 20 10.65 20.65 12 21.5C13.35 20.65 15.8 20 17.5 20C19.15 20 20.85 20.3 22.25 21.05C22.35 21.1 22.4 21.1 22.5 21.1C22.75 21.1 23 20.85 23 20.6V6C22.4 5.55 21.75 5.25 21 5C19.89 4.65 18.67 4.5 17.5 4.5C15.55 4.5 13.45 4.9 12 6C10.55 4.9 8.45 4.5 6.5 4.5C5.33 4.5 4.11 4.65 3 5C2.25 5.25 1.6 5.55 1 6V20.6C1 20.85 1.25 21.1 1.5 21.1C1.6 21.1 1.65 21.1 1.75 21.05C3.15 20.3 4.85 20 6.5 20M12 19.5V8C13.35 7.15 15.8 6.5 17.5 6.5C18.7 6.5 19.9 6.65 21 7V18.5C19.9 18.15 18.7 18 17.5 18C15.8 18 13.35 18.65 12 19.5Z"/>',
    dumbbell: '<path fill="currentColor" stroke="none" d="M12 5C10.89 5 10 5.89 10 7S10.89 9 12 9 14 8.11 14 7 13.11 5 12 5M22 1V6H20V4H4V6H2V1H4V3H20V1H22M15 11.26V23H13V18H11V23H9V11.26C6.93 10.17 5.5 8 5.5 5.5L5.5 5H7.5L7.5 5.5C7.5 8 9.5 10 12 10S16.5 8 16.5 5.5L16.5 5H18.5L18.5 5.5C18.5 8 17.07 10.17 15 11.26Z"/>',
    user: '<path fill="currentColor" stroke="none" d="M9,11.75A1.25,1.25 0 0,0 7.75,13A1.25,1.25 0 0,0 9,14.25A1.25,1.25 0 0,0 10.25,13A1.25,1.25 0 0,0 9,11.75M15,11.75A1.25,1.25 0 0,0 13.75,13A1.25,1.25 0 0,0 15,14.25A1.25,1.25 0 0,0 16.25,13A1.25,1.25 0 0,0 15,11.75M12,2A10,10 0 0,0 2,12A10,10 0 0,0 12,22A10,10 0 0,0 22,12A10,10 0 0,0 12,2M12,20C7.59,20 4,16.41 4,12C4,11.71 4,11.42 4.05,11.14C6.41,10.09 8.28,8.16 9.26,5.77C11.07,8.33 14.05,10 17.42,10C18.2,10 18.95,9.91 19.67,9.74C19.88,10.45 20,11.21 20,12C20,16.41 16.41,20 12,20Z"/>',
    userF: '<path fill="currentColor" stroke="none" d="M13.75 13C13.75 12.31 14.31 11.75 15 11.75S16.25 12.31 16.25 13 15.69 14.25 15 14.25 13.75 13.69 13.75 13M22 12V22H2V12C2 6.5 6.5 2 12 2S22 6.5 22 12M4 12C4 16.41 7.59 20 12 20S20 16.41 20 12C20 11.21 19.88 10.45 19.67 9.74C18.95 9.91 18.2 10 17.42 10C14.05 10 11.07 8.33 9.26 5.77C8.28 8.16 6.41 10.09 4.05 11.14C4 11.42 4 11.71 4 12M9 14.25C9.69 14.25 10.25 13.69 10.25 13S9.69 11.75 9 11.75 7.75 12.31 7.75 13 8.31 14.25 9 14.25Z"/>',
    plus: '<path d="M12 5v14M5 12h14"/>',
    mic: '<rect x="9" y="2.5" width="6" height="12" rx="3"/><path d="M5.5 11.5a6.5 6.5 0 0 0 13 0M12 18v3"/>',
    sound: '<path d="M4 9v6h4l5 4V5L8 9H4Z"/><path d="M17 9.5a3.5 3.5 0 0 1 0 5M19.5 7a7 7 0 0 1 0 10"/>',
    sparkle: '<path d="M12 3l1.8 5.2L19 10l-5.2 1.8L12 17l-1.8-5.2L5 10l5.2-1.8L12 3Z"/><path d="M19 3.5l1.5 4.7M5 17l1.2 3.2"/>',
    check: '<path d="M5 12.5l4.5 4.5L19 7"/>',
    chevR: '<path d="M9 5l7 7-7 7"/>',
    chevL: '<path d="M15 5l-7 7 7 7"/>',
    chevD: '<path d="M5 9l7 7 7-7"/>',
    search: '<circle cx="11" cy="11" r="7"/><path d="M20 20l-4-4"/>',
    close: '<path d="M6 6l12 12M18 6L6 18"/>',
    globe: '<circle cx="12" cy="12" r="9"/><path d="M3 12h18M12 3c3 3 3 15 0 18M12 3c-3 3-3 15 0 18"/>',
    bell: '<path d="M6 9a6 6 0 0 1 12 0c0 5 2 6 2 6H4s2-1 2-6Z"/><path d="M10 20a2 2 0 0 0 4 0"/>',
    moon: '<path d="M20 14.5A8 8 0 1 1 9.5 4a6.5 6.5 0 0 0 10.5 10.5Z"/>',
    edit: '<path d="M14 5l5 5M4 20l1-4L16 5l3 3L8 19l-4 1Z"/>',
    help: '<circle cx="12" cy="12" r="9"/><path d="M9.5 9.5a2.5 2.5 0 1 1 3.4 2.3c-.7.3-.9.8-.9 1.5v.3"/><circle cx="12" cy="17" r="0.7" fill="currentColor" stroke="none"/>',
    invite: '<circle cx="9" cy="8" r="3.5"/><path d="M3 20c0-3.3 2.7-5.5 6-5.5s6 2.2 6 5.5M18 8v6M15 11h6"/>',
    flame: '<path d="M12 3c1 3-1.5 4-1.5 6.5 0 1.4 1.1 2 1.5 2 .4 0 1.5-.6 1.5-2C13.5 8 16 9 16 13a4 4 0 0 1-8 0c0-2 1-3 1-4 0 0-2.5.5-2.5 3.5A6 6 0 0 0 18 13c0-6-6-6-6-10Z"/>',
    bookmark: '<path d="M6 4h12v16l-6-4-6 4V4Z"/>',
    star: '<path fill="currentColor" stroke="none" d="M12,17.27L18.18,21L16.54,13.97L22,9.24L14.81,8.62L12,2L9.19,8.62L2,9.24L7.45,13.97L5.82,21L12,17.27Z"/>',
    arrowR: '<path d="M5 12h14M13 6l6 6-6 6"/>',
    play: '<circle cx="12" cy="12" r="9"/><path d="M10 8.5v7l6-3.5-6-3.5Z" fill="currentColor" stroke="none"/>',
    trash: '<path d="M4 7h16M9 7V5a2 2 0 0 1 2-2h2a2 2 0 0 1 2 2v2M6 7l1 13h10l1-13M10 11v6M14 11v6"/>',
    cards: '<rect x="3" y="6" width="13" height="15" rx="2.5"/><path d="M8 3h10a2.5 2.5 0 0 1 2.5 2.5V17"/>',
    plane: '<path fill="currentColor" stroke="none" d="M20.56 3.91C21.15 4.5 21.15 5.45 20.56 6.03L16.67 9.92L18.79 19.11L17.38 20.53L13.5 13.1L9.6 17L9.96 19.47L8.89 20.53L7.13 17.35L3.94 15.58L5 14.5L7.5 14.87L11.37 11L3.94 7.09L5.36 5.68L14.55 7.8L18.44 3.91C19 3.33 20 3.33 20.56 3.91Z"/>',
    car: '<path fill="currentColor" stroke="none" d="M16,6L19,10H21C22.11,10 23,10.89 23,12V15H21A3,3 0 0,1 18,18A3,3 0 0,1 15,15H9A3,3 0 0,1 6,18A3,3 0 0,1 3,15H1V12C1,10.89 1.89,10 3,10L6,6H16M10.5,7.5H6.75L4.86,10H10.5V7.5M12,7.5V10H17.14L15.25,7.5H12M6,13.5A1.5,1.5 0 0,0 4.5,15A1.5,1.5 0 0,0 6,16.5A1.5,1.5 0 0,0 7.5,15A1.5,1.5 0 0,0 6,13.5M18,13.5A1.5,1.5 0 0,0 16.5,15A1.5,1.5 0 0,0 18,16.5A1.5,1.5 0 0,0 19.5,15A1.5,1.5 0 0,0 18,13.5Z"/>',
    film: '<path fill="currentColor" stroke="none" d="M18,14.5V11A1,1 0 0,0 17,10H16C18.24,8.39 18.76,5.27 17.15,3C15.54,0.78 12.42,0.26 10.17,1.87C9.5,2.35 8.96,3 8.6,3.73C6.25,2.28 3.17,3 1.72,5.37C0.28,7.72 1,10.8 3.36,12.25C3.57,12.37 3.78,12.5 4,12.58V21A1,1 0 0,0 5,22H17A1,1 0 0,0 18,21V17.5L22,21.5V10.5L18,14.5M13,4A2,2 0 0,1 15,6A2,2 0 0,1 13,8A2,2 0 0,1 11,6A2,2 0 0,1 13,4M6,6A2,2 0 0,1 8,8A2,2 0 0,1 6,10A2,2 0 0,1 4,8A2,2 0 0,1 6,6Z"/>',
    brief: '<path fill="currentColor" stroke="none" d="M10,2H14A2,2 0 0,1 16,4V6H20A2,2 0 0,1 22,8V19A2,2 0 0,1 20,21H4C2.89,21 2,20.1 2,19V8C2,6.89 2.89,6 4,6H8V4C8,2.89 8.89,2 10,2M14,6V4H10V6H14Z"/>',
    grad: '<path fill="currentColor" stroke="none" d="M12,3L1,9L12,15L21,10.09V17H23V9M5,13.18V17.18L12,21L19,17.18V13.18L12,17L5,13.18Z"/>',
    food: '<path fill="currentColor" stroke="none" d="M11,9H9V2H7V9H5V2H3V9C3,11.12 4.66,12.84 6.75,12.97V22H9.25V12.97C11.34,12.84 13,11.12 13,9V2H11V9M16,6V14H18.5V22H21V2C18.24,2 16,4.24 16,6Z"/>',
    ball: '<path fill="currentColor" stroke="none" d="M16.93 17.12L16.13 15.76L17.59 11.39L19 10.92L20 11.67C20 11.7 20 11.75 20 11.81C20 11.88 20.03 11.94 20.03 12C20.03 13.97 19.37 15.71 18.06 17.21L16.93 17.12M9.75 15L8.38 10.97L12 8.43L15.62 10.97L14.25 15H9.75M12 20.03C11.12 20.03 10.29 19.89 9.5 19.61L8.81 18.1L9.47 17H14.58L15.19 18.1L14.5 19.61C13.71 19.89 12.88 20.03 12 20.03M5.94 17.21C5.41 16.59 4.95 15.76 4.56 14.75C4.17 13.73 3.97 12.81 3.97 12C3.97 11.94 4 11.88 4 11.81C4 11.75 4 11.7 4 11.67L5 10.92L6.41 11.39L7.87 15.76L7.07 17.12L5.94 17.21M11 5.29V6.69L7 9.46L5.66 9.04L5.24 7.68C5.68 7 6.33 6.32 7.19 5.66S8.87 4.57 9.65 4.35L11 5.29M14.35 4.35C15.13 4.57 15.95 5 16.81 5.66C17.67 6.32 18.32 7 18.76 7.68L18.34 9.04L17 9.47L13 6.7V5.29L14.35 4.35M4.93 4.93C3 6.89 2 9.25 2 12S3 17.11 4.93 19.07 9.25 22 12 22 17.11 21 19.07 19.07 22 14.75 22 12 21 6.89 19.07 4.93 14.75 2 12 2 6.89 3 4.93 4.93Z"/>',
    music: '<path fill="currentColor" stroke="none" d="M21,3V15.5A3.5,3.5 0 0,1 17.5,19A3.5,3.5 0 0,1 14,15.5A3.5,3.5 0 0,1 17.5,12C18.04,12 18.55,12.12 19,12.34V6.47L9,8.6V17.5A3.5,3.5 0 0,1 5.5,21A3.5,3.5 0 0,1 2,17.5A3.5,3.5 0 0,1 5.5,14C6.04,14 6.55,14.12 7,14.34V6L21,3Z"/>',
    leaf: '<path fill="currentColor" stroke="none" d="M10,21V18H3L8,13H5L10,8H7L12,3L17,8H14L19,13H16L21,18H14V21H10Z"/>',
    laptop: '<path fill="currentColor" stroke="none" d="M3 6H21V4H3C1.9 4 1 4.9 1 6V18C1 19.1 1.9 20 3 20H7V18H3V6M13 12H9V13.78C8.39 14.33 8 15.11 8 16C8 16.89 8.39 17.67 9 18.22V20H13V18.22C13.61 17.67 14 16.88 14 16S13.61 14.33 13 13.78V12M11 17.5C10.17 17.5 9.5 16.83 9.5 16S10.17 14.5 11 14.5 12.5 15.17 12.5 16 11.83 17.5 11 17.5M22 8H16C15.5 8 15 8.5 15 9V19C15 19.5 15.5 20 16 20H22C22.5 20 23 19.5 23 19V9C23 8.5 22.5 8 22 8M21 18H17V10H21V18Z"/>',
    bag: '<path fill="currentColor" stroke="none" d="M17,18C15.89,18 15,18.89 15,20A2,2 0 0,0 17,22A2,2 0 0,0 19,20C19,18.89 18.1,18 17,18M1,2V4H3L6.6,11.59L5.24,14.04C5.09,14.32 5,14.65 5,15A2,2 0 0,0 7,17H19V15H7.42A0.25,0.25 0 0,1 7.17,14.75C7.17,14.7 7.18,14.66 7.2,14.63L8.1,13H15.55C16.3,13 16.96,12.58 17.3,11.97L20.88,5.5C20.95,5.34 21,5.17 21,5A1,1 0 0,0 20,4H5.21L4.27,2M7,18C5.89,18 5,18.89 5,20A2,2 0 0,0 7,22A2,2 0 0,0 9,20C9,18.89 8.1,18 7,18Z"/>',
    heart: '<path fill="currentColor" stroke="none" d="M12,21.35L10.55,20.03C5.4,15.36 2,12.27 2,8.5C2,5.41 4.42,3 7.5,3C9.24,3 10.91,3.81 12,5.08C13.09,3.81 14.76,3 16.5,3C19.58,3 22,5.41 22,8.5C22,12.27 18.6,15.36 13.45,20.03L12,21.35Z"/>',
    child: '<path fill="currentColor" stroke="none" d="M12.5 11.5C13.3 11.5 14 10.8 14 10S13.3 8.5 12.5 8.5 11 9.2 11 10 11.7 11.5 12.5 11.5M5.5 6C6.6 6 7.5 5.1 7.5 4S6.6 2 5.5 2 3.5 2.9 3.5 4 4.4 6 5.5 6M7.5 22V15H9V9C9 7.9 8.1 7 7 7H4C2.9 7 2 7.9 2 9V15H3.5V22H7.5M14 22V18H15V14C15 13.2 14.3 12.5 13.5 12.5H11.5C10.7 12.5 10 13.2 10 14V18H11V22H14M18.5 6C19.6 6 20.5 5.1 20.5 4S19.6 2 18.5 2 16.5 2.9 16.5 4 17.4 6 18.5 6M22 9V15H20.5V22H17V14C17 12.6 16.2 11.4 15 10.9V9C15 7.9 15.9 7 17 7H20C21.1 7 22 7.9 22 9Z"/>',
    chat: '<path fill="currentColor" stroke="none" d="M7,5H23V9H22V10H16A1,1 0 0,0 15,11V12A2,2 0 0,1 13,14H9.62C9.24,14 8.89,14.22 8.72,14.56L6.27,19.45C6.1,19.79 5.76,20 5.38,20H2C2,20 -1,20 3,14C3,14 6,10 2,10V5H3L3.5,4H6.5L7,5M14,12V11A1,1 0 0,0 13,10H12C12,10 11,11 12,12A2,2 0 0,1 10,10A1,1 0 0,0 9,11V12A1,1 0 0,0 10,13H13A1,1 0 0,0 14,12Z"/>',
    copy: '<rect x="9" y="9" width="11" height="11" rx="2.5"/><path d="M15 9V6.5A2.5 2.5 0 0 0 12.5 4h-6A2.5 2.5 0 0 0 4 6.5v6A2.5 2.5 0 0 0 6.5 15H9"/>',
    share: '<path d="M12 15V4M8 7.5 12 3.5l4 4"/><path d="M5 12v7.5h14V12"/>',
    clip: '<path d="M20.5 12.5l-7.8 7.8a5.3 5.3 0 0 1-7.5-7.5l8.2-8.2a3.5 3.5 0 0 1 5 5l-8.2 8.2a1.77 1.77 0 0 1-2.5-2.5l7.5-7.5"/>',
    image: '<rect x="3.5" y="5" width="17" height="14" rx="2.5"/><circle cx="9" cy="10" r="1.6"/><path d="M4.5 17.5 10 12l4 4 2.5-2.5 3 3"/>',
    dots: '<circle cx="5.5" cy="12" r="1.5" fill="currentColor" stroke="none"/><circle cx="12" cy="12" r="1.5" fill="currentColor" stroke="none"/><circle cx="18.5" cy="12" r="1.5" fill="currentColor" stroke="none"/>',
    cat: '<path fill="currentColor" stroke="none" d="M12,8L10.67,8.09C9.81,7.07 7.4,4.5 5,4.5C5,4.5 3.03,7.46 4.96,11.41C4.41,12.24 4.07,12.67 4,13.66L2.07,13.95L2.28,14.93L4.04,14.67L4.18,15.38L2.61,16.32L3.08,17.21L4.53,16.32C5.68,18.76 8.59,20 12,20C15.41,20 18.32,18.76 19.47,16.32L20.92,17.21L21.39,16.32L19.82,15.38L19.96,14.67L21.72,14.93L21.93,13.95L20,13.66C19.93,12.67 19.59,12.24 19.04,11.41C20.97,7.46 19,4.5 19,4.5C16.6,4.5 14.19,7.07 13.33,8.09L12,8M9,11A1,1 0 0,1 10,12A1,1 0 0,1 9,13A1,1 0 0,1 8,12A1,1 0 0,1 9,11M15,11A1,1 0 0,1 16,12A1,1 0 0,1 15,13A1,1 0 0,1 14,12A1,1 0 0,1 15,11M11,14H13L12.3,15.39C12.5,16.03 13.06,16.5 13.75,16.5A1.5,1.5 0 0,0 15.25,15H15.75A2,2 0 0,1 13.75,17C13,17 12.35,16.59 12,16V16H12C11.65,16.59 11,17 10.25,17A2,2 0 0,1 8.25,15H8.75A1.5,1.5 0 0,0 10.25,16.5C10.94,16.5 11.5,16.03 11.7,15.39L11,14Z"/>',
    dog: '<path fill="currentColor" stroke="none" d="M18,4C16.29,4 15.25,4.33 14.65,4.61C13.88,4.23 13,4 12,4C11,4 10.12,4.23 9.35,4.61C8.75,4.33 7.71,4 6,4C3,4 1,12 1,14C1,14.83 2.32,15.59 4.14,15.9C4.78,18.14 7.8,19.85 11.5,20V15.72C10.91,15.35 10,14.68 10,14C10,13 12,13 12,13C12,13 14,13 14,14C14,14.68 13.09,15.35 12.5,15.72V20C16.2,19.85 19.22,18.14 19.86,15.9C21.68,15.59 23,14.83 23,14C23,12 21,4 18,4M4.15,13.87C3.65,13.75 3.26,13.61 3,13.5C3.25,10.73 5.2,6.4 6.05,6C6.59,6 7,6.06 7.37,6.11C5.27,8.42 4.44,12.04 4.15,13.87M9,12A1,1 0 0,1 8,11C8,10.46 8.45,10 9,10A1,1 0 0,1 10,11C10,11.56 9.55,12 9,12M15,12A1,1 0 0,1 14,11C14,10.46 14.45,10 15,10A1,1 0 0,1 16,11C16,11.56 15.55,12 15,12M19.85,13.87C19.56,12.04 18.73,8.42 16.63,6.11C17,6.06 17.41,6 17.95,6C18.8,6.4 20.75,10.73 21,13.5C20.75,13.61 20.36,13.75 19.85,13.87Z"/>',
    burger: '<path fill="currentColor" stroke="none" d="M18.06 23H19.72C20.56 23 21.25 22.35 21.35 21.53L23 5.05H18V1H16.03V5.05H11.06L11.36 7.39C13.07 7.86 14.67 8.71 15.63 9.65C17.07 11.07 18.06 12.54 18.06 14.94V23M1 22V21H16.03V22C16.03 22.54 15.58 23 15 23H2C1.45 23 1 22.54 1 22M16.03 15C16.03 7 1 7 1 15H16.03M1 17H16V19H1V17Z"/>',
    drink: '<path fill="currentColor" stroke="none" d="M9.5 3C7.56 3 5.85 4.24 5.23 6.08C3.36 6.44 2 8.09 2 10C2 12.21 3.79 14 6 14V22H17V20H20C20.55 20 21 19.55 21 19V11C21 10.45 20.55 10 20 10H18V8C18 5.79 16.21 4 14 4H12.32C11.5 3.35 10.53 3 9.5 3M9.5 5C10.29 5 11.03 5.37 11.5 6H14C15.11 6 16 6.9 16 8H12C10 8 9.32 9.13 8.5 10.63C7.68 12.13 6 12 6 12C4.89 12 4 11.11 4 10C4 8.9 4.89 8 6 8H7V7.5C7 6.12 8.12 5 9.5 5M17 12H19V18H17Z"/>',
    phone: '<path fill="currentColor" stroke="none" d="M12,17.27L18.18,21L16.54,13.97L22,9.24L14.81,8.62L12,2L9.19,8.62L2,9.24L7.45,13.97L5.82,21L12,17.27Z"/>',
    send: '<path d="M22 2 11 13"/><path d="M22 2 15 22l-4-9-9-4 20-7Z"/>',
  };

  RD.ICON_NAMES = Object.keys(PATHS);

  RD.ic = function (name, size, color, sw) {
    size = size || 20; color = color || 'var(--ink)'; sw = sw || 1.9;
    return '<svg width="' + size + '" height="' + size + '" viewBox="0 0 24 24" fill="none" stroke="' + color +
      '" stroke-width="' + sw + '" stroke-linecap="round" stroke-linejoin="round" style="flex:none;color:' + color + '">' +
      (PATHS[name] || '') + '</svg>';
  };

  /* ---------- brand ---------- */
  const HEX = '26,0 13,22.5 -13,22.5 -26,0 -13,-22.5 13,-22.5';
  RD.logo = function (size, color, accent) {
    color = color || 'var(--purple)'; accent = accent || '#FFCC00';
    return '<svg width="' + size + '" height="' + size + '" viewBox="-58 -58 116 116" style="flex:none">' +
      '<g transform="translate(-19.5,0)" stroke-linejoin="round">' +
      '<polygon points="' + HEX + '" fill="' + color + '" stroke="' + color + '" stroke-width="6"/>' +
      '<polygon points="' + HEX + '" transform="translate(39,-22.5)" fill="' + color + '" stroke="' + color + '" stroke-width="6"/>' +
      '<polygon points="' + HEX + '" transform="translate(39,22.5)" fill="' + accent + '" stroke="' + accent + '" stroke-width="6"/>' +
      '</g></svg>';
  };
  RD.honeycomb = function (size, alpha) {
    alpha = alpha || 0.18;
    return '<svg width="' + size + '" height="' + size + '" viewBox="-58 -58 116 116" style="position:absolute;top:-24px;right:-24px;opacity:' + alpha + '">' +
      '<g transform="translate(-19.5,0)" fill="none" stroke="#fff" stroke-width="5" stroke-linejoin="round">' +
      '<polygon points="' + HEX + '"/><polygon points="' + HEX + '" transform="translate(39,-22.5)"/><polygon points="' + HEX + '" transform="translate(39,22.5)"/>' +
      '</g></svg>';
  };
  /* ---------- honeycomb coin (currency) ---------- */
  RD.coin = function (size) {
    size = size || 16;
    return '<svg width="' + size + '" height="' + size + '" viewBox="-12 -12 24 24" style="flex:none">' +
      '<polygon points="10,0 5,8.66 -5,8.66 -10,0 -5,-8.66 5,-8.66" fill="#FFCC00" stroke="#E9B400" stroke-width="1" stroke-linejoin="round"/>' +
      '<polygon points="6.1,0 3.05,5.28 -3.05,5.28 -6.1,0 -3.05,-5.28 3.05,-5.28" fill="none" stroke="#96762B" stroke-width="1.7" stroke-linejoin="round" opacity=".55"/></svg>';
  };

  RD.google = function (size) {
    size = size || 20;
    return '<svg width="' + size + '" height="' + size + '" viewBox="0 0 24 24" style="flex:none">' +
      '<path fill="#4285F4" d="M21.6 12.2c0-.7-.06-1.3-.18-1.9H12v3.6h5.4a4.6 4.6 0 0 1-2 3v2.5h3.2c1.9-1.7 3-4.3 3-7.2Z"/>' +
      '<path fill="#34A853" d="M12 22c2.7 0 5-.9 6.6-2.4l-3.2-2.5c-.9.6-2 1-3.4 1-2.6 0-4.8-1.7-5.6-4.1H3.1v2.6A10 10 0 0 0 12 22Z"/>' +
      '<path fill="#FBBC05" d="M6.4 14c-.2-.6-.3-1.3-.3-2s.1-1.4.3-2V7.4H3.1A10 10 0 0 0 2 12c0 1.6.4 3.1 1.1 4.6L6.4 14Z"/>' +
      '<path fill="#EA4335" d="M12 5.9c1.5 0 2.8.5 3.8 1.5l2.8-2.8A10 10 0 0 0 3.1 7.4L6.4 10c.8-2.4 3-4.1 5.6-4.1Z"/></svg>';
  };

  /* ---------- status bar ---------- */
  RD.statusbar = function (light) {
    const c = light ? 'rgba(255,255,255,.95)' : 'var(--ink)';
    return '<div style="height:50px;flex:none;display:flex;align-items:center;justify-content:space-between;padding:14px 28px 0;font-size:15px;font-weight:700;color:' + c + '">' +
      '<span>9:41</span><span style="display:flex;gap:6px;align-items:center">' +
      '<svg width="17" height="11" viewBox="0 0 17 11" fill="' + c + '"><rect x="0" y="7" width="3" height="4" rx="1"/><rect x="4.5" y="5" width="3" height="6" rx="1"/><rect x="9" y="2.5" width="3" height="8.5" rx="1"/><rect x="13.5" y="0" width="3" height="11" rx="1"/></svg>' +
      '<svg width="16" height="11" viewBox="0 0 16 11" fill="' + c + '"><path d="M8 2.2c2 0 3.8.8 5.1 2l1.3-1.4A9 9 0 0 0 8 .2 9 9 0 0 0 1.6 2.8L2.9 4.2A7 7 0 0 1 8 2.2Z"/><path d="M8 5.6c1.1 0 2 .4 2.7 1.1l1.3-1.4A6 6 0 0 0 8 3.6a6 6 0 0 0-4 1.7l1.3 1.4A4 4 0 0 1 8 5.6Z"/><circle cx="8" cy="9.2" r="1.6"/></svg>' +
      '<svg width="25" height="12" viewBox="0 0 25 12" fill="none"><rect x="0.5" y="0.5" width="21" height="11" rx="3" stroke="' + c + '" opacity="0.5"/><rect x="2" y="2" width="17" height="8" rx="1.6" fill="' + c + '"/><rect x="23" y="3.5" width="1.6" height="5" rx="0.8" fill="' + c + '" opacity="0.5"/></svg>' +
      '</span></div>';
  };

  /* ---------- canvas placement ---------- */
  let maxX = 0, maxY = 0;
  function track(x, y, w, h) {
    maxX = Math.max(maxX, x + w + 200);
    maxY = Math.max(maxY, y + h + 240);
    document.body.style.width = maxX + 'px';
    document.body.style.height = maxY + 'px';
  }

  RD.frame = function (o) {
    const w = o.w || 390, h = o.h || 844, r = o.r == null ? 30 : o.r;
    const s = document.createElement('section');
    s.className = 'frame t-' + (o.theme || 'light');
    s.style.cssText = 'left:' + o.x + 'px;top:' + o.y + 'px;width:' + w + 'px;height:' + h + 'px;';
    s.setAttribute('data-screen-label', o.label || '');
    s.innerHTML = '<div class="frame-label">' + (o.label || '') +
      (o.theme === 'dark' ? ' <span class="lbl-chip dark-chip">dark</span>' : ' <span class="lbl-chip">light</span>') + '</div>' +
      '<div class="cv" style="border-radius:' + r + 'px">' + o.body + '</div>';
    document.body.appendChild(s);
    track(o.x, o.y, w, h);
    return s;
  };

  RD.board = function (o) { // non-phone wide spec board
    const s = document.createElement('section');
    s.className = 'board' + (o.cls ? ' ' + o.cls : '');
    s.style.cssText = 'left:' + o.x + 'px;top:' + o.y + 'px;width:' + o.w + 'px;' + (o.h ? 'height:' + o.h + 'px;' : '');
    s.setAttribute('data-screen-label', o.label || '');
    s.innerHTML = (o.label ? '<div class="frame-label">' + o.label + '</div>' : '') + o.body;
    document.body.appendChild(s);
    track(o.x, o.y, o.w, o.h || 400);
    return s;
  };

  RD.note = function (o) {
    const s = document.createElement('aside');
    s.className = 'note';
    s.style.cssText = 'left:' + o.x + 'px;top:' + o.y + 'px;width:' + (o.w || 300) + 'px;';
    s.innerHTML = '<h4>' + o.title + '</h4><ul>' + o.items.map(function (i) { return '<li>' + i + '</li>'; }).join('') + '</ul>';
    document.body.appendChild(s);
    track(o.x, o.y, o.w || 300, 200);
    return s;
  };

  RD.sec = function (o) {
    const s = document.createElement('div');
    s.className = 'sec-h';
    s.style.cssText = 'left:' + o.x + 'px;top:' + o.y + 'px;';
    s.innerHTML = '<span class="sec-num">' + o.num + '</span><h2>' + o.text + '</h2>' + (o.sub ? '<p>' + o.sub + '</p>' : '');
    document.body.appendChild(s);
    track(o.x, o.y, 700, 120);
    return s;
  };
})();
