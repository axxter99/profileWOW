package org.sakaiproject.profilewow.tool.producers;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sakaiproject.api.common.edu.person.SakaiPerson;
import org.sakaiproject.api.common.edu.person.SakaiPersonManager;
import org.sakaiproject.content.api.ContentCollection;
import org.sakaiproject.content.api.ContentCollectionEdit;
import org.sakaiproject.content.api.ContentHostingService;
import org.sakaiproject.content.api.ContentResource;
import org.sakaiproject.entity.api.Entity;
import org.sakaiproject.entity.api.ResourceProperties;
import org.sakaiproject.entity.api.ResourcePropertiesEdit;
import org.sakaiproject.exception.IdUnusedException;
import org.sakaiproject.exception.PermissionException;
import org.sakaiproject.exception.TypeException;
import org.sakaiproject.profilewow.tool.params.ImageViewParamaters;
import org.sakaiproject.profilewow.tool.producers.templates.ProfilePicRenderer;
import org.sakaiproject.profilewow.tool.util.ResourceUtil;
import org.sakaiproject.site.api.SiteService;
import org.sakaiproject.user.api.UserDirectoryService;

import uk.org.ponder.messageutil.TargettedMessageList;
import uk.org.ponder.rsf.components.UIBranchContainer;
import uk.org.ponder.rsf.components.UICommand;
import uk.org.ponder.rsf.components.UIContainer;
import uk.org.ponder.rsf.components.UIForm;
import uk.org.ponder.rsf.components.UIInternalLink;
import uk.org.ponder.rsf.components.UILink;
import uk.org.ponder.rsf.components.UIMessage;
import uk.org.ponder.rsf.components.UIOutput;
import uk.org.ponder.rsf.components.UISelect;
import uk.org.ponder.rsf.components.UISelectChoice;
import uk.org.ponder.rsf.components.UIVerbatim;
import uk.org.ponder.rsf.flow.jsfnav.NavigationCase;
import uk.org.ponder.rsf.flow.jsfnav.NavigationCaseReporter;
import uk.org.ponder.rsf.view.ComponentChecker;
import uk.org.ponder.rsf.view.ViewComponentProducer;
import uk.org.ponder.rsf.viewstate.SimpleViewParameters;
import uk.org.ponder.rsf.viewstate.ViewParameters;
import uk.org.ponder.stringutil.StringList;

public class ChangePicture implements ViewComponentProducer, NavigationCaseReporter {

	private static Log log = LogFactory.getLog(ChangePicture.class);
	private static final String NO_PIC_URL = ProfilePicRenderer.NO_PIC_URL;
	

	public static final String VIEW_ID="changepic";
	
	public String getViewID() {
		// TODO Auto-generated method stub
		return VIEW_ID;
	}

	private SakaiPersonManager spm;
	public void setSakaiPersonManager(SakaiPersonManager in) {
		spm = in;
	}
	
	private ResourceUtil resourceUtil;
	public void setResourceUtil(ResourceUtil ru) {
		resourceUtil = ru;
	}

	private TargettedMessageList tml;
	public void setTargettedMessageList(TargettedMessageList tml) {
		this.tml = tml;
	}
	
	public void fillComponents(UIContainer tofill, ViewParameters viewparams,
			ComponentChecker checker) {
		// TODO Auto-generated method stub
		ContentCollection pCollection = resourceUtil.getUserCollection();
			log.debug("got a collection with " + pCollection.getMemberCount() + " objects");
			
		
		SakaiPerson sPerson = spm.getSakaiPerson(spm.getUserMutableType());
		
		//picture stuff
		String picUrl = sPerson.getPictureUrl();
		if (picUrl == null || picUrl.trim().length() == 0)
			picUrl = NO_PIC_URL;
		else 
			picUrl = sPerson.getPictureUrl();
		
		if (sPerson.isSystemPicturePreferred() != null &&  sPerson.isSystemPicturePreferred().booleanValue()) {
			//System pic present and set to active
			//UIOutput.make(tofill, "remove-image-link");
			UIBranchContainer uib = UIBranchContainer.make(tofill, "selected-image:");
			UIInternalLink.make(uib, "selected-image", new ImageViewParamaters("imageServlet", sPerson.getUuid()));
			UIBranchContainer uib2 = UIBranchContainer.make(tofill, "no-image:");
			UIInternalLink.make(uib2, "no-image", NO_PIC_URL);
			//UIMessage.make(tofill, "current-pic-title", "current.picture.title.official");	
		} else if (sPerson.isSystemPicturePreferred() == null || !sPerson.isSystemPicturePreferred().booleanValue() ) {
			UIBranchContainer uib = UIBranchContainer.make(tofill, "selected-image:");
			UILink.make(uib, "selected-image", picUrl);
			if (!picUrl.equals(NO_PIC_URL)) {
				//UIOutput.make(tofill, "remove-image-link");
				//UIMessage.make(tofill, "current-pic-title", "current.picture.title");
				UIBranchContainer uib2 = UIBranchContainer.make(tofill, "no-image:");
				UIInternalLink.make(uib2, "no-image", NO_PIC_URL);
			}else{
			//no profile image at all
				//UIMessage.make(tofill, "warning-no-image", "warning.picture.set");
				//UIMessage.make(tofill, "current-pic-title", "current.picture.title.noimage");		
				//UIBranchContainer uib2 = UIBranchContainer.make(tofill, "no-image:");
				//UIInternalLink.make(uib2, "no-image", NO_PIC_URL);
			}
			//should only display if there is an official pic
			if (hasProfilePic()) {
				UIBranchContainer op = UIBranchContainer.make(tofill, "official-pic:");
				UIMessage.make(op, "official-pic-title", "official.picture.title");
				UIInternalLink.make(op, "official-pic-image", new ImageViewParamaters("imageServlet", sPerson.getUuid() ));
				UIBranchContainer op2 = UIBranchContainer.make(tofill, "official-pic-form:");
				UIForm form = UIForm.make(op2, "official-pic-form");
				UICommand.make(form, "official-pic-field", UIMessage.make("useOfficialSub"),"uploadBean.useOfficial");
			}	
		}
		
		UIForm formUpload = UIForm.make(tofill, "upload-pic-form");
		//UIInput.make(form,"file-upload", "uploadBean")
		UICommand.make(formUpload,"submit","uploadBean.processUpload");

		UIForm form = UIForm.make(tofill,"form");
			List<ContentResource> resources = pCollection.getMemberResources();
			
			UISelect selectPic = UISelect.makeMultiple(form, "select-pic",
					null, "uploadBean.picUrl", new String[] {});
			StringList selections = new StringList();
			for (int i = 0; i < resources.size(); i++) {
				//UIBranchContainer row = UIBranchContainer.make(form, "pic-row:");
				
				//for (int q =0; q < 5 && i< resources.size(); q++) {
					ContentResource resource = (ContentResource)resources.get(i);
					String rUrl = resource.getUrl();
					if(!rUrl.equals(picUrl)){
					UIBranchContainer cell = UIBranchContainer.make(tofill, "pic-cell:");
					selections.add(rUrl);
					UISelectChoice choice =  UISelectChoice.make(cell, "select", selectPic.getFullID(), (selections.size() -1 ));
					UILink.make(cell, "pic", rUrl);
					//i++;				
					}
				//}
			}
			selectPic.optionlist.setValue(selections.toStringArray());
			UICommand.make(form, "submit","Change picture","uploadBean.changePicture");
	}

	private boolean hasProfilePic() {
		
		SakaiPerson sp = spm.getSakaiPerson(spm.getSystemMutableType());
		
		if (sp == null)
			return false;
		else if (sp.getJpegPhoto() != null)
			return true;
	
		return false;
		
	}
	public List reportNavigationCases() {
		List togo = new ArrayList(); // Always navigate back to this view.
		togo.add(new NavigationCase(null, new SimpleViewParameters(VIEW_ID)));
		togo.add(new NavigationCase("success", new SimpleViewParameters(MainProducer.VIEW_ID)));
		return togo;
	}

	

	
}
