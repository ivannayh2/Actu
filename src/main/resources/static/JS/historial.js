document.addEventListener("DOMContentLoaded", function() {
  const btnLimpiar = document.querySelector('.limpiar-historial-btn');
  if (btnLimpiar) {
    btnLimpiar.addEventListener('click', async function() {
      if (!confirm('¿Seguro que deseas borrar todo el historial?')) return;
      btnLimpiar.disabled = true;
      try {
        const r = await fetch('/api/historial/limpiar', { method: 'DELETE' });
        if (!r.ok) throw new Error('Error al limpiar historial');
        location.reload();
      } catch (err) {
        alert(err.message);
      } finally {
        btnLimpiar.disabled = false;
      }
    });
  }
});
