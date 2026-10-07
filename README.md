# КиноДомой

Сервис для выбора фильма и сеанса, бронирования мест и хранения билетов в аккаунте.

## Запуск

1. Скопируйте `.env.example` в `.env`.
2. Задайте в `.env` собственный `JWT_SECRET` длиной не менее 32 символов.
3. При необходимости измените `ITEMS_PER_PAGE` в `.env` (размер страницы каталога, по умолчанию 4).
4. Запустите `docker compose up --build`.
4. Откройте `http://localhost:3000`.

Compose автоматически читает настройки из `.env`. PostgreSQL хранит данные в Docker volume `cinema_pgdata`.

## Страницы

- `/movies` — афиша, поиск, фильтры и постраничная загрузка.
- `/movies/{id}` — описание фильма, сеансы и места.
- `/schedule` — расписание по дате и кинотеатру.
- `/cinemas` — кинотеатры.
- `/login`, `/register` — вход и регистрация.
- `/checkout`, `/account` — покупка и билеты пользователя.

При попытке купить билет без входа выбранные сеанс и места сохраняются до авторизации. Покупка использует профиль текущего пользователя; клиент не передаёт имя или email владельца билета.

## REST API

- `GET /api/movies?page=0&size=4&query=&available=true&date=YYYY-MM-DD&cinemaId=` — каталог с фильтрами и метаданными страниц; без `size` применяется `ITEMS_PER_PAGE`.
- `GET /api/movies/{id}`, `POST /api/movies`, `PUT /api/movies/{id}`, `DELETE /api/movies/{id}` — чтение и CRUD фильмов.
- `GET /api/screenings?page=0&size=12&date=YYYY-MM-DD&movieId=&cinemaId=` — расписание страницами.
- `GET /api/screenings/{id}`, `GET /api/screenings/{id}/seats` — сеанс и места с признаком занятости.
- `POST /api/tickets` с `screeningId` и `seatIds`, `GET /api/tickets`, `DELETE /api/tickets/{id}` — операции над билетами. Требуется JWT; список и удаление ограничены аккаунтом владельца.
- `POST /api/auth/register`, `POST /api/auth/login` — регистрация и вход.

Демонстрационные фильмы, сеансы и залы создаются при пустой базе, если `TEST_DATA_ENABLED=true`. Подробное описание домена и 12 факторов приведено в [Отчёте](Отчёт.md).
