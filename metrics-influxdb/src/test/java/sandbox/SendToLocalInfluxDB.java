package sandbox;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.ScheduledReporter;
import com.codahale.metrics.Timer;
import com.izettle.metrics.influxdb.InfluxDbHttpSender;
import com.izettle.metrics.influxdb.InfluxDbReporter;
import com.izettle.metrics.influxdb.InfluxDbSender;
import com.izettle.metrics.influxdb.InfluxDbTcpSender;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public final class SendToLocalInfluxDB {
    private SendToLocalInfluxDB() {

    }

    public static void main(String[] args) {
        InfluxDbReporter influxDbReporter = null;
        ScheduledReporter consoleReporter = null;
        Timer.Context context = null;
        try {
            final MetricRegistry registry = new MetricRegistry();
            consoleReporter = startConsoleReporter(registry);
            influxDbReporter = startInfluxDbReporter(registry, GetHttpSender());

            final Meter myMeter = registry.meter(MetricRegistry.name(SendToLocalInfluxDB.class, "testMetric"));

            final Timer myTimer = registry.timer("testTimer");
            context = myTimer.time();
            for (int i = 0; i < 5; i++) {
                myMeter.mark();
                myMeter.mark(Math.round(Math.random() * 100.0));
                Thread.sleep(2000);
            }
        } catch (Exception exc) {
            exc.printStackTrace();
            System.exit(1);
        } finally {
            if (context != null) {
                context.stop();
            }
            if (influxDbReporter != null) {
                influxDbReporter.report();
                influxDbReporter.stop();
            }
            if (consoleReporter != null) {
                consoleReporter.report();
                consoleReporter.stop();
            }
            System.out.println("Finished");
        }
    }

    private static InfluxDbSender GetTcpSender() throws Exception {
        return new InfluxDbTcpSender("127.0.0.1", 8094, "dropwzard", TimeUnit.SECONDS, 1000);
    }

    private static InfluxDbHttpSender GetHttpSender() throws Exception {
        return new InfluxDbHttpSender(
                "http",
                "127.0.0.1",
                8086,
                "dropwizard",
                "root:root",
                TimeUnit.MINUTES,
                1000,
                1000);
    }

    private static InfluxDbReporter startInfluxDbReporter(MetricRegistry registry, InfluxDbSender influxDbSender) throws Exception {
        final Map<String, String> tags = new HashMap<String, String>();
        tags.put("host", "localhost");
        final InfluxDbReporter reporter = InfluxDbReporter
                .forRegistry(registry)
                .withTags(tags)
                .skipIdleMetrics(true)
                .convertRatesTo(TimeUnit.SECONDS)
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .filter(MetricFilter.ALL)
                .build(influxDbSender);
        reporter.start(10, TimeUnit.SECONDS);
        return reporter;
    }

    private static ConsoleReporter startConsoleReporter(MetricRegistry registry) throws Exception {
        final ConsoleReporter reporter = ConsoleReporter
                .forRegistry(registry)
                .convertRatesTo(TimeUnit.SECONDS)
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .build();
        reporter.start(1, TimeUnit.MINUTES);
        return reporter;
    }
}
