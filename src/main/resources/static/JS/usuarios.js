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

/* Edit/Delete desde botones */
document.addEventListener("click", async (e) => {
  const edit = e.target.getAttribute("data-edit");
  const del = e.target.getAttribute("data-del");

  if (edit) {
    const data = await apiList();
    const u = (data.users || []).find(x => x.codigo === edit);
    if (!u) return;

    editMode = true;
    editingCodigo = u.codigo;
    openModal("Editar usuario");

    inputCodigo.value = u.codigo;
    inputCodigo.disabled = true;

    inputRol.value = (u.rol || "ADMINISTRADOR").trim();
    inputNombre.value = u.nombre_usuario || u.nombreUsuario || "";
    inputEmail.value = u.email || "";
    inputPasswordHash.value = ""; // ✅ no mostramos hash, solo se cambia si escriben nueva clave
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
