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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegexValidator {
	private final Pattern[] patterns;

	public RegexValidator(String regex) {
		this(regex,  true);
	}

	public RegexValidator(String regex, boolean caseSensitive) {
		this(new String[] {regex}, caseSensitive);
	}

	public RegexValidator(String[] regexs, boolean caseSensitive) {
		if (regexs == null || regexs.length == 0) {
			throw new IllegalArgumentException("Regular expressions are missing");
		}
		patterns = new Pattern[regexs.length];
		int flags =  (caseSensitive ? 0: Pattern.CASE_INSENSITIVE);
		for (int i = 0; i < regexs.length; i++) {
			if (regexs[i] == null || regexs[i].length() == 0) {
				throw new IllegalArgumentException("Regular expression[" + i + "] is missing");
			}
			patterns[i] =  Pattern.compile(regexs[i], flags);
		}
	}

	/*
	 * Validate a value against the set of regular expressions.
	 */
	public boolean isValid(String value) {
		if (value == null) return false;
		for (int i = 0; i < patterns.length; i++) {
			if (patterns[i].matcher(value).matches()) return true;
		}
		return false;
	}

	/*
	 * Validate a value against the set of regular expressions
	 * returning the array of matched groups.
	 */
	public String[] match(String value) {
		if (value == null) return null;
		for (int i = 0; i < patterns.length; i++) {
			Matcher matcher = patterns[i].matcher(value);
			if (matcher.matches()) {
				int count = matcher.groupCount();
				String[] groups = new String[count];
				for (int j = 0; j < count; j++) {
					groups[j] = matcher.group(j+1);
				}
				return groups;
			}
		}
		return null;
	}
}
