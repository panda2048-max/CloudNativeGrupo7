import { useCallback, useEffect, useState } from "react";
import { fetchBindings, fetchColas, fetchExchanges } from "../api/rabbitAdminApi";
import { LoadingIndicator } from "../components/LoadingIndicator";
import { StatusMessage } from "../components/StatusMessage";
import { BindingsSection } from "./mensajeria/BindingsSection";
import { ColasSection } from "./mensajeria/ColasSection";
import { ExchangesSection } from "./mensajeria/ExchangesSection";
import { MensajesFallidosSection } from "./mensajeria/MensajesFallidosSection";

export function MensajeriaPage() {
  const [topologia, setTopologia] = useState({ colas: [], exchanges: [], bindings: [] });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const recargar = useCallback(() => {
    setError(null);
    return Promise.all([fetchColas(), fetchExchanges(), fetchBindings()])
      .then(([colas, exchanges, bindings]) => setTopologia({ colas, exchanges, bindings }))
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    recargar();
  }, [recargar]);

  return (
    <section className="page">
      <div className="section-header">
        <h1>Mensajeria (RabbitMQ)</h1>
        <button type="button" onClick={recargar}>
          Actualizar
        </button>
      </div>
      <p>
        Administracion de colas, exchanges y bindings a traves del microservicio
        <code> timetogame-rabbitmq-admin</code>. Los recursos de la topologia del
        sistema (<code>timetogame.catalogo.*</code>, <code>timetogame.documentos.*</code>,{" "}
        <code>timetogame.admin.*</code>)
        y los reservados del broker (<code>amq.*</code>) estan protegidos contra
        eliminacion.
      </p>

      {loading && <LoadingIndicator label="Cargando topologia..." />}
      <StatusMessage variant="error">{error}</StatusMessage>

      {!loading && !error && (
        <>
          <ExchangesSection exchanges={topologia.exchanges} onCambio={recargar} />
          <ColasSection colas={topologia.colas} exchanges={topologia.exchanges} onCambio={recargar} />
          <BindingsSection
            bindings={topologia.bindings}
            exchanges={topologia.exchanges}
            colas={topologia.colas}
            onCambio={recargar}
          />
        </>
      )}

      <MensajesFallidosSection />
    </section>
  );
}
