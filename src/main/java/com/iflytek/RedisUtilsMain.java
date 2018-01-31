package com.iflytek;

import org.apache.log4j.Logger;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

/**
 * 连接redis-twemproxy模式的工具，解析dmp标签数据
 *
 * @author 10530
 * @date 2018/1/30
 */
public final class RedisUtilsMain {

    private static Logger log = Logger.getLogger(RedisUtilsMain.class);
    /**
     * Redis服务器IP 10.200.63.180
     * Redis的端口号 22122
     * 访问密码 xylxredis123!@#mima
     */
    public static String IP = "10.200.63.180";
    public static int PORT = 22122;
    public static String AUTH = "xylxredis123!@#mima";
    public static String key;
    /**
     * 可用连接实例的最大数目，默认值为8；
     * 如果赋值为-1，则表示不限制；如果pool已经分配了maxActive个jedis实例，则此时pool的状态为exhausted(耗尽)。
     */
    public static int MAX_ACTIVE = 1024;
    /**
     * 控制一个pool最多有多少个状态为idle(空闲的)的jedis实例，默认值也是8。
     */
    public static int MAX_IDLE = 200;
    /**
     * 等待可用连接的最大时间，单位毫秒，默认值为-1，表示永不超时。如果超过等待时间，则直接抛出JedisConnectionException；
     */
    public static int MAX_WAIT = 10000;
    public static int TIMEOUT = 10000;
    /**
     * 在borrow一个jedis实例时，是否提前进行validate操作；如果为true，则得到的jedis实例均是可用的；
     */
    public static boolean TEST_ON_BORROW = true;
    public static JedisPool jedisPool = null;

    /**
     *
     * 初始化Redis连接池
     */
    public  JedisPool getJedisPool(){
        try {
            JedisPoolConfig config = new JedisPoolConfig();
            config.setMaxTotal(MAX_ACTIVE);
            config.setMaxIdle(MAX_IDLE);
            config.setMaxWaitMillis(MAX_WAIT);
            config.setTestOnBorrow(TEST_ON_BORROW);
            if (AUTH == null || AUTH.isEmpty()) {
                jedisPool = new JedisPool(config, IP, PORT, TIMEOUT);
            } else {
                jedisPool = new JedisPool(config, IP, PORT, TIMEOUT, AUTH);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return jedisPool;
    }

    /**
     * 获取Jedis实例
     *
     * @return jedis
     */
    public synchronized Jedis getJedis() {
        try {
            if (jedisPool != null) {
                Jedis resource = jedisPool.getResource();
                return resource;
            } else {
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 释放redis的资源
     *
     * @param jedis
     */
    public void returnResource(final Jedis jedis) {
        if (jedis != null) {
            jedisPool.returnResource(jedis);
        }
    }

    /**
     * 释放redis的资源 --异常
     *
     * @param jedis
     */
    public void returnBrokenResource(Jedis jedis) {
        jedisPool.returnBrokenResource(jedis);
    }

    /**
     * 根据设备号从dmp获取去用户标签
     *
     * @param did 为DMP中redis的key,md5加密后作为参数传入。
     * @return
     * @throws Exception
     */
    public  Map<String, List<String>> getUserTags(String did ,Map<Integer, String> tagsMap) throws Exception {
        Map<String, List<String>> userTagMap = new HashMap<String, List<String>>();
        byte[] result;
        Jedis jedis;
        RedisUtilsMain redisUtilsMain = new RedisUtilsMain();
        jedis = redisUtilsMain.getJedisPool().getResource();

        try {
            if (did.trim().length() == 32) {
                result = jedis.get(did.getBytes());
            } else {
                result = jedis.get(MD5.Md5(Utils.formatImei(did)).getBytes());
            }

            if (result == null) {
                System.out.println("did not find");
            }
            BitSet bitSetTag = BitSet.valueOf(result);

            for (int sourceIndex = 0; sourceIndex < bitSetTag.size(); sourceIndex++) {
                if (bitSetTag.get(sourceIndex)) {
                    int mod = sourceIndex % 8;
                    int targetIndex = (sourceIndex - mod - 1) + (8 - mod);
                    System.out.println("targetIndes: "+targetIndex);
                    // 从标签codeMap中获取标签code
                    String tagCode = tagsMap.get(targetIndex);
                    String codeType = tagCode;
                    // 取前八位作为标签类
                    if (!StringUtils.isBlank(codeType) && codeType.length() > 8) {
                        codeType = codeType.substring(0, 8);
                    } else {
                        continue;
                    }
                    // 标签大类下具体属性信息
                    List<String> codeList = userTagMap.get(codeType);
                    if (null != codeList) {
                        codeList.add(tagCode);
                    } else {
                        codeList = new ArrayList<String>();
                        codeList.add(tagCode);
                        userTagMap.put(codeType, codeList);
                    }
                }
            }
            System.out.println("userTagMap: " + userTagMap);
        } catch (Exception e) {
            log.error("getUserTags() error", e);
            redisUtilsMain.returnBrokenResource(jedis);
        }
     return userTagMap ;
    }

    public static void main(String[] args) throws Exception {

        try {

            /**
             * 读取标签与位ID的映射关系 在tagid2sequenceid.txt文件中
             */
            InputStream ts = RedisUtilsMain.class.getResourceAsStream("/tagid2sequenceid.txt");
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(ts));
            Map<Integer, String> tagsMap = new HashMap<>();
            String str = null;
            String[] str1 = null;
            while ((str = bufferedReader.readLine()) != null) {
                str1 = str.split(",");
                tagsMap.put(Integer.parseInt(str1[1].trim()), str1[0].trim());
            }

            /**
             * 连接redis 通过查询key解析并获取用户具有的标签
             */
              RedisUtilsMain redisUtilsMain = new RedisUtilsMain();
             IP = args[0];
             PORT = Integer.parseInt(args[1]);
             key = args[2] ;
              redisUtilsMain.getUserTags(args[2],tagsMap);


        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
