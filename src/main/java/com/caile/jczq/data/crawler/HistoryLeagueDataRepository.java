package com.caile.jczq.data.crawler;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;

import java.util.List;

/**@author denglei
 * Created by Administrator on 2018/3/14.
 */
public interface HistoryLeagueDataRepository extends JpaRepository<HistoryLeagueData,Long> , QuerydslPredicateExecutor<HistoryLeagueData> {

    @Query("select uri from HistoryLeagueData")
    List<String> findUri();
}
