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

import java.util.ArrayList;
import java.util.List;

import org.springframework.xd.dirt.stream.dsl.ast.ArgumentNode;
import org.springframework.xd.dirt.stream.dsl.ast.ModuleNode;
import org.springframework.xd.dirt.stream.dsl.ast.StreamNode;

/**
 * @author Andy Clement
 */
public class StreamConfigParser {
	
	private String expressionString;
	private List<Token> tokenStream;
	private int tokenStreamLength;
	private int tokenStreamPointer; // Current location in the token stream when processing tokens

	public StreamNode parse(String stream) {
		try {
			this.expressionString = stream;
			Tokenizer tokenizer = new Tokenizer(expressionString);
			tokenStream = tokenizer.getTokens();
			tokenStreamLength = tokenStream.size();
			tokenStreamPointer = 0;
			StreamNode ast = eatStream();
			if (moreTokens()) {
				throw new SpelParseException(peekToken().startpos,XDDSLMessages.MORE_INPUT,toString(nextToken()));
			}
			return ast;
		} catch (InternalParseException ipe) {
			throw ipe.getCause();
		}
	}
	
	// stream: module (| module)*
	private StreamNode eatStream() {
		List<ModuleNode> moduleNodes= new ArrayList<ModuleNode>();
		moduleNodes.add(eatModule());
		while (moreTokens()) {
			Token t = peekToken();
			if (t.kind == TokenKind.PIPE) {
				nextToken();
				moduleNodes.add(eatModule());
			} else {
				raiseInternalException(t.startpos,XDDSLMessages.UNEXPECTED_DATA_AFTER_MODULE,toString(peekToken()));
			}
		}
		StreamNode streamNode= new StreamNode(this.expressionString, moduleNodes);
		return streamNode;
	}
	
	// module: identifier (moduleArguments)*
	private ModuleNode eatModule() {
		Token moduleName = eatToken(TokenKind.IDENTIFIER);
		ArgumentNode[] args = maybeEatModuleArgs();
		return new ModuleNode(moduleName.data, moduleName.startpos, moduleName.endpos, args);
	}
	
	// moduleArguments : DOUBLE_MINUS identifier(name) EQUALS identifier(value)	
	private ArgumentNode[] maybeEatModuleArgs() {
		List<ArgumentNode> args = null;
		while (peekToken(TokenKind.DOUBLE_MINUS)) {
			nextToken(); // skip the '--'
			if (peekToken(TokenKind.IDENTIFIER) && !isNextTokenAdjacent()) {
				raiseInternalException(peekToken().startpos, XDDSLMessages.NO_WHITESPACE_BEFORE_ARG_NAME);
			}
			Token argName = eatToken(TokenKind.IDENTIFIER);
			if (peekToken(TokenKind.EQUALS) && !isNextTokenAdjacent()) {
				raiseInternalException(peekToken().startpos, XDDSLMessages.NO_WHITESPACE_BEFORE_ARG_EQUALS);
			}
			eatToken(TokenKind.EQUALS);
			if (peekToken(TokenKind.IDENTIFIER) && !isNextTokenAdjacent()) {
				raiseInternalException(peekToken().startpos, XDDSLMessages.NO_WHITESPACE_BEFORE_ARG_VALUE);
			}
			// Process argument value:
			Token t = peekToken();
			String argValue = null;
			if (t.getKind()==TokenKind.IDENTIFIER) {
				argValue = t.data;
			} else if (t.getKind() == TokenKind.LITERAL_STRING) {
				argValue = t.data.substring(1,t.data.length()-1).replaceAll("''", "'").replaceAll("\"\"", "\"");
			} else {
				raiseInternalException(t.startpos,XDDSLMessages.EXPECTED_ARGUMENT_VALUE,t.data);
			}
			nextToken();
			if (args == null) {
				args = new ArrayList<ArgumentNode>();
			}
			args.add(new ArgumentNode(argName.data,argValue,argName.startpos-2,t.endpos));
		}
		return args==null?null:args.toArray(new ArgumentNode[args.size()]);
	}
	
	private Token eatToken(TokenKind expectedKind) {
		Token t = nextToken();
		if (t==null) {
			raiseInternalException( expressionString.length(), XDDSLMessages.OOD);
		}
		if (t.kind!=expectedKind) {
			raiseInternalException(t.startpos,XDDSLMessages.NOT_EXPECTED_TOKEN, expectedKind.toString().toLowerCase(),t.getKind().toString().toLowerCase());
		}
		return t;
	}

	private boolean peekToken(TokenKind desiredTokenKind) {
		return peekToken(desiredTokenKind,false);
	}

	private boolean peekToken(TokenKind desiredTokenKind, boolean consumeIfMatched) {
		if (!moreTokens()) {
			return false;
		}
		Token t = peekToken();
		if (t.kind==desiredTokenKind) {
			if (consumeIfMatched) {
				tokenStreamPointer++;
			}
			return true;
		} else {
			if (desiredTokenKind == TokenKind.IDENTIFIER) {
				// might be one of the textual forms of the operators (e.g. NE for != ) - in which case we can treat it as an identifier
				// The list is represented here: Tokenizer.alternativeOperatorNames and those ones are in order in the TokenKind enum
				if (t.kind.ordinal()>=TokenKind.DIV.ordinal() && t.kind.ordinal()<=TokenKind.NOT.ordinal() && t.data!=null) {
					// if t.data were null, we'd know it wasn't the textual form, it was the symbol form
					return true;
				}
			}
			return false;
		}
	}
	
	private boolean moreTokens() {
		return tokenStreamPointer<tokenStream.size();
	}

	private Token nextToken() {
		if (tokenStreamPointer>=tokenStreamLength) {
			return null;
		}
		return tokenStream.get(tokenStreamPointer++);
	}
	
	private boolean isNextTokenAdjacent() {
		if (tokenStreamPointer>=tokenStreamLength) {
			return false;
		}
		Token last = tokenStream.get(tokenStreamPointer-1);
		Token next = tokenStream.get(tokenStreamPointer);
		return next.startpos==last.endpos;
	}

	private Token peekToken() {
		if (tokenStreamPointer>=tokenStreamLength) {
			return null;
		}
		return tokenStream.get(tokenStreamPointer);
	}

	private void raiseInternalException(int pos, XDDSLMessages message,Object... inserts) {
		throw new InternalParseException(new SpelParseException(expressionString,pos,message,inserts));
	}

	public String toString(Token t) {
		if (t.getKind().hasPayload()) {
			return t.stringValue();
		} else {
			return t.kind.toString().toLowerCase();
		}
	}
}
