package com.github.stella.springredisjob.common.lock;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.lock")
public class LockProperties {
    /** 키 프리픽스 (예: lock:) */
    private String prefix = "lock:";
    /** tryLock 대기 시간(초) */
    private long waitSeconds = 2L;
    /** 락 보유 시간(초) */
    private long leaseSeconds = 20L;

    public String getPrefix() { return prefix; }
    public void setPrefix(String prefix) { this.prefix = prefix; }
    public long getWaitSeconds() { return waitSeconds; }
    public void setWaitSeconds(long waitSeconds) { this.waitSeconds = waitSeconds; }
    public long getLeaseSeconds() { return leaseSeconds; }
    public void setLeaseSeconds(long leaseSeconds) { this.leaseSeconds = leaseSeconds; }
}
