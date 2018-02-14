package org.wikipedia.ro.model;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.security.auth.login.LoginException;

import org.wikipedia.Wiki;
import org.wikipedia.ro.ops.Op;
import org.wikipedia.ro.parser.AggregatingParser;

/**
 * Models a Wiki page. To perform changes of a page, create an instance by specifying the title and the wiki, and then
 * calling {@link #load()}.
 * 
 * Changing operations can be performed by creating objects of type {@link Op} and adding them to this article by calling
 * {{@link #addOp(Op)}. In the end, {@link #save()} should be called in order to persist the changes.
 * 
 * @author acstroe
 *
 */
public class WikiPage {
    private String title;
    private String text;
    private Wiki wiki;
    private List<WikiPart> parts;
    private List<Op> ops = new ArrayList<>();

    public WikiPage(String title, Wiki wiki) {
        super();
        this.title = title;
        this.wiki = wiki;
    }

    public String getTitle() {
        return title;
    }

    public Wiki getWiki() {
        return wiki;
    }

    public WikiPage setTitle(String title) {
        this.title = title;
        return this;
    }

    public String getText() {
        return text;
    }

    public WikiPage setText(String text) {
        this.text = text;
        parts =
            new AggregatingParser().parse(text).stream().map(elem -> elem.getIdentifiedPart()).collect(Collectors.toList());
        return this;
    }

    public List<? extends WikiPart> getParts() {
        return parts;
    }

    public WikiPage setParts(List<WikiPart> parts) {
        this.parts = parts;
        return this;
    }

    public WikiPage addPart(WikiPart part) {
        if (null == parts) {
            parts = new ArrayList<>();
        }
        parts.add(part);
        return this;
    }

    public void addPart(int idx, WikiPart part) {
        if (null == parts) {
            parts = new ArrayList<>();
        }
        parts.add(idx, part);
    }

    public WikiPage removeParts(int idx) {
        this.parts.remove(idx);
        return this;
    }

    /**
     * Recomputes the text of the article based on the changes made on the parts model. 
     * @return this
     */
    public WikiPage assembleText() {
        if (null == parts) {
            return this;
        }
        StringBuilder textBuilder = new StringBuilder();
        for (WikiPart eachPart : parts) {
            textBuilder.append(eachPart.toString());
        }
        text = textBuilder.toString();
        return this;
    }

    public WikiPage load() throws IOException {
        setText(wiki.getPageText(title));
        return this;
    }

    public WikiPage addOp(Op op) {
        ops.add(op);
        return this;
    }

    public WikiPage save() throws LoginException, IOException {
        for (Op eachOp : ops) {
            eachOp.execute();
        }
        return this;
    }
    
    public List<PartContext> search(Predicate<WikiPart> predicate) {
        List<PartContext> ret = new ArrayList<>();
        for (WikiPart eachDirectPart: parts) {
            if (predicate.test(eachDirectPart)) {
                PartContext res = new PartContext(eachDirectPart);
                res.setPage(this);
                res.setParentPart(null);
                res.setSiblings(parts);
            }
            ret.addAll(eachDirectPart.search(predicate));
        }
        return ret;
    }
}
