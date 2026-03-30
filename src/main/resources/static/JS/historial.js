document.addEventListener("DOMContentLoaded", function() {
  const btnLimpiar = document.querySelector('.limpiar-historial-btn');
  const modal = document.getElementById('modal-confirm-eliminar');
  const btnCancelar = document.getElementById('cancelar-eliminar');
  const btnConfirmar = document.getElementById('confirmar-eliminar');
  let limpiarCallback = null;
  if (btnLimpiar) {
    btnLimpiar.addEventListener('click', function() {
      if (modal) {
        modal.style.display = 'flex';
        limpiarCallback = async function() {
          btnLimpiar.disabled = true;
          btnConfirmar.disabled = true;
          try {
            const r = await fetch('/api/historial/limpiar', { method: 'DELETE' });
            if (!r.ok) throw new Error('Error al limpiar historial');
            location.reload();
          } catch (err) {
            alert(err.message);
          } finally {
            btnLimpiar.disabled = false;
            btnConfirmar.disabled = false;
            modal.style.display = 'none';
          }
        };
      }
    });
    if (btnCancelar && modal) {
      btnCancelar.addEventListener('click', function() {
        modal.style.display = 'none';
      });
    }
    if (btnConfirmar && modal) {
      btnConfirmar.addEventListener('click', function() {
        if (typeof limpiarCallback === 'function') limpiarCallback();
      });
    }
  }
});
