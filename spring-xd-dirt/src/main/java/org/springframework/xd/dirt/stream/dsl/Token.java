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

/**
 * Holder for a kind of token, the associated data and its position in the input data stream (start/end).
 *
 * @author Andy Clement
 */
class Token {

	TokenKind kind;
	String data;
	int startpos; // index of first character
	int endpos;   // index of char after the last character

	/**
	 * Constructor for use when there is no particular data for the token (eg. TRUE or '+')
	 * @param startpos the exact start
	 * @param endpos the index to the last character
	 */
	Token(TokenKind tokenKind, int startpos, int endpos) {
		this.kind = tokenKind;
		this.startpos = startpos;
		this.endpos = endpos;
	}

	Token(TokenKind tokenKind, char[] tokenData, int pos, int endpos) {
		this(tokenKind,pos,endpos);
		this.data = new String(tokenData);
	}


	public TokenKind getKind() {
		return kind;
	}

	@Override
	public String toString() {
		StringBuilder s = new StringBuilder();
		s.append("[").append(kind.toString());
		if (kind.hasPayload()) {
			s.append(":").append(data);
		}
		s.append("]");
		s.append("(").append(startpos).append(",").append(endpos).append(")");
		return s.toString();
	}

	public boolean isIdentifier() {
		return kind==TokenKind.IDENTIFIER;
	}

	public boolean isNumericRelationalOperator() {
		return kind==TokenKind.GT || kind==TokenKind.GE || kind==TokenKind.LT || kind==TokenKind.LE || kind==TokenKind.EQ || kind==TokenKind.NE;
	}

	public String stringValue() {
		return data;
	}

	public Token asInstanceOfToken() {
		return new Token(TokenKind.INSTANCEOF,startpos,endpos);
	}

	public Token asMatchesToken() {
		return new Token(TokenKind.MATCHES,startpos,endpos);
	}

	public Token asBetweenToken() {
		return new Token(TokenKind.BETWEEN,startpos,endpos);
	}
	
	public int hashCode() {
		return this.kind.ordinal()*37+(this.startpos+this.endpos)*37+(this.kind.hasPayload()?this.data.hashCode():0);
	}
	
	public boolean equals(Object o) {
		if (!(o instanceof Token)) {
			return false;
		}
		Token token = (Token)o;
		boolean basicmatch = this.kind == token.kind && this.startpos == token.startpos && this.endpos == token.endpos;
		if (!basicmatch) return false;
		if (this.kind.hasPayload()) {
			if (!this.data.equals(token.data)) {
				return false;
			}
		}
		return true;
	}
}
