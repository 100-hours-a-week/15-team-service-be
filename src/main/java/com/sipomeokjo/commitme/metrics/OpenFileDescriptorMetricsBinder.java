package com.sipomeokjo.commitme.metrics;

import com.sun.management.UnixOperatingSystemMXBean;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import java.lang.management.ManagementFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class OpenFileDescriptorMetricsBinder implements MeterBinder {

    @Override
    public void bindTo(MeterRegistry registry) {
        Gauge.builder(
                        "openfd.open.count",
                        this,
                        OpenFileDescriptorMetricsBinder::openFileDescriptorCount)
                .description("Current number of open file descriptors")
                .register(registry);

        Gauge.builder(
                        "openfd.max.count",
                        this,
                        OpenFileDescriptorMetricsBinder::maxFileDescriptorCount)
                .description("Maximum number of file descriptors allowed by OS")
                .register(registry);

        Gauge.builder(
                        "openfd.usage.ratio",
                        this,
                        OpenFileDescriptorMetricsBinder::openFileDescriptorUsageRatio)
                .description("Open file descriptor usage ratio (open/max)")
                .register(registry);
    }

    private double openFileDescriptorCount() {
        UnixOperatingSystemMXBean unixOsBean = resolveUnixOperatingSystemMxBean();
        if (unixOsBean == null) {
            return Double.NaN;
        }
        return unixOsBean.getOpenFileDescriptorCount();
    }

    private double maxFileDescriptorCount() {
        UnixOperatingSystemMXBean unixOsBean = resolveUnixOperatingSystemMxBean();
        if (unixOsBean == null) {
            return Double.NaN;
        }
        return unixOsBean.getMaxFileDescriptorCount();
    }

    private double openFileDescriptorUsageRatio() {
        double openCount = openFileDescriptorCount();
        double maxCount = maxFileDescriptorCount();
        if (Double.isNaN(openCount) || Double.isNaN(maxCount) || maxCount <= 0D) {
            return Double.NaN;
        }
        return openCount / maxCount;
    }

    private UnixOperatingSystemMXBean resolveUnixOperatingSystemMxBean() {
        java.lang.management.OperatingSystemMXBean osBean =
                ManagementFactory.getOperatingSystemMXBean();
        if (osBean instanceof UnixOperatingSystemMXBean unixOsBean) {
            return unixOsBean;
        }
        log.debug(
                "[OPEN_FD_METRIC] UnixOperatingSystemMXBean unsupported osBean={}",
                osBean.getClass().getName());
        return null;
    }
}
