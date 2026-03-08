package scit.ainiinu.timeline.repository;

import java.time.LocalDate;

public interface ActivityDailyCountProjection {
    LocalDate getActivityDate();

    long getActivityCount();
}
