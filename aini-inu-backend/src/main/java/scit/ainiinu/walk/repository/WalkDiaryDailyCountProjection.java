package scit.ainiinu.walk.repository;

import java.time.LocalDate;

public interface WalkDiaryDailyCountProjection {
    LocalDate getWalkDate();

    long getWalkCount();
}
