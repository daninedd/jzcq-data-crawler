package com.caile.jczq.data.crawler;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;

public interface HistoryMatchDataRepository extends JpaRepository<HistoryMatchData, Long>,QuerydslPredicateExecutor<HistoryMatchData> {

    HistoryMatchData findAllByHomeTeamAndAwayTeamAndLeagueNameAndSeasonAndWeek(String homeTeam,String AwayTeam,String LeagueName,String Season,String Week);
}

