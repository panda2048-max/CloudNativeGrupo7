import { useCallback, useEffect, useState } from "react";
import { fetchBindings, fetchColas, fetchExchanges } from "../api/rabbitAdminApi";
import { LoadingIndicator } from "../components/LoadingIndicator";
import { StatusMessage } from "../components/StatusMessage";
import { BindingsSection } from "./mensajeria/BindingsSection";
import { ColasSection } from "./mensajeria/ColasSection";
import { ExchangesSection } from "./mensajeria/ExchangesSection";
import { MensajesFallidosSection } from "./mensajeria/MensajesFallidosSection";
import { KafkaClusterSection } from "./mensajeria/kafka/KafkaClusterSection";
import { KafkaGruposSection } from "./mensajeria/kafka/KafkaGruposSection";
import { KafkaTopicosSection } from "./mensajeria/kafka/KafkaTopicosSection";

const PESTANAS = [
  { id: "rabbitmq", titulo: "RabbitMQ" },
  { id: "kafka", titulo: "Kafka" },
];

export function MensajeriaPage() {
  const [pestana, setPestana] = useState("rabbitmq");

  return (
    <section className="page">
      <h1>Mensajeria</h1>
      <div className="tabs" role="tablist">
        {PESTANAS.map((p) => (
          <button
            key={p.id}
            type="button"
            role="tab"
            aria-selected={pestana === p.id}
            className={pestana === p.id ? "tabs__item tabs__item--activo" : "tabs__item"}
            onClick={() => setPestana(p.id)}
          >
            {p.titulo}
          </button>
        ))}
      </div>

      {pestana === "rabbitmq" ? <RabbitMqPanel /> : <KafkaPanel />}
    </section>
  );
}

function RabbitMqPanel() {
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
    <>
      <div className="section-header">
        <p>
          Administracion de colas, exchanges y bindings a traves del microservicio
          <code> timetogame-rabbitmq-admin</code>. Los recursos de la topologia del
          sistema (<code>timetogame.catalogo.*</code>, <code>timetogame.documentos.*</code>,{" "}
          <code>timetogame.admin.*</code>)
          y los reservados del broker (<code>amq.*</code>) estan protegidos contra
          eliminacion.
        </p>
        <button type="button" onClick={recargar}>
          Actualizar
        </button>
      </div>

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
    </>
  );
}

function KafkaPanel() {
  return (
    <>
      <p>
        Topicos y consumer groups del cluster Kafka, administrados con <code>AdminClient</code> desde el
        mismo microservicio. Los topicos <code>timetogame.*</code> y los internos (<code>__*</code>) estan
        protegidos contra eliminacion.
      </p>
      <KafkaClusterSection />
      <KafkaGruposSection />
      <KafkaTopicosSection />
    </>
  );
}
