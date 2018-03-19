package com.caile.jczq.data.crawler;

import com.querydsl.core.types.dsl.BooleanExpression;
import lombok.SneakyThrows;
import lombok.experimental.Delegate;
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
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class JCZQCrawler {

    @Resource
    private HistoryMatchDataRepository historyMatchDataRepository;

    @Delegate
    @Resource
    private HistoryParamsDataRepository historyParamsDataRepository;

    @Resource
    private ResourceLoader             resourceLoader;

 //   @PostConstruct
    @SneakyThrows
    public void craw() {
        System.exit(0);
        HttpClient httpClient = HttpClientBuilder.create().build();
        HttpPost httppost = new HttpPost("http://info.sporttery.cn/football/history/action.php");
        String[] blocks = IOUtils.toString(resourceLoader.getResource("classpath:league-api-params.list").getInputStream(), "UTF-8").split("#");
        for (String block : blocks) {
            block = block.trim();
            if (!StringUtils.isEmpty(block)) {
                int i = 0;
                while (i != -1) {
                    i++;
                    String seasonAndName = block.split("\n")[0];
                    String leagueName = block.substring(0,seasonAndName.length()-9);
                    String season = block.substring(seasonAndName.length()-9,seasonAndName.length());
                    Map<String,Object> conditions = new HashMap<>();

                    for (String lists : block.split("\n")){

                        if(lists.contains(":")){
                            String[] listsstr = lists.split(":");
                            conditions.put(listsstr[0],listsstr[1]);
                        }
                    }

                    //查询是否被采集过数据
                    String action = conditions.get("action").toString();
                    Long c_id = Long.valueOf((conditions.get("c_id")).toString());
                    Long competition_id = Long.valueOf(conditions.get("competition_id").toString());
                    Long s_id = Long.valueOf(conditions.get("s_id").toString());
                    Long r_id = Long.valueOf(conditions.get("r_id").toString());
                    Long g_id = Long.valueOf(conditions.get("g_id").toString());
                    String table_type = conditions.get("table_type").toString();
                    String order_type = conditions.get("order_type").toString();
                    Long groups = Long.valueOf(conditions.get("groups").toString());
                    String round_type = conditions.get("round_type").toString();
                    String type1 = conditions.get("type1").toString();
                    String type2 = conditions.get("type2").toString();
                    BooleanExpression predicate;
                    QHistoryParamsData qHistoryParamsData = QHistoryParamsData.historyParamsData;
                    predicate = qHistoryParamsData.leagueName.eq(seasonAndName).and(qHistoryParamsData.action.eq(action))
                            .and(qHistoryParamsData.week.eq(i))
                            .and(qHistoryParamsData.cId.eq(c_id))
                            .and(qHistoryParamsData.competitionId.eq(competition_id))
                            .and(qHistoryParamsData.sId.eq(s_id))
                            .and(qHistoryParamsData.rId.eq(r_id))
                            .and(qHistoryParamsData.gId.eq(g_id))
                            .and(qHistoryParamsData.tableType.eq(table_type))
                            .and(qHistoryParamsData.orderType.eq(order_type))
                            .and(qHistoryParamsData.groups.eq(groups))
                            .and(qHistoryParamsData.roundType.eq(round_type))
                            .and(qHistoryParamsData.type1.eq(type1))
                            .and(qHistoryParamsData.type2.eq(type2));

                    Iterable<HistoryParamsData> iterable = historyParamsDataRepository.findAll(predicate);
                    if(iterable.iterator().hasNext()){
                        continue;
                    }

                    System.out.println("开始处理" + seasonAndName + ": week " + i);

                    List<NameValuePair> params = new ArrayList<>();
                    for (String srcParam : block.split("\n")) {
                        //String season
                        if (srcParam.contains(":")) {
                            String[] values = srcParam.split(":");
                            if (values[0].equals("week")) {
                                params.add(new BasicNameValuePair("week", String.valueOf(i)));
                            } else {
                                params.add(new BasicNameValuePair(values[0], values[1]));
                            }
                        }
                    }
                    httppost.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
                    HttpResponse httpresponse = httpClient.execute(httppost);
                    HttpEntity entity = httpresponse.getEntity();
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
                      //      System.out.println(body);
                    //        System.out.println(list);
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
                                historyMatchData.setWeek(String.valueOf(i));

                                //update by dl--更新半场各队得分
                                String[] halfSocre = match.get(3).split(":");
                                historyMatchData.setHalfHomeScore(Long.valueOf(halfSocre[0]));
                                historyMatchData.setHalfAwayScore(Long.valueOf(halfSocre[1]));

                                historyMatchData.setMatchDate(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(match.get(0)));

                                //update by dl--区分终赔和初赔更新
                                String endOrStart = params.get(11).getValue();
                                String last = endOrStart.substring(endOrStart.length()-1,endOrStart.length());

                                if(last.equals("s")){//初赔

                                    historyMatchData = historyMatchDataRepository.findAllByHomeTeamAndAwayTeamAndLeagueNameAndSeasonAndWeek
                                                (match.get(2),match.get(5),leagueName,season,String.valueOf(i));

                                    if(historyMatchData!=null){

                                        try {
                                            historyMatchData.setJczqWinFirstOdds(new BigDecimal(match.get(6)).multiply(new BigDecimal("100")).longValue());
                                        } catch (Exception ex) {
                                            //忽略数字异常
                                        }
                                        try {
                                            historyMatchData.setJczqDrawFirstOdds(new BigDecimal(match.get(7)).multiply(new BigDecimal("100")).longValue());
                                        } catch (Exception ex) {
                                            //忽略数字异常
                                        }
                                        try {
                                            historyMatchData.setJczqLossFirstOdds(new BigDecimal(match.get(8)).multiply(new BigDecimal("100")).longValue());
                                        } catch (Exception ex) {
                                            //忽略数字异常
                                        }
                                    }else{

                                        continue;
                                    }

                                }else if(last.equals("e")){//终赔
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

                                }else{

                                    System.out.println("未知type1");
                                    System.exit(-1);
                                }
                                historyMatchDataRepository.save(historyMatchData);
                            }
                            //将已经采集过数据的list存进数据库，防止多次采集
                            HistoryParamsData historyParamsData = new HistoryParamsData();
                            historyParamsData.setLeagueName(seasonAndName);
                            historyParamsData.setAction(action);
                            historyParamsData.setCId(c_id);
                            historyParamsData.setCompetitionId(competition_id);
                            historyParamsData.setSId(s_id);
                            historyParamsData.setRId(r_id);
                            historyParamsData.setGId(g_id);
                            historyParamsData.setTableType(table_type);
                            historyParamsData.setOrderType(order_type);
                            historyParamsData.setGroups(groups);
                            historyParamsData.setRoundType(round_type);
                            historyParamsData.setType1(type1);
                            historyParamsData.setType2(type2);
                            historyParamsData.setLeagueName(seasonAndName);
                            historyParamsData.setLeagueName(seasonAndName);
                            historyParamsData.setWeek(i);
                            historyParamsDataRepository.save(historyParamsData);
                        } else {
                            i = -1;
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        System.out.println(body);
                        System.exit(0);
                    }
                }
            }
        }
    }

//    @PostConstruct
//    @SneakyThrows
//    public void leagueCraw(){
//
//        System.out.println("123");
//        System.exit(0);
//        HttpClient httpClient = HttpClientBuilder.create().build();
//        HttpGet httpGet = new HttpGet("http://info.sporttery.cn/football/history/data_center.php");
//
//    }

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
                        case 'b':
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
