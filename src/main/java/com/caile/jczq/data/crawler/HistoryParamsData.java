package com.caile.jczq.data.crawler;

import lombok.Data;
import org.hibernate.annotations.DynamicUpdate;

import javax.persistence.*;

/**@author denglei
 * Created by Administrator on 2018/3/13.
 */
@Data
@Entity
@Table(name = "caile_x_history_match_params")
@DynamicUpdate
public class HistoryParamsData {

    @SequenceGenerator(name = "caile_x_history_match_params_id_sequence",sequenceName = "caile_x_history_match_params_id_sequence")
    @GeneratedValue(generator = "caile_x_history_match_params_id_sequence")
    @Id
    private long id;

    /**
     * 赛季和联赛名称
     * */
    private String leagueName;

    /**
     * 操作方法，初赔或者终赔
     * */
    private String action;

    /**
     *比赛轮次
     * */
    private Integer week;

    /**
     *c_id
     * */
    private Long cId;

    /**
     *competition_id
     * */
    private Long competitionId;

    /**
     *s_id
     * */
    private Long sId;

    /**
     *r_id
     * */
    private Long rId;

    /**
     *g_id
     * */
    private Long gId;

    /**
     *table_type
     * */
    private String tableType;

    /**
     *order_type
     * */
    private String orderType;

    /**
     *groups
     * */
    private Long groups;

    /**
     *round_type
     * */
    private String roundType;

    /**
     *type1
     * */
    private String type1;

    /**
     *type2
     * */
    private String type2;

    /**
     *is_ok
     * */
    private Integer isOk;

    /**
     * 联赛或者杯赛
     */
    @Enumerated(EnumType.STRING)
    private IsLeague isLeague;

    public enum IsLeague {
        Table("联赛"), Cup("杯赛");

        private final String text;

        IsLeague(String text) {
            this.text = text;
        }

        public String text() {
            return this.text;
        }
    }
}
