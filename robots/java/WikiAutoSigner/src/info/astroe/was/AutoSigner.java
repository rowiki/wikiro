package info.astroe.was;

import static org.wikipedia.Wiki.CATEGORY_TALK_NAMESPACE;
import static org.wikipedia.Wiki.HIDE_BOT;
import static org.wikipedia.Wiki.HIDE_SELF;
import static org.wikipedia.Wiki.PROJECT_NAMESPACE;
import static org.wikipedia.Wiki.PROJECT_TALK_NAMESPACE;
import static org.wikipedia.Wiki.TALK_NAMESPACE;
import static org.wikipedia.Wiki.USER_TALK_NAMESPACE;

import java.io.IOException;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;

import javax.security.auth.login.FailedLoginException;

import org.wikipedia.Wiki;
import org.wikipedia.Wiki.Revision;

public class AutoSigner {
	
	private static Wiki wiki;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		wiki = new Wiki("ro.wikipedia.org");
		try {
			wiki.login("Andrebot", "**".toCharArray());
			Revision[] revisions = wiki.recentChanges(20, HIDE_BOT | HIDE_SELF,
					new int[] { TALK_NAMESPACE, USER_TALK_NAMESPACE,
							PROJECT_TALK_NAMESPACE, PROJECT_NAMESPACE,
							CATEGORY_TALK_NAMESPACE });
			for (Revision rev : revisions) {
				System.out.println(rev.getPage() + "\t" + rev.getRevid());
				if (rev.getRevid() == 0) {
					continue;
				}
				String crtText = rev.getText();
				String crtAuthor = rev.getUser();

				//get a chunk of history large enough to compare
				Calendar now = Calendar.getInstance();
				Calendar yesterday = Calendar.getInstance();
				yesterday.roll(Calendar.DATE, false);
				Revision[] pageHistory = wiki.getPageHistory(rev.getPage(),
						now, yesterday);
				while (pageHistory.length < 2 && countEditors(pageHistory) < 2 && !isNew(pageHistory[pageHistory.length - 1])) {
					yesterday.add(Calendar.MONTH, -1);
					pageHistory = wiki.getPageHistory(rev.getPage(), now,
							yesterday);
				}
				
				if (isNew(pageHistory[0])) {
					analyzeText(pageHistory[0].getText());
				}
				//locate the last message entered by another user
				int i;
				for (i = 1; i < pageHistory.length && pageHistory[i].getUser().equals(crtAuthor); i++) {					
				}
				Revision lastMessageByOther = pageHistory[i];
				
				String diff = rev.diff(lastMessageByOther.getRevid());

				System.out.println(diff);
			}
		} catch (FailedLoginException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static void analyzeText(String text) {
		// TODO Auto-generated method stub
		
	}

	private static int countEditors(Revision[] pageHistory) {
		Set<String> editors = new HashSet<String>();
		for (Revision rev : pageHistory) {
			editors.add(rev.getUser());
		}
		return editors.size();
	}
	
	private static boolean isNew(Revision rev) throws IOException {
		if (rev.isNew()) {
			return true;
		}
		
		String page = rev.getPage();
		Revision firstPageRev = wiki.getFirstRevision(page);
		return firstPageRev.getRevid() == rev.getRevid();
	}
}
