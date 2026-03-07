package com.tradebot.upstox.common;

import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;

public class DateUtils {

	public static Date[] getStartAndEndOfDay(Date date) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		Calendar startOfDayCalendar = (Calendar) calendar.clone();
		startOfDayCalendar.set(Calendar.HOUR_OF_DAY, 0);
		startOfDayCalendar.set(Calendar.MINUTE, 0);
		startOfDayCalendar.set(Calendar.SECOND, 0);
		startOfDayCalendar.set(Calendar.MILLISECOND, 0);
		Date startOfDay = startOfDayCalendar.getTime();
		Calendar endOfDayCalendar = (Calendar) calendar.clone();
		endOfDayCalendar.set(Calendar.HOUR_OF_DAY, 23);
		endOfDayCalendar.set(Calendar.MINUTE, 59);
		endOfDayCalendar.set(Calendar.SECOND, 59);
		endOfDayCalendar.set(Calendar.MILLISECOND, 999);
		Date endOfDay = endOfDayCalendar.getTime();
		return new Date[] { startOfDay, endOfDay };
	}

	public static String convertDateToString(Date date) {
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		return dateFormat.format(date);
	}

	public static LocalDate getThursdayDate() {
		LocalDate today = LocalDate.now();
		DayOfWeek currentDay = today.getDayOfWeek();
		int daysToThursday = DayOfWeek.THURSDAY.getValue() - currentDay.getValue();
		if (daysToThursday < 0) {
			daysToThursday += 7;
		}
		return today.plusDays(daysToThursday);
	}

	public static String getThursdayDateStringFormat() {
		LocalDate today = LocalDate.now();
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
		DayOfWeek currentDay = today.getDayOfWeek();
		int daysToThursday = DayOfWeek.THURSDAY.getValue() - currentDay.getValue();
		if (daysToThursday < 0) {
			daysToThursday += 7;
		}
		LocalDate desiredThursday = today.plusDays(daysToThursday);
		return desiredThursday.format(formatter);
	}
}
