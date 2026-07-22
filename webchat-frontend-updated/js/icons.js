// کتابخونه آیکون‌های SVG (جایگزین ایموجی)

const NETVIBE_ICONS = {
  camera: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M4 8a2 2 0 0 1 2-2h1.5l1-1.5h7l1 1.5H18a2 2 0 0 1 2 2v9a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V8Z"/><circle cx="12" cy="13" r="3.5"/></svg>`,

  user: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="8" r="3.5"/><path d="M5 20c0-3.5 3-6 7-6s7 2.5 7 6"/></svg>`,

  idCard: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="5" width="18" height="14" rx="2"/><circle cx="9" cy="12" r="2"/><path d="M14 10h4M14 14h4M6 16.5c.5-1.5 1.8-2 3-2s2.5.5 3 2"/></svg>`,

  mail: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="5" width="18" height="14" rx="2"/><path d="M3 7l9 6 9-6"/></svg>`,

  cake: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M4 20v-6a2 2 0 0 1 2-2h12a2 2 0 0 1 2 2v6"/><path d="M4 20h16"/><path d="M8 12V9M12 12V9M16 12V9"/><path d="M8 9c0-1 .5-1.5.5-2.5S8 5 8 5M12 9c0-1 .5-1.5.5-2.5S12 5 12 5M16 9c0-1 .5-1.5.5-2.5S16 5 16 5"/></svg>`,

  globe: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="9"/><path d="M3 12h18M12 3c2.5 2.5 3.8 5.7 3.8 9s-1.3 6.5-3.8 9c-2.5-2.5-3.8-5.7-3.8-9S9.5 5.5 12 3Z"/></svg>`,

  moon: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M20 14.5A8.5 8.5 0 1 1 9.5 4a7 7 0 0 0 10.5 10.5Z"/></svg>`,

  palette: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M12 3a9 9 0 1 0 0 18c1.1 0 2-.9 2-2 0-.5-.2-1-.5-1.3-.3-.4-.5-.8-.5-1.2 0-.9.7-1.5 1.5-1.5H16a4 4 0 0 0 4-4c0-4.4-3.6-8-8-8Z"/><circle cx="7.5" cy="10.5" r="1"/><circle cx="10.5" cy="7" r="1"/><circle cx="15" cy="8" r="1"/></svg>`,

  textSize: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M4 7V5h11v2"/><path d="M9.5 5v14M7 19h5"/><path d="M15 13h5M17.5 13v6"/></svg>`,

  clock: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="9"/><path d="M12 7v5l3.5 2"/></svg>`,

  image: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="4" width="18" height="16" rx="2"/><circle cx="8.5" cy="9.5" r="1.5"/><path d="M21 16l-5.5-5.5L4 21"/></svg>`,

  userBlocked: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="8" r="3.5"/><path d="M5 20c0-3.5 3-6 7-6 1 0 1.9.15 2.7.42"/><circle cx="18" cy="17" r="3"/><path d="M16.4 15.4l3.2 3.2"/></svg>`,

  logout: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M9 4H6a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h3"/><path d="M15 16l4-4-4-4"/><path d="M19 12H9"/></svg>`,

  trash: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M4 7h16"/><path d="M9 7V5a1 1 0 0 1 1-1h4a1 1 0 0 1 1 1v2"/><path d="M6 7l1 12a2 2 0 0 0 2 2h6a2 2 0 0 0 2-2l1-12"/><path d="M10 11v6M14 11v6"/></svg>`,

  bell: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M6 10a6 6 0 0 1 12 0c0 4 1.5 5.5 1.5 5.5H4.5S6 14 6 10Z"/><path d="M9.5 18.5a2.5 2.5 0 0 0 5 0"/></svg>`,

  bellOff: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M6 10a6 6 0 0 1 9.5-4.9M18 10c0 4 1.5 5.5 1.5 5.5H8"/><path d="M9.5 18.5a2.5 2.5 0 0 0 5 0"/><path d="M4.5 15.5S6 14 6 10c0-.6.08-1.2.23-1.7"/><path d="M3 3l18 18"/></svg>`,

  info: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="9"/><path d="M12 11v5.5"/><circle cx="12" cy="8" r="0.5" fill="currentColor"/></svg>`,

  search: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><circle cx="10.5" cy="10.5" r="6.5"/><path d="M20 20l-4.5-4.5"/></svg>`,

  archive: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="4" width="18" height="5" rx="1.5"/><path d="M5 9v9a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V9"/><path d="M10 13h4"/></svg>`,

  paperclip: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M17 7.5l-7.5 7.5a3 3 0 0 0 4.24 4.24l8-8a5 5 0 0 0-7.07-7.07l-8 8a7 7 0 0 0 9.9 9.9"/></svg>`,

  video: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="6" width="13" height="12" rx="2"/><path d="M16 10l5-3v10l-5-3"/></svg>`,

  mic: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><rect x="9" y="3" width="6" height="11" rx="3"/><path d="M6 11a6 6 0 0 0 12 0"/><path d="M12 17v4M9 21h6"/></svg>`,

  file: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M7 3h7l4 4v14a1 1 0 0 1-1 1H7a1 1 0 0 1-1-1V4a1 1 0 0 1 1-1Z"/><path d="M14 3v4h4"/></svg>`,

  pin: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M14.5 3.5l6 6-3 2-3.5 3.5.5 5-2-2-4 4-1-1 4-4-2-2 5 .5 3.5-3.5 2-3Z"/></svg>`,

  pinOff: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M14.5 3.5l6 6-3 2-3.5 3.5.5 5-2-2-4 4-1-1 4-4-2-2 5 .5 3.5-3.5 2-3Z"/><path d="M3 3l18 18"/></svg>`,

  moreVertical: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><circle cx="12" cy="5" r="0.7" fill="currentColor" stroke="none"/><circle cx="12" cy="12" r="0.7" fill="currentColor" stroke="none"/><circle cx="12" cy="19" r="0.7" fill="currentColor" stroke="none"/></svg>`,

  userPlus: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><circle cx="9" cy="8" r="3.5"/><path d="M2.5 20c0-3.5 2.9-6 6.5-6s6.5 2.5 6.5 6"/><path d="M19 8v6M16 11h6"/></svg>`,

  edit: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M4 20h4l10.5-10.5a2.1 2.1 0 0 0-3-3L5 17v3Z"/><path d="M13.5 6.5l3 3"/></svg>`,

  reply: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M9 6l-5.5 5.5L9 17"/><path d="M3.5 11.5H14a6.5 6.5 0 0 1 6.5 6.5v1"/></svg>`,

  flag: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M5 3v18"/><path d="M5 4h11l-2.5 3.5L16 11H5"/></svg>`,
};

// جایگزینی خودکار: هر عنصری با data-icon="key" با SVG متناظر پر می‌شود
function injectNetVibeIcons() {
  document.querySelectorAll('[data-icon]').forEach(el => {
    const key = el.getAttribute('data-icon');
    if (NETVIBE_ICONS[key]) el.innerHTML = NETVIBE_ICONS[key];
  });
}
document.addEventListener('DOMContentLoaded', injectNetVibeIcons);
injectNetVibeIcons();
