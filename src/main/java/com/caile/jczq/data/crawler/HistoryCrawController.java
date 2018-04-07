package com.caile.jczq.data.crawler;

import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.caile.jczq.data.utill.DataUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.querydsl.core.types.dsl.BooleanExpression;
import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.data.domain.Sort;
import org.springframework.http.codec.json.Jackson2CodecSupport;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;

/**@author denglei
 * Created by Administrator on 2018/3/14.
 */
@Service
@RestController
public class HistoryCrawController {

    @Resource
    private HistoryLeagueDataRepository historyLeagueDataRepository;
    @Resource
    private HistoryParamsDataRepository historyParamsDataRepository;
    @Resource
    private HistoryMatchDataRepository historyMatchDataRepository;

    /**
     * 所有国家比赛
     * */
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

    /**
     * 联赛参数
     * */
    @SneakyThrows
    public @ResponseBody String tableMatchParams(){

        String www = "http://info.sporttery.cn/football/history/";
        HttpClient httpClient = HttpClientBuilder.create().build();
        Iterable<String> iterable = historyLeagueDataRepository.findUri();
        Iterator<String> iterator = iterable.iterator();
        try {

            while (iterator.hasNext()){
                String url = iterator.next();
                String urls = www+url;
                iterator.remove();
                URL url_end = new URL(urls);
                URI uri = new URI(url_end.getProtocol(), url_end.getHost(), url_end.getPath(), url_end.getQuery(), null);
                HttpGet httpget = new HttpGet(uri);
                HttpResponse httpResponse = httpClient.execute(httpget);
                HttpEntity entity = httpResponse.getEntity();
                String body = EntityUtils.toString(entity, "gb2312");

                Document document = Jsoup.parse(body);

                //先获取所有的赛季
                Element season_list = document.getElementById("season_list");
                Elements season_lists = season_list.getElementsByTag("li");

                //准备数据
                String[] action = {"lc","three_-1_e"};//联赛+竞彩终赔
                String[] action_bc = {"bc","three_-1_s"};//联赛+竞彩初赔

                List<String[]> actions = new ArrayList<>();

                actions.add(action);
                actions.add(action_bc);

                Long c_id;
                Long competition_id = Long.valueOf(url.substring(url.indexOf("mid=")+4).trim());
                c_id = competition_id;
                //先循环赛季
                Long g_id=0L;
                Long r_id=0L;
                String table_type = "";
                String order_type = "";
                Long groups = 0L;
                String round_type = "";
                String type2 = "asia_229_e";
                HistoryParamsData.IsLeague isLeagueEnum = null;

                for (Element element: season_lists) {

                    Long s_id = Long.valueOf(element.id());
                    Elements a = element.getElementsByTag("a");
                    String season_and_name = a.html();
                    String url_seasons = urls + "&s_id=" + String.valueOf(s_id);
                    //System.out.println(url_seasons);
                    URL url_season = new URL(url_seasons);
                    URI uri_season = new URI(url_season.getProtocol(), url_season.getHost(), url_season.getPath(), url_season.getQuery(), null);
                    HttpGet httpget_season = new HttpGet(uri_season);
                    HttpResponse httpResponse_season = httpClient.execute(httpget_season);
                    HttpEntity entity_season = httpResponse_season.getEntity();
                    String body_season = EntityUtils.toString(entity_season, "gb2312");

                    Document document_season = Jsoup.parse(body_season);

                    //获取js里的变量值
                    Elements e = document_season.getElementsByAttributeValueContaining("language","JavaScript");
                    Element attrs = e.first();

                    String attr = attrs.data();
                    String[] att = attr.split("\r\n");

                    for (String at: att
                            ) {
                        if (at.contains("var")){

                            if(at.contains("g_id=")){
                                at = at.trim();
                                at = at.substring(at.indexOf("g_id=")+5,at.lastIndexOf(";"));
                                g_id = Long.valueOf(at);
                            }
                            if(at.contains("r_id=")){
                                at = at.trim();
                                at = at.substring(at.indexOf("r_id=")+5,at.lastIndexOf(";"));
                                if(at.equals("")){
                                    System.out.println("RID为空"+season_and_name);
                                    continue;
                                }else{
                                    r_id = Long.valueOf(at);
                                }
                            }
                            if(at.contains("table_type='")){
                                at = at.trim();
                                table_type = at.substring(at.indexOf("table_type='")+12,at.lastIndexOf("';"));
                            }
                            if(at.contains("order_type='")){
                                at = at.trim();
                                order_type = at.substring(at.indexOf("order_type='")+12,at.lastIndexOf("';"));
                            }
                            if(at.contains("groups=")){
                                at = at.trim();
                                at = at.substring(at.indexOf("groups=")+7,at.lastIndexOf(";"));
                                groups = Long.valueOf(at);
                            }
                            if(at.contains("round_type='")){
                                at = at.trim();
                                round_type = at.substring(at.indexOf("round_type='")+12,at.lastIndexOf("';"));
                            }
                            if(at.contains("is_league='")){
                                at = at.trim();
                                String is_league = at.substring(at.indexOf("is_league='")+11,at.lastIndexOf("';"));
                                if(is_league.equals("1")){

                                    isLeagueEnum = HistoryParamsData.IsLeague.Table;
                                }else if(is_league.equals("0")){
                                    isLeagueEnum = HistoryParamsData.IsLeague.Cup;
                                }
                            }
                        }
                    }
                    Integer totalnum = 0;

                    //只采集联赛
                    if(isLeagueEnum == HistoryParamsData.IsLeague.Cup || isLeagueEnum == null){
                        System.out.println("跳过联赛"+season_and_name);
                        continue;
                    }

                    //获取比赛总轮次
                    //常规赛还是普通联赛
                    if(round_type.equals("cup")){
                        //先获取常规赛轮次
                        HistoryParamsData historyParamsDatax = new HistoryParamsData();
                        Element element1 = document_season.getElementsByAttributeValue("round_type","table").first();
                        historyParamsDatax.setAction("round");
                        historyParamsDatax.setCompetitionId(competition_id);
                        historyParamsDatax.setCId(c_id);
                        r_id = Long.valueOf(element1.id());
                        historyParamsDatax.setRId(r_id);
                        historyParamsDatax.setGId(g_id);
                        historyParamsDatax.setTableType(table_type);
                        historyParamsDatax.setOrderType(order_type);
                        historyParamsDatax.setGroups(groups);
                        historyParamsDatax.setRoundType("table");
                        historyParamsDatax.setType1("three_-1_e");
                        historyParamsDatax.setType2(type2);
                        if(this.getTotalNums(historyParamsDatax) == null){
                            System.out.println("总轮次为空"+season_and_name);
                            continue;
                        }else{
                            totalnum = this.getTotalNums(historyParamsDatax);
                        }


                    }else if(round_type.equals("table")){
                        Elements league_nums = document_season.select("div.league_num");
                        if(league_nums.isEmpty()){
                            System.out.println("空leaguename:"+season_and_name);
                            continue;
                        }
                        Element league_num = league_nums.first();
                        Elements tds = league_num.getElementsByTag("td");
                        totalnum = Integer.valueOf(tds.last().id());
                    }


                    //循环初赔终赔
                    for (String[] act: actions) {
                        //检查是否已插入数据
                        BooleanExpression preciate;
                        QHistoryParamsData qHistoryParamsData = QHistoryParamsData.historyParamsData;
                        preciate = qHistoryParamsData.leagueName.eq(season_and_name).and(qHistoryParamsData.action.eq(act[0])).and(qHistoryParamsData.cId.eq(c_id))
                                .and(qHistoryParamsData.competitionId.eq(competition_id)).and(qHistoryParamsData.sId.eq(s_id)).and(qHistoryParamsData.rId.eq(r_id))
                                .and(qHistoryParamsData.gId.eq(g_id)).and(qHistoryParamsData.tableType.eq(table_type)).and(qHistoryParamsData.orderType.eq(order_type))
                                .and(qHistoryParamsData.groups.eq(groups)).and(qHistoryParamsData.roundType.eq(round_type)).and(qHistoryParamsData.type1.eq(act[1]))
                                .and(qHistoryParamsData.type2.eq(type2)).and(qHistoryParamsData.isLeague.eq(isLeagueEnum));
                        Iterable<HistoryParamsData> iterable1 = historyParamsDataRepository.findAll(preciate);
                        Iterator<HistoryParamsData> iterator1 = iterable1.iterator();
                        if(iterator1.hasNext()){
                            System.out.println("已插入跳过"+season_and_name);
                            continue;
                        }

                        HistoryParamsData historyParamsData = new HistoryParamsData();
                        historyParamsData.setLeagueName(season_and_name);
                        historyParamsData.setAction(act[0]);
                        historyParamsData.setCId(c_id);
                        historyParamsData.setCompetitionId(competition_id);
                        historyParamsData.setSId(s_id);
                        historyParamsData.setRId(r_id);
                        historyParamsData.setGId(g_id);
                        historyParamsData.setTableType(table_type);
                        historyParamsData.setOrderType(order_type);
                        historyParamsData.setGroups(groups);
                        historyParamsData.setRoundType(round_type);
                        historyParamsData.setType1(act[1]);
                        historyParamsData.setType2(type2);
                        historyParamsData.setWeek(1);
                        historyParamsData.setIsLeague(isLeagueEnum);
                        historyParamsData.setIsOk(0);
                        historyParamsData.setTotalNums(totalnum);
                        historyParamsDataRepository.save(historyParamsData);
                        System.out.println("开始处理"+season_and_name);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("完成");
        return "完成11";
    }

    /**
     * 联赛数据及终赔
     * */
    //@PostConstruct
    @SneakyThrows
    public @ResponseBody String tableDatas(){

        //获取数据库中未抓取过的联赛
        BooleanExpression preicate;
        QHistoryParamsData qHistoryParamsData = QHistoryParamsData.historyParamsData;
        //先更新终赔
        preicate = qHistoryParamsData.isOk.eq(0).and(qHistoryParamsData.action.eq("lc")).
                and(qHistoryParamsData.isLeague.eq(HistoryParamsData.IsLeague.Table));
        List<Sort.Order> orders = new ArrayList<>();
        orders.add(new Sort.Order(Sort.Direction.fromString("asc"),"id"));
        orders.add(new Sort.Order(Sort.Direction.fromString("asc"),"action"));

        Iterable<HistoryParamsData> iterable = historyParamsDataRepository.findAll(preicate,new Sort(orders));

        List<HistoryParamsData> historyParamsDataList = new ArrayList<>();
        iterable.forEach(historyData->{
            historyParamsDataList.add(historyData);
        });

        for (HistoryParamsData historyParamsData: historyParamsDataList) {
            Integer week = historyParamsData.getWeek();

            String seasonAndName = historyParamsData.getLeagueName();
            String leagueName = seasonAndName.substring(0,seasonAndName.indexOf("20"));
            String season = seasonAndName.substring(seasonAndName.indexOf("20"),seasonAndName.lastIndexOf("赛季"));

            Integer nums = historyParamsData.getTotalNums();

            while (week != -1){
                List<List<String>> matches;
                matches = this.getMatch(historyParamsData,week,false);

                System.out.println("开始处理"+seasonAndName+"week::"+week);

                try {
                    if (matches != null && matches.size() > 0) {
                        for (List<String> match : matches) {

                            HistoryMatchData historyMatchData = new HistoryMatchData();
                            historyMatchData.setHomeTeam(match.get(2));
                            historyMatchData.setAwayTeam(match.get(5));
                            String[] fullScore = match.get(4).split(":");
                            historyMatchData.setFullHomeScore(Long.valueOf(fullScore[0]));
                            historyMatchData.setFullAwayScore(Long.valueOf(fullScore[1]));

                            //更新获胜球队
                            if(Long.valueOf(fullScore[0]) > Long.valueOf(fullScore[1])){
                                historyMatchData.setWinTeam(match.get(2));
                            }else if(Long.valueOf(fullScore[0]) < Long.valueOf(fullScore[1])){
                                historyMatchData.setWinTeam(match.get(5));
                            }else{
                                historyMatchData.setWinTeam("平");
                            }

                            //更新赛季
                            historyMatchData.setSeason(season);
                            //更新联赛名称
                            historyMatchData.setLeagueName(leagueName);
                            //更新比赛轮次
                            historyMatchData.setWeek(String.valueOf(week));

                            //update by dl--更新半场各队得分
                            String[] halfSocre = match.get(3).split(":");
                            historyMatchData.setHalfHomeScore(Long.valueOf(halfSocre[0]));
                            historyMatchData.setHalfAwayScore(Long.valueOf(halfSocre[1]));

                            historyMatchData.setMatchDate(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(match.get(0)));

                            try {
                                historyMatchData.setJczqWinFinalOdds(new BigDecimal(match.get(6)).multiply(new BigDecimal("100")).longValue());
                            } catch (Exception ex) {
                                //忽略数字异常
                            }
                            try {
                                historyMatchData.setJczqDrawFinalOdds(new BigDecimal(match.get(7)).multiply(new BigDecimal("100")).longValue());
                            } catch (Exception ex) {
                                //忽略数字异常
                            }
                            try {
                                historyMatchData.setJczqLossFinalOdds(new BigDecimal(match.get(8)).multiply(new BigDecimal("100")).longValue());
                            } catch (Exception ex) {
                                //忽略数字异常
                            }
                            HistoryMatchData xx =historyMatchDataRepository.findAllByHomeTeamAndAwayTeamAndLeagueNameAndSeasonAndWeek
                                    (match.get(2),match.get(5), leagueName,season,String.valueOf(week));

                            if(xx == null){
                                historyMatchDataRepository.save(historyMatchData);
                            }


                        }
                        if(week.equals(nums)){
                            historyParamsData.setIsOk(1);
                            historyParamsDataRepository.save(historyParamsData);
                        }
                        historyParamsData.setWeek(week);
                        historyParamsDataRepository.save(historyParamsData);
                        week++;
                    } else {
                        week = -1;
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    System.exit(0);
                }
            }
        }
        return "ok";
    }

    /**
     * 获取联赛轮次
     * */
    @SneakyThrows
    private Integer getTotalNums(HistoryParamsData historyParamsData){
        HttpClient httpClient = HttpClientBuilder.create().build();
        HttpPost httppost =  new HttpPost("http://info.sporttery.cn/football/history/action.php");
        //组装参数
        List<NameValuePair> params = new ArrayList<>();
        String action  = historyParamsData.getAction();

        Long cId  = historyParamsData.getCId();
        Long competitionId  = historyParamsData.getCompetitionId();
        Long gId = historyParamsData.getGId();
        Long groups = historyParamsData.getGroups();
        String orderType = historyParamsData.getOrderType();
        Long rId = historyParamsData.getRId();
        String roundType = historyParamsData.getRoundType();
        Long sId = historyParamsData.getSId();
        String tableType = historyParamsData.getTableType();
        String type1 = historyParamsData.getType1();
        String type2 = historyParamsData.getType2();
        params.add(new BasicNameValuePair("action",action));
        params.add(new BasicNameValuePair("c_id",String.valueOf(cId)));
        params.add(new BasicNameValuePair("competition_id",String.valueOf(competitionId)));
        params.add(new BasicNameValuePair("g_id",String.valueOf(gId)));
        params.add(new BasicNameValuePair("groups",String.valueOf(groups)));
        params.add(new BasicNameValuePair("order_type",orderType));
        params.add(new BasicNameValuePair("r_id",String.valueOf(rId)));
        params.add(new BasicNameValuePair("round_type",roundType));
        params.add(new BasicNameValuePair("s_id",String.valueOf(sId)));
        params.add(new BasicNameValuePair("table_type",tableType));
        params.add(new BasicNameValuePair("type1",type1));
        params.add(new BasicNameValuePair("type2",type2));

        //System.out.println("开始处理"+seasonAndName+"action:"+action+"week::"+week);

        httppost.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
        HttpResponse httpresponse = httpClient.execute(httppost);
        HttpEntity entity = httpresponse.getEntity();
        //得到数据
        String body = EntityUtils.toString(entity, "UTF-8");

        Map<String,Object> bodym = new ObjectMapper().readValue(body,Map.class);
        if(bodym.containsKey("weeks") && DataUtils.toMap(bodym.get("weeks")).containsKey("this_week")){

            return  Integer.valueOf(DataUtils.toMap(bodym.get("weeks")).get("this_week").toString());
        }else{

            return null;
        }


    }

    /***
     * 获取比赛详情
     * */
    @SneakyThrows
    private List<List<String>> getMatch(HistoryParamsData historyParamsData,Integer week,Boolean isCup){

        HttpClient httpClient = HttpClientBuilder.create().build();
        HttpPost httppost =  new HttpPost("http://info.sporttery.cn/football/history/action.php");
        //组装参数
        List<NameValuePair> params = new ArrayList<>();
        String action  = historyParamsData.getAction();

        Long cId  = historyParamsData.getCId();
        Long competitionId  = historyParamsData.getCompetitionId();
        Long gId = historyParamsData.getGId();
        Long groups = historyParamsData.getGroups();
        String orderType = historyParamsData.getOrderType();
        Long rId = historyParamsData.getRId();
        String roundType = historyParamsData.getRoundType();
        Long sId = historyParamsData.getSId();
        String tableType = historyParamsData.getTableType();
        String type1 = historyParamsData.getType1();
        String type2 = historyParamsData.getType2();
        if(!isCup && roundType.equals("cup")) roundType = "table";

        params.add(new BasicNameValuePair("action",action));
        params.add(new BasicNameValuePair("c_id",String.valueOf(cId)));
        params.add(new BasicNameValuePair("competition_id",String.valueOf(competitionId)));
        params.add(new BasicNameValuePair("g_id",String.valueOf(gId)));
        params.add(new BasicNameValuePair("groups",String.valueOf(groups)));
        params.add(new BasicNameValuePair("order_type",orderType));
        params.add(new BasicNameValuePair("r_id",String.valueOf(rId)));
        params.add(new BasicNameValuePair("round_type",roundType));
        params.add(new BasicNameValuePair("s_id",String.valueOf(sId)));
        params.add(new BasicNameValuePair("table_type",tableType));
        params.add(new BasicNameValuePair("type1",type1));
        params.add(new BasicNameValuePair("type2",type2));
        if(!isCup){
            params.add(new BasicNameValuePair("week",String.valueOf(week)));
        }

        httppost.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
        HttpResponse httpresponse = httpClient.execute(httppost);
        HttpEntity entity = httpresponse.getEntity();
        //得到数据
        String body = EntityUtils.toString(entity, "UTF-8");
        if(body.contains("\"matches\":{\"result_str\":\" ") && body.lastIndexOf("<\\/table>\"")!=-1){

            body = body.substring(body.indexOf("\"matches\":{\"result_str\":\" "), body.lastIndexOf("<\\/table>\""));
            body = "<table>\n"
                    + body.substring(body.indexOf("<tr>"))
                    .replaceAll("\\\\r", "\n")
                    .replaceAll("\\\\/", "/")
                    .replaceAll("\\\\\"", "\"")
                    .replaceAll("</liid>", "</li>")
                    .replaceAll("&nbsp", "").replaceAll("gif\">", "gif\"/>")
                    + "\n</table>";
        }else{
            return null;
        }

        SAXParserFactory factory = SAXParserFactory.newInstance();
        SAXParser parser = factory.newSAXParser();
        XMLReader reader = parser.getXMLReader();
        List<List<String>> matches = new ArrayList<>();
        reader.setContentHandler(new DefaultHandler() {

            private List<String> match;

            @Override
            public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
                if (qName.equals("td")) {
                    if (match == null) {
                        match = new ArrayList<>();
                    }
                }
            }

            @Override
            public void endElement(String uri, String localName, String qName) throws SAXException {
                if (qName.equals("tr")) {
                    if (match != null) {
                        if (match.size() != 0) {
                            matches.add(match);
                        }
                        match = null;
                    }
                }
            }

            @Override
            public void characters(char[] ch, int start, int length) throws SAXException {
                if (match != null) {
                    String s = new String(ch, start, length).trim().replaceAll("   ", " ");
                    if (s.startsWith("\\u")) {
                        s = convertUnicode(s);
                    }
                    if(s.equals("一 一 一")){
                        match.add("一");
                        match.add("一");
                        match.add("一");
                    }else if(s.isEmpty()){
                        match.add("");
                    } else {
                        match.add(s);
                    }
                }
            }
        });
        reader.parse(new InputSource(IOUtils.toInputStream(body, "UTF-8")));
        return matches;
    }

    /***
     * 获取比赛详情--杯赛
     * */
    @SneakyThrows
    private List<List<String>> getMatchCup(HistoryParamsData historyParamsData){

//        HttpClient httpClient = HttpClientBuilder.create().build();
//        HttpPost httppost =  new HttpPost("http://info.sporttery.cn/football/history/action.php");

        //创建httpclient对象
        CloseableHttpClient httpClient = HttpClients.createDefault();
        //创建post方式请求对象
        HttpPost httppost = new HttpPost("http://info.sporttery.cn/football/history/action.php");

        //组装参数
        List<NameValuePair> params = new ArrayList<>();
        String action  = historyParamsData.getAction();

        Long cId  = historyParamsData.getCId();
        Long competitionId  = historyParamsData.getCompetitionId();
        Long gId = historyParamsData.getGId();
        Long groups = historyParamsData.getGroups();
        String orderType = historyParamsData.getOrderType();
        Long rId = historyParamsData.getRId();
        String roundType = historyParamsData.getRoundType();
        Long sId = historyParamsData.getSId();
        String tableType = historyParamsData.getTableType();
        String type1 = historyParamsData.getType1();
        String type2 = historyParamsData.getType2();

/*        if(cId==604&&rId==9263&&sId==2774){

            System.out.println(historyParamsData);
        }*/

        params.add(new BasicNameValuePair("action",action));
        params.add(new BasicNameValuePair("c_id",String.valueOf(cId)));
        params.add(new BasicNameValuePair("competition_id",String.valueOf(competitionId)));
        params.add(new BasicNameValuePair("g_id",String.valueOf(gId)));
        params.add(new BasicNameValuePair("groups",String.valueOf(groups)));
        params.add(new BasicNameValuePair("order_type",orderType));
        params.add(new BasicNameValuePair("r_id",String.valueOf(rId)));
        params.add(new BasicNameValuePair("round_type",roundType));
        params.add(new BasicNameValuePair("s_id",String.valueOf(sId)));
        params.add(new BasicNameValuePair("table_type",tableType));
        params.add(new BasicNameValuePair("type1",type1));
        params.add(new BasicNameValuePair("type2",type2));

        httppost.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
        try {
            //int code = httpClient.execute(httppost).getStatusLine().getStatusCode();
            //HttpResponse httpresponse = httpClient.execute(httppost);
//            httppost.setProtocolVersion(HttpVersion.HTTP_1_0);
//            httppost.addHeader(HTTP.CONN_DIRECTIVE, HTTP.CONN_CLOSE);

            CloseableHttpResponse response = httpClient.execute(httppost);
            HttpEntity entity = response.getEntity();
            //得到数据
            String body = EntityUtils.toString(entity, "UTF-8");
            response.close();
            httpClient.close();
            List<List<String>> matches = new ArrayList<>();
            try {
                JSONObject jsonObject = JSONObject.parseObject(body);
                if(jsonObject.containsKey("matches")){
                    //if result has week :foreach week
                    if(jsonObject.containsKey("weeks")){
                        JSONObject jsonObjectWeek = jsonObject.getJSONObject("weeks");
                        if(jsonObjectWeek.containsKey("rscode") && jsonObjectWeek.getString("rscode").equals("0")
                                && jsonObjectWeek.containsKey("this_week")){

                            Long weeks = Long.valueOf(jsonObjectWeek.getString("this_week"));
                            for (int i=1;i<=weeks;i++) {
                                //再次请求action.php
                                CloseableHttpClient httpClientWeek = HttpClients.createDefault();
                                //创建post方式请求对象
                                HttpPost httpPostWeek = new HttpPost("http://info.sporttery.cn/football/history/action.php");

                                //组装参数
                                String actionWeek = "lc";
                                params.remove(0);
                                params.add(new BasicNameValuePair("action",actionWeek));
                                httpPostWeek.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
                                CloseableHttpResponse responseWeek = httpClientWeek.execute(httpPostWeek);
                                HttpEntity entityWeek = responseWeek.getEntity();
                                //得到数据
                                String bodyWeek = EntityUtils.toString(entityWeek, "UTF-8");
                                response.close();
                                httpClient.close();

                                try{
                                    JSONObject jsonObjectWeekResults = JSONObject.parseObject(bodyWeek);
                                    if(jsonObjectWeekResults.containsKey("matches")){
                                        JSONObject jsonObjectWeekResult = jsonObjectWeekResults.getJSONObject("matches");
                                        if(jsonObjectWeekResult.containsKey("rscode") && jsonObjectWeekResult.getString("rscode").equals("0")){
                                            String resultsWeek = jsonObjectWeekResult.getString("result_str");
                                            Document documentWeek = Jsoup.parse(resultsWeek);
                                            Elements allElementsWeek = documentWeek.getElementsByTag("tr");
                                            //保留标题
                                            //去除标题
                                            allElementsWeek.remove(0);
                                            for (Element elementWeek: allElementsWeek
                                                    ) {
                                                Elements tdElementsWeek = elementWeek.getElementsByTag("td");
                                                Elements oddsElementsWeek = elementWeek.getElementsByIndexEquals(6).first().getElementsByTag("span");
                                                tdElementsWeek.remove(6);
                                                tdElementsWeek.remove(6);
                                                tdElementsWeek.remove(6);

                                                List<String> listWeek = new ArrayList<>();
                                                for (Element endElementWeek: tdElementsWeek
                                                        ) {
                                                    String result = endElementWeek.text();
                                                    listWeek.add(result);
                                                }
                                                for (Element oddElementWeek: oddsElementsWeek
                                                        ) {
                                                    if(oddElementWeek.text().equals("一 一 一")){
                                                        listWeek.add("一");
                                                    }else{
                                                        listWeek.add(oddElementWeek.text());
                                                    }
                                                }
                                                matches.add(listWeek);
                                            }
                                        }else{
                                            return null;
                                        }

                                    }else{
                                        return null;
                                    }
                                }catch (Exception e){
                                    e.printStackTrace();
                                }

                            }

                        }
                    }
                    if(jsonObject.containsKey("groups")){
                        JSONObject jsonObjectGroup = jsonObject.getJSONObject("groups");
                        if(jsonObjectGroup.containsKey("rscode") && jsonObjectGroup.getString("rscode").equals("0")
                                && jsonObjectGroup.containsKey("result_str")){
                            String groupsResults = jsonObjectGroup.getString("result_str");
                            Document documentGroup = Jsoup.parse(groupsResults);
                            Elements allElementsGroup = documentGroup.getElementsByTag("td");
                            int groups_g = allElementsGroup.size();
                            for (Element elementGroup:allElementsGroup) {

                                //再次请求action.php
                                CloseableHttpClient httpClientGroup = HttpClients.createDefault();
                                //创建post方式请求对象
                                HttpPost httpPostGroup = new HttpPost("http://info.sporttery.cn/football/history/action.php");

                                //组装参数
                                params.set(0,new BasicNameValuePair("action","group"));
                                params.set(3,new BasicNameValuePair("g_id",String.valueOf(elementGroup.id())));
                                params.set(4,new BasicNameValuePair("groups",String.valueOf(groups_g)));
                                httpPostGroup.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
                                CloseableHttpResponse responseGroup = httpClientGroup.execute(httpPostGroup);
                                HttpEntity entityGroup = responseGroup.getEntity();
                                //得到数据
                                String bodyGroup = EntityUtils.toString(entityGroup, "UTF-8");
                                response.close();
                                httpClient.close();

                                try{
                                    JSONObject jsonObjectGroupResults = JSONObject.parseObject(bodyGroup);
                                    if(jsonObjectGroupResults.containsKey("matches")){
                                        JSONObject jsonObjectGroupResult = jsonObjectGroupResults.getJSONObject("matches");
                                        if(jsonObjectGroupResult.containsKey("rscode") && jsonObjectGroupResult.getString("rscode").equals("0")){
                                            String resultsGroup = jsonObjectGroupResult.getString("result_str");
                                            Document documentGroupResult = Jsoup.parse(resultsGroup);
                                            Elements allElementsGroupResults = documentGroupResult.getElementsByTag("tr");
                                            //保留标题
                                            //去除标题
                                            allElementsGroupResults.remove(0);
                                            for (Element elementGroup_s: allElementsGroupResults
                                                    ) {
                                                Elements tdElementsGroup = elementGroup_s.getElementsByTag("td");
                                                Elements oddsElementsGroup = elementGroup_s.getElementsByIndexEquals(6).first().getElementsByTag("span");
                                                tdElementsGroup.remove(6);
                                                tdElementsGroup.remove(6);
                                                tdElementsGroup.remove(6);

                                                List<String> listWeek = new ArrayList<>();
                                                for (Element endElementWeek: tdElementsGroup
                                                        ) {
                                                    String result = endElementWeek.text();
                                                    listWeek.add(result);
                                                }
                                                for (Element oddElementWeek: oddsElementsGroup
                                                        ) {
                                                    if(oddElementWeek.text().equals("一 一 一") || oddElementWeek.text().isEmpty()){
                                                        listWeek.add("一");
                                                    }else{
                                                        listWeek.add(oddElementWeek.text());
                                                    }
                                                }
                                                matches.add(listWeek);
                                            }
                                        }else{
                                            return null;
                                        }

                                    }else{
                                        return null;
                                    }
                                }catch (Exception e){
                                    e.printStackTrace();
                                }

                            }
                        }
                    }
                    if(jsonObject.containsKey("matches") && !jsonObject.containsKey("weeks") && !jsonObject.containsKey("groups")){
                        JSONObject jsonObject1 = jsonObject.getJSONObject("matches");
                        if(jsonObject1.containsKey("rscode") && jsonObject1.getString("rscode").equals("0")){
                            String results = jsonObject1.getString("result_str");
                            Document document = Jsoup.parse(results);
                            Elements allElements = document.getElementsByTag("tr");
                            //保留标题
                            //去除标题
                            allElements.remove(0);
                            for (Element element: allElements
                                    ) {
                                Elements tdElements = element.getElementsByTag("td");
                                Elements oddsElements = element.getElementsByIndexEquals(6).first().getElementsByTag("span");
                                tdElements.remove(6);
                                tdElements.remove(6);
                                tdElements.remove(6);
                                List<String> list = new ArrayList<>();
                                for (Element endElement: tdElements
                                        ) {
                                    String result = endElement.text();
                                    list.add(result);
                                }
                                for (Element oddElement: oddsElements
                                        ) {
                                    if(oddElement.text().equals("一 一 一")){
                                        list.add("一");
                                    }else{
                                        list.add(oddElement.text());
                                    }
                                }
                                matches.add(list);
                            }
                        }else{
                            return null;
                        }
                    }
                }else{
                    return null;
                }
                return matches;
            } catch (Exception e) {
                System.out.println(body);
                e.printStackTrace();
                //System.exit(-1);
            }

        } catch (IOException e) {
            e.getMessage().equals("Connection reset");
            System.out.println(httppost);
            httpClient.close();
            e.printStackTrace();
        }
        return null;
    }

    public static String convertUnicode(String ori) {
        char aChar;
        int len = ori.length();
        StringBuffer outBuffer = new StringBuffer(len);
        for (int x = 0; x < len; ) {
            aChar = ori.charAt(x++);
            if (aChar == '\\') {
                aChar = ori.charAt(x++);
                if (aChar == 'u') {
                    // Read the xxxx
                    int value = 0;
                    for (int i = 0; i < 4; i++) {
                        aChar = ori.charAt(x++);
                        switch (aChar) {
                            case '0':
                            case '1':
                            case '2':
                            case '3':
                            case '4':
                            case '5':
                            case '6':
                            case '7':
                            case '8':
                            case '9':
                                value = (value << 4) + aChar - '0';
                                break;
                            case 'a':
                            case 'b':/**/
                            case 'c':
                            case 'd':
                            case 'e':
                            case 'f':
                                value = (value << 4) + 10 + aChar - 'a';
                                break;
                            case 'A':
                            case 'B':
                            case 'C':
                            case 'D':
                            case 'E':
                            case 'F':
                                value = (value << 4) + 10 + aChar - 'A';
                                break;
                            default:
                                throw new IllegalArgumentException(
                                        "Malformed   \\uxxxx   encoding.");
                        }
                    }
                    outBuffer.append((char) value);
                } else {
                    if (aChar == 't')
                        aChar = '\t';
                    else if (aChar == 'r')
                        aChar = '\r';
                    else if (aChar == 'n')
                        aChar = '\n';
                    else if (aChar == 'f')
                        aChar = '\f';
                    outBuffer.append(aChar);
                }
            } else
                outBuffer.append(aChar);
        }
        return outBuffer.toString();
    }

    /***
     * 更新联赛初赔
     */
    //@PostConstruct
    @SneakyThrows
    public String tableFirst(){
        BooleanExpression preciate;
        QHistoryParamsData qHistoryParamsData = QHistoryParamsData.historyParamsData;
        preciate = qHistoryParamsData.isOk.eq(0).and(qHistoryParamsData.action.eq("bc"))
        .and(qHistoryParamsData.isLeague.eq(HistoryParamsData.IsLeague.Table));

        List<Sort.Order> orders = new ArrayList<>();
        orders.add(new Sort.Order(Sort.Direction.fromString("asc"),"id"));
        orders.add(new Sort.Order(Sort.Direction.fromString("asc"),"action"));

        Iterable<HistoryParamsData> iterable = historyParamsDataRepository.findAll(preciate,new Sort(orders));

        List<HistoryParamsData> historyParamsDataList = new ArrayList<>();
        iterable.forEach(historyData->{
            historyParamsDataList.add(historyData);
        });

        for (HistoryParamsData historyParamsData: historyParamsDataList) {
            Integer week = historyParamsData.getWeek();

            String seasonAndName = historyParamsData.getLeagueName();
            String leagueName = seasonAndName.substring(0,seasonAndName.indexOf("20"));
            String season = seasonAndName.substring(seasonAndName.indexOf("20"),seasonAndName.lastIndexOf("赛季"));

            Integer nums = historyParamsData.getTotalNums();

            while (week != -1){
                List<List<String>> matches;
                matches = this.getMatch(historyParamsData,week,false);

                System.out.println("开始处理"+seasonAndName+"week::"+week+"action:bccc");

                try {
                    if (matches != null && matches.size() > 0) {
                        for (List<String> match : matches) {

                            HistoryMatchData old_historyMatchData =historyMatchDataRepository.findAllByHomeTeamAndAwayTeamAndLeagueNameAndSeasonAndWeek
                                    (match.get(2),match.get(5), leagueName,season,String.valueOf(week));

                            if(old_historyMatchData != null){
                                if(old_historyMatchData.getJczqWinFinalOdds() == null || old_historyMatchData.getJczqDrawFirstOdds() != null){
                                    continue;
                                }
                                try {
                                    old_historyMatchData.setJczqWinFirstOdds(new BigDecimal(match.get(6)).multiply(new BigDecimal("100")).longValue());
                                } catch (Exception ex) {
                                    //忽略数字异常
                                }
                                try {
                                    old_historyMatchData.setJczqDrawFirstOdds(new BigDecimal(match.get(7)).multiply(new BigDecimal("100")).longValue());
                                } catch (Exception ex) {
                                    //忽略数字异常
                                }
                                try {
                                    old_historyMatchData.setJczqLossFirstOdds(new BigDecimal(match.get(8)).multiply(new BigDecimal("100")).longValue());
                                } catch (Exception ex) {
                                    //忽略数字异常
                                }
                                historyMatchDataRepository.save(old_historyMatchData);
                            }
                        }
                        if(week.equals(nums)){
                            historyParamsData.setIsOk(1);
                            historyParamsDataRepository.save(historyParamsData);
                        }
                        historyParamsData.setWeek(week);
                        historyParamsDataRepository.save(historyParamsData);
                        week++;
                    } else {
                        week = -1;
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    System.exit(0);
                }
            }
        }
        return "11";
    }

    /**
     * 杯赛参数
     * */
    //@PostConstruct
    @SneakyThrows
    public String cupMatchParams(){

        String www = "http://info.sporttery.cn/football/history/";
        HttpClient httpClient = HttpClientBuilder.create().build();
        Iterable<String> iterable = historyLeagueDataRepository.findUri();
        Iterator<String> iterator = iterable.iterator();
        try {

            while (iterator.hasNext()){
                String url = iterator.next();
                String urls = www+url;
                iterator.remove();
                URL url_end = new URL(urls);
                URI uri = new URI(url_end.getProtocol(), url_end.getHost(), url_end.getPath(), url_end.getQuery(), null);
                HttpGet httpget = new HttpGet(uri);
                HttpResponse httpResponse = httpClient.execute(httpget);
                HttpEntity entity = httpResponse.getEntity();
                String body = EntityUtils.toString(entity, "gb2312");

                Document document = Jsoup.parse(body);

                //跳过联赛
                Elements e = document.getElementsByAttributeValueContaining("language","JavaScript");
                Element attrs = e.first();
                String attr = attrs.data();
                String[] att = attr.split("\r\n");
                Boolean is_table = false;
                for (String at: att
                        ) {
                    if(at.contains("is_league='")){
                        at = at.trim();
                        String is_league = at.substring(at.indexOf("is_league='")+11,at.lastIndexOf("';"));
                        if(is_league.equals("1")){
                            is_table = true;
                        }
                    }
                }
                if(is_table) continue;

                //先获取所有的赛季
                Element season_list = document.getElementById("season_list");
                Elements season_lists = season_list.getElementsByTag("li");

                //准备数据
                String[] action = {"round","three_-1_e"};//杯赛+竞彩终赔
                String[] action_bc = {"bc","three_-1_s"};//杯赛+竞彩初赔

                List<String[]> actions = new ArrayList<>();

                actions.add(action);
                actions.add(action_bc);

                Long c_id;
                Long competition_id = Long.valueOf(url.substring(url.indexOf("mid=")+4).trim());
                c_id = competition_id;
                //先循环赛季
                Long g_id=0L;
                Long r_id;
                String table_type = "";
                String order_type = "";
                Long groups = 0L;
                String round_type = "";
                String type2 = "asia_229_e";
                HistoryParamsData.IsLeague isLeagueEnum = null;

                for (Element element: season_lists) {

                    Long s_id = Long.valueOf(element.id());
                    Elements a = element.getElementsByTag("a");
                    String season_and_name = a.html();
                    String url_seasons = urls + "&s_id=" + String.valueOf(s_id);
                    URL url_season = new URL(url_seasons);
                    URI uri_season = new URI(url_season.getProtocol(), url_season.getHost(), url_season.getPath(), url_season.getQuery(), null);
                    HttpGet httpget_season = new HttpGet(uri_season);
                    HttpResponse httpResponse_season = httpClient.execute(httpget_season);
                    HttpEntity entity_season = httpResponse_season.getEntity();
                    String body_season = EntityUtils.toString(entity_season, "gb2312");

                    Document document_season = Jsoup.parse(body_season);

                    //获取js里的变量值
                    Elements e1 = document_season.getElementsByAttributeValueContaining("language","JavaScript");
                    Element attrs1 = e1.first();

                    String attr1 = attrs1.data();
                    String[] att1 = attr1.split("\r\n");

                    for (String at1: att1
                            ) {
                        if (at1.contains("var")){

                            if(at1.contains("g_id=")){
                                at1 = at1.trim();
                                at1 = at1.substring(at1.indexOf("g_id=")+5,at1.lastIndexOf(";"));
                                g_id = Long.valueOf(at1);
                            }
                            if(at1.contains("table_type='")){
                                at1 = at1.trim();
                                table_type = at1.substring(at1.indexOf("table_type='")+12,at1.lastIndexOf("';"));
                            }
                            if(at1.contains("order_type='")){
                                at1 = at1.trim();
                                order_type = at1.substring(at1.indexOf("order_type='")+12,at1.lastIndexOf("';"));
                            }
                            if(at1.contains("groups=")){
                                at1 = at1.trim();
                                at1 = at1.substring(at1.indexOf("groups=")+7,at1.lastIndexOf(";"));
                                groups = Long.valueOf(at1);
                            }
                            if(at1.contains("round_type='")){
                                at1 = at1.trim();
                                round_type = at1.substring(at1.indexOf("round_type='")+12,at1.lastIndexOf("';"));
                            }
                            isLeagueEnum = HistoryParamsData.IsLeague.Cup;
                        }
                    }

                    //获取season_name
                    Element element2 = document_season.select("table[class=league_name seasons]").first();
                    if(element2 != null){

                        Elements elements = element2.getElementsByTag("td");
                        for (Element element1 : elements){
                            //获取杯赛名称和r_id和round_type
                            r_id = Long.valueOf(element1.id());
                            if(r_id==0){
                                System.out.println("RID为0");
                                System.exit(-1);
                            }
                            String matchName = element1.getElementsByTag("a").first().html();

                            if(element1.hasAttr("round_type")){
                                round_type = element1.attr("round_type");
                            }
                            if(element1.hasAttr("groups")){
                                groups = Long.valueOf(element1.attr("groups"));
                            }

                            //循环初赔终赔
                            for (String[] act: actions) {
                                //检查是否已插入数据
                                BooleanExpression preciate;
                                QHistoryParamsData qHistoryParamsData = QHistoryParamsData.historyParamsData;
                                preciate = qHistoryParamsData.leagueName.eq(season_and_name).and(qHistoryParamsData.action.eq(act[0])).and(qHistoryParamsData.cId.eq(c_id))
                                        .and(qHistoryParamsData.competitionId.eq(competition_id)).and(qHistoryParamsData.sId.eq(s_id)).and(qHistoryParamsData.rId.eq(r_id))
                                        .and(qHistoryParamsData.gId.eq(g_id)).and(qHistoryParamsData.tableType.eq(table_type)).and(qHistoryParamsData.orderType.eq(order_type))
                                        .and(qHistoryParamsData.groups.eq(groups)).and(qHistoryParamsData.roundType.eq(round_type)).and(qHistoryParamsData.type1.eq(act[1]))
                                        .and(qHistoryParamsData.type2.eq(type2)).and(qHistoryParamsData.isLeague.eq(isLeagueEnum));
                                Iterable<HistoryParamsData> iterable1 = historyParamsDataRepository.findAll(preciate);
                                Iterator<HistoryParamsData> iterator1 = iterable1.iterator();
                                if(iterator1.hasNext()){
                                    System.out.println("已插入杯赛跳过"+season_and_name);
                                    continue;
                                }

                                HistoryParamsData historyParamsData = new HistoryParamsData();
                                historyParamsData.setLeagueName(season_and_name);
                                historyParamsData.setAction(act[0]);
                                historyParamsData.setCId(c_id);
                                historyParamsData.setCompetitionId(competition_id);
                                historyParamsData.setSId(s_id);
                                historyParamsData.setRId(r_id);
                                historyParamsData.setGId(g_id);
                                historyParamsData.setTableType(table_type);
                                historyParamsData.setOrderType(order_type);
                                historyParamsData.setGroups(groups);
                                historyParamsData.setRoundType(round_type);
                                historyParamsData.setType1(act[1]);
                                historyParamsData.setType2(type2);
                                historyParamsData.setWeek(1);
                                historyParamsData.setIsLeague(isLeagueEnum);
                                historyParamsData.setIsOk(0);
                                historyParamsData.setMatchName(matchName);
                                historyParamsDataRepository.save(historyParamsData);
                                System.out.println("开始处理杯赛"+season_and_name);
                            }
                        }
                        System.out.println("一个td循环完成");
                    }else{
                        System.out.println("空的杯赛season_name并且mid="+c_id+"并且赛季="+season_and_name);
                        System.exit(-1);
                    }
                }
                System.out.println("一个杯赛完成");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("完成");
        return "完成11";
    }

    /**
     * 杯赛数据以及终赔
     * */
    @PostConstruct
    @SneakyThrows
    public String cupDatas(){

        BooleanExpression preciate;
        QHistoryParamsData qHistoryParamsData = QHistoryParamsData.historyParamsData;
        preciate = qHistoryParamsData.isLeague.eq(HistoryParamsData.IsLeague.Cup).and(qHistoryParamsData.isOk.eq(0))
                .and(qHistoryParamsData.action.eq("round"));
        List<Sort.Order> orders = new ArrayList<>();
        orders.add(new Sort.Order(Sort.Direction.fromString("asc"),"id"));
        orders.add(new Sort.Order(Sort.Direction.fromString("asc"),"action"));
        Iterable<HistoryParamsData> iterable = historyParamsDataRepository.findAll(preciate,new Sort(orders));

        List<HistoryParamsData> historyParamsDataList = new ArrayList<>();
        iterable.forEach(historyData->{
            historyParamsDataList.add(historyData);
        });

        for (HistoryParamsData historyParamsData:historyParamsDataList){
            String seasonAndName = historyParamsData.getLeagueName();
            String leagueName = seasonAndName.substring(0,seasonAndName.indexOf("20"));
            String season = seasonAndName.substring(seasonAndName.indexOf("20"),seasonAndName.lastIndexOf("赛季"));
            String matchName = historyParamsData.getMatchName();

            List<List<String>> matches;
            matches = this.getMatchCup(historyParamsData);

          try {
                if(matches != null && matches.size()>0){
                    Boolean ok = true;
                    for (List<String> match: matches
                         ) {

                        String fullScore = match.get(4);
                        String halfScore = match.get(3);
                        if(fullScore.isEmpty() && halfScore.isEmpty() && season.equals("2017/2018")){
                            ok = false;
                            continue;
                        }
                        String homeTeam = match.get(2).replaceAll("\u0000", "");
                        String awayTeam = match.get(5).replaceAll("\u0000", "");
                        String date = match.get(0);

                        BooleanExpression expression;
                        QHistoryMatchData qHistoryMatchData = QHistoryMatchData.historyMatchData;
                        expression = qHistoryMatchData.homeTeam.eq(homeTeam).and(qHistoryMatchData.awayTeam.eq(awayTeam))
                                .and(qHistoryMatchData.season.eq(season)).and(qHistoryMatchData.leagueName.eq(leagueName))
                                .and(qHistoryMatchData.matchName.eq(matchName)).and(qHistoryMatchData.matchDate.eq(
                                        new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(date) ));
                        Optional<HistoryMatchData> optional = historyMatchDataRepository.findOne(expression);
                        HistoryMatchData oldhistoryMatchData = optional.orElse(null);
                        if(oldhistoryMatchData == null){
                            //没有插入过的话就插入
                            HistoryMatchData historyMatchData = new HistoryMatchData();
                            historyMatchData.setHomeTeam(homeTeam);
                            historyMatchData.setAwayTeam(awayTeam);
                            if(!fullScore.isEmpty() && fullScore.length()>1){

                                String[] fullScores = fullScore.split(":");
                                Long fullHomeScore = Long.valueOf(fullScores[0]);
                                Long fullAwayScore = Long.valueOf(fullScores[1]);
                                historyMatchData.setFullHomeScore(fullHomeScore);
                                historyMatchData.setFullAwayScore(fullAwayScore);
                                //更新获胜球队
                                if(fullHomeScore > fullAwayScore){
                                    historyMatchData.setWinTeam(homeTeam);
                                }else if(fullHomeScore < fullAwayScore){
                                    historyMatchData.setWinTeam(awayTeam);
                                }else{
                                    historyMatchData.setWinTeam("平");
                                }
                            }
                            if(!halfScore.isEmpty()){

                                String[] halfScores = halfScore.split(":");
                                Long halfHomeScore = Long.valueOf(halfScores[0]);
                                Long halfAwayScore = Long.valueOf(halfScores[1]);
                                historyMatchData.setHalfHomeScore(halfHomeScore);
                                historyMatchData.setHalfAwayScore(halfAwayScore);
                            }

                            //更新赛季
                            historyMatchData.setSeason(season);
                            //更新联赛名称
                            historyMatchData.setLeagueName(leagueName);
                            //更新比赛名称
                            historyMatchData.setMatchName(matchName);
                            //更新比赛日期
                            historyMatchData.setMatchDate(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(date));
                            String winFinalOdds = match.get(6);
                            String drawFinalOdds = match.get(7);
                            String lossFinalOdds = match.get(8);
                            try {
                                historyMatchData.setJczqWinFinalOdds(new BigDecimal(winFinalOdds).multiply(new BigDecimal("100")).longValue());
                            } catch (Exception ex) {
                                //忽略数字异常
                            }
                            try {
                                historyMatchData.setJczqDrawFinalOdds(new BigDecimal(drawFinalOdds).multiply(new BigDecimal("100")).longValue());
                            } catch (Exception ex) {
                                //忽略数字异常
                            }
                            try {
                                historyMatchData.setJczqLossFinalOdds(new BigDecimal(lossFinalOdds).multiply(new BigDecimal("100")).longValue());
                            } catch (Exception ex) {
                                //忽略数字异常
                            }
                            historyMatchDataRepository.save(historyMatchData);
                        }else{
                            System.out.println("跳过"+leagueName+season+matchName);
                        }

                        System.out.println("完成"+leagueName+season+matchName);
                    }
                    //如果都采集完的话就更新Ok
                    QHistoryMatchData qHistoryMatchData = QHistoryMatchData.historyMatchData;
                    BooleanExpression expression1 = qHistoryMatchData.leagueName.eq(leagueName).and(qHistoryMatchData.season.eq(season))
                            .and(qHistoryMatchData.matchName.eq(matchName));
                    Long matchLength = historyMatchDataRepository.count(expression1);
                    if(matches.size() == matchLength && ok){
                        historyParamsData.setIsOk(1);
                        historyParamsDataRepository.save(historyParamsData);
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
            System.out.println("完成"+historyParamsData.getLeagueName());
        }
        return "ok";
    }

    /**
     * 杯赛初赔
     * */
    //@PostConstruct
    @SneakyThrows
    public String cupFirst(){
        BooleanExpression preciate;
        QHistoryParamsData qHistoryParamsData = QHistoryParamsData.historyParamsData;
        preciate = qHistoryParamsData.isOk.eq(0).and(qHistoryParamsData.action.eq("bc")).
                and(qHistoryParamsData.isLeague.eq(HistoryParamsData.IsLeague.Cup));

        List<Sort.Order> orders = new ArrayList<>();
        orders.add(new Sort.Order(Sort.Direction.fromString("asc"),"id"));
        orders.add(new Sort.Order(Sort.Direction.fromString("asc"),"action"));

        Iterable<HistoryParamsData> iterable = historyParamsDataRepository.findAll(preciate,new Sort(orders));

        List<HistoryParamsData> historyParamsDataList = new ArrayList<>();
        iterable.forEach(historyData->{
            historyParamsDataList.add(historyData);
        });

        for (HistoryParamsData historyParamsData:historyParamsDataList){
            String seasonAndName = historyParamsData.getLeagueName();
            String leagueName = seasonAndName.substring(0,seasonAndName.indexOf("20"));
            String season = seasonAndName.substring(seasonAndName.indexOf("20"),seasonAndName.lastIndexOf("赛季"));
            String matchName = historyParamsData.getMatchName();

            List<List<String>> matches;
            if(historyParamsData.getCId()==856 && historyParamsData.getRId()==8223){

                System.out.println(historyParamsData);
            }
            matches = this.getMatchCup(historyParamsData);


            try {

                if(matches != null && matches.size()>0){
                    Boolean ok = true;
                    for (List<String> match: matches
                            ) {

                        String winFinalOdds = match.get(6);
                        String drawFinalOdds = match.get(7);
                        String lossFinalOdds = match.get(8);
                        String fullScore = match.get(4);
                        String halfScore = match.get(3);
                        if(fullScore.isEmpty() && halfScore.isEmpty() && season.equals("2017/2018")){
                            ok = false;
                            continue;
                        }
                        if(winFinalOdds.isEmpty() || drawFinalOdds.isEmpty() || lossFinalOdds.isEmpty()) continue;
                        String homeTeam = match.get(2).replaceAll("\u0000", "");
                        String awayTeam = match.get(5).replaceAll("\u0000", "");
                        String date = match.get(0);

                        BooleanExpression expression;
                        QHistoryMatchData qHistoryMatchData = QHistoryMatchData.historyMatchData;
                        expression = qHistoryMatchData.homeTeam.eq(homeTeam).and(qHistoryMatchData.awayTeam.eq(awayTeam))
                                .and(qHistoryMatchData.season.eq(season)).and(qHistoryMatchData.leagueName.eq(leagueName))
                                .and(qHistoryMatchData.matchName.eq(matchName)).and(qHistoryMatchData.matchDate.eq(
                                        new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(date) ));
                        Optional<HistoryMatchData> optional = historyMatchDataRepository.findOne(expression);
                        HistoryMatchData oldhistoryMatchData = optional.orElse(null);
                        if(oldhistoryMatchData != null){
                            //数据库中存在
                            try {
                                oldhistoryMatchData.setJczqWinFirstOdds(new BigDecimal(winFinalOdds).multiply(new BigDecimal("100")).longValue());
                            } catch (Exception ex) {
                                //忽略数字异常
                            }
                            try {
                                oldhistoryMatchData.setJczqDrawFirstOdds(new BigDecimal(drawFinalOdds).multiply(new BigDecimal("100")).longValue());
                            } catch (Exception ex) {
                                //忽略数字异常
                            }
                            try {
                                oldhistoryMatchData.setJczqLossFirstOdds(new BigDecimal(lossFinalOdds).multiply(new BigDecimal("100")).longValue());
                            } catch (Exception ex) {
                                //忽略数字异常
                            }
                            historyMatchDataRepository.save(oldhistoryMatchData);
                        }else{
                            System.out.println("跳过"+leagueName+season+matchName);
                        }

                        System.out.println("完成"+leagueName+season+matchName);
                    }
                    //如果都采集完的话就更新Ok
                    QHistoryMatchData qHistoryMatchData = QHistoryMatchData.historyMatchData;
                    BooleanExpression expression1 = qHistoryMatchData.leagueName.eq(leagueName).and(qHistoryMatchData.season.eq(season))
                            .and(qHistoryMatchData.matchName.eq(matchName));
                    Long matchLength = historyMatchDataRepository.count(expression1);
                    if(matches.size() == matchLength && ok){
                        historyParamsData.setIsOk(1);
                        historyParamsDataRepository.save(historyParamsData);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            System.out.println("完成"+historyParamsData.getLeagueName());
        }
        return "11";
    }
}
