package com.cinema.ticket_booking.component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.cinema.ticket_booking.DTO.AgeRating;
import com.cinema.ticket_booking.DTO.Cinema;
import com.cinema.ticket_booking.DTO.Hall;
import com.cinema.ticket_booking.DTO.Movie;
import com.cinema.ticket_booking.DTO.Screening;
import com.cinema.ticket_booking.DTO.Seat;
import com.cinema.ticket_booking.repository.CinemaRepository;
import com.cinema.ticket_booking.repository.HallRepository;
import com.cinema.ticket_booking.repository.MovieRepository;
import com.cinema.ticket_booking.repository.ScreeningRepository;
import com.cinema.ticket_booking.repository.SeatRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final CinemaRepository cinemaRepository;
    private final HallRepository hallRepository;
    private final ScreeningRepository screeningRepository;
    private final SeatRepository seatRepository;
    private final MovieRepository movieRepository;

    @Value("${test-data.enabled:false}")
    private boolean useTestData;

    @Override
    public void run(String... args) {
        if (!useTestData) {
            return;
        }

        if (cinemaRepository.count() > 0) {
            return;
        }

        List<Cinema> cinemas = createCinemas();
        List<Hall> halls = createHalls(cinemas);

        createSeats(halls);

        List<Movie> movies = createMovies();
        createScreenings(halls, movies);
    }

    private List<Cinema> createCinemas() {
        List<Cinema> cinemas = List.of(
                createCinema(
                        "Октябрь",
                        "ул. Советская, 18"
                ),
                createCinema(
                        "Москва",
                        "пр. Независимости, 73"
                ),
                createCinema(
                        "Салют",
                        "ул. Кальварийская, 24"
                ),
                createCinema(
                        "Аврора",
                        "пр. Победителей, 9"
                )
        );

        return cinemaRepository.saveAll(cinemas);
    }

    private Cinema createCinema(String name, String address) {
        Cinema cinema = new Cinema();
        cinema.setName(name);
        cinema.setAddress(address);

        return cinema;
    }

    private List<Hall> createHalls(List<Cinema> cinemas) {
        List<Hall> halls = new ArrayList<>();

        for (Cinema cinema : cinemas) {
            for (int i = 1; i <= 3; i++) {
                Hall hall = new Hall();
                hall.setCinema(cinema);
                hall.setName("Зал " + i);

                halls.add(hall);
            }
        }

        return hallRepository.saveAll(halls);
    }

    private void createSeats(List<Hall> halls) {
        List<Seat> seats = new ArrayList<>();

        for (Hall hall : halls) {
            for (int row = 1; row <= 10; row++) {
                for (int seatNumber = 1; seatNumber <= 12; seatNumber++) {
                    Seat seat = new Seat();

                    seat.setHall(hall);
                    seat.setRowNumber(row);
                    seat.setSeatNumber(seatNumber);

                    seats.add(seat);
                }
            }
        }

        seatRepository.saveAll(seats);
    }

    private List<Movie> createMovies() {
        List<Movie> movies = List.of(
                createMovie(
                        "Интерстеллар",
                        "Исследователи отправляются через червоточину в поисках нового дома для человечества.",
                        169,
                        AgeRating.R12,
                        "2014-11-06",
                        "https://image.tmdb.org/t/p/w500/gEU2QniE6E77NI6lCU6MxlNBvIx.jpg"
                ),
                createMovie(
                        "Начало",
                        "Профессиональный вор проникает в подсознание людей через управление снами.",
                        148,
                        AgeRating.R16,
                        "2010-07-22",
                        "https://image.tmdb.org/t/p/w500/oYuLEt3zVCKq57qu2F8dT7NIa6f.jpg"
                ),
                createMovie(
                        "Матрица",
                        "Хакер узнаёт, что окружающий мир является искусственной реальностью.",
                        136,
                        AgeRating.R16,
                        "1999-03-31",
                        "https://image.tmdb.org/t/p/w500/f89U3ADr1oiB1s9GkdPOEpXUk5H.jpg"
                ),
                createMovie(
                        "Дюна",
                        "Наследник благородного дома оказывается втянут в борьбу за пустынную планету.",
                        155,
                        AgeRating.R12,
                        "2021-09-15",
                        "https://image.tmdb.org/t/p/w500/d5NXSklXo0qyIYkgV94XAgMIckC.jpg"
                ),
                createMovie(
                        "Джокер",
                        "История человека, который постепенно превращается в криминальную фигуру Готэма.",
                        122,
                        AgeRating.R18,
                        "2019-10-03",
                        "https://image.tmdb.org/t/p/w500/udDclJoHjfjb8Ekgsd4FDteOkCU.jpg"
                ),
                createMovie(
                        "Пятый элемент",
                        "Таксист из будущего становится участником борьбы за спасение Земли.",
                        126,
                        AgeRating.R12,
                        "1997-05-07",
                        "https://image.tmdb.org/t/p/w500/tXl4LcgFAjDvD17ThWEabfAVNVY.jpg"
                ),
                createMovie(
                        "Гарри Поттер и философский камень",
                        "Юный волшебник узнаёт о своём прошлом и поступает в школу магии.",
                        152,
                        AgeRating.R6,
                        "2001-11-04",
                        "https://image.tmdb.org/t/p/w500/wuMc08IPKEatf9rnMNXvIDxqP4W.jpg"
                ),
                createMovie(
                        "Властелин колец: Братство кольца",
                        "Хоббит Фродо отправляется в путешествие, чтобы уничтожить могущественное кольцо.",
                        178,
                        AgeRating.R12,
                        "2001-12-19",
                        "https://image.tmdb.org/t/p/w500/6oom5QYQ2yQTMJIbnvbkBL9cHo6.jpg"
                ),
                createMovie(
                        "Остров проклятых",
                        "Федеральный маршал расследует исчезновение пациентки на закрытом острове.",
                        138,
                        AgeRating.R16,
                        "2010-02-18",
                        "https://image.tmdb.org/t/p/w500/nrmXQ0zcZUL8jFLrakWc90IR8z9.jpg"
                ),
                createMovie(
                        "Безумный Макс: Дорога ярости",
                        "Герои пытаются вырваться из пустыни, преследуемые вооружённой армией.",
                        120,
                        AgeRating.R16,
                        "2015-05-14",
                        "https://image.tmdb.org/t/p/w500/hA2ple9q4qnwxp3hKVNhroipsir.jpg"
                ),
                createMovie(
                        "Корпорация монстров",
                        "Монстры получают энергию из детского смеха и сталкиваются с необычным ребёнком.",
                        92,
                        AgeRating.R6,
                        "2001-11-02",
                        "https://image.tmdb.org/t/p/w500/6gcgtaiWgkFJptFG7l3dkfxJF75.jpg"
                ),
                createMovie(
                        "Назад в будущее",
                        "Подросток случайно отправляется в прошлое на машине времени.",
                        116,
                        AgeRating.R6,
                        "1985-07-03",
                        "https://image.tmdb.org/t/p/w500/pTpxQB1N0waaSc3OSn0e9oc8kx9.jpg"
                )
        );

        return movieRepository.saveAll(movies);
    }

    private Movie createMovie(
            String title,
            String description,
            int durationMinutes,
            AgeRating ageRating,
            String releaseDate,
            String posterUrl
    ) {
        Movie movie = new Movie();

        movie.setTitle(title);
        movie.setDescription(description);
        movie.setDurationMinutes(durationMinutes);
        movie.setAgeRating(ageRating);
        movie.setReleaseDate(LocalDate.parse(releaseDate));
        movie.setPosterUrl(posterUrl);

        return movie;
    }

    private void createScreenings(
            List<Hall> halls,
            List<Movie> movies
    ) {
        List<Screening> screenings = new ArrayList<>();

        LocalDate startDate = LocalDate.now();

        List<LocalTime> startTimes = List.of(
                LocalTime.of(10, 0),
                LocalTime.of(12, 45),
                LocalTime.of(15, 30),
                LocalTime.of(18, 15),
                LocalTime.of(21, 0)
        );

        for (int day = 0; day < 10; day++) {
            LocalDate date = startDate.plusDays(day);

            for (int hallIndex = 0; hallIndex < halls.size(); hallIndex++) {
                Hall hall = halls.get(hallIndex);

                for (int timeIndex = 0; timeIndex < startTimes.size(); timeIndex++) {
                    Movie movie = movies.get(
                            (day + hallIndex + timeIndex) % movies.size()
                    );

                    Screening screening = new Screening();

                    screening.setMovie(movie);
                    screening.setHall(hall);

                    screening.setStartTime(
                            LocalDateTime.of(
                                    date,
                                    startTimes.get(timeIndex)
                            )
                    );

                    screening.setPrice(
                            calculatePrice(
                                    hallIndex,
                                    day,
                                    timeIndex
                            )
                    );

                    screenings.add(screening);
                }
            }
        }

        screeningRepository.saveAll(screenings);
    }

    private BigDecimal calculatePrice(
            int hallIndex,
            int day,
            int timeIndex
    ) {
        BigDecimal price = BigDecimal.valueOf(8.90);

        if (hallIndex % 3 == 1) {
            price = price.add(BigDecimal.valueOf(2.00));
        }

        if (hallIndex % 3 == 2) {
            price = price.add(BigDecimal.valueOf(4.00));
        }

        if (timeIndex >= 3) {
            price = price.add(BigDecimal.valueOf(1.50));
        }

        if (day % 7 == 5 || day % 7 == 6) {
            price = price.add(BigDecimal.valueOf(2.00));
        }

        return price;
    }
}
