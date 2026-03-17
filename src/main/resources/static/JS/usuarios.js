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
  if (e.target.dataset.close) closeModal();
});

btnNuevo.addEventListener("click", () => {
  form.reset();
  editMode = false;
  editingCodigo = null;
  inputCodigo.disabled = false;
  inputPasswordHash.required = true; // requerido en creación
  openModal("Nuevo usuario");
});

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
  if (!r.ok) throw new Error(text || "Error creando");
  return text ? JSON.parse(text) : {};
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

  // ✅ Payload adaptado a tu tabla/entidad
  const payload = {
    codigo: inputCodigo.value.trim(),
    rol: inputRol.value.trim(),
    nombre_usuario: inputNombre.value.trim(),
    email: inputEmail.value.trim(),
    password_hash: inputPasswordHash.value // clave en texto plano (se hashea en el service)
  };

  try {
    if (!payload.codigo) throw new Error("Código obligatorio");
    if (!payload.rol) throw new Error("Rol obligatorio");

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
    msg.textContent = err.message;
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
  const inactiveCountEl = document.getElementById("filterInactiveCount");

  const isInactive = (item) => item.classList.contains("is-disabled");

  const updateCounts = () => {
    const allCount = items.length;
    const inactiveCount = items.filter(isInactive).length;

    if (allCountEl) allCountEl.textContent = String(allCount);
    if (inactiveCountEl) inactiveCountEl.textContent = String(inactiveCount);
  };

  const applyFilter = (filter) => {
    items.forEach((item) => {
      const shouldShow = filter === "all" ? true : isInactive(item);
      item.classList.toggle("hidden-by-filter", !shouldShow);
    });

    tabs.forEach((tab) => {
      const isActive = tab.dataset.userFilter === filter;
      tab.classList.toggle("is-active", isActive);
      tab.setAttribute("aria-selected", isActive ? "true" : "false");
    });
  };

  tabs.forEach((tab) => {
    tab.addEventListener("click", () => {
      applyFilter(tab.dataset.userFilter || "all");
    });
  });

  updateCounts();
  applyFilter("all");
}

initUserFilters();