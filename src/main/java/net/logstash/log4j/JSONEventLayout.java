package net.logstash.log4j;

import com.fasterxml.jackson.core.JsonGenerator.Feature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.logstash.log4j.data.HostData;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.FastDateFormat;
import org.apache.log4j.Layout;
import org.apache.log4j.spi.LocationInfo;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.spi.ThrowableInformation;

import java.util.Date;
import java.util.Map;

public class JSONEventLayout extends Layout {
    private static final ObjectMapper MAPPER = new ObjectMapper().configure(Feature.ESCAPE_NON_ASCII, true);

    private boolean locationInfo = false;
    private boolean ignoreThrowable = false;

    private boolean activeIgnoreThrowable = ignoreThrowable;
    private final String hostname = new HostData().getHostName();

    public static final FastDateFormat ISO_DATETIME_TIME_ZONE_FORMAT_WITH_MILLIS = FastDateFormat.getInstance("yyyy-MM-dd'T'HH:mm:ss.SSSZZ");

    public static String dateFormat(long timestamp) {
        return ISO_DATETIME_TIME_ZONE_FORMAT_WITH_MILLIS.format(new Date(timestamp));
    }

    /**
     * For backwards compatibility, the default is to generate location information
     * in the log messages.
     */
    public JSONEventLayout() {
        this(true);
    }

    /**
     * Creates a layout that optionally inserts location information into log messages.
     *
     * @param locationInfo whether or not to include location information in the log messages.
     */
    public JSONEventLayout(boolean locationInfo) {
        this.locationInfo = locationInfo;
    }

    public String format(LoggingEvent loggingEvent) {
        long timestamp = loggingEvent.getTimeStamp();
        Map<String, String> mdc = loggingEvent.getProperties();
        String ndc = loggingEvent.getNDC();

        ObjectNode eventNode = MAPPER.createObjectNode();

        eventNode.put("@source_host",hostname);
        eventNode.put("@message",loggingEvent.getRenderedMessage());
        eventNode.put("@timestamp",dateFormat(timestamp));
        ObjectNode fieldsNode = MAPPER.createObjectNode();
        eventNode.put("@fields", fieldsNode);

        if(loggingEvent.getThrowableInformation() != null) {
            ObjectNode exceptionNode = MAPPER.createObjectNode();

            final ThrowableInformation throwableInformation = loggingEvent.getThrowableInformation();
            if(throwableInformation.getThrowable().getClass().getCanonicalName() != null){
                exceptionNode.put("exception_class", throwableInformation.getThrowable().getClass().getCanonicalName());
            }
            if(throwableInformation.getThrowable().getMessage() != null) {
                exceptionNode.put("exception_message", throwableInformation.getThrowable().getMessage());
            }
            if( throwableInformation.getThrowableStrRep() != null) {
                String stackTrace = StringUtils.join(throwableInformation.getThrowableStrRep(),"\n");
                exceptionNode.put("stacktrace", stackTrace);
            }
            fieldsNode.put("exception", exceptionNode);
        }

        if (locationInfo) {
            LocationInfo info = loggingEvent.getLocationInformation();
            fieldsNode.put("file", info.getFileName());
            fieldsNode.put("line_number", info.getLineNumber());
            fieldsNode.put("class", info.getClassName());
            fieldsNode.put("method", info.getMethodName());
        }

        ObjectNode mdcNode = MAPPER.createObjectNode();
        for (Map.Entry<String, String> entry : mdc.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            mdcNode.put(key, value);
        }
        fieldsNode.put("mdc", mdcNode);

        fieldsNode.put("ndc", ndc);
        fieldsNode.put("level", loggingEvent.getLevel().toString());

        return eventNode.toString() + "\n";
    }

    public boolean ignoresThrowable() {
        return ignoreThrowable;
    }

    /**
     * Query whether log messages include location information.
     *
     * @return true if location information is included in log messages, false otherwise.
     */
    public boolean getLocationInfo() {
        return locationInfo;
    }

    /**
     * Set whether log messages should include location information.
     *
     * @param locationInfo true if location information should be included, false otherwise.
     */
    public void setLocationInfo(boolean locationInfo) {
        this.locationInfo = locationInfo;
    }

    public void activateOptions() {
        activeIgnoreThrowable = ignoreThrowable;
    }
}
