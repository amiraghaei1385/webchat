// لوگوی برند NetVibe

const NETVIBE_LOGO_SVG = `
<svg viewBox="0 0 90 90" xmlns="http://www.w3.org/2000/svg" width="100%" height="100%">
  <defs>
    <linearGradient id="netvibeLogoGradient" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" stop-color="#00e6d8"/>
      <stop offset="55%" stop-color="#6a5cff"/>
      <stop offset="100%" stop-color="#ff4d8d"/>
    </linearGradient>
  </defs>
  <path d="M45 6 C 66 6 84 24 84 45 C 84 62 72 76 56 81 L 60 90 L 44 80 C 24 79 6 63 6 45 C 6 24 24 6 45 6 Z"
        fill="url(#netvibeLogoGradient)"/>
  <path d="M 20 46 L 30 46 L 35 33 L 43 58 L 51 33 L 57 46 L 70 46"
        fill="none" stroke="white" stroke-width="5" stroke-linecap="round" stroke-linejoin="round"/>
</svg>
`.trim();

function injectNetVibeLogos() {
  document.querySelectorAll('[data-netvibe-logo]').forEach(el => {
    el.innerHTML = NETVIBE_LOGO_SVG;
  });
}
document.addEventListener('DOMContentLoaded', injectNetVibeLogos);
injectNetVibeLogos();
