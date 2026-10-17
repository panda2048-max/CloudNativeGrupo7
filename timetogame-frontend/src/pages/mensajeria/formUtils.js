/** Quita campos vacios y convierte los numericos, para no mandar "" al backend. */
export function limpiarPayload(form, camposNumericos = []) {
  return Object.fromEntries(
    Object.entries(form)
      .filter(([, valor]) => valor !== "" && valor !== null && valor !== undefined)
      .map(([clave, valor]) => [clave, camposNumericos.includes(clave) ? Number(valor) : valor])
  );
}
