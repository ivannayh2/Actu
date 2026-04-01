const modal = document.getElementById("modal");
const btnNuevo = document.getElementById("btnNuevo");
const form = document.getElementById("formUsuario");
const msg = document.getElementById("formMsg");

const inputCodigo = document.getElementById("codigo");
const inputRol = document.getElementById("rol");
const inputNombre = document.getElementById("nombre_usuario");
const inputEmail = document.getElementById("email");
const inputPasswordHash = document.getElementById("password_hash"); // ✅ id correcto

let editMode = false;
let editingCodigo = null;

/* ===== CSRF helper (si Spring Security lo tiene activo) ===== */
function csrfHeaders() {
  const token = document.querySelector('meta[name="_csrf"]')?.getAttribute("content");
  const header = document.querySelector('meta[name="_csrf_header"]')?.getAttribute("content");
  const h = { "Content-Type": "application/json" };
  if (token && header) h[header] = token;
  return h;
}

function openModal(title) {
  document.getElementById("modalTitle").textContent = title;
  modal.classList.remove("hidden");
  modal.setAttribute("aria-hidden", "false");
  msg.textContent = "";
}

function closeModal() {
  modal.classList.add("hidden");
  modal.setAttribute("aria-hidden", "true");
  form.reset();
  editMode = false;
  editingCodigo = null;
  inputCodigo.disabled = false;
}

modal.addEventListener("click", (e) => {
  if (e.target.closest("[data-close]")) {
    closeModal();
  }
});

document.addEventListener("keydown", (e) => {
  if (e.key === "Escape" && !modal.classList.contains("hidden")) {
    closeModal();
  }
});

btnNuevo.addEventListener("click", () => {
  form.reset();
  editMode = false;
  editingCodigo = null;
  inputCodigo.disabled = false;
  inputPasswordHash.required = true; // requerido en creación
  openModal("Nuevo usuario");
  // Marcar permisos por defecto del rol actual
  setTimeout(autoCheckPermisosPorRol, 0);
});
// Autochequeo de permisos según el rol seleccionado
function autoCheckPermisosPorRol() {
  // Limpiar todos los permisos
  const checks = form.querySelectorAll('input[type="checkbox"]');
  checks.forEach(chk => { chk.checked = false; });

  const rol = inputRol.value.trim().toUpperCase();
  if (rol === 'ADMINISTRADOR') {
    // Marcar todos los permisos si es ADMINISTRADOR
    checks.forEach(chk => { chk.checked = true; });
    return;
  }
  // Mapear los nombres de los checkboxes a los permisos de cada rol
  const permisosPorRol = {
    'PROVEEDORES': [
      'permComprobanteEgresosView',
      'permPerfilView'
    ],
    'PUBLICADOR': [
      'permImportarArchivosView',
      'permComprobanteEgresosView',
      'permHistorialView',
      'permPerfilView',
      'permUsuariosView',
      'permCrearUsuariosEdit',
      'permEditarUsuariosEdit',
      'permEliminarUsuariosEdit'
    ]
    // Puedes agregar más roles aquí si lo necesitas
  };

  const permisos = permisosPorRol[rol] || [];
  permisos.forEach(nombre => {
    const chk = form.querySelector(`[name="${nombre}"]`);
    if (chk) chk.checked = true;
  });

  // Mostrar/ocultar advertencia para publicador
  const warning = document.getElementById('publicador-user-warning');
  if (warning) {
    warning.style.display = rol === 'PUBLICADOR' ? '' : 'none';
  }
}

// Listener para cambio de rol
inputRol.addEventListener('change', autoCheckPermisosPorRol);

async function apiList() {
  const r = await fetch("/api/usuarios");
  if (!r.ok) throw new Error("No se pudo cargar usuarios");
  return r.json();
}

async function apiCreate(payload) {
  const r = await fetch("/api/usuarios", {
    method: "POST",
    headers: csrfHeaders(),               // ✅ CSRF
    body: JSON.stringify(payload)
  });

  const text = await r.text();            // ✅ para ver errores aunque no sean JSON
  if (!r.ok) {
    if (text && (text.includes("correo electrónico es obligatorio") || text.toLowerCase().includes("email") || text.toLowerCase().includes("correo"))) {
      throw new Error("<strong>El correo electrónico es obligatorio.</strong> Por favor ingresa un correo válido.");
    }
    if (text && (text.includes("Ya existe un usuario con ese código") || text.toLowerCase().includes("codigo") || text.toLowerCase().includes("código"))) {
      throw new Error("<strong>El código ya está en uso.</strong> Por favor ingresa un código único para el usuario.");
    }
    if (text && text.trim().startsWith("<")) {
      throw new Error("<strong>Error al crear usuario.</strong> Revisa los datos e inténtalo de nuevo.");
    }
    throw new Error(text || "Error creando");
  }
  // Si la respuesta parece JSON, parsea, si no, retorna objeto vacío
  try {
    return text && text.trim().startsWith('{') ? JSON.parse(text) : {};
  } catch (e) {
    return {};
  }
}

async function apiUpdate(codigo, payload) {
  const r = await fetch(`/api/usuarios/${encodeURIComponent(codigo)}`, {
    method: "PUT",
    headers: csrfHeaders(),               // ✅ CSRF
    body: JSON.stringify(payload)
  });

  const text = await r.text();
  if (!r.ok) throw new Error(text || "Error actualizando");
  return text ? JSON.parse(text) : {};
}

async function apiDelete(codigo) {
  const r = await fetch(`/api/usuarios/${encodeURIComponent(codigo)}`, {
    method: "DELETE",
    headers: csrfHeaders()                // ✅ CSRF (a veces lo exige también)
  });

  const text = await r.text();
  if (!r.ok) throw new Error(text || "Error eliminando");
  return text ? JSON.parse(text) : {};
}

async function apiToggleStatus(codigo) {
  const r = await fetch(`/api/usuarios/${encodeURIComponent(codigo)}/desactivar`, {
    method: "PUT",
    headers: csrfHeaders()
  });

  const text = await r.text();
  if (!r.ok) throw new Error(text || "Error cambiando estado");
  return text ? JSON.parse(text) : {};
}

/* Edit/Delete desde botones */
document.addEventListener("click", async (e) => {
  const editBtn = e.target.closest("[data-edit]");
  const delBtn = e.target.closest("[data-del]");
  const disableBtn = e.target.closest("[data-disable]");

  const edit = editBtn?.getAttribute("data-edit");
  const del = delBtn?.getAttribute("data-del");
  const disable = disableBtn?.getAttribute("data-disable");

  if (disable) {
    const estadoActual = (disableBtn.getAttribute("data-estado") || "activo").toLowerCase();
    const accion = estadoActual === "inactivo" ? "activar" : "inactivar";

    if (!confirm(`¿${accion.charAt(0).toUpperCase() + accion.slice(1)} usuario ${disable}?`)) return;

    try {
      await apiToggleStatus(disable);
      location.reload();
    } catch (err) {
      alert(err.message);
    }
  }

  if (edit) {
    try {
      const data = await apiList();
      const u = (data.users || []).find(x => x.codigo === edit);
      if (!u) {
        alert("Usuario no encontrado");
        return;
      }

      editMode = true;
      editingCodigo = u.codigo;
      openModal("Editar usuario");

      inputCodigo.value = u.codigo;
      inputCodigo.disabled = true;

      inputRol.value = (u.rol || "ADMINISTRADOR").trim();
      inputNombre.value = u.nombre_usuario || u.nombreUsuario || "";
      inputEmail.value = u.email || "";
      inputPasswordHash.value = ""; // no mostramos hash, solo se cambia si escriben nueva clave
      inputPasswordHash.required = false; // no requerido en edición

      // Marcar permisos: si el usuario tiene permisos personalizados, usarlos; si no, usar los del rol
      setTimeout(() => {
        const checks = form.querySelectorAll('input[type="checkbox"]');
        if (Array.isArray(u.permisos) && u.permisos.length > 0) {
          checks.forEach(chk => {
            chk.checked = u.permisos.includes(chk.name);
          });
        } else {
          autoCheckPermisosPorRol();
        }
      }, 0);
    } catch (error) {
      console.error("Error in edit handler:", error);
      alert("Error al cargar usuario: " + error.message);
    }
  }

  if (del) {
    if (!confirm(`¿Eliminar usuario ${del}?`)) return;
    try {
      await apiDelete(del);
      location.reload();
    } catch (err) {
      alert(err.message);
    }
  }
});

/* Submit del formulario */
form.addEventListener("submit", async (e) => {
  e.preventDefault();
  msg.textContent = "";

  const permisosSeleccionados = Array.from(
    form.querySelectorAll('input[type="checkbox"][name^="perm"]:checked')
  ).map((chk) => chk.name);

  // ✅ Payload adaptado a tu tabla/entidad
  const payload = {
    codigo: inputCodigo.value.trim(),
    rol: inputRol.value.trim(),
    nombre_usuario: inputNombre.value.trim(),
    email: inputEmail.value.trim(),
    password_hash: inputPasswordHash.value, // clave en texto plano (se hashea en el service)
    permisos: permisosSeleccionados
  };

  try {
    if (!payload.codigo) throw new Error("Código obligatorio");
    if (!payload.rol) throw new Error("Rol obligatorio");
    if (!payload.nombre_usuario) throw new Error("Nombre obligatorio");

    if (editMode) {
      // ✅ si NO escribió clave nueva, no la mandamos (para no romper update)
      if (!payload.password_hash || payload.password_hash.trim() === "") {
        delete payload.password_hash;
      }
      await apiUpdate(editingCodigo, payload);
    } else {
      if (!payload.password_hash || payload.password_hash.trim() === "") {
        throw new Error("Clave obligatoria al crear");
      }
      await apiCreate(payload);
    }

    closeModal();
    location.reload();
  } catch (err) {
    msg.innerHTML = err.message;
    msg.classList.add("active");
  }
});

/* ===== USER KEBAB MENUS ===== */

document.addEventListener("click", function(e){

  const kebabBtn = e.target.closest(".kebab-btn");

  if(kebabBtn){

    const menu = kebabBtn.nextElementSibling;

    document
      .querySelectorAll(".kebab-dropdown")
      .forEach(m => {
        if(m !== menu) m.classList.remove("active");
      });

    menu.classList.toggle("active");

    return;
  }

  /* cerrar menus si clic afuera */

  if(!e.target.closest(".user-menu")){
    document
      .querySelectorAll(".kebab-dropdown")
      .forEach(m => m.classList.remove("active"));
  }

  function toggleSidebar(){

const sidebar = document.getElementById("sidebar");
const overlay = document.getElementById("sidebarOverlay");

sidebar.classList.toggle("open");
overlay.classList.toggle("active");

}

});

/* ===== USERS FILTER TABS ===== */

function initUserFilters() {
  const tabs = Array.from(document.querySelectorAll(".users-tab"));
  if (!tabs.length) return;

  const items = Array.from(document.querySelectorAll(".user-item"));

  const allCountEl = document.getElementById("filterAllCount");
  const activeCountEl = document.getElementById("filterActiveCount");
  const inactiveCountEl = document.getElementById("filterInactiveCount");

  const isInactive = (item) => item.classList.contains("is-disabled");
  const isActive = (item) => !item.classList.contains("is-disabled");

  const updateCounts = () => {
    const allCount = items.length;
    const activeCount = items.filter(isActive).length;
    const inactiveCount = items.filter(isInactive).length;

    if (allCountEl) allCountEl.textContent = String(allCount);
    if (activeCountEl) activeCountEl.textContent = String(activeCount);
    if (inactiveCountEl) inactiveCountEl.textContent = String(inactiveCount);
  };

  const applyFilter = (filter) => {
    items.forEach((item) => {
      let shouldShow = true;
      if (filter === "inactive") shouldShow = isInactive(item);
      else if (filter === "active") shouldShow = isActive(item);
      // "all" muestra todos
      item.classList.toggle("hidden-by-filter", !shouldShow);
    });

    tabs.forEach((tab) => {
      const isActiveTab = tab.dataset.userFilter === filter;
      tab.classList.toggle("is-active", isActiveTab);
      tab.setAttribute("aria-selected", isActiveTab ? "true" : "false");
    });
  };

  tabs.forEach((tab) => {
    tab.addEventListener("click", () => {
      applyFilter(tab.dataset.userFilter || "all");
    });
  });

  updateCounts();
  applyFilter("active");
}

initUserFilters();
//prueba de commit