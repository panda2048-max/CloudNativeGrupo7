# TimeToGame - Frontend

SPA en React (Vite) que consume la API REST del backend `timetogame-backend`.

## Requisitos

- Node.js 18+ y npm
- Backend corriendo (por defecto en `http://localhost:8000`)

## Configuracion

La URL del backend se lee de una variable de entorno, nunca esta hardcodeada
en el codigo. Copia `.env.example` a `.env` y ajusta el valor si es necesario:

```
VITE_API_BASE_URL=http://localhost:8000/api
```

Para apuntar a otro entorno (backend local en otro puerto, o un API
Manager/Gateway), solo hace falta cambiar esta variable y reiniciar `npm run dev`
(o reconstruir con `npm run build`). No requiere tocar codigo fuente.

## Uso

```bash
npm install
npm run dev
```

La app queda disponible en `http://localhost:5173` (puerto configurado en
`vite.config.js`, y el mismo que el backend tiene habilitado en CORS).

## Usuarios de prueba (definidos en el backend)

| Usuario | Password  | Rol             |
|---------|-----------|------------------|
| user    | user123   | USER             |
| admin   | admin123  | USER + ADMIN     |

## Estructura

- `src/api`: cliente HTTP y funciones por recurso (`authApi`, `videojuegosApi`, `generosApi`).
- `src/auth`: contexto de autenticacion (JWT en `localStorage`, decodificacion de rol/usuario).
- `src/components`: navegacion, rutas protegidas, mensajes de estado.
- `src/pages`: vistas (inicio, catalogo publico, detalle, login, panel autenticado, administracion).
