package i18n;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.logging.Logger;

/**
 * Logging objects are able to perform basic logging.
 * 
 * The basic logging uses two different methods for logging - one using
 * {@link java.text.MessageFormat} and another using {@link String.format}.
 *
 * @author Antti Kautiainen
 *
 */
public interface Logging {

	/**
	 * The localized logging performs localization to the formats.
	 * 
	 * @author Antti Kautiainen
	 *
	 */
	public static interface LocalizedLogging extends Logging, Localized {

		/**
		 * Creates default instance of the localized logging.
		 * 
		 * @return The default instance of the localized logging.
		 */
		static LocalizedLogging instance() {
			return new LocalizedLogging() {
			};
		}

		/**
		 * The localized logging using given localization.
		 * 
		 * @param localization The localization. If undefined, using default
		 *                     localization.
		 * @return The localized logging using given localization.
		 */
		static LocalizedLogging createLocalizedLogging(Localized localization) {
			if (localization == null) {
				return instance();
			}

			// Returns the default logging with localization
			return new LocalizedLogging() {

				@Override
				public ResourceBundle getLocalisationBundle() {
					return localization.getLocalisationBundle();
				}

				@Override
				public boolean isSupported(Locale locale) {
					return localization.isSupported(locale);
				}

				@Override
				public Locale getLocale() {
					return localization.getLocale();
				}

				@Override
				public Locale setLocale(Locale newLocale) throws IllegalArgumentException {
					return localization.setLocale(newLocale);
				}

				@Override
				public String getLocalized(String value) {
					return localization.getLocalized(value);
				}

			};

		}

		/**
		 * The localized logging using given logging.
		 * 
		 * @param logging The logging used for localized logging.
		 * @return The localized logging using given logging for localized formats.
		 */
		static LocalizedLogging createLocalizedLogging(Logging logging) {
			if (logging == null) {
				return instance();
			} else {
				return new LocalizedLogging() {

					@Override
					public String info(String format, Object... formatArgs) {
						return logging.info(this.getLocalizedFormat(format), formatArgs);
					}

					@Override
					public String severe(String format, Object... formatArgs) {
						return logging.severe(this.getLocalizedFormat(format), formatArgs);
					}

					@Override
					public Logger getLogger() {
						return logging.getLogger();
					}

					@Override
					public String format(String format, Object... formatArgs) {
						// TODO Auto-generated method stub
						return logging.format(getLocalizedFormat(format), formatArgs);
					}

				};
			}
		}

		/**
		 * Get the localized version of the given logging.
		 * 
		 * If the given localization is undefined, uses the default localization.
		 * 
		 * @param logging      The logging whose localized version is queried.
		 * @param localization The localization of the created localized logging.
		 * @return The localized logging version of the given logging object using given
		 *         localization.
		 */
		static LocalizedLogging createLocalizedLogging(Logging logging, Localized localization) {
			if (logging == null) {
				return createLocalizedLogging(localization);
			} else if (localization == null) {
				return createLocalizedLogging(logging);
			} else {
				// Creating wrapper using given logging for logging with given localization.
				return new LocalizedLogging() {

					@Override
					public ResourceBundle getLocalisationBundle() {
						// TODO Auto-generated method stub
						return localization.getLocalisationBundle();
					}

					@Override
					public boolean isSupported(Locale locale) {
						// TODO Auto-generated method stub
						return localization.isSupported(locale);
					}

					@Override
					public Locale getLocale() {
						// TODO Auto-generated method stub
						return localization.getLocale();
					}

					@Override
					public Locale setLocale(Locale newLocale) throws IllegalArgumentException {
						// TODO Auto-generated method stub
						return localization.setLocale(newLocale);
					}

					@Override
					public String getLocalized(String value) {
						// TODO Auto-generated method stub
						return localization.getLocalized(value);
					}

					@Override
					public String info(String format, Object... formatArgs) {
						return logging.info(this.getLocalizedFormat(format), formatArgs);
					}

					@Override
					public String severe(String format, Object... formatArgs) {
						return logging.severe(this.getLocalizedFormat(format), formatArgs);
					}

					@Override
					public Logger getLogger() {
						return logging.getLogger();
					}

					@Override
					public String format(String format, Object... formatArgs) {
						// TODO Auto-generated method stub
						return logging.format(getLocalizedFormat(format), formatArgs);
					}

				};
			}
		}

		/**
		 * The resource bundle handling the localization of the formats.
		 * 
		 * @return The resource bundle containing the localized formats.
		 */
		default ResourceBundle getFormatLocalizationBundle() {
			return this.getLocalisationBundle();
		}

		/**
		 * The localized version of the format.
		 * 
		 * @param format The format string whose localized version is acquired.
		 * @return The localized format string.
		 */
		default String getLocalizedFormat(String format) {
			return this.getFormatLocalizationBundle().getString(format);
		}

		@Override
		default String format(String format, Object... formatArgs) {
			return String.format(getLocalizedFormat(format), formatArgs);
		}

	}

	/**
	 * Logs information message.
	 * 
	 * @param format     The format of the message.
	 * @param formatArgs The format arguments.
	 * @return The logged message.
	 */
	default String info(String format, Object... formatArgs) {
		Logger logger = getLogger();
		String result = format(format, formatArgs);
		if (logger != null) {
			getLogger().info(result);
		}
		return result;
	}

	/**
	 * Logs error message.
	 * 
	 * @param format     The format of the message.
	 * @param formatArgs The format arguments.
	 * @return The logged message.
	 */
	default String severe(String format, Object... formatArgs) {
		Logger logger = getLogger();
		String result = format(format, formatArgs);
		if (logger != null) {
			getLogger().severe(result);
		}
		return result;
	}

	/**
	 * The logger of the logging object.
	 * 
	 * Undefined logger means the logging is not done.
	 * 
	 * @return The logger of the logging object.
	 */
	default Logger getLogger() {
		return Logger.getAnonymousLogger();
	}

	/**
	 * Get formatted message.
	 * 
	 * @param format     The format.
	 * @param formatArgs The format arguments
	 * @return The formatted message.
	 */
	default String format(String format, Object... formatArgs) {
		return String.format(format, formatArgs);
	}

	/**
	 * The logging using message format instead of normal format.
	 * 
	 * @author Antti Kautiainen
	 *
	 */
	public static interface MessageLogging extends Logging {

		/**
		 * Format a message using message format.
		 * 
		 * @param pattern   The message format pattern.
		 * @param arguments The arguments of the message format.
		 * @return The formatted message using given pattern and arguments.
		 */
		default String format(String pattern, Object... arguments) {
			return MessageFormat.format(pattern, arguments);
		}
	}

	/**
	 * Localized logging using message format.
	 * 
	 * @author Antti Kautiainen
	 *
	 */
	public static interface LocalizedMessageLogging extends MessageLogging, LocalizedLogging {

		@Override
		default String format(String pattern, Object... arguments) {
			return MessageFormat.format(this.getLocalizedFormat(pattern), arguments);
		}

	}

}
