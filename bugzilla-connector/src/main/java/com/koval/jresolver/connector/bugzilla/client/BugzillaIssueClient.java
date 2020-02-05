package com.koval.jresolver.connector.bugzilla.client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.koval.jresolver.common.api.bean.issue.Issue;
import com.koval.jresolver.common.api.bean.issue.IssueField;
import com.koval.jresolver.common.api.component.connector.IssueClient;
import com.koval.jresolver.common.api.component.connector.IssueTransformer;
import com.koval.jresolver.connector.bugzilla.configuration.BugzillaQuery;
import com.koval.jresolver.connector.bugzilla.configuration.BugzillaQueryParser;

import b4j.core.DefaultSearchData;
import b4j.core.session.BugzillaHttpSession;


public class BugzillaIssueClient implements IssueClient {

  private static final Logger LOGGER = LoggerFactory.getLogger(BugzillaIssueClient.class);

  private final BugzillaHttpSession session;
  private final IssueTransformer<b4j.core.Issue> issueTransformer;

  BugzillaIssueClient(BugzillaHttpSession session) {
    this.session = session;
    this.issueTransformer = new BugzillaIssueTransformer();
    session.open();
  }

  @Override
  public int getTotalIssues(String query) {
    return 1000000;
  }

  @Override
  public List<Issue> search(String query, int maxResults, int startAt, List<String> fields) {
    LOGGER.debug("Send search request: Query = '{}' MaxResults = '{}' StartAt = '{}'.", query, maxResults, startAt);
    DefaultSearchData searchData = getSearchDataByQuery(query);

    searchData.add("offset", String.valueOf(startAt));
    searchData.add("limit", String.valueOf(maxResults));

    Iterable<b4j.core.Issue> issues = session.searchBugs(searchData, null);
    List<b4j.core.Issue> rawIssueList = new ArrayList<>();
    for (b4j.core.Issue issue : issues) {
      LOGGER.info("Bug found: " + issue.getId() + " - " + issue.getSummary());
      rawIssueList.add(issue);
    }
    return issueTransformer.transform(rawIssueList);
  }

  private DefaultSearchData getSearchDataByQuery(String query) {
    DefaultSearchData searchData = new DefaultSearchData();

    BugzillaQueryParser queryParser = new BugzillaQueryParser();
    BugzillaQuery parsedQuery = queryParser.parse(query);
    if (parsedQuery.getAssignee() != null) {
      searchData.add("assignee", parsedQuery.getAssignee());
    }
    if (parsedQuery.getReporter() != null) {
      searchData.add("reporter", parsedQuery.getReporter());
    }
    if (parsedQuery.getProduct() != null) {
      searchData.add("product", parsedQuery.getProduct());
    }
    if (parsedQuery.getStatus() != null) {
      searchData.add("status", parsedQuery.getStatus());
    }
    if (parsedQuery.getResolution() != null) {
      searchData.add("resolution", parsedQuery.getResolution());
    }
    if (parsedQuery.getPriority() != null) {
      searchData.add("priority", parsedQuery.getPriority());
    }
    if (parsedQuery.getVersion() != null) {
      searchData.add("version", parsedQuery.getVersion());
    }

    return searchData;
  }

  @Override
  public Issue getIssueByKey(String issueKey) {
    return issueTransformer.transform(session.getIssue(issueKey));
  }

  @Override
  public List<IssueField> getIssueFields() {
    DefaultSearchData searchData = new DefaultSearchData();
    searchData.add("offset", "0");
    searchData.add("limit", "1");
    Iterator<b4j.core.Issue> issues = session.searchBugs(searchData, null).iterator();
    List<IssueField> issueFields = new ArrayList<>();
    if (issues.hasNext()) {
      issues.next().getCustomFieldNames().forEach((fieldName) -> {
        issueFields.add(new IssueField(fieldName));
      });
    }
    return issueFields;
  }

  @Override
  public void close() throws IOException {
    session.close();
  }
}