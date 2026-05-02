# 📚 GlobalBiblion

Aplicación Android desarrollada en Kotlin que permite a los usuarios explorar libros, leerlos y escribir reseñas, integrando servicios de Firebase para la gestión de datos y autenticación.

---

## 🚀 Funcionalidades principales

* 🔐 **Autenticación de usuarios** con Firebase Authentication
* 📖 **Catálogo de libros** con información detallada
* ▶️ **Continuar leyendo** (persistencia del último libro leído)
* ⭐ **Sistema de reseñas** (una reseña por usuario por libro)
* 🧑 **Perfil de usuario** con información personal
* ☁️ **Carga de portadas y PDFs** desde Firebase Storage

---

## 🏗️ Arquitectura del proyecto

El proyecto está estructurado en varias actividades principales:

* `Menuprincipal` → Pantalla inicial con libros destacados
* `Biblioteca` → Catálogo completo
* `LibroSeleccionado` → Detalle del libro
* `ContinuarLeyendo` → Lectura del último libro guardado
* `EscribirResenia` → Creación y edición de reseñas
* `PerfilUsuario` → Información del usuario
* `BottomBar` → Clase base para navegación común

---

## 🔥 Firebase utilizado

* **Authentication** → gestión de usuarios
* **Firestore** → base de datos:

  * `users`
  * `books`
  * `reviews`
  * `lecturas`
* **Storage** → PDFs y portadas de libros

---

## 💾 Estructura de datos

### 📚 books

```json
{
  "title": "Nombre del libro",
  "authors": ["Autor"],
  "coverPath": "ruta/storage",
  "pdfPath": "ruta/pdf",
  "averageRating": 4.5
}
```

### ⭐ reviews

```json
{
  "userId": "uid",
  "userName": "Nombre Usuario",
  "rating": 5,
  "comment": "Texto",
  "updatedAt": "timestamp"
}
```

👉 Cada review usa como ID el UID del usuario:

```
books/{bookId}/reviews/{uid}
```

---

### 📖 lecturas

```json
{
  "idLibro": "bookId",
  "title": "Título",
  "authors": ["Autor"],
  "pdfPath": "ruta",
  "coverPath": "ruta",
  "updatedAt": "timestamp"
}
```

👉 Se guarda una sola lectura por usuario:

```
lecturas/{uid}
```

---

## 🧠 Conceptos clave implementados

* Uso de **Intent** para navegación entre Activities
* Persistencia de datos en Firebase
* Reutilización de UI con `<include>`
* Clase base (`BottomBar`) para navegación global
* Control de estado (evitar múltiples clicks con `navegando`)

---

## ⚠️ Problemas resueltos

* ❌ Duplicación de layouts → solucionado con `<include>`
* ❌ Múltiples reseñas por usuario → restringido por UID
* ❌ Error en navegación → centralizado en `BottomBar`
* ❌ Persistencia de lectura → implementada en Firestore

---

## 🛠️ Tecnologías utilizadas

* Kotlin
* Android SDK
* Firebase (Auth, Firestore, Storage)
* Glide (carga de imágenes)

---

## 📱 Requisitos

* Android Studio
* Dispositivo o emulador Android
* Cuenta Firebase configurada

---

## 👩‍💻 Autores

Proyecto desarrollado como parte de un trabajo académico.

---

## 📌 Mejoras futuras

* 📊 Sistema de ranking más avanzado
* 💬 Comentarios y respuestas en reseñas
* 🔍 Búsqueda avanzada
* 📥 Descarga offline de libros
* 🎤 Integración de micrófono

---

## 📄 Licencia

Uso académico.
