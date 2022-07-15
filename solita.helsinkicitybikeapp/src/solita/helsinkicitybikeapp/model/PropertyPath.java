package solita.helsinkicitybikeapp.model;

import java.util.List;

/**
 * Property path class allows creating property paths.
 * 
 * By default the property names must be defined strings.
 * 
 * @author Antti Kautiainen
 *
 */
public class PropertyPath {

	/**
	 * The parent property path.
	 */
	private PropertyPath parent = null;

	/**
	 * The base property name.
	 */
	private String basePropertyName;

	/**
	 * The delimiter between base property name and content property names.
	 */
	private String delimiter = ".";

	/**
	 * Creates a new property path with given base property.
	 * 
	 * @param baseProperty
	 * @throws IllegalArgumentException The base property was invalid.
	 */
	public PropertyPath(String baseProperty) throws java.lang.IllegalArgumentException {
		this(baseProperty, null);
	}

	/**
	 * Creates a new property path with a parent path and a base property name.
	 * 
	 * @param parent           The parent path. Undefined value indicates there is
	 *                         no parent.
	 * @param basePropertyName The property name of the child path name.
	 */
	public PropertyPath(PropertyPath parent, String basePropertyName) {
		this(basePropertyName, parent == null ? null : parent.getDelimiter());
		this.parent = parent;
	}

	/**
	 * The delimiter of the path.
	 * 
	 * @return The delimiter of the path.
	 */
	public String getDelimiter() {
		return this.delimiter;
	}

	/**
	 * Creates a new Property Path with given base property, delimiter, and default
	 * properties.
	 * 
	 * @param baseProperty The base property name.
	 * @param delimiter    The delimiter between base property and the content
	 *                     property name.
	 * @throws IllegalArgumentException Any parameter was invalid.
	 */
	public PropertyPath(String baseProperty, String delimiter) throws java.lang.IllegalArgumentException {
		this.basePropertyName = baseProperty;
		if (delimiter != null) {
			this.delimiter = delimiter;
		}
	}

	public String getBasePropertyName() {
		return this.basePropertyName;
	}

	/**
	 * The path to the base property path name.
	 * 
	 * @return The string containing the property path.
	 */
	public String getBasePathName() {
		return parent == null ? this.getBasePropertyName() : parent.getPropertyName(this.getBasePropertyName());
	}

	/**
	 * The base property path.
	 * 
	 * @return The path of the parent property. If this is undefined,
	 */
	public PropertyPath getBasePath() {
		return parent;
	}

	/**
	 * The path created by combining this path with given path.
	 * 
	 * @param contentProperty The content property name.
	 * @return The new property path for the given content.
	 * @throws IllegalArgumentException
	 */
	public PropertyPath getPath(String contentProperty) throws IllegalArgumentException {
		if (this.validContentPropertyName(contentProperty)) {
			return (contentProperty == null ? this : new PropertyPath(this, contentProperty));
		} else {
			throw new IllegalArgumentException("Invalid content property name");
		}
	}

	/**
	 * The parent of the current path.
	 * 
	 * @return The parent path of the current path.
	 */
	public PropertyPath getParentPath() {
		return this.parent;
	}

	/**
	 * How long the current path is.
	 * 
	 * @return The number of steps the path has to the root node.
	 */
	public int length() {
		return (parent == null ? 0 : parent.length() + 1);
	}

	/**
	 * The path entries of the path.
	 * 
	 * The entries includes the delimiters of the path between property names.
	 * 
	 * @return The list of path entries the path is formed from.
	 */
	public java.util.List<String> getEntries() {
		PropertyPath parent = this.getParentPath();
		java.util.List<String> result = (parent == null ? new java.util.ArrayList<>() : parent.getEntries());
		if (parent != null)
			result.add(parent.getDelimiter());
		result.add(this.getBasePropertyName());
		return result;
	}

	/**
	 * The shared path with current path.
	 * 
	 * @param path The path whose shared path with current is queried.
	 * @return The shared path, if any exists. Undefined value otherwise.
	 */
	public PropertyPath getSharedPath(PropertyPath path) {
		PropertyPath result = null;
		if (path != null) {
			PropertyPath myParent = this.parent, otherParent = path.parent;
			List<String> myEntries = this.getEntries();
			List<String> pathEntries = path.getEntries();
			boolean divergent = false;
			int myLen = myEntries.size(), pathLen = pathEntries.size(), len;

			// Setting the default result to the smaller of the two property paths.
			if (pathLen < myLen) {
				len = pathLen;
				result = path;
			} else {
				len = myLen;
				result = this;
			}
			for (int i = 0; (i < len); i++) {
				if (divergent) {
					// We are passing through sub-path tail which is not shared.
					// - Moving the result to its parent path.
					if (i % 2 == 1) {
						// The delimiter entry moves the result to parent.
						// (Every odd index is delimiter index, which moves the result to its parent)
						result = result.getParentPath();
					}
				} else if (myEntries.get(i) != pathEntries.get(i)) {
					// WE found divergent path - the current path is set as path, as the future
					// steps
					if (i == 0) {
						// The first step causes the difference - there is no shared path.
						// (The loop would give correct answer even without this change, but the
						// automatic
						// return optimizes the path).
						return null;
					} else {
						// move the result backwards to the parents to get the correct shared path.
						//
						// Using the loop to move result property path to its parent as many times
						// as the result has divergent path element.
						divergent = true;
					}
				}
			}
		}
		return result;
	}

	/**
	 * Test the delimiter.
	 * 
	 * @param delimiter The tested delimiter.
	 * @return True, if and only if the given delimiter value is valid delimiter
	 *         value.
	 */
	public boolean validDelimiter(String delimiter) {
		return true;
	}

	/**
	 * Test the base path.
	 * 
	 * @param basePath The tested base bath.
	 * @return True, if and only if the base path is valid.
	 */
	public boolean validBasePath(String basePath) {
		return basePath == null || !basePath.isEmpty();
	}

	/**
	 * Test the validity of property name.
	 * 
	 * @param propertyName The tested property name.
	 * @return True, if and only if the given property name is valid property name.
	 */
	public boolean validPropertyName(String propertyName) {
		return propertyName != null;
	}

	/**
	 * Test the validity of the content property name.
	 * 
	 * @param propertyName The tested property name.
	 * @return True, if and only if the given content property name is valid.
	 */
	public boolean validContentPropertyName(String propertyName) {
		return propertyName == null || validPropertyName(propertyName);
	}

	/**
	 * The property name for the property with given base property, delimiter, and
	 * property name.
	 * 
	 * If both the base property name, and the property name are undefined, an empty
	 * string is returned.
	 * 
	 * If the base property name is undefined, the property name is returned.
	 * 
	 * If the property name is undefined, the base property name is returned.
	 * 
	 * Otherwise both property name and base property name are defined, and the
	 * compound property name is created by combining the base property name with
	 * delimiter followed by the property name.
	 * 
	 * @param baseProperty The base property name. If this is null, the prefix is
	 *                     empty string.
	 * @param delimiter    The delimiter separating the prefix and the property
	 *                     name. Defaults to dot ('.').
	 * @param propertyName The property name. Defaults to no property name.
	 * @return The property name with given base property name, delimiter, and
	 *         property name.
	 */
	public static String getPropertyName(String baseProperty, String delimiter, String propertyName) {
		if (propertyName == null) {
			return (baseProperty == null ? "" : baseProperty);
		} else {
			return (baseProperty == null ? "" : (baseProperty + (delimiter == null ? "." : delimiter)))
					+ propertyName;
		}
	}

	/**
	 * The property name of the given content property name.
	 * 
	 * If the content property name is undefined (<code>null</code>), the base
	 * property name is returned.
	 * 
	 * @param contentPropertyName The queried property name.
	 * @return If the given content property name is undefined, the base property
	 *         name. Otherwise the full property name of the given content property
	 *         name.
	 * @throws IllegalArgumentException The given property name is invalid.
	 */
	public String getPropertyName(String contentPropertyName) throws java.lang.IllegalArgumentException {
		if (this.validContentPropertyName(contentPropertyName)) {
			return getPropertyName(this.basePropertyName, this.delimiter, contentPropertyName);
		} else {
			throw new IllegalArgumentException("Invalid content property name!");
		}
	}

	/**
	 * The property names of property content names.
	 * 
	 * @param propertyNames The property content names.
	 * @return the list containing the absolute property names of the given property
	 *         names.
	 * @throws NullPointerExcpetion The given property names was undefined.
	 */
	public java.util.Set<String> getPropertyNames(java.util.Collection<String> propertyNames) {
		java.util.TreeSet<String> result = new java.util.TreeSet<>();
		propertyNames.parallelStream().forEach((String key) -> {
			result.add(this.getPropertyName(key));
		});
		return result;
	}

	/**
	 * The string representation of the path is the combination of path entries.
	 * 
	 * @return {@inheritDoc}
	 */
	@Override
	public String toString() {
		return String.join("", this.getEntries());
	}
}