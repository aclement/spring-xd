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

import java.util.List;
import java.util.Properties;

import org.junit.Test;
import org.springframework.xd.dirt.stream.EnhancedStreamParser;
import org.springframework.xd.dirt.stream.dsl.ast.ArgumentNode;
import org.springframework.xd.dirt.stream.dsl.ast.ModuleNode;
import org.springframework.xd.dirt.stream.dsl.ast.StreamNode;

import static org.junit.Assert.*;

/**
 * @author Andy Clement
 */
public class ParserTests {

	@Test
	public void oneModule() {
		StreamNode ast = new StreamConfigParser().parse("foo");
		assertEquals("Stream[foo](ModuleNode:foo:0>3)",ast.stringify());
	}

	@Test
	public void twoModules() {
		StreamNode ast = new StreamConfigParser().parse("foo | bar");
		assertEquals("Stream[foo | bar](ModuleNode:foo:0>3)(ModuleNode:bar:6>9)",ast.stringify());
	}

	@Test
	public void oneModuleWithParam() {
		StreamNode ast = new StreamConfigParser().parse("foo --name=value");
		assertEquals("Stream[foo --name=value](ModuleNode:foo --name=value:0>16)",ast.stringify());
	}
	
	@Test
	public void needAdjacentTokens() {
		checkForParseError("foo -- name=value",XDDSLMessages.NO_WHITESPACE_BEFORE_ARG_NAME,7);
		checkForParseError("foo --name =value",XDDSLMessages.NO_WHITESPACE_BEFORE_ARG_EQUALS,11);
		checkForParseError("foo --name= value",XDDSLMessages.NO_WHITESPACE_BEFORE_ARG_VALUE,12);
	}
	
	@Test
	public void oneModuleWithTwoParams() {
		StreamNode ast = new StreamConfigParser().parse("foo --name=value --x=y");
		assertTrue(ast instanceof StreamNode);
		StreamNode sn = (StreamNode)ast;
		List<ModuleNode> moduleNodes = sn.getModuleNodes();
		assertEquals(1,moduleNodes.size());
		
		ModuleNode mn = moduleNodes.get(0);
		assertEquals("foo",mn.getName());
		ArgumentNode[] args = mn.getArguments();
		assertNotNull(args);
		assertEquals(2,args.length);
		assertEquals("name",args[0].getName());
		assertEquals("value",args[0].getValue());
		assertEquals("x",args[1].getName());
		assertEquals("y",args[1].getValue());
		
		assertEquals("Stream[foo --name=value --x=y](ModuleNode:foo --name=value --x=y:0>22)",ast.stringify());
	}
	
	@Test
	public void testParameters() {
		String module = "gemfire-cq --query='Select * from /Stocks where symbol=''VMW''' --regionName=foo --foo=bar";
		StreamConfigParser parser = new StreamConfigParser();
		StreamNode ast = parser.parse(module);
		ModuleNode gemfireModule = ast.getModule("gemfire-cq");
		Properties parameters = gemfireModule.getArgumentsAsProperties(module);
		assertEquals(3, parameters.size());
		assertEquals("Select * from /Stocks where symbol='VMW'", parameters.get("query"));
		assertEquals("foo", parameters.get("regionName"));
		assertEquals("bar", parameters.get("foo"));

		module = "test";
		parameters = parser.parse(module).getModule("test").getArgumentsAsProperties(module);
		assertEquals(0, parameters.size());

		module = "foo --x=1 --y=two ";
		parameters = parser.parse(module).getModule("foo").getArgumentsAsProperties(module);
		assertEquals(2, parameters.size());
		assertEquals("1", parameters.get("x"));
		assertEquals("two", parameters.get("y"));
		
		module = "foo --x=1a2b --y=two ";
		parameters = parser.parse(module).getModule("foo").getArgumentsAsProperties(module);
		assertEquals(2, parameters.size());
		assertEquals("1a2b", parameters.get("x"));
		assertEquals("two", parameters.get("y"));

		module = "foo --x=2";
		parameters = parser.parse(module).getModule("foo").getArgumentsAsProperties(module);
		assertEquals(1, parameters.size());
		assertEquals("2", parameters.get("x"));
		
		module = "--foo = bar";
		try {
			parser.parse(module);
			fail(module + " is invalid. Should throw exception");
		} catch (Exception e) {
			// success
		}
	}

	@Test
	public void testInvalidModules() {
		String config = "test | foo--x=13";
		EnhancedStreamParser parser = new EnhancedStreamParser();
		try {
			parser.parse("t", config);
			fail(config + " is invalid. Should throw exception");
		} catch (Exception e) {
			// success
		}
	}

	// ---
	
	private void checkForParseError(String stream,XDDSLMessages msg,int pos) {
		try {
			new StreamConfigParser().parse(stream);
			fail("expected to fail");
		} catch (SpelParseException e) {
			assertEquals(msg,e.getMessageCode());
			assertEquals(pos,e.getPosition());
		}
	}
}