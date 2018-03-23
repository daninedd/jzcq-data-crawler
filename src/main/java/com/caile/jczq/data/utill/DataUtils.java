package com.caile.jczq.data.utill;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.springframework.core.convert.ConversionService;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.lang.reflect.Method;
import java.util.*;

public class DataUtils {

    private static DataUtils         INSTANCE;

    @Resource
    private ConversionService conversionService;

    @Resource
    private ObjectMapper objectMapper;
//
//    @Resource
//    private        PasswordEncoder   passwordEncoder;

    /**
     * 将数组转换为列表
     *
     * @param array
     * @param <T>
     * @return
     */
    public static <T> List<T> toList(T... array) {
        return Arrays.asList(array);
    }

//    /**
//     * 获取文本的信息指纹
//     *
//     * @param obj
//     * @return
//     */
//    public static String fingerprint(Object obj) {
//        return INSTANCE.passwordEncoder.encode(obj.toString());
//    }

//    /**
//     * 分转为元
//     *
//     * @param amount
//     * @return
//     */
//    public static BigDecimal toCurrency(long amount) {
//        return new BigDecimal(amount).divide(new BigDecimal(DataConst.CURRENCY_FRACTION), 2, BigDecimal.ROUND_DOWN);
//    }

//    /**
//     * 元转为分
//     *
//     * @param amount
//     * @return
//     */
//    public static BigDecimal toCent(BigDecimal amount) {
//        return amount.multiply(new BigDecimal(DataConst.CURRENCY_FRACTION)).setScale(2, BigDecimal.ROUND_DOWN);
//    }

    /**
     * 转换对象
     *
     * @param obj
     * @param type
     * @param <T>
     * @return
     */
    @SneakyThrows
    public static <T> T convert(Object obj, Class<T> type) {
        if (obj instanceof String && Map.class.isAssignableFrom(type)) {
            Assert.notNull(obj, "转换对象不能为空");
            return (T) INSTANCE.objectMapper.readValue(obj.toString(), Map.class);
        } else if (obj instanceof String && List.class.isAssignableFrom(type)) {
            Assert.notNull(obj, "转换对象不能为空");
            return (T) INSTANCE.objectMapper.readValue(obj.toString(), List.class);
        }
        return INSTANCE.conversionService.convert(obj, type);
    }

    /**
     * 隐藏手机号中间4位
     *
     * @param mobile
     * @return
     */
    public static String maskMobile(String mobile) {
        return mobile.replaceAll("(\\d{3})\\d{4}(\\d{4})", "$1****$2");
    }

    /**
     * 隐藏18位身份证号码
     *
     * @param idCard
     * @return
     */
    public static String maskIdCard(String idCard) {
        return idCard.replaceAll("(\\d{3})\\d{12}(\\w{3})", "$1*****$2");
    }



    /**
     * 转换成Json字符串
     *
     * @param obj
     * @return
     */
    @SneakyThrows
    public static String toJson(Object obj) {
        return INSTANCE.objectMapper.writeValueAsString(obj);
    }

    /**
     * 将列表转换数组
     *
     * @param list
     * @param <T>
     * @return
     */
    public static <T> T[] toArray(List<T> list) {
        return list.toArray((T[]) java.lang.reflect.Array.newInstance(list.get(0).getClass(), 0));
    }

    /**
     * 将Long型列表转换为long数组
     *
     * @param list
     * @return
     */
    public static long[] toPrimitiveLongs(List<Long> list) {
        Long[] array = list.toArray(new Long[0]);
        if (array == null) {
            return null;
        } else if (array.length == 0) {
            return new long[0];
        } else {
            long[] result = new long[array.length];
            for (int i = 0; i < array.length; ++i) {
                result[i] = array[i].longValue();
            }
            return result;
        }
    }

    /**
     * 将值转换为long
     *
     * @param value
     * @return
     */
    public static long toPrimitiveLong(Object value) {
        Assert.notNull(value, "无法把空值转换为long类型");
        return Long.parseLong(value.toString());
    }

    /**
     * 将对象转换为Map，如果传入了keys，则只取keys中定义的属性
     *
     * @param object
     * @param keys
     * @return
     */
    public static Map<String, Object> toMap(Object object, String... keys) {
        Map<String, Object> map;
        if (object instanceof Map) {
            map = new LinkedHashMap<>((Map) object);
        } else {
            map = INSTANCE.objectMapper.convertValue(object, Map.class);
            for (Method method : ReflectionUtils.getAllDeclaredMethods(object.getClass())) {
                String name = method.getName();
                if (name.startsWith("get")) {
                    Object value = ReflectionUtils.invokeMethod(method, object);
                    if (name.equals("getId")) {
                        map.put("id", value);
                        continue;
                    }
                    String char1 = name.substring(3, 4);
                    String key = StringUtils.uncapitalize(name.substring(3));
                    //处理ObjectMapper对连续大写字母的名称处理会全部转化为小写的问题，这里格式化为驼峰
                    if (name.length() > 4) {
                        String char2 = name.substring(4, 5);
                        if (char2.toUpperCase().equals(char2)) {
                            Object removedObj = map.remove(char1.toLowerCase() + StringUtils.uncapitalize(key.substring(1)));
                            if (!map.containsKey(key)) {
                                map.put(key, removedObj);
                            }
                        }
                    }
                }
            }
        }
        if (null != keys && 0 < keys.length) {
            out:
            for (Map.Entry<String, Object> entry : new HashSet<>(map.entrySet())) {
                String mapKey = entry.getKey();
                if (null != entry.getValue()) {
                    for (String key : keys) {
                        if (key.equals(mapKey)) {
                            continue out;
                        }
                    }
                }
                map.remove(mapKey);
            }
        }
        return map;
    }

    /**
     * 必须获得数据
     *
     * @param map
     * @param name
     * @param type
     * @param <T>
     * @return
     */
    public static <T> T require(Map<String, Object> map, String name, Class<T> type) {
        Object obj = map.get(name);
        if (StringUtils.isEmpty(obj) || !StringUtils.hasText(obj.toString())) {
            throw new IllegalArgumentException(name + "的值不能为空");
        } else {
            return convert(obj, type);
        }
    }

    /**
     * 必须获得Long型数据
     *
     * @param map
     * @param name
     * @return
     */
    public static Long requireLong(Map<String, Object> map, String name) {
        return require(map, name, Long.class);
    }

    /**
     * 是否在当前月
     *
     * @param date
     * @return
     */
    public static boolean isDateInCurrentMonth(Date date) {
        if (null != date) {
            Calendar current = Calendar.getInstance();
            Calendar target = new Calendar.Builder().setInstant(date).build();
            return current.get(Calendar.YEAR) == target.get(Calendar.YEAR) && current.get(Calendar.MONTH) == target.get(Calendar.MONTH);
        }
        return false;
    }

    /**
     * 必须获得Id
     *
     * @param map
     * @return
     */
    public static long requireId(Map<String, Object> map) {
        return requireLong(map, "id");
    }

    /**
     * 必须获得字符串
     *
     * @param map
     * @param name
     * @return
     */
    public static String requireStr(Map<String, Object> map, String name) {
        Object obj = map.get(name);
        if (StringUtils.isEmpty(obj) || !StringUtils.hasText(obj.toString())) {
            throw new IllegalArgumentException(name + "的值不能为空白字符串");
        } else {
            return convert(obj, String.class);
        }
    }

    /**
     * 尝试获取值，没有则返回null
     *
     * @param map
     * @param name
     * @param type
     * @param <T>
     * @return
     */
    public static <T> T optional(Map<String, Object> map, String name, Class<T> type) {
        return defaultVal(map, name, type, null);
    }

    /**
     * 尝试获取字符串，没有则返回null
     *
     * @param map
     * @param name
     * @return
     */
    public static String optionalStr(Map<String, Object> map, String name) {
        return optional(map, name, String.class);
    }

    /**
     * 尝试获取Long型数据，没有则返回null
     *
     * @param map
     * @param name
     * @return
     */
    public static Long optionalLong(Map<String, Object> map, String name) {
        return optional(map, name, Long.class);
    }

    /**
     * 尝试获取值，没有则返回设定的默认值
     *
     * @param map
     * @param name
     * @param type
     * @param defaultValue
     * @param <T>
     * @return
     */
    public static <T> T defaultVal(Map<String, Object> map, String name, Class<T> type, T defaultValue) {
        Object obj = map.get(name);
        if (obj == null) {
            return defaultValue;
        } else {
            return convert(obj, type);
        }
    }

    /**
     * 尝试获取字符串，没有则返回空白字符串
     *
     * @param map
     * @param name
     * @return
     */
    public static String defaultStr(Map<String, Object> map, String name) {
        return defaultVal(map, name, String.class, "");
    }

    /**
     * 取指定范围内的一个随机整数
     *
     * @param min
     * @param max
     * @return
     */
    public static int randomInt(int min, int max) {
        if (min == max) {
            return min;
        }
        return (int) (Math.random() * (max - min + 1) + min);
    }

    @PostConstruct
    private void setUp() {
        INSTANCE = this;
    }
}