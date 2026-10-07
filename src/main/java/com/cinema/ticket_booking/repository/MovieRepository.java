package com.cinema.ticket_booking.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.cinema.ticket_booking.DTO.Movie;

public interface MovieRepository extends JpaRepository<Movie, Long> {
    @Query("""
            select m from Movie m
            where (:query = '' or lower(m.title) like lower(concat('%', :query, '%')))
              and (:available = false or exists (
                  select s.id from Screening s
                  where s.movie = m
                    and (:cinemaId = 0 or s.hall.cinema.id = :cinemaId)
                    and s.startTime >= :fromTime
                    and s.startTime < :toTime
              ))
            order by m.releaseDate desc, m.title asc
            """)
    Page<Movie> findCatalogPage(
            @Param("query") String query,
            @Param("available") boolean available,
            @Param("cinemaId") Long cinemaId,
            @Param("fromTime") java.time.LocalDateTime fromTime,
            @Param("toTime") java.time.LocalDateTime toTime,
            Pageable pageable
    );
}
