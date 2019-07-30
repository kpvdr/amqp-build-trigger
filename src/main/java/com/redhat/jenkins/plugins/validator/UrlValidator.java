/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.redhat.jenkins.plugins.validator;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UrlValidator {
	private static final int MAX_UNSIGNED_16_BIT_INT = 0xFFFF; // port max
	private static final String[] DEFAULT_SCHEMES = {"amqp", "amqps"}; // Must be lower-case
	private static final String URL_REGEX =
			"^(([^:/?#]+):)?(//([^/?#]*))?([^?#]*)(\\?([^#]*))?(#(.*))?";
    //        12            3  4          5       6   7        8 9
	private static final Pattern URL_PATTERN = Pattern.compile(URL_REGEX);
	private static final int PARSE_URL_SCHEME = 2;
	private static final int PARSE_URL_AUTHORITY = 4;
	private static final int PARSE_URL_PATH = 5;
	private static final int PARSE_URL_QUERY = 7;
	private static final int PARSE_URL_FRAGMENT = 9;

	private static final String SCHEME_REGEX = "^\\p{Alpha}[\\p{Alnum}\\+\\-\\.]*";
	private static final Pattern SCHEME_PATTERN = Pattern.compile(SCHEME_REGEX);

	// Drop numeric, and  "+-." for now
	// TODO does not allow for optional userinfo.
	// Validation of character set is done by isValidAuthority
	private static final String AUTHORITY_CHARS_REGEX = "\\p{Alnum}\\-\\."; // allows for IPV4 but not IPV6
	private static final String IPV6_REGEX = "[0-9a-fA-F:]+"; // do this as separate match because : could cause ambiguity with port prefix

	// userinfo    = *( unreserved / pct-encoded / sub-delims / ":" )
	// unreserved    = ALPHA / DIGIT / "-" / "." / "_" / "~"
	// sub-delims    = "!" / "$" / "&" / "'" / "(" / ")" / "*" / "+" / "," / ";" / "="
	// We assume that password has the same valid chars as user info
	private static final String USERINFO_CHARS_REGEX = "[a-zA-Z0-9%-._~!$&'()*+,;=]";
	// since neither ':' nor '@' are allowed chars, we don't need to use non-greedy matching
	private static final String USERINFO_FIELD_REGEX =
			USERINFO_CHARS_REGEX + "+" + // At least one character for the name
			"(?::" + USERINFO_CHARS_REGEX + "*)?@"; // colon and password may be absent

	private static final String AUTHORITY_REGEX =
			"(?:\\[("+IPV6_REGEX+")\\]|(?:(?:"+USERINFO_FIELD_REGEX+")?([" + AUTHORITY_CHARS_REGEX + "]*)))(?::(\\d*))?(.*)?";
    //             1                          e.g. user:pass@          2                                         3       4	
	private static final Pattern AUTHORITY_PATTERN = Pattern.compile(AUTHORITY_REGEX);
	private static final int PARSE_AUTHORITY_IPV6 = 1;
	private static final int PARSE_AUTHORITY_HOST_IP = 2; // excludes userinfo, if present
	private static final int PARSE_AUTHORITY_PORT = 3; // excludes leading colon
	private static final int PARSE_AUTHORITY_EXTRA = 4; // Should always be empty. The code currently allows spaces.

	private static final String PATH_REGEX = "^(/[-\\w:@&?=+,.!/~*'%$_;\\(\\)]*)?$";
	private static final Pattern PATH_PATTERN = Pattern.compile(PATH_REGEX);

	// The set of schemes that are allowed to be in a URL.
	private final Set<String> allowedSchemes;

    public UrlValidator() {
        allowedSchemes = new HashSet<String>(DEFAULT_SCHEMES.length);
        for(int i=0; i < DEFAULT_SCHEMES.length; i++) {
            allowedSchemes.add(DEFAULT_SCHEMES[i].toLowerCase(Locale.ENGLISH));
        }
    }

    /*
     * Checks if a field has a valid url address.
     */
    public boolean isValid(String value) {
    	// Check for null
    	if (value == null) return false;

    	// Check the whole url address structure
    	Matcher urlMatcher = URL_PATTERN.matcher(value);
    	if (!urlMatcher.matches()) return false;
    	
    	if (!isValidScheme(urlMatcher.group(PARSE_URL_SCHEME))) return false;

    	if (!isValidAuthority(urlMatcher.group(PARSE_URL_AUTHORITY))) return false;

    	if (!isValidPath(urlMatcher.group(PARSE_URL_PATH))) return false;

    	if (!isValidQuery(urlMatcher.group(PARSE_URL_QUERY))) return false;

    	if (!isValidFragment(urlMatcher.group(PARSE_URL_FRAGMENT))) return false;

    	return true;
    }

    /*
     * Validate scheme. If schemes[] was initialized to a non null,
     * then only those schemes are allowed.
     */
    protected boolean isValidScheme(String scheme) {
    	if (scheme == null) return false;
    	if (!SCHEME_PATTERN.matcher(scheme).matches()) return false;
    	if (!allowedSchemes.contains(scheme.toLowerCase(Locale.ENGLISH))) return false;
    	return true;
    }

    /*
     * Returns true if the authority is properly formatted.  An authority is the combination
     * of hostname and port.  A <code>null</code> authority value is considered invalid.
     * Note: this implementation validates the domain.
     */
    protected boolean isValidAuthority(String authority) {
    	if (authority == null) return false;

    	// convert to ASCII if possible
    	final String authorityASCII = DomainValidator.unicodeToASCII(authority);
    	Matcher authorityMatcher = AUTHORITY_PATTERN.matcher(authorityASCII);
    	if (!authorityMatcher.matches()) return false;

    	// We have to process IPV6 separately because that is parsed in a different group
    	String ipv6 = authorityMatcher.group(PARSE_AUTHORITY_IPV6);
    	if (ipv6 != null) {
    		InetAddressValidator inetAddressValidator = InetAddressValidator.getInstance();
    		if (!inetAddressValidator.isValidInet6Address(ipv6)) return false;
    	} else {
    		String hostLocation = authorityMatcher.group(PARSE_AUTHORITY_HOST_IP);
    		// check if authority is hostname or IP address:
    		// try a hostname first since that's much more likely
    		DomainValidator domainValidator = DomainValidator.getInstance();
    		if (!domainValidator.isValid(hostLocation)) {
    			InetAddressValidator inetAddressValidator = InetAddressValidator.getInstance();
    			if (!inetAddressValidator.isValidInet4Address(hostLocation)) return false;
    		}
    		String port = authorityMatcher.group(PARSE_AUTHORITY_PORT);
    		if (port != null && port.length() > 0) {
    			try {
    				int iPort = Integer.parseInt(port);
    				if (iPort < 0 || iPort > MAX_UNSIGNED_16_BIT_INT) return false;
    			} catch (NumberFormatException nfe) {
    				return false;
    			}
    		}
    	}
    	String extra = authorityMatcher.group(PARSE_AUTHORITY_EXTRA);
    	if (extra != null && extra.trim().length() > 0) return false;

    	return true;
    }

    /*
     * Returns true if the path is valid.  A <code>null</code> value is considered invalid.
     */
    protected boolean isValidPath(String path) {
    	if (path == null) return false;
    	if (!PATH_PATTERN.matcher(path).matches()) return false;
    	try {
    		URI uri = new URI(null,null,path,null);
    		String norm = uri.normalize().getPath();
    		if (norm.startsWith("/../") || // Trying to go via the parent dir
    				norm.equals("/..")) {   // Trying to go to the parent dir
    			return false;
    		}
    	} catch (URISyntaxException e) {
    		return false;
    	}

    	// Disallow multiple slashes in path
    	int slash2Count = countToken("//", path);
    	if (slash2Count > 0) return false;

    	return true;
    }

    /*
     * Do not allow queries
     */
    protected boolean isValidQuery(String query) {
    	if (query != null) return false;
    	return true;
    }

    /*
     * Do not allow fragments
     */
    protected boolean isValidFragment(String fragment) {
    	if (fragment != null) return false;
    	return true;
    }

    protected int countToken(String token, String target) {
    	int tokenIndex = 0;
    	int count = 0;
    	while (tokenIndex != -1) {
    		tokenIndex = target.indexOf(token, tokenIndex);
    		if (tokenIndex > -1) {
    			tokenIndex++;
    			count++;
    		}
    	}
    	return count;
    }
}
