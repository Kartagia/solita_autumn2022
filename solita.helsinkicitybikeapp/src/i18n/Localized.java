package i18n;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Indicates the current object supports localizetion. 
 * 
 * @author Antti Kautiainen
 *
 */
public interface Localized {

	/**
	 * The localization bundle. 
	 * @return The resource bundle handling localization of the current file. 
	 */
	default ResourceBundle getLocalisationBundle() {
		return ResourceBundle.getBundle("messages");
	}
	
	/**
	 * Does the localization support given locale. 
	 * @param locale The tested locale.
	 * @return True, if and only if the locale is supported. 
	 */
	default boolean isSupported(Locale locale) {
		return (locale != null) && Arrays.asList(Locale.getAvailableLocales()).contains(locale); 
	}
	
	/**
	 * The current locale of the localized. 
	 * @return The current locale of the localized.  
	 */
	default Locale getLocale() {
		return Locale.getDefault();
	}
	
	/**
	 * Set the current locale. 
	 * @param newLocale The new locale. 
	 * @return The old locale replaced by the new locale. 
	 * @throws IllegalArgumentException The new locale was not supported. 
	 */
	default Locale setLocale(Locale newLocale) throws IllegalArgumentException {
		if (isSupported(newLocale)) {
			Locale result = getLocale(); 
			Locale.setDefault(newLocale);
			return result; 
		} else {
			throw new IllegalArgumentException("Locale not suported"); 
		}
	}
	
	/**
	 * Localized the given value. 
	 * @param value The localized string. 
	 * @return The localized version of the given value.
	 */
	default String getLocalized(String value) {
		return this.getLocalisationBundle().getString(value); 
	}
}
