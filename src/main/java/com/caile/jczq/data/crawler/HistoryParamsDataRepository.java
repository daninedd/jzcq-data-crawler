package com.caile.jczq.data.crawler;

import com.querydsl.core.types.Predicate;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;

import java.util.List;

/**@author denglei
 * Created by Administrator on 2018/3/13.
 */
public interface HistoryParamsDataRepository extends JpaRepository<HistoryParamsData,Long>,QuerydslPredicateExecutor<HistoryParamsData> {
//
//    @Query("select action,CId,competitionId,GId,groups,orderType,RId,roundType,SId,tableType,type1,type2,week from HistoryParamsData where isOk = 0")
//    List<String> findData();

//   List<HistoryParamsData> findAll(Predicate predicate, Sort sort);
}
