import { useEffect, useMemo, useState } from 'react';
import { api, formatApiError, unwrapPage } from './api.js';

const emptyEvent = { name: '', date: '', location: '' };
const emptySession = { title: '', description: '', eventId: '', speakerIds: [] };
const emptySpeaker = { firstName: '', lastName: '', bio: '' };
const emptyAttendee = { name: '', email: '' };
const emptyTicket = { type: 'STANDARD', price: '', purchaseDate: '', attendeeId: '', sessionId: '' };
const ticketTypes = ['STANDARD', 'VIP', 'STUDENT'];
const defaultSessionPage = { number: 0, size: 5, totalPages: 0, totalElements: 0, first: true, last: true };
const defaultLocalPages = {
  events: { number: 0, size: 3 },
  eventSessions: { number: 0, size: 5 },
  speakers: { number: 0, size: 5 },
  attendees: { number: 0, size: 5 },
  tickets: { number: 0, size: 5 }
};

const views = [
  { id: 'events', label: 'Events' },
  { id: 'schedule', label: 'Schedule' },
  { id: 'speakers', label: 'Speakers' },
  { id: 'registration', label: 'Registration' },
  { id: 'tickets', label: 'Tickets' }
];

export default function App() {
  const [activeView, setActiveView] = useState('events');
  const [events, setEvents] = useState([]);
  const [sessions, setSessions] = useState([]);
  const [scheduleSessions, setScheduleSessions] = useState([]);
  const [speakers, setSpeakers] = useState([]);
  const [attendees, setAttendees] = useState([]);
  const [tickets, setTickets] = useState([]);
  const [selectedEventId, setSelectedEventId] = useState('');
  const [eventFilter, setEventFilter] = useState({ name: '', date: '' });
  const [sessionFilter, setSessionFilter] = useState({ title: '', speakerFirstName: '' });
  const [sessionPage, setSessionPage] = useState(defaultSessionPage);
  const [localPages, setLocalPages] = useState(defaultLocalPages);
  const [eventForm, setEventForm] = useState(emptyEvent);
  const [sessionForm, setSessionForm] = useState(emptySession);
  const [speakerForm, setSpeakerForm] = useState(emptySpeaker);
  const [attendeeForm, setAttendeeForm] = useState(emptyAttendee);
  const [ticketForm, setTicketForm] = useState(emptyTicket);
  const [bulkText, setBulkText] = useState('');
  const [editing, setEditing] = useState({ type: '', id: null });
  const [openComposer, setOpenComposer] = useState('');
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [notice, setNotice] = useState('');
  const [error, setError] = useState('');

  useEffect(() => {
    loadWorkspace();
  }, []);

  useEffect(() => {
    function handleEscape(event) {
      if (event.key !== 'Escape') {
        return;
      }

      if (openComposer || editing.type) {
        closeComposer();
        return;
      }

      if (activeView === 'events') {
        setSelectedEventId('');
        setLocalPages((current) => ({
          ...current,
          eventSessions: { ...current.eventSessions, number: 0 }
        }));
      }
    }

    window.addEventListener('keydown', handleEscape);
    return () => window.removeEventListener('keydown', handleEscape);
  }, [activeView, editing.type, openComposer]);

  const selectedEvent = useMemo(
    () => events.find((event) => String(event.id) === String(selectedEventId)) || null,
    [events, selectedEventId]
  );

  const selectedEventSessions = useMemo(() => {
    if (!selectedEvent) {
      return [];
    }

    return sortSessionsByEventDate(sessions.filter((session) => String(session.event?.id) === String(selectedEvent.id)));
  }, [selectedEvent, sessions]);

  const eventStats = useMemo(() => {
    return events.map((event) => {
      const eventSessions = sessions.filter((session) => String(session.event?.id) === String(event.id));
      const speakerIds = new Set(eventSessions.flatMap((session) => (session.speakers || []).map((speaker) => speaker.id)));
      return {
        eventId: event.id,
        sessions: eventSessions.length,
        speakers: speakerIds.size
      };
    });
  }, [events, sessions]);

  async function loadWorkspace() {
    setLoading(true);
    setError('');

    try {
      const [eventData, sessionData, sessionPageData, speakerData, attendeeData, ticketData] = await Promise.all([
        api.events.list(eventFilter),
        api.sessions.list(),
        api.sessions.search(buildSessionSearchParams(sessionFilter, sessionPage.number, sessionPage.size)),
        api.speakers.list(),
        api.attendees.list(),
        api.tickets.list()
      ]);

      setEvents(sortEventsByDate(eventData || []));
      setSessions(sortSessionsByEventDate(sessionData || []));
      setScheduleSessions(sortSessionsByEventDate(unwrapPage(sessionPageData)));
      setSessionPage(readPageInfo(sessionPageData, sessionPage));
      setSpeakers(speakerData || []);
      setAttendees(attendeeData || []);
      setTickets(ticketData || []);
      setSelectedEventId((current) => ((eventData || []).some((item) => String(item.id) === String(current)) ? current : ''));
    } catch (requestError) {
      setError(formatApiError(requestError));
    } finally {
      setLoading(false);
    }
  }

  async function refreshEvents(filters = eventFilter) {
    const result = await api.events.list(filters);
    const sortedEvents = sortEventsByDate(result || []);
    setEvents(sortedEvents);
    setSelectedEventId((current) => (sortedEvents.some((event) => String(event.id) === String(current)) ? current : ''));
  }

  async function refreshSessions(filters = sessionFilter, page = 0, size = sessionPage.size) {
    const result = await api.sessions.search(buildSessionSearchParams(filters, page, size));
    setScheduleSessions(sortSessionsByEventDate(unwrapPage(result)));
    setSessionPage(readPageInfo(result, { ...sessionPage, number: page, size }));
  }

  function setLocalPage(resource, patch) {
    setLocalPages((current) => ({
      ...current,
      [resource]: {
        ...current[resource],
        ...patch
      }
    }));
  }

  function handleLocalPageChange(resource, page) {
    setLocalPage(resource, { number: page });
  }

  function handleLocalPageSizeChange(resource, size) {
    setLocalPage(resource, { number: 0, size });
  }

  async function runAction(action, successMessage) {
    setBusy(true);
    setError('');
    setNotice('');

    try {
      await action();
      if (successMessage) {
        setNotice(successMessage);
      }
    } catch (requestError) {
      setError(formatApiError(requestError));
    } finally {
      setBusy(false);
    }
  }

  function statFor(eventId) {
    return eventStats.find((stat) => String(stat.eventId) === String(eventId)) || { sessions: 0, speakers: 0 };
  }

  async function handleEventFilter(event) {
    event.preventDefault();
    await runAction(async () => {
      await refreshEvents(eventFilter);
      setLocalPage('events', { number: 0 });
    }, 'Event board refreshed.');
  }

  async function handleSessionFilter(event) {
    event.preventDefault();
    await runAction(() => refreshSessions(sessionFilter, 0, sessionPage.size), 'Schedule search refreshed.');
  }

  async function handleSessionPageChange(page) {
    await runAction(() => refreshSessions(sessionFilter, page, sessionPage.size), '');
  }

  async function handleSessionPageSizeChange(size) {
    await runAction(() => refreshSessions(sessionFilter, 0, size), '');
  }

  async function handleEventSubmit(event) {
    event.preventDefault();

    await runAction(async () => {
      if (editing.type === 'event') {
        await api.events.update(editing.id, eventForm);
      } else {
        await api.events.create(eventForm);
      }
      setEventForm(emptyEvent);
      setEditing({ type: '', id: null });
      setOpenComposer('');
      await refreshEvents();
    }, editing.type === 'event' ? 'Event updated.' : 'Event created.');
  }

  async function handleSessionSubmit(event) {
    event.preventDefault();
    const payload = {
      ...sessionForm,
      eventId: Number(sessionForm.eventId),
      speakerIds: sessionForm.speakerIds.map(Number)
    };

    await runAction(async () => {
      if (editing.type === 'session') {
        await api.sessions.update(editing.id, payload);
      } else {
        await api.sessions.create(payload);
      }
      setSessionForm(emptySession);
      setEditing({ type: '', id: null });
      setOpenComposer('');
      await loadWorkspace();
    }, editing.type === 'session' ? 'Session updated.' : 'Session added to the program.');
  }

  async function handleSpeakerSubmit(event) {
    event.preventDefault();

    await runAction(async () => {
      if (editing.type === 'speaker') {
        await api.speakers.update(editing.id, speakerForm);
      } else {
        await api.speakers.create(speakerForm);
      }
      setSpeakerForm(emptySpeaker);
      setEditing({ type: '', id: null });
      setOpenComposer('');
      await loadWorkspace();
    }, editing.type === 'speaker' ? 'Speaker updated.' : 'Speaker added.');
  }

  async function handleAttendeeSubmit(event) {
    event.preventDefault();

    await runAction(async () => {
      if (editing.type === 'attendee') {
        await api.attendees.update(editing.id, attendeeForm);
      } else {
        await api.attendees.create(attendeeForm);
      }
      setAttendeeForm(emptyAttendee);
      setEditing({ type: '', id: null });
      setOpenComposer('');
      await loadWorkspace();
    }, editing.type === 'attendee' ? 'Attendee updated.' : 'Attendee registered.');
  }

  async function handleBulkAttendees(event) {
    event.preventDefault();
    const parsed = parseBulkAttendees(bulkText);

    if (!parsed.length) {
      setError('Add at least one attendee line before sending bulk registration.');
      return;
    }

    await runAction(async () => {
      await api.attendees.bulk(parsed);
      setBulkText('');
      setOpenComposer('');
      await loadWorkspace();
    }, `${parsed.length} attendees registered.`);
  }

  async function handleTicketSubmit(event) {
    event.preventDefault();
    const payload = {
      type: ticketForm.type,
      price: Number(ticketForm.price),
      attendeeId: Number(ticketForm.attendeeId),
      sessionId: Number(ticketForm.sessionId)
    };

    if (ticketForm.purchaseDate) {
      payload.purchaseDate = `${ticketForm.purchaseDate}T00:00:00`;
    }

    await runAction(async () => {
      if (editing.type === 'ticket') {
        await api.tickets.update(editing.id, payload);
      } else {
        await api.tickets.create(payload);
      }
      setTicketForm(emptyTicket);
      setEditing({ type: '', id: null });
      setOpenComposer('');
      await loadWorkspace();
    }, editing.type === 'ticket' ? 'Ticket updated.' : 'Ticket issued.');
  }

  async function removeResource(type, id) {
    const labels = {
      event: 'event',
      session: 'session',
      speaker: 'speaker',
      attendee: 'attendee',
      ticket: 'ticket'
    };

    if (!window.confirm(`Delete this ${labels[type]}?`)) {
      return;
    }

    await runAction(async () => {
      await api[`${type}s`].remove(id);
      await loadWorkspace();
    }, `${capitalize(type)} deleted.`);
  }

  function editEvent(event) {
    setActiveView('events');
    setOpenComposer('event');
    setEditing({ type: 'event', id: event.id });
    setEventForm({ name: event.name || '', date: event.date || '', location: event.location || '' });
  }

  function editSession(session) {
    setActiveView('schedule');
    setOpenComposer('session');
    setEditing({ type: 'session', id: session.id });
    setSessionForm({
      title: session.title || '',
      description: session.description || '',
      eventId: session.event?.id || '',
      speakerIds: (session.speakers || []).map((speaker) => String(speaker.id))
    });
  }

  function editSpeaker(speaker) {
    setActiveView('speakers');
    setOpenComposer('speaker');
    setEditing({ type: 'speaker', id: speaker.id });
    setSpeakerForm({ firstName: speaker.firstName || '', lastName: speaker.lastName || '', bio: speaker.bio || '' });
  }

  function editAttendee(attendee) {
    setActiveView('registration');
    setOpenComposer('attendee');
    setEditing({ type: 'attendee', id: attendee.id });
    setAttendeeForm({ name: attendee.name || '', email: attendee.email || '' });
  }

  function editTicket(ticket) {
    setActiveView('tickets');
    setOpenComposer('ticket');
    setEditing({ type: 'ticket', id: ticket.id });
    setTicketForm({
      type: ticket.type || 'STANDARD',
      price: ticket.price || '',
      purchaseDate: ticket.purchaseDate ? ticket.purchaseDate.slice(0, 10) : '',
      attendeeId: ticket.attendee?.id || '',
      sessionId: ticket.session?.id || ''
    });
  }

  function openCreateComposer(type) {
    setEditing({ type: '', id: null });

    if (type === 'event') {
      setEventForm(emptyEvent);
    }
    if (type === 'session') {
      setSessionForm(emptySession);
    }
    if (type === 'speaker') {
      setSpeakerForm(emptySpeaker);
    }
    if (type === 'attendee') {
      setAttendeeForm(emptyAttendee);
    }
    if (type === 'ticket') {
      setTicketForm(emptyTicket);
    }

    setOpenComposer(type);
  }

  function closeComposer() {
    setEditing({ type: '', id: null });
    setOpenComposer('');
    setEventForm(emptyEvent);
    setSessionForm(emptySession);
    setSpeakerForm(emptySpeaker);
    setAttendeeForm(emptyAttendee);
    setTicketForm(emptyTicket);
  }

  function cancelEdit() {
    closeComposer();
  }

  function renderTopbarActions() {
    if (activeView === 'events') {
      return <button className="primary-button heading-action-button" disabled={loading || busy} type="button" onClick={() => openCreateComposer('event')}>Create event</button>;
    }

    if (activeView === 'schedule') {
      return <button className="primary-button heading-action-button" disabled={loading || busy} type="button" onClick={() => openCreateComposer('session')}>Add session</button>;
    }

    if (activeView === 'speakers') {
      return <button className="primary-button heading-action-button" disabled={loading || busy} type="button" onClick={() => openCreateComposer('speaker')}>Add speaker</button>;
    }

    if (activeView === 'registration') {
      return (
        <>
          <button className="primary-button heading-action-button" disabled={loading || busy} type="button" onClick={() => openCreateComposer('attendee')}>Register attendee</button>
          <button className="ghost-button heading-action-button" disabled={loading || busy} type="button" onClick={() => openCreateComposer('bulk-attendees')}>Batch</button>
        </>
      );
    }

    if (activeView === 'tickets') {
      return <button className="primary-button heading-action-button" disabled={loading || busy} type="button" onClick={() => openCreateComposer('ticket')}>Issue ticket</button>;
    }

    return null;
  }

  return (
    <div className="app-shell">
      <aside className="side-rail">
        <div className="brand-mark">
          <span className="brand-symbol">EH</span>
          <div>
            <strong>Event Hub</strong>
            <small>Conference planner</small>
          </div>
        </div>

        <nav className="nav-list" aria-label="Main sections">
          {views.map((view) => (
            <button
              key={view.id}
              className={activeView === view.id ? 'nav-item active' : 'nav-item'}
              onClick={() => {
                setActiveView(view.id);
                closeComposer();
              }}
              type="button"
            >
              {view.label}
            </button>
          ))}
        </nav>

        {activeView === 'events' && (
          <div className="rail-card">
            <span className="eyebrow">Workspace</span>
            <strong>{selectedEvent?.name || 'No event selected'}</strong>
            <small>{selectedEvent ? `${formatDate(selectedEvent.date)} · ${selectedEvent.location}` : 'Create an event to begin'}</small>
          </div>
        )}
      </aside>

      <main className="main-stage">
        <header className="topbar">
          <div>
            <h1>{titleFor(activeView)}</h1>
          </div>
          <div className="topbar-actions">{renderTopbarActions()}</div>
        </header>

        {error && <div className="alert error">{error}</div>}
        {notice && <div className="alert success">{notice}</div>}

        {loading ? (
          <div className="loading-panel">Loading event workspace...</div>
        ) : (
          <>
            {activeView === 'events' && (
              <EventsView
                events={events}
                selectedEvent={selectedEvent}
                selectedEventSessions={selectedEventSessions}
                eventFilter={eventFilter}
                setEventFilter={setEventFilter}
                onFilter={handleEventFilter}
                eventForm={eventForm}
                setEventForm={setEventForm}
                onSubmit={handleEventSubmit}
                editing={editing}
                cancelEdit={cancelEdit}
                composerOpen={openComposer === 'event' || editing.type === 'event'}
                selectEvent={(id) => {
                  setSelectedEventId(id);
                  setLocalPage('eventSessions', { number: 0 });
                }}
                statFor={statFor}
                editEvent={editEvent}
                removeEvent={(id) => removeResource('event', id)}
                editSession={editSession}
                removeSession={(id) => removeResource('session', id)}
                pages={localPages}
                onPageChange={handleLocalPageChange}
                onPageSizeChange={handleLocalPageSizeChange}
                busy={busy}
              />
            )}

            {activeView === 'schedule' && (
              <ScheduleView
                sessions={scheduleSessions}
                events={events}
                speakers={speakers}
                filter={sessionFilter}
                setFilter={setSessionFilter}
                onFilter={handleSessionFilter}
                page={sessionPage}
                onPageChange={handleSessionPageChange}
                onPageSizeChange={handleSessionPageSizeChange}
                form={sessionForm}
                setForm={setSessionForm}
                onSubmit={handleSessionSubmit}
                editing={editing}
                cancelEdit={cancelEdit}
                composerOpen={openComposer === 'session' || editing.type === 'session'}
                editSession={editSession}
                removeSession={(id) => removeResource('session', id)}
                busy={busy}
              />
            )}

            {activeView === 'speakers' && (
              <SpeakersView
                speakers={speakers}
                sessions={sessions}
                form={speakerForm}
                setForm={setSpeakerForm}
                onSubmit={handleSpeakerSubmit}
                editing={editing}
                cancelEdit={cancelEdit}
                composerOpen={openComposer === 'speaker' || editing.type === 'speaker'}
                editSpeaker={editSpeaker}
                removeSpeaker={(id) => removeResource('speaker', id)}
                pageState={localPages.speakers}
                onPageChange={(page) => handleLocalPageChange('speakers', page)}
                onPageSizeChange={(size) => handleLocalPageSizeChange('speakers', size)}
                busy={busy}
              />
            )}

            {activeView === 'registration' && (
              <RegistrationView
                attendees={attendees}
                tickets={tickets}
                form={attendeeForm}
                setForm={setAttendeeForm}
                bulkText={bulkText}
                setBulkText={setBulkText}
                onSubmit={handleAttendeeSubmit}
                onBulk={handleBulkAttendees}
                editing={editing}
                cancelEdit={cancelEdit}
                attendeeComposerOpen={openComposer === 'attendee' || editing.type === 'attendee'}
                bulkComposerOpen={openComposer === 'bulk-attendees'}
                editAttendee={editAttendee}
                removeAttendee={(id) => removeResource('attendee', id)}
                pageState={localPages.attendees}
                onPageChange={(page) => handleLocalPageChange('attendees', page)}
                onPageSizeChange={(size) => handleLocalPageSizeChange('attendees', size)}
                busy={busy}
              />
            )}

            {activeView === 'tickets' && (
              <TicketsView
                tickets={tickets}
                attendees={attendees}
                sessions={sessions}
                form={ticketForm}
                setForm={setTicketForm}
                onSubmit={handleTicketSubmit}
                editing={editing}
                cancelEdit={cancelEdit}
                composerOpen={openComposer === 'ticket' || editing.type === 'ticket'}
                editTicket={editTicket}
                removeTicket={(id) => removeResource('ticket', id)}
                pageState={localPages.tickets}
                onPageChange={(page) => handleLocalPageChange('tickets', page)}
                onPageSizeChange={(size) => handleLocalPageSizeChange('tickets', size)}
                busy={busy}
              />
            )}
          </>
        )}
      </main>
    </div>
  );
}

function EventsView(props) {
  const {
    events,
    selectedEvent,
    selectedEventSessions,
    eventFilter,
    setEventFilter,
    onFilter,
    eventForm,
    setEventForm,
    onSubmit,
    editing,
    cancelEdit,
    composerOpen,
    selectEvent,
    statFor,
    editEvent,
    removeEvent,
    editSession,
    removeSession,
    pages,
    onPageChange,
    onPageSizeChange,
    busy
  } = props;
  const eventPage = paginateLocal(events, pages.events);

  return (
    <div className={composerOpen ? 'workspace-grid' : 'workspace-grid workspace-grid-full'}>
      {composerOpen && (
        <section className="control-stack">
          <Panel title={editing.type === 'event' ? 'Edit event' : 'Create event'} eyebrow="Event">
            <form className="stacked-form" onSubmit={onSubmit}>
              <label>
                Title
                <input required minLength="3" value={eventForm.name} onChange={(event) => setEventForm({ ...eventForm, name: event.target.value })} />
              </label>
              <label>
                Date
                <input required type="date" value={eventForm.date} onChange={(event) => setEventForm({ ...eventForm, date: event.target.value })} />
              </label>
              <label>
                Location
                <input required minLength="2" value={eventForm.location} onChange={(event) => setEventForm({ ...eventForm, location: event.target.value })} />
              </label>
              <div className="form-actions">
                <button className="primary-button" disabled={busy} type="submit">{editing.type === 'event' ? 'Save event' : 'Create event'}</button>
                <button className="ghost-button" type="button" onClick={cancelEdit}>Cancel</button>
              </div>
            </form>
          </Panel>
        </section>
      )}

      <section className="content-stack">
        <section className="event-board-panel">
          <form className="filter-bar compact-filter" onSubmit={onFilter}>
            <div>
              <strong>Find events</strong>
            </div>
            <label>
              <input
                aria-label="Event name"
                value={eventFilter.name}
                onChange={(event) => setEventFilter({ ...eventFilter, name: event.target.value })}
                placeholder="Java Day"
              />
            </label>
            <label>
              <input
                aria-label="Event date"
                type="date"
                value={eventFilter.date}
                onChange={(event) => setEventFilter({ ...eventFilter, date: event.target.value })}
              />
            </label>
            <button className="primary-button" disabled={busy} type="submit">Apply filter</button>
          </form>

          <div className="event-card-grid">
            {eventPage.items.map((event) => {
              const stat = statFor(event.id);
              const isSelected = String(selectedEvent?.id) === String(event.id);

              return (
                <article
                  className={isSelected ? 'event-card selected' : 'event-card'}
                  key={event.id}
                  tabIndex={0}
                  onClick={() => selectEvent(event.id)}
                  onKeyDown={(keyEvent) => {
                    if (keyEvent.target !== keyEvent.currentTarget) {
                      return;
                    }
                    if (keyEvent.key === 'Enter' || keyEvent.key === ' ') {
                      keyEvent.preventDefault();
                      selectEvent(event.id);
                    }
                  }}
                >
                  <div className="calendar-tile">
                    <span>{monthName(event.date)}</span>
                    <strong>{dayOfMonth(event.date)}</strong>
                  </div>
                  <div className="event-card-body">
                    <h3>{event.name}</h3>
                    <p>{event.location}</p>
                    <div className="mini-stats">
                      <span>{stat.sessions} sessions</span>
                      <span>{stat.speakers} speakers</span>
                    </div>
                  </div>
                  <div className="card-actions">
                    <button
                      type="button"
                      onClick={(clickEvent) => {
                        clickEvent.stopPropagation();
                        editEvent(event);
                      }}
                    >
                      Edit
                    </button>
                    <button
                      type="button"
                      onClick={(clickEvent) => {
                        clickEvent.stopPropagation();
                        removeEvent(event.id);
                      }}
                    >
                      Delete
                    </button>
                  </div>
                </article>
              );
            })}
          </div>

          <PaginationControls
            page={eventPage.page}
            busy={busy}
            itemLabel="events"
            onPageChange={(page) => onPageChange('events', page)}
            onPageSizeChange={(size) => onPageSizeChange('events', size)}
          />
        </section>

        <EventDetail
          event={selectedEvent}
          sessions={selectedEventSessions}
          pageState={pages.eventSessions}
          onPageChange={(page) => onPageChange('eventSessions', page)}
          onPageSizeChange={(size) => onPageSizeChange('eventSessions', size)}
          editSession={editSession}
          removeSession={removeSession}
          busy={busy}
        />
      </section>
    </div>
  );
}

function EventDetail({ event, sessions, pageState, onPageChange, onPageSizeChange, editSession, removeSession, busy }) {
  if (!event) {
    return <EmptyState title="No event selected" text="Select an event to view its program." />;
  }

  const sessionPage = paginateLocal(sessions, pageState);

  return (
    <section className="detail-panel">
      <div className="detail-hero">
        <div>
          <h2>{event.name}</h2>
          <p>{formatDate(event.date)} · {event.location}</p>
        </div>
        <div className="program-count">{sessions.length}</div>
      </div>

      <div className="timeline">
        {sessionPage.items.length ? sessionPage.items.map((session, index) => (
          <article className="timeline-row" key={session.id}>
            <div className="timeline-index">{String(sessionPage.page.number * sessionPage.page.size + index + 1).padStart(2, '0')}</div>
            <div className="timeline-content">
              <h3>{session.title}</h3>
              <p>{session.description || 'No description yet.'}</p>
              <SpeakerChips speakers={session.speakers} />
            </div>
            <div className="row-actions">
              <button type="button" onClick={() => editSession(session)}>Edit</button>
              <button type="button" onClick={() => removeSession(session.id)}>Delete</button>
            </div>
          </article>
        )) : <EmptyState title="No sessions planned" text="Use Schedule Builder to add sessions and attach speakers." />}
      </div>
      <div className="detail-pagination">
        <PaginationControls
          page={sessionPage.page}
          busy={busy}
          itemLabel="sessions"
          onPageChange={onPageChange}
          onPageSizeChange={onPageSizeChange}
        />
      </div>
    </section>
  );
}

function ScheduleView({
  sessions,
  events,
  speakers,
  filter,
  setFilter,
  onFilter,
  page,
  onPageChange,
  onPageSizeChange,
  form,
  setForm,
  onSubmit,
  editing,
  cancelEdit,
  composerOpen,
  editSession,
  removeSession,
  busy
}) {
  return (
    <div className={composerOpen ? 'workspace-grid' : 'workspace-grid workspace-grid-full'}>
      {composerOpen && (
        <section className="control-stack">
          <Panel title={editing.type === 'session' ? 'Edit session' : 'Add session'} eyebrow="Schedule">
            <form className="stacked-form" onSubmit={onSubmit}>
              <label>
                Title
                <input required minLength="3" value={form.title} onChange={(event) => setForm({ ...form, title: event.target.value })} />
              </label>
              <label>
                Description
                <textarea rows="4" value={form.description} onChange={(event) => setForm({ ...form, description: event.target.value })} />
              </label>
              <label>
                Event
                <select required value={form.eventId} onChange={(event) => setForm({ ...form, eventId: event.target.value })}>
                  <option value="">Choose event</option>
                  {events.map((event) => <option value={event.id} key={event.id}>{event.name}</option>)}
                </select>
              </label>
              <label>
                Speakers
                <select
                  multiple
                  value={form.speakerIds}
                  onChange={(event) => setForm({ ...form, speakerIds: Array.from(event.target.selectedOptions).map((option) => option.value) })}
                >
                  {speakers.map((speaker) => <option value={speaker.id} key={speaker.id}>{speakerName(speaker)}</option>)}
                </select>
              </label>
              <div className="form-actions">
                <button className="primary-button" disabled={busy} type="submit">{editing.type === 'session' ? 'Save session' : 'Add session'}</button>
                <button className="ghost-button" type="button" onClick={cancelEdit}>Cancel</button>
              </div>
            </form>
          </Panel>
        </section>
      )}

      <section className="content-stack">
        <form className="filter-bar" onSubmit={onFilter}>
          <div>
            <strong>Search sessions</strong>
          </div>
          <label>
            Title
            <input value={filter.title} onChange={(event) => setFilter({ ...filter, title: event.target.value })} placeholder="Spring" />
          </label>
          <label>
            Speaker first name
            <input value={filter.speakerFirstName} onChange={(event) => setFilter({ ...filter, speakerFirstName: event.target.value })} placeholder="Ivan" />
          </label>
          <button className="primary-button" disabled={busy} type="submit">Search program</button>
        </form>

        <div className="program-list">
          {sessions.map((session) => (
            <article className="program-card" key={session.id}>
              <div className="program-main">
                <span className="eyebrow">{session.event?.name || 'No event'}</span>
                <h3>{session.title}</h3>
                <p>{session.description || 'No description yet.'}</p>
                <SpeakerChips speakers={session.speakers} />
              </div>
              <div className="row-actions">
                <button type="button" onClick={() => editSession(session)}>Edit</button>
                <button type="button" onClick={() => removeSession(session.id)}>Delete</button>
              </div>
            </article>
          ))}
          {!sessions.length && <EmptyState title="No sessions found" text="Create a session or clear the filter." />}
        </div>

        <PaginationControls
          page={page}
          busy={busy}
          itemLabel="sessions"
          onPageChange={onPageChange}
          onPageSizeChange={onPageSizeChange}
        />
      </section>
    </div>
  );
}

function SpeakersView({
  speakers,
  sessions,
  form,
  setForm,
  onSubmit,
  editing,
  cancelEdit,
  composerOpen,
  editSpeaker,
  removeSpeaker,
  pageState,
  onPageChange,
  onPageSizeChange,
  busy
}) {
  const speakerPage = paginateLocal(speakers, pageState);

  return (
    <div className={composerOpen ? 'workspace-grid' : 'workspace-grid workspace-grid-full'}>
      {composerOpen && (
        <section className="control-stack">
          <Panel title={editing.type === 'speaker' ? 'Edit speaker' : 'Add speaker'} eyebrow="Directory">
            <form className="stacked-form" onSubmit={onSubmit}>
              <label>
                First name
                <input required minLength="2" value={form.firstName} onChange={(event) => setForm({ ...form, firstName: event.target.value })} />
              </label>
              <label>
                Last name
                <input required minLength="2" value={form.lastName} onChange={(event) => setForm({ ...form, lastName: event.target.value })} />
              </label>
              <label>
                Bio
                <textarea rows="5" value={form.bio} onChange={(event) => setForm({ ...form, bio: event.target.value })} />
              </label>
              <div className="form-actions">
                <button className="primary-button" disabled={busy} type="submit">{editing.type === 'speaker' ? 'Save speaker' : 'Add speaker'}</button>
                <button className="ghost-button" type="button" onClick={cancelEdit}>Cancel</button>
              </div>
            </form>
          </Panel>
        </section>
      )}

      <section className="content-stack">
        <div className="speaker-grid">
          {speakerPage.items.map((speaker) => {
            const speakerSessions = sessions.filter((session) => (session.speakers || []).some((item) => item.id === speaker.id));

            return (
              <article className="speaker-card" key={speaker.id}>
                <h3>{speakerName(speaker)}</h3>
                <p>{speaker.bio || 'Bio not provided.'}</p>
                <small>{speakerSessions.length} sessions assigned</small>
                <div className="speaker-session-list">
                  {speakerSessions.slice(0, 3).map((session) => <span key={session.id}>{session.title}</span>)}
                </div>
                <div className="card-actions inline">
                  <button type="button" onClick={() => editSpeaker(speaker)}>Edit</button>
                  <button type="button" onClick={() => removeSpeaker(speaker.id)}>Delete</button>
                </div>
              </article>
            );
          })}
          {!speakers.length && <EmptyState title="No speakers yet" text="Add speakers and attach them to sessions." />}
        </div>
        <PaginationControls
          page={speakerPage.page}
          busy={busy}
          itemLabel="speakers"
          onPageChange={onPageChange}
          onPageSizeChange={onPageSizeChange}
        />
      </section>
    </div>
  );
}

function RegistrationView({
  attendees,
  tickets,
  form,
  setForm,
  bulkText,
  setBulkText,
  onSubmit,
  onBulk,
  editing,
  cancelEdit,
  attendeeComposerOpen,
  bulkComposerOpen,
  editAttendee,
  removeAttendee,
  pageState,
  onPageChange,
  onPageSizeChange,
  busy
}) {
  const attendeePage = paginateLocal(attendees, pageState);
  const composerOpen = attendeeComposerOpen || bulkComposerOpen;

  return (
    <div className={composerOpen ? 'workspace-grid' : 'workspace-grid workspace-grid-full'}>
      {composerOpen && (
        <section className="control-stack">
          {attendeeComposerOpen && (
            <Panel title={editing.type === 'attendee' ? 'Edit attendee' : 'Register attendee'} eyebrow="Desk">
              <form className="stacked-form" onSubmit={onSubmit}>
                <label>
                  Name
                  <input required minLength="2" value={form.name} onChange={(event) => setForm({ ...form, name: event.target.value })} />
                </label>
                <label>
                  Email
                  <input required type="email" value={form.email} onChange={(event) => setForm({ ...form, email: event.target.value })} />
                </label>
                <div className="form-actions">
                  <button className="primary-button" disabled={busy} type="submit">{editing.type === 'attendee' ? 'Save attendee' : 'Register'}</button>
                  <button className="ghost-button" type="button" onClick={cancelEdit}>Cancel</button>
                </div>
              </form>
            </Panel>
          )}

          {bulkComposerOpen && (
            <Panel title="Bulk registration" eyebrow="Batch">
              <form className="stacked-form" onSubmit={onBulk}>
                <label>
                  One attendee per line
                  <textarea
                    rows="7"
                    value={bulkText}
                    onChange={(event) => setBulkText(event.target.value)}
                    placeholder={'Jane Doe, jane@example.com\nMax Smith <max@example.com>'}
                  />
                </label>
                <div className="form-actions">
                  <button className="primary-button" disabled={busy} type="submit">Register batch</button>
                  <button className="ghost-button" type="button" onClick={cancelEdit}>Cancel</button>
                </div>
              </form>
            </Panel>
          )}
        </section>
      )}

      <section className="content-stack">
        <div className="attendee-list">
          {attendeePage.items.map((attendee) => {
            const attendeeTickets = tickets.filter((ticket) => ticket.attendee?.id === attendee.id);

            return (
              <article className="attendee-card" key={attendee.id}>
                <div>
                  <h3>{attendee.name}</h3>
                  <p>{attendee.email}</p>
                </div>
                <div className="ticket-count">{attendeeTickets.length} tickets</div>
                <div className="row-actions">
                  <button type="button" onClick={() => editAttendee(attendee)}>Edit</button>
                  <button type="button" onClick={() => removeAttendee(attendee.id)}>Delete</button>
                </div>
              </article>
            );
          })}
          {!attendees.length && <EmptyState title="No attendees yet" text="Register attendees individually or use batch mode." />}
        </div>
        <PaginationControls
          page={attendeePage.page}
          busy={busy}
          itemLabel="attendees"
          onPageChange={onPageChange}
          onPageSizeChange={onPageSizeChange}
        />
      </section>
    </div>
  );
}

function TicketsView({
  tickets,
  attendees,
  sessions,
  form,
  setForm,
  onSubmit,
  editing,
  cancelEdit,
  composerOpen,
  editTicket,
  removeTicket,
  pageState,
  onPageChange,
  onPageSizeChange,
  busy
}) {
  const ticketPage = paginateLocal(tickets, pageState);

  return (
    <div className={composerOpen ? 'workspace-grid' : 'workspace-grid workspace-grid-full'}>
      {composerOpen && (
        <section className="control-stack">
          <Panel title={editing.type === 'ticket' ? 'Edit ticket' : 'Issue ticket'} eyebrow="Access">
            <form className="stacked-form" onSubmit={onSubmit}>
              <label>
                Type
                <select value={form.type} onChange={(event) => setForm({ ...form, type: event.target.value })}>
                  {ticketTypes.map((type) => <option value={type} key={type}>{type}</option>)}
                </select>
              </label>
              <label>
                Price
                <input required min="0.01" step="0.01" type="number" value={form.price} onChange={(event) => setForm({ ...form, price: event.target.value })} />
              </label>
              <label>
                Attendee
                <select required value={form.attendeeId} onChange={(event) => setForm({ ...form, attendeeId: event.target.value })}>
                  <option value="">Choose attendee</option>
                  {attendees.map((attendee) => <option value={attendee.id} key={attendee.id}>{attendee.name}</option>)}
                </select>
              </label>
              <label>
                Session
                <select required value={form.sessionId} onChange={(event) => setForm({ ...form, sessionId: event.target.value })}>
                  <option value="">Choose session</option>
                  {sessions.map((session) => <option value={session.id} key={session.id}>{session.title}</option>)}
                </select>
              </label>
              <label>
                Purchase date
                <input
                  type="text"
                  inputMode="numeric"
                  pattern="\\d{4}-\\d{2}-\\d{2}"
                  placeholder="YYYY-MM-DD"
                  value={form.purchaseDate}
                  onChange={(event) => setForm({ ...form, purchaseDate: event.target.value })}
                />
              </label>
              <div className="form-actions">
                <button className="primary-button" disabled={busy} type="submit">{editing.type === 'ticket' ? 'Save ticket' : 'Issue ticket'}</button>
                <button className="ghost-button" type="button" onClick={cancelEdit}>Cancel</button>
              </div>
            </form>
          </Panel>
        </section>
      )}

      <section className="content-stack">
        <div className="ticket-grid">
          {ticketPage.items.map((ticket) => (
            <article className="ticket-card" key={ticket.id}>
              <div className="ticket-type">{ticket.type}</div>
              <h3>{ticket.attendee?.name || 'Unknown attendee'}</h3>
              <p>{ticket.session?.title || 'No session selected'}</p>
              <small>{ticket.session?.event?.name || 'No event'} · {formatMoney(ticket.price)}</small>
              <SpeakerChips speakers={ticket.session?.speakers} />
              <div className="card-actions inline">
                <button type="button" onClick={() => editTicket(ticket)}>Edit</button>
                <button type="button" onClick={() => removeTicket(ticket.id)}>Delete</button>
              </div>
            </article>
          ))}
          {!tickets.length && <EmptyState title="No tickets issued" text="Create an attendee, add sessions, then issue the first ticket." />}
        </div>
        <PaginationControls
          page={ticketPage.page}
          busy={busy}
          itemLabel="tickets"
          onPageChange={onPageChange}
          onPageSizeChange={onPageSizeChange}
        />
      </section>
    </div>
  );
}

function Panel({ title, eyebrow, children }) {
  return (
    <section className="panel">
      <span className="eyebrow">{eyebrow}</span>
      <h2>{title}</h2>
      {children}
    </section>
  );
}

function SpeakerChips({ speakers = [] }) {
  if (!speakers?.length) {
    return <div className="chip-row"><span>No speakers</span></div>;
  }

  return (
    <div className="chip-row">
      {speakers.map((speaker) => <span key={speaker.id}>{speakerName(speaker)}</span>)}
    </div>
  );
}

function EmptyState({ title, text }) {
  return (
    <div className="empty-state">
      <strong>{title}</strong>
      <p>{text}</p>
    </div>
  );
}

function paginateLocal(items, state) {
  const size = state?.size || 5;
  const totalElements = items.length;
  const totalPages = totalElements ? Math.ceil(totalElements / size) : 0;
  const maxPage = Math.max(totalPages - 1, 0);
  const number = Math.min(Math.max(state?.number || 0, 0), maxPage);
  const start = number * size;

  return {
    items: items.slice(start, start + size),
    page: {
      number,
      size,
      totalPages,
      totalElements,
      first: number === 0,
      last: totalPages === 0 || number >= totalPages - 1
    }
  };
}

function PaginationControls({ page, busy, itemLabel = 'items', onPageChange, onPageSizeChange }) {
  const currentPage = page.totalPages ? page.number + 1 : 0;

  return (
    <div className="pagination-bar">
      <div>
        <strong>Page {currentPage} of {page.totalPages}</strong>
        <small>{page.totalElements} total {itemLabel}</small>
      </div>

      <label>
        Page size
        <select
          value={page.size}
          onChange={(event) => onPageSizeChange(Number(event.target.value))}
          disabled={busy}
        >
          {[3, 5, 6, 10, 20].map((size) => <option value={size} key={size}>{size}</option>)}
        </select>
      </label>

      <div className="pagination-actions">
        <button
          type="button"
          disabled={busy || page.first || page.totalPages === 0}
          onClick={() => onPageChange(page.number - 1)}
        >
          Previous
        </button>
        <button
          type="button"
          disabled={busy || page.last || page.totalPages === 0}
          onClick={() => onPageChange(page.number + 1)}
        >
          Next
        </button>
      </div>
    </div>
  );
}

function parseBulkAttendees(text) {
  return text
    .split(/\r?\n/)
    .map((line) => line.trim())
    .filter(Boolean)
    .map((line) => {
      const emailMatch = line.match(/[^\s,<>()]+@[^\s,<>()]+/);
      const email = emailMatch?.[0] || '';
      const name = line
        .replace(email, '')
        .replace(/[<>,;]/g, ' ')
        .replace(/\s+/g, ' ')
        .trim() || email.split('@')[0];

      return { name, email };
    })
    .filter((item) => item.name && item.email);
}

function buildSessionSearchParams(filters, page, size) {
  return {
    ...filters,
    page,
    size
  };
}

function readPageInfo(payload, fallback = defaultSessionPage) {
  if (!payload || Array.isArray(payload)) {
    return fallback;
  }

  return {
    number: payload.number ?? fallback.number,
    size: payload.size ?? fallback.size,
    totalPages: payload.totalPages ?? fallback.totalPages,
    totalElements: payload.totalElements ?? fallback.totalElements,
    first: payload.first ?? fallback.first,
    last: payload.last ?? fallback.last
  };
}

function sortEventsByDate(eventList) {
  return [...eventList].sort((first, second) => {
    const firstDate = first.date || '9999-12-31';
    const secondDate = second.date || '9999-12-31';
    return firstDate.localeCompare(secondDate) || (first.name || '').localeCompare(second.name || '');
  });
}

function sortSessionsByEventDate(sessionList) {
  return [...sessionList].sort((first, second) => {
    const firstDate = first.event?.date || '9999-12-31';
    const secondDate = second.event?.date || '9999-12-31';
    return firstDate.localeCompare(secondDate) || (first.title || '').localeCompare(second.title || '');
  });
}

function titleFor(view) {
  const titles = {
    events: 'Explore events',
    schedule: 'Schedule builder',
    speakers: 'Speaker directory',
    registration: 'Registration desk',
    tickets: 'Ticket office'
  };

  return titles[view];
}

function speakerName(speaker) {
  return `${speaker.firstName || ''} ${speaker.lastName || ''}`.trim() || 'Unnamed speaker';
}

function monthName(date) {
  if (!date) {
    return 'TBA';
  }

  return new Intl.DateTimeFormat('en', { month: 'short' }).format(new Date(`${date}T00:00:00`));
}

function dayOfMonth(date) {
  if (!date) {
    return '--';
  }

  return new Intl.DateTimeFormat('en', { day: '2-digit' }).format(new Date(`${date}T00:00:00`));
}

function formatDate(date) {
  if (!date) {
    return 'Date not set';
  }

  return new Intl.DateTimeFormat('en', { month: 'long', day: 'numeric', year: 'numeric' }).format(new Date(`${date}T00:00:00`));
}

function formatMoney(value) {
  const numeric = Number(value || 0);
  return new Intl.NumberFormat('en', { style: 'currency', currency: 'USD' }).format(numeric);
}

function capitalize(value) {
  return value.charAt(0).toUpperCase() + value.slice(1);
}
