package kz.jqd.arqa.repository;

import kz.jqd.arqa.entity.Trip;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;

public interface TripRepository extends JpaRepository<Trip, String> {

    List<Trip> findByLocalTripDateOrderByStartedAtAsc(LocalDate localTripDate);

    List<Trip> findByLocalTripDateBetweenOrderByStartedAtAsc(LocalDate firstDay, LocalDate lastDay);

    Page<Trip> findByLocalTripDateBetween(LocalDate firstDay, LocalDate lastDay, Pageable pageable);

    @Query("select distinct trip.localTripDate from Trip trip order by trip.localTripDate")
    List<LocalDate> findAllDatesWithTrips();
}
