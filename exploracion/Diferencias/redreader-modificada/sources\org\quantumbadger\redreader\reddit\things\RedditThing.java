package org.quantumbadger.redreader.reddit.things;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import org.quantumbadger.redreader.jsonwrap.JsonBufferedObject;

public final class RedditThing {
    private static final Map<String, Kind> kinds = new HashMap();
    public JsonBufferedObject data;
    public String kind;

    public enum Kind {
        POST,
        USER,
        COMMENT,
        MESSAGE,
        SUBREDDIT,
        MORE_COMMENTS,
        LISTING
    }

    static {
        kinds.put("t1", Kind.COMMENT);
        kinds.put("t2", Kind.USER);
        kinds.put("t3", Kind.POST);
        kinds.put("t4", Kind.MESSAGE);
        kinds.put("t5", Kind.SUBREDDIT);
        kinds.put("more", Kind.MORE_COMMENTS);
        kinds.put("Listing", Kind.LISTING);
    }

    public Kind getKind() {
        return (Kind) kinds.get(this.kind);
    }

    public RedditMoreComments asMoreComments() throws InstantiationException, IllegalAccessException, InterruptedException, IOException, NoSuchMethodException, InvocationTargetException {
        return (RedditMoreComments) this.data.asObject(RedditMoreComments.class);
    }

    public RedditComment asComment() throws InstantiationException, IllegalAccessException, InterruptedException, IOException, NoSuchMethodException, InvocationTargetException {
        return (RedditComment) this.data.asObject(RedditComment.class);
    }

    public RedditPost asPost() throws InstantiationException, IllegalAccessException, InterruptedException, IOException, NoSuchMethodException, InvocationTargetException {
        return (RedditPost) this.data.asObject(RedditPost.class);
    }

    public RedditSubreddit asSubreddit() throws InstantiationException, IllegalAccessException, InterruptedException, IOException, NoSuchMethodException, InvocationTargetException {
        return (RedditSubreddit) this.data.asObject(RedditSubreddit.class);
    }

    public RedditUser asUser() throws InstantiationException, IllegalAccessException, InterruptedException, IOException, NoSuchMethodException, InvocationTargetException {
        return (RedditUser) this.data.asObject(RedditUser.class);
    }

    public RedditMessage asMessage() throws IllegalAccessException, InterruptedException, InstantiationException, InvocationTargetException, NoSuchMethodException, IOException {
        return (RedditMessage) this.data.asObject(RedditMessage.class);
    }
}
