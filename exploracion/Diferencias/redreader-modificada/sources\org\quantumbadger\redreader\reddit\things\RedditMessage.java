package org.quantumbadger.redreader.reddit.things;

import org.apache.commons.lang3.StringEscapeUtils;
import org.quantumbadger.redreader.jsonwrap.JsonValue;

public class RedditMessage {
    public boolean _json_new;
    public String author;
    public String body;
    public String body_html;
    public String context;
    public long created;
    public long created_utc;
    public JsonValue first_message;
    public String name;
    public String parent_id;
    public JsonValue replies;
    public String subject;
    public String subreddit;
    public boolean was_comment;

    public String getUnescapedBodyMarkdown() {
        return StringEscapeUtils.unescapeHtml4(this.body);
    }
}
