/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mule.extension.ftp.api.ftp.FtpFileAttributes;
import org.mule.extension.ftp.api.matchers.FileMatcher;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.rules.ExpectedException.none;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.extension.ftp.api.matchers.MatchPolicy.EXCLUDE;
import static org.mule.extension.ftp.api.matchers.MatchPolicy.REQUIRE;

@SmallTest
public class FileMatcherContractTestCase<T extends FileMatcher, A extends FtpFileAttributes>
    extends AbstractMuleTestCase {

  private static final String FILENAME = "Mule.java";
  private static final String PATH = "a/b/c/" + FILENAME;
  private static final long SIZE = 1024;

  protected T builder = createPredicateBuilder();
  protected A attributes;

  @Rule
  public ExpectedException expectedException = none();

  @Before
  public void before() {
    attributes = mock(getFileAttributesClass());
    when(attributes.getName()).thenReturn(FILENAME);
    when(attributes.getPath()).thenReturn(PATH);
    when(attributes.getSize()).thenReturn(SIZE);
    when(attributes.isRegularFile()).thenReturn(true);
    when(attributes.isSymbolicLink()).thenReturn(false);
    when(attributes.isDirectory()).thenReturn(false);
  }

  private class TestFileMatcher extends FileMatcher {

  }

  protected T createPredicateBuilder() {
    return (T) new TestFileMatcher();
  }

  protected Class<A> getFileAttributesClass() {
    return (Class<A>) FtpFileAttributes.class;
  }

  @Test
  public void matchesAll() {
    builder.setFilenamePattern("glob:*.{java, js}")
        .setPathPattern("glob:**.{java, js}")
        .setRegularFiles(REQUIRE)
        .setDirectories(EXCLUDE)
        .setSymLinks(EXCLUDE)
        .setMinSize(1L)
        .setMaxSize(1024L);

    assertMatch();
  }

  @Test
  public void matchesManyButFailsOne() {
    matchesAll();
    builder.setMaxSize(1L);

    assertReject();
  }

  @Test
  public void matchFilenameLiterally() {
    builder.setFilenamePattern(FILENAME);
    assertMatch();
  }

  @Test
  public void rejectFilenameLiterally() {
    builder.setFilenamePattern("fail.pdf");
    assertReject();
  }

  @Test
  public void matchFilenameByGlob() {
    builder.setFilenamePattern("glob:*.{java, js}");
    assertMatch();
  }

  @Test
  public void rejectFilenameByGlob() {
    builder.setFilenamePattern("glob:*.{pdf}");
    assertReject();
  }

  @Test
  public void matchFilenameByRegex() {
    when(attributes.getName()).thenReturn("20060101_test.csv");
    builder.setFilenamePattern("regex:[0-9]*_test.csv");
    assertMatch();
  }

  @Test
  public void rejectFilenameByRegex() {
    when(attributes.getName()).thenReturn("20060101_TEST.csv");
    builder.setFilenamePattern("regex:[0-9]*_test.csv");
    assertReject();
  }

  @Test
  public void matchPathLiterally() {
    builder.setPathPattern(PATH);
    assertMatch();
  }

  @Test
  public void rejectPathLiterally() {
    builder.setPathPattern("a/b/d/Mule.pdf");
    assertReject();
  }

  @Test
  public void matchPathByGlob() {
    builder.setPathPattern("glob:**.{java, js}");
    assertMatch();
  }

  @Test
  public void rejectPathByGlob() {
    builder.setPathPattern("glob:*.{java, js}");
    assertReject();
  }

  @Test
  public void matchPathByRegex() {
    when(attributes.getPath()).thenReturn("a/b/c/20060101_test.csv");
    builder.setPathPattern("regex:a/b/c/[0-9]*_test.csv");
    assertMatch();
  }

  @Test
  public void rejectPathByRegex() {
    when(attributes.getName()).thenReturn("20060101_TEST.csv");
    builder.setFilenamePattern("regex:[0-9]*_test.csv");
    assertReject();
  }

  @Test
  public void minSize() {
    builder.setMinSize(1L);
    assertMatch();
  }

  @Test
  public void maxSize() {
    builder.setMaxSize(1024L);
    assertMatch();
  }

  @Test
  public void rejectMinSize() {
    builder.setMinSize(2048L);
    assertReject();
  }

  @Test
  public void rejectMaxSize() {
    builder.setMaxSize(500L);
    assertReject();
  }

  @Test
  public void regularFile() {
    when(attributes.isRegularFile()).thenReturn(true);
    builder.setRegularFiles(REQUIRE);
    assertMatch();
  }

  @Test
  public void rejectNotRegularFile() {
    when(attributes.isRegularFile()).thenReturn(false);
    builder.setRegularFiles(REQUIRE);
    assertReject();
  }

  @Test
  public void rejectRegularFile() {
    when(attributes.isRegularFile()).thenReturn(true);
    builder.setRegularFiles(EXCLUDE);
    assertReject();
  }

  @Test
  public void isDirectory() {
    when(attributes.isDirectory()).thenReturn(true);
    builder.setDirectories(REQUIRE);
    assertMatch();
  }

  @Test
  public void rejectNotDirectory() {
    when(attributes.isDirectory()).thenReturn(false);
    builder.setDirectories(REQUIRE);
    assertReject();
  }

  @Test
  public void rejectDirectory() {
    when(attributes.isDirectory()).thenReturn(true);
    builder.setDirectories(EXCLUDE);
    assertReject();
  }


  @Test
  public void isSymbolicLink() {
    when(attributes.isSymbolicLink()).thenReturn(true);
    builder.setSymLinks(REQUIRE);
    assertMatch();
  }

  @Test
  public void rejectNotSymbolicLink() {
    when(attributes.isSymbolicLink()).thenReturn(false);
    builder.setSymLinks(REQUIRE);
    assertReject();
  }

  @Test
  public void rejectSymbolicLink() {
    when(attributes.isSymbolicLink()).thenReturn(true);
    builder.setSymLinks(EXCLUDE);
    assertReject();
  }

  @Test
  public void failOnInvalidMinSize() {
    expectedException.expect(IllegalArgumentException.class);
    builder.setMinSize(-1L);
    builder.build();
  }

  @Test
  public void failOnInvalidMaxSize() {
    expectedException.expect(IllegalArgumentException.class);
    builder.setMaxSize(-1L);
    builder.build();
  }

  protected void assertMatch() {
    assertThat(builder.build().test(attributes), is(true));
  }

  protected void assertReject() {
    assertThat(builder.build().test(attributes), is(false));
  }
}
