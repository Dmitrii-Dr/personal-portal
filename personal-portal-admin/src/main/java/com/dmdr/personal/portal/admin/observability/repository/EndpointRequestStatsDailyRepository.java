package com.dmdr.personal.portal.admin.observability.repository;

import com.dmdr.personal.portal.admin.observability.model.EndpointRequestStatsDailyEntity;
import com.dmdr.personal.portal.admin.observability.model.EndpointStatsId;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface EndpointRequestStatsDailyRepository
    extends JpaRepository<EndpointRequestStatsDailyEntity, EndpointStatsId>, JpaSpecificationExecutor<EndpointRequestStatsDailyEntity> {

    interface GroupedPeriodStatsProjection {
        String getMethod();
        String getTemplatePath();
        long getTotalCount();
        long getSuccessCount();
        long getAuthErrorCount();
        long getClientErrorCount();
        long getServerErrorCount();
        long getOtherNonSuccessCount();
    }

    interface GroupedPeriodStatsByMethodProjection {
        String getMethod();
        long getTotalCount();
        long getSuccessCount();
        long getAuthErrorCount();
        long getClientErrorCount();
        long getServerErrorCount();
        long getOtherNonSuccessCount();
    }

    interface GroupedPeriodStatsByTemplatePathProjection {
        String getTemplatePath();
        long getTotalCount();
        long getSuccessCount();
        long getAuthErrorCount();
        long getClientErrorCount();
        long getServerErrorCount();
        long getOtherNonSuccessCount();
    }

    interface MethodTemplatePairProjection {
        String getMethod();
        String getTemplatePath();
    }

    interface TopErrorEndpointProjection {
        String getMethod();
        String getTemplatePath();
        long getTotalCount();
        long getSuccessCount();
        long getAuthErrorCount();
        long getClientErrorCount();
        long getServerErrorCount();
        long getOtherNonSuccessCount();
        long getTotalErrorCount();
    }

    Optional<EndpointRequestStatsDailyEntity> findByBucketStartAndMethodAndTemplatePath(
        LocalDate bucketStart,
        String method,
        String templatePath
    );

    @Query("select distinct e.method as method, e.templatePath as templatePath from EndpointRequestStatsDailyEntity e")
    List<MethodTemplatePairProjection> findDistinctMethodTemplatePairs();

    @Query(
        """
        select
            e.method as method,
            e.templatePath as templatePath,
            sum(e.totalCount) as totalCount,
            sum(e.successCount) as successCount,
            sum(e.authErrorCount) as authErrorCount,
            sum(e.clientErrorCount) as clientErrorCount,
            sum(e.serverErrorCount) as serverErrorCount,
            sum(e.otherNonSuccessCount) as otherNonSuccessCount
        from EndpointRequestStatsDailyEntity e
        where e.bucketStart >= :from
          and e.bucketStart <= :to
          and e.method in :methods
          and e.templatePath in :templatePaths
        group by e.method, e.templatePath
        order by e.method asc, e.templatePath asc
        """
    )
    List<GroupedPeriodStatsProjection> aggregatePeriodByMethodAndTemplatePath(
        LocalDate from,
        LocalDate to,
        List<String> methods,
        List<String> templatePaths
    );

    @Query(
        """
        select
            e.method as method,
            sum(e.totalCount) as totalCount,
            sum(e.successCount) as successCount,
            sum(e.authErrorCount) as authErrorCount,
            sum(e.clientErrorCount) as clientErrorCount,
            sum(e.serverErrorCount) as serverErrorCount,
            sum(e.otherNonSuccessCount) as otherNonSuccessCount
        from EndpointRequestStatsDailyEntity e
        where e.bucketStart >= :from
          and e.bucketStart <= :to
          and e.method in :methods
        group by e.method
        order by e.method asc
        """
    )
    List<GroupedPeriodStatsByMethodProjection> aggregatePeriodByMethod(
        LocalDate from,
        LocalDate to,
        List<String> methods
    );

    @Query(
        """
        select
            e.templatePath as templatePath,
            sum(e.totalCount) as totalCount,
            sum(e.successCount) as successCount,
            sum(e.authErrorCount) as authErrorCount,
            sum(e.clientErrorCount) as clientErrorCount,
            sum(e.serverErrorCount) as serverErrorCount,
            sum(e.otherNonSuccessCount) as otherNonSuccessCount
        from EndpointRequestStatsDailyEntity e
        where e.bucketStart >= :from
          and e.bucketStart <= :to
          and e.templatePath in :templatePaths
        group by e.templatePath
        order by e.templatePath asc
        """
    )
    List<GroupedPeriodStatsByTemplatePathProjection> aggregatePeriodByTemplatePath(
        LocalDate from,
        LocalDate to,
        List<String> templatePaths
    );

    @Query(
        value = """
        select
            e.method as method,
            e.templatePath as templatePath,
            sum(e.totalCount) as totalCount,
            sum(e.successCount) as successCount,
            sum(e.authErrorCount) as authErrorCount,
            sum(e.clientErrorCount) as clientErrorCount,
            sum(e.serverErrorCount) as serverErrorCount,
            sum(e.otherNonSuccessCount) as otherNonSuccessCount,
            (sum(e.authErrorCount) + sum(e.clientErrorCount) + sum(e.serverErrorCount) + sum(e.otherNonSuccessCount)) as totalErrorCount
        from EndpointRequestStatsDailyEntity e
        where e.bucketStart >= :from
          and e.bucketStart <= :to
        group by e.method, e.templatePath
        having (sum(e.authErrorCount) + sum(e.clientErrorCount) + sum(e.serverErrorCount) + sum(e.otherNonSuccessCount)) > 0
        order by (sum(e.authErrorCount) + sum(e.clientErrorCount) + sum(e.serverErrorCount) + sum(e.otherNonSuccessCount)) desc,
                 e.method asc,
                 e.templatePath asc
        """,
        countQuery = """
        select count(distinct concat(e.method, '||', e.templatePath))
        from EndpointRequestStatsDailyEntity e
        where e.bucketStart >= :from
          and e.bucketStart <= :to
          and (e.authErrorCount + e.clientErrorCount + e.serverErrorCount + e.otherNonSuccessCount) > 0
        """
    )
    Page<TopErrorEndpointProjection> findTopErrorEndpoints(LocalDate from, LocalDate to, Pageable pageable);
}
