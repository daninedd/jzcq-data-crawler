package com.caile.jczq.data.crawler;

import com.querydsl.core.types.dsl.BooleanExpression;
import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import javax.annotation.Resource;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URL;
import java.text.SimpleDateFormat;
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
    @Resource
    private HistoryParamsDataRepository historyParamsDataRepository;
    @Resource
    private HistoryMatchDataRepository historyMatchDataRepository;

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

    @RequestMapping("/league_match")
    @SneakyThrows
    public @ResponseBody String league_match(){

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

                    //获取比赛总轮次
                    Elements league_nums = document_season.select("div.league_num");
                    if(league_nums.isEmpty()){
                        continue;
                    }
                    Element league_num = league_nums.first();
                    Elements tds = league_num.getElementsByTag("td");
                    Integer totalnum = Integer.valueOf(tds.last().id());


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
                                r_id = Long.valueOf(at);
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

                    //只采集联赛
                    if(isLeagueEnum == HistoryParamsData.IsLeague.Cup || isLeagueEnum == null){
                        continue;
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
                                .and(qHistoryParamsData.type2.eq(type2)).and(qHistoryParamsData.isLeague.eq(isLeagueEnum)).and(qHistoryParamsData.isOk.eq(0));
                        Iterable<HistoryParamsData> iterable1 = historyParamsDataRepository.findAll(preciate);
                        Iterator<HistoryParamsData> iterator1 = iterable1.iterator();
                        if(iterator1.hasNext()){
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

    @RequestMapping("/lc_match_details")
    @SneakyThrows
    public @ResponseBody String lc_match_details(){

        //获取数据库中未抓取过的联赛
        BooleanExpression preicate;
        QHistoryParamsData qHistoryParamsData = QHistoryParamsData.historyParamsData;
        //先更新终赔
        preicate = qHistoryParamsData.isOk.eq(0).and(qHistoryParamsData.action.eq("lc"));
        List<Sort.Order> orders = new ArrayList<>();
        orders.add(new Sort.Order(Sort.Direction.fromString("asc"),"id"));
        orders.add(new Sort.Order(Sort.Direction.fromString("asc"),"action"));

        Iterable<HistoryParamsData> iterable = historyParamsDataRepository.findAll(preicate,new Sort(orders));

        List<HistoryParamsData> historyParamsDataList = new ArrayList<>();
        iterable.forEach(historyData->{
            historyParamsDataList.add(historyData);
        });

        HttpClient httpClient = HttpClientBuilder.create().build();
        HttpPost httppost = new HttpPost("http://info.sporttery.cn/football/history/action.php");
        for (HistoryParamsData historyParamsData: historyParamsDataList) {
            Integer week = historyParamsData.getWeek();
            //int i = 0;
            while (week != -1){
                //组装参数
                List<NameValuePair> params = new ArrayList<>();
                String seasonAndName = historyParamsData.getLeagueName();
                String leagueName = seasonAndName.substring(0,seasonAndName.indexOf("20"));
                String season = seasonAndName.substring(seasonAndName.indexOf("20"),seasonAndName.lastIndexOf("赛季"));
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
                params.add(new BasicNameValuePair("week",String.valueOf(week)));

                System.out.println("开始处理"+seasonAndName+"action:"+action+"week::"+week);

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
                    break;
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
                                if (14 == match.size()) {
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
                            }else {
                                match.add(s);
                            }
                        }
                    }
                });
                reader.parse(new InputSource(IOUtils.toInputStream(body, "UTF-8")));

                try {
                    if (matches.size() > 0) {
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

                            historyMatchDataRepository.save(historyMatchData);
                        }
                        historyParamsData.setWeek(week);
                        week++;
                    } else {
                        week = -1;
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    System.out.println(body);
                    System.exit(0);
                }
            }

            historyParamsData.setIsOk(1);
            historyParamsDataRepository.save(historyParamsData);


        }
        return "ok";
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


}
