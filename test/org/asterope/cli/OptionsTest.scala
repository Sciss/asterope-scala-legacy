package org.asterope.cli

import org.asterope.util.ScalaTestCase

/**
 * Tests the use of the options parser
 *
 * @version $Revision : 1.1 $
 */

case class Config(var foo: Int = -1,
  var bar: String = null,
  var xyz: Boolean = false,
  var libname: String = null,
  var libfile: String = null,
  var maxlibname: String = null,
  var maxcount: Int = -1,
  var whatnot: String = null,
  var files: List[String] = Nil)

class OptionsTest extends ScalaTestCase {
  var config: Config = _

  val parser1 = new OptionParser("scopt") {
    intOpt("f", "foo", "foo is an integer property", {v: Int => config.foo = v})
    opt("o", "output", "<file>", "output is a string property", {v: String => config.bar = v})
    booleanOpt("x", "xyz", "xyz is a boolean property", {v: Boolean => config.xyz = v})
    keyValueOpt("l", "lib", "<libname>", "<filename>", "load library <libname>",
      {(key: String, value: String) => { config.libname = key; config.libfile = value } })
    keyIntValueOpt("m", "max", "<libname>", "<max>", "maximum count for <libname>",
      {(key: String, value: Int) => { config.maxlibname = key; config.maxcount = value } })
    arg("<file>", "some argument", {v: String => config.whatnot = v})
  }
  
  def test_valid_arguments_are_parsed_correctly {
    validArguments(parser1, Config(whatnot = "blah"), "blah")
    validArguments(parser1, Config(foo = 35, whatnot = "abc"), "-f", "35", "abc")
    validArguments(parser1, Config(foo = 22, bar = "beer", whatnot = "drink"), "-o", "beer", "-f", "22", "drink")
    validArguments(parser1, Config(foo = 22, bar = "beer", whatnot = "drink"), "-f", "22", "--output", "beer", "drink")
    validArguments(parser1, Config(libname = "key", libfile = "value", whatnot = "drink"), "--lib:key=value", "drink")
    validArguments(parser1, Config(maxlibname = "key", maxcount = 5, whatnot = "drink"), "-m:key=5", "drink")
  }

  def test_invalid_arguments_fail{
    invalidArguments(parser1)
    invalidArguments(parser1, "-z", "blah")
    invalidArguments(parser1, "blah", "blah")
    invalidArguments(parser1, "-z", "abc", "blah")
    invalidArguments(parser1, "-f", "22", "-z", "abc", "blah")
  }

  def test_bad_numbers_fail_to_parse_nicely {
    invalidArguments(parser1, "-f", "shouldBeNumber", "blah")
  }

  def test_bad_booleans_fail_to_parse_nicely {
    invalidArguments(parser1, "-x", "shouldBeBoolean", "blah")
  }

  val parser2 = new OptionParser("scopt") {
    arglist("<file>...", "some argument", {v: String => config.files = (v :: config.files).reverse })
  }
  
  def test_valid_argument_list_is_parsed_correctly {
    validArguments(parser2, Config(files = List("foo", "bar")), "foo", "bar")
  }
  
  def validArguments(parser: OptionParser,
      expectedConfig: Config, args: String*) {
    config = new Config()
    expect(true) {
      parser.parse(args)
    }

    expect(expectedConfig) {
      config
    }
  }

  def invalidArguments(parser: OptionParser,
      args: String*) {
    config = new Config()
    expect(false) {
      parser.parse(args)
    }
  }
}