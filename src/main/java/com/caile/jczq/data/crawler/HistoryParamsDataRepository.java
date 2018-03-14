package com.caile.jczq.data.crawler;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;

/**@author denglei
 * Created by Administrator on 2018/3/13.
 */
public interface HistoryParamsDataRepository extends JpaRepository<HistoryParamsData,Long>,QuerydslPredicateExecutor<HistoryParamsData> {

//    HistoryParamsData findAllByLeagueNameAndActionAndWeekAndCompetitionIdAndSIdAndRIdAndGIdAndTableTypeAndOrderTypeAndGroupsAndType1AndType2(
//            String LeagueName,String Action,Integer Week,Long CompetitionId,Long SId,Long RId,Long GId,String TableType,String OrderType,String Groups,String Type1,String Type2);
}
