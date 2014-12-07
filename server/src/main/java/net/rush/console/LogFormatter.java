package net.rush.console;

import java.text.SimpleDateFormat;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

public class LogFormatter extends Formatter {

	private final SimpleDateFormat date = new SimpleDateFormat("HH:mm:ss");

	@Override
	public String format(LogRecord record) {

		if (record.getThrown() != null)
			record.getThrown().printStackTrace();

		return date.format(record.getMillis()) + " [" + record.getLevel().toString() + "] " + formatMessage(record) + System.lineSeparator();
	}

}