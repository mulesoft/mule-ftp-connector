/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp.api;

import org.junit.Test;
import java.net.URI;
import java.util.regex.PatternSyntaxException;
import static org.junit.Assert.*;

public class UriUtilsTest {

  @Test
  public void testCreateUriWithSimplePath() {
    URI uri = UriUtils.createUri("/test/path");
    assertEquals("/test/path", uri.getPath());
  }

  @Test
  public void testCreateUriWithEmptyPath() {
    URI uri = UriUtils.createUri("");
    assertEquals("", uri.getPath());
  }

  @Test
  public void testCreateUriWithBasePathAndFilePath() {
    URI uri = UriUtils.createUri("/base", "file.txt");
    assertEquals("/base/file.txt", uri.getPath());
  }

  @Test
  public void testCreateUriWithAbsoluteFilePath() {
    URI uri = UriUtils.createUri("/base", "/absolute/path.txt");
    assertEquals("/absolute/path.txt", uri.getPath());
  }

  @Test
  public void testCreateUriWithEmptyFilePath() {
    URI uri = UriUtils.createUri("/base", "");
    assertEquals("/base", uri.getPath());
  }

  @Test(expected = IllegalPathException.class)
  public void testCreateUriWithNewlineCharacter() {
    UriUtils.createUri("/path\nwith\nnewlines");
  }

  @Test
  public void testNormalizeUri() {
    URI uri = UriUtils.createUri("/path/../to/./file");
    URI normalized = UriUtils.normalizeUri(uri);
    assertEquals("/to/file", normalized.getPath());
  }

  @Test
  public void testNormalizeUriWithTrailingSlash() {
    URI uri = UriUtils.createUri("/path/to/dir/");
    URI normalized = UriUtils.normalizeUri(uri);
    assertEquals("/path/to/dir", normalized.getPath());
  }

  @Test
  public void testTrimLastFragment() {
    URI uri = UriUtils.createUri("/path/to/file.txt");
    URI trimmed = UriUtils.trimLastFragment(uri);
    assertEquals("/path/to", trimmed.getPath());
  }

  @Test
  public void testTrimLastFragmentWithSingleLevel() {
    URI uri = UriUtils.createUri("/file.txt");
    URI trimmed = UriUtils.trimLastFragment(uri);
    assertEquals("", trimmed.getPath());
  }

  @Test
  public void testToRegexPatternWithSimpleGlob() {
    String regex = UriUtils.toRegexPattern("*.txt");
    assertEquals("^[^/]*\\.txt$", regex);
  }

  @Test
  public void testToRegexPatternWithDirectoryGlob() {
    String regex = UriUtils.toRegexPattern("**/*.txt");
    assertEquals("^.*/[^/]*\\.txt$", regex);
  }

  @Test
  public void testToRegexPatternWithCharacterClass() {
    String regex = UriUtils.toRegexPattern("[abc].txt");
    assertEquals("^[[^/]&&[abc]]\\.txt$", regex);
  }

  @Test
  public void testToRegexPatternWithGroup() {
    String regex = UriUtils.toRegexPattern("{a,b,c}.txt");
    assertEquals("^(?:(?:a)|(?:b)|(?:c))\\.txt$", regex);
  }

  @Test(expected = PatternSyntaxException.class)
  public void testToRegexPatternWithInvalidRange() {
    UriUtils.toRegexPattern("[z-a]");
  }

  @Test(expected = PatternSyntaxException.class)
  public void testToRegexPatternWithUnclosedGroup() {
    UriUtils.toRegexPattern("{a,b");
  }

  @Test(expected = PatternSyntaxException.class)
  public void testToRegexPatternWithNestedGroups() {
    UriUtils.toRegexPattern("{{a,b}}");
  }

  @Test(expected = PatternSyntaxException.class)
  public void testToRegexPatternWithInvalidEscape() {
    UriUtils.toRegexPattern("\\");
  }

  @Test
  public void testToRegexPatternWithEscapedCharacters() {
    String regex = UriUtils.toRegexPattern("\\*.txt");
    assertEquals("^\\*\\.txt$", regex);
  }
}
