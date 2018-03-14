package com.caile.jczq.data.crawler;

import org.springframework.data.jpa.repository.JpaRepository;

public interface HistoryMatchDataRepository extends JpaRepository<HistoryMatchData, Long> {

    HistoryMatchData findAllByHomeTeamAndAwayTeamAndLeagueNameAndSeasonAndWeek(String homeTeam,String AwayTeam,String LeagueName,String Season,String Week);
}
