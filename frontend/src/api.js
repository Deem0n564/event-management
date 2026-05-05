const API_BASE = toApiBase(import.meta.env.VITE_API_BASE_URL);

function toApiBase(value) {
  const base = (value || '').replace(/\/+$/, '');

  if (!base) {
    return '/api';
  }

  return base.endsWith('/api') ? base : `${base}/api`;
}

function toQuery(params) {
  const search = new URLSearchParams();

  Object.entries(params || {}).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') {
      search.set(key, value);
    }
  });

  const query = search.toString();
  return query ? `?${query}` : '';
}

async function request(path, options = {}) {
  const headers = {
    Accept: 'application/json',
    ...options.headers
  };

  if (options.body && !headers['Content-Type']) {
    headers['Content-Type'] = 'application/json';
  }

  const response = await fetch(`${API_BASE}${path}`, {
    ...options,
    headers
  });

  if (response.status === 204) {
    return null;
  }

  const text = await response.text();
  const data = text ? parseBody(text) : null;

  if (!response.ok) {
    const message = data?.message || data?.error || text || `HTTP ${response.status}`;
    const error = new Error(message);
    error.status = response.status;
    error.payload = data;
    throw error;
  }

  return data;
}

function parseBody(text) {
  try {
    return JSON.parse(text);
  } catch {
    return text;
  }
}

function json(method, body) {
  return {
    method,
    body: JSON.stringify(body)
  };
}

export function unwrapPage(payload) {
  if (Array.isArray(payload)) {
    return payload;
  }

  return payload?.content || [];
}

export function formatApiError(error) {
  const fieldErrors = error?.payload?.errors;
  if (fieldErrors) {
    return Object.entries(fieldErrors)
      .map(([field, messages]) => `${field}: ${messages.join(', ')}`)
      .join('\n');
  }

  return error?.message || 'Request failed';
}

export const api = {
  events: {
    list: (filters = {}) => request(`/events${toQuery(filters)}`),
    create: (payload) => request('/events', json('POST', payload)),
    update: (id, payload) => request(`/events/${id}`, json('PUT', payload)),
    remove: (id) => request(`/events/${id}`, { method: 'DELETE' })
  },
  sessions: {
    list: () => request('/sessions/with-speakers'),
    search: (filters = {}) => request(`/sessions/search-native${toQuery(filters)}`),
    create: (payload) => request('/sessions', json('POST', payload)),
    update: (id, payload) => request(`/sessions/${id}`, json('PUT', payload)),
    remove: (id) => request(`/sessions/${id}`, { method: 'DELETE' })
  },
  speakers: {
    list: () => request('/speakers'),
    create: (payload) => request('/speakers', json('POST', payload)),
    update: (id, payload) => request(`/speakers/${id}`, json('PUT', payload)),
    remove: (id) => request(`/speakers/${id}`, { method: 'DELETE' })
  },
  attendees: {
    list: () => request('/attendees'),
    create: (payload) => request('/attendees', json('POST', payload)),
    bulk: (payload) => request('/attendees/bulk', json('POST', payload)),
    update: (id, payload) => request(`/attendees/${id}`, json('PUT', payload)),
    remove: (id) => request(`/attendees/${id}`, { method: 'DELETE' })
  },
  tickets: {
    list: () => request('/tickets/with-details'),
    create: (payload) => request('/tickets', json('POST', payload)),
    update: (id, payload) => request(`/tickets/${id}`, json('PUT', payload)),
    remove: (id) => request(`/tickets/${id}`, { method: 'DELETE' })
  }
};
