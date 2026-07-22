// جلوه‌های بصری مشترک

const UiEffects = (() => {

  // پارتیکل پس‌زمینه
  function initParticles(container = document.body, count = 20) {
    for (let i = 0; i < count; i++) {
      const p = document.createElement('div');
      p.className = 'ui-particle';
      const size = 4 + Math.random() * 8;
      p.style.cssText = `
        left:${Math.random() * 100}%;
        bottom:-20px;
        width:${size}px;
        height:${size}px;
        opacity:${0.2 + Math.random() * 0.4};
        animation-duration:${6 + Math.random() * 6}s;
        animation-delay:${Math.random() * 8}s;
      `;
      container.appendChild(p);
    }
  }

  // افکت تایپ‌رایتر
  function startTypewriter(el, texts) {
    if (!el || !texts || !texts.length) return () => {};
    let ti = 0, ci = 0, deleting = false, stopped = false, timer = null;

    function step() {
      if (stopped) return;
      const cur = texts[ti];
      if (!deleting && ci < cur.length) {
        el.textContent = cur.slice(0, ++ci);
        timer = setTimeout(step, 110);
      } else if (!deleting && ci === cur.length) {
        timer = setTimeout(() => { deleting = true; step(); }, 1800);
      } else if (deleting && ci > 0) {
        el.textContent = cur.slice(0, --ci);
        timer = setTimeout(step, 60);
      } else {
        deleting = false;
        ti = (ti + 1) % texts.length;
        timer = setTimeout(step, 200);
      }
    }
    step();

    return () => { stopped = true; clearTimeout(timer); };
  }

  // تیلت سه‌بعدی
  function enableTilt(card) {
    if (!card) return;
    card.addEventListener('mousemove', (e) => {
      const r = card.getBoundingClientRect();
      const x = (e.clientX - r.left) / r.width - 0.5;
      const y = (e.clientY - r.top) / r.height - 0.5;
      card.style.transform = `perspective(1000px) rotateY(${x * 10}deg) rotateX(${-y * 10}deg) scale(1.015)`;
    });
    card.addEventListener('mouseleave', () => {
      card.style.transform = 'perspective(1000px) rotateY(0) rotateX(0) scale(1)';
    });
  }

  function enableLogoSpin(logo) {
    if (!logo) return;
    logo.addEventListener('click', () => {
      logo.classList.remove('spin');
      void logo.offsetWidth;
      logo.classList.add('spin');
      logo.addEventListener('animationend', () => logo.classList.remove('spin'), { once: true });
    });
  }

  function setupPasswordToggle(btn, input) {
    if (!btn || !input) return;
    btn.addEventListener('click', () => {
      const show = input.type === 'password';
      input.type = show ? 'text' : 'password';
      btn.textContent = show ? '🙈' : '👁️';
    });
  }

  function addRipple(btn, event) {
    const r = btn.getBoundingClientRect();
    const rip = document.createElement('span');
    rip.className = 'ripple-el';
    rip.style.left = (event.clientX - r.left - 20) + 'px';
    rip.style.top = (event.clientY - r.top - 20) + 'px';
    btn.appendChild(rip);
    rip.addEventListener('animationend', () => rip.remove());
  }

  function shake(el) {
    if (!el) return;
    el.classList.add('shake-target');
    el.addEventListener('animationend', () => el.classList.remove('shake-target'), { once: true });
  }

  function setDisabled(container, disabled) {
    container.querySelectorAll('input, button').forEach(el => { el.disabled = disabled; });
  }

  return {
    initParticles,
    startTypewriter,
    enableTilt,
    enableLogoSpin,
    setupPasswordToggle,
    addRipple,
    shake,
    setDisabled,
  };
})();
