package com.tronget.islab1.aop;

import jakarta.persistence.EntityManagerFactory;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.hibernate.SessionFactory;
import org.hibernate.stat.CacheRegionStatistics;
import org.hibernate.stat.Statistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Aspect
@Component
public class CacheStatisticsAspect {
    private static final Logger log = LoggerFactory.getLogger(CacheStatisticsAspect.class);

    private final SessionFactory sessionFactory;
    private final boolean logStatistics;

    public CacheStatisticsAspect(
            EntityManagerFactory emf,
            @Value("${app.jpa.second-level-cache.log-statistics:false}")
            boolean logStatistics) {
        this.sessionFactory = emf.unwrap(SessionFactory.class);
        this.logStatistics = logStatistics;
        if (this.logStatistics) {
            Statistics s = sessionFactory.getStatistics();
            s.setStatisticsEnabled(true);
        }
    }

    @Around("within(com.tronget.islab1.service..*)")
    public Object aroundService(ProceedingJoinPoint pjp) throws Throwable {
        if (!logStatistics) {
            return pjp.proceed();
        }
        Statistics s = sessionFactory.getStatistics();
        long beforeHits = s.getSecondLevelCacheHitCount();
        long beforeMiss = s.getSecondLevelCacheMissCount();
        long beforePut = s.getSecondLevelCachePutCount();
        Object result = pjp.proceed();
        long afterHits = s.getSecondLevelCacheHitCount();
        long afterMiss = s.getSecondLevelCacheMissCount();
        long afterPut = s.getSecondLevelCachePutCount();
        long diffHits = afterHits - beforeHits;
        long diffMiss = afterMiss - beforeMiss;
        long diffPut = afterPut - beforePut;
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("2LC stats for call ")
                    .append(pjp.getSignature().toShortString())
                    .append(" - ")
                    .append("hits=")
                    .append(diffHits)
                    .append(", misses=")
                    .append(diffMiss)
                    .append(", puts=")
                    .append(diffPut)
                    .append("; region stats: ");
            String[] regions = sessionFactory.getStatistics().getSecondLevelCacheRegionNames();
            Arrays.stream(regions).forEach(r -> {
                CacheRegionStatistics reg = s.getCacheRegionStatistics(r);
                if (reg != null) {
                    sb.append("[")
                            .append(r)
                            .append(": hits=")
                            .append(reg.getHitCount())
                            .append(", misses=")
                            .append(reg.getMissCount())
                            .append(", puts=")
                            .append(reg.getPutCount())
                            .append("]");
                }
            });
            log.info(sb.toString());
        } catch (Exception e) {
            log.warn("Failed to log second-level cache statistics: {}", e.getMessage());
        }
        return result;
    }
}
