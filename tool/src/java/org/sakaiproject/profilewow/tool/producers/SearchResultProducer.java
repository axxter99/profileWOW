package org.sakaiproject.profilewow.tool.producers;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.sakaiproject.api.common.edu.person.SakaiPerson;
import org.sakaiproject.api.common.edu.person.SakaiPersonManager;
import org.sakaiproject.authz.api.SecurityService;
import org.sakaiproject.component.api.ServerConfigurationService;
import org.sakaiproject.entitybroker.DeveloperHelperService;
import org.sakaiproject.entitybroker.EntityReference;
import org.sakaiproject.profilewow.tool.params.SakaiPersonViewParams;
import org.sakaiproject.profilewow.tool.params.SearchViewParamaters;
import org.sakaiproject.profilewow.tool.producers.templates.SearchBoxRenderer;
import org.sakaiproject.search.api.InvalidSearchQueryException;
import org.sakaiproject.search.api.SearchList;
import org.sakaiproject.search.api.SearchResult;
import org.sakaiproject.search.api.SearchService;
import org.sakaiproject.user.api.User;
import org.sakaiproject.user.api.UserDirectoryService;
import org.sakaiproject.user.api.UserNotDefinedException;

import lombok.extern.slf4j.Slf4j;
import uk.org.ponder.messageutil.TargettedMessageList;
import uk.org.ponder.rsf.components.UIBranchContainer;
import uk.org.ponder.rsf.components.UIContainer;
import uk.org.ponder.rsf.components.UIInternalLink;
import uk.org.ponder.rsf.components.UIMessage;
import uk.org.ponder.rsf.components.UIOutput;
import uk.org.ponder.rsf.view.ComponentChecker;
import uk.org.ponder.rsf.view.ViewComponentProducer;
import uk.org.ponder.rsf.viewstate.ViewParameters;
import uk.org.ponder.rsf.viewstate.ViewParamsReporter;

@Slf4j
public class SearchResultProducer implements ViewComponentProducer,ViewParamsReporter {

	
	public static final String VIEW_ID= "searchProfile";
	public String getViewID() {
		// TODO Auto-generated method stub
		return VIEW_ID;
	}

	
	private SakaiPersonManager sakaiPersonManager;
	public void setSakaiPersonManager(SakaiPersonManager in) {
		sakaiPersonManager = in;
	}
	
	private TargettedMessageList messages;
	public void setMessages(TargettedMessageList messages) {
		this.messages = messages;
	}
	
	private SecurityService securityService;
	public void setSecurityService(SecurityService securityService) {
		this.securityService = securityService;
	}
	
	private UserDirectoryService userDirectoryService;
	public void setUserDirectoryService(UserDirectoryService uds) {
		this.userDirectoryService = uds;
	}
	
	private SearchBoxRenderer searchBoxRenderer;
	public void setSearchBoxRenderer(SearchBoxRenderer searchBoxRenderer) {
		this.searchBoxRenderer = searchBoxRenderer;
	}
	
	private ServerConfigurationService serverConfigurationService;
	public void setServerConfigurationService(
			ServerConfigurationService serverConfigurationService) {
		this.serverConfigurationService = serverConfigurationService;
	}

	private SearchService searchService;
	public void setSearchService(SearchService searchService) {
		this.searchService = searchService;
	}
	private DeveloperHelperService developerHelperService;
	public void setDeveloperHelperService(
			DeveloperHelperService developerhelperSerive) {
		this.developerHelperService = developerhelperSerive;
	}

	private static final int SEARCH_PAGING_SIZE = 20;
	/**
	 * prefix for profile objects
	 */
	private static String PROFILE_PREFIX = "profile";
	
	
	private boolean moreResults = false;
	
	private int numberOfpages =0;
	
	public void fillComponents(UIContainer tofill, ViewParameters viewparams,
			ComponentChecker checker) {
		
		SearchViewParamaters svp = (SearchViewParamaters)viewparams;
		String searchString = svp.searchText;
		log.debug("search string is: "  + searchString);
		int start = 0;
		if (svp.start != null) {
			start = Integer.valueOf(svp.start).intValue();
		} 
		
		
		if (start < 0) {
			start = 0;
		}
		log.debug("Start: " + start);
		moreResults = false;
		List<SakaiPerson> profiles = this.findProfiles(searchString, start, start + SEARCH_PAGING_SIZE);
		UIMessage.make(tofill, "searchTitle", "searchTitle", new Object[]{ searchString});
		
		
		
		if (useSearchService() && moreResults) {
			log.debug("rendering the next!");
			UIInternalLink.make(tofill, "searchNext", new SearchViewParamaters(svp.viewID, searchString, Integer.valueOf(start +  SEARCH_PAGING_SIZE).toString()));
		}
		if (useSearchService() && start != 0) {
			log.debug("rendering the back!");
			UIInternalLink.make(tofill, "searchBack", new SearchViewParamaters(svp.viewID, searchString, Integer.valueOf(start - SEARCH_PAGING_SIZE - 1).toString()));
			
		}
		
		
		
		searchBoxRenderer.renderSearchBox(tofill, "search:");
		
		for (int i =0 ; i < profiles.size(); i++) {
			SakaiPerson sPerson = (SakaiPerson) profiles.get(i);
			log.debug("creating row for " + sPerson.getGivenName());
			UIBranchContainer row = UIBranchContainer.make(tofill, "resultRow:");
			String eid = null;
			try {
				eid = userDirectoryService.getUserEid(sPerson.getAgentUuid());
			} catch (UserNotDefinedException e) {
				// could be an orphaned record
				log.debug("user does not exits"  + sPerson.getAgentUuid());
				continue;
			}
			if(sPerson.getSurname()==null && sPerson.getGivenName()==null){
				UIInternalLink.make(row, "resultLink", eid, new SakaiPersonViewParams(ViewProfileProducer.VIEW_ID, eid));
			}
			else{
				String label = sPerson.getSurname() == null ? "" : sPerson.getSurname();
				label += (sPerson.getSurname() != null && sPerson.getGivenName()!=null) ? ", " : "";
				label += sPerson.getGivenName()==null ? "" : sPerson.getGivenName();
				UIInternalLink.make(row, "resultLink", label,
					new SakaiPersonViewParams(ViewProfileProducer.VIEW_ID, eid));
			}
			
		}
		if(profiles.size() == 15 && useSearchService())
				UIOutput.make(tofill, "limitmessage");
			//log.debug(profiles.size());
	}

	public ViewParameters getViewParameters() {
		// TODO Auto-generated method stub
		return new SearchViewParamaters();
	}


	private List<SakaiPerson> findProfiles(String searchString, int start, int end) {
		if (useSearchService())
			return findProfilesSearch(searchString, start, end);
		else
			return findProfilesDB(searchString);
	}

	private boolean useSearchService() {
		return serverConfigurationService.getBoolean("profilewow.useSearch", false) && searchService.isEnabled();
	}
	
	
	private List<SakaiPerson> findProfilesSearch(String searchString, int start, int end) {
		List<SakaiPerson>  searchResults = new ArrayList<SakaiPerson> ();
		List<String> contexts = new ArrayList<String>();
		contexts.add("~global");
		contexts.add(developerHelperService.getCurrentLocationId());
		log.debug("searchString: " + searchString);
		String searchFor ="+" + searchString + " +tool:profile"; //  + " +tool:" + PROFILE_PREFIX;
		log.debug("were going to search for: " + searchFor);
		long startTime = System.currentTimeMillis();
		log.debug("searching from: " + start + " to: " + end);
		SearchList res = null;
		try {
			res = searchService.search(searchFor, contexts, start, end);
		} catch (InvalidSearchQueryException e) {
			log.warn(e.getMessage(), e);
			return null;
		}
		log.debug("search got: " + res.size() + " results full size: " + res.getFullSize());
		moreResults = (end  < res.getFullSize());
		
		long endTime = System.currentTimeMillis();
		log.debug("got " + res.size() + " search results in: " + (endTime - startTime) + " ms");
		
		//get the nuber of pages in the result
		double pagesRaw = (double)res.getFullSize() / (double)SEARCH_PAGING_SIZE;
		numberOfpages = (int)Math.ceil(pagesRaw);
		log.debug("found " + numberOfpages + " pages in a resultset of " + res.getFullSize());
		
		
		
		
		//this list actually contains all the items
		Iterator<SearchResult> i = res.iterator();
		while (i.hasNext()) {
			SearchResult resI =  i.next();  //(SearchResult) res.get(i);
			String ref = resI.getReference();
			
			log.debug("ref: " + ref);
			String id = EntityReference.getIdFromRefByKey(ref, "id");
			String prefix = EntityReference.getPrefix(ref);
			if (!PROFILE_PREFIX.equals(prefix)) {
				log.warn(ref + " is not a profile object");
				continue;
			}
			
			log.debug("getting id: " + id);
			SakaiPerson profile = sakaiPersonManager.getSakaiPerson(id, sakaiPersonManager.getUserMutableType());
			// Select the user mutable profile for display on if the public information is viewable.
			if ((profile != null)
					&& profile.getTypeUuid().equals(sakaiPersonManager.getUserMutableType().getUuid()))
			{
				if ((getCurrentUserId().equals(profile.getAgentUuid()) || securityService.isSuperUser()))
				{
					// allow user to search and view own profile and superuser to view all profiles
					searchResults.add(profile);
				}
				else if ((profile.getHidePublicInfo() != null) && (profile.getHidePublicInfo().booleanValue() != true))
				{
					if (profile.getHidePrivateInfo() != null && profile.getHidePrivateInfo().booleanValue() != true)
					{
						searchResults.add(profile);
					}
					else
					{
						searchResults.add(getOnlyPublicProfile(profile));
					}

				}
			}
			
		}
		return searchResults;
	}
	
	private List<SakaiPerson> findProfilesDB(String searchString)
	{
		if (log.isDebugEnabled())
		{
			log.debug("findProfiles(" + searchString + ")");
		}
		if (searchString == null || searchString.length() < 4)
			throw new IllegalArgumentException("Illegal searchString argument passed!");

		List<SakaiPerson> profiles = sakaiPersonManager.findSakaiPerson(searchString);
		List<SakaiPerson> searchResults = new ArrayList<SakaiPerson>();
		SakaiPerson profile;

		if ((profiles != null) && (profiles.size() > 0))
		{
			Iterator<SakaiPerson> profileIterator = profiles.iterator();

			while (profileIterator.hasNext())
			{
				profile = (SakaiPerson) profileIterator.next();
				
				// Select the user mutable profile for display on if the public information is viewable.
				if ((profile != null)
						&& profile.getTypeUuid().equals(sakaiPersonManager.getUserMutableType().getUuid())
						&& getSakai(profile))
				{
					if ((getCurrentUserId().equals(profile.getAgentUuid()) || securityService.isSuperUser()))
					{
						// allow user to search and view own profile and superuser to view all profiles
						searchResults.add(profile);
					}
					else if ((profile.getHidePublicInfo() != null) && (profile.getHidePublicInfo().booleanValue() != true))
					{
						if (profile.getHidePrivateInfo() != null && profile.getHidePrivateInfo().booleanValue() != true)
						{
							searchResults.add(profile);
						}
						else
						{
							searchResults.add(getOnlyPublicProfile(profile));
						}
					}
				}
				
				if(searchResults.size() == 15)
					break;

			}
		}
		log.info("found a resultsList of: " + searchResults.size());
		return searchResults;
	}

	private boolean getSakai(SakaiPerson profile) {
		  // User account types
		  String TYPE_STUDENT = "student";
		  String TYPE_STAFF = "staff";
		  String TYPE_THIRDPARTY = "thirdparty";
		  //String TYPE_OFFER = "offer";
		  
		log.info("User: " + profile + "Type: " + profile.getTypeUuid());
		User user = null;
		try {
			user = userDirectoryService.getUser(profile.getAgentUuid());
			String type = user.getType();
			log.info("User: " + user.getType());
			if ("guest".equals(type)) {
				return true;
			} else if (TYPE_STUDENT.equals(type)) {
				return true;
			} else if (TYPE_STAFF.equals(type)) {
				return true;
			} else if (TYPE_THIRDPARTY.equals(type)) {
				return true;
			}
		} catch (UserNotDefinedException e) {
			log.info(e.getLocalizedMessage(), e);
		}
		
		return false;
	}
	private SakaiPerson getOnlyPublicProfile(SakaiPerson profile)
	{
		if (log.isDebugEnabled())
		{
			log.debug("getOnlyPublicProfile(Profile" + profile + ")");
		}
		profile.setJpegPhoto(null);
		profile.setPictureUrl(null);
		profile.setMail(null);
		profile.setLabeledURI(null);
		profile.setHomePhone(null);
		profile.setNotes(null);
		return profile;
	}
	
	private String getCurrentUserId() {
		return userDirectoryService.getCurrentUser().getId();
	}
	
	
}
