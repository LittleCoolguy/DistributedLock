package day0510_DistributedLock;

import org.apache.commons.lang3.StringUtils;
import redis.clients.jedis.Jedis;

import java.time.temporal.JulianFields;

/**
 * @description: 基于setnx(set if not exists)实现
 * @author: XiaoGao
 * @time: 2022/5/10 13:38
 */
public class MyDistributedLock {

    private String host;
    private Integer port;
    private long lockTimeout;
    private long preSleep;
    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public long getLockTimeout() {
        return lockTimeout;
    }

    public void setLockTimeout(long lockTimeout) {
        this.lockTimeout = lockTimeout;
    }

    public long tryLock(String key) {
        long expireTime = 0;
        Jedis jedis = new Jedis(host, port);
        expireTime = System.currentTimeMillis() + lockTimeout + 1;
        if (jedis.setnx(key, String.valueOf(expireTime)) == 1) {
            return expireTime;
        }
        else {
            //key已经存在
            String curLockTimeStr = jedis.get(key);
            if (StringUtils.isBlank(curLockTimeStr) ||
                System.currentTimeMillis() > Long.parseLong(curLockTimeStr)) {
                expireTime = System.currentTimeMillis() + lockTimeout + 1;
                return expireTime;
            }
            else {
                return 0;
            }
        }
    }

    /**
     * tryLock与Lock实现区别就是多个无限循环
     * 因为lock是会一直尝试获取锁的
     * @param key
     * @return
     * @throws InterruptedException
     */
    public long lock(String key) throws InterruptedException {
        long startTime = System.currentTimeMillis();
        long expireTime = 0;
        Jedis jedis = new Jedis(host, port);
        expireTime = System.currentTimeMillis() + lockTimeout + 1;
        long sleep = (preSleep == 0 ? lockTimeout / 15 : preSleep);
        for (;;) {
            if (jedis.setnx(key, String.valueOf(expireTime)) == 1) {
                return expireTime;
            }
            else {
                //key已经存在
                String curLockTimeStr = jedis.get(key);
                if (StringUtils.isBlank(curLockTimeStr) ||
                        System.currentTimeMillis() > Long.parseLong(curLockTimeStr)) {
                    expireTime = System.currentTimeMillis() + lockTimeout + 1;
                    return expireTime;
                }
                else {
                    Thread.sleep(sleep);
                }
            }
            if (lockTimeout > 0 && (System.currentTimeMillis() - startTime) >= lockTimeout) {
                expireTime = 0;
                return expireTime;
                //也不能一直尝试~~
            }
        }
    }

    public void unlock(String key, long expireTime) {
        if (System.currentTimeMillis() - expireTime > 0) {
            return;
        }
        Jedis jedis = new Jedis(host, port);
        String curLockTimeStr = jedis.get(key);
        if (StringUtils.isNoneBlank(curLockTimeStr) && Long.valueOf(curLockTimeStr) > System.currentTimeMillis()) {
            jedis.del(key);
        }
    }
}
