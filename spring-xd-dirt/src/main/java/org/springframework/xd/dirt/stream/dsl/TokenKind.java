/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.xd.dirt.stream.dsl;

// TODO [Andy] more tokens than needed for SpringXD, trim down
/**
 * @author Andy Clement
 */
enum TokenKind {
	
	// For Spring XD
	IDENTIFIER,
	DOUBLE_MINUS("--"),
	EQUALS("="),
	PIPE("|"),
	
	// ordered by priority - operands first
	LITERAL_INT, LITERAL_LONG, LITERAL_HEXINT, LITERAL_HEXLONG, LITERAL_STRING, LITERAL_REAL, LITERAL_REAL_FLOAT,
	LPAREN("("), RPAREN(")"), COMMA(","),
	COLON(":"),HASH("#"),RSQUARE("]"), LSQUARE("["),
	LCURLY("{"),RCURLY("}"),
	DOT("."), PLUS("+"), STAR("*"),  MINUS("-"), SELECT_FIRST("^["), SELECT_LAST("$["), QMARK("?"), PROJECT("!["),
	DIV("/"), GE(">="), GT(">"), LE("<="), LT("<"), EQ("=="), NE("!="),
	MOD("%"), NOT("!"),  INSTANCEOF("instanceof"), MATCHES("matches"), BETWEEN("between"),
	SELECT("?["),   POWER("^"),
	ELVIS("?:"), SAFE_NAVI("?."), BEAN_REF("@"), SYMBOLIC_OR("||"), SYMBOLIC_AND("&&"), INC("++"), 
	;

	char[] tokenChars;
	private boolean hasPayload; // is there more to this token than simply the kind

	private TokenKind(String tokenString) {
		tokenChars = tokenString.toCharArray();
		hasPayload = tokenChars.length==0;
	}

	private TokenKind() {
		this("");
	}

	@Override
	public String toString() {
		return this.name()+(tokenChars.length!=0?"("+new String(tokenChars)+")":"");
	}

	public boolean hasPayload() {
		return hasPayload;
	}

	public int getLength() {
		return tokenChars.length;
	}
}
