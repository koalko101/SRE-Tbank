import React, { useEffect, useRef, useState } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'

const API = import.meta.env.VITE_API_URL || '/api'
const PAGE_SIZE = Number(window.APP_CONFIG?.itemsPerPage) || 4

function token() {
  return localStorage.getItem('cinema-jwt')
}

async function request(path, options = {}) {
  const headers = new Headers(options.headers || {})
  const authToken = token()

  if (authToken) {
    headers.set('Authorization', `Bearer ${authToken}`)
  }
  if (options.body) {
    headers.set('Content-Type', 'application/json')
  }

  const response = await fetch(`${API}${path}`, {
    ...options,
    headers,
  })

  if (!response.ok) {
    const body = await response.json().catch(() => ({}))
    throw new Error(body.error || 'Не удалось выполнить запрос')
  }

  if (response.status === 204) {
    return null
  }
  return response.json()
}

function usePageData(loader, dependencies = []) {
  const [data, setData] = useState(null)
  const [error, setError] = useState('')

  useEffect(() => {
    let active = true
    setError('')
    loader()
      .then((result) => active && setData(result))
      .catch((reason) => active && setError(reason.message))

    return () => {
      active = false
    }
  }, dependencies)

  return { data, error }
}

function formatDate(value, options = {}) {
  const date = new Date(value)

  if (!value || Number.isNaN(date.getTime())) {
    return '—'
  }

  return new Intl.DateTimeFormat('ru-RU', options).format(date)
}

function formatTime(value) {
  return formatDate(value, { hour: '2-digit', minute: '2-digit' })
}

function localDateValue(value = new Date()) {
  const year = value.getFullYear()
  const month = String(value.getMonth() + 1).padStart(2, '0')
  const day = String(value.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

function earliestBirthDate() {
  const date = new Date()
  date.setFullYear(date.getFullYear() - 120)
  return localDateValue(date)
}

function movieDuration(minutes) {
  const hours = Math.floor(minutes / 60)
  const remainder = minutes % 60
  return remainder ? `${hours} ч ${remainder} мин` : `${hours} ч`
}

function navigate(path, replace = false) {
  if (replace) {
    window.history.replaceState({}, '', path)
  } else {
    window.history.pushState({}, '', path)
  }
  window.dispatchEvent(new PopStateEvent('popstate'))
  window.scrollTo(0, 0)
}

function App() {
  const [pathname, setPathname] = useState(window.location.pathname)
  const [user, setUser] = useState(() => {
    if (!localStorage.getItem('cinema-jwt')) {
      return null
    }
    try {
      return JSON.parse(localStorage.getItem('cinema-user') || 'null')
    } catch {
      return null
    }
  })

  useEffect(() => {
    const updatePath = () => setPathname(window.location.pathname)
    window.addEventListener('popstate', updatePath)
    return () => window.removeEventListener('popstate', updatePath)
  }, [])

  function signOut() {
    localStorage.removeItem('cinema-jwt')
    localStorage.removeItem('cinema-user')
    setUser(null)
    navigate('/movies')
  }

  const detailMatch = pathname.match(/^\/movies\/(\d+)$/)

  return (
    <>
      <Header user={user} onSignOut={signOut} />
      {pathname === '/login' || pathname === '/register' ? (
        <AuthPage mode={pathname.slice(1)} onAuthenticated={(nextUser, nextPath) => {
          setUser(nextUser)
          navigate(nextPath)
        }} />
      ) : pathname === '/schedule' ? (
        <SchedulePage />
      ) : pathname === '/cinemas' ? (
        <CinemasPage />
      ) : pathname === '/account' ? (
        user ? <AccountPage user={user} /> : <AuthRedirect />
      ) : pathname === '/checkout' ? (
        user ? <CheckoutPage user={user} /> : <AuthRedirect next="/checkout" />
      ) : pathname === '/admin' ? (
        user?.role === 'ADMIN' ? <AdminPage /> : <main className="page-section"><ErrorMessage message="Доступ разрешён только администраторам." /></main>
      ) : detailMatch ? (
        <MoviePage movieId={detailMatch[1]} />
      ) : (
        <CatalogPage />
      )}
      <Footer />
    </>
  )
}

function Header({ user, onSignOut }) {
  const links = [
    ['/movies', 'Фильмы'],
    ['/schedule', 'Расписание'],
    ['/cinemas', 'Кинотеатры'],
  ]

  return (
    <header className="site-header">
      <a className="brand" href="/movies">
        <span className="brand-symbol">К</span>
        <span>Кино<span className="brand-accent">Домой</span></span>
      </a>
      <nav className="main-nav" aria-label="Основная навигация">
        {links.map(([href, label]) => (
          <a key={href} href={href}>{label}</a>
        ))}
      </nav>
      <div className="header-account">
        {user ? (
          <>
            {user.role === 'ADMIN' && <a className="account-link" href="/admin">Управление</a>}
            <a className="account-link" href="/account">Мои билеты</a>
            <button className="quiet-button" onClick={onSignOut}>Выйти</button>
          </>
        ) : (
          <a className="account-link" href="/login">Войти</a>
        )}
      </div>
    </header>
  )
}

function AdminPage() {
  const [tab, setTab] = useState('movies')
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [options, setOptions] = useState({ movies: [], halls: [] })
  const [rows, setRows] = useState([])
  const [rowsLoading, setRowsLoading] = useState(true)
  const [rowsError, setRowsError] = useState(false)
  const [editing, setEditing] = useState(null)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [busy, setBusy] = useState(false)
  const rowsRequestId = useRef(0)

  async function refreshOptions() {
    const data = await request('/admin/options')
    setOptions({
      movies: Array.isArray(data.movies) ? data.movies : [],
      halls: Array.isArray(data.halls) ? data.halls : [],
    })
  }

  async function refreshRows(section = tab, pageNumber = page) {
    const requestId = ++rowsRequestId.current
    setRowsLoading(true)
    setRowsError(false)

    try {
      const data = await request(`/admin/${section}?page=${pageNumber}&size=20`)
      if (requestId !== rowsRequestId.current) {
        return
      }

      setRows(Array.isArray(data.content) ? data.content : [])
      setTotalPages(Number.isInteger(data.totalPages) ? data.totalPages : 0)
      setRowsError(false)
    } catch (reason) {
      if (requestId === rowsRequestId.current) {
        setRows([])
        setTotalPages(0)
        setRowsError(true)
        setError(reason.message)
      }
    } finally {
      if (requestId === rowsRequestId.current) {
        setRowsLoading(false)
      }
    }
  }

  useEffect(() => {
    refreshOptions().catch((reason) => setError(reason.message))
  }, [])

  useEffect(() => {
    setEditing(null)
    setError('')
    setRows([])
    setTotalPages(0)
    refreshRows(tab, page)
    return () => {
      rowsRequestId.current += 1
    }
  }, [tab, page])

  async function submit(event) {
    event.preventDefault()
    const formElement = event.currentTarget
    setBusy(true)
    setError('')
    setNotice('')
    const form = new FormData(event.currentTarget)
    let payload
    if (tab === 'movies') {
      payload = Object.fromEntries(form.entries())
      payload.durationMinutes = Number(payload.durationMinutes)
    } else {
      payload = {
        movieId: Number(form.get('movieId')),
        hallId: Number(form.get('hallId')),
        startTime: form.get('startTime'),
        price: Number(form.get('price')),
      }
    }
    try {
      await request(`/admin/${tab}${editing ? `/${editing.id}` : ''}`, {
        method: editing ? 'PUT' : 'POST', body: JSON.stringify(payload),
      })
      setEditing(null)
      setNotice('Изменения сохранены')
      await refreshRows()
      await refreshOptions()
      formElement.reset()
    } catch (reason) {
      setError(reason.message)
    } finally {
      setBusy(false)
    }
  }

  async function remove(row) {
    if (!window.confirm('Удалить запись?')) {
      return
    }
    setError('')
    try {
      await request(`/admin/${tab}/${row.id}`, { method: 'DELETE' })
      setNotice('Запись удалена')
      await refreshRows()
      await refreshOptions()
    } catch (reason) {
      setError(reason.message)
    }
  }

  function edit(row) {
    setEditing(row)
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }

  return (
    <main>
      <PageIntro eyebrow="УПРАВЛЕНИЕ КИНОТЕАТРОМ" title="Администрирование" text="Фильмы, расписание и проданные билеты." />
      <section className="page-section admin-section">
        <div className="admin-tabs" role="tablist" aria-label="Разделы управления">
          {[['movies', 'Фильмы'], ['screenings', 'Сеансы'], ['tickets', 'Проданные билеты']].map(([key, label]) => (
            <button
              key={key}
              className={tab === key ? 'active' : ''}
              role="tab"
              aria-selected={tab === key}
              onClick={() => {
                setRows([])
                setRowsLoading(true)
                setRowsError(false)
                setTotalPages(0)
                setEditing(null)
                setError('')
                setTab(key)
                setPage(0)
              }}
            >
              {label}
            </button>
          ))}
        </div>
        {error && <ErrorMessage message={error} />}
        {notice && <p className="admin-notice" role="status">{notice}</p>}
        {tab !== 'tickets' && (
          <form
            key={editing?.id || 'new'}
            className="admin-form"
            onSubmit={submit}
          >
            <h2>
              {editing ? 'Редактировать' : 'Добавить'}{' '}
              {tab === 'movies' ? 'фильм' : 'сеанс'}
            </h2>
            {tab === 'movies' ? (
              <>
                <label>
                  Название
                  <input
                    name="title"
                    required
                    maxLength="255"
                    defaultValue={editing?.title || ''}
                  />
                </label>
                <label>
                  Описание
                  <textarea
                    name="description"
                    required
                    maxLength="255"
                    defaultValue={editing?.description || ''}
                  />
                </label>
                <div className="admin-form-grid">
                  <label>
                    Длительность, мин
                    <input
                      name="durationMinutes"
                      type="number"
                      min="1"
                      required
                      defaultValue={editing?.durationMinutes || ''}
                    />
                  </label>
                  <label>
                    Возрастной рейтинг
                    <select name="ageRating" required defaultValue={editing?.ageRating || 'R0'}>
                      {['R0', 'R6', 'R12', 'R16', 'R18'].map((rating) => (
                        <option key={rating}>{rating}</option>
                      ))}
                    </select>
                  </label>
                  <label>
                    Дата выхода
                    <input
                      name="releaseDate"
                      type="date"
                      required
                      defaultValue={editing?.releaseDate || ''}
                    />
                  </label>
                  <label>
                    Ссылка на постер
                    <input
                      name="posterUrl"
                      type="url"
                      required
                      defaultValue={editing?.posterUrl || ''}
                    />
                  </label>
                </div>
              </>
            ) : (
              <div className="admin-form-grid">
                <label>
                  Фильм
                  <select name="movieId" required defaultValue={editing?.movieId || ''}>
                    <option value="" disabled>Выберите фильм</option>
                    {options.movies.map((movie) => (
                      <option value={movie.id} key={movie.id}>{movie.title}</option>
                    ))}
                  </select>
                </label>
                <label>
                  Зал
                  <select name="hallId" required defaultValue={editing?.hallId || ''}>
                    <option value="" disabled>Выберите зал</option>
                    {options.halls.map((hall) => (
                      <option value={hall.id} key={hall.id}>
                        {hall.cinemaName}, {hall.name}
                      </option>
                    ))}
                  </select>
                </label>
                <label>
                  Начало сеанса
                  <input
                    name="startTime"
                    type="datetime-local"
                    required
                    defaultValue={editing?.startTime?.slice(0, 16) || ''}
                  />
                </label>
                <label>
                  Цена, BYN
                  <input
                    name="price"
                    type="number"
                    min="0"
                    step="0.01"
                    required
                    defaultValue={editing?.price || ''}
                  />
                </label>
              </div>
            )}
            <div className="admin-form-actions">
              <button className="button-primary" disabled={busy}>
                {busy ? 'Сохраняем…' : editing ? 'Сохранить' : 'Добавить'}
              </button>
              {editing && (
                <button
                  type="button"
                  className="admin-cancel"
                  onClick={() => setEditing(null)}
                >
                  Отмена
                </button>
              )}
            </div>
          </form>
        )}
        {rowsLoading ? (
          <p className="admin-empty" role="status">Загружаем данные раздела...</p>
        ) : rowsError ? (
          <div className="admin-empty-state" role="alert">
            <h2>Не удалось загрузить список</h2>
            <p>Проверьте соединение и повторите запрос.</p>
            <button className="admin-retry" onClick={() => refreshRows()}>
              Повторить
            </button>
          </div>
        ) : rows.length === 0 ? (
          <div className="admin-empty-state" role="status">
            <h2>
              {tab === 'tickets'
                ? 'Покупок пока нет'
                : tab === 'screenings'
                  ? 'Сеансов пока нет'
                  : 'Фильмов пока нет'}
            </h2>
            <p>
              {tab === 'tickets'
                ? 'Проданные билеты появятся здесь после первой покупки.'
                : tab === 'screenings'
                  ? 'Добавьте первый сеанс с помощью формы выше.'
                  : 'Добавьте первый фильм с помощью формы выше.'}
            </p>
            {tab === 'screenings' && (!options.movies.length || !options.halls.length) && (
              <p>
                {!options.movies.length
                  ? 'Сначала добавьте фильм. Для создания сеанса также необходим кинозал.'
                  : 'В базе пока нет кинозалов, поэтому создать сеанс нельзя.'}
              </p>
            )}
          </div>
        ) : (
          <div className="admin-table-wrap">
            {tab === 'tickets' ? (
              <table>
                <thead>
                  <tr>
                    <th>Покупатель</th>
                    <th>Фильм / сеанс</th>
                    <th>Зал и место</th>
                    <th>Цена</th>
                  </tr>
                </thead>
                <tbody>
                  {rows.map((row) => (
                    <tr key={row.id}>
                      <td>
                        {row.customer}
                        <small>{row.email}</small>
                      </td>
                      <td>
                        {row.movie}
                        <small>{formatDate(row.startTime, { dateStyle: 'medium', timeStyle: 'short' })}</small>
                      </td>
                      <td>
                        {row.cinema}, {row.hall}
                        <small>Ряд {row.row}, место {row.seat}</small>
                      </td>
                      <td>{Number(row.price).toFixed(2)} BYN</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            ) : (
              <table>
                <thead>
                  <tr>
                    <th>{tab === 'movies' ? 'Фильм' : 'Сеанс'}</th>
                    <th>{tab === 'movies' ? 'Рейтинг / длительность' : 'Кинотеатр / зал'}</th>
                    <th></th>
                  </tr>
                </thead>
                <tbody>
                  {rows.map((row) => (
                    <tr key={row.id}>
                      <td>
                        {tab === 'movies' ? row.title : row.movieTitle}
                        <small>
                          {tab === 'screenings' && formatDate(row.startTime, { dateStyle: 'medium', timeStyle: 'short' })}
                        </small>
                      </td>
                      <td>
                        {tab === 'movies'
                          ? `${row.ageRating} · ${row.durationMinutes} мин`
                          : `${row.cinemaName}, ${row.hallName} · ${Number(row.price).toFixed(2)} BYN`}
                      </td>
                      <td className="admin-row-actions">
                        <button onClick={() => edit(row)}>Изменить</button>
                        <button onClick={() => remove(row)}>Удалить</button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>
        )}
        {totalPages > 1 && (
          <nav className="admin-pagination" aria-label="Страницы списка">
            <button disabled={page === 0} onClick={() => setPage((current) => current - 1)}>Назад</button>
            <span>{page + 1} / {totalPages}</span>
            <button disabled={page + 1 >= totalPages} onClick={() => setPage((current) => current + 1)}>Далее</button>
          </nav>
        )}
      </section>
    </main>
  )
}

function Footer() {
  return (
    <footer className="site-footer">
      <a className="brand" href="/movies">
        <span className="brand-symbol">К</span>
        <span>Кино<span className="brand-accent">Домой</span></span>
      </a>
      <span>Ваш вечер начинается здесь</span>
    </footer>
  )
}

function PageIntro({ eyebrow, title, text, image }) {
  return (
    <section className={`page-intro ${image ? 'page-intro-image' : ''}`} style={image ? { '--intro-image': `url("${image}")` } : undefined}>
      <div className="page-intro-copy">
        <p className="eyebrow">{eyebrow}</p>
        <h1>{title}</h1>
        {text && <p className="intro-text">{text}</p>}
      </div>
    </section>
  )
}

function CatalogPage() {
  const [query, setQuery] = useState('')
  const [cinemaId, setCinemaId] = useState('')
  const [page, setPage] = useState(0)
  const [onlyWithSessions, setOnlyWithSessions] = useState(true)
  const [date, setDate] = useState(localDateValue())
  const [cinemas, setCinemas] = useState([])
  const params = new URLSearchParams({
    page: String(page),
    size: String(PAGE_SIZE),
    query,
    available: String(onlyWithSessions),
    date,
  })
  if (cinemaId) params.set('cinemaId', cinemaId)

  const { data, error } = usePageData(
    () => request(`/movies?${params}`),
    [query, cinemaId, page, onlyWithSessions, date],
  )

  useEffect(() => {
    request('/cinemas').then(setCinemas).catch(() => setCinemas([]))
  }, [])

  function updateFilter(update) {
    setPage(0)
    update()
  }

  return (
    <main>
      <PageIntro
        eyebrow="БОЛЬШОЕ КИНО РЯДОМ"
        title={<>Выберите фильм.<br /><span>Остальное устроим.</span></>}
        text="Смотрите афишу, выбирайте удобный сеанс и места в зале. Билет сохранится в вашем аккаунте."
        image={data?.content?.[0]?.posterUrl}
      />
      <section className="page-section">
        <div className="section-head">
          <div>
            <p className="eyebrow">АФИША</p>
            <h2>Сейчас в кино</h2>
          </div>
          <a className="text-link" href="/schedule">Все сеансы <span aria-hidden="true">→</span></a>
        </div>
        <div className="catalog-filters">
          <label className="search-field">
            <span aria-hidden="true">⌕</span>
            <input
              value={query}
              onChange={(event) => updateFilter(() => setQuery(event.target.value))}
              placeholder="Название фильма"
              aria-label="Поиск фильма"
            />
          </label>
          <label className="filter-select">
            <span>Кинотеатр</span>
            <select value={cinemaId} onChange={(event) => updateFilter(() => setCinemaId(event.target.value))}>
              <option value="">Все кинотеатры</option>
              {cinemas.map((cinema) => <option key={cinema.id} value={cinema.id}>{cinema.name}</option>)}
            </select>
          </label>
          <label className="filter-select">
            <span>Дата сеанса</span>
            <input type="date" value={date} onChange={(event) => updateFilter(() => setDate(event.target.value))} />
          </label>
          <label className="check-filter">
            <input
              type="checkbox"
              checked={onlyWithSessions}
              onChange={(event) => updateFilter(() => setOnlyWithSessions(event.target.checked))}
            />
            <span>Есть сеансы</span>
          </label>
        </div>
        {error && <ErrorMessage message={error} />}
        {!data && !error && <Loading />}
        {data && data.content.length > 0 ? (
          <>
            <div className="movie-grid">
              {data.content.map((movie) => <MovieCard key={movie.id} movie={movie} />)}
            </div>
            <Pagination data={data} onPage={setPage} />
          </>
        ) : data && (
          <EmptyState title="Фильмы не найдены" text="Попробуйте изменить фильтры или дату." />
        )}
      </section>
    </main>
  )
}

function MovieCard({ movie }) {
  return (
    <a className="movie-card" href={`/movies/${movie.id}`}>
      <div className="movie-poster">
        {movie.posterUrl && <img src={movie.posterUrl} alt={`Постер фильма «${movie.title}»`} loading="lazy" />}
        <span className="age-label">{String(movie.ageRating || '').replace('R', '')}+</span>
        <span className="poster-action" aria-hidden="true">→</span>
      </div>
      <div className="movie-card-copy">
        <h3>{movie.title}</h3>
        <p>{movieDuration(movie.durationMinutes)} <span>·</span> {formatDate(movie.releaseDate, { year: 'numeric' })}</p>
      </div>
    </a>
  )
}

function Pagination({ data, onPage }) {
  if (data.totalPages < 2) return null

  return (
    <nav className="pagination" aria-label="Страницы каталога">
      <span>Фильмов: {data.totalElements}</span>
      <div className="pagination-controls">
        <button disabled={data.first} onClick={() => onPage(data.number - 1)} aria-label="Предыдущая страница">←</button>
        <span>{data.number + 1} / {data.totalPages}</span>
        <button disabled={data.last} onClick={() => onPage(data.number + 1)} aria-label="Следующая страница">→</button>
      </div>
    </nav>
  )
}

function SchedulePage() {
  const [date, setDate] = useState(localDateValue())
  const [cinemaId, setCinemaId] = useState(() => new URLSearchParams(window.location.search).get('cinemaId') || '')
  const [page, setPage] = useState(0)
  const [cinemas, setCinemas] = useState([])
  const params = new URLSearchParams({ date, page: String(page), size: '12' })
  if (cinemaId) params.set('cinemaId', cinemaId)
  const { data, error } = usePageData(() => request(`/screenings?${params}`), [date, cinemaId, page])

  useEffect(() => {
    request('/cinemas').then(setCinemas).catch(() => setCinemas([]))
  }, [])

  return (
    <main>
      <PageIntro
        eyebrow="ПЛАНИРУЙТЕ ВЕЧЕР"
        title="Расписание сеансов"
        text="Выберите день и кинотеатр, чтобы найти удобное время."
        image={data?.content?.[0]?.posterUrl}
      />
      <section className="page-section">
        <div className="catalog-filters schedule-filters">
          <label className="filter-select">
            <span>Дата</span>
            <input type="date" value={date} onChange={(event) => { setDate(event.target.value); setPage(0) }} />
          </label>
          <label className="filter-select">
            <span>Кинотеатр</span>
            <select value={cinemaId} onChange={(event) => { setCinemaId(event.target.value); setPage(0) }}>
              <option value="">Все кинотеатры</option>
              {cinemas.map((cinema) => <option key={cinema.id} value={cinema.id}>{cinema.name}</option>)}
            </select>
          </label>
        </div>
        {error && <ErrorMessage message={error} />}
        {!data && !error && <Loading />}
        {data && data.content.length > 0 ? (
          <>
            <div className="screening-list">
              {data.content.map((screening) => <ScreeningRow key={screening.id} screening={screening} />)}
            </div>
            <Pagination data={data} onPage={setPage} />
          </>
        ) : data && <EmptyState title="На эту дату сеансов нет" text="Выберите другой день или кинотеатр." />}
      </section>
    </main>
  )
}

function ScreeningRow({ screening }) {
  return (
    <article className="screening-row">
      <div className="screening-time">{formatTime(screening.startTime)}<span>{formatDate(screening.startTime, { day: 'numeric', month: 'short' })}</span></div>
      <div className="screening-info">
        <a href={`/movies/${screening.movieId}`}><strong>{screening.movieTitle}</strong></a>
        <span>{screening.cinemaName} · {screening.hallName}</span>
      </div>
      <div className="screening-price">{Number(screening.price).toFixed(2)} BYN</div>
      <a
        className="outline-button"
        href={`/movies/${screening.movieId}?screening=${screening.id}&date=${localDateValue(new Date(screening.startTime))}`}
      >
        Выбрать места
      </a>
    </article>
  )
}

function CinemasPage() {
  const { data, error } = usePageData(() => request('/cinemas'))

  return (
    <main>
      <PageIntro
        eyebrow="НАШИ ЗАЛЫ"
        title="Кинотеатры"
        text="Выберите удобный кинотеатр и посмотрите сеансы рядом."
      />
      <section className="page-section">
        {error && <ErrorMessage message={error} />}
        {!data && !error && <Loading />}
        {data && data.length > 0 ? (
          <div className="cinema-grid">
            {data.map((cinema) => (
              <article className="cinema-card" key={cinema.id}>
                <p className="eyebrow">КИНОТЕАТР</p>
                <h2>{cinema.name}</h2>
                <p>{cinema.address}</p>
                <a className="text-link" href={`/schedule?cinemaId=${cinema.id}`}>Сеансы кинотеатра <span aria-hidden="true">→</span></a>
              </article>
            ))}
          </div>
        ) : data && <EmptyState title="Кинотеатры пока не добавлены" text="Список появится здесь." />}
      </section>
    </main>
  )
}

function MoviePage({ movieId }) {
  const requestedDate = new URLSearchParams(window.location.search).get('date')
  const { data: movie, error } = usePageData(() => request(`/movies/${movieId}`), [movieId])
  const [date, setDate] = useState(requestedDate || localDateValue())
  const [selectedScreening, setSelectedScreening] = useState(null)
  const { data: screeningPage } = usePageData(
    () => request(`/screenings?movieId=${movieId}&date=${date}&page=0&size=24`),
    [movieId, date],
  )

  useEffect(() => {
    const screeningId = new URLSearchParams(window.location.search).get('screening')
    const selected = screeningPage?.content.find((item) => String(item.id) === screeningId)
    if (selected) setSelectedScreening(selected)
  }, [screeningPage])

  if (error) return <main className="page-section"><ErrorMessage message={error} /></main>
  if (!movie) return <main className="page-section"><Loading /></main>

  return (
    <main>
      <div className="back-link-wrap"><a className="text-link" href="/movies">← Все фильмы</a></div>
      <section className="movie-detail">
        <div className="detail-poster">{movie.posterUrl && <img src={movie.posterUrl} alt={`Постер фильма «${movie.title}»`} />}</div>
        <div className="detail-copy">
          <p className="eyebrow">ФИЛЬМ · {String(movie.ageRating || '').replace('R', '')}+</p>
          <h1>{movie.title}</h1>
          <p className="detail-meta">
            {movieDuration(movie.durationMinutes)}
            <span>·</span>
            {formatDate(movie.releaseDate, { day: 'numeric', month: 'long', year: 'numeric' })}
          </p>
          <p className="detail-description">{movie.description}</p>
          <div className="detail-schedule">
            <div className="section-head">
              <div><p className="eyebrow">РАСПИСАНИЕ</p><h2>Выберите сеанс</h2></div>
              <input className="date-input" type="date" value={date} onChange={(event) => { setDate(event.target.value); setSelectedScreening(null) }} />
            </div>
            {screeningPage?.content.length ? (
              <div className="detail-screenings">
                {screeningPage.content.map((screening) => (
                  <button
                    className={selectedScreening?.id === screening.id ? 'showtime selected' : 'showtime'}
                    key={screening.id}
                    onClick={() => setSelectedScreening(screening)}
                  >
                    <strong>{formatTime(screening.startTime)}</strong>
                    <span>{screening.cinemaName} · {screening.hallName}</span>
                    <small>{Number(screening.price).toFixed(2)} BYN</small>
                  </button>
                ))}
              </div>
            ) : <p className="muted">На выбранную дату сеансов нет.</p>}
          </div>
          {selectedScreening && <SeatPicker screening={selectedScreening} />}
        </div>
      </section>
    </main>
  )
}

function SeatPicker({ screening }) {
  const [seats, setSeats] = useState([])
  const [selected, setSelected] = useState([])
  const [error, setError] = useState('')

  useEffect(() => {
    setSelected([])
    request(`/screenings/${screening.id}/seats`)
      .then(setSeats)
      .catch((reason) => setError(reason.message))
  }, [screening.id])

  function continueToCheckout() {
    if (!selected.length) return
    sessionStorage.setItem('pending-booking', JSON.stringify({
      screeningId: screening.id,
      seatIds: selected,
    }))
    const next = `/checkout`
    navigate(token() ? next : `/login?next=${encodeURIComponent(next)}`)
  }

  return (
    <section className="seat-picker">
      <div className="seat-picker-heading">
        <div>
          <p className="eyebrow">ВЫБОР МЕСТ</p>
          <h2>{screening.cinemaName} · {screening.hallName}</h2>
        </div>
        <span>{formatTime(screening.startTime)}</span>
      </div>
      {error && <ErrorMessage message={error} />}
      {!seats.length && !error && <Loading />}
      {!!seats.length && <>
        <div className="screen-label">ЭКРАН</div>
        <div className="seats-grid">
          {seats.map((seat) => {
            const chosen = selected.includes(seat.id)
            return (
              <button
                key={seat.id}
                className={`seat ${seat.available ? '' : 'seat-taken'} ${chosen ? 'seat-selected' : ''}`}
                disabled={!seat.available}
                aria-label={`Ряд ${seat.row}, место ${seat.number}${seat.available ? '' : ', занято'}`}
                onClick={() => setSelected((current) => chosen ? current.filter((id) => id !== seat.id) : [...current, seat.id])}
              >{seat.number}</button>
            )
          })}
        </div>
        <div className="seat-key">
          <span><i className="key-free" /> Свободно</span>
          <span><i className="key-selected" /> Выбрано</span>
          <span><i className="key-taken" /> Занято</span>
        </div>
        <div className="seat-checkout">
          <span>
            {selected.length
              ? `${selected.length} мест · ${(selected.length * Number(screening.price)).toFixed(2)} BYN`
              : 'Выберите места в зале'}
          </span>
          <button className="button-primary" disabled={!selected.length} onClick={continueToCheckout}>Продолжить</button>
        </div>
      </>}
    </section>
  )
}

function AuthRedirect({ next = '/account' }) {
  useEffect(() => {
    navigate(`/login?next=${encodeURIComponent(next)}`, true)
  }, [next])
  return <main className="page-section"><Loading /></main>
}

function AuthPage({ mode, onAuthenticated }) {
  const isRegister = mode === 'register'
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const nextParam = new URLSearchParams(window.location.search).get('next') || '/account'
  const next = nextParam.startsWith('/') && !nextParam.startsWith('//') ? nextParam : '/account'

  async function submit(event) {
    event.preventDefault()
    setError('')
    setBusy(true)
    const form = new FormData(event.currentTarget)
    const payload = {
      email: form.get('email'),
      password: form.get('password'),
    }
    if (isRegister) {
      payload.name = form.get('name')
      payload.phone = form.get('phone')
      payload.birthDate = form.get('birthDate')
    }

    try {
      const result = await request(`/auth/${isRegister ? 'register' : 'login'}`, {
        method: 'POST',
        body: JSON.stringify(payload),
      })
      localStorage.setItem('cinema-jwt', result.token)
      localStorage.setItem('cinema-user', JSON.stringify(result.user))
      onAuthenticated(result.user, next)
    } catch (reason) {
      setError(reason.message)
    } finally {
      setBusy(false)
    }
  }

  return (
    <main className="auth-page">
      <section className="auth-panel">
        <a className="text-link auth-back" href="/movies">← Вернуться к афише</a>
        <p className="eyebrow">ЛИЧНЫЙ КАБИНЕТ</p>
        <h1>{isRegister ? 'Создать аккаунт' : 'С возвращением'}</h1>
        <p className="auth-description">
          {isRegister
            ? 'Сохраните билеты и оформляйте покупки быстрее.'
            : 'Войдите, чтобы продолжить покупку и увидеть свои билеты.'}
        </p>
        <div className="auth-switch">
          <a className={!isRegister ? 'selected' : ''} href={`/login?next=${encodeURIComponent(next)}`}>Вход</a>
          <a className={isRegister ? 'selected' : ''} href={`/register?next=${encodeURIComponent(next)}`}>Регистрация</a>
        </div>
        <form className="auth-form" onSubmit={submit}>
          {isRegister && <label>Имя<input name="name" required minLength="2" maxLength="64" autoComplete="name" /></label>}
          <label>Email<input name="email" type="email" required autoComplete="email" /></label>
          {isRegister && <>
            <label>Телефон<input name="phone" type="tel" required autoComplete="tel" placeholder="+375291234567" /></label>
            <label>Дата рождения<input name="birthDate" type="date" required max={localDateValue()} min={earliestBirthDate()} /></label>
          </>}
          <label>
            Пароль
            <input
              name="password"
              type="password"
              required
              minLength={isRegister ? 8 : undefined}
              maxLength={isRegister ? 128 : undefined}
              autoComplete={isRegister ? 'new-password' : 'current-password'}
            />
          </label>
          {error && <ErrorMessage message={error} />}
          <button className="button-primary auth-submit" type="submit" disabled={busy}>
            {busy ? 'Подождите…' : isRegister ? 'Зарегистрироваться' : 'Войти'}
          </button>
        </form>
      </section>
    </main>
  )
}

function CheckoutPage({ user }) {
  const [booking, setBooking] = useState(null)
  const [screening, setScreening] = useState(null)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [complete, setComplete] = useState(false)

  useEffect(() => {
    try {
      const saved = JSON.parse(sessionStorage.getItem('pending-booking') || 'null')
      setBooking(saved)
      if (saved?.screeningId) {
        request(`/screenings/${saved.screeningId}/seats`).then((seats) => {
          request(`/screenings/${saved.screeningId}`)
            .then(setScreening)
            .catch(() => setError('Не удалось загрузить данные сеанса'))
          setBooking({ ...saved, seatDetails: seats.filter((seat) => saved.seatIds.includes(seat.id)) })
        }).catch((reason) => setError(reason.message))
      }
    } catch {
      setError('Не удалось прочитать выбранные места')
    }
  }, [])

  async function confirmPurchase() {
    if (!booking?.seatIds?.length) return
    setBusy(true)
    setError('')
    try {
      await request('/tickets', {
        method: 'POST',
        body: JSON.stringify({ screeningId: booking.screeningId, seatIds: booking.seatIds }),
      })
      sessionStorage.removeItem('pending-booking')
      setComplete(true)
    } catch (reason) {
      setError(reason.message)
    } finally {
      setBusy(false)
    }
  }

  if (complete) {
    return (
      <main className="page-section completion-state">
        <p className="eyebrow">ЗАКАЗ ОФОРМЛЕН</p>
        <h1>Билеты уже в аккаунте</h1>
        <p>Покажите билет при входе в зал.</p>
        <a className="button-primary" href="/account">Мои билеты</a>
      </main>
    )
  }

  return (
    <main>
      <PageIntro
        eyebrow="ПОСЛЕДНИЙ ШАГ"
        title="Подтверждение заказа"
        text={`Билеты будут оформлены на имя ${user.name}.`}
      />
      <section className="checkout-panel page-section">
        {!booking && (
          <EmptyState
            title="Места не выбраны"
            text="Сначала выберите сеанс и места в зале."
            action={<a className="button-primary" href="/schedule">К расписанию</a>}
          />
        )}
        {booking && <>
          <div className="section-head">
            <div>
              <p className="eyebrow">ВАШ ЗАКАЗ</p>
              <h2>{screening?.movieTitle || 'Выбранный сеанс'}</h2>
            </div>
          </div>
          {screening && (
            <p className="checkout-details">
              {screening.cinemaName} · {screening.hallName} ·
              {' '}{formatDate(screening.startTime, { day: 'numeric', month: 'long' })},
              {' '}{formatTime(screening.startTime)}
            </p>
          )}
          <div className="checkout-seats">{booking.seatDetails?.map((seat) => <span key={seat.id}>Ряд {seat.row}, место {seat.number}</span>)}</div>
          {error && <ErrorMessage message={error} />}
          <button
            className="button-primary"
            disabled={busy || !booking.seatDetails?.length}
            onClick={confirmPurchase}
          >
            {busy ? 'Оформляем…' : 'Подтвердить покупку'}
          </button>
        </>}
      </section>
    </main>
  )
}

function AccountPage({ user }) {
  const { data: tickets, error } = usePageData(() => request('/tickets'), [user.email])

  return (
    <main>
      <PageIntro eyebrow="ЛИЧНЫЙ КАБИНЕТ" title={`Здравствуйте, ${user.name}`} text={user.email} />
      <section className="page-section">
        <div className="section-head"><div><p className="eyebrow">ВАШИ ПОКУПКИ</p><h2>Мои билеты</h2></div></div>
        {error && <ErrorMessage message={error} />}
        {!tickets && !error && <Loading />}
        {tickets && tickets.length ? (
          <div className="ticket-list">
            {tickets.map((ticket) => (
              <article className="account-ticket" key={ticket.id}>
                <div className="ticket-date">
                  {formatDate(ticket.startTime, { day: '2-digit', month: 'short' })}
                  <strong>{formatTime(ticket.startTime)}</strong>
                </div>
                <div className="ticket-copy">
                  <h3>{ticket.movieTitle}</h3>
                  <p>{ticket.cinemaName} · {ticket.hallName}</p>
                  <span>Ряд {ticket.row}, место {ticket.number}</span>
                </div>
                <strong className="ticket-price">{Number(ticket.price).toFixed(2)} BYN</strong>
              </article>
            ))}
          </div>
        ) : tickets && (
          <EmptyState
            title="Билетов пока нет"
            text="Выберите фильм и удобный сеанс."
            action={<a className="button-primary" href="/movies">Выбрать фильм</a>}
          />
        )}
      </section>
    </main>
  )
}

function ErrorMessage({ message }) {
  return <p className="error-message" role="alert">{message}</p>
}

function Loading() {
  return <p className="loading-state" role="status">Загружаем…</p>
}

function EmptyState({ title, text, action }) {
  return <div className="empty-state"><h2>{title}</h2><p>{text}</p>{action}</div>
}

createRoot(document.getElementById('root')).render(<App />)
