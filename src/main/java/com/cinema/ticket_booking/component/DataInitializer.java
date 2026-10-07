package com.cinema.ticket_booking.component;

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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {
    private final CinemaRepository cinemaRepository;
    private final HallRepository hallRepository;
    private final ScreeningRepository screeningRepository;
    private final SeatRepository seatRepository;
    private final MovieRepository movieRepository;

    @Value("${test-data.enabled:false}")
    private boolean enabled;

    @Override
    public void run(String... args) {
        if (!enabled) {
            return;
        }

        List<Cinema> cinemas = cinemaRepository.findAll();
        if (cinemas.isEmpty()) {
            cinemas = cinemaRepository.saveAll(List.of(
                    cinema("Октябрь", "ул. Советская, 18"),
                    cinema("Москва", "пр. Независимости, 73"),
                    cinema("Салют", "ул. Кальварийская, 24")
            ));
        }

        List<Hall> halls = hallRepository.findAll();
        if (halls.isEmpty()) {
            halls = createHalls(cinemas);
        }
        if (seatRepository.count() == 0) {
            createSeats(halls);
        }

        List<Movie> catalog = List.of(
                movie(
                        "Интерстеллар",
                        "Исследователи отправляются через червоточину на поиски нового дома для человечества.",
                        169,
                        AgeRating.R12,
                        "2014-11-06",
                        "https://st.kp.yandex.net/images/film_iphone/iphone360_258687.jpg"),
                movie(
                        "Начало",
                        "Профессиональный вор проникает в сознание людей через управление снами.",
                        148,
                        AgeRating.R16,
                        "2010-07-22",
                        "https://st.kp.yandex.net/images/film_iphone/iphone360_447301.jpg"),
                movie(
                        "Матрица",
                        "Хакер узнаёт, что окружающий мир является искусственной реальностью.",
                        136,
                        AgeRating.R16,
                        "1999-03-31",
                        "https://st.kp.yandex.net/images/film_iphone/iphone360_301.jpg"),
                movie(
                        "Дюна",
                        "Наследник благородного дома оказывается втянут в борьбу за пустынную планету.",
                        155,
                        AgeRating.R12,
                        "2021-09-15",
                        "https://avatars.mds.yandex.net/get-kinopoisk-image/4303601/9eb762d6-4cdd-464f-9937-aebf30067acc/600x900"),
                movie(
                        "Джокер",
                        "История человека, который постепенно превращается в криминальную фигуру Готэма.",
                        122,
                        AgeRating.R18,
                        "2019-10-03",
                        "https://st.kp.yandex.net/images/film_iphone/iphone360_1048334.jpg"),
                movie(
                        "Пятый элемент",
                        "Таксист из будущего становится участником борьбы за спасение Земли.",
                        126,
                        AgeRating.R12,
                        "1997-05-07",
                        "https://st.kp.yandex.net/images/film_iphone/iphone360_2656.jpg"),
                movie(
                        "Побег из Шоушенка",
                        "Банкир, осуждённый за преступление, которого не совершал, ищет надежду и свободу за стенами тюрьмы.",
                        142,
                        AgeRating.R16,
                        "1994-09-10",
                        "https://st.kp.yandex.net/images/film_iphone/iphone360_326.jpg"),
                movie(
                        "Крёстный отец",
                        "Сага о семье Корлеоне и цене власти в послевоенной Америке.",
                        175,
                        AgeRating.R16,
                        "1972-03-14",
                        "https://st.kp.yandex.net/images/film_iphone/iphone360_333.jpg"),
                movie(
                        "Зелёная миля",
                        "Надзиратель блока смертников встречает необычного заключённого с удивительным даром.",
                        189,
                        AgeRating.R16,
                        "1999-12-06",
                        "https://st.kp.yandex.net/images/film_iphone/iphone360_435.jpg"),
                movie(
                        "Аватар",
                        "На далёкой Пандоре бывший морпех оказывается между приказом и судьбой планеты.",
                        162,
                        AgeRating.R12,
                        "2009-12-10",
                        "https://st.kp.yandex.net/images/film_iphone/iphone360_251733.jpg"),
                movie(
                        "Одержимость",
                        "Молодой барабанщик стремится к совершенству под руководством безжалостного дирижёра.",
                        106,
                        AgeRating.R16,
                        "2014-01-16",
                        "https://st.kp.yandex.net/images/film_iphone/iphone360_725190.jpg"),
                movie(
                        "Паразиты",
                        "История двух семей из разных миров, чьи судьбы неожиданно переплетаются.",
                        131,
                        AgeRating.R18,
                        "2019-05-21",
                        "https://st.kp.yandex.net/images/film_iphone/iphone360_1043758.jpg")
        );

        Set<String> existingTitles = new HashSet<>();
        movieRepository.findAll().forEach(movie -> existingTitles.add(movie.getTitle()));
        List<Movie> newMovies = catalog.stream()
                .filter(movie -> !existingTitles.contains(movie.getTitle()))
                .toList();
        if (newMovies.isEmpty()) {
            return;
        }

        List<Movie> savedMovies = movieRepository.saveAll(newMovies);
        createScreenings(halls, savedMovies);
    }

    private List<Hall> createHalls(List<Cinema> cinemas) {
        List<Hall> halls = new ArrayList<>();
        for (Cinema cinema : cinemas) {
            for (int number = 1; number <= 2; number++) {
                Hall hall = new Hall();
                hall.setCinema(cinema);
                hall.setName("Зал " + number);
                halls.add(hall);
            }
        }
        return hallRepository.saveAll(halls);
    }

    private void createSeats(List<Hall> halls) {
        List<Seat> seats = new ArrayList<>();
        for (Hall hall : halls) {
            for (int row = 1; row <= 6; row++) {
                for (int number = 1; number <= 8; number++) {
                    Seat seat = new Seat();
                    seat.setHall(hall);
                    seat.setRowNumber(row);
                    seat.setSeatNumber(number);
                    seats.add(seat);
                }
            }
        }
        seatRepository.saveAll(seats);
    }

    private void createScreenings(List<Hall> halls, List<Movie> movies) {
        List<LocalTime> times = List.of(
                LocalTime.of(10, 30),
                LocalTime.of(13, 30),
                LocalTime.of(16, 30),
                LocalTime.of(19, 30),
                LocalTime.of(21, 45)
        );
        List<Screening> screenings = new ArrayList<>();

        for (int day = 0; day < 7; day++) {
            for (int hallIndex = 0; hallIndex < halls.size(); hallIndex++) {
                for (int timeIndex = 0; timeIndex < times.size(); timeIndex++) {
                    Screening screening = new Screening();
                    screening.setHall(halls.get(hallIndex));
                    screening.setMovie(movies.get((day + hallIndex + timeIndex) % movies.size()));
                    screening.setStartTime(LocalDateTime.of(LocalDate.now().plusDays(day), times.get(timeIndex)));
                    screening.setPrice(BigDecimal.valueOf(8.90 + hallIndex * 2 + (timeIndex > 2 ? 1.5 : 0)));
                    screenings.add(screening);
                }
            }
        }
        screeningRepository.saveAll(screenings);
    }

    private Cinema cinema(String name, String address) {
        Cinema cinema = new Cinema();
        cinema.setName(name);
        cinema.setAddress(address);
        return cinema;
    }

    private Movie movie(String title, String description, int duration, AgeRating rating, String date, String poster) {
        Movie movie = new Movie();
        movie.setTitle(title);
        movie.setDescription(description);
        movie.setDurationMinutes(duration);
        movie.setAgeRating(rating);
        movie.setReleaseDate(LocalDate.parse(date));
        movie.setPosterUrl(poster);
        return movie;
    }
}
