package org.sakaiproject.profilewow.tool.producers;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sakaiproject.api.common.edu.person.SakaiPerson;
import org.sakaiproject.api.common.edu.person.SakaiPersonManager;
import org.sakaiproject.entity.api.EntityPropertyNotDefinedException;
import org.sakaiproject.entity.api.EntityPropertyTypeException;
import org.sakaiproject.entity.api.ResourceProperties;
import org.sakaiproject.profilewow.tool.params.ImageViewParamaters;
import org.sakaiproject.user.api.User;
import org.sakaiproject.user.api.UserDirectoryService;

import uk.org.ponder.messageutil.MessageLocator;
import uk.org.ponder.rsf.components.UIBranchContainer;
import uk.org.ponder.rsf.components.UICommand;
import uk.org.ponder.rsf.components.UIContainer;
import uk.org.ponder.rsf.components.UIForm;
import uk.org.ponder.rsf.components.UIInput;
import uk.org.ponder.rsf.components.UIInternalLink;
import uk.org.ponder.rsf.components.UILink;
import uk.org.ponder.rsf.components.UIMessage;
import uk.org.ponder.rsf.components.UISelect;
import uk.org.ponder.rsf.components.UISelectChoice;
import uk.org.ponder.rsf.components.UISelectLabel;
import uk.org.ponder.rsf.components.decorators.UILabelTargetDecorator;
import uk.org.ponder.rsf.view.ComponentChecker;
import uk.org.ponder.rsf.view.DefaultView;
import uk.org.ponder.rsf.view.ViewComponentProducer;
import uk.org.ponder.rsf.viewstate.SimpleViewParameters;
import uk.org.ponder.rsf.viewstate.ViewParameters;

public class EditProducer implements ViewComponentProducer, DefaultView {

	public static final String VIEW_ID = "edit";
	
	private static Log log = LogFactory.getLog(EditProducer.class);
	public String getViewID() {
		// TODO Auto-generated method stub
		return VIEW_ID;
	}

	private SakaiPersonManager spm;
	public void setSakaiPersonManager(SakaiPersonManager in) {
		spm = in;
	}
	
	private MessageLocator messageLocator;
	public void setMessageLocator(MessageLocator messageLocator) {

		this.messageLocator = messageLocator;
	}

	
	private UserDirectoryService userDirectoryService;
	public void setUserDirectoryService(UserDirectoryService uds) {
		this.userDirectoryService = uds;
	}
	
	public void fillComponents(UIContainer tofill, ViewParameters viewparams,
			ComponentChecker checker) {
		// TODO Auto-generated method stub
		
		UIForm form = UIForm.make(tofill,"edit-form");
		
		SakaiPerson sPerson = spm.getSakaiPerson(spm.getUserMutableType());
		if (sPerson == null) {
			sPerson = spm.create(userDirectoryService.getCurrentUser().getId(), spm.getUserMutableType());
			spm.save(sPerson);
		}
		
		log.info("got profile for: " + sPerson.getGivenName() + " " + sPerson.getSurname());
		
		String otpBean = "profileBeanLocator." + sPerson.getUid() + ".sakaiPerson";
		
		UIInput.make(form,"editProfileForm-first_name", otpBean + ".givenName" ,sPerson.getGivenName());
		UIInput.make(form,"editProfileForm-lname", otpBean + ".surname", sPerson.getSurname());
		UIInput.make(form,"editProfileForm-nickname", otpBean + ".nickname", sPerson.getNickname());
		UIInput.make(form,"editProfileForm-position", otpBean + ".title", sPerson.getTitle());
		UIInput.make(form,"editProfileForm-department", otpBean + ".organizationalUnit", sPerson.getOrganizationalUnit());
		UIInput.make(form,"editProfileForm-school", otpBean + ".campus", sPerson.getCampus());
		UIInput.make(form,"editProfileForm-room", otpBean + ".roomNumber", sPerson.getRoomNumber());
		UIInput.make(form,"editProfileForm-email", otpBean + ".mail", sPerson.getMail());
		UIInput.make(form,"editProfileForm-title", otpBean + ".title", sPerson.getTitle());
		//not in profile data yet
		UIInput.make(form,"editProfileForm-country", otpBean + ".localityName", sPerson.getLocalityName());
		
		UIInput.make(form,"editProfileForm-homepage", otpBean + ".labeledURI", sPerson.getLabeledURI());
		UIInput.make(form,"editProfileForm-workphone", otpBean + ".telephoneNumber", sPerson.getTelephoneNumber());
		UIInput.make(form,"editProfileForm-mobile", otpBean + ".mobile", sPerson.getMobile());
		UIInput.make(form,"editProfileForm-more", otpBean + ".notes", sPerson.getNotes());
		
		//picture stuff
		String picUrl = sPerson.getPictureUrl();
		if (picUrl == null || picUrl.trim().length() == 0)
			picUrl = "../images/pictureUnavailable.jpg";
		else 
			picUrl = sPerson.getPictureUrl();
		
		
		if (sPerson.isSystemPicturePreferred() != null &&  sPerson.isSystemPicturePreferred().booleanValue()) {
			UIInternalLink.make(form, "current-pic", new ImageViewParamaters("imageServlet", sPerson.getUuid()));
		} else if (sPerson.isSystemPicturePreferred() == null) {
			UILink.make(form, "current-pic", picUrl);
		} 
		
		//hide
		String hideS = "false";
		if (sPerson.getHidePrivateInfo()!=null && sPerson.getHidePrivateInfo().booleanValue()) {
			hideS = "true";
		}
		log.info("hide personal is " + hideS);
		
		UISelect hide = UISelect.make(form, "hide-select",new String[]{"true", "false"},
				new String[]{messageLocator.getMessage("editProfile.sms.yes"), messageLocator.getMessage("editProfile.sms.no")}, 
				otpBean + ".hidePrivateInfo", hideS);
		
		 String hideID = hide.getFullID();
	     
		 for(int i = 0; i < 2; i ++ ) {
			 UIBranchContainer radiobranch = UIBranchContainer.make(form,
					 "hideSelect:", new Integer(i).toString());
			 UISelectChoice choice = UISelectChoice.make(radiobranch, "editProfile-hide", hideID, i);
			 UISelectLabel lb = UISelectLabel.make(radiobranch, "hide-label", hideID, i);
			 UILabelTargetDecorator.targetLabel(lb, choice);
		 }
		
		
		//sms preference
		 
		UISelect sms = UISelect.make(form, "sms-select",new String[]{"true", "false"},
				new String[]{messageLocator.getMessage("editProfile.sms.yes"), messageLocator.getMessage("editProfile.sms.no")}, 
				"profileBeanLocator." + sPerson.getUid() + ".smsNotifications", recieveSMSNotifications().toString());
		 String selectID = sms.getFullID();
	     
		 for(int i = 0; i < 2; i ++ ) {
			 UIBranchContainer radiobranch = UIBranchContainer.make(form,
					 "smsSelect:", new Integer(i).toString());
			 UISelectChoice choice = UISelectChoice.make(radiobranch, "editProfileForm-sms", selectID, i);
			 UISelectLabel lb = UISelectLabel.make(radiobranch, "smslabel", selectID, i);
			 UILabelTargetDecorator.targetLabel(lb, choice);
		 }
		//UIInput sms = UIInput.make(form, "editProfileForm-sms", "profileBeanLocator." + sPerson.getUid() + ".smsNotifications", "true");
		
		//UIMessage.make(form, "smslabel","editProfile.sms");
		
		
		
		UICommand.make(form, "profile-save","save","profileBeanLocator.saveAll");
		
		
		
		
		UIInternalLink.make(form, "change-pic", messageLocator.getMessage("editProfile.changePic"), new SimpleViewParameters(ChangePicture.VIEW_ID));
		
		//the change password form
		UIForm passForm = UIForm.make(tofill, "passForm:");
		UIInput.make(passForm,"pass1","userBeanLocator." + sPerson.getUid() + ".pass1");
		UIInput.make(passForm,"pass2","userBeanLocator." + sPerson.getUid() + ".pass2");
		UICommand.make(passForm, "passSubmit", "userBeanLocator.saveAll");
	}
	
	
	/**
	 * Returns String for image. Uses the config bundle
	 * to return paths to not available images.  
	 */
	private String getUrlOfficialPicture(String userId) {
		
		String imageUrl = "";
			
		imageUrl = "ProfileImageServlet.prf?photo=" + userId;
			
		return imageUrl; 
	}
	
	
	private Boolean recieveSMSNotifications() {
		User u = userDirectoryService.getCurrentUser();
		ResourceProperties rp = u.getProperties();
		
		Boolean ret = null;
		
			String val = rp.getProperty("smsnotifications");
			ret = new Boolean(val);
			log.info("got sms notification of: " + val + ", "+ ret.toString());
		
		if (ret == null)
			ret = new Boolean(false);
		
		return ret;
		
	}

	
	
}
