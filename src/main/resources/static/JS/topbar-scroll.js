// Efecto topbar transparente con animación moderna al hacer scroll
// Aplica a la clase .topbar

document.addEventListener('DOMContentLoaded', function () {
  const topbar = document.querySelector('.topbar');
  if (!topbar) return;

  let lastScrollY = window.scrollY;
  let ticking = false;

  function updateTopbar() {
    if (window.scrollY > 10) {
      topbar.classList.add('topbar-scrolled');
    } else {
      topbar.classList.remove('topbar-scrolled');
    }
    ticking = false;
  }

  window.addEventListener('scroll', function () {
    if (!ticking) {
      window.requestAnimationFrame(updateTopbar);
      ticking = true;
    }
  });
});
