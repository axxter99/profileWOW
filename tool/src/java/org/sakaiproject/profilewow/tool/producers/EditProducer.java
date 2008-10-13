package org.sakaiproject.profilewow.tool.producers;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sakaiproject.api.common.edu.person.SakaiPerson;
import org.sakaiproject.api.common.edu.person.SakaiPersonManager;
import org.sakaiproject.authz.api.SecurityService;
import org.sakaiproject.component.api.ServerConfigurationService;
import org.sakaiproject.entity.api.EntityPropertyNotDefinedException;
import org.sakaiproject.entity.api.EntityPropertyTypeException;
import org.sakaiproject.entity.api.ResourceProperties;
import org.sakaiproject.profilewow.tool.params.ImageViewParamaters;
import org.sakaiproject.profilewow.tool.params.SakaiPersonViewParams;
import org.sakaiproject.profilewow.tool.producers.templates.PasswordFormRenderer;
import org.sakaiproject.profilewow.tool.producers.templates.ProfilePicRenderer;
import org.sakaiproject.tool.api.ToolManager;
import org.sakaiproject.user.api.User;
import org.sakaiproject.user.api.UserDirectoryService;

import uk.org.ponder.messageutil.MessageLocator;
import uk.org.ponder.messageutil.TargettedMessageList;
import uk.org.ponder.rsf.components.UIBranchContainer;
import uk.org.ponder.rsf.components.UICommand;
import uk.org.ponder.rsf.components.UIContainer;
import uk.org.ponder.rsf.components.UIELBinding;
import uk.org.ponder.rsf.components.UIForm;
import uk.org.ponder.rsf.components.UIInput;
import uk.org.ponder.rsf.components.UIInternalLink;
import uk.org.ponder.rsf.components.UILink;
import uk.org.ponder.rsf.components.UIMessage;
import uk.org.ponder.rsf.components.UIOutput;
import uk.org.ponder.rsf.components.UISelect;
import uk.org.ponder.rsf.components.UISelectChoice;
import uk.org.ponder.rsf.components.UISelectLabel;
import uk.org.ponder.rsf.components.decorators.DecoratorList;
import uk.org.ponder.rsf.components.decorators.UIDisabledDecorator;
import uk.org.ponder.rsf.components.decorators.UILabelTargetDecorator;
import uk.org.ponder.rsf.view.ComponentChecker;
import uk.org.ponder.rsf.view.DefaultView;
import uk.org.ponder.rsf.view.ViewComponentProducer;
import uk.org.ponder.rsf.viewstate.SimpleViewParameters;
import uk.org.ponder.rsf.viewstate.ViewParameters;
import uk.org.ponder.rsf.viewstate.ViewParamsReporter;

public class EditProducer implements ViewComponentProducer {

	public static final String VIEW_ID = "editProfile";
	
	

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

	private ServerConfigurationService serverConfigurationService;
	public void setServerConfigurationService(
			ServerConfigurationService serverConfigurationService) {
		this.serverConfigurationService = serverConfigurationService;
	}

	
	private SecurityService securityService;

	public void setSecurityService(SecurityService securityService) {
		this.securityService = securityService;
	}
	
	private ToolManager toolManager;
	public void setToolManager(ToolManager toolManager) {
		this.toolManager = toolManager;
	}

	
	private TargettedMessageList tml;
	public void setTargettedMessageList(TargettedMessageList tml) {
		this.tml = tml;
	}

	private ProfilePicRenderer profilePicRenderer;
	public void setProfilePicRenderer(ProfilePicRenderer profilePicRenderer) {
		this.profilePicRenderer = profilePicRenderer;
	}
	
	private PasswordFormRenderer passwordFormRenderer;
	public void setPasswordFormRenderer(PasswordFormRenderer passwordFormRenderer) {
		this.passwordFormRenderer = passwordFormRenderer;
	}

	
	public void fillComponents(UIContainer tofill, ViewParameters viewparams,
			ComponentChecker checker) {

		//process any messages
		
		if (tml.size() > 0) {
			for (int i = 0; i < tml.size(); i ++ ) {
				UIBranchContainer errorRow = UIBranchContainer.make(tofill,"error-row:", new Integer(i).toString());
				//if (tml.messageAt(i).args != null ) {	    		
					//UIMessage.make(errorRow,"error",tml.messageAt(i).acquireMessageCode(),(String[])tml.messageAt(i).args[0]);
				//} else {
					UIMessage.make(errorRow,"error",tml.messageAt(i).acquireMessageCode());
				//}

			}
		}


		
		UIForm form = UIForm.make(tofill,"edit-form");

		SakaiPerson sPerson = spm.getSakaiPerson(spm.getUserMutableType());
		if (sPerson == null) {
			log.debug("creating a new profile!");
			sPerson = spm.create(userDirectoryService.getCurrentUser().getId(), spm.getUserMutableType());
			spm.save(sPerson);
		}

		log.debug("got profile for: " + sPerson.getGivenName() + " " + sPerson.getSurname());
		log.debug("uuid: " + sPerson.getUid() + ", agent_uuid: " + sPerson.getAgentUuid());

		
		//makeProfilePic(tofill, sPerson); 
		profilePicRenderer.makeProfilePic(tofill, "profile-pic:", sPerson);
		passwordFormRenderer.renderPasswordForm(tofill, "passForm:", sPerson);
		
		
		String otpBean = "profileBeanLocator." + sPerson.getUid() + ".sakaiPerson";

		
		boolean enableEdit = this.canEditeName(userDirectoryService.getCurrentUser());
		if (!enableEdit) {
			//fName.decorators = new DecoratorList(new UIDisabledDecorator(true));
			//lName.decorators = new DecoratorList(new UIDisabledDecorator(true));
			UIOutput.make(form, "firstname-text",sPerson.getGivenName());
			UIOutput.make(form, "lastname-text", sPerson.getSurname());
			form.parameters.add(new UIELBinding(otpBean + ".givenName" ,sPerson.getGivenName()));
			form.parameters.add(new UIELBinding(otpBean + ".surname", sPerson.getSurname()));
		} else {
			UIInput fName = UIInput.make(form,"editProfileForm-first_name", otpBean + ".givenName" ,sPerson.getGivenName());
			UIInput lName = UIInput.make(form,"editProfileForm-lname", otpBean + ".surname", sPerson.getSurname());
		}

	
		
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



		//hide
		String hideS = "false";
		if (sPerson.getHidePrivateInfo()!=null && sPerson.getHidePrivateInfo().booleanValue()) {
			hideS = "true";
		}
		log.debug("hide personal is " + hideS);

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



		UICommand.make(form, "profile-save","Save","profileBeanLocator.saveAll");


		//UIInternalLink.make(tofill, "change-pic", messageLocator.getMessage("editProfile.changePic"), new SimpleViewParameters(ChangePicture.VIEW_ID));
		UIInternalLink.make(tofill, "upload-pic", "editProfile.uploadPic");
		/*
		//picture stuff
		String picUrl = sPerson.getPictureUrl();
		if (picUrl == null || picUrl.trim().length() == 0)
			picUrl = MainProducer.NO_PIC_URL;
		else 
			picUrl = sPerson.getPictureUrl();


		if (sPerson.isSystemPicturePreferred() != null &&  sPerson.isSystemPicturePreferred().booleanValue()) {
			UIInternalLink.make(tofill, "current-pic", new ImageViewParamaters("imageServlet", sPerson.getUuid()));
		} else if (sPerson.isSystemPicturePreferred() == null || !sPerson.isSystemPicturePreferred().booleanValue() ) {
			UILink.make(tofill, "current-pic", picUrl);
		} 
		
		//the official pic selector
		if (sPerson.isSystemPicturePreferred() == null || !sPerson.isSystemPicturePreferred().booleanValue()) {
			UIForm opForm = UIForm.make(tofill, "use-official-form");
			UICommand.make(opForm, "use-official", "profileBeanLocator.useOfficialPic");
		}
		*/
		
		//the change password form
		//only render for some users
		/*
		if (canChangePassword(userDirectoryService.getCurrentUser())) {
			UIForm passForm = UIForm.make(tofill, "passForm:");
			UIInput.make(passForm,"pass1","userBeanLocator." + sPerson.getUid() + ".passOne");
			UIInput.make(passForm,"pass2","userBeanLocator." + sPerson.getUid() + ".passTwo");
			//form.parameters.add(new UIELBinding("userBeanLocator." + )
			UICommand.make(passForm, "passSubmit", "userBeanLocator.saveAll");
		}		
		*/
		//UIInternalLink.make(tofill, "test", new SakaiPersonViewParams(ViewProfileProducer.VIEW_ID, sPerson.getId().toString()));
		//UIInternalLink.make(tofill, "test2", new SakaiPersonViewParams(ViewProfileProducer.VIEW_ID, sPerson.getAgentUuid()));
		
		/**
		 * The upload picture form
		 */
		UIForm form2 = UIForm.make(tofill, "upload-pic-form");
		//UIInput.make(form,"file-upload", "uploadBean")
		UICommand.make(form2,"submit","uploadBean.processUpload");
	}


	
	private boolean canEditeName(User u) {
		if (securityService.unlock(UserDirectoryService.SECURE_UPDATE_USER_OWN_NAME, "/site/" + toolManager.getCurrentPlacement().getContext())) {
			log.debug("user can change name");
			return true;
		}
		return false;		
	
		
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
		log.debug("got sms notification of: " + val + ", "+ ret.toString());

		if (ret == null)
			ret = new Boolean(false);

		return ret;

	}









}
