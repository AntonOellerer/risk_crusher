package at.ac.tuwien.ifs.sge.leeroy.util.logging;

import java.util.logging.Formatter;
import java.util.logging.LogRecord;

public class CleanFormatter extends Formatter {

    @Override
    public String format(LogRecord record) {
        StringBuilder sb = new StringBuilder();
        sb.append(record.getMessage());
        sb.append("\n");
        return sb.toString();
    }
}
