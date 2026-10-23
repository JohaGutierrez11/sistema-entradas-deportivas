import { useEffect, useState } from 'react';
import { matchApi } from '../../api/matchApi';
import MatchCard from '../../components/MatchCard';

export default function MatchesPage() {
  const [date, setDate] = useState('');
  const [name, setName] = useState('');
  const [filters, setFilters] = useState({});   // los filtros que se aplicaron con "Buscar"
  const [matches, setMatches] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    let current = true;   // si llega una búsqueda más nueva, esta respuesta se ignora (race condition)
    setLoading(true);
    setError(null);
    matchApi.searchMatches(filters)
      .then((data) => { if (current) setMatches(data); })
      .catch((err) => { if (current) setError(err.message); })
      .finally(() => { if (current) setLoading(false); });
    return () => { current = false; };
  }, [filters]);

  const nameTooShort = name.trim().length === 1;   // el backend exige mínimo 2 letras

  function handleSearch(event) {
    event.preventDefault();
    if (nameTooShort) return;
    setFilters({ date, name: name.trim() });
  }

  function handleClear() {
    setDate('');
    setName('');
    setFilters({});
  }

  const hasFilters = Boolean(filters.date || filters.name);

  return (
    <section>
      <div className="page-header">
        <h1>{hasFilters ? 'Resultados de la búsqueda' : 'Próximos partidos'}</h1>
      </div>

      <form className="card filters" onSubmit={handleSearch} role="search">
        <div className="filter-field">
          <label htmlFor="date">Fecha</label>
          <input id="date" type="date" value={date} onChange={(e) => setDate(e.target.value)} />
        </div>
        <div className="filter-field filter-grow">
          <label htmlFor="name">Equipo o partido</label>
          <input id="name" placeholder="Ej. Barcelona" value={name} onChange={(e) => setName(e.target.value)}
                 aria-invalid={nameTooShort ? 'true' : undefined} />
          {nameTooShort && <small className="field-error">Escribe al menos 2 letras</small>}
        </div>
        <div className="filter-actions">
          <button type="submit" className="btn btn-solid">Buscar</button>
          {hasFilters && <button type="button" className="btn" onClick={handleClear}>Limpiar</button>}
        </div>
      </form>

      {error && <p className="alert alert-error" role="alert">{error}</p>}

      {loading ? (
        <p className="muted">Cargando partidos…</p>
      ) : !error && matches.length === 0 ? (
        <div className="card empty-state">
          <p><strong>No hay partidos {hasFilters ? 'que coincidan con tu búsqueda' : 'programados'}.</strong></p>
          {hasFilters && <p className="muted">Prueba con otra fecha o con el nombre de un equipo.</p>}
        </div>
      ) : (
        <div className="match-grid">
          {matches.map((match) => <MatchCard key={match.id} match={match} />)}
        </div>
      )}
    </section>
  );
}