import { useEffect, useState } from 'react';
import { reservationApi } from '../api/reservationApi';

/**
 * Imagen del QR de un ticket. La imagen se pide con el token (no es pública),
 * así que no basta con <img src=...>: se descarga como Blob y se muestra con una URL temporal.
 */
export default function TicketQr({ ticketPublicId, label }) {
  const [url, setUrl] = useState(null);
  const [error, setError] = useState(false);

  useEffect(() => {
    let objectUrl = null;
    let current = true;
    reservationApi.getTicketQr(ticketPublicId)
      .then((blob) => {
        if (!current) return;
        objectUrl = URL.createObjectURL(blob);
        setUrl(objectUrl);
      })
      .catch(() => { if (current) setError(true); });
    return () => {
      current = false;
      if (objectUrl) URL.revokeObjectURL(objectUrl);   // libera la imagen de la memoria
    };
  }, [ticketPublicId]);

  if (error) return <p className="muted small">No se pudo cargar el QR.</p>;
  if (!url) return <div className="qr-placeholder" aria-label="Cargando QR" />;
  return <img src={url} alt={`Código QR de la entrada: ${label}`} className="qr-image" width="160" height="160" />;
}
