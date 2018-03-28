package com.caile.jczq.data.crawler;

import lombok.Data;
import org.hibernate.annotations.DynamicUpdate;

import javax.persistence.*;
import java.util.Date;

/**@author denglei
 * Created by Administrator on 2018/3/14.
 */
@Data
@Entity
@Table(name = "caile_x_history_league")
@DynamicUpdate

public class HistoryLeagueData {

    @SequenceGenerator(name = "caile_x_history_league_id_sequence",sequenceName = "caile_x_history_league_id_sequence")
    @GeneratedValue(generator = "caile_x_history_league_id_sequence")
    @Id
    private long id;

    /**
     *国家名称
     * */
    private String country;

    /**
     *国家logo_url
     * */
    private String logo;

    /**
     *联赛名称
     * */
    private String leagueName;

    /**
     * 联赛Id：mid
     * */
    private Long mId;

    /**
     * 联赛链接
     * */
    private String uri;

    /**
     * 创建时间
     * */
    private Date creationTime;

    /**
     * 是否联赛
     * */

}
