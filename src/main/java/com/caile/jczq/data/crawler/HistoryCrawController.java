package com.caile.jczq.data.crawler;

import com.querydsl.core.types.dsl.BooleanExpression;
import lombok.SneakyThrows;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

/**@author denglei
 * Created by Administrator on 2018/3/14.
 */
@Service
@RestController
public class HistoryCrawController {

    @Resource
    private HistoryLeagueDataRepository historyLeagueDataRepository;
    private QHistoryLeagueData qHistoryLeagueData;

    @RequestMapping("/league")
    @SneakyThrows
    public String league(){
        HttpClient httpClient = HttpClientBuilder.create().build();
        HttpGet httpGet = new HttpGet("http://info.sporttery.cn/football/history/data_center.php");
        HttpResponse httpresponse = httpClient.execute(httpGet);
        HttpEntity entity = httpresponse.getEntity();
        String body = EntityUtils.toString(entity, "gb2312");

        Document document = Jsoup.parse(body);
        Element content = document.getElementById("content");
        Elements contents = content.getElementsByClass("matchList");
        Date date = new Date();

        BooleanExpression preciate;
        QHistoryLeagueData qHistoryLeagueData = QHistoryLeagueData.historyLeagueData;

        for (Element cont:contents) {

            String country_name = cont.getElementsByClass("name").html();
            Elements img = cont.getElementsByTag("img");
            String logo = img.attr("src");
            Elements match_box = cont.getElementsByTag("li");

            for (Element li:match_box) {
                HistoryLeagueData historyLeagueData = new HistoryLeagueData();
                Element a = li.child(0);
                String url = a.attr("href");
                String league_name = a.html();
                Long m_id = Long.valueOf(url.substring(url.indexOf("mid=")+4).trim());

                preciate = qHistoryLeagueData.mId.eq(m_id);
                Iterable<HistoryLeagueData> iterator = historyLeagueDataRepository.findAll(preciate);
                if(iterator.iterator().hasNext()) continue;

                historyLeagueData.setCountry(country_name);
                historyLeagueData.setCreationTime(date);
                historyLeagueData.setLeagueName(league_name);
                historyLeagueData.setLogo(logo);
                historyLeagueData.setMId(m_id);
                historyLeagueData.setUri(url);
                historyLeagueDataRepository.save(historyLeagueData);
            }
        }
        return body;
    }

    @RequestMapping("/match")
    @SneakyThrows
    public String match(){
        String www = "http://info.sporttery.cn/football/history/";
        HttpClient httpClient = HttpClientBuilder.create().build();
        Iterable<String> iterable = historyLeagueDataRepository.findUri();

        Iterator<String> iterator = iterable.iterator();

        while (iterator.hasNext()){
            String url = iterator.next();
            String urls = www+url;
            iterator.remove();
            HttpGet httpGet = new HttpGet(urls);
            HttpResponse httpResponse = httpClient.execute(httpGet);
            HttpEntity entity = httpResponse.getEntity();
            String body = EntityUtils.toString(entity, "gb2312");

            Document document = Jsoup.parse(body);

            //先获取所有的赛季
            Element season_list = document.getElementById("season_list");
            Elements season_lists = season_list.getElementsByTag("li");
            //获取联赛轮次
            Element league_num = document.select("div.league_num").first();
            Elements tds = league_num.getElementsByTag("td");

            //准备数据
            String[] action = {"lc","three_-1_e"};//竞彩终赔
            String[] action_bc = {"bc","three_-1_s"};//竞彩初赔

            List<String[]> actions = new ArrayList<>();

            actions.add(action);
            actions.add(action_bc);

            Long c_id,competition_id = Long.valueOf(url.substring(url.indexOf("mid=")+4).trim());
            Long r_id = Long.valueOf(document.select("table.league_name.seasons td").first().id());
            Long g_id = 0L;

            //先循环赛季
            for (Element element: season_lists) {

                Long s_id = Long.valueOf(element.id());
                Elements a = element.getElementsByTag("a");
                String season = a.html();

                //循环初赔终赔
                for (String[] act: actions) {


                }

                //循环比赛轮次/添加竞彩终赔
                for(Element td:tds){

                    Long week = Long.valueOf(td.id());

                }
                //再次循环比赛轮次/添加竞彩初赔


            }


        }

        return "11";
    }
}
