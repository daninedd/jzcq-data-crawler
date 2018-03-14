package com.caile.jczq.data.crawler;

import lombok.Data;
import org.hibernate.annotations.DynamicUpdate;

import javax.persistence.*;
import java.util.Date;

@Data
@Entity
@Table(name = "caile_x_history_match")
@DynamicUpdate
public class HistoryMatchData {

    @SequenceGenerator(name = "caile_x_history_match_id_sequence", sequenceName = "caile_x_history_match_id_sequence")
    @GeneratedValue(generator = "caile_x_history_match_id_sequence")
    @Id
    private long   id;

    /**
     * 主队
     */
    private String homeTeam;

    /**
     * 客队
     */
    private String awayTeam;

    /**
     * 全场主队得分
     */
    private Long    fullHomeScore;

    /**
     * 半场主队得分
     */
    private Long    halfHomeScore;

    /**
     * 全场客队得分
     */
    private Long    fullAwayScore;

    /**
     * 半场客队得分
     */
    private Long    halfAwayScore;

    /**
     * 比赛时间
     */
    private Date   matchDate;

    /**
     * 竞彩胜终赔
     */
    private Long   jczqWinFinalOdds;

    /**
     * 竞彩平终赔
     */
    private Long   jczqDrawFinalOdds;

    /**
     * 竞彩负终赔
     */
    private Long   jczqLossFinalOdds;

    /**
     * 竞彩胜初赔
     */
    private Long    jczqWinFirstOdds;

    /**
     * 竞彩平初赔
     */
    private Long    jczqDrawFirstOdds;

    /**
     * 竞彩负初赔
     */
    private Long    jczqLossFirstOdds;

    /**
     * 获胜球队名称
     */
    private String  winTeam;

    /**
     * 赛季
     */
    private String  season;

    /**
     * 比赛轮次
     */
    private String  week;

    /**
     * 联赛名称
     */
    private String  leagueName;
}
